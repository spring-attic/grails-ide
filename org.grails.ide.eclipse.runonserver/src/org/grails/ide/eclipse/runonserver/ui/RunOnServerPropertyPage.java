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
package org.grails.ide.eclipse.runonserver.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.GrailsVersion;

import org.grails.ide.eclipse.runonserver.RunOnServerProperties;

/**
 * Property page for Grails App projects, containing run on server related properties.
 * 
 * @author Kris De Volder
 * @since 2.6
 */
public class RunOnServerPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

	Button[] envButtons;
	private Button customEnvButton;
	private Text customEnvText;
	private Button incrementalButton;
	
	public RunOnServerPropertyPage() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 1;
        layout.marginWidth = 1;
        page.setLayout(layout);
       
		createEnvSection(page);
		createIncrementalSection(page);
        
        return page;
	}

	private void createEnvSection(Composite page) {
        GridDataFactory grabHorizontal = GridDataFactory.fillDefaults().grab(true, false);
		
		Label label = new Label(page, SWT.NONE);
		label.setText("Grails Environment");
		label.setToolTipText("Grails Environment parameter that will be used to deploy\n" +
				"this Grails app to TcServer");

        Composite composite = new Composite(page, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
        composite.setLayout(layout);
        
		envButtons = new Button[] {
				new Button(composite, SWT.RADIO),
				new Button(composite, SWT.RADIO),
				new Button(composite, SWT.RADIO)
		};
		
		GridDataFactory spanTwo = GridDataFactory.fillDefaults().span(2, 1);
		for (Button b : envButtons) {
			spanTwo.applyTo(b);
		}
				
        envButtons[0].setText("dev");
        envButtons[1].setText("prod");
        envButtons[2].setText("test");
        
        customEnvButton = new Button(composite, SWT.RADIO);
        customEnvButton.setText("Custom");
        customEnvText = new Text(composite, SWT.BORDER);
        customEnvText.setText(RunOnServerProperties.DEFAULT_ENV);
        
        customEnvButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				customEnvText.setEnabled(customEnvButton.getSelection());
			}
		});
        
        grabHorizontal.applyTo(composite);
        grabHorizontal.applyTo(customEnvText);

        setEnvironmentInPage(getEnvironment());
        
        oldGrailsBugWarning();
	}

	private void createIncrementalSection(Composite page) {
        incrementalButton = new Button(page, SWT.CHECK);
		incrementalButton.setText("Incremental War Build");
		incrementalButton.setToolTipText("When enabled, tries to avoid calls to grails war command by applying " +
				"changes from workspace incrementally to previously created war contents.");
		
		incrementalButton.setSelection(getIncremental());
	}
	
	private void oldGrailsBugWarning() {
		GrailsVersion version = GrailsVersion.getEclipseGrailsVersion(getProject());
		if (GrailsVersion.V_1_3_6.compareTo(version)>0) {
			setMessage("Grails "+version+" (older than 1.3.6) may not honor the environment parameter!", DialogPage.WARNING);
		}
	}

	private String getEnvironmentInPage() {
		for (Button b : envButtons) {
			if (b.getSelection()) {
				return b.getText();
			}
		}
		String customEnv = customEnvText.getText().trim();
		if ("".equals(customEnv)) {
			customEnv = RunOnServerProperties.DEFAULT_ENV;
		}
		return customEnv;
	}
	
	private void setEnvironmentInPage(String environment) {
		boolean custom = true;
		for (Button b : envButtons) {
			boolean setThis = b.getText().equals(environment);
			b.setSelection(setThis);
			if (setThis) {
				custom = false;
			}
		}
		customEnvButton.setSelection(custom);
		customEnvText.setEnabled(custom);
		if (custom) {
			customEnvText.setText(environment);
		}
	}

	private String getEnvironment() {
		IProject project = getProject();
		return RunOnServerProperties.getEnv(project);
	}

	private boolean getIncremental() {
		IProject project = getProject();
		return RunOnServerProperties.getIncremental(project);
	}
	
	private IProject getProject() {
		return (IProject) getElement().getAdapter(IProject.class);
	}
	
	private void setEnvironment(String env) throws CoreException {
		IProject project = getProject();
		RunOnServerProperties.setEnv(project, env);
	}

	private void setIncremental(boolean isIncremental) throws CoreException {
		IProject project = getProject();
		RunOnServerProperties.setIncremental(project, isIncremental);
	}
	
	@Override
	public boolean performOk() {
		super.performOk();
		try {
			setEnvironment(getEnvironmentInPage());
			setIncremental(incrementalButton.getSelection());
			return true;
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
			return false;
		}
	}

}
