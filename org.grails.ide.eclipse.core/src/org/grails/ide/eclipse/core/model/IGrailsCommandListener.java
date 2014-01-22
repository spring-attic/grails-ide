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

import org.eclipse.core.resources.IProject;


/**
 * @author Christian Dupuis
 * @author Nieraj Singh
 */
public interface IGrailsCommandListener {
	
	/**
	 * Notifies the listener that a Grails command is about to execute.
	 */
	void start();
	
	/**
	 * Notifies the listener that a Grails command finished execution.
	 */
	void finish();
	
	/**
	 * Indicate if the this command listener supports events for the current project.
	 * @return <code>true</code> if the events for the given project should be send to the listener 
	 */
	boolean supports(IProject project);
	

}
