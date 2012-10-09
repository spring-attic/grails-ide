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
package org.grails.ide.api.impl;

import grails.build.logging.GrailsConsole;
import grails.build.logging.GrailsEclipseConsole;
import grails.util.BuildSettings;
import groovy.lang.ExpandoMetaClass;

import java.io.File;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.codehaus.groovy.grails.cli.GrailsScriptRunner;
import org.codehaus.groovy.grails.cli.parsing.CommandLine;
import org.codehaus.groovy.grails.cli.parsing.CommandLineParser;
import org.codehaus.groovy.grails.cli.support.ClasspathConfigurer;
import org.grails.ide.api.GrailsConnector;

public class GrailsConnectorImpl implements GrailsConnector {
	
	public static boolean instantiated = false;

	/**
	 * The directory that we are connected to. Once connected, the directory can not be changed.
	 */
	private final File baseDir;

	//TODO: encapsulate all important state
	
	//Ideally all of the important state of a GrailsConnector should be represented / contained 
	//within the instance fields below. This is probably not really the case yet. Some
	//state may linger elsewhere (static fields, system properties, etc.)
	
	private BuildSettings buildSettings = null;
	private GrailsScriptRunner scriptRunner;
    private CommandLineParser parser = GrailsScriptRunner.getCommandLineParser();
	private Map<String,String> savedSystemProps = null; //used to 'reset' system props prior to executing a command.

	public GrailsConnectorImpl(File baseDir) {
		//For now, we only allow one connector to be created per JVM. 
		//Maybe if are more confident that Grails is able to keep several connectors
		//in a single JVM without 'mixing' their states together this restriction can be lifted.
		//What may be feasible, for example is have only multiple instances in the same JVM
		//as long as an 'old' instance is disposed of before creating a new one.
		if (instantiated) {
			throw new IllegalStateException("Only one connector per JVM is supported by this Tooling API implementation");
		}
		instantiated = true;
		this.baseDir = baseDir;

        System.setProperty("net.sf.ehcache.skipUpdateCheck", "true"); //TODO: Copied from GrailsScriptRunner, what's this for?
        System.setProperty("jline.terminal", "jline.UnsupportedTerminal"); //TODO: Bad! Needs API way.
        System.setProperty("grails.console.class", GrailsEclipseConsole.class.getName()); //TODO: Bad! Needs API way.
        System.setProperty("grails.disable.exit", "true"); //TODO: do we really need this?
        ExpandoMetaClass.enableGlobally(); //TODO: copied from GrailsScriptRunner. What's this for?
		saveSystemProperties();
	}

	public File getBaseDir() {
		if (buildSettings==null) {
			return baseDir; // not yet initialised, so our originally set baseDir is the one.
		} else {
			//buildSettings already created its state takes priority over the 'original' baseDir
			return buildSettings.getBaseDir();
		}
		
//More conservative version: treats mismatching baseDir between buildSettings and our own instance as
// a 'corrupted' state.
		
//		if (baseDir!=null) {
//			if (getBuildSettings()!=null) {
//				//BuildSettings and initial basedir must agree, otherwise flaky results ensue.
//				if (baseDir.equals(getBuildSettings().getBaseDir())) {
//					return baseDir;
//				} else {
//					return null; //To the client this will mean we are in an not very well defined state
//								// and the process shouldn't be re-used.
//				}
//			}
//		}
//		return baseDir;
	}

	private BuildSettings createBuildSettings()  {
		BuildSettings buildSettings = null;
		// Get hold of the grails.home system property. 
        String grailsHome = System.getProperty("grails.home");

        try {
        	buildSettings = new BuildSettings(new File(grailsHome), baseDir);
        	buildSettings.setRootLoader((URLClassLoader) this.getClass().getClassLoader());
        } catch (Exception e) {
            throw new Error("An error occurred loading the grails-app/conf/BuildConfig.groovy file: " + e.getMessage());
        }
        
        return buildSettings;
	}
	
	private void ensureInitialized() {
		if (buildSettings==null) {
			buildSettings = createBuildSettings();
			scriptRunner = new GrailsScriptRunner(buildSettings);
			GrailsConsole.getInstance().log("Loading Grails "+buildSettings.getGrailsVersion());
			
//			File grailsHome = buildSettings.getGrailsHome();
//			debug("Starting Remote Script runner for Grails " + buildSettings.getGrailsVersion());
//			debug("Grails home is " + (grailsHome == null ? "not set" : "set to: " + grailsHome) + '\n');
			
			buildSettings.loadConfig();
			//if (resolveDeps) {
			ClasspathConfigurer.cleanResolveCache(buildSettings);
			buildSettings.setModified(true);
			//}
			scriptRunner.initializeState();
			//scriptRunner.setInteractive(false);
		} else {
			GrailsConsole.getInstance().log("Loading Grails "+buildSettings.getGrailsVersion());
		}
	}

	public int executeCommand(String cmdString, GrailsConsole console) {
		resetSystemProperties();
		ReflectionHacks.GrailsConsole_setInstance(console);
		CommandLine command =  parser.parseString(cmdString);
		if (command.hasOption(CommandLine.REFRESH_DEPENDENCIES_ARGUMENT)) {
			buildSettings = null;  //force complete(?) reinitialization
		}
		ensureInitialized();
		scriptRunner.setInteractive(!command.hasOption(CommandLine.NON_INTERACTIVE_ARGUMENT));
		
		return scriptRunner.executeScriptWithCaching(command);
	}

	public BuildSettings getBuildSettings() {
		return buildSettings;
	}


	private void saveSystemProperties() {
		try {
			savedSystemProps = new HashMap<String, String>();
			Properties currentProps = System.getProperties();
			Enumeration<?> enumeration = currentProps.propertyNames();
			while (enumeration.hasMoreElements()) {
				String prop = (String)enumeration.nextElement();
				savedSystemProps.put(prop, System.getProperty(prop));
			}
		} catch (Exception e) {
			savedSystemProps = null;
		}
	}
	
	private void resetSystemProperties() {
		if (savedSystemProps!=null) {
			Properties currentProps = System.getProperties();
			//1: clear any properties that got added since we saved...
			Enumeration<?> enumeration = currentProps.propertyNames();
			while (enumeration.hasMoreElements()) {
				String prop = (String)enumeration.nextElement();
				if (!savedSystemProps.containsKey(prop)) {
					System.clearProperty(prop);
				}
			}
			//2: reset values of any properties that were set when we saved
			for (Entry<String, String> entry : savedSystemProps.entrySet()) {
				if (entry.getValue()!=null) {
					System.setProperty(entry.getKey(), entry.getValue());
				}
			}
		}
	}

}
