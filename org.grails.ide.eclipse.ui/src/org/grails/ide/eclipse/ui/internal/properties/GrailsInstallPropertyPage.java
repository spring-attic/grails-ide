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
package org.grails.ide.eclipse.ui.internal.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.springsource.ide.eclipse.commons.core.SpringCorePreferences;
import org.springsource.ide.eclipse.commons.ui.ProjectAndPreferencePage;


/**
 * @author Christian Dupuis
 * @author Kris De Volder
 * @since 2.2.0
 */
public class GrailsInstallPropertyPage extends ProjectAndPreferencePage {

	public interface IProjectInstallListener {
		/**
		 * Called when the user changes a project's install settings on a GrailsInstallPropertyPage
		 * @param project		The affected project
		 * @param useDefault	Whether the project was set to inherit the workspace default
		 * @param installName	The name of the install (only relevant when useDefault is false)
		 */
		public void projectInstallChanged(IProject project, boolean useDefault, String installName);
	}

	public static final String PREF_ID = "org.grails.ide.eclipse.ui.preferencePage"; //$NON-NLS-1$

	public static final String PROP_ID = "org.grails.ide.eclipse.ui.projectPropertyPage"; //$NON-NLS-1$

	private static IProjectInstallListener projectInstallListener;

	private Combo grailsInstallCombo;

	public GrailsInstallPropertyPage() {
		noDefaultAndApplyButton();
	}

	protected Control createPreferenceContent(Composite composite) {
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		composite.setLayout(layout);

		IGrailsInstall install = GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(getProject());
		String currentInstallName = "no install configured";
		if (install != null) {
			currentInstallName = install.getName();
		}

		Label notes = new Label(composite, SWT.WRAP);
		notes
				.setText("If no project specific Grails installation is selected, the workspace default installation will be used.\n\nThe project is configured to use '"
						+ currentInstallName + "'.");
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		notes.setLayoutData(gd);

		// Label spacer = new Label(composite, SWT.NONE);
		// spacer.setLayoutData(gd);

		Label options = new Label(composite, SWT.WRAP);
		options.setText("Grails Installation: ");
		options.setLayoutData(new GridData(GridData.BEGINNING));

		grailsInstallCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		grailsInstallCombo.setItems(GrailsCoreActivator.getDefault().getInstallManager().getAllInstallNames());
		grailsInstallCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		String installName = SpringCorePreferences.getProjectPreferences(getProject(), GrailsCoreActivator.PLUGIN_ID)
				.getString(GrailsCoreActivator.GRAILS_INSTALL_PROPERTY, null);
		String[] names = grailsInstallCombo.getItems();
		for (int i = 0; i < names.length; i++) {
			if (names[i].equals(installName)) {
				grailsInstallCombo.select(i);
				break;
			}
		}

		Dialog.applyDialogFont(composite);

		return composite;
	}

	protected String getPreferencePageID() {
		return PREF_ID;
	}

	protected String getPropertyPageID() {
		return PROP_ID;
	}

	protected boolean hasProjectSpecificOptions(IProject project) {
		return SpringCorePreferences.getProjectPreferences(project, GrailsCoreActivator.PLUGIN_ID).getBoolean(
				GrailsCoreActivator.PROJECT_PROPERTY_ID, false);
	}

	public boolean performOk() {
		setInstall(getProject(), useProjectSettings(), grailsInstallCombo.getText());
		return super.performOk();
	}

	/**
	 * This method does the work that happens on "performOK" it is exposed as a public method to ease test creation
	 * (so we can call this functionality without actually using all the UI widgetry in the test).
	 */
	@SuppressWarnings("deprecation")
	public static void setInstall(IProject project, boolean useProjectSettings, String installName) {
		final SpringCorePreferences projectPreferences = SpringCorePreferences.getProjectPreferences(project, GrailsCoreActivator.PLUGIN_ID);
		if (useProjectSettings) {
			projectPreferences.putBoolean(
					GrailsCoreActivator.PROJECT_PROPERTY_ID, useProjectSettings);
			projectPreferences.putString(
					GrailsCoreActivator.GRAILS_INSTALL_PROPERTY, installName);
		} else {
			projectPreferences.putBoolean(
					GrailsCoreActivator.PROJECT_PROPERTY_ID, useProjectSettings);
		}
		GrailsCoreActivator.getDefault().savePluginPreferences();
//		GrailsClasspathContainerUpdateJob.scheduleClasspathContainerUpdateJob(project, true); => Counting on GrailsProjectVersionFixer. Avoid doing this twice!
		notifyInstallListeners(project, !useProjectSettings, installName);
	}
	
	private static void notifyInstallListeners(IProject project, boolean useDefault, String installName) {
		if (projectInstallListener!=null) {
			projectInstallListener.projectInstallChanged(project, useDefault, installName);
		}
	}

	public static void addProjectInstallListener(IProjectInstallListener listener) {
		Assert.isLegal(projectInstallListener==null||projectInstallListener.equals(listener), 
				"Current implementation only supports one listener");
		projectInstallListener = listener;
	}
}
