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
package org.grails.ide.eclipse.maven.ui;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.codeassist.ThrownExceptionFinder;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.wizards.IWizardDescriptor;
import org.eclipse.ui.wizards.IWizardRegistry;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.eclipse.m2e.core.ui.internal.wizards.MavenImportWizard;
import org.eclipse.m2e.core.ui.internal.wizards.MavenImportWizardPage;

@SuppressWarnings("restriction")
public abstract class GrailsM2EUtils {
	
	private static GrailsM2EUtils instance;

	public static GrailsM2EUtils getInstance() {
		if (instance==null) {
			boolean m2eInstalled = Platform.getBundle("org.eclipse.m2e.core") != null;
			try {
				if (m2eInstalled) {
					instance = new GrailsM2EUtilsImplementation();
				}
			} catch (Throwable e) {
				GrailsCoreActivator.log(e);
			}
			if (instance==null) {
				//Make sure it is never null, always at least have dummy implementation no matter
				//what might have gone wrong.
				instance = new GrailsM2EUtilsDummy();
			}
		}
		return instance;
	}

	public abstract boolean isInstalled();
	public abstract void openM2EImportWizard(File rootFolderlocation);
	
	/**
	 * The 'real' implementation of this class.
	 */
	@SuppressWarnings("restriction")
	public static class GrailsM2EUtilsImplementation extends GrailsM2EUtils {

		@Override
		public boolean isInstalled() {
			return true;
		}

		private IWizardRegistry getWizardRegistry() {
			return WorkbenchPlugin.getDefault()
					.getImportWizardRegistry();
		}
		
		@Override
		public void openM2EImportWizard(File rootFolderlocation) {
			try {
				String wizardId = "org.eclipse.m2e.core.wizards.Maven2ImportWizard";
				IWizardRegistry wizardRegistry = getWizardRegistry();
				IWizardDescriptor wizardDescriptor = wizardRegistry
						.findWizard(wizardId);
				if (wizardDescriptor != null) {
					IWorkbenchWizard wizard = wizardDescriptor.createWizard();
					
					///Hack!! We will try to tell the wizard some inital value to show for location
					try {
						MavenImportWizard mvnWizard = (MavenImportWizard) wizard;
						Field locField = MavenImportWizard.class.getDeclaredField("locations");
						locField.setAccessible(true);
						locField.set(mvnWizard, Collections.singletonList(rootFolderlocation.toString()));
					} catch (Throwable e) {
						//Ignore failed hack. Wizard will still open, won't have project location but
						//that's still better than not opening at all.
					}
					
					IWorkbench workbench = PlatformUI.getWorkbench();
					wizard.init(workbench, new StructuredSelection());
					Shell parent = workbench.getActiveWorkbenchWindow().getShell();
					WizardDialog dialog = new WizardDialog(parent, wizard);
					dialog.create();
					dialog.open();

				} else {
					throw new CoreException(new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Unknown wizard: "+wizardId));
				}
			} catch (Throwable e) {
				GrailsCoreActivator.log(e);
			}
		}
	}
	
	/**
	 * Dummy implementation, the methods in here simply do nothing.
	 * This implementation will be active when m2e is not installed.
	 */
	public static class GrailsM2EUtilsDummy extends GrailsM2EUtils {

		@Override
		public boolean isInstalled() {
			return false;
		}

		@Override
		public void openM2EImportWizard(File rootFolderlocation) {
		}
	}
}
