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
package org.grails.ide.eclipse.refactoring.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class GrailsRefactoringPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {
	
	private IWorkbench workbench;
	private Button warnNonGrailsAware;

	public GrailsRefactoringPreferencesPage() {
		super("Refactoring", null);
	}

	public void init(IWorkbench workbench) {
		this.workbench = workbench;
	}

	@Override
	protected Control createContents(Composite _parent) {
		Composite parent = new Composite(_parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		
		warnNonGrailsAware = new Button(parent, SWT.CHECK);
		warnNonGrailsAware.setText("Warn about non-grails aware refactoring");
		warnNonGrailsAware.setToolTipText("Popup a warning dialog when a non-grails aware refactoring of Grails elements is detected.");
		warnNonGrailsAware.setSelection(GrailsRefactoringPreferences.getWarnNonGrailsAware());
		
		return parent;
	}
	
	@Override
	public boolean performOk() {
		GrailsRefactoringPreferences.setWarnNonGrailsAware(warnNonGrailsAware.getSelection());
		return super.performOk();
	}

}
