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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.IgnoredProjectsList;
import org.grails.ide.eclipse.core.util.JobUtil;
import org.grails.ide.eclipse.core.wizard.GrailsImportWizardCore;
import org.grails.ide.eclipse.ui.GrailsUiActivator;

public class GrailsImportWizard extends Wizard implements IImportWizard {

	public static final ImageDescriptor WIZBAN_IMAGE = GrailsUiActivator.getImageDescriptor("icons/full/wizban/grails_wizban.png");
	
	private IWorkbench workbench;
	private GrailsImportWizardPageOne pageOne;
	//private GrailsImportWizardPageMaven mavenPage; //Removed: added this to the bottom of page one

	public final GrailsImportWizardCore model = new GrailsImportWizardCore();
	
	public GrailsImportWizard() {
	}
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
	}
	
	public void addPages() {
		super.addPages();
		addPage(getPageOne());
		//addPage(getMavenPage()); //This page should only be shown for mavenized projects.
	}
	
	public GrailsImportWizardPageOne getPageOne() {
		if (pageOne==null) {
			pageOne = new GrailsImportWizardPageOne(this);
		}
		return pageOne;
	}

//	public GrailsImportWizardPageMaven getMavenPage() {
//		if (mavenPage==null) {
//			mavenPage = new GrailsImportWizardPageMaven(this);
//		}
//		return mavenPage;
//	}

	@Override
	public boolean performFinish() {
		JobUtil.schedule(new Job("Import Grails Project") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					String projectName = model.getProjectName();
					if (projectName!=null) {
						IgnoredProjectsList.addIgnoredProject(projectName);
						try {
							model.perform(monitor);
							return Status.OK_STATUS;
						} finally {
							IgnoredProjectsList.removeIgnoredProject(projectName);
						}
					} else {
						return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Couldn't determine project name");
					}
				} catch (CoreException e) {
					GrailsCoreActivator.log(e);
					return e.getStatus();
				}
			}
			
		});
		return true;
	}

//	public IWizardPage getRegularPage() {
//		if (regularPage==null) {
//			regularPage = new GrailsImportWizardPageRegular(this);
//		}
//		return regularPage;
//	}
	
}
