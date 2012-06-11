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
package org.grails.ide.eclipse.core.model;

import org.eclipse.core.resources.IResource;

/**
 * Empty implementation of the {@link IGrailsCommandResourceChangeListener} interface.
 * @author Christian Dupuis
 * @author Nieraj Singh
 * @since 2.2.1
 */
public abstract class GrailsCommandAdapter implements IGrailsCommandResourceChangeListener {

	/**
	 * {@inheritDoc} 
	 */
	public void changedResource(IResource resource) {
	}

	/**
	 * {@inheritDoc} 
	 */
	public void finish() {
	}

	/**
	 * {@inheritDoc} 
	 */
	public void newResource(IResource resource) {
	}

	/**
	 * {@inheritDoc} 
	 */
	public void start() {
	}
}
