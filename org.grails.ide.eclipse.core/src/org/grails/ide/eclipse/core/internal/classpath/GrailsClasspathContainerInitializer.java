/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.internal.classpath;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;


/**
 * @author Christian Dupuis
 * @author Kris De Volder
 * @since 2.2.0
 */
public class GrailsClasspathContainerInitializer extends ClasspathContainerInitializer {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
		//GroovyCompilerVersionCheck.check(javaProject);
		// Only responsible for our own class path container
		if (containerPath.equals(GrailsClasspathContainer.CLASSPATH_CONTAINER_PATH)) {

			// Retrieve persisted class path container
			IClasspathContainer oldContainer = GrailsClasspathUtils.getClasspathContainer(javaProject);

			GrailsClasspathContainer newContainer = null;

			if (oldContainer == null) {
				newContainer = new GrailsClasspathContainer(javaProject);
			}
			else {
				newContainer = new GrailsClasspathContainer(javaProject, oldContainer.getClasspathEntries());
			}

			// Install the class path container with the project
			JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { javaProject },
					new IClasspathContainer[] { newContainer }, new NullProgressMonitor());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getComparisonID(IPath containerPath, IJavaProject project) {
		if (containerPath == null || project == null)
			return null;

		return containerPath.segment(0) + "/" + project.getPath().segment(0); //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription(IPath containerPath, IJavaProject project) {
		return GrailsClasspathContainer.CLASSPATH_CONTAINER_DESCRIPTION;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IClasspathContainer getFailureContainer(IPath containerPath, IJavaProject project) {
		// re-try in case something went wrong
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
		// always ok to return classpath container
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject javaProject,
			IClasspathContainer containerSuggestion) throws CoreException {
		// Store source attachments and dismiss any other changes to the container
		GrailsClasspathContainer.storeSourceAttachments(javaProject, containerSuggestion);

		// Schedule refresh of class path container
		GrailsClasspathContainerUpdateJob.scheduleClasspathContainerUpdateJob(javaProject, false);
	}
}
