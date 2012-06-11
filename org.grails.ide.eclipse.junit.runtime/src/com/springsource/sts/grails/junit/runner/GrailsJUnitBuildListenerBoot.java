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
package com.springsource.sts.grails.junit.runner;

import grails.build.GrailsBuildListener;

import java.net.URLClassLoader;

/**
 * We are getting loaded by the Grails RootLoader and so do not have access
 * to all the classes that we want (most notably, JUnit related classes). So
 * the "Boot" listener is just a temporary front for the real listener. It waits
 * for the first event that contains a Suite and then it creates the real listener
 * by loading it in the Suite.class's class loader.
 * @author Kris De Volder
 */
public class GrailsJUnitBuildListenerBoot implements GrailsBuildListener {
	
	private static GrailsBuildListener realListener = null;

	public void receiveGrailsBuildEvent(String name, Object... args) {
		if (realListener!=null) {
			realListener.receiveGrailsBuildEvent(name, args);
			return;
		}
		if (name.equals(GrailsBuildListenerEvents.TEST_SUITE_PREPARED)) {
			Object suite = args[0];
			Class<?> suiteClass = suite.getClass();
			if (!suiteClass.getName().endsWith(".Suite")) 
				throw new Error("Unexpected class for args[0] of TEST_SUITE_PREPARED: "+suiteClass.getName());
			URLClassLoader grailsLoader = (URLClassLoader) args[0].getClass().getClassLoader();
			try {
				Class<?> realBuildListener = Class.forName(
						"com.springsource.sts.grails.junit.runner.GrailsJUnitBuildListener", true, grailsLoader);
				realListener = (GrailsBuildListener) realBuildListener.newInstance();
				receiveGrailsBuildEvent(name, args);
			} catch (Exception e) {
				throw new Error(e);
			}

		}
	}
	
}
