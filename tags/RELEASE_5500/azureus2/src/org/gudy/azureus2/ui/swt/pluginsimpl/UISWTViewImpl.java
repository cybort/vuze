/*
 * File    : UISWTViewImpl.java
 * Created : Oct 14, 2005
 * By      : TuxPaper
 *
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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

package org.gudy.azureus2.ui.swt.pluginsimpl;

import java.awt.Frame;
import java.awt.Panel;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.UIRuntimeException;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.plugins.*;

import com.aelitis.azureus.ui.common.ToolBarEnabler;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.util.MapUtils;

/**
 * This class creates an IView that triggers UISWTViewEventListener 
 * appropriately
 * 
 * @author TuxPaper
 *
 */
public class UISWTViewImpl
	implements UISWTViewCore, AEDiagnosticsEvidenceGenerator
{
	public static final String CFG_PREFIX = "Views.plugins.";

	private PluginUISWTSkinObject skinObject;

	private final Object initialDatasource;
	
	private UISWTView	parentView;
	
	private Object dataSource 			= null;

	private boolean useCoreDataSource = false;

	private final UISWTViewEventListener eventListener;

	private Composite composite;

	private final String sViewID;

	private int iControlType = UISWTView.CONTROLTYPE_SWT;

	private boolean bFirstGetCompositeCall = true;

	//private final String sParentID;

	private String sTitle = null;

	private String lastFullTitleKey = null;

	private String lastFullTitle = "";

	//private Boolean hasFocus = null;

	private UIPluginViewToolBarListener toolbarListener;

	private volatile Map<Object,Object>	user_data;
	
	public UISWTViewImpl(String sParentID, String sViewID,
			UISWTViewEventListener eventListener, Object _initialDatasource)
			throws Exception {
		//this.sParentID = sParentID;
		this.sViewID = sViewID;
		initialDatasource = _initialDatasource;
		this.eventListener = eventListener;
		if (eventListener instanceof UISWTViewCoreEventListener) {
			useCoreDataSource = true;
		}

		AEDiagnostics.addEvidenceGenerator(this);

		if (initialDatasource != null) {
			triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, initialDatasource);
		}

			// we could pass the parentid as the data for the create call but unfortunately
			// there's a bunch of crap out there that assumes that data is the view object :(
		if (!eventListener.eventOccurred(new UISWTViewEventImpl(sParentID, this,
				UISWTViewEvent.TYPE_CREATE, this)))
			throw new UISWTViewEventCancelledException();

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#getEventListener()
	 */
	public UISWTViewEventListener getEventListener() {
		return eventListener;
	}

	// UISWTPluginView implementation
	// ==============================

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#getDataSource()
	 */
	public Object getInitialDataSource() {
		return initialDatasource;
	}
	
	public Object getDataSource() {
		return dataSource;
	}

	public void
	setParentView(
		UISWTView		p )
	{
		parentView = p;
	}
	
	public UISWTView
	getParentView()
	{
		return( parentView );
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginView#getViewID()
	 */
	public String getViewID() {
		return sViewID;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginView#closeView()
	 */
	public void closeView() {
		try {

			UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (uiFunctions != null) {
				uiFunctions.closePluginView(this);
			}
			
			Composite c = getComposite();
			
			if ( c != null && !c.isDisposed()){
			
				Composite parent = c.getParent();
				
				triggerEvent( UISWTViewEvent.TYPE_DESTROY, null );
				
				if ( parent instanceof CTabFolder ){
					
					for ( CTabItem item: ((CTabFolder)parent).getItems()){
						
						if ( item.getControl() == c ){
							
							item.dispose();
						}
					}
				}
			}
		} catch (Throwable e) {
			Debug.out(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#setControlType(int)
	 */
	public void setControlType(int iControlType) {
		if (iControlType == CONTROLTYPE_AWT || iControlType == CONTROLTYPE_SWT
				|| iControlType == CONTROLTYPE_SKINOBJECT)
			this.iControlType = iControlType;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#getControlType()
	 */
	public int getControlType() {
		return iControlType;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#triggerEvent(int, java.lang.Object)
	 */
	public void triggerEvent(int eventType, Object data) {
		// prevent double fire of focus gained/lost
		/* erm, this code doesn't do anything atm as nothing is setting hasFocus, removing for the moment
		if (eventType == UISWTViewEvent.TYPE_FOCUSGAINED && hasFocus != null
				&& hasFocus) {
			return;
		}
		if (eventType == UISWTViewEvent.TYPE_FOCUSLOST && hasFocus != null
				&& !hasFocus) {
			return;
		}
		*/
		if (eventType == UISWTViewEvent.TYPE_DATASOURCE_CHANGED) {
			Object newDataSource = PluginCoreUtils.convert(data, useCoreDataSource);
			if (dataSource == newDataSource) {
				return;
			}
			data = dataSource = newDataSource;
		} else if (eventType == UISWTViewEvent.TYPE_LANGUAGEUPDATE) {
			lastFullTitle = "";
			Messages.updateLanguageForControl(getComposite());
		} else if (eventType == UISWTViewEvent.TYPE_OBFUSCATE
				&& (eventListener instanceof ObfusticateImage)) {
			if (data instanceof Map) {
				((ObfusticateImage) eventListener).obfusticatedImage((Image) MapUtils.getMapObject(
						(Map) data, "image", null, Image.class));
			}
		}

		try {
			eventListener.eventOccurred(new UISWTViewEventImpl(null,this, eventType, data));
		} catch (Throwable t) {
			Debug.out("ViewID=" + sViewID + "; EventID=" + eventType + "; data="
					+ data, t);
			//throw (new UIRuntimeException("UISWTView.triggerEvent:: ViewID="
			//		+ sViewID + "; EventID=" + eventType + "; data=" + data, t));
		}
		
		if (eventType == UISWTViewEvent.TYPE_DESTROY) {
			Composite c = getComposite();
			if (c != null && !c.isDisposed()) {
				Composite parent = c.getParent();
				Utils.disposeComposite(c);
				Utils.relayout(parent);
			}
		}	
	}

	protected boolean triggerEventRaw(int eventType, Object data) {
		try {
			return eventListener.eventOccurred(new UISWTViewEventImpl(null,this,
					eventType, data));
		} catch (Throwable t) {
			throw (new UIRuntimeException("UISWTView.triggerEvent:: ViewID="
					+ sViewID + "; EventID=" + eventType + "; data=" + data, t));
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
		if ( title.contains( "." ) && MessageText.keyExists(title)){
				// it if appears to be a resource key then resolve it here
			title = MessageText.getString( title );
		}
		sTitle = title;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#getPluginInterface()
	 */
	public PluginInterface getPluginInterface() {
		if (eventListener instanceof UISWTViewEventListenerHolder) {
			return (((UISWTViewEventListenerHolder) eventListener).getPluginInterface());
		}

		return null;
	}

	
	// Core Functions

	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#getComposite()
	 */
	public Composite getComposite() {
		if (bFirstGetCompositeCall) {
			bFirstGetCompositeCall = false;
		}
		return composite;
	}


	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#getTitleID()
	 */
	public String getTitleID() {
		if (sTitle == null) {
			// still need this crappy check because some plugins still expect their
			// view id to be their name
			if (MessageText.keyExists(sViewID)) {
				return sViewID;
			}
			String id = CFG_PREFIX + sViewID + ".title";
			if (MessageText.keyExists(id)) {
				return id;
			}
			return "!" + sViewID + "!";
		}
		return "!" + sTitle + "!";
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#getFullTitle()
	 */
	public String getFullTitle() {
		//System.out.println("getFullTitle " + sTitle + ";" + getTitleID() + ";" + lastFullTitle + ";" + lastFullTitleKey);
		if (sTitle != null) {
			return sTitle;
		}

		String key = getTitleID();
		if (key == null) {
			return "";
		}

		if (lastFullTitle.length() > 0 && key.equals(lastFullTitleKey)) {
			return lastFullTitle;
		}

		lastFullTitleKey = key;

		if (MessageText.keyExists(key) || key.startsWith("!") && key.endsWith("!")) {
			lastFullTitle = MessageText.getString(key);
		} else {
			lastFullTitle = key.replace('.', ' '); // support old plugins
		}

		return lastFullTitle;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#initialize(org.eclipse.swt.widgets.Composite)
	 */
	public void initialize(Composite parent) {
		if (iControlType == UISWTView.CONTROLTYPE_SWT) {
			GridData gridData;
			Layout parentLayout = parent.getLayout();
			if (parentLayout instanceof FormLayout) {
				composite = parent;
			} else {
				composite = new Composite(parent, SWT.NULL);
				GridLayout layout = new GridLayout(1, false);
				layout.marginHeight = 0;
				layout.marginWidth = 0;
				composite.setLayout(layout);
				gridData = new GridData(GridData.FILL_BOTH);
				composite.setLayoutData(gridData);
			}

			triggerEvent(UISWTViewEvent.TYPE_INITIALIZE, composite);

			if (composite.getLayout() instanceof GridLayout) {
				// Force children to have GridData layoutdata.
				Control[] children = composite.getChildren();
				for (int i = 0; i < children.length; i++) {
					Control control = children[i];
					Object layoutData = control.getLayoutData();
					if (layoutData == null || !(layoutData instanceof GridData)) {
						if (layoutData != null)
							Logger.log(new LogEvent(LogIDs.PLUGIN, LogEvent.LT_WARNING,
									"Plugin View '" + sViewID + "' tried to setLayoutData of "
											+ control + " to a " + layoutData.getClass().getName()));

						if (children.length == 1)
							gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
						else
							gridData = new GridData();

						control.setLayoutData(gridData);
					}
				}
			}
		} else if (iControlType == UISWTView.CONTROLTYPE_AWT) {
			composite = new Composite(parent, SWT.EMBEDDED);
			FillLayout layout = new FillLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			composite.setLayout(layout);
			GridData gridData = new GridData(GridData.FILL_BOTH);
			composite.setLayoutData(gridData);

			Frame f = SWT_AWT.new_Frame(composite);

			Panel pan = new Panel();

			f.add(pan);

			triggerEvent(UISWTViewEvent.TYPE_INITIALIZE, pan);
		} else if (iControlType == UISWTViewCore.CONTROLTYPE_SKINOBJECT) {
			triggerEvent(UISWTViewEvent.TYPE_INITIALIZE, getSkinObject());
		}
	}

	/**
	 * @return
	 */
	public boolean requestClose() {
		return triggerEventRaw(UISWTViewEvent.TYPE_CLOSE, null);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#useCoreDataSource()
	 */
	public boolean useCoreDataSource() {
		return useCoreDataSource;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#setUseCoreDataSource(boolean)
	 */
	public void setUseCoreDataSource(boolean useCoreDataSource) {
		if (this.useCoreDataSource == useCoreDataSource) {
			return;
		}

		this.useCoreDataSource = useCoreDataSource;
		triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, dataSource);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#getSkinObject()
	 */
	public PluginUISWTSkinObject getSkinObject() {
		return skinObject;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#setSkinObject(org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject, org.eclipse.swt.widgets.Composite)
	 */
	public void setSkinObject(PluginUISWTSkinObject skinObject, Composite c) {
		this.skinObject = skinObject;
		this.composite = c;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator#generate(org.gudy.azureus2.core3.util.IndentWriter)
	 */
	public void generate(IndentWriter writer) {
		if (eventListener instanceof AEDiagnosticsEvidenceGenerator) {
			writer.println("View: " + sViewID + ": " + sTitle);

			try {
				writer.indent();

				((AEDiagnosticsEvidenceGenerator) eventListener).generate(writer);
			} catch (Exception e) {

			} finally {

				writer.exdent();
			}
		} else {
			writer.println("View (no generator): " + sViewID + ": " + sTitle);
		}
	}

	public boolean toolBarItemActivated(ToolBarItem item, long activationType, Object datasource) {
		if (toolbarListener != null) {
			return toolbarListener.toolBarItemActivated(item, activationType, datasource);
		}
		if (eventListener instanceof UIPluginViewToolBarListener) {
			return ((UIPluginViewToolBarListener) eventListener).toolBarItemActivated(item, activationType, datasource);
		} else if (eventListener instanceof ToolBarEnabler) {
			return ((ToolBarEnabler) eventListener).toolBarItemActivated(item.getID());
		} 
		return false;
	}

	public void refreshToolBarItems(Map<String, Long> list) {
		if (eventListener instanceof UIPluginViewToolBarListener) {
			((UIPluginViewToolBarListener) eventListener).refreshToolBarItems(list);
		} else if (eventListener instanceof ToolBarEnabler) {
			Map<String, Boolean> states = new HashMap<String, Boolean>();
			for ( Map.Entry<String, Long> entry: list.entrySet()){
				String id = entry.getKey();
				states.put(id, (entry.getValue() & UIToolBarItem.STATE_ENABLED) > 0);
			}
			
			((ToolBarEnabler) eventListener).refreshToolBar(states);

			for ( Map.Entry<String, Boolean> entry: states.entrySet()){
				String id = entry.getKey();
				Boolean visible = entry.getValue();
				list.put(id, visible ? UIToolBarItem.STATE_ENABLED : 0);
			}
		}
	}

	public void setToolBarListener(UIPluginViewToolBarListener l) {
		toolbarListener = l;
	}
	
	public UIPluginViewToolBarListener getToolBarListener() {
		return toolbarListener;
	}
	
	public void
	setUserData(
		Object		key,
		Object		data )
	{
		synchronized( this ){
			
			if ( user_data == null ){
				
				if ( data == null ){
					
					return;
				}
				
				user_data = new HashMap<Object, Object>();
			}
			
			if ( data == null ){
				
				user_data.remove( key );
				
				if ( user_data.isEmpty()){
					
					user_data = null;
				}
			}else{
					
				user_data.put( key, data );
			}
		}
	}
	
	public Object
	getUserData(
		Object		key )
	{
		Map<Object,Object> temp = user_data;
		
		if ( temp == null ){
			
			return( null );
			
		}else{
			
			return( temp.get( key ));
		}
	}
}
