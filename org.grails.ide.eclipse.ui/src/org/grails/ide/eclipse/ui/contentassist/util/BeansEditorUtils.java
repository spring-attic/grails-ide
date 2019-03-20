// COPIED from spring-ide org.springframework.ide.eclipse.beans.ui.editor.util.BeansEditorUtils
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
package org.grails.ide.eclipse.ui.contentassist.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Collection of helper methods for beans XML editor.
 * @author Christian Dupuis
 * @author Torsten Juergeleit
 * @author Terry Denney
 */
public class BeansEditorUtils {

	/**
	 * Returns the non-blocking Progress Monitor form the StatuslineManger
	 * @return the progress monitor
	 */
	public static final IProgressMonitor getProgressMonitor() {
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		// this check is to allow for non UI thread call to this method
		if (activeWorkbenchWindow == null) {
			return new NullProgressMonitor();
		}

		IEditorPart editor = activeWorkbenchWindow.getActivePage().getActiveEditor();
		if (editor != null && editor.getEditorSite() != null && editor.getEditorSite().getActionBars() != null
				&& editor.getEditorSite().getActionBars().getStatusLineManager() != null
				&& editor.getEditorSite().getActionBars().getStatusLineManager().getProgressMonitor() != null) {

			IStatusLineManager manager = editor.getEditorSite().getActionBars().getStatusLineManager();
			IProgressMonitor monitor = manager.getProgressMonitor();
			manager.setMessage("Processing completion proposals");
			manager.setCancelEnabled(true);
			return monitor;
		}
		else {
			return new NullProgressMonitor();
		}
	}
}
