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
package org.grails.ide.eclipse.runonserver;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ModuleArtifactAdapterDelegate;
import org.eclipse.wst.server.core.util.WebResource;

/**
 * An artifact adapter is what makes "Run On Server" menu appear and work for different 
 * resources in a project (or the project itself). 
 * <p>
 * This artifact adapter supports "Run On Server" for a GrailsProject. Currently only
 * the project itself can be run. No supports is provided for selecting and running
 * resource inside the project.
 * @author Kris De Volder
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @since 2.5.1
 */
public class GrailsAppArtifactAdapter extends ModuleArtifactAdapterDelegate {

	public GrailsAppArtifactAdapter() {
	}

	@Override
	public IModuleArtifact getModuleArtifact(Object obj) {
		IProject project = (IProject) ((IAdaptable)obj).getAdapter(IProject.class);
		IModule webModule = ServerUtil.getModule(project);
		Assert.isLegal(webModule!=null);
		return new WebResource(webModule, Path.EMPTY);
	}

}
