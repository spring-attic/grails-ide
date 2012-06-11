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
package org.grails.ide.eclipse.ui.internal.inplace;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.launch.GrailsLaunchConfigurationDelegate;
import org.grails.ide.eclipse.core.model.IGrailsInstall;

import org.grails.ide.eclipse.ui.GrailsUiActivator;
import org.grails.ide.eclipse.ui.internal.utils.OpenNewResourcesCommandListener;
import org.grails.ide.eclipse.ui.internal.utils.RefreshDependenciesCommandListener;

/**
 * @author Christian Dupuis
 * @author Andrew Eisenberg
 * @author Kris De Volder
 * @since 2.2.1
 */
public abstract class GrailsLaunchUtils {

	/**
	 * Launch a grails command.
	 * 
	 * @param javaProject Context for launching
	 * @param script      Text of grails command to launch
	 * @param persist     Controls whether created launch configuration is persisted in Eclipse launch history.
	 */
	public static void launch(IJavaProject javaProject, String script, boolean persist) {

		IGrailsInstall grailsHome = GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(
				javaProject.getProject());
		if (grailsHome == null) {
			MessageDialog
					.openInformation(
							Display.getDefault().getActiveShell(),
							"Grails Installation",
							"The Grails installation directory has not been configured or is invalid.\\n\\nCheck the Grails project or workspace preference page.");
		}

		// Register the command listener
		GrailsCoreActivator.getDefault().addGrailsCommandResourceListener(
				new OpenNewResourcesCommandListener(javaProject.getProject()));

		if (script != null && script.contains("install-plugin") || script.contains("s2-create-acl-domains")) {
			GrailsCoreActivator.getDefault().addGrailsCommandResourceListener(
					new RefreshDependenciesCommandListener(javaProject.getProject()));
		}
		if (script!=null)

		try {
			ILaunchConfiguration launchConf = GrailsLaunchConfigurationDelegate.getLaunchConfiguration(javaProject.getProject(),
					script, persist);
			DebugUITools.launch(launchConf, ILaunchManager.RUN_MODE);
		}
		catch (CoreException e) {
			GrailsCoreActivator.log(e);
			ErrorDialog.openError(Display.getDefault().getActiveShell(), "Error running Grails command",
					"An error occured running Grails command", new Status(IStatus.ERROR, GrailsUiActivator.PLUGIN_ID,
							0, e.getMessage(), e));
		}
	}

	/**
	 * Launch a grails command. The launch configuration created for the launch is not persisted.
	 * 
	 * @param javaProject Context for launching
	 * @param script      Text of grails command to launch
	 */
	public static void launch(IJavaProject javaProject, String script) {
		launch(javaProject, script, false);
	}

}
