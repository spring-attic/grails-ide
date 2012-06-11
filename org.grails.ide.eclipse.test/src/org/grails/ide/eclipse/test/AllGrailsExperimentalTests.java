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
package org.grails.ide.eclipse.test;

import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.springsource.ide.eclipse.commons.tests.util.ManagedTestSuite;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.grails.ide.eclipse.ui.internal.importfixes.GrailsProjectVersionFixer;

/**
 * @author Andrew Eisenberg
 * @author Steffen Pingel
 * @author Christian Dupuis
 * @author Kris De Volder
 * @author Nieraj Singh
 * @created Dec 7, 2009
 */
public class AllGrailsExperimentalTests {
	
    public static Test suite() {
    	GrailsCoreActivator.testMode(StsTestUtil.isOnBuildSite());
    	GrailsProjectVersionFixer.testMode(); // Answers 'no' to all questions automatically (so as not to disrupt existing tests).
    	
    	TestSuite suite = new ManagedTestSuite(AllGrailsExperimentalTests.class.getName());
    	suite.addTestSuite(GrailsInstallWorkspaceConfiguratorTest.class);
    	return suite;
    }

}
