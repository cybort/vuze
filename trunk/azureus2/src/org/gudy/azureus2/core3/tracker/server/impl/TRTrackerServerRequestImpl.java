/*
 * File    : TRTrackerServerRequestImpl.java
 * Created : 13-Dec-2003
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

package org.gudy.azureus2.core3.tracker.server.impl;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.tracker.server.*;

public class 
TRTrackerServerRequestImpl
	implements TRTrackerServerRequest
{
	protected TRTrackerServerImpl			server;
	protected TRTrackerServerPeerImpl	peer;
	protected TRTrackerServerTorrentImpl	torrent;
	protected int							type;
	protected Map							response;
	
	public
	TRTrackerServerRequestImpl(
		TRTrackerServerImpl				_server,
		TRTrackerServerPeerImpl			_peer,
		TRTrackerServerTorrentImpl		_torrent,
		int								_type,
		Map								_response )
	{
		server		= _server;
		peer		= _peer;
		torrent		= _torrent;
		type		= _type;
		response	= _response;
	}
	
	public int
	getType()
	{
		return( type );
	}
	
	public TRTrackerServerPeer
	getPeer()
	{
		return( peer );
	}
	
	public TRTrackerServerTorrent
	getTorrent()
	{
		return( torrent );	
	}
	
	public Map
	getResponse()
	{
		return( response );
	}
}
