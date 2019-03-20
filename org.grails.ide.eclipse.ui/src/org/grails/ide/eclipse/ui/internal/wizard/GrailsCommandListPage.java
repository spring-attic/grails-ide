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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.IFrameworkCommandDescriptor;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.wizard.GenericWizardCommandListPage;


/**
 * Grails-specific command list page that has additional controls for setting
 * Grails environment variable. By default the option to set the Grails
 * environment variable is false (i.e unchecked)
 * @author Nieraj Singh
 */
public class GrailsCommandListPage extends GenericWizardCommandListPage {

	public GrailsCommandListPage(String pageName) {
		super(pageName);
	}

	private Button grailsSystemEnvOption;

	// Add shortcut mneumonic
	private static final String GRAILS_SYSTEM_ENV_OPTION_LABEL = "&Configure Grails System Environment Variable";

	protected Composite createPageArea(Composite parent) {
		Composite area = super.createPageArea(parent);
		createGrailsSystemPropertyOptionArea(area);
		return area;

	}

	public boolean canFlipToNextPage() {
		IFrameworkCommandDescriptor command = getSelectedCommandDescriptor();
		if (command != null && command.getParameters().length == 0
				&& shouldAddGrailsSystemEnv()) {
			setMessage("This command has no parameters. Click finish to execute the command, or go to the next page to set the option Grails environment parameter.");
			return true;
		} else {
			return super.canFlipToNextPage();
		}
	}

	/**
	 * Create area containing the controls to select Grails system environment
	 * variable for configuration
	 * 
	 * @param parent
	 */
	protected void createGrailsSystemPropertyOptionArea(Composite parent) {

		Composite baseCommandArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0)
				.applyTo(baseCommandArea);
		GridDataFactory.fillDefaults().grab(false, false)
				.applyTo(baseCommandArea);

		grailsSystemEnvOption = new Button(baseCommandArea, SWT.CHECK);
		grailsSystemEnvOption.setText(GRAILS_SYSTEM_ENV_OPTION_LABEL);
		grailsSystemEnvOption.setSelection(false);
		grailsSystemEnvOption
				.setToolTipText("Check to configure in the parameters page.");

		grailsSystemEnvOption.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				// Force the wizard to recalculate page complete
				if (getGenericCommandWizard().canFinish()) {
					setPageComplete(true);
				}
			}

		});

		GridDataFactory.fillDefaults().grab(false, false)
				.applyTo(grailsSystemEnvOption);
	}

	public void setGrailsSystemEnv(boolean setGrailsSystemEnv) {
		if (grailsSystemEnvOption != null
				&& !grailsSystemEnvOption.isDisposed()) {
			grailsSystemEnvOption.setSelection(setGrailsSystemEnv);
		}
	}

	/**
	 * If set to true, adds UI controls to configure the Grails environment
	 * variable in the parameters page. If false, nothing happens. Default value
	 * is false.
	 * 
	 * @return true if Grails environment variable should be configured. False
	 *         otherwise. Default is false.
	 */
	public boolean shouldAddGrailsSystemEnv() {
		if (grailsSystemEnvOption != null
				&& !grailsSystemEnvOption.isDisposed()) {
			return grailsSystemEnvOption.getSelection();
		}
		// Default is false
		return false;
	}
}
