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
package org.grails.ide.eclipse.core.model;

import org.eclipse.core.resources.IResource;

/**
 * @author Christian Dupuis
 * @author Nieraj Singh
 * @since 2.2.1
 */
public interface IGrailsCommandResourceChangeListener extends IGrailsCommandListener{
	
	
	/**
	 * Notifies that during command executing a new resource has been created.
	 */
	void newResource(IResource resource);

	/**
	 * Notifies that during command executing a resources has been changed.
	 */
	void changedResource(IResource resource);
	
}
