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
package org.grails.ide.eclipse.ui.internal.importfixes;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;


/**
 * An instance of this class is responsible for fixing the output folder of a Grails project
 * if it overlaps with the output folder used by grails war command.
 * <p>
 * This functionality is hooked and triggered in two ways:
 *   - called from GrailsProjectVersionFixer
 *   - makes a scan of existing grails projects in the workspace when it starts.
 *  
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class GrailsOutputFolderFixer {
	
	/**
	 * The project relative path that is used by the grails war command as its output folder.
	 */
	private static final IPath GRAILS_WAR_OUTPUT_FOLDER = new Path("web-app/WEB-INF/classes");

	public GrailsOutputFolderFixer(GrailsProjectVersionFixer versionFixer) {
		versionFixer.setGrailsOutputFolderFixer(this); //Allows version fixer to call us when new grails projects are detected.
		scanExistingGrailsProjects();
	}
	
	/**
	 * This method is public *only* for testing purposes. It shouldn't be called directly from
	 * outside this class.
	 */
	public void scanExistingGrailsProjects() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject p : projects) {
			if (GrailsNature.looksLikeGrailsProject(p)) {
				fix(p);
			}
		}
	}

	public void dispose() {
	}

	/**
	 * Called by project version fixer when it detects a new grails project in the workspace.
	 * <p>
	 * This is called after the version fixer has already applied its fixer logic. (But beware that
	 * the fixer actually shedules most of its work as jobs so the actually fixes probably
	 * have not yet been applied.
	 */
	public void fix(IProject project) {
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				final IJavaProject javaProject = JavaCore.create(project);
				IPath outputFolder = javaProject.getOutputLocation();
				if (isBadOutputFolder(outputFolder)) {
					WorkspaceJob job = new WorkspaceJob("Fix output folder") {
						@Override
						public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
							try {
								//We recheck the fixing condition because, it may have changed by the time the job is scheduled.
								//This is because version fixer in some cases calls eclipsify (also from a scheduled job) and
								//this also fixes the output folder. It is probably harmless to fix twice, but it does
								//cause a potential build, so it is best not to do it.
								IPath outputFolder = javaProject.getOutputLocation();
								if (isBadOutputFolder(outputFolder)) {
									GrailsCommandUtils.setDefaultOutputFolder(javaProject);
								}
								return Status.OK_STATUS;
							} catch (Exception e) {
								return new Status(IStatus.WARNING, GrailsCoreActivator.PLUGIN_ID, "Error while fixing output folder for project "+javaProject.getElementName());
							}
						}
					};
					job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
					job.setPriority(Job.INTERACTIVE);
					job.schedule();
				}
			}
		} catch (Exception e) {
			GrailsCoreActivator.log(e);
		}
	}

	
	private boolean isBadOutputFolder(IPath outputFolder) {
		return outputFolder.removeFirstSegments(1).equals(GRAILS_WAR_OUTPUT_FOLDER);
	}
	
}
