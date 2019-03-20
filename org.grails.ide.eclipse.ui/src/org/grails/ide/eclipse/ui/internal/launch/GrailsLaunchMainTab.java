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
package org.grails.ide.eclipse.ui.internal.launch;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.internal.launching.JavaMigrationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.launch.GrailsLaunchArgumentUtils;
import org.grails.ide.eclipse.core.model.GrailsBuildSettingsHelper;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.springsource.ide.eclipse.commons.core.SpringCoreUtils;

import org.grails.ide.eclipse.ui.GrailsUiImages;
import org.grails.ide.eclipse.ui.internal.inplace.GrailsCompletionUtils;

/**
 * @author Christian Dupuis
 * @since 2.2.0
 */
public class GrailsLaunchMainTab extends JavaLaunchTab {

	/**
	 * A listener which handles widget change events for the controls in this tab.
	 */
	private class WidgetListener implements ModifyListener, SelectionListener {

		public void modifyText(ModifyEvent e) {
			updateLaunchConfigurationDialog();
		}

		public void widgetDefaultSelected(SelectionEvent e) {/* do nothing */
		}

		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();
			if (source == fProjButton) {
				handleProjectButtonSelected();
			}
			else {
				updateLaunchConfigurationDialog();
			}
		}
	}

	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$

	// Project UI widgets
	protected Text fProjText;

	private Button fProjButton;

	private WidgetListener fListener = new WidgetListener();

	/**
	 * chooses a project for the type of java launch config that it is
	 * @return
	 */
	private IJavaProject chooseJavaProject() {
		ILabelProvider labelProvider = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setTitle("");
		dialog.setMessage("");
		try {
			Set<IJavaProject> projects = new LinkedHashSet<IJavaProject>();
			for (IJavaProject project : JavaCore.create(getWorkspaceRoot()).getJavaProjects()) {
				if (SpringCoreUtils.hasNature(project.getResource(), GrailsNature.NATURE_ID)) {
					projects.add(project);
				}
			}
			dialog.setElements((IJavaProject[]) projects.toArray(new IJavaProject[projects.size()]));
		}
		catch (JavaModelException e) {
			GrailsCoreActivator.log(e);
		}
		IJavaProject javaProject = getJavaProject();
		if (javaProject != null) {
			dialog.setInitialSelections(new Object[] { javaProject });
		}
		if (dialog.open() == Window.OK) {
			return (IJavaProject) dialog.getFirstResult();
		}
		return null;
	}

	/**
	 * Creates the widgets for specifying a main type.
	 * 
	 * @param parent the parent composite
	 */
	protected void createProjectEditor(Composite parent) {
		Font font = parent.getFont();
		Group group = new Group(parent, SWT.NONE);
		group.setText(LauncherMessages.AbstractJavaMainTab_0);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setFont(font);
		fProjText = new Text(group, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fProjText.setLayoutData(gd);
		fProjText.setFont(font);
		fProjText.addModifyListener(fListener);
		fProjText.setEditable(false);
		fProjButton = createPushButton(group, LauncherMessages.AbstractJavaMainTab_1, null);
		fProjButton.addSelectionListener(fListener);
	}

	/**
	 * returns the default listener from this class. For all subclasses this listener will only provide the
	 * functionality of updating the current tab
	 * 
	 * @return a widget listener
	 */
	protected WidgetListener getDefaultListener() {
		return fListener;
	}

	/**
	 * Convenience method to get access to the java model.
	 */
	private IJavaModel getJavaModel() {
		return JavaCore.create(getWorkspaceRoot());
	}

	/**
	 * Return the IJavaProject corresponding to the project name in the project name text field, or null if the text
	 * does not match a project name.
	 */
	protected IJavaProject getJavaProject() {
		String projectName = fProjText.getText().trim();
		if (projectName == null || projectName.length() < 1) {
            setErrorMessage("No project selected");
			return null;
		}
		IJavaProject javaProject = getJavaModel().getJavaProject(projectName);
		if (! javaProject.exists()) {
		    setErrorMessage("Project " + projectName + " doesn't exist");
		    return null;
		}
		if (! javaProject.exists()) {
		    setErrorMessage("Project " + projectName + " isn't open");
            return null;
		}
		setErrorMessage(null);
        return javaProject;
	}

	/**
	 * Convenience method to get the workspace root.
	 */
	protected IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/**
	 * Show a dialog that lets the user select a project. This in turn provides context for the main type, allowing the
	 * user to key a main type name, or constraining the search for main types to the specified project.
	 */
	protected void handleProjectButtonSelected() {
		IJavaProject project = chooseJavaProject();
		if (project == null) {
			return;
		}
		String projectName = project.getElementName();
		fProjText.setText(projectName);
		initializeContentAssist();
	}

	/**
	 * updates the project text field form the configuration
	 * @param config the configuration we are editing
	 */
	private void updateProjectFromConfig(ILaunchConfiguration config) {
		String projectName = EMPTY_STRING;
		try {
			projectName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, EMPTY_STRING);
			script.setText(config.getAttribute(GrailsCoreActivator.PLUGIN_ID + ".SCRIPT", EMPTY_STRING));
		}
		catch (CoreException ce) {
			setErrorMessage(ce.getStatus().getMessage());
		}
		fProjText.setText(projectName);
	}

	/**
	 * Maps the config to associated java resource
	 * 
	 * @param config
	 */
	protected void mapResources(ILaunchConfigurationWorkingCopy config) {
		try {
			// CONTEXTLAUNCHING
			IJavaProject javaProject = getJavaProject();
			if (javaProject != null && javaProject.exists() && javaProject.isOpen()) {
				JavaMigrationDelegate.updateResourceMapping(config);
			}
		}
		catch (CoreException ce) {
			setErrorMessage(ce.getStatus().getMessage());
		}
	}

	private Text script;

	public Image getImage() {
		return GrailsUiImages.getImage(GrailsUiImages.IMG_OBJ_GRAILS);
	}

	public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		setControl(mainComposite);

		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		mainComposite.setLayout(layout);
		mainComposite.setLayoutData(gridData);
		mainComposite.setFont(parent.getFont());

		createProjectEditor(mainComposite);

		Group group = new Group(mainComposite, SWT.NONE);
		group.setText("Grails Command");
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);
		layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);

		Label label = new Label(group, SWT.NONE);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		label.setText("grails> ");

		script = new Text(group, SWT.BORDER);
		script.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		script.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});

		new Label(group, SWT.NONE);
		
		Label note = new Label(group, SWT.NONE);
		note.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		note.setText("Note: type command without the 'grails' prefix");
	}

	public void initializeFrom(ILaunchConfiguration configuration) {
		super.initializeFrom(configuration);
		updateProjectFromConfig(configuration);
		initializeContentAssist();
		setDirty(false);
	}

	private void initializeContentAssist() {
		if (getJavaProject() != null) {
			GrailsCompletionUtils.addTypeFieldAssistToText(script, getJavaProject().getProject());
		}
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		if (configuration == null) {
			return;
		}
		if (getJavaProject() != null) {
			IProject project = getJavaProject().getProject();
			IGrailsInstall install = GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(project);
			try {
				GrailsLaunchArgumentUtils.prepareLaunchConfiguration(project, script.getText(), install,
						GrailsBuildSettingsHelper.getBaseDir(project), configuration);
				configuration.doSave();
				setDirty(false);
			}
			catch (CoreException e) {
				GrailsCoreActivator.log(e);
			}
		}
	}

	public String getName() {
		return "Grails";
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
		configuration.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, new HashMap<String, String>());
	}
}
