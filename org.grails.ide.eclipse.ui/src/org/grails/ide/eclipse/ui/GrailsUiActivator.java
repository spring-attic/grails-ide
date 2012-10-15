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
package org.grails.ide.eclipse.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.grails.ide.eclipse.commands.GroovyCompilerVersionCheck;
import org.grails.ide.eclipse.commands.JDKCheck;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.longrunning.GrailsProcessManager;
import org.grails.ide.eclipse.longrunning.LongRunningProcessGrailsExecutor;
import org.osgi.framework.BundleContext;

import org.grails.ide.eclipse.ui.console.GrailsUIConsoleProvider;
import org.grails.ide.eclipse.ui.internal.dialogues.GroovyCompilerVersionCheckDialogProvider;
import org.grails.ide.eclipse.ui.internal.importfixes.GrailsOutputFolderFixer;
import org.grails.ide.eclipse.ui.internal.importfixes.GrailsProjectVersionFixer;
import org.grails.ide.eclipse.ui.internal.importfixes.JDKCheckMessageDialogProvider;

/**
 * @author Christian Dupuis
 * @author Kris De Volder
 * @since 2.2.0
 */
public class GrailsUiActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.grails.ide.eclipse.ui";

	// The shared instance
	private static GrailsUiActivator plugin;

	/**
	 * A resource listener responsible for helping users fix imported but broken projects
	 */
	public GrailsProjectVersionFixer projectVersionFixer;

	/**
	 * An object that is responsible for fixing project output folders (when they have the 'bad' shared folder that is also used by
	 * Grails war command. 
	 */
	private GrailsOutputFolderFixer outputFolderFixer;

    public static final String M2ECLIPSE_NATURE = "org.eclipse.m2e.core.maven2Nature";

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		projectVersionFixer = new GrailsProjectVersionFixer();
		outputFolderFixer = new GrailsOutputFolderFixer(projectVersionFixer);
		
		// Hookup some stuff the UI plugin implements that needs to be 'plugged in' to core
		LongRunningProcessGrailsExecutor.consoleProvider = new GrailsUIConsoleProvider();
		JDKCheck.ui = new JDKCheckMessageDialogProvider();
		GroovyCompilerVersionCheck.setDialogProvider(new GroovyCompilerVersionCheckDialogProvider());
	}

	public void stop(BundleContext context) throws Exception {
		if (projectVersionFixer!=null) {
			projectVersionFixer.dispose();
		}
		plugin = null;
		super.stop(context);
	}

	public static boolean isM2EProject(IProject project) {
        try {
            return project.isAccessible() && project.hasNature(M2ECLIPSE_NATURE);
        } catch (CoreException e) {
            GrailsCoreActivator.log(e);
            return false;
        }
    }

    public static GrailsUiActivator getDefault() {
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
