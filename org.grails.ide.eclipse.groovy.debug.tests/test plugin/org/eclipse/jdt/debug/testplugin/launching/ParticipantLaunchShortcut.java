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
package org.eclipse.jdt.debug.testplugin.launching;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

/**
 * Test launch shortcut implementation
 * @author Andrew Eisenberg
 * @since 3.4
 */
public class ParticipantLaunchShortcut implements ILaunchShortcut2 {

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut2#getLaunchConfigurations(org.eclipse.jface.viewers.ISelection)
	 */
	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
		return getConfigurations();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut2#getLaunchConfigurations(org.eclipse.ui.IEditorPart)
	 */
	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart) {
		return getConfigurations();
	}

	/**
	 * Returns all of the launch configurations of type <code>org.eclipse.jdt.debug.tests.testConfigType</code>
	 * @return all of the launch configurations of type <code>org.eclipse.jdt.debug.tests.testConfigType</code>
	 */
	protected ILaunchConfiguration[] getConfigurations() {
		try {
			ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = lm.getLaunchConfigurationType("org.eclipse.jdt.debug.tests.testConfigType");
			return lm.getLaunchConfigurations(type);
		}
		catch(CoreException ce) {DebugUIPlugin.log(ce);}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut2#getLaunchableResource(org.eclipse.jface.viewers.ISelection)
	 */
	public IResource getLaunchableResource(ISelection selection) {
		return null;//ResourcesPlugin.getWorkspace().getRoot();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut2#getLaunchableResource(org.eclipse.ui.IEditorPart)
	 */
	public IResource getLaunchableResource(IEditorPart editorpart) {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.jface.viewers.ISelection, java.lang.String)
	 */
	public void launch(ISelection selection, String mode) {
		performLaunch(mode);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.ui.IEditorPart, java.lang.String)
	 */
	public void launch(IEditorPart editor, String mode) {
		performLaunch(mode);
	}

	protected void performLaunch(String mode) {
		//first try to find a config
		try {
			ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = lm.getLaunchConfigurationType("org.eclipse.jdt.debug.tests.testConfigType");
			ILaunchConfiguration config = null;
			if(type != null) {
				ILaunchConfiguration[] configs = lm.getLaunchConfigurations(type);
				if(configs.length > 0) {
					config = configs[0];
				}
				if(config == null) {
					//create a new one
				    // doesn't work on Eclipse 3.5
//					ILaunchConfigurationWorkingCopy copy = type.newInstance(null, lm.generateLaunchConfigurationName("New_Test_Config"));
					@SuppressWarnings("deprecation")
                    ILaunchConfigurationWorkingCopy copy = type.newInstance(null, lm.generateUniqueLaunchConfigurationNameFrom("New_Test_Config"));
					copy.setAttribute("testconfig", true);
					config = copy.doSave();
				}
				if(config != null) {
					config.launch(mode, new NullProgressMonitor());
				}
			}
		}
		catch(CoreException ce) {DebugPlugin.log(ce);}
		
	}
}
