/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.wizard;

import static org.springsource.ide.eclipse.commons.livexp.core.ValidationResult.error;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.springsource.ide.eclipse.commons.livexp.core.CompositeValidator;
import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;
import org.springsource.ide.eclipse.commons.livexp.core.LiveVariable;
import org.springsource.ide.eclipse.commons.livexp.core.ValidationResult;
import org.springsource.ide.eclipse.commons.livexp.core.Validator;
import org.springsource.ide.eclipse.commons.livexp.core.ValueListener;

/**
 * Core counterpart of {@link GrailsImportWizard} in the ui plugin. 
 * <p>
 * Contains code to validate wizard contents and execute the import operation.
 * It makes use of 'LiveExpressions' to decouple this logic from the wizard's UI.
 * 
 * @author Kris De Volder
 */
public class GrailsImportWizardCore {

	///////////////////////////////////////////////////////////////////////////////////////////
	/// "inputs" where the user can enter data
	/// 
	/// In the UI there will be some widgetry to set the values of these things.
	/// But this 'core' wizard can also be used in headless mode. Anyone/thing can set the values.
	
	public final LiveVariable<File> location = new LiveVariable<File>(null);
	public final LiveVariable<Boolean> ignoreMavenWarning = new LiveVariable<Boolean>(false);
	public final LiveVariable<IGrailsInstall> grailsInstall = new LiveVariable<IGrailsInstall>(null);
	public final LiveVariable<Boolean> copyToWorkspace = new LiveVariable<Boolean>(false);
	
	///////////////////////////////////////////////////////////////////////////////////////////
	
	private boolean existsInWorkspace(String name) {
		if (name!=null) {
			return ResourcesPlugin.getWorkspace().getRoot().getProject(name).exists();
		}
		return false;
	}
	
	/**
	 * Location is valid if it looks like a Grails project exists at the location.
	 */
	public final LiveExpression<ValidationResult> locationValidator = new Validator() {
		protected ValidationResult compute() {
			final File rf = location.getValue();
			if (rf==null) {
				return error("Specify the location of a Grails project to import.");
			} else if (!rf.exists()) {
				return error("'"+rf+"' doesn't exist.");
			} else if (!rf.isDirectory()) {
				return error("'"+rf+"' is not a directory.");
			} else if (!GrailsNature.looksLikeGrailsProject(rf)) {
				return error("'"+rf+"' doesn't look like a Grails project");
			} else if (existsInWorkspace(rf.getName())) {
				return error("Project '"+rf.getName()+"' already exists in the workspace.");
			}
			return ValidationResult.OK;
		}

	}
	.dependsOn(location);

	/**
	 * Compute the target location where the imported project should be copied to.
	 * If the option to copy resources into the workspace is not selected this will return null.
	 * It may also return null if other pre-requisites to compute this have not yet been 
	 * filled in (e.g. if a project to import has yet to be selected).
	 */
	public File getCopyLocation() {
		if (copyToWorkspace.getValue()) {
			File loc = location.getValue();
			if (loc!=null) {
				String name = loc.getName();
				if (name!=null && !"".equals(name)) {
					return Platform.getLocation().append(name).toFile();
				}
			}
		}
		return null;
	}
	
	public LiveExpression<ValidationResult> copyToWorkspaceValidator = new Validator() {
		protected ValidationResult compute() {
			File targetLocation = getCopyLocation();
			if (targetLocation!=null && targetLocation.exists()) {
				return error("Can not copy project into workspace because '"+targetLocation+"' already exists");
			}
			return ValidationResult.OK;
		}
	}
	.dependsOn(copyToWorkspace)
	.dependsOn(location);
	
	public final LiveExpression<Boolean> isMaven = new LiveExpression<Boolean>(false) {
		@Override
		protected Boolean compute() {
			return locationValidator.getValue().isOk() && lookslikeMavenProject(location.getValue());
		}
	}
	.dependsOn(location)
	.dependsOn(locationValidator);
	
	private boolean lookslikeMavenProject(File l) {
		if (l!=null && l.isDirectory()) {
			File pom = new File(l, "pom.xml");
			return pom.isFile();
		}
		return false;
	}
	
