/*
 * Created on May 29, 2006 3:06:06 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui;


/**
 * @author TuxPaper
 * @created May 29, 2006
 *
 */
public interface IUIIntializer
{
	public void stopIt(boolean isForRestart, boolean isCloseAreadyInPorgress);

	public void run();

	/**
	 * Add a listener that gets triggered on progress changes (tasks, percent)
	 * 
	 * @param listener
	 *
	 * @since 3.0.1.3
	 */
	public void addListener(InitializerListener listener);

	/**
	 * Remove listener that gets triggered on progress changes (tasks, percent)
	 * 
	 * @param listener
	 *
	 * @since 3.0.1.3
	 */
	public void removeListener(InitializerListener listener);

	/**
	 * 
	 *
	 * @since 3.0.4.3
	 */
	void increaseProgresss();
	
	/**
	 *
	 * @since 3.0.4.3
	 */
	void abortProgress();

	/**
	 * @param currentTaskString
	 *
	 * @since 3.0.4.3
	 */
	void reportCurrentTask(String currentTaskString);
	
	void reportPercent(int percent);
}
