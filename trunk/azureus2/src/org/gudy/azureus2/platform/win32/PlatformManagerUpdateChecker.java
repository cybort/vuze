/*
 * Created on 07-May-2004
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

package org.gudy.azureus2.platform.win32;

/**
 * @author parg
 *
 */

import java.io.*;
import java.util.*;
import java.net.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.*;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.pluginsimpl.local.update.*;

public class 
PlatformManagerUpdateChecker
	implements UpdatableComponent
{
	public static final int	RD_SIZE_RETRIES	= 3;
	public static final int	RD_SIZE_TIMEOUT	= 10000;

	public static final String	UPDATE_DIR				= Constants.SF_WEB_SITE + "update/";
	public static final String	UPDATE_PROPERTIES_FILE	= UPDATE_DIR + PlatformManagerImpl.DLL_NAME + ".dll.properties";
	
	
	protected PlatformManagerImpl		platform;
	
	protected
	PlatformManagerUpdateChecker(
		PlatformManagerImpl		_platform )
	{
		platform	= _platform;
		
		UpdateManagerImpl.getSingleton().registerUpdatableComponent( this, false );
	}
	
	public void
	checkForUpdate(
		final UpdateChecker	checker )
	{
		String	current_dll_version = platform==null?"1.0":platform.getVersion();
		
		LGLogger.log( "PlatformManager:Win32 update check starts: current = " + current_dll_version );
		
		try{
			ResourceDownloaderFactory rdf = ResourceDownloaderFactoryImpl.getSingleton();
			
			ResourceDownloader	rd = rdf.create( new URL(UPDATE_PROPERTIES_FILE));

			rd	= rdf.getRetryDownloader( rd, 5 );
			
			String  current_az_version 	= Constants.getBaseVersion();
			boolean current_az_is_cvs	= Constants.isCVSVersion();
			
			InputStream is = rd.download();
			
			Properties	props = new Properties();
			
			props.load( is );
			
			String	target_dll_version	= null;
			
			Iterator it = props.keySet().iterator();
			
				// find the most recent version
			
			while( it.hasNext()){
				
				String	this_az_version 	= (String)it.next();
				String	this_dll_version	= (String)props.get(this_az_version);
				
				String	this_base 	= Constants.getBaseVersion( this_az_version );
				boolean	this_cvs	= Constants.isCVSVersion( this_az_version );
				
				if ( current_az_is_cvs != this_cvs ){
					
					continue;
				}
				
				if ( Constants.compareVersions( this_base, current_az_version ) == 0 ){
					
					if ( Constants.compareVersions( this_dll_version, current_dll_version ) > 0 ){
						
						target_dll_version	= this_dll_version;
						
						break;
					}
				}
			}
		
			LGLogger.log( "PlatformManager:Win32 update required = " + (target_dll_version!=null));
			
			if ( target_dll_version != null ){
				
				String	target = UPDATE_DIR + PlatformManagerImpl.DLL_NAME + "_" + target_dll_version + ".dll";
				
				ResourceDownloader dll_rd = rdf.create( new URL( target ));
			
					// get size here so it is cached
				
				rdf.getTimeoutDownloader(rdf.getRetryDownloader(dll_rd,RD_SIZE_RETRIES),RD_SIZE_TIMEOUT).getSize();

				final String f_target_dll_version	= target_dll_version;
				
				dll_rd.addListener( 
						new ResourceDownloaderListener()
						{
							public void
							reportPercentComplete(
								ResourceDownloader	downloader,
								int					percentage )
							{								
							}
							
							public void
							reportActivity(
								ResourceDownloader	downloader,
								String				activity )
							{	
							}
								
							public boolean
							completed(
								final ResourceDownloader	downloader,
								InputStream					data )
							{	
								installUpdate( checker, downloader, f_target_dll_version, data );
									
								return( true );
							}
							
							public void
							failed(
								ResourceDownloader			downloader,
								ResourceDownloaderException e )
							{
							}
							
						});

				checker.addUpdate(
						"Windows native support: " + PlatformManagerImpl.DLL_NAME + ".dll",
						new String[]{"This DLL supports native operations such as file-associations" },
						target_dll_version,
						dll_rd,
						Update.RESTART_REQUIRED_YES );
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			checker.failed();
			
		}finally{
			
			checker.completed();
		}
	}
	
	protected void
	installUpdate(
		UpdateChecker		checker,
		ResourceDownloader	rd,
		String				version,
		InputStream			data )
	{
		try{
			String	temp_dll_name 	= PlatformManagerImpl.DLL_NAME + "_" + version + ".dll";
			String	target_dll_name	= PlatformManagerImpl.DLL_NAME + ".dll";
			
			
			UpdateInstaller	installer = checker.createInstaller();
			
			installer.addResource( temp_dll_name, data );
			
			installer.addMoveAction( 
					temp_dll_name,
					installer.getInstallDir() + File.separator + target_dll_name );
			
		}catch( Throwable e ){
			
			rd.reportActivity("Update install failed:" + e.getMessage());
		}
	}
}
