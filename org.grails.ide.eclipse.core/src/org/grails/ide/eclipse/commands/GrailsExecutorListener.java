/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.commands;

import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;

/**
 * Listens for commands executed by a {@link GrailsExecutor}.
 * 
 * @author Kris De Volder
 */
public abstract class GrailsExecutorListener {
	
	/**
	 * Called when a command execution returned a result.
	 */
	public abstract void commandExecuted(GrailsCommand cmd, ILaunchResult result);

	/**
	 * Called when a command execution threw some exception.
	 */
	public abstract void commandExecuted(GrailsCommand cmd, Throwable thrown);

}
