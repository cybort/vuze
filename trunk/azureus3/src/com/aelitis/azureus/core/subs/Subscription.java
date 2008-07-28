/*
 * Created on Jul 11, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.subs;

public interface 
Subscription 
{
	public String
	getName();
	
	public byte[]
	getPublicKey();
	
	public int
	getVersion();
	
	public boolean
	isMine();
	
	public boolean
	isPublic();
	
	public boolean
	isSubscribed();
	
	public void
	setSubscribed(
		boolean		subscribed );
	
	public long
	getPopularity()
	
		throws SubscriptionException;
	
	public void
	addAssociation(
		byte[]		hash );
	
	public String
	getString();
}
