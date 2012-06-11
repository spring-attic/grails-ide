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
package org.grails.ide.eclipse.test.ui;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.grails.ide.eclipse.commands.GroovyCompilerVersionCheck;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.springsource.ide.eclipse.commons.frameworks.test.util.UITestCase;
import org.springsource.ide.eclipse.commons.tests.util.ManagedTestSuite;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

import org.grails.ide.eclipse.test.util.GrailsTest;
import org.grails.ide.eclipse.test.util.GroovySanityTest;
import org.grails.ide.eclipse.ui.test.GrailsExplorerTests;
import org.grails.ide.eclipse.ui.test.GrailsJUnitIntegrationTests;
import org.grails.ide.eclipse.ui.test.GrailsPluginManagerBasicTests;
import org.grails.ide.eclipse.ui.test.GrailsPluginManagerSelectionTests;
import org.grails.ide.eclipse.ui.test.GrailsPluginManagerUpdatesTest;
import org.grails.ide.eclipse.ui.test.GrailsPluginProjectExplorerTests;
import org.grails.ide.eclipse.ui.test.TestInplaceDialog;

/**
 * From the IDE run this suite as an "SWTBot test".
 * 
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @author Kris De Volder
 */
public class AllGrailsUITests {
	public static Test suite() {

		GrailsTest.mavenOffline();

		GrailsCoreActivator.testMode(StsTestUtil.isOnBuildSite());
		GroovyCompilerVersionCheck.testMode();
		final TestSuite suite = new ManagedTestSuite(
				AllGrailsUITests.class.getName());

		suite.addTestSuite(GroovySanityTest.class);
		if (GroovySanityTest.isSane()) {

			// ////////////////////////////////////////////////////////////////////////////////////////////
			// Tests that aren't really SWTBot test (or UI tests), but that
			// can't
			// run in the UIThread.
			// We add them here because we know that SWTBot test runner provides
			// proper context: i.e. non UIThread with
			// fully initialised STS.

//TODO: disabled for now, put back in after tcServer componentisation is complete
//     The test code itself has been moved out of the 'src' folder into 'srcToPutBack'

//			if (StsTestUtil.ECLIPSE_3_6_OR_LATER) {
//				// run on server tests unreliable in 3.5 builds.
//				suite.addTestSuite(RunOnServerTest26.class);
//			} else {
//				// add an empty test suite with a nothing test just so a warning
//				// is not produced
//				suite.addTestSuite(EmptyTest.class);
//			}

			// //////////////////////////////////////////////////////
			// SWTBot tests:

			addTest(suite, GrailsJUnitIntegrationTests.class);
			addTest(suite, GrailsExplorerTests.class);
			if (!StsTestUtil.isOnBuildSite()) {
				// These tests are not working on the build server. Until we can
				// figure a way around it
				// we won't be running them on the build server.
				addTest(suite, TestInplaceDialog.class);
				addTest(suite, GrailsPluginProjectExplorerTests.class);
				addTest(suite, GrailsPluginManagerBasicTests.class);
				addTest(suite, GrailsPluginManagerSelectionTests.class);
				addTest(suite, GrailsPluginManagerUpdatesTest.class);
			}
		}
		return suite;
	}

	private static void addTest(TestSuite suite,
			Class<? extends UITestCase> test) {
		suite.addTest(UITestCase.createSuite(test));
	}
}

class EmptyTest extends TestCase {
	public EmptyTest(String name) {
		super(name);
	}

	public void testNothing() throws Exception {
		System.out.println("Grails tests are disabled on Eclipse 3.5.");
	}
}