/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.debug.testplugin;

import org.eclipse.debug.core.DebugEvent;

/**
 * Waits for an event on a specific element
 * @author Andrew Eisenberg
 */
public class DebugElementEventWaiter extends DebugEventWaiter {
	
	protected Object fElement;
	
	/**
	 * Constructor
	 * @param kind
	 * @param element
	 */
	public DebugElementEventWaiter(int kind, Object element) {
		super(kind);
		fElement = element;
	}
	
	/**
	 * @see org.eclipse.jdt.debug.testplugin.DebugEventWaiter#accept(org.eclipse.debug.core.DebugEvent)
	 */
	public boolean accept(DebugEvent event) {
		return super.accept(event) && fElement == event.getSource();
	}

}
