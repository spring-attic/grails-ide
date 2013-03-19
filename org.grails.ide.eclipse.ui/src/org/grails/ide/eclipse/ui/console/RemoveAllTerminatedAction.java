/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Kris De Volder - copied from org.eclipse.debug.internal.ui.views.console.ConsoleRemoveAllTerminatedAction
 *                    - modified to add similar action to GrailsIOConsole
 *******************************************************************************/
package org.grails.ide.eclipse.ui.console;

import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.views.console.ConsoleMessages;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IUpdate;
import org.grails.ide.eclipse.longrunning.ConsoleProvider;
import org.grails.ide.eclipse.longrunning.GrailsProcessManager;

/**
 * ConsoleRemoveAllTerminatedAction
 */
public class RemoveAllTerminatedAction extends Action implements IUpdate {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	public void update() {
		setEnabled(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		ConsoleProvider cp = GrailsProcessManager.consoleProvider;
		if (cp!=null) {
			cp.removeAllTerminated();
		}
	}
	
	public RemoveAllTerminatedAction() {
		super(ConsoleMessages.ConsoleRemoveAllTerminatedAction_0); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.CONSOLE_REMOVE_ALL_TERMINATED);
		setToolTipText(ConsoleMessages.ConsoleRemoveAllTerminatedAction_1); 
		setImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_REMOVE_ALL));
		setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_REMOVE_ALL));
		setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_REMOVE_ALL));
		update();
	}

	public void dispose() {
	}

}
