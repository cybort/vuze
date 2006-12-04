/**
 * 
 */
package com.aelitis.azureus.ui.swt.skin;

import org.eclipse.swt.widgets.Control;

/**
 * @author TuxPaper
 * @created Jun 12, 2006
 *
 */
public interface SWTSkinObject
{
	/**
	 * Retrieve the associated SWT Control used by the skin object
	 * 
	 * @return SWT Control
	 */
	public Control getControl();

	/**
	 * Retrieve the type of widget.
	 * @return
	 * 
	 * TODO Move widget types to SWTSkinObject
	 */
	public String getType();

	/**
	 * Retrieve the Skin Object ID that represents this object.  Typically the
	 * same as {@link #getConfigID()}, however, may be different if a config
	 * id is used to make independant copies
	 * 
	 * @return An unique Skin Object ID
	 */
	public String getSkinObjectID();

	/**
	 * Retrieve the Config ID which is ID in the skin config file.
	 * 
	 * @return Config ID
	 */
	public String getConfigID();

	public SWTSkinObject getParent();

	public SWTSkin getSkin();

	public void setVisible(boolean visible);

	/**
	 * @param sConfigID
	 * @param sSuffix
	 */
	void setBackground(String sConfigID, String sSuffix);

	/**
	 * @param suffix
	 * @param level 
	 * @param walkUp TODO
	 * @return TODO
	 */
	String switchSuffix(String suffix, int level, boolean walkUp);

	/**
	 * @return
	 */
	String getSuffix();

	/**
	 * @param properties
	 */
	void setProperties(SWTSkinProperties skinProperties);

	/**
	 * @return
	 */
	SWTSkinProperties getProperties();

	//void getTemplate(String property)

	public void addListener(SWTSkinObjectListener listener);

	public void removeListener(SWTSkinObjectListener listener);

	/**
	 * @return
	 */
	public SWTSkinObjectListener[] getListeners();

	public String getViewID();

	/**
	 * @param eventType
	 */
	void triggerListeners(int eventType);

	/**
	 * @param eventType
	 * @param params
	 */
	void triggerListeners(int eventType, Object params);
}
