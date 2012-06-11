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
package org.grails.ide.eclipse.core.internal.classpath;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;


/**
 * @author Christian Dupuis
 * @author Kris De Volder
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 * @since 2.2.0
 */
public class GrailsClasspathContainerUpdateJob extends WorkspaceJob {

	/** Internal cache of scheduled and <b>unfinished</b> update jobs */
	private static final Queue<IJavaProject> SCHEDULED_PROJECTS = new ConcurrentLinkedQueue<IJavaProject>();

	/** The {@link IJavaProject} this jobs should refresh the class path container for */
	private final IJavaProject javaProject;

	/** Should the output of the associated process be shown in the Console view? */
	private boolean showOutput;

	/**
	 * Private constructor to create an instance
	 * @param javaProject the {@link IJavaProject} the class path container should be updated for
	 * @param showOutput 
	 * @param types the change types happened to the manifest
	 */
	private GrailsClasspathContainerUpdateJob(IJavaProject javaProject, boolean showOutput) {
		super("Updating Grails dependencies for project '" + javaProject.getElementName() + "'");
		this.javaProject = javaProject;
		this.showOutput = showOutput;
	}

	/**
	 * Returns the internal {@link IJavaProject}
	 */
	public IJavaProject getJavaProject() {
		return javaProject;
	}

	/**
	 * Runs the job in the context of the workspace. Simply delegates refreshing of the class path container to
	 * {@link GrailsCommandUtils#refreshDependencies(IJavaProject)}.
	 */
	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		IProject project = javaProject.getProject();
        if (!project.isOpen() || monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}
		
		// STS-2247 purge all caches for project
		GrailsCore.get().disconnectProject(project);
		
		if(!GrailsClasspathUtils.hasClasspathContainer(javaProject)) {
		    return new Status(IStatus.OK, GrailsCoreActivator.PLUGIN_ID, "Grails dependency management is not enabled.");
		}
		try {
			GrailsCommandUtils.refreshDependencies(javaProject, showOutput);
		}
		catch (CoreException e) {
			return e.getStatus();
		}
		catch (Throwable e) {
			return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Refresh dependecies failed", e);
		}

		return new Status(IStatus.OK, GrailsCoreActivator.PLUGIN_ID, "Updated Grails classpath container");
	}

	/**
	 * Helper method to schedule a new {@link GrailsClasspathContainerUpdateJob}.
	 * @param javaProject the {@link IJavaProject} the class path container should be updated for
	 * @param types the change types of the manifest
	 */
	public static void scheduleClasspathContainerUpdateJob(IJavaProject javaProject, boolean showOutput) {
		if (javaProject != null && !SCHEDULED_PROJECTS.contains(javaProject)) {
			newClasspathContainerUpdateJob(javaProject, showOutput);
		}
	}

	public static void scheduleClasspathContainerUpdateJob(IProject project, boolean showOutput) {
		scheduleClasspathContainerUpdateJob(JavaCore.create(project), showOutput);
	}

	/**
	 * Creates a new instance of {@link GrailsClasspathContainerUpdateJob} and configures required properties and
	 * schedules it to the workbench.
	 * @param showOutput 
	 */
	private static GrailsClasspathContainerUpdateJob newClasspathContainerUpdateJob(IJavaProject javaProject, boolean showOutput) {
		GrailsClasspathContainerUpdateJob job = new GrailsClasspathContainerUpdateJob(javaProject, showOutput);
		job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		job.setPriority(Job.BUILD);
		job.addJobChangeListener(new DuplicateJobListener());
		job.schedule();
		return job;
	}

	/**
	 * Internal {@link IJobChangeListener} to detect duplicates in the scheduled list of
	 * {@link GrailsClasspathContainerUpdateJob Jobs}.
	 */
	private static class DuplicateJobListener extends JobChangeAdapter implements IJobChangeListener {

		@Override
		public void done(IJobChangeEvent event) {
			SCHEDULED_PROJECTS.remove(((GrailsClasspathContainerUpdateJob) event.getJob()).getJavaProject());
		}

		@Override
		public void scheduled(IJobChangeEvent event) {
			SCHEDULED_PROJECTS.add(((GrailsClasspathContainerUpdateJob) event.getJob()).getJavaProject());
		}

	}

}
