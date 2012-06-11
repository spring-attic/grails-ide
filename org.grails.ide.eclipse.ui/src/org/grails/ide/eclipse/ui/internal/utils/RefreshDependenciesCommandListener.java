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

import org.eclipse.core.resources.IProject;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathContainerUpdateJob;
import org.grails.ide.eclipse.core.model.GrailsCommandAdapter;
import org.grails.ide.eclipse.core.model.IGrailsCommandResourceChangeListener;


/**
 * {@link IGrailsCommandResourceChangeListener} that refreshes the dependencies after installing a new plugin
 * @author Christian Dupuis
 * @author Nieraj Singh
 * @author Kris De Volder
 * @since 2.3.0
 */
public class RefreshDependenciesCommandListener extends GrailsCommandAdapter {
	
	private final IProject project;
	
	public RefreshDependenciesCommandListener(IProject project) {
		this.project = project;
	}
	
	public IProject getProject() {
		return project;
	}
	
	public boolean supports(IProject project) {
		return this.project.equals(project);
	}
	
	@Override
	public void finish() {
		GrailsCoreActivator.getDefault().removeGrailsCommandResourceListener(this);
		GrailsClasspathContainerUpdateJob.scheduleClasspathContainerUpdateJob(project, false);
	}

}
