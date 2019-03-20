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
package org.grails.ide.eclipse.ui.internal.wizard.inport;

import java.io.File;
import java.util.LinkedHashSet;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.grails.ide.eclipse.ui.GrailsUiActivator;
import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;
import org.springsource.ide.eclipse.commons.livexp.core.ValidationResult;
import org.springsource.ide.eclipse.commons.livexp.core.ValueListener;
import org.springsource.ide.eclipse.commons.livexp.ui.WizardPageSection;

public class RootFolderSection extends WizardPageSection {

	private static final String GRAILS_IMPORT_LOCATION_HIST = "grails.import.location.hist";
	private static final int MAX_GRAILS_IMPORT_LOCATION_HIST = 10;

	private final GrailsImportWizardPage page;
	private Combo rootFolderCombo;
	
	public RootFolderSection(GrailsImportWizardPage owner) {
		super(owner);
		page = owner;
	}

	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return page.model.locationValidator;
	}

	public File getRootFolder() {
		String text = rootFolderCombo.getText();
		if (text!=null && !("".equals(text.trim()))) {
			return new File(text);
		}
		return null;
	}

	private void updateRootFolderHistory() {
		File rf = getRootFolder();
		if (rf!=null){
			String[] hist = getRootFolderHistory();
			LinkedHashSet<String> newHist = new LinkedHashSet<String>(hist.length+1);
			newHist.add(rf.toString());
			for (String string : hist) {
				if (newHist.size()<MAX_GRAILS_IMPORT_LOCATION_HIST) {
					newHist.add(string);
				}
			}
			setRootFolderHistory(newHist.toArray(new String[newHist.size()]));
		}
	}
	
	@Override
	public void createContents(Composite page) {
		GridDataFactory grabHorizontal = GridDataFactory.fillDefaults().grab(true, false);

		//Project location selection
		GridLayout layout = new GridLayout(3, false);
		Composite composite = new Composite(page, SWT.NONE);
		composite.setLayout(layout);

		Label label = new Label(composite, SWT.NONE);
		label.setText("Location:");
		rootFolderCombo = new Combo(composite, SWT.DROP_DOWN);
		rootFolderCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				RootFolderSection.this.page.model.location.setValue(getRootFolder());
			}
		});

		Button browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText("Browse...");

		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//Button clicked
				File file = openFileDialog(rootFolderCombo.getText());
				if (file!=null) {
					rootFolderCombo.setText(file.toString());
				}
			}
		});

		String[] rootFolderHistory = getRootFolderHistory();
		if (rootFolderHistory.length>0) {
			rootFolderCombo.setItems(rootFolderHistory);
			rootFolderCombo.select(0);
		}

		grabHorizontal.applyTo(composite);
		grabHorizontal.applyTo(rootFolderCombo);
		
		getValidator().addListener(new ValueListener<ValidationResult>() {
			/** 
			 * Update the history every time there is valid grails project location in 
			 * the 'location' field.
			 */
			public void gotValue(LiveExpression<ValidationResult> exp, ValidationResult value) {
				if (value.isOk()) {
					updateRootFolderHistory();
				}
			}
		});
	}

	String[] getRootFolderHistory() {
		IDialogSettings settings = GrailsUiActivator.getDefault().getDialogSettings(); 
		String [] hist = settings.getArray(GRAILS_IMPORT_LOCATION_HIST);
		if (hist==null) {
			hist = new String[0];
		}
		return hist;
	}
	
	void setRootFolderHistory(String[] entries) {
		IDialogSettings settings = GrailsUiActivator.getDefault().getDialogSettings();
		settings.put(GRAILS_IMPORT_LOCATION_HIST, entries);
	}

	File openFileDialog(String initialSelection) {
		DirectoryDialog fileDialog = new DirectoryDialog(page.getShell(),  SWT.OPEN);
		fileDialog.setFilterPath(initialSelection);
		String file = fileDialog.open();
		if (file!=null) {
			return new File(file);
		}
		return null;
	}


	
}