/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Kris De Volder - copied from org.eclipse.debug.internal.ui.views.console.ConsoleTerminateAction
 *                    - modified to create a StopCommandAction for Grails long running process.
 *******************************************************************************/
package org.grails.ide.eclipse.ui.console;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.commands.ITerminateHandler;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.commands.actions.DebugCommandService;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.debug.internal.ui.views.console.ConsoleMessages;
import org.grails.ide.eclipse.longrunning.client.GrailsCommandExecution;

/**
 * ConsoleTerminateAction
 */
public class StopCommandAction extends Action implements IUpdate {

	private GrailsIOConsole fConsole;
	private IWorkbenchWindow fWindow;

	/**
	 * Creates a terminate action for the console 
	 * @param window the window
	 * @param console the console
	 */
	public StopCommandAction(IWorkbenchWindow window, GrailsIOConsole console) {
		super(ConsoleMessages.ConsoleTerminateAction_0); 
		fConsole = console;
		fWindow = window;
		setToolTipText(ConsoleMessages.ConsoleTerminateAction_1); 
		setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_LCL_TERMINATE));
		setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_TERMINATE));
		setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_LCL_TERMINATE));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.CONSOLE_TERMINATE_ACTION);
		update();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	public void update() {
		GrailsCommandExecution process = fConsole.getExecution(); 
		setEnabled(process!=null && process.canTerminate());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		final GrailsCommandExecution process = fConsole.getExecution();
		process.destroy();
	}
	
    public void dispose() {
	    fConsole = null;
	}

}