	public final LiveExpression<GrailsVersion> projectGrailsVersion = new LiveExpression<GrailsVersion>(null) {
		protected GrailsVersion compute() {
			//It is slighly better to also check whether the project location is valid, otherwise we can't hope to
			//determine the version anyway.
			if (locationValidator.getValue().isOk()) {
				return GrailsVersion.getGrailsVersion(location.getValue());
			} else {
				return GrailsVersion.UNKNOWN;
			}
		}
	}
	.dependsOn(location)
	.dependsOn(locationValidator);
	
	/**
	 * 'maven' state is valid if either the selected project is not a maven project, or ignoreMavenWarning
	 * has been selected.
	 */
	public final LiveExpression<ValidationResult> mavenValidator = new Validator() {
		@Override
		protected ValidationResult compute() {
			if (isMaven.getValue()) {
				if (!ignoreMavenWarning.getValue()) {
					return error("Mavenized project");
				}
			}
			return ValidationResult.OK;
		}
	}
	.dependsOn(isMaven)
	.dependsOn(ignoreMavenWarning);

	/**
	 * To be valid, a Grails install must be selected that matches the Grails version of the selected project.
	 */
	public final LiveExpression<ValidationResult> installValidator = new Validator() {
		protected ValidationResult compute() {
			IGrailsInstall install = grailsInstall.getValue();
			if (install == null) {
				return error("No Grails install selected");
			} else {
				GrailsVersion installVersion = install.getVersion();
				GrailsVersion projectVersion = GrailsVersion.getGrailsVersion(location.getValue());
				if (projectVersion.equals(GrailsVersion.UNKNOWN)) {
					return error("Unable to determine Grails version for the project");
				} else if (!installVersion.equals(projectVersion)) {
					return error("Project Grails version ("+projectVersion+ ") does not match install version ("+installVersion+")");
				}
			}
			return ValidationResult.OK;
		};
	}
	.dependsOn(grailsInstall)
	.dependsOn(projectGrailsVersion);
	
	{
		/*
		 * If a project is selected and a matching Grails install is not, then try to automatically select 
		 * a matching Grails install.
		 */
		projectGrailsVersion.addListener(new ValueListener<GrailsVersion>() {
			public void gotValue(LiveExpression<GrailsVersion> exp, GrailsVersion projectVersion) {
				if (projectVersion!=null) {
					IGrailsInstall instl = grailsInstall.getValue();
					if (instl==null || !projectVersion.equals(instl.getVersion())) {
						instl = projectVersion.getInstall();
						if (instl!=null) {
							grailsInstall.setValue(instl);
						}
					}
				}
			}
		});
	}
	
	private boolean isDefaultProjectLocation(String projectName, File projectDir) {
		IPath workspaceLoc = Platform.getLocation();
		if (workspaceLoc!=null) {
			File defaultLoc = new File(workspaceLoc.toFile(), projectName);
			return defaultLoc.equals(projectDir);
		}
		return false;
	}
	
	public boolean perform(IProgressMonitor mon) throws CoreException {
		int totalWork = 2;
		File copyLoc = getCopyLocation();
		if (copyLoc!=null) {
			totalWork++;
		}
		mon.beginTask("Import", totalWork);
		try {
			//1: copy files to workspace
			if (copyLoc!=null) {
				FileUtils.copyDirectory(location.getValue(), copyLoc);
				mon.worked(1);
			}
			
			IWorkspace ws = ResourcesPlugin.getWorkspace();
			File projectDir = copyLoc!=null?copyLoc:location.getValue();
			String projectName = projectDir.getName();
	
			//2: create project
			IProjectDescription projectDescription = ws.newProjectDescription(projectName);
			if (!isDefaultProjectLocation(projectName, projectDir)) {
				projectDescription.setLocation(new Path(projectDir.getAbsolutePath()));
			}
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			project.create(projectDescription, new SubProgressMonitor(mon, 1));

			//3: configure project
			IGrailsInstall install = grailsInstall.getValue();
			if (install!=null) {
				File projectAbsoluteFile = location.getValue();
				if (projectAbsoluteFile!=null) {
					IPath projectAbsolutePath = new Path(projectAbsoluteFile.toString());
					GrailsCommandUtils.eclipsifyProject(grailsInstall.getValue(), false, projectAbsolutePath);
					return true;
				}
			}
			mon.worked(1);
			
			return false;
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Import failed", e));
		} finally {
			mon.done();
		}
	}

	public String getProjectName() {
		File loc = location.getValue();
		if (loc!=null) {
			return loc.getName();
		}
		return null;
	}
	
}
