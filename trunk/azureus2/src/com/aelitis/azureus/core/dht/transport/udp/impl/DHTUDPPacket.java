/*
 * Created on 21-Jan-2005
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

package com.aelitis.azureus.core.dht.transport.udp.impl;

import java.io.*;
import java.util.*;

import org.gudy.azureus2.plugins.logging.LoggerChannel;


import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.net.udp.*;


/**
 * @author parg
 *
 */

public class 
DHTUDPPacket 
{
	public static final byte	VERSION					= DHTTransportUDP.PROTOCOL_VERSION;
	
		// these actions have to co-exist with the tracker ones when the connection
		// is shared, hence 1024
	
	public static final int		ACT_REQUEST_PING		= 1024;
	public static final int		ACT_REPLY_PING			= 1025;
	public static final int		ACT_REQUEST_STORE		= 1026;
	public static final int		ACT_REPLY_STORE			= 1027;
	public static final int		ACT_REQUEST_FIND_NODE	= 1028;
	public static final int		ACT_REPLY_FIND_NODE		= 1029;
	public static final int		ACT_REQUEST_FIND_VALUE	= 1030;
	public static final int		ACT_REPLY_FIND_VALUE	= 1031;
	public static final int		ACT_REPLY_ERROR			= 1032;
	public static final int		ACT_REPLY_STATS			= 1033;
	public static final int		ACT_REQUEST_STATS		= 1034;
	
	
	private static boolean	registered				= false;
	private static boolean	version_fail_reported	= false;
	
	private static LoggerChannel	logger;
	
	protected static void
	registerCodecs(
		LoggerChannel	_logger )
	{
		if ( registered ){
			
			return;
		}
	
		registered	= true;
		
		logger		= _logger;
			
		PRUDPPacketRequestDecoder	request_decoder =
			new PRUDPPacketRequestDecoder()
			{
				public PRUDPPacketRequest
				decode(
					PRUDPPacketHandler	handler,
					DataInputStream		is,
					long				connection_id,
					int					action,
					int					transaction_id )
				
					throws IOException
				{
					DHTTransportUDPImpl	transport = (DHTTransportUDPImpl)handler.getRequestHandler();

					switch( action ){
						case ACT_REQUEST_PING:
						{
							return( new DHTUDPPacketRequestPing(is, connection_id,transaction_id));
						}
						case ACT_REQUEST_STORE:
						{
							return( new DHTUDPPacketRequestStore(transport,is, connection_id,transaction_id));
						}
						case ACT_REQUEST_FIND_NODE:
						{
							return( new DHTUDPPacketRequestFindNode(is, connection_id,transaction_id));
						}
						case ACT_REQUEST_FIND_VALUE:
						{
							return( new DHTUDPPacketRequestFindValue(is, connection_id,transaction_id));
						}
						case ACT_REQUEST_STATS:
						{
							return( new DHTUDPPacketRequestStats(is, connection_id, transaction_id));
						}
						default:
						{
							throw( new IOException( "Unknown action type" ));
						}
					}
				}
			};
			
		Map	request_decoders = new HashMap();
		
		request_decoders.put( new Integer( ACT_REQUEST_PING ), request_decoder );
		request_decoders.put( new Integer( ACT_REQUEST_STORE ), request_decoder );
		request_decoders.put( new Integer( ACT_REQUEST_FIND_NODE ), request_decoder );
		request_decoders.put( new Integer( ACT_REQUEST_FIND_VALUE ), request_decoder );
		request_decoders.put( new Integer( ACT_REQUEST_STATS ), request_decoder );
		
		PRUDPPacketRequest.registerDecoders( request_decoders );	
			
		
		
		PRUDPPacketReplyDecoder	reply_decoder =
			new PRUDPPacketReplyDecoder()
			{
				public PRUDPPacketReply
				decode(
					PRUDPPacketHandler	handler,
					DataInputStream		is,
					int					action,
					int					transaction_id )
				
					throws IOException
				{
					DHTTransportUDPImpl	transport = (DHTTransportUDPImpl)handler.getRequestHandler();
					
					switch( action ){
					
						case ACT_REPLY_PING:
						{
							return( new DHTUDPPacketReplyPing(is, transaction_id));
						}
						case ACT_REPLY_STORE:
						{
							return( new DHTUDPPacketReplyStore(is, transaction_id));
						}
						case ACT_REPLY_FIND_NODE:
						{
							return( new DHTUDPPacketReplyFindNode(transport, is, transaction_id));
						}
						case ACT_REPLY_FIND_VALUE:
						{
							return( new DHTUDPPacketReplyFindValue(transport, is, transaction_id));
						}
						case ACT_REPLY_ERROR:
						{
							return( new DHTUDPPacketReplyError( is, transaction_id));
						}
						case ACT_REPLY_STATS:
						{
							return( new DHTUDPPacketReplyStats( is, transaction_id));
						}
						default:
						{
							throw( new IOException( "Unknown action type" ));
						}
					}
				}
			};
			
		Map	reply_decoders = new HashMap();
		
		reply_decoders.put( new Integer( ACT_REPLY_PING ), reply_decoder );
		reply_decoders.put( new Integer( ACT_REPLY_STORE ), reply_decoder );
		reply_decoders.put( new Integer( ACT_REPLY_FIND_NODE ), reply_decoder );
		reply_decoders.put( new Integer( ACT_REPLY_FIND_VALUE ), reply_decoder );
		reply_decoders.put( new Integer( ACT_REPLY_ERROR ), reply_decoder );
		reply_decoders.put( new Integer( ACT_REPLY_STATS ), reply_decoder );
		
		PRUDPPacketReply.registerDecoders( reply_decoders );
	}
	
	protected static void
	checkVersion(
		int		version )
	
		throws IOException
	{
		if ( version != DHTUDPPacket.VERSION ){
		
			if ( version > DHTUDPPacket.VERSION ){
				
				synchronized( DHTUDPPacket.class ){
			
					if ( !version_fail_reported ){
						
						version_fail_reported	= true;
						
						logger.log(
							"DHT protocol version is too old, please update Azureus" );
					}
				}
			}
			
			throw( new IOException( "Invalid DHT protocol version, please update Azureus" ));
		}
	}
}
