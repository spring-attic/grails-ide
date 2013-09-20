/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.internal;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.grails.ide.eclipse.core.GrailsCoreActivator;


/**
 * Listens to resource deltas specific to Grails projects. Clients can subclass to
 * execute commands when a Grails resource delta is received.
 * @author Nieraj Singh
 */
public abstract class GrailsResourceDeltaListener implements
		IResourceChangeListener {
	protected IResourceDeltaVisitor visitor = new GrailsResourceChangeVisitor();
	
	protected GrailsResourceDeltaListener() {
		init();
	}

	protected void init() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				this);
	}

	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();

		if (visitor != null && delta != null) {
			try {
				delta.accept(visitor);
			} catch (CoreException e) {
				GrailsCoreActivator.log(e.getLocalizedMessage(), e);
			}
		}
	}

	/**
	 * Visits any changes associated with an underlying Grails project resource
	 * in the workspace, including local workspace plugin dependencies.
	 * 
	 * @author nisingh
	 * 
	 */
	protected class GrailsResourceChangeVisitor implements
			IResourceDeltaVisitor {

		public boolean visit(IResourceDelta delta) throws CoreException {
			final IResource resource = delta.getResource();
			if (resource == null) {
				return false;
			}

			if (resource.getType() == IResource.ROOT) {
				return true;
			}

			return visitGrailsResource(resource);

		}
	}

	abstract protected boolean visitGrailsResource(IResource resourceChanged);

	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(
				this);

	}
}
