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
package org.grails.ide.eclipse.ui.internal.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathContainer;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathUtils;
import org.grails.ide.eclipse.core.workspace.GrailsClassPath;
import org.grails.ide.eclipse.core.workspace.GrailsProject;
import org.grails.ide.eclipse.core.workspace.GrailsWorkspace;
import org.springsource.ide.eclipse.commons.core.JdtUtils;


/**
 * @author Christian Dupuis
 * @author Andrew Eisenberg
 * @since 2.2.0
 */
public class EnableGrailsClasspathContainerActionDelegate implements IObjectActionDelegate {

	private final List<IProject> selected = new ArrayList<IProject>();

	public void run(IAction action) {
		final Set<IJavaProject> projects = new LinkedHashSet<IJavaProject>();
		Iterator<IProject> iter = selected.iterator();
		while (iter.hasNext()) {
			IProject project = iter.next();
			projects.add(JdtUtils.getJavaProject(project));
		}

		WorkspaceJob job = new WorkspaceJob("Configuring Grails Dependency Management") {
			
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				for (IJavaProject javaProject : projects) {
					try {
						if (GrailsClasspathUtils.hasClasspathContainer(javaProject)) {
							removeFromClasspath(javaProject, JavaCore.newContainerEntry(
									GrailsClasspathContainer.CLASSPATH_CONTAINER_PATH, null, null, false),
									new NullProgressMonitor());
						}
						else {
							addToClasspath(javaProject, JavaCore.newContainerEntry(
									GrailsClasspathContainer.CLASSPATH_CONTAINER_PATH, null, null, false),
									new NullProgressMonitor());
						}
					}
					catch (CoreException e) {
						GrailsCoreActivator.log(e);
					}
				}
				return Status.OK_STATUS;
			}
		}; 
		job.setPriority(Job.BUILD);
		job.schedule();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		selected.clear();
		if (selection instanceof IStructuredSelection) {
			boolean enabled = true;
			Iterator<?> iter = ((IStructuredSelection) selection).iterator();
			while (iter.hasNext()) {
				Object obj = iter.next();
				if (obj instanceof IJavaProject) {
					obj = ((IJavaProject) obj).getProject();
				}
				if (obj instanceof IProject) {
					IProject project = (IProject) obj;
					if (!project.isOpen()) {
						enabled = false;
						break;
					}
					else {
						selected.add(project);
					}
				}
				else {
					enabled = false;
					break;
				}
			}
			action.setEnabled(enabled);
		}
	}

	private void addToClasspath(IJavaProject javaProject, IClasspathEntry entry, IProgressMonitor monitor)
			throws CoreException {
		GrailsProject project = GrailsWorkspace.get().create(javaProject);
		GrailsClassPath current = project.getClassPath();
		current.add(entry);
		project.setClassPath(current, monitor);
	}

	/**
	 * Removes plugin source folders and the grails classpath container from the classpath.
	 */
	protected void removeFromClasspath(IJavaProject javaProject, IClasspathEntry entry, IProgressMonitor monitor)
			throws CoreException {
		GrailsProject project = GrailsWorkspace.get().create(javaProject);
		GrailsClassPath current = project.getClassPath();
		current.removeGrailsClasspathContainer();
		current.removePluginSourceFolders();
		project.setClassPath(current, monitor);
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

}
