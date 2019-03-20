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
package org.grails.ide.eclipse.runtime;

import grails.build.GrailsBuildListener;
import grails.util.BuildSettings;

import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.gant.GantBinding;
import org.grails.ide.eclipse.runtime.shared.SharedLaunchConstants;


/**
 * This is an implementation of GrailsBuildListener that can be attached to an external grails process.
 * <p>
 * It will extract grailsSettings from a gant event near the end of the the process and then use this settings
 * object to write out a dependency xml file reflecting the state of the buildsettings. STS can then later on
 * read and use this data to determine classpaths, linked source folders etc.
 * <p>
 * Note that this class will be attached to the running grails process in the grail's process's JVM instance.
 * So it executes 'outside' of STS's JVM and we cannot communicate with it directly (hence writing data to
 * a file).
 * @author Kris De Volder
 */
public class DependencyExtractingBuildListener implements GrailsBuildListener {
	
	public static final boolean DEBUG = false;
	
	private String eventOfInterest = null;
	
	public void receiveGrailsBuildEvent(String eventName, Object... args) {
		debug("event received: "+eventName); 
		if (eventOfInterest==null) {
			//The first "Start" event we see containing a gant binding will determine the 'event of interest'
			if (eventName.endsWith("Start")) {
				if (args.length>0) {
					try {
						eventOfInterest = eventName.substring(0, eventName.length()-"Start".length()) + "End";
						debug("eventOfInterest determined: "+eventOfInterest);
					} catch (ClassCastException e) {
						debug(e);
					}
				}
			}
		} else if (eventOfInterest.equals(eventName)) {
			debug("event of interest received: "+eventOfInterest);
			//Let's see what we find inside the gant bindings variables
			GantBinding binding = (GantBinding) args[0];
			if (DEBUG) {
				@SuppressWarnings("unchecked")
				Map<String, Object> variables = binding.getVariables();
				for (Entry<String, Object> varEntry : variables.entrySet()) {
					Object value = varEntry.getValue();
					if (value!=null) {
						debug("   "+varEntry.getKey()+" = "+value.getClass().getName());
					} else {
						debug("   "+varEntry.getKey()+" = ");
					}
					debug("        "+value);
				}
			}
			BuildSettings settings = (BuildSettings) binding.getVariable("grailsSettings");
			debug("BuildSettings: "+settings);
			GrailsBuildSettingsDependencyExtractor extractor = new GrailsBuildSettingsDependencyExtractor(settings);
			String dependencyFileName = System.getProperty(SharedLaunchConstants.DEPENDENCY_FILE_NAME_PROP);
			if (dependencyFileName!=null) {
				debug("Writing dependencies to file: "+dependencyFileName);
				try {
					extractor.writeDependencyFile(dependencyFileName);
				} catch (Exception e) {
					debug(e);
				}
			} else {
				debug("Dependency file writing is currenty DISABLED");
			}
		}
	}

	private void debug(Exception e) {
		if (DEBUG) {
			e.printStackTrace(System.out);
		}
	}

	public static void debug(String string) {
		if (DEBUG) {
			System.out.println(string);
		}
	}
}