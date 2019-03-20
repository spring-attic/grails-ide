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
package org.grails.ide.eclipse.groovy.debug.ui.preferences;

import org.eclipse.contribution.jdt.IsWovenTester;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.grails.ide.eclipse.groovy.debug.core.preferences.ExtendedDebugPreferenceConstants;
import org.grails.ide.eclipse.groovy.debug.ui.GroovyDebugUIActivator;
import org.osgi.framework.Bundle;


/**
 * @author Andrew Eisenberg
 * @since 2.5.1
 */
public class ExtendedDebugPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public ExtendedDebugPreferencePage() {
		super(GRID);
		setPreferenceStore(GroovyDebugUIActivator.getDefault().getCorePreferenceStore());
		setDescription("Extended debug support for Groovy applications");
	}
	
	public void createFieldEditors() {
		
		if (!IsWovenTester.isWeavingActive()) {
			Label l = new Label(getFieldEditorParent(), SWT.NONE);
			l.setText("\n\nWarning: Extended debug support not available because JDT Weaving is not enabled.\n\n");
		}
		
		Bundle bundle = Platform.getBundle("org.eclipse.jdt.core");
        if (bundle != null && bundle.getVersion().getMinor() < 6) {
            Label l = new Label(getFieldEditorParent(), SWT.NONE);
            l.setText("\n\nWarning: Extended debug support not available in STS built on top of Eclipse 3.5 or earlier.\n" +
            		"Please upgrade to enable this feature.\n\n");
		}
		addField(new BooleanFieldEditor(ExtendedDebugPreferenceConstants.ENABLE_EXTRA_STEP_FILTERING, 
				"Enable extra step filtering for debugging Groovy apps.\n" +
				"This allows you to 'Step into' Groovy methods and closures.", getFieldEditorParent()));
		addField(new BooleanFieldEditor(ExtendedDebugPreferenceConstants.ENABLE_GROOVY_DISPLAY_VIEW, 
		        "Enable Groovy syntax in the Display and Expressions views\n" +
		        "(for groovy stack frames only)", getFieldEditorParent()));
		addField(new BooleanFieldEditor(ExtendedDebugPreferenceConstants.ENABLE_EXTRA_STEP_FILTERING_ON_ALL, 
		        "Enable extra Groovy debug support on *all* projects (not just Groovy projects).", getFieldEditorParent()));
	}

	public void init(IWorkbench workbench) {
	}
	
}