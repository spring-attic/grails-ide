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
package org.grails.ide.eclipse.ui.internal.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.model.DefaultGrailsInstall;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.springframework.util.StringUtils;

import org.grails.ide.eclipse.ui.GrailsUiActivator;

/**
 * @author Christian Dupuis
 * @author Kris De Volder
 */
public class GrailsInstallDialog extends TitleAreaDialog {

	private Text homeText;

	private Text nameText;

	private Text versionText;

	private String home;
	
	private String name;
	
	private IGrailsInstall install;

	private InstalledGrailsInstallBlock prefPage;

	public GrailsInstallDialog(Shell parentShell, IGrailsInstall install, InstalledGrailsInstallBlock parent) {
		super(parentShell);
		this.prefPage = parent;
		this.install = install;
		this.name = install.getName();
		this.home = install.getHome();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite parentComposite = (Composite) super.createDialogArea(parent);
		Composite composite = new Composite(parentComposite, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		layout.numColumns = 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label name = new Label(composite, SWT.WRAP);
		name.setText("Name:");
		// name.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		nameText = new Text(composite, SWT.BORDER);
		nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (install.getName() != null) {
			nameText.setText(install.getName());
		}
		nameText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				GrailsInstallDialog.this.name = nameText.getText();
				validate(false);
			}
		});

		Label version = new Label(composite, SWT.WRAP);
		version.setText("Version:");
		// version.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		versionText = new Text(composite, SWT.BORDER);
		versionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		versionText.setEnabled(false);
		if (install.getVersionString() != null && !"<unknown>".equals(install.getVersionString())) {
			versionText.setText(install.getVersionString());
		}

		Label directory = new Label(composite, SWT.WRAP);
		directory.setText("Grails home:");
		// directory.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		homeText = new Text(composite, SWT.BORDER);
		homeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		homeText.setEnabled(false);
		if (install.getHome() != null) {
			homeText.setText(install.getHome());
		}
		homeText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				validate(true);
			}
		});

		new Label(composite, SWT.WRAP);

		Button browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setMessage("Select Grails installation directory");
				dialog.setText("Grails installation directory");
				String result = dialog.open();
				if (StringUtils.hasText(result)) {
					homeText.setText(result);
					home = result;
					validate(true);
					if (!StringUtils.hasText(nameText.getText()) && StringUtils.hasText(versionText.getText())) {
						nameText.setText(prefPage.generateName("Grails " + versionText.getText(), install));
					}
				}

			}
		});

		Dialog.applyDialogFont(composite);

		setTitle("Configure Grails Installation");
		setTitleImage(GrailsUiActivator.getImageDescriptor("icons/full/wizban/grails_wizban.png").createImage());
		return composite;
	}

	protected void validate(boolean validateHome) {
		clearError();
		if (homeText.getText() == null || homeText.getText().equals("")) {
			setError("Select a Grails home directory");
			return;
		}
		else if (validateHome) {
			File rooHome = new File(homeText.getText());
			if (!rooHome.exists()) {
				setError("Specified directory does not exist");
				return;
			}
			else if (!rooHome.isDirectory()) {
				setError("Specified path does not point to a directory");
				return;
			}
			else {
				if (!(new File(rooHome, "lib").exists())) {
					setError("Specified directory does not appear to be a Grails installation");
					return;
				}
				if (!(new File(rooHome, "dist").exists())) {
					setError("Specified directory does not appear to be a Grails installation");
					return;
				}
			}

			File buildProperties = new File(rooHome, "build.properties");
			if (buildProperties.exists()) {
				Properties props = new Properties();
				try {
					props.load(new FileInputStream(buildProperties));
					if (props.getProperty("grails.version") != null) {
						versionText.setText(props.getProperty("grails.version"));
					}
					else {
						setError("Can't read version from build.properties");
						return;
					}
				}
				catch (FileNotFoundException e) {
					GrailsCoreActivator.log("Can't read version from build.properties", e);
					setError("Specified directory does not appear to be a Grails installation");
					return;
				}
				catch (IOException e) {
					GrailsCoreActivator.log("Can't read version from build.properties", e);
					setError("Specified directory does not appear to be a Grails installation");
					return;
				}
			}
			else {
				setError("Specified directory does not appear to be a Grails installation");
				return;
			}
		}
		if (nameText.getText() == null || nameText.getText().equals("")) {
			setError("A unique name is required");
		}
		else {
			if (prefPage.isDuplicateName(nameText.getText(), install)) {
				setError("Name is not unique");
			}
		}
	}

	private void setError(String message) {
		getButton(OK).setEnabled(false);
		setErrorMessage(message);
	}

	private void clearError() {
		getButton(OK).setEnabled(true);
		setErrorMessage(null);
	}

	public IGrailsInstall getResult() {
		return new DefaultGrailsInstall(home, name, install.isDefault());
	}
}
