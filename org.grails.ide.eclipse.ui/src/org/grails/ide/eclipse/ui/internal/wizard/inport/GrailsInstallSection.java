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

import java.util.Collections;
import java.util.Set;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.core.model.IGrailsInstallListener;
import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;
import org.springsource.ide.eclipse.commons.livexp.core.ValidationResult;
import org.springsource.ide.eclipse.commons.livexp.core.ValueListener;
import org.springsource.ide.eclipse.commons.livexp.ui.WizardPageSection;

public class GrailsInstallSection extends WizardPageSection {

	private GrailsImportWizardPage page;
	private Combo grailsInstallCombo;
	private InstallChangeListener installListener;
	
	public GrailsInstallSection(GrailsImportWizardPage page) {
		super(page);
		this.page = page;
		this.installListener = new InstallChangeListener();
		GrailsCoreActivator.getDefault().getInstallManager().addGrailsInstallListener(installListener);
	}
	
	private class InstallChangeListener implements IGrailsInstallListener {
		public void installsChanged(Set<IGrailsInstall> installs) {
			// Make sure that we run the refresh in the UI thread
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					refreshInstalls();
				}
			});
		}
		public void defaultInstallChanged(IGrailsInstall oldDefault,
				IGrailsInstall newDefault) {
			//We don't care.
		}
	}
	
	@Override
	public void dispose() {
		GrailsCoreActivator.getDefault().getInstallManager().removeGrailsInstallListener(installListener);
	}
	
	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return page.model.installValidator;
	}

//	private GridLayout initGridLayout(GridLayout layout, boolean margins) {
//		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
//		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
//		if (margins) {
//			layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
//			layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
//		}
//		else {
//			layout.marginWidth = 0;
//			layout.marginHeight = 0;
//		}
//		return layout;
//	}
	
	private void openPreferences() {
		String id = "org.grails.ide.eclipse.ui.preferencePage";
		PreferencesUtil.createPreferenceDialogOn(page.getShell(), id, new String[] { id }, Collections.EMPTY_MAP).open();
	}
	
	
	@Override
	public void createContents(Composite composite) {
		//Composite grailsHomeGroup = new Composite(composite, SWT.NONE);
		//grailsHomeGroup.setFont(composite.getFont());
		//grailsHomeGroup.setText("Grails Installation");
		//grailsHomeGroup.setLayout(new GridLayout(1, false));
		//GridDataFactory.fillDefaults().grab(true, false).applyTo(grailsHomeGroup);

		final Composite installComposite = new Composite(composite, SWT.NULL);
		installComposite.setFont(composite.getFont());
		installComposite.setLayout(new GridLayout(3, false));
		installComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label options = new Label(installComposite, SWT.WRAP);
		options.setText("Install: ");
		options.setLayoutData(new GridData(GridData.BEGINNING));

		grailsInstallCombo = new Combo(installComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
		grailsInstallCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		refreshInstalls();
		
		//grailsHomeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Link link = new Link(installComposite, SWT.NONE);
		link.setFont(composite.getFont());
		link.setText("<A>Configure Grails Installations....</A>"); //$NON-NLS-1$//$NON-NLS-2$
		link.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		link.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				openPreferences();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				openPreferences();
			}
		});
		grailsInstallCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				page.model.grailsInstall.setValue(getInstallFromUI());
			}
		});
		page.model.grailsInstall.addListener(new ValueListener<IGrailsInstall>() {
			public void gotValue(LiveExpression<IGrailsInstall> exp, IGrailsInstall value) {
				if (value!=null) {
					grailsInstallCombo.setText(value.getName());
				} else {
					grailsInstallCombo.deselectAll();
				}
			}
		});
	}

	private IGrailsInstall getInstallFromUI() {
		if (grailsInstallCombo!=null) {
			return GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(grailsInstallCombo.getText());
		}
		return null;
	}
	
	private void refreshInstalls() {
		if (grailsInstallCombo!=null) {
			IGrailsInstall selectedInstall = page.model.grailsInstall.getValue();
			String selectedName = selectedInstall==null?null:selectedInstall.getName();
			grailsInstallCombo.setItems(GrailsCoreActivator.getDefault().getInstallManager().getAllInstallNames());
			String[] names = grailsInstallCombo.getItems();
			for (int i = 0; i < names.length; i++) {
				if (names[i].equals(selectedName)) {
					grailsInstallCombo.select(i);
					break;
				}
			}
		}
	}

}
