/*
 * File    : WebPlugin.java
 * Created : 23-Jan-2004
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

package org.gudy.azureus2.ui.webplugin;

/**
 * @author parg
 *
 */

import java.io.*;
import java.util.*;
//import java.net.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;


import HTML.Template;

public class 
WebPlugin
	implements Plugin, TrackerWebPageGenerator
{
	public static final String DEFAULT_PORT		= "8089";
	public static final String DEFAULT_PROTOCOL	= "HTTP";
	
	protected static final String	NL			= "\r\n";
	
	protected static final String[]		welcome_pages = {"index.html", "index.htm", "index.php", "index.tmpl" };
	protected static File[]				welcome_files;
	
	protected PluginInterface		plugin_interface;
	protected LoggerChannel			log;
	protected Tracker				tracker;
	
	protected String				home_page;
	protected String				file_root;
	protected String				resource_root;
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	
		throws PluginException
	{	
		plugin_interface	= _plugin_interface;
		
		log = plugin_interface.getLogger().getChannel("WebPlugin");
		
		tracker = plugin_interface.getTracker();
	
		Properties	props = plugin_interface.getPluginProperties();
		
		home_page = (String)props.get("homepage");
		
		if ( home_page != null ){
			
			home_page = home_page.trim();
			
			if ( home_page.length() == 0 ){
				
				home_page = null;
				
			}else if (!home_page.startsWith("/" )){
			
				home_page = "/" + home_page;
			}
		}
		
		resource_root = (String)props.get("rootresource");
		
		if ( resource_root != null ){
			
			resource_root = resource_root.trim();
			
			if ( resource_root.length() == 0 ){
				
				resource_root = null;
				
			}else if ( resource_root.startsWith("/" )){
			
				resource_root = resource_root.substring(1);
			}
		}
		
		String	root_dir	= (String)props.get("rootdir");
		
		if ( root_dir != null ){
			
			root_dir = root_dir.trim();
		}
		
		if ( root_dir == null || root_dir.length() == 0 ){
			
			file_root = plugin_interface.getPluginDirectoryName();
			
			if ( file_root == null ){
				
				file_root = FileUtil.getApplicationPath() + "web";
			}
		}else{
			
				// absolute or relative
			
			if ( root_dir.startsWith(File.separator) || root_dir.indexOf(":") != -1 ){
				
				file_root = root_dir;
				
			}else{
				
				file_root = FileUtil.getApplicationPath() + "web" + File.separator + root_dir;
				
			}
		}

		File	f_root = new File( file_root );
		
		if ( !f_root.exists()){
	
			String	error = "WebPlugin: root dir '" + file_root + "' doesn't exist";
			
			log.log( LoggerChannel.LT_ERROR, error );
			
			throw( new PluginException( error ));
		}

		if ( !f_root.isDirectory()){
			
			String	error = "WebPlugin: root dir '" + file_root + "' isn't a directory";
			
			log.log( LoggerChannel.LT_ERROR, error );
			
			throw( new PluginException( error ));
		}
		
		welcome_files = new File[welcome_pages.length];
		
		for (int i=0;i<welcome_pages.length;i++){
			
			welcome_files[i] = new File( file_root + File.separator + welcome_pages[i] );
		}
		
					
		int port	= Integer.parseInt( props.getProperty( "port", DEFAULT_PORT ));

		String	protocol_str = props.getProperty( "protocol", DEFAULT_PROTOCOL );
		
		int	protocol = protocol_str.equalsIgnoreCase( "HTTP")?
							Tracker.PR_HTTP:Tracker.PR_HTTPS;
	
		log.log( LoggerChannel.LT_INFORMATION, "WebPlugin Initialisation: port = " + port + ", protocol = " + protocol_str + ", root = " + root_dir );
		
		try{
			TrackerWebContext	context = tracker.createWebContext( port, protocol );
		
			context.addPageGenerator( this );
			
		}catch( TrackerException e ){
			
			log.log( "Plugin Initialisation Fails", e );
		}
	}
	
	public boolean
	generate(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		OutputStream os = response.getOutputStream();
		
		String	url = request.getURL();
		
		if (url.equals("/")){
			
			if (home_page != null ){
				
				url = home_page;
				
			}else{
			
				for (int i=0;i<welcome_files.length;i++){
					
					if ( welcome_files[i].exists()){
						
						url = "/" + welcome_pages[i];
						
						break;
					}
				}	
			}
		}
	
			// first try file system for data
		
		if ( response.useFile( file_root, url )){
			
			return( true );
		}
		
				// now try jars		
			
		String	resource_name = url;
		
		if (resource_name.startsWith("/")){
			
			resource_name = resource_name.substring(1);
		}
			
		if ( resource_root != null && !resource_name.startsWith( resource_root )){
			
			resource_name = resource_root + "/" + resource_name;
		}
		
		int	pos = resource_name.lastIndexOf(".");
		
		if ( pos != -1 ){
			
			String	type = resource_name.substring( pos+1 );
		
			InputStream is = WebPlugin.class.getClassLoader().getResourceAsStream( resource_name );
		
			System.out.println( resource_name + "->" + is );
		
			if (is != null ){
			
				try{
					response.useStream( type, is );
				
				}finally{
				
					is.close();
				}
			
				return( true );
			}
		}
		
		return( false );
	}
}
