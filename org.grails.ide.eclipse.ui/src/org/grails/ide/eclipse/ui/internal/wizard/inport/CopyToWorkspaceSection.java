package org.grails.ide.eclipse.ui.internal.wizard.inport;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.grails.ide.eclipse.core.wizard.GrailsImportWizardCore;
import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;
import org.springsource.ide.eclipse.commons.livexp.core.ValidationResult;
import org.springsource.ide.eclipse.commons.livexp.ui.WizardPageSection;

public class CopyToWorkspaceSection extends WizardPageSection {

	private GrailsImportWizardCore model;

	public CopyToWorkspaceSection(GrailsImportWizardPage owner) {
		super(owner);
		model = owner.model;
	}

	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return model.copyToWorkspaceValidator;
	}

	@Override
	public void createContents(Composite page) {
		final Button checkBox = new Button(page, SWT.CHECK);
		checkBox.setText("Copy resources into workspace");
		checkBox.setSelection(model.copyToWorkspace.getValue());
		checkBox.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				model.copyToWorkspace.setValue(checkBox.getSelection());
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				model.copyToWorkspace.setValue(checkBox.getSelection());
			}
		});
	}

}
