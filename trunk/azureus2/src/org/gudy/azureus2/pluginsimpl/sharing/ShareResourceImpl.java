/*
 * File    : ShareResourceImpl.java
 * Created : 31-Dec-2003
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

package org.gudy.azureus2.pluginsimpl.sharing;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;

import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.core3.util.*;

public abstract class 
ShareResourceImpl
	implements ShareResource, Comparable
{
	protected static Md5Hasher	hasher = new Md5Hasher();
	
	protected ShareManagerImpl		manager;
	protected int					type;
	
	protected
	ShareResourceImpl(
		ShareManagerImpl	_manager,
		int					_type )
	{
		manager	= _manager;
		type 	= _type;
	}
	
	protected abstract void
	serialiseResource(
		Map		map );
	
	public int
	getType()
	{
		return( type );
	}
	
	
	public void
	delete()
	{
		manager.delete(this);
	}
	
	protected abstract void
	deleteInternal();
	
	protected byte[]
	getFingerPrint(
		File		file )
	
		throws ShareException
	{
		try{
			long	mod 	= file.lastModified();
			long	size	= file.length();
			
			String	finger_print = file.getName()+":"+mod+":"+size;
					
			return( hasher.calculateHash(finger_print.getBytes()));
			
		}catch( Throwable e ){
			
			throw( new ShareException( "ShareResource::getFingerPring: fails", e ));
		}
	}
	
	protected String
	getNewTorrentLocation()
	
		throws ShareException
	{
		return( manager.getNewTorrentLocation());
	}
	
	protected void
	writeTorrent(
		ShareItemImpl		item )
	
		throws ShareException
	{
		manager.writeTorrent( item );
	}
	
	protected void
	readTorrent(
		ShareItemImpl		item )
	
		throws ShareException
	{
		manager.readTorrent( item );
	}	
	
	protected void
	deleteTorrent(
		ShareItemImpl		item )
	{
		manager.deleteTorrent( item );
	}
	
	protected abstract void
	checkConsistency();
	
}
