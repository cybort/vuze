/* Written and copyright 2001-2003 Tobias Minich.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 *
 * Main.java
 *
 * Created on 22. August 2003, 00:04
 */

package org.gudy.azureus2.ui.web;

import java.util.Properties;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.internat.ILocaleUtilChooser;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.ui.common.util.LocaleUtilHeadless;
import org.gudy.azureus2.ui.console.ConsoleInput;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

/**
 *
 * @author  Tobias Minich
 */
public class Main implements ILocaleUtilChooser {
  
  Jhttpp2Server server;
  GlobalManager gm;
  ConsoleInput ci;
  
  /** Creates a new instance of Main */
  public Main(String args[]) {
    LocaleUtil.setLocaleUtilChooser(this);
    Properties p = new Properties(System.getProperties());
    p.put("java.awt.headless", "true");
    System.setProperties(p);
    gm = GlobalManagerFactory.create();
    PluginInitializer.getSingleton(gm,null).initializePlugins( PluginManager.UI_NONE );  
  
    server = new Jhttpp2Server(gm, true);
    ci = new ConsoleInput("Main", gm, System.in, System.out, Boolean.TRUE);
    org.gudy.azureus2.ui.common.Main.initRootLogger();
    new Thread(server, "Webinterface Server").start();
    System.out.println("Running on port " + COConfigurationManager.getIntParameter("Server_iPort"));
    
    new Thread("Plugin Init Complete")
    {
    	public void
    	run()
    	{
    		PluginInitializer.initialisationComplete();
    	}
    }.start();  
  }
  
  public static void main(String args[]) {
    new Main(args);
  }
  
  public org.gudy.azureus2.core3.internat.LocaleUtil getProperLocaleUtil() {
    return new LocaleUtilHeadless();
  }
  
}
