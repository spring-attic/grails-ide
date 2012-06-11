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
import org.eclipse.debug.core.model.IDebugElement;

/**
 * This event waiter is used to wait for a certain type of event (create, terminate, suspend, etc.) 
 * on a *specific* debug element.  Contrast this with DebugElementKindEventWaiter which is similar, 
 * but is used to wait for a certain type of event on a *kind* of debug element (thread, debug target, etc.)
 * @author Andrew Eisenberg
 */
public class SpecificDebugElementEventWaiter extends DebugEventWaiter {

	protected IDebugElement fDebugElement;
	
	/**
	 * Constructor
	 * @param eventKind
	 * @param element
	 */
	public SpecificDebugElementEventWaiter(int eventKind, IDebugElement element) {
		super(eventKind);
		fDebugElement = element;
	}
	
	/**
	 * @see org.eclipse.jdt.debug.testplugin.DebugEventWaiter#accept(org.eclipse.debug.core.DebugEvent)
	 */
	public boolean accept(DebugEvent event) {
		Object o = event.getSource();
		if (o instanceof IDebugElement) {
			return super.accept(event) && ((IDebugElement)o).equals(fDebugElement);
		}
		return false;
	}
}
