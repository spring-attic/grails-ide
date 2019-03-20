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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.grails.ide.eclipse.core.wizard.GrailsImportWizardCore;
import org.grails.ide.eclipse.maven.ui.GrailsM2EUtils;
import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;
import org.springsource.ide.eclipse.commons.livexp.core.ValidationResult;
import org.springsource.ide.eclipse.commons.livexp.core.Validator;
import org.springsource.ide.eclipse.commons.livexp.core.ValueListener;
import org.springsource.ide.eclipse.commons.livexp.ui.CommentSection;
import org.springsource.ide.eclipse.commons.livexp.ui.GroupSection;
import org.springsource.ide.eclipse.commons.livexp.ui.WizardPageSection;

public class MavenWarningSection extends GroupSection {
	
	private static final GrailsM2EUtils m2e = GrailsM2EUtils.getInstance();
	
	private static class IgnoreMavenWarning extends WizardPageSection {

		private String msg;
		private final GrailsImportWizardCore model;

		public IgnoreMavenWarning(GrailsImportWizardPage owner, String msg) {
			super(owner);
			this.msg = msg;
			this.model = owner.model;
		}

		@Override
		public LiveExpression<ValidationResult> getValidator() {
			return model.mavenValidator;
		}

		@Override
		public void createContents(Composite page) {
			final Button checkBox = new Button(page, SWT.CHECK);
			checkBox.setSelection(model.ignoreMavenWarning.getValue());
			checkBox.setText(msg);
			checkBox.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					model.ignoreMavenWarning.setValue(checkBox.getSelection());
				}
			});
		}
	}	
	
	public MavenWarningSection(GrailsImportWizardPage owner, String title) {
		super(owner, title,
				new CommentSection(owner, 
						"The selected project looks like a Mavenized project and should be imported using m2e's " +
						"'Import Existing Maven Projects' wizard. If you proceed the project may not be imported correctly."),
				new IgnoreMavenWarning(owner, "Ignore this warning and allow me to proceed"),	
				new OpenM2EImportWizardSection(owner)
		);
		
		//Hide this entire section if it does not apply:
		owner.model.isMaven.addListener(new ValueListener<Boolean>() {
			public void gotValue(LiveExpression<Boolean> exp, Boolean isMaven) {
				isVisible.setValue(isMaven);
			}
		});
	}
	
	private static class OpenM2EImportWizardSection extends WizardPageSection {
		
		private GrailsImportWizardPage page;
		private GrailsImportWizardCore model;

		public OpenM2EImportWizardSection(GrailsImportWizardPage page) {
			super(page);
			this.page = page;
			this.model = page.model;
		}

		private void openWizard() {
			owner.getShell().close();
			m2e.openM2EImportWizard(model.location.getValue());
		}
		
		@Override
		public LiveExpression<ValidationResult> getValidator() {
			return Validator.constant(ValidationResult.OK);
		}

		@Override
		public void createContents(Composite page) {
			if (m2e.isInstalled()) {
				Button button = new Button(page, SWT.PUSH);
				button.setText("Switch to Maven Import");
				button.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						openWizard();
					}
				});
			} else {
				Label label = new Label(page, SWT.WRAP);
				label.setText("Can't open M2E Wizard because M2E is not installed.");
			}
		}
		
	}
}
