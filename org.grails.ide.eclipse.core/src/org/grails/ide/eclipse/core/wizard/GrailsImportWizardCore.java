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
package org.grails.ide.eclipse.core.wizard;

import static org.springsource.ide.eclipse.commons.livexp.core.ValidationResult.OK;
import static org.springsource.ide.eclipse.commons.livexp.core.ValidationResult.error;

import java.io.File;

import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
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
	/// inputs where the user can enter data
	
	public final LiveVariable<File> location = new LiveVariable<File>(null);
	public final LiveVariable<Boolean> ignoreMavenWarning = new LiveVariable<Boolean>(false);
	public final LiveVariable<IGrailsInstall> grailsInstall = new LiveVariable<IGrailsInstall>(null);

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
			}
			return OK;
		}
	}
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
	.dependsOn(location);
	
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
			return OK;
		}
	}
	.dependsOn(location)
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
			return OK;
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
		mon.beginTask("Import", 2);
		try {
			IWorkspace ws = ResourcesPlugin.getWorkspace();
			File projectDir = location.getValue();
			String projectName = projectDir.getName();
	
			//1: create project
			IProjectDescription projectDescription = ws.newProjectDescription(projectName);
			if (!isDefaultProjectLocation(projectName, projectDir)) {
				projectDescription.setLocation(new Path(projectDir.getAbsolutePath()));
			}
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			project.create(projectDescription, new SubProgressMonitor(mon, 1));

			//2: configure project
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
