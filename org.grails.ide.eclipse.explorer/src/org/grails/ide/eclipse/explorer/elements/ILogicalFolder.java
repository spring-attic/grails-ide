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
package org.grails.ide.eclipse.explorer.elements;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes;


/**
 * Represents a Grails logical folder contributed to the common navigator
 * @author Nieraj Singh
 * @author Andy Clement
 * @author Kris De Volder
 */
public interface ILogicalFolder extends IAdaptable {

	/**
	 * 
	 * @return the display name of the logical folder. Should not be null.
	 */
	public String getName();

	/**
	 * 
	 * @return the Grails folder type that this logical folder is representing.
	 *         Should not be null
	 */
	public GrailsProjectStructureTypes getType();

	/**
	 * 
	 * @return The common navigator/tree parent of this logical folder. In many cases it
	 * may just be the plugin project. 
	 */
	public Object getParent();

	/**
	 * @return children that should be contained inside the logical folder. Can be null or empty
	 */
	public List<Object> getChildren();

	/**
	 * @return project this IlogicalFolder node is nested under.
	 */
	public IProject getProject();

}
