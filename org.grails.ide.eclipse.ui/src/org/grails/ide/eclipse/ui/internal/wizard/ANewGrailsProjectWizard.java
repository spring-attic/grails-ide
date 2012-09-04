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
package org.grails.ide.eclipse.ui.internal.wizard;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.IgnoredProjectsList;
import org.grails.ide.eclipse.core.model.GrailsInstallManager;
import org.grails.ide.eclipse.core.model.IGrailsInstall;


/**
 * Abstract superclass for both "NewGrailsProject" and "NewGrailsPlugin"
 * wizards. Both wizards are nearly identical, except for the command that each
 * runs and a few titles and info texts that get displayed.
 * @author Kris De Volder
 * @author Nieraj Singh
 */
@SuppressWarnings("restriction")
public abstract class ANewGrailsProjectWizard extends NewElementWizard
		implements INewWizard, IExecutableExtension {

	private NewGrailsProjectWizardPageOne projectPage;
	private IConfigurationElement configElement;

	/**
	 * Factory method to create the command that this wizard executes.
	 */
	protected abstract GrailsCommand createCommand(IGrailsInstall install,
			String projectName);

	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		configElement = config;
	}

	protected abstract String getPageTitle();

	protected abstract String getPageDescription();

	@Override
	public void addPages() {
		if (isSubclipse()) {
			MessageDialog.open(MessageDialog.WARNING, getShell(), 
					"Warning! Your chosen method of checkout is unreliable!",
					"You asked Subclipse to configure a project using the New Grails Project Wizard.\n" +
					"This method is unlreliable because it requires the wizard to generate a project " +
					"configuration before its contents is checked out.\n" +
					"\n"+
					"The resulting project will likely be configured incorrectly.\n" +
					"\n"+
					"Please consider using 'Check out as project in the workspace instead'.\n"+
					"STS will detect a new Grails project after it has been checked out\n"+
					"and offer to configure it for you.",
					SWT.NONE);
		}
		projectPage = new NewGrailsProjectWizardPageOne(getPageTitle(), getPageDescription(), isPluginWizard());
		addPage(projectPage);
	}

	protected abstract boolean isPluginWizard();

    Boolean isSubclipse = null;
	private IJavaProject createdProject = null;
	   // Note this will be null, in normal use, only with subclipse will we hold
	   // off on closing the dialog until the job creating the project is finished.
	
	private boolean isSubclipse() {
		if (isSubclipse==null) {
			StringWriter stackTrace = new StringWriter();
			new Exception().printStackTrace(new PrintWriter(stackTrace));
			isSubclipse = stackTrace.toString().contains("org.tigris.subversion.subclipse");
		}
		return isSubclipse;
	}

	protected void finishPage(IProgressMonitor monitor)
			throws InterruptedException, CoreException {
	}

	@Override
	public boolean performFinish() {
	
		final IWorkingSet[] workingSets = projectPage.getWorkingSets();
		final String grailsInstall = projectPage.getGrailsInstallName();
		final boolean useDefault = projectPage.useDefaultGrailsInstall();

		final IGrailsInstall install = getGrailsInstall(useDefault,
				grailsInstall);
		if (install == null) {
		    GrailsCoreActivator.log("No default Grails install available.");
		    return false;
		}
		final String projectName = projectPage.getProjectName();
		final GrailsCommand command = createCommand(install,
				projectName);

		//Note: the call below will prompt user to switch to grails perspective. This call
		// MUST be made from the UI thread or will silently fail. Do NOT move this call inside of
		// the Job!
		BasicNewProjectResourceWizard.updatePerspective(configElement);

		// The next part takes too long to run in the UI thread, Schedule as a
		// Job
		final Job job = new Job(command.toString()) {
			@Override
			public IStatus run(IProgressMonitor monitor) {
				boolean isImport = false;
				monitor.beginTask("create-app", 2);
				IgnoredProjectsList.addIgnoredProject(projectName);
				try {
					URI location = projectPage.getProjectLocationURI();
					IProject project = null;
					if (location != null) {
						// Full path to the project location
						IPath projectPath = null;
						
						// Base directory where Grails should create
						// the project. If the project already exists
						// the base directory should be the same as the
						// project path.
						String baseDirectory = null;
						try {
							File file = new File(location);
							String locationPath = file.getCanonicalPath();
							projectPath = new Path(locationPath);
							
							// This means that a new project is being created
							if (!file.exists()) {
								// base directory is the parent, which the wizard should already
								// checked for accessibility. Grails requires an existing location
								// to create a project, therefore the parent should be used
								// as the base directory
								baseDirectory = projectPath.removeLastSegments(1).toString();
							} else {
								// This means that the project already exists, and
								// the user may be importing the contents
								baseDirectory = projectPath.toString();
								isImport = new File(new File(baseDirectory), "grails-app").exists();
							}
						} catch (IOException e1) {
							GrailsCoreActivator.log(e1.getLocalizedMessage(), e1);
							return createProjectFailedError();
						}
						if (!isImport) {
							command.setPath(baseDirectory);
							command.synchExec();
						}
						monitor.worked(1);
						
						project = GrailsCommandUtils.eclipsifyProject(install,
								useDefault, projectPath);
						
					} else { //location == null 
						File commandLocation = new File(command.getPath());
						File projectLocation = new File(commandLocation, projectName);
						isImport = new File(projectLocation, "grails-app").exists();
						if (!isImport) {
							command.synchExec();
						}
						monitor.worked(1);
						
						project = ResourcesPlugin
						.getWorkspace().getRoot()
						.getProject(projectName);
						
						GrailsCommandUtils.eclipsifyProject(install,
								useDefault, project);
					}

					monitor.worked(1);
					configureProjectUi(project, workingSets);
					
					if (project!=null) {
						createdProject = JavaCore.create(project);
					}
					
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return e.getStatus();
				} catch (IOException e) {
					GrailsCoreActivator.log(e);
					return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, e.getMessage());
				} finally {
					IgnoredProjectsList.removeIgnoredProject(projectName);
					monitor.done();
				}
			}
		};
		job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		job.setPriority(Job.BUILD);
		job.schedule();

		if (isSubclipse()) {
			// We need to wait for the job to finish or subclipse will not find the project and won't perform a checkout.
			try {
				PlatformUI.getWorkbench().getProgressService().runInUI(getContainer(), new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException 
					{
					}
				}, job.getRule() );
			} catch (InterruptedException e) {
				return false;
			} catch (InvocationTargetException e) {
				GrailsCoreActivator.log(e);
			}
		}
		
		return true;
	}
	
	protected IStatus createProjectFailedError() {
		return new Status(
				IStatus.ERROR,
				GrailsCoreActivator.PLUGIN_ID,
				IStatus.ERROR,
				"Unable to create project "
						+ projectPage.getProjectName()
						+ ". Check project location to make sure it exists and is accessible.",
				null);
	}

	private IGrailsInstall getGrailsInstall(boolean useDefault,
			String grailsInstallName) {
		GrailsInstallManager installMan = GrailsCoreActivator.getDefault()
				.getInstallManager();
		if (useDefault) {
			return installMan.getDefaultGrailsInstall();
		} else {
			return installMan.getGrailsInstall(grailsInstallName);
		}
	}

	/**
	 * This method returns the created element. In our wizard, it typically returns null,
	 * because we schedule the creation of the element as a background job. 
	 * <p>
	 * Only when isSubclipse() is true, will we wait for the job to finish before
	 * returning from the performFinish method.
	 */
	@Override
	public IJavaElement getCreatedElement() {
		return createdProject;
	}

	private void configureProjectUi(IProject project, IWorkingSet[] workingSets) {
		if (workingSets.length > 0) {
			PlatformUI.getWorkbench().getWorkingSetManager()
					.addToWorkingSets(project, workingSets);
		}
		BasicNewResourceWizard.selectAndReveal(project, PlatformUI
				.getWorkbench().getActiveWorkbenchWindow());
	}

}
