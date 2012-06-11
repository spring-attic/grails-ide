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
package org.grails.ide.eclipse.ui.internal.properties;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.model.DefaultGrailsInstall;
import org.grails.ide.eclipse.core.model.IGrailsInstall;

import org.grails.ide.eclipse.ui.GrailsUiActivator;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Kris De Volder
 */
public class GrailsInstallPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private InstalledGrailsInstallBlock fJREBlock;

	public GrailsInstallPreferencePage() {
		super("Grails Installations");
	}

	public void init(IWorkbench workbench) {
	}

	protected Control createContents(Composite ancestor) {
		initializeDialogUnits(ancestor);

		noDefaultAndApplyButton();

		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		ancestor.setLayout(layout);

		SWTFactory
				.createWrapLabel(
						ancestor,
						"Add, edit or remove Grails installations. By default the checked Grails installation is addded to the build path of newly created Grails projects.",
						1, 300);
		SWTFactory.createVerticalSpacer(ancestor, 1);

		fJREBlock = new InstalledGrailsInstallBlock();
		fJREBlock.createControl(ancestor);

		Control control = fJREBlock.getControl();
		GridDataFactory.fillDefaults().grab(true, true).applyTo(control);

		fJREBlock.restoreColumnSettings(GrailsUiActivator.getDefault().getDialogSettings(),
				"org.grails.ide.eclipse.ui.dialogsettings");

		fJREBlock.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				setValid(checkValid());
			}
		});
		
//		SWTFactory.createVerticalSpacer(ancestor, 1);
		
		applyDialogFont(ancestor);
		setValid(checkValid());
		return ancestor;
	}

	public boolean checkValid() {
		if (getCurrentDefaultVM() == null && fJREBlock.getJREs().length > 0) {
			setErrorMessage("Select a default Grails installation");
			return false;
		} else {
			for (IGrailsInstall install : fJREBlock.getJREs()) {
				IStatus status = install.verify();
				if (!status.isOK()) {
					setErrorMessage(status.getMessage());
					return false;
				}
			} 
		}
		setErrorMessage(null);
		return true;
	}

	public boolean performOk() {
		final boolean[] canceled = new boolean[] { false };
		BusyIndicator.showWhile(null, new Runnable() {
			public void run() {
				Set<IGrailsInstall> newInstalls = new LinkedHashSet<IGrailsInstall>();
				IGrailsInstall defaultVM = getCurrentDefaultVM();
				IGrailsInstall[] vms = fJREBlock.getJREs();
				for (IGrailsInstall vm : vms) {
					newInstalls.add(new DefaultGrailsInstall(vm.getHome(), vm.getName(), vm.equals(defaultVM)));
				}

				GrailsCoreActivator.getDefault().getInstallManager().setGrailsInstalls(newInstalls);
			}
		});

		if (canceled[0]) {
			return false;
		}

		// save column widths
		IDialogSettings settings = GrailsUiActivator.getDefault().getDialogSettings();
		fJREBlock.saveColumnSettings(settings, "org.grails.ide.eclipse.ui.dialogsettings");
		
		
		return super.performOk();
	}

	private IGrailsInstall getCurrentDefaultVM() {
		return fJREBlock.getCheckedJRE();
	}
}
