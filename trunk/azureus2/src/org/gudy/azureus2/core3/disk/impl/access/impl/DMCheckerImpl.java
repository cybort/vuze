/*
 * Created on 31-Jul-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.disk.impl.access.impl;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.impl.DiskManagerHelper;
import org.gudy.azureus2.core3.disk.impl.access.*;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceList;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceMapEntry;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;

/**
 * @author parg
 *
 */

public class 
DMCheckerImpl 
	implements DMChecker
{
	private static final LogIDs LOGID = LogIDs.DISK;
    
	private static boolean 	friendly_hashing;
	private static boolean	flush_pieces;

    static{
    	
    	 ParameterListener param_listener = new ParameterListener() {
    	    public void 
			parameterChanged( 
				String  str ) 
    	    {
    	      friendly_hashing 	= COConfigurationManager.getBooleanParameter( "diskmanager.friendly.hashchecking" );
    	  	  
    	  	  flush_pieces		= COConfigurationManager.getBooleanParameter( "diskmanager.perf.cache.flushpieces" );

    	    }
    	 };

 		COConfigurationManager.addAndFireParameterListener( "diskmanager.friendly.hashchecking", param_listener );
		COConfigurationManager.addParameterListener( "diskmanager.perf.cache.flushpieces", param_listener );

    }

	private DiskManagerHelper		disk_manager;
		
	private int				async_checks;
	private AESemaphore		async_check_sem 	= new AESemaphore("DMChecker::asyncCheck");
	
	private int				async_reads;
	private AESemaphore		async_read_sem 		= new AESemaphore("DMChecker::asyncRead");

	private boolean	started;
	
	private volatile boolean	stopped;
	
	private int			pieceLength;
	private int			lastPieceLength;
	
	private int		nbPieces;
	
	private boolean	complete_recheck_in_progress;
	
	private AEMonitor		this_mon	= new AEMonitor( "DMChecker" );
		
	public
	DMCheckerImpl(
		DiskManagerHelper	_disk_manager )
	{
		disk_manager	= _disk_manager;
		
		pieceLength		= disk_manager.getPieceLength();
		lastPieceLength	= disk_manager.getLastPieceLength();
		
		nbPieces		= disk_manager.getNumberOfPieces();
	}

	public void
	start()
	{
		try{
			this_mon.enter();

			if ( started ){
				
				throw( new RuntimeException( "DMChecker: start while started"));
			}
			
			if ( stopped ){
				
				throw( new RuntimeException( "DMChecker: start after stopped"));
			}

			started	= true;
 			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	stop()
	{
		int	check_wait;
		int	read_wait;
		
		try{
			this_mon.enter();

			if ( stopped || !started ){
			
				return;
			}
					
				// when we exit here we guarantee that all file usage operations have completed
				// i.e. writes and checks (checks being doubly async)
			
			stopped	= true;
         			
			read_wait	= async_reads;
			check_wait	= async_checks;
			
		}finally{
			
			this_mon.exit();
		}		
	
			// wait for reads
		
		for (int i=0;i<read_wait;i++){
			
			async_read_sem.reserve();
		}
		
			// wait for checks
		
		for (int i=0;i<check_wait;i++){
			
			async_check_sem.reserve();
		}
	}
	
	public boolean 
	isChecking() 
	{
	   return( complete_recheck_in_progress );
	}
	  
	public void 
	enqueueCompleteRecheckRequest(
		final DiskManagerCheckRequestListener 	listener,
		final Object							user_data ) 
	{  	
	 	Thread t = new AEThread("DMChecker::completeRecheck")
		{
	  		public void
			runSupport()
	  		{
	  			try{
	  				complete_recheck_in_progress	= true;
	  					
	  				final AESemaphore	sem = new AESemaphore( "DMChecker::completeRecheck" );
	  				
	  				int	checks_submitted	= 0;
	  				
		            int delay = 0;  //if friendly hashing is enabled, no need to delay even more here
		
		            if( !friendly_hashing ) {
		              //delay a bit normally anyway, as we don't want to kill the user's system
		              //during the post-completion check (10k of piece = 1ms of sleep)
		              delay = pieceLength /1024 /10;
		              delay = Math.min( delay, 409 );
		              delay = Math.max( delay, 12 );
		            }
            
            
		            final AESemaphore	 run_sem = new AESemaphore( "DMChecker::completeRecheck:runsem", 2 );
		            
	  				for ( int i=0; i < nbPieces; i++ ){
	  					
	  					run_sem.reserve();
	  					
	  					if ( stopped ){
	  						
	  						break;
	  					}
	  					
	  					enqueueCheckRequest( 
	  	       				i, 
	  	       				new DiskManagerCheckRequestListener()
							{
		  	       				public void
								pieceChecked( 
									int 		_pieceNumber, 
									boolean 	_result,
									Object		_user_data )
		  	       				{
		  	       					try{
		  	       						listener.pieceChecked( _pieceNumber, _result, _user_data );
		  	       						
		  	       					}finally{
		  	       						
		  	       						run_sem.release();
		  	       						
		  	       						sem.release();
		  	       					}
		  	       				}
							},
							user_data,
							false );
	  					
	  					checks_submitted++;
	  					
	  					if( delay > 0 ){
	  						
	  						try{
	  						
	  							Thread.sleep( delay );
	  							
	  						}catch( Throwable e ){
	  							
	  						}
	  					}
	  				}
	  					  					
	  					// wait for all to complete
	  					
	  				for (int i=0;i<checks_submitted;i++){
	  						
	  					sem.reserve();
	  				}
	  	       }finally{
	  	       	
	  	       		complete_recheck_in_progress	= false;
	  	       }
	        }     			
	 	};
	
	 	t.setDaemon(true);
	 	
	 	t.start();
	}
	
	public void 
	enqueueCheckRequest(
		int 									pieceNumber,
		final DiskManagerCheckRequestListener 	listener,
		Object									user_data ) 
	{
		enqueueCheckRequest( pieceNumber, listener, user_data, flush_pieces );
	}
	
	protected void 
	enqueueCheckRequest(
		int 									pieceNumber,
		final DiskManagerCheckRequestListener 	listener,
		Object									user_data,
		boolean									read_flush ) 
	{  	
		checkPiece( 
				pieceNumber, 
				new DMCheckerRequestListener() 
				{
					public void 
					processResult(
						int 	pieceNumber, 
						int 	result,
						Object 	user_data ) 
					{
						if (result == DMCheckerRequestListener.OP_SUCCESS) {
							if (Logger.isEnabled())
								Logger.log(new LogEvent(disk_manager, LOGID, "Piece "
										+ pieceNumber + " passed hash check."));
		
						} else if (result == DMCheckerRequestListener.OP_FAILURE) {
							if (Logger.isEnabled())
								Logger.log(new LogEvent(disk_manager, LOGID,
										LogEvent.LT_ERROR, "Piece " + pieceNumber
												+ " failed hash check."));
		
						} else {
							if (Logger.isEnabled())
								Logger.log(new LogEvent(disk_manager, LOGID,
										LogEvent.LT_ERROR, "Piece " + pieceNumber
												+ " hash check cancelled."));
		
						}

						if ( listener != null){

							listener.pieceChecked(
									pieceNumber,
									result == DMCheckerRequestListener.OP_SUCCESS,
									user_data );
						}
					}
				}, user_data, false, read_flush );
	}  
	  
	public void
	checkPiece(
		final int 						pieceNumber,
		final DMCheckerRequestListener	_result_handler,
		final Object					user_data,
		final boolean					low_priorty )
	{
		checkPiece( pieceNumber, _result_handler, user_data, low_priorty, false );
	}
	
	protected void
	checkPiece(
		final int 						pieceNumber,
		final DMCheckerRequestListener	_result_handler,
		final Object					user_data,
		final boolean					low_priorty,
		boolean							read_flush )
	{
		final DMCheckerRequestListener	result_handler =
			new DMCheckerRequestListener()
			{
				public void
				processResult(
					int		piece_number,
					int		result,
					Object	_user_data )
				{
					try{						
						disk_manager.getPieces()[piece_number].setDone( result == DMCheckerRequestListener.OP_SUCCESS );
						
					}finally{
						
						if ( _result_handler != null ){
							
							_result_handler.processResult( pieceNumber, result, _user_data );
						}
					}
				}
			};
					             		
		try{
			
			final byte[]	required_hash = disk_manager.getPieceHash(pieceNumber);
	        
				// quick check that the files that make up this piece are at least big enough
				// to warrant reading the data to check
			
			DMPieceList pieceList = disk_manager.getPieceList(pieceNumber);

			try{
				for (int i = 0; i < pieceList.size(); i++) {
					
					DMPieceMapEntry piece_entry = pieceList.get(i);
						
					if ( piece_entry.getFile().getCacheFile().getLength() < piece_entry.getOffset()){
							
						result_handler.processResult( pieceNumber, DMCheckerRequestListener.OP_FAILURE, user_data );
						
						return;
					}
				}
			}catch( Throwable e ){
			
					// we can fail here if the disk manager has been stopped as the cache file length access may be being
					// performed on a "closed" (i.e. un-owned) file
				
				result_handler.processResult( pieceNumber, DMCheckerRequestListener.OP_CANCELLED, user_data );

				return;
			}
			
			int this_piece_length = pieceNumber < nbPieces - 1 ? pieceLength : lastPieceLength;

			DiskManagerReadRequest read_request = disk_manager.createReadRequest( pieceNumber, 0, this_piece_length );
			
		   	try{
		   		this_mon.enter();
		   	
				if ( stopped ){
					
					result_handler.processResult( pieceNumber, DMCheckerRequestListener.OP_CANCELLED, user_data );
					
					return;
				}
				
				async_reads++;
		   		
		   	}finally{
		   		
		   		this_mon.exit();
		   	}
		   	
		   	read_request.setFlush( read_flush );
		   	
			disk_manager.enqueueReadRequest( 
				read_request,
				new DiskManagerReadRequestListener()
				{
					public void 
					readCompleted( 
						DiskManagerReadRequest 	read_request, 
						DirectByteBuffer 		buffer )
					{
						complete();
						
					   	try{
					   		this_mon.enter();
					   	
							if ( stopped ){
								
								buffer.returnToPool();
								
								result_handler.processResult( pieceNumber, DMCheckerRequestListener.OP_CANCELLED, user_data );
								
								return;
							}
							
							async_checks++;
					   		
					   	}finally{
					   		
					   		this_mon.exit();
					   	}
						
						try{
					    	final	DirectByteBuffer	f_buffer	= buffer;
					    	
						   	ConcurrentHasher.getSingleton().addRequest(
					    			buffer.getBuffer(DirectByteBuffer.SS_DW),
									new ConcurrentHasherRequestListener()
									{
					    				public void
										complete(
											ConcurrentHasherRequest	request )
					    				{
					    					int	async_result	= DMCheckerRequestListener.OP_CANCELLED;
					    						    		    					
					    					try{
					    						
												byte[] testHash = request.getResult();
														    								
												if ( testHash != null ){
															
				    								async_result = DMCheckerRequestListener.OP_SUCCESS;
				    								
				    								for (int i = 0; i < testHash.length; i++){
				    									
				    									if ( testHash[i] != required_hash[i]){
				    										
				    										async_result = DMCheckerRequestListener.OP_FAILURE;
				    										
				    										break;
				    									}
				    								}
												}
					    					}finally{
					    						
					    						try{
						    						f_buffer.returnToPool();
	
						    						result_handler.processResult( 
						    								pieceNumber, 
															async_result,
															user_data );
						    						
					    						}finally{
					    							
					    							try{
					    								this_mon.enter();
					    							
					    								async_checks--;
					    								
					    								if ( stopped ){
					    									  
					    									async_check_sem.release();
					    								}
					    							}finally{
					    								
					    								this_mon.exit();
					    							}
					    						}
					    					}
					    				}
					    				
									},
									low_priorty );
						
					    	
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
    						buffer.returnToPool();
    						
    						result_handler.processResult( 
    								pieceNumber, 
    								DMCheckerRequestListener.OP_FAILURE,
									user_data );
						}
					}
					  
					public void 
					readFailed( 
						DiskManagerReadRequest 	request, 
						Throwable		 		cause )
					{
						complete();
						
						result_handler.processResult( pieceNumber, DMCheckerRequestListener.OP_FAILURE, user_data );
					}
					
					  protected void
					  complete()
					  {
						  try{
							  this_mon.enter();
							
							  async_reads--;
							  
							  if ( stopped ){
								  
								  async_read_sem.release();
							  }
						  }finally{
							  
							  this_mon.exit();
						  }
					  }
				});
				
		}catch( Throwable e ){
			
			disk_manager.setFailed( "Piece check error - " + Debug.getNestedExceptionMessage(e));
			
			Debug.printStackTrace( e );
			
			result_handler.processResult( pieceNumber, DMCheckerRequestListener.OP_FAILURE, user_data );
		}
	}	 
}
