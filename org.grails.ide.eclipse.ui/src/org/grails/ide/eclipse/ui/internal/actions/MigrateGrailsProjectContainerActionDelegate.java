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

/**
 * @author Christian Dupuis
 * @since 2.2.0
 */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;


/**
 * Action invoked by the "Configure >> Convert to Grails Project" menu. 
 * @author Christian Dupuis
 * @author Kris De Volder
 */
public class MigrateGrailsProjectContainerActionDelegate implements IObjectActionDelegate {

	private final List<IProject> selected = new ArrayList<IProject>();

	public void run(IAction action) {
		if (selected!=null && selected.size()>0) {
			WorkspaceJob job = new WorkspaceJob("Configuring Grails Dependency Management") {
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
					if (monitor==null) {
						monitor = new NullProgressMonitor();
					}
					monitor.beginTask("Converting projects to Grails projects", selected.size());
					try {
						for (IProject project: selected) {
							monitor.setTaskName("Converting project "+project.getName()+" to Grails");
							GrailsCommandUtils.eclipsifyProject(project);
							monitor.worked(1);
							if (monitor.isCanceled()) {
								return Status.CANCEL_STATUS;
							}
						}
					} finally {
						monitor.done();
					}
					return Status.OK_STATUS;
				}
			}; 
			job.setPriority(Job.BUILD);
			job.schedule();
		}
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

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}
	
}
