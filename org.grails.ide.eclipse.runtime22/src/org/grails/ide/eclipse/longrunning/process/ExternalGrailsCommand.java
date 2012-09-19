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
package org.grails.ide.eclipse.longrunning.process;

import org.codehaus.groovy.grails.cli.parsing.CommandLineParser;
import org.grails.ide.api.impl.APIScriptRunner;
import org.grails.ide.eclipse.runtime.shared.longrunning.ProtocolException;

/**
 * Helper class, contains info about a command that should be executed by an external Grails
 * process.
 * <p>
 * The typical use of this class is to create one instance per command execution using the 
 * default, no arguments constructor and then use the setter and/or parse methods to initialise
 * the properties of the command. 
 * <p>
 * Setter methods in this class contain some checking code to enforce that properties
 * are not initialised more than once.
 * 
 * @author Kris De Volder
 */
class ExternalGrailsCommand {
	
	private static final CommandLineParser parser = APIScriptRunner.getCommandLineParser();
	
	private String grailsCommand;
	private String dependencyFile = null; // if set, makes us write dependency data to file at this location after successful command completion.
	
	public ExternalGrailsCommand() {
	}

	public void setDependencyFile(String dependencyFile) throws ProtocolException {
		if (this.dependencyFile!=null) {
			throw new ProtocolException("Dependency file set multiple times");
		}
		this.dependencyFile = dependencyFile;
	}

	public String getDependencyFile() {
		return dependencyFile;
	}
	
	@Override
	public String toString() {
		return ""+grailsCommand;
	}

	public String getCommandLine() {
		return grailsCommand;
	}

	public void parse(String cmd) {
		this.grailsCommand = cmd;
	}
}