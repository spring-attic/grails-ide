/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River - code snippets copied from org.eclipse.tm.internal.terminal.ssh.SshSettingsPage
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.terminal;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tm.internal.terminal.provisional.api.ISettingsPage;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.longrunning.LongRunningProcessGrailsExecutor;

/**
 * A dummy settings page to satisfy the API requirements. Once we make the
 * connector 'hidden' and it can only be instantiated programmatically by
 * executing Grails commands, then we can probably get rid of this class
 * entirely and return 'null' instead of a instance of this.
 * 
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class GrailsTerminalSettingsPage implements ISettingsPage {

	private Combo fProject;
	private Text fCommand;
	private GrailsTerminalSettings settings;

	public GrailsTerminalSettingsPage(GrailsTerminalSettings settings) {
		this.settings = settings;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(2, false);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);

		composite.setLayout(gridLayout);
		composite.setLayoutData(gridData);

		fProject = createProjectField(composite, "Project");
		fCommand = createTextField(composite, "Command");
		
		loadSettings();
	}
	
	@Override
	public void loadSettings() {
		fProject.setText(settings.getProjectName());
		fCommand.setText(settings.getCommand());
	}

	@Override
	public void saveSettings() {
		settings.setProjectName(fProject.getText());
		settings.setCommand(fCommand.getText());
	}
	
	private Combo createProjectField(Composite composite, String labelText) {
		GridData gridData;
		// Add label
		Label ctlLabel = new Label(composite, SWT.RIGHT);
		ctlLabel.setText(labelText + ":"); //$NON-NLS-1$

		// Add control
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		Combo combo= new Combo(composite, SWT.READ_ONLY);
		combo.setItems(getGrailsProjectNames());
		combo.setLayoutData(gridData);
		return combo;
	}

	private Text createTextField(Composite composite, String labelTxt, int textOptions) {
		GridData gridData;
		// Add label
		Label ctlLabel = new Label(composite, SWT.RIGHT);
		ctlLabel.setText(labelTxt + ":"); //$NON-NLS-1$

		// Add control
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		Text text= new Text(composite, SWT.BORDER | textOptions);
		text.setLayoutData(gridData);
		return text;
	}
	private Text createTextField(Composite composite, String labelTxt) {
		return createTextField(composite, labelTxt, 0);
	}
	
	@Override
	public boolean validateSettings() {
		IProject project = getProject();
		if (project!=null && GrailsNature.isGrailsProject(project)) {
			GrailsVersion version = GrailsVersion.getEclipseGrailsVersion(project);
			//TODO: It may be possible to do this for any grails version?
			// As long as we can enable ansi codes.
			return LongRunningProcessGrailsExecutor.canHandleVersion(version);
		}
		return true;
	}

	private String[] getGrailsProjectNames() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		java.util.List<String> names = new ArrayList<String>();
		for (IProject project : projects) {
			// Test if the selected project has Grails nature
			if (isValidProject(project)) {
				names.add(project.getName());
			}
		}
//		if (selectedProject!=null && !isValidProject(selectedProject)) {
//			names.add(selectedProject.getName()); // make sure selected project is always added! or it won't show in the dropdown!
//		}
		return names.toArray(new String[names.size()]);
	}
	
	private boolean isValidProject(IProject project) {
		return project!=null && GrailsNature.isGrailsProject(project);
	}

	private IProject getProject() {
		try {
			if (fProject!=null) {
				String name = fProject.getText();
				if ("".equals(name)) {
					return null;
				}
				IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
				return proj;
			}
		} catch (Exception e) {
			GrailsCoreActivator.log(e);
		}
		return null;
	}

}
