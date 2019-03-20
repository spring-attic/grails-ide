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

import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaBreakpointListener;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;

/**
 * Listens to all breakpoint notifications.
 * @author Andrew Eisenberg
 */
public class GlobalBreakpointListener implements IJavaBreakpointListener {
	
	public static IJavaBreakpoint ADDED;
	public static IJavaBreakpoint HIT;
	public static IJavaBreakpoint INSTALLED;
	public static IJavaBreakpoint REMOVED;
	public static IJavaBreakpoint INSTALLING;
	
	public static void clear() {
		ADDED = null;
		HIT = null;
		INSTALLED = null;
		REMOVED = null;
		INSTALLING = null;
	}

	public GlobalBreakpointListener() {
	}

	public void addingBreakpoint(IJavaDebugTarget target, IJavaBreakpoint breakpoint) {
		ADDED = breakpoint;
	}

	public void breakpointHasCompilationErrors(IJavaLineBreakpoint breakpoint, Message[] errors) {
	}

	public void breakpointHasRuntimeException(IJavaLineBreakpoint breakpoint, DebugException exception) {
	}

	public int breakpointHit(IJavaThread thread, IJavaBreakpoint breakpoint) {
		HIT = breakpoint;
		return DONT_CARE;
	}

	public void breakpointInstalled(IJavaDebugTarget target, IJavaBreakpoint breakpoint) {
		INSTALLED = breakpoint;
	}

	public void breakpointRemoved(IJavaDebugTarget target, IJavaBreakpoint breakpoint) {
		REMOVED = breakpoint;
	}

	public int installingBreakpoint(IJavaDebugTarget target, IJavaBreakpoint breakpoint, IJavaType type) {
		INSTALLING = breakpoint;
		return DONT_CARE;
	}

}
