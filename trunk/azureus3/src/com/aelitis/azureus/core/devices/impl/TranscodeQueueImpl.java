/*
 * Created on Feb 6, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.devices.impl;

import java.io.File;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;

import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.util.CopyOnWriteList;

public class 
TranscodeQueueImpl 	
	implements TranscodeQueue
{
	private static final String	CONFIG_FILE 			= "xcodejobs.config";

	private TranscodeManagerImpl		manager;
	
	private List<TranscodeJobImpl>		queue		= new ArrayList<TranscodeJobImpl>();
	private AESemaphore 				queue_sem 	= new AESemaphore( "XcodeQ" );
	private AEThread2					queue_thread;
	
	private CopyOnWriteList<TranscodeQueueListener>	listeners = new CopyOnWriteList<TranscodeQueueListener>();
	
	private volatile boolean 	paused;
	private volatile int		max_bytes_per_sec;
	
	private volatile boolean	config_dirty;
	
	protected
	TranscodeQueueImpl(
		TranscodeManagerImpl	_manager )
	{
		manager = _manager;
	}
	
	protected void
	initialise()
	{	
		loadConfig();
		
		COConfigurationManager.addAndFireParameterListeners(
			new String[]{
				"xcode.queue.paused",
			},
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String		name )
				{
					paused				= COConfigurationManager.getBooleanParameter( "xcode.queue.paused", false );
					max_bytes_per_sec	= COConfigurationManager.getIntParameter( "xcode.queue.maxbps", 0 );
				}
			});
		
		schedule();
	}
	
	protected boolean
	process(
		final TranscodeJobImpl		job )
	{		
		TranscodeProvider provider = job.getProfile().getProvider();
		
		try{
			job.starts();
			
			final AESemaphore sem = new AESemaphore( "Xcode:proc" );
			
			final TranscodeProviderJob[] provider_job = { null }; 
			
			final Throwable[] error = { null };

			
			TranscodeProviderAdapter adapter = 
				new TranscodeProviderAdapter()
				{
					public void
					updatePercentDone(
						int								percent )
					{
						TranscodeProviderJob	prov_job = provider_job[0];
						
						if ( prov_job == null ){
							
							return;
						}
						
						int	job_state = job.getState();
						
						if ( job_state == TranscodeJob.ST_CANCELLED ){
															
							prov_job.cancel();
							
						}else if ( paused || job_state == TranscodeJob.ST_PAUSED ){
								
							prov_job.pause();
							
						}else{
							
							if ( job_state == TranscodeJob.ST_RUNNING ){
								
								prov_job.resume();
							}
							
							job.setPercentDone( percent );
							
							prov_job.setMaxBytesPerSecond( max_bytes_per_sec );
						}
					}
					
					public void
					failed(
						TranscodeProviderException		e )
					{
						error[0] = e;
						
						sem.release();
					}
					
					public void 
					complete() 
					{
						sem.release();
					}
				};
				
			TranscodeProfile profile = job.getProfile();
				
			String ext = profile.getFileExtension();

			DiskManagerFileInfo	file = job.getFile();
			
			String	target_file = file.getFile().getName();
			
			if ( ext != null ){
				
				int	pos = target_file.lastIndexOf( '.' );
				
				if ( pos != -1 ){
					
					target_file = target_file.substring( 0, pos ); 
				}
				
				target_file += ext;
			}
			
			File output_file = job.getTarget().getWorkingDirectory();
			
			output_file = new File( output_file.getAbsoluteFile(), target_file );
					
			provider_job[0] = 
				provider.transcode(
					adapter,
					job.getFile(),
					profile,
					output_file.toURI().toURL());
			
			provider_job[0].setMaxBytesPerSecond( max_bytes_per_sec );
			
			TranscodeQueueListener listener = 
				new TranscodeQueueListener()
				{
					public void
					jobAdded(
						TranscodeJob		job )
					{					
					}
					
					public void
					jobChanged(
						TranscodeJob		changed_job )
					{
						if ( changed_job == job ){
							
							int	state = job.getState();
							
							if ( state == TranscodeJob.ST_PAUSED ){
								
								provider_job[0].pause();
								
							}else if ( state == TranscodeJob.ST_RUNNING ){
									
								provider_job[0].resume();
								
							}else if ( 	state == TranscodeJob.ST_CANCELLED ||
										state == TranscodeJob.ST_STOPPED ){
							
								provider_job[0].cancel();
							}
						}
					}
					
					public void
					jobRemoved(
						TranscodeJob		removed_job )
					{	
						if ( removed_job == job ){
							
							provider_job[0].cancel();
						}
					}
				};
				
			try{
				addListener( listener );
			
				sem.reserve();
				
			}finally{
				
				removeListener( listener );
			}
			
			if ( error[0] != null ){
				
				throw( error[0] );
			}

			job.complete();
			
			return( true );
			
		}catch( Throwable e ){
			
			job.failed( e );
			
			return( false );
		}
	}
	
	protected void
	schedule()
	{
		synchronized( this ){

			if ( queue.size() > 0 && queue_thread == null ){
				
				queue_thread = new
					AEThread2( "XcodeQ", true )
					{
						public void 
						run() 
						{
							while( true ){
								
								boolean got = queue_sem.reserve( 30*1000 );
									
								TranscodeJobImpl	job = null;
								
								synchronized( TranscodeQueueImpl.this ){
									
									if ( !got ){
										
										if ( queue.size() == 0 ){
											
											queue_thread = null;
											
											return;
										}
										
										continue;
									}
									
									for ( TranscodeJobImpl j: queue ){
										
										int state = j.getState();
										
											// pick up any existing paused ones
										
										if ( state == TranscodeJob.ST_PAUSED ){

											job = j;
											
										}else if ( state == TranscodeJob.ST_QUEUED ){
											
											if ( job == null ){
											
												job = j;
											}
										}
									}
								}
								
								if ( job != null ){
								
									if ( process( job )){
									
										remove( job );
									}
								}	
							}
						}
					};
					
				queue_thread.start();
			}
		}
	}
	
	public TranscodeJobImpl
	add(
		TranscodeTarget			target,
		TranscodeProfile		profile,
		DiskManagerFileInfo		file,
		boolean					stream )
	{
		TranscodeJobImpl job = new TranscodeJobImpl( this, target, profile, file, stream );
		
		synchronized( this ){
			
			queue.add( job );
			
			queue_sem.release();
			
			saveConfig();
		}
		
		for ( TranscodeQueueListener listener: listeners ){
			
			try{
				listener.jobAdded( job );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
		
		schedule();
		
		return( job );
	}
	
	protected void
	remove(
		TranscodeJobImpl		job )
	{
		synchronized( this ){
			
			if ( !queue.remove( job )){
				
				return;
			}
			
			saveConfig();
		}

		for ( TranscodeQueueListener listener: listeners ){
			
			try{
				listener.jobRemoved( job );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
		
		schedule();
	}
	
	protected void
	jobChanged(
		TranscodeJob			job,
		boolean					schedule,
		boolean					persistable )
	{

		for ( TranscodeQueueListener listener: listeners ){
			
			try{
				listener.jobChanged( job );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
		
		if ( persistable ){
		
			configDirty();
		}
		
		if ( schedule ){
			
			queue_sem.release();
			
			schedule();
		}
	}
	
	protected int
	getIndex(
		TranscodeJobImpl		job )
	{
		return( queue.indexOf(job)+1);
	}
	
	public TranscodeJob[]
	getJobs()
	{
		synchronized( queue ){

			return( queue.toArray( new TranscodeJob[queue.size()]));
		}
	}
	
	public void 
	moveUp(
		TranscodeJobImpl	job )
	{
		TranscodeJob[] updated;
			
		synchronized( queue ){
		
			int index = queue.indexOf( job );
			
			if ( index <= 0 || queue.size() == 1 ){
				
				return;
			}
			
			queue.remove( job );
			
			queue.add( index-1, job );
			
			updated = getJobs();
		}
		
		for ( TranscodeJob j: updated ){
			
			jobChanged( j, false, true );
		}
	}
	
	public void 
	moveDown(
		TranscodeJobImpl	job )
	{
		TranscodeJob[] updated;

		synchronized( queue ){
		
		int index = queue.indexOf( job );
			
			if ( index < 0 || index == queue.size() - 1 ){
				
				return;
			}
			
			queue.remove( job );
			
			queue.add( index+1, job );
			
			updated = getJobs();
		}
		
		for ( TranscodeJob j: updated ){
			
			jobChanged( j, false, true );
		}
	}
	
	public void
	pause()
	{
		if ( !paused ){
			
			if ( paused ){
				
				COConfigurationManager.setParameter( "xcode.queue.paused", true );
			}
		}
	}
	
	public boolean
	isPaused()
	{
		return( paused );
	}
	
	public void
	resume()
	{
		if ( paused ){
			
			COConfigurationManager.setParameter( "xcode.queue.paused", false );
		}
	}
	
	public long
	getMaxBytesPerSecond()
	{
		return( max_bytes_per_sec );
	}
	
	public void
	setMaxBytesPerSecond(
		long		max )
	{
		COConfigurationManager.setParameter( "xcode.queue.maxbps", max );
	}
	
	protected TranscodeTarget
	lookupTarget(
		String		target_id )
	
		throws TranscodeProviderException
	{
		return( manager.lookupTarget( target_id ));
	}
	
	protected TranscodeProfile
	lookupProfile(
		String		profile_id )
	
		throws TranscodeProviderException
	{
		TranscodeProfile profile = manager.getProfileFromUID( profile_id );
		
		if ( profile == null ){
			
			throw( new TranscodeProviderException( "Transcode profile with id '" + profile_id + "' not found" ));
		}
		
		return( profile );
	}
	
	protected DiskManagerFileInfo
	lookupFile(
		byte[]		hash,
		int			index )
	
		throws TranscodeProviderException
	{
		return( manager.lookupFile( hash, index ));
	}
	
	protected void
	configDirty()
	{
		synchronized( this ){
			
			if ( config_dirty ){
				
				return;
			}
			
			config_dirty = true;
		
			new DelayedEvent( 
				"TranscodeQueue:save", 5000,
				new AERunnable()
				{
					public void 
					runSupport() 
					{
						synchronized( TranscodeQueueImpl.this ){
							
							if ( !config_dirty ){

								return;
							}
							
							saveConfig();
						}	
					}
				});
		}
	}
	
	protected void
	loadConfig()
	{
		if ( !FileUtil.resilientConfigFileExists( CONFIG_FILE )){
			
			return;
		}
		
		log( "Loading configuration" );
				
		synchronized( this ){
			
			Map map = FileUtil.readResilientConfigFile( CONFIG_FILE );
			
			List<Map>	l_jobs = (List<Map>)map.get( "jobs" );
			
			for ( Map m: l_jobs ){
				
				try{
					TranscodeJobImpl job = new TranscodeJobImpl( this, m );
				
					queue.add(job );
					
					queue_sem.release();
					
				}catch( Throwable e ){
					
					log( "Failed to restore job: " + m, e );
				}
			}
		}
	}
	
	protected void
	saveConfig()
	{
		synchronized( this ){

			config_dirty = false;
			
			if ( queue.size() == 0 ){

				FileUtil.deleteResilientConfigFile( CONFIG_FILE );

			}else{
				
				Map	map = new HashMap();
				
				List	l_jobs = new ArrayList();
				
				map.put( "jobs", l_jobs );
				
				for ( TranscodeJobImpl job: queue ){
				
					if ( job.isStream()){
						
						continue;
					}
					
					try{
					
						l_jobs.add( job.toMap());
						
					}catch( Throwable e ){
						
						log( "Failed to save job", e );
					}
				}
				
				FileUtil.writeResilientConfigFile( CONFIG_FILE, map );
			}
		}
	}
	
	protected void
	close()
	{
		if ( config_dirty ){
		
			saveConfig();
		}
	}
	
	public void
	addListener(
		TranscodeQueueListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		TranscodeQueueListener		listener )
	{
		listeners.remove( listener );	
	}
	
	protected void
	log( 
		String	str )
	{
		manager.log( "Queue: " + str );
	}
	
	protected void
	log( 
		String		str,
		Throwable	e )
	{
		manager.log( "Queue: " + str, e );
	}
}
