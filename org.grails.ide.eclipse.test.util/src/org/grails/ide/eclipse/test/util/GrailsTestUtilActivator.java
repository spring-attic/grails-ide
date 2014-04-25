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
package org.grails.ide.eclipse.test.util;

import org.codehaus.groovy.eclipse.core.compiler.CompilerUtils;
import org.codehaus.groovy.frameworkadapter.util.SpecifiedVersion;
import org.eclipse.core.runtime.Platform;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class GrailsTestUtilActivator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		configureGrailsVersions();
	}

//	private void configureGroovyCompilerVersion() {
//		dumpGroovyBundles();
//		System.out.println("Configuring Groovy compiler for Grails: "+GrailsVersion.MOST_RECENT);
//		boolean useGroovy18 = GrailsVersion.V_2_0.compareTo(GrailsVersion.MOST_RECENT) <=0;
//		System.out.println("Use Groovy 18? "+useGroovy18);
//		if (useGroovy18) {
//			//Must make sure 18 is enabled
//			if (CompilerUtils.isGroovy18DisabledOrMissing()) {
//				assertTrue("Couldn switch compiler versions", CompilerUtils.switchVersions(useGroovy18).isOK());
//			}
//		} else {
//			//Must make sure 18 is not enabled
//			if (!CompilerUtils.isGroovy18DisabledOrMissing()) {
//				assertTrue("Couldn switch compiler versions", CompilerUtils.switchVersions(useGroovy18).isOK());
//			}
//		}
//		dumpGroovyBundles();
//		System.out.println("active groovy bundle is now: "+CompilerUtils.getActiveGroovyBundle());
//	}

	/**
	 * Dum which groovy bundles are there... for debugging purposes
	 */
	private static void dumpGroovyBundles() {
		System.out.println(">>>> Groovy bundles : ");
		Bundle[] bundles = Platform.getBundles("org.codehaus.groovy", null);
		for (Bundle bundle : bundles) {
			System.out.println(bundle + " " + (stateString(bundle.getState())));
		}
		System.out.println("<<<< Groovy bundles");
	}

	private static String stateString(int state) {
		switch (state) {
		case Bundle.ACTIVE:
			return "ACTIVE";
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.STARTING:
			return "STARTING";
		case Bundle.STOPPING:
			return "STOPPING";
		default:
			return "UNKOWN("+state+")";
		}
	}

	public static void configureGrailsVersions() {
		dumpGroovyBundles();
		configureGrailsVersions(CompilerUtils.getWorkspaceCompilerLevel());
	}
	
	public static void configureGrailsVersions(SpecifiedVersion groovyVersion) {
		switch (groovyVersion) {
		case _20:
			System.out.println("Detected Groovy Workspace Version = 2.0");
			configureGrails22();
			break;
		case _21:
			System.out.println("Detected Groovy Workspace Version = 2.1");
			configureGrails23();
			break;
		case _22:
			System.out.println("Detected Groovy Workspace Version = 2.2");
			configureGrails24();
			break;
		default:
			//The groovy compiler level is probably wrong... but anyhoo...
			configureGrails23();
			break;
		}
		System.out.println("GrailsVersion.MOST_RECENT = "+GrailsVersion.MOST_RECENT);
	}

	private static void configureGrails24() {
		GrailsVersion.PREVIOUS_PREVIOUS =  GrailsVersion.V_2_3_7;
		GrailsVersion.PREVIOUS = GrailsVersion.V_2_3_8;
		GrailsVersion.MOST_RECENT = GrailsVersion.V_2_4_0_M2;
	}
	
	private static void configureGrails23() {
		GrailsVersion.PREVIOUS_PREVIOUS =  GrailsVersion.V_2_3_5;
		GrailsVersion.PREVIOUS = GrailsVersion.V_2_3_7;
		GrailsVersion.MOST_RECENT = GrailsVersion.V_2_3_8;
	}

	private static void configureGrails22() {
		GrailsVersion.PREVIOUS_PREVIOUS = GrailsVersion.V_2_0_4;
		GrailsVersion.PREVIOUS = GrailsVersion.V_2_1_0; 
		GrailsVersion.MOST_RECENT = GrailsVersion.V_2_2_4;
	}

	public void stop(BundleContext context) throws Exception {
	}

}
