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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.grails.ide.eclipse.commands.GroovyCompilerVersionCheck;
import org.grails.ide.eclipse.commands.test.DependencyFileFormatTest;
import org.grails.ide.eclipse.commands.test.ExtraPluginInstallerTests;
import org.grails.ide.eclipse.commands.test.GrailsCommandFactoryTest;
import org.grails.ide.eclipse.commands.test.GrailsCommandTest;
import org.grails.ide.eclipse.commands.test.GrailsCommandUtilTest;
import org.grails.ide.eclipse.commands.test.GrailsCommandWizardExpressionTest;
import org.grails.ide.eclipse.commands.test.JointGrailsCommandTest;
import org.grails.ide.eclipse.commands.test.LaunchSystemPropertiesTest;
import org.grails.ide.eclipse.commands.test.PluginInstallerTests;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.wizard.GrailsImportWizardCore;
import org.grails.ide.eclipse.groovy.debug.tests.AllGroovyDebugTests;
import org.grails.ide.eclipse.longrunning.test.LongRunningGrailsTest;
import org.grails.ide.eclipse.longrunning.test.PrefixedOutputStreamTest;
import org.grails.ide.eclipse.test.gsp.ControllerCacheTests;
import org.grails.ide.eclipse.test.gsp.GSPContentAssistTests;
import org.grails.ide.eclipse.test.gsp.GSPHyperlinkTests;
import org.grails.ide.eclipse.test.gsp.GSPSearchTests;
import org.grails.ide.eclipse.test.gsp.GSPStructuredModelCreationTests;
import org.grails.ide.eclipse.test.gsp.GSPTagDocParserTests;
import org.grails.ide.eclipse.test.gsp.GSPTagsTests;
import org.grails.ide.eclipse.test.gsp.GSPTokenizerTest;
import org.grails.ide.eclipse.test.gsp.GSPTranslationTests;
import org.grails.ide.eclipse.test.gsp.GSPValidationTests;
import org.grails.ide.eclipse.test.gsp.ScannerUnitTests;
import org.grails.ide.eclipse.test.inferencing.BelongsToInferencingTests;
import org.grails.ide.eclipse.test.inferencing.CaseInsensitiveDynamicFinderProposalsTests;
import org.grails.ide.eclipse.test.inferencing.ControllerReturnTypeInferencingTests;
import org.grails.ide.eclipse.test.inferencing.DSLDGrailsInferencingTests;
import org.grails.ide.eclipse.test.inferencing.DynamicFinderProposalsTests;
import org.grails.ide.eclipse.test.inferencing.DynamicFinderTests;
import org.grails.ide.eclipse.test.inferencing.GrailsContentAssistTests;
import org.grails.ide.eclipse.test.inferencing.GrailsInferencingTests;
import org.grails.ide.eclipse.test.inferencing.GrailsServiceInferencingTests;
import org.grails.ide.eclipse.test.inferencing.NamedQueryInferencingTests;
import org.grails.ide.eclipse.test.inferencing.PluginDataTests;
import org.grails.ide.eclipse.test.inferencing.PluginInferencingTests;
import org.grails.ide.eclipse.test.util.GrailsTest;
import org.grails.ide.eclipse.test.util.GroovySanityTest;
import org.grails.ide.eclipse.test.util.ZipFileUtilTest;
import org.grails.ide.eclipse.ui.test.GrailsConsoleLineTrackerTests;
import org.grails.ide.eclipse.ui.test.StackFrameConsoleLineTrackerTests;
import org.springsource.ide.eclipse.commons.tests.util.ManagedTestSuite;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

/**
 * @author Andrew Eisenberg
 * @author Steffen Pingel
 * @author Christian Dupuis
 * @author Kris De Volder
 * @author Nieraj Singh
 * @created Dec 7, 2009
 */
public class AllGrailsTests {
    
	public static class Grails35Test extends TestCase {
		
		public Grails35Test() {
			super(Grails35Test.class.getName());
		}
		
		public void testGrailsTestsDisabledOn35AndEarlier() throws Exception {
			System.out.println("Grails tests are not run on Eclipse 3.5 or earlier");
		}
		
	}
	
