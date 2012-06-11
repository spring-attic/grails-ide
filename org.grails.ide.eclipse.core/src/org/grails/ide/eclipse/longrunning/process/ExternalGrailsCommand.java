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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.codehaus.groovy.grails.cli.GrailsScriptRunner;



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
	String scriptName = null;
	String args = "";
	String env = null;
	String dependencyFile = null; // if set, makes us write dependency data to file at this location after successful command completion.
	
	public ExternalGrailsCommand() {
	}

	public String getScriptName() throws ProtocolException {
		if (scriptName==null) {
			throw new ProtocolException("script name must be set");
		}
		return scriptName;
	}

	public void setScriptName(String scriptName) throws ProtocolException {
		if (this.scriptName!=null) {
			throw new ProtocolException("script name is set more than once");
		}
		this.scriptName = scriptName;
	}

	public String getArgs() {
		return args;
	}

	public void setArgs(String args) throws ProtocolException {
		if (this.args.equals("")) {
			this.args = args;
		} else {
			throw new ProtocolException("args set multiple times");
		}
	}

	public String getEnv() {
		return env;
	}

	public void setEnv(String env) throws ProtocolException {
		if (this.env!=null) {
			throw new ProtocolException("env set multiple times");
		}
		this.env = env;
	}

	/**
	 * Initialise this instance from a "raw" command String as might be entered by the
	 * user in a command line interface.
	 */
	public void parse(String commandString) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, ProtocolException, NoSuchFieldException {
		Method m = GrailsScriptRunner.class.getDeclaredMethod("processArgumentsAndReturnScriptName", String.class);
		m.setAccessible(true);
		Object scriptInfos = m.invoke(null, commandString);
		Class<?> ScriptAndArgs = scriptInfos.getClass();
		setScriptName(getField(ScriptAndArgs, scriptInfos, "name"));
		setArgs(getField(ScriptAndArgs, scriptInfos, "args"));
		setEnv(getField(ScriptAndArgs, scriptInfos, "env"));
	}

	private String getField(Class<?> ScriptAndArgs, Object scriptInfos, String fName) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field field = ScriptAndArgs.getField(fName);
		field.setAccessible(true);
		return (String)field.get(scriptInfos);
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
		return scriptName + " " + args;
	}
}