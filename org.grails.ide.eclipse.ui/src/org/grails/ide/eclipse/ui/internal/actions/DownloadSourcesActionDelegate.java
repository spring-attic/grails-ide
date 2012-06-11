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

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathContainer;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathUtils;
import org.grails.ide.eclipse.core.internal.classpath.PerProjectAttachementsCache;
import org.grails.ide.eclipse.core.util.JobUtil;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.utils.ProjectFilter;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.utils.SelectionUtils;


public class DownloadSourcesActionDelegate implements IObjectActionDelegate {

	private List<IProject> selected;

	public void selectionChanged(IAction action, ISelection selection) {
		selected = SelectionUtils.getProjects(selection, new ProjectFilter() {
			@Override
			public boolean isAcceptable(IProject project) {
				return GrailsNature.isGrailsProject(project);
			}
		});
		boolean enabled = !selected.isEmpty();
		action.setEnabled(enabled);
	}

	public void run(IAction action) {
		for (final IProject project : selected) {
			JobUtil.schedule(new Job("Download source jars") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Download source jars", 2);
					try {
						doit(project, monitor);
					} catch (CoreException e) {
						return new Status(e.getStatus().getSeverity(), GrailsCoreActivator.PLUGIN_ID, e.getMessage(), e);
					} finally {
						monitor.done();
					}
					return Status.OK_STATUS;
				}

			});
		}
	}

	public static void doit(final IProject project, IProgressMonitor monitor) throws CoreException, JavaModelException {
		IJavaProject javaProject = JavaCore.create(project);
		GrailsCommandFactory.downloadSourceJars(project).synchExec();
		monitor.worked(1);
		PerProjectAttachementsCache.get(project).refreshData();
		GrailsClasspathContainer cpc = GrailsClasspathUtils.getClasspathContainer(javaProject);
		cpc.invalidate();
		JavaCore.setClasspathContainer(cpc.getPath(),new IJavaProject[] {javaProject}, new IClasspathContainer[] {null}, monitor);
//		javaProject.setRawClasspath(javaProject.getRawClasspath(), new SubProgressMonitor(monitor, 1));
	}
	
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		//Ignore
	}


}
