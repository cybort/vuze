/*
 * File    : PRUDPPacketReceiverFactoryImpl.java
 * Created : 20-Jan-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.aelitis.net.udp.uc.impl;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.net.udp.uc.PRUDPPacketHandler;
import com.aelitis.net.udp.uc.PRUDPReleasablePacketHandler;
import com.aelitis.net.udp.uc.PRUDPRequestHandler;

public class 
PRUDPPacketHandlerFactoryImpl 
{
	private static Map			receiver_map = new HashMap();
	private static AEMonitor	class_mon	= new AEMonitor( "PRUDPPHF" );
	private static Map			releasable_map = new HashMap();
	private static Set			non_releasable_set = new HashSet();
	

	public static PRUDPPacketHandler
	getHandler(
		int						port,
		PRUDPRequestHandler		request_handler)
	{
		final Integer	f_port = new Integer( port );

		try{
			class_mon.enter();
		
			non_releasable_set.add( f_port );
			
			PRUDPPacketHandlerImpl	receiver = (PRUDPPacketHandlerImpl)receiver_map.get( f_port );
			
			if ( receiver == null ){
				
				receiver = new PRUDPPacketHandlerImpl( port );
				
				receiver_map.put( f_port, receiver );
			}
			
				// only set the incoming request handler if one has been specified. This is important when
				// the port is shared (e.g. default udp tracker and dht) and only one usage has need to handle
				// unsolicited inbound requests as we don't want the tracker null handler to erase the dht's
				// one
			
			if ( request_handler != null ){
				
				receiver.setRequestHandler( request_handler );
			}
			
			return( receiver );
			
		}finally{
			
			class_mon.exit();
		}
	}	
	
	public static PRUDPReleasablePacketHandler
	getReleasableHandler(
		int						port,
		PRUDPRequestHandler		request_handler)
	{
		final Integer	f_port = new Integer( port );
		
		try{
			class_mon.enter();
		
			PRUDPPacketHandlerImpl	receiver = (PRUDPPacketHandlerImpl)receiver_map.get( f_port );
			
			if ( receiver == null ){
				
				receiver = new PRUDPPacketHandlerImpl( port );
				
				receiver_map.put( f_port, receiver );
			}
			
				// only set the incoming request handler if one has been specified. This is important when
				// the port is shared (e.g. default udp tracker and dht) and only one usage has need to handle
				// unsolicited inbound requests as we don't want the tracker null handler to erase the dht's
				// one
			
			if ( request_handler != null ){
				
				receiver.setRequestHandler( request_handler );
			}
			
			final PRUDPPacketHandlerImpl f_receiver = receiver;
			
			final PRUDPReleasablePacketHandler rel = 
				new PRUDPReleasablePacketHandler()
				{
					public PRUDPPacketHandler
					getHandler()
					{
						return( f_receiver );
					}
					
					public void
					release()
					{
						try{
							class_mon.enter();
							
							List l = (List)releasable_map.get( f_port );
							
							if ( l == null ){
								
								Debug.out( "hmm" );
								
							}else{
								
								if ( !l.remove( this )){
									
									Debug.out( "hmm" );
									
								}else{
									
									if ( l.size() == 0 ){
										
										if ( !non_releasable_set.contains( f_port )){
										
											f_receiver.destroy();
										}
										
										releasable_map.remove( f_port );
									}
								}
							}
						}finally{
							
							class_mon.exit();
						}
					}
				};
			
			List l = (List)releasable_map.get( f_port );
			
			if ( l == null ){
				
				l = new ArrayList();
				
				releasable_map.put( f_port, l );
			}
			
			l.add( rel );

			if ( l.size() > 1024 ){
				
				Debug.out( "things going wrong here" );
			}
			
			return( rel );
			
		}finally{
			
			class_mon.exit();
		}
	}
}
