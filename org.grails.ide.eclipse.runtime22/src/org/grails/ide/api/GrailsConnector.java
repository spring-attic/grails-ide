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
package org.grails.ide.api;

import grails.build.logging.GrailsConsole;
import grails.util.BuildSettings;

import java.io.File;

/**
 * A GrailsConnector instance represents a connection to a Grails processs. The process is associated with
 * a specific 'baseDir'. It allows for retrieving information about the project that exists at this location
 * and executing commands at this location. 
 * <p>
 * Some commands like 'create-app' and 'create-plugin' can be executed in a location that doesn't correspond
 * to a project.
 */
public interface GrailsConnector {

	/**
	 * @return The current baseDir associated with this connector. If this returns null, the connector
	 * is an a undefined/disposed state and should not be (re)used to execute additional commands
	 * or retrieve additional information.  
	 */
	File getBaseDir();
	
	/**
	 * Retrieve a BuildSettings object which contain various bits of info about the project at the baseDir.
	 * <p>
	 * In the current implementation the information contained in this object is highly dependent on the
	 * sequence of commands that was executed so far. In fact, unless a command was executed the method
	 * may return null. 
	 * <p>
	 * Ideally, the implementation of the connector should be responsible to ensure the information 
	 * returned by this method is up-to-date rather than put the onus on the client to figure out
	 * which commands need to be executed to make it up-to-date.
	 * <p>
	 * @return The BuildSettings object associated with the project at the current basedir.
	 */
	BuildSettings getBuildSettings(); //TODO: Shouldn't use BuildSettings directly, need to create an interface and adapter
	
	/**
	 * @param commandLine String to parse and execute.
	 * @param console Where the execution should send output and read input.
	 * @return equivalent of process exit code. 0 means OK any other value signifies some kind of problem.
	 */
	int executeCommand(String commandLine, GrailsConsole console); //TODO: Would be nice to have 'clean' and simple IGrailsConsole interface
	
}
