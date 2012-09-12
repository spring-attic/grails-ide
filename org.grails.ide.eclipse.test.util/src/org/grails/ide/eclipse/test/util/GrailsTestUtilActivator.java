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
		boolean useGrails200 = true;
		//!CompilerUtils.isGroovyVersionDisabledOrMissing(SpecifiedVersion._18);
        // Uncomment to force use of 1.3.7
//      useGrails200 = false;
		configureGrailsVersions(useGrails200);
		
//		String prop = System.getProperty("grails.version.most.recent");
//		boolean useGrails200;
//		if (prop!=null) {
//			if (prop.equals("2.0.0")) {
//				useGrails200 = true;
//			} else if (prop.equals("1.3.7")) {
//				useGrails200 = false;
//			} else {
//				throw new Error("1.3.7");
//			}
//		} else {
//			useGrails200 = StsTestUtil.ECLIPSE_3_7_OR_LATER;
//		}
//		configureGrailsVersions(useGrails200);
	}
	
	public static void configureGrailsVersions(boolean useGrails200) {
		if (useGrails200) {
			//Treats 2.0. as most recent Grails, runs majority of tests with 2.0:
			GrailsVersion.PREVIOUS_PREVIOUS = GrailsVersion.V_1_3_7;
			GrailsVersion.PREVIOUS = GrailsVersion.V_2_0_4; 
			//GrailsVersion.MOST_RECENT = GrailsVersion.V_2_0_0_M1;
			//GrailsVersion.MOST_RECENT = GrailsVersion.V_2_0_0_M2;
			//GrailsVersion.MOST_RECENT = GrailsVersion.BUILDSNAPHOT_2_0_2;
			//GrailsVersion.MOST_RECENT = GrailsVersion.V_2_0_1;
			//GrailsVersion.MOST_RECENT = GrailsVersion.V_2_0_3;
			//GrailsVersion.MOST_RECENT = GrailsVersion.V_2_1_0;
			GrailsVersion.MOST_RECENT = GrailsVersion.V_2_1_1_SNAP;
			//GrailsVersion.MOST_RECENT = GrailsVersion.V_2_2_0_SNAP;
		} else {
			//Treats 1.3.7. as most recent Grails, runs majority of tests with 1.3.8:
			GrailsVersion.PREVIOUS_PREVIOUS = GrailsVersion.V_1_3_6;
			GrailsVersion.PREVIOUS = GrailsVersion.V_1_3_7; 
			GrailsVersion.MOST_RECENT = GrailsVersion.V_1_3_8;
		}
		System.out.println("GrailsVersion.MOST_RECENT = "+GrailsVersion.MOST_RECENT);
	}

	public void stop(BundleContext context) throws Exception {
	}

}
