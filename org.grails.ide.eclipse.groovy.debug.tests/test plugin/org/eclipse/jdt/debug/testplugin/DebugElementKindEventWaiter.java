/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.debug.testplugin;

import org.eclipse.debug.core.DebugEvent;

/**
 * Waits for a type of event on a kind of element.  Compare this to SpecificDebugElementEventWaiter which is
 * used to wait for a type of event on a specific debug element object.
 * @author Andrew Eisenberg
 */

public class DebugElementKindEventWaiter extends DebugEventWaiter {
	
	protected Class fElementClass;
	
	/**
	 * Constructor
	 * @param eventKind
	 * @param elementClass
	 */
	public DebugElementKindEventWaiter(int eventKind, Class elementClass) {
		super(eventKind);
		fElementClass = elementClass;
	}
	
	/**
	 * @see org.eclipse.jdt.debug.testplugin.DebugEventWaiter#accept(org.eclipse.debug.core.DebugEvent)
	 */
	public boolean accept(DebugEvent event) {
		Object o = event.getSource();
		return super.accept(event) && fElementClass.isInstance(o);
	}

}


