/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.ui.internal.wizard.inport;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;
import org.springsource.ide.eclipse.commons.livexp.core.ValidationResult;
import org.springsource.ide.eclipse.commons.livexp.core.Validator;
import org.springsource.ide.eclipse.commons.livexp.core.ValueListener;
import org.springsource.ide.eclipse.commons.livexp.ui.WizardPageSection;

/**
 * Page section that simply displays the GrailsVersion associated with selected Grails project location.
 * 
 * @author Kris De Volder
 */
public class DisplayGrailsVersionSection extends WizardPageSection {

	private final GrailsImportWizardPage page;
	private LiveExpression<GrailsVersion> grailsVersion;

	public DisplayGrailsVersionSection(GrailsImportWizardPage owner) {
		super(owner);
		page = owner;
		grailsVersion = page.model.projectGrailsVersion;
	}
	
	@Override
	public void createContents(Composite page) {
		GridDataFactory grabHorizontal = GridDataFactory.fillDefaults().grab(true, false);

		Composite section = new Composite(page, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		section.setLayout(layout);
		grabHorizontal.applyTo(section);
		
		new Label(section, SWT.NONE).setText("Required Grails Version: ");
		final Label grailsVersionLabel = new Label(section, SWT.NONE);
		grailsVersion.addListener(new ValueListener<GrailsVersion>() {
			public void gotValue(LiveExpression<GrailsVersion> exp, GrailsVersion v) {
				grailsVersionLabel.setText(""+v);
			}
		});
		grabHorizontal.applyTo(grailsVersionLabel);
	}

	@Override
	public LiveExpression<ValidationResult> getValidator() {
		//Nothing to validate here. This is just a info display widget.
		return Validator.constant(ValidationResult.OK);
	}
	
}
