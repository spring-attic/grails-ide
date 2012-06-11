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
package org.eclipse.jdt.debug.testplugin;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Test tab contribution to an existing tab group.
 * @author Andrew Eisenberg
 * @since 3.3
 */
public class JavaAlernateModeTab extends AbstractLaunchConfigurationTab implements ILaunchConfigurationListener {
	
	private Button fAlternateModeCheckBox;

	/** Returns the set of modes this tab supports
	 * @return the set of modes this tab supports
	 */
	public Set getModes() {
		HashSet modes = new HashSet();
		modes.add("alternate");
		return modes;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return "Alternate";
	}
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
		DebugPlugin.getDefault().getLaunchManager().removeLaunchConfigurationListener(this);
		super.dispose();
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationAdded(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationAdded(ILaunchConfiguration configuration) {}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationRemoved(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationRemoved(ILaunchConfiguration configuration) {}	

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		launchConfigurationChanged(configuration);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		if (fAlternateModeCheckBox.getSelection()) {
			configuration.addModes(getModes());
		} else {
			configuration.removeModes(getModes());
		}

	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		fAlternateModeCheckBox = new Button(parent, SWT.CHECK);
		fAlternateModeCheckBox.setText("Check &me for 'alternate' mode");
		fAlternateModeCheckBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
		setControl(fAlternateModeCheckBox);
		DebugPlugin.getDefault().getLaunchManager().addLaunchConfigurationListener(this);
	}
	
	/**
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
	 */
	public String getId() {
		return "org.eclipse.jdt.debug.tests.javaAlternateModeTab";
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationChanged(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationChanged(final ILaunchConfiguration configuration) {
		if(DebugUIPlugin.getStandardDisplay().getThread().equals(Thread.currentThread())) {
			setWidget(configuration);
		}
		else {
			DebugUIPlugin.getStandardDisplay().syncExec(new Runnable() {
				public void run() {
					setWidget(configuration);
				}
			});
		}
	}
	
	/**
	 * handles setting the checked state of the widget
	 * must check if we are in the UI thread before calling this method, as the launch ocnfiguration 
	 * notification can come from the non-UI tread.
	 * @param configuration
	 */
	private void setWidget(ILaunchConfiguration configuration) {
		try {
			Set modes = configuration.getModes();
			modes.add(getLaunchConfigurationDialog().getMode());
			if(!fAlternateModeCheckBox.isDisposed()) {
				fAlternateModeCheckBox.setSelection(modes.contains("alternate"));
			}
		}
		catch(CoreException ce) {DebugUIPlugin.log(ce);}
	}

}