	public static Test suite(boolean heartbeat) {
				
		GrailsTest.mavenOffline();

	    if (!StsTestUtil.ECLIPSE_3_6_OR_LATER) {
	        return new TestSuite(Grails35Test.class);
	    }
	    
    	GrailsCoreActivator.testMode(StsTestUtil.isOnBuildSite());
    	GroovyCompilerVersionCheck.testMode(); // Disable modal dialog
        TestSuite suite = new ManagedTestSuite(AllGrailsTests.class.getName());

        suite.addTestSuite(GroovySanityTest.class);
        if (GroovySanityTest.isSane()) {
        	
    		suite.addTestSuite(GrailsRunAsTestAppTests.class);
        	suite.addTestSuite(ControllerCacheTests.class);
        	suite.addTestSuite(DefaultGrailsInstallTests.class);
        	suite.addTestSuite(StackFrameConsoleLineTrackerTests.class);
        	suite.addTestSuite(GrailsConsoleLineTrackerTests.class);
        	suite.addTestSuite(GrailsVersionTest.class);
        	suite.addTestSuite(DependencyFileFormatTest.class);
        	suite.addTestSuite(DynamicFinderTests.class);
        	suite.addTestSuite(GrailsCommandHistoryTest.class);
        	if (!heartbeat) { //Disable in HB for now
        		suite.addTestSuite(GrailsCommandUtilTest.class);
        	}
        	suite.addTestSuite(JointGrailsCommandTest.class);
        	JointGrailsCommandTest.heartbeat = heartbeat;
        	suite.addTestSuite(GSPTokenizerTest.class);
        	suite.addTestSuite(GSPTranslationTests.class);
        	suite.addTestSuite(PrefixedOutputStreamTest.class);
        	suite.addTestSuite(ScannerUnitTests.class);

        	if (StsTestUtil.ECLIPSE_3_6_OR_LATER) {
        		if (!heartbeat) { //Disable for now in HB build
        			suite.addTest(PluginInferencingTests.suite());
        		}
        	}

        	// Extra tests
        	if (!heartbeat) {
        		if (GrailsVersion.MOST_RECENT.compareTo(GrailsVersion.V_2_0_0)>=0) {
        			suite.addTestSuite(Grails20JUnitIntegrationTests.class);
                    suite.addTest(DSLDGrailsInferencingTests.suite());
        		}
            	suite.addTestSuite(ZipFileUtilTest.class);
        		suite.addTestSuite(GrailsCoreTests.class);
        		suite.addTestSuite(UrlMappingTests.class);
        		// this if statement is unnecessary now that Grails tests are not run on 
        		// 3.5 or earlier.  Consider removing.
        		if (StsTestUtil.ECLIPSE_3_6_OR_LATER) {
        			suite.addTestSuite(GrailsProjectVersionFixerTest.class);
        			suite.addTest(GrailsInferencingTests.suite());
        			suite.addTest(BelongsToInferencingTests.suite());
        			suite.addTest(ControllerReturnTypeInferencingTests.suite());
        			suite.addTest(GrailsServiceInferencingTests.suite());
 //       			suite.addTest(AllGroovyDebugTests.suite());
        			suite.addTestSuite(GrailsContentAssistTests.class);
        		}
        		suite.addTestSuite(DynamicFinderProposalsTests.class);
                suite.addTestSuite(CaseInsensitiveDynamicFinderProposalsTests.class);
        		suite.addTestSuite(GSPStructuredModelCreationTests.class);
        		suite.addTestSuite(GSPTagsTests.class);
        		suite.addTestSuite(GSPSearchTests.class);
        		suite.addTestSuite(GSPTagDocParserTests.class);
        		suite.addTestSuite(GSPContentAssistTests.class);
        		suite.addTestSuite(PluginDataTests.class);
        		suite.addTestSuite(GSPValidationTests.class);
        		suite.addTestSuite(GrailsCommandTest.class);
        		suite.addTestSuite(GrailsCommandFactoryTest.class);
        		suite.addTestSuite(LaunchSystemPropertiesTest.class);
        		if (GrailsVersion.MOST_RECENT.compareTo(GrailsVersion.V_2_2_)>=0) {
        			suite.addTestSuite(LongRunningGrailsTest.class);
        		}
        		suite.addTestSuite(GrailsCommandWizardExpressionTest.class);
        		suite.addTestSuite(PluginInstallerTests.class);
// Next test disabled: see https://issuetracker.springsource.com/browse/STS-3266
//        		suite.addTestSuite(ExtraPluginInstallerTests.class);
        		suite.addTest(AllGrailsRefactoringTests.suite());
        		suite.addTestSuite(GrailsSourceCodeTest.class);
        		suite.addTestSuite(GSPHyperlinkTests.class);
        		suite.addTestSuite(NamedQueryInferencingTests.class);
        		suite.addTestSuite(GrailsImportWizardCoreTests.class);
        	}
        }
		suite.addTestSuite(ThreadLeakTest.class);
        return suite;
	}

	public static Test suite() {
    	return suite(false);
    }

}
