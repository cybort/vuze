/*
 * Created on Jul 9, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package org.gudy.azureus2.pluginsimpl.local.download;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadStub;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;

import com.aelitis.azureus.util.MapUtils;

public class 
DownloadStubImpl
	implements DownloadStub
{
	private final DownloadManagerImpl		manager;
	private final String					name;
	private final byte[]					hash;
	private final DownloadStubFileImpl[]	files;
	private final Map						gm_map;
	
	private boolean					exported;
	private Map						attributes;
	
	protected
	DownloadStubImpl(
		DownloadManagerImpl		_manager,
		String					_name,
		byte[]					_hash,
		DownloadStubFile[]		_files,
		Map						_gm_map )
	{
		manager		= _manager;
		name		= _name;
		hash		= _hash;

		gm_map		= _gm_map;
		
		files		= new DownloadStubFileImpl[_files.length];
		
		for ( int i=0;i<files.length;i++){
			
			files[i] = new DownloadStubFileImpl( _files[i] );
		}
	}
	
	protected
	DownloadStubImpl(
		DownloadManagerImpl		_manager,
		Map						_map )
	{
		manager		= _manager;
		
		hash = (byte[])_map.get( "hash" );
		
		name	= MapUtils.getMapString( _map, "name", null );
		
		gm_map = (Map)_map.get( "gm" );
		
		List	file_list = (List)_map.get( "files" );
		
		if ( file_list == null ){
			
			files = new DownloadStubFileImpl[0];
			
		}else{
			
			files = new DownloadStubFileImpl[file_list.size()];
			
			for ( int i=0;i<files.length;i++){
				
				files[i] = new DownloadStubFileImpl((Map)file_list.get(i));
			}
		}
		
		attributes = (Map)_map.get( "attr" );
	}
	
	public Map
	exportToMap()
	{
		Map	map = new HashMap();
		
		map.put( "hash", hash );
		
		MapUtils.setMapString(map, "name", name );
		
		map.put( "gm", gm_map );
		
		if ( attributes != null ){
		
			map.put( "attr", attributes );
		}
		
		exported = true;
		
		return( map );
	}
	
	public boolean
	isStub()
	{
		return( true );
	}
	
	public Download
	destubbify()
	
		throws DownloadException
	{
		return( manager.destubbify( this ));
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	public byte[]
	getTorrentHash()
	{
		return( hash );
	}
	
	public DownloadStubFile[]
	getStubFiles()
	{
		return( files );
	}
	
	public long 
	getLongAttribute(
		TorrentAttribute 	attribute )
	{
		if ( attributes == null ){
			
			return( 0 );
		}
		
		Long l = (Long)attributes.get( attribute.getName());
		
		if ( l == null ){
			
			return( 0 );
		}
		
		return( l );
	}
	
	  
	public void 
	setLongAttribute(
		TorrentAttribute 	attribute, 
		long 				value)
	{
		if ( exported ){
			
			Debug.out( "Not supported!" );
			
			return;
		}
		
		if ( attributes == null ){
			
			attributes = new HashMap();
		}
		
		attributes.put( attribute.getName(), value );
	}
	
	public Map
	getGMMap()
	{
		return( gm_map );
	}
	
	public void
	remove()
	{
		manager.remove( this );
	}
	
	protected static class
	DownloadStubFileImpl
		implements DownloadStubFile
	{
		private final File		file;
		private final long		length;
		
		protected
		DownloadStubFileImpl(
			DownloadStubFile	stub_file )
		{
			file	= stub_file.getFile();
			length	= stub_file.getLength();
		}
		
		protected
		DownloadStubFileImpl(
			Map		map )
		{
			file 	= new File( MapUtils.getMapString(map, "file", null ));
			
			length 	= (Long)map.get( "len" );
		}
		
		protected Map
		exportToMap()
		{
			Map	map = new HashMap();

			map.put( "file", file.getAbsolutePath());
			map.put( "len", length );
			
			return( map );
		}
		
		public File
		getFile()
		{
			return( file );
		}
		
		public long
		getLength()
		{
			return( length );
		}
	}
}
