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
package org.grails.ide.eclipse.ui.internal.utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Display;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.GrailsCommandAdapter;
import org.grails.ide.eclipse.core.model.IGrailsCommandResourceChangeListener;
import org.springsource.ide.eclipse.commons.core.JdtUtils;
import org.springsource.ide.eclipse.commons.ui.SpringUIUtils;


/**
 * {@link IGrailsCommandResourceChangeListener} that open new file resources in an Eclipse editor.
 * @author Christian Dupuis
 * @author Nieraj Singh
 * @since 2.2.1
 */
public class OpenNewResourcesCommandListener extends GrailsCommandAdapter {

	private final IProject project;

	public OpenNewResourcesCommandListener(IProject project) {
		this.project = project;
	}
	
	public IProject getProject() {
		return project;
	}

	public void newResource(final IResource resource) {
		if (resource instanceof IFile && resource.getName().endsWith(".groovy")
				&& !resource.getName().endsWith("Tests.groovy")) {
			// Only open resource if is in any source folder
			IJavaProject jp = JdtUtils.getJavaProject(project);
			if (jp != null) {
				try {
					for (IClasspathEntry entry : jp.getRawClasspath()) {
						if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
							if (entry.getPath() != null && entry.getPath().isPrefixOf(resource.getFullPath())) {
								Display.getDefault().asyncExec(new Runnable() {
									
									public void run() {
										SpringUIUtils.openInEditor((IFile) resource, -1);
									}
								});
								break;
							}
						}
					}
				}
				catch (JavaModelException e) {
				}
			}
		}
	}

	@Override
	public void finish() {
		GrailsCoreActivator.getDefault().removeGrailsCommandResourceListener(this);
	}

	public boolean supports(IProject project) {
		return this.project.equals(project);
	}

}
