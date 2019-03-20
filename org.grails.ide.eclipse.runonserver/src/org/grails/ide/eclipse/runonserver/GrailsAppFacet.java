/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.runonserver;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;


/**
 * @author Kris De Volder
 * @author Christian Dupuis
 * @since 2.5.1
 */
public class GrailsAppFacet {
	
	public static final String ID = "grails.app";
	public static final IProjectFacet FACET = ProjectFacetsManager.getProjectFacet(ID); 
	
	public static boolean hasFacet(IProject _project) {
		try {
			IFacetedProject project = ProjectFacetsManager.create(_project);
			return project.hasProjectFacet(FACET);
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
			return false;
		}
	}
	
	public static void addFacetIfNeeded(IProject project) throws CoreException {
		if (!hasFacet(project)) {
			addFacet(project);
		}
	}
	
	public static void addFacet(IProject _project) throws CoreException {
		Assert.isLegal(!hasFacet(_project));
		IFacetedProject project = ProjectFacetsManager.create(_project);
		project.installProjectFacet(FACET.getDefaultVersion(), null, null);
	}
	
	public static class InstallDelegate implements IDelegate {
		public void execute(IProject project, IProjectFacetVersion fv,
				Object config, IProgressMonitor monitor) throws CoreException {
			//There really isn't much to do here. We just check if this project is a GrailsApp.
			if (!GrailsNature.isGrailsAppProject(project)) {
				throw new CoreException(new Status(IStatus.ERROR, RunOnServerPlugin.PLUGIN_ID, "Grails App Facet can only be installed on Grails App projects"));
			}
		}
	}

}
