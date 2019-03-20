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
package org.grails.ide.eclipse.ui.internal.wizard;

import org.eclipse.osgi.util.NLS;

/**
 * @author Terry Denney
 * @author Andy Clement
 * @author Nieraj Singh
 */
public class NewGrailsWizardMessages extends NLS {
	private static final String BUNDLE_NAME = "org.grails.ide.eclipse.ui.internal.wizard.messages"; //$NON-NLS-1$
	public static String NewGrailsProjectWizardPageOne_createGrailsProject;
	public static String NewGrailsProjectWizardPageOne_createGrailsProjectInWorkspace;
	public static String NewGrailsPluginProjectWizardPageOne_createGrailsProject;
	public static String NewGrailsPluginProjectWizardPageOne_createGrailsProjectInWorkspace;
	public static String NewGrailsProjectWizardPageOne_noGrailsInstallation;
	public static String NewGrailsProjectWizardPageOne_noGrailsInstallationInWorkspacePreferences;
	public static String NewGrailsProjectWizardPageOne_useDefaultGrailsInstallation;
	public static String NewGrailsProjectWizardPageOne_useDefaultGrailsInstallationNoCurrent;
	public static String NewGrailsProjectWizardPageOne_notExisingProjectOnWorkspaceRoot;
	public static String NewGrailsProjectWizardPageOne_notOnWorkspaceRoot;
	public static String NewGrailsProjectWizardPageOne_importingExistingProject;
	public static String NewGrailsProjectWizardPageOne_invalidProjectNameForExternalLocation;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, NewGrailsWizardMessages.class);
	}

	private NewGrailsWizardMessages() {
	}
}
