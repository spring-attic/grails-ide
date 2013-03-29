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

import static org.grails.ide.eclipse.core.launch.GrailsLaunchConfigurationDelegate.getScript;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestRoot;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestElementContainer;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jdt.junit.model.ITestSuiteElement;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.test.util.GrailsTest;
import org.grails.ide.eclipse.ui.internal.launch.GrailsTestLaunchShortcut;
import org.grails.ide.eclipse.ui.internal.launch.OpenInterestingNewResourceListener;
import org.springsource.ide.eclipse.commons.frameworks.test.util.ACondition;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

/**
 * Tests for Eclipse / Junit integration features in the Grails tooling.
 * @author Kris De Volder
 * @author Andrew Eisenberg
 * @created 2010-08-20
 */
public final class GrailsRunAsTestAppTests extends GrailsTest {
	private static final String G_TUNES = "bTunes"; //Use a different name for this 'gTunes' app to avoid confusing grails
													// with lingering state of the other gTunes test app

	//This class made final because at the oment it uses static fields in a way that
	// would break subclasses.

	public static final long TIMEOUT_TEST_APP = 180000;

	private static IProject project;
	GrailsTestLaunchShortcut shortCut = new GrailsTestLaunchShortcut();
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		OpenInterestingNewResourceListener.testMode(true);
		StsTestUtil.setAutoBuilding(false);
		setJava16Compliance();
		if (project==null) {
			//Only first time when creating the project
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(G_TUNES);
			if (project.exists()) {
				project.delete(true, true, new NullProgressMonitor());
			}
			project = ensureProject(G_TUNES);
	
			createResource(project, "grails-app/domain/"+G_TUNES+"/domain/Song.groovy",
					"package "+G_TUNES+".domain\n" + 
					"\n" + 
					"class Song {\n" + 
					"\n" + 
					"    static constraints = {\n" + 
					"		title(blank:false)\n" + 
					"		artist(blank:false)\n" + 
					"    }\n" + 
					"	\n" + 
					"	String title\n" + 
					"	String artist\n" + 
					"	\n" + 
					"}\n");
			
			createResource(project, "test/unit/"+G_TUNES+"/domain/SongTests.groovy",
					"package "+G_TUNES+".domain\n" + 
					"\n" + 
					"import grails.test.*\n" + 
					"\n" + 
					"class SongTests extends GrailsUnitTestCase {\n" + 
					"    protected void setUp() {\n" + 
					"        super.setUp()\n" + 
					"    }\n" + 
					"\n" + 
					"    protected void tearDown() {\n" + 
					"        super.tearDown()\n" + 
					"    }\n" + 
					"\n" + 
					"    void testSomething() {\n" + 
					"    }\n" + 
					"}\n");
			
			createResource(project, "test/integration/"+G_TUNES+"/SongITests.groovy",
					"package "+G_TUNES+"\n" + 
					"\n" + 
					"import "+G_TUNES+".domain.Song;\n" + 
					"import grails.test.*\n" + 
					"\n" + 
					"class SongITests extends GrailsUnitTestCase {\n" + 
					"    protected void setUp() {\n" + 
					"        super.setUp()\n" + 
					"    }\n" + 
					"\n" + 
					"    protected void tearDown() {\n" + 
					"        super.tearDown()\n" + 
					"    }\n" + 
					"\n" + 
					"    void testSomething() {\n" + 
					"		Song song = new Song()\n" + 
					"		song.title = \"foo\"\n" + 
					"		song.artist = \"Kung\"\n" + 
					"		if (song.validate()) {\n" + 
					"			//OK\n" + 
					"		}\n" + 
					"		else {\n" + 
					"			fail \"Validation should be ok!\"\n" + 
					"		}\n" + 
					"    }\n" + 
					"	\n" + 
					"}\n");
			StsTestUtil.assertNoErrors(project);
		}
		//Every test run:
		deleteOldTestReports();
	}
	
	@Override
	protected void tearDown() throws Exception {
		OpenInterestingNewResourceListener.testMode(false);
		super.tearDown();
	}

	private void deleteOldTestReports() throws CoreException {
		IFolder testReports = getTestReportsFolder();
		if (testReports.exists()) {
			testReports.delete(true, new NullProgressMonitor());
		}
	}

	private IFolder getTestReportsFolder() {
		return project.getFolder(new Path("target/test-reports"));
	}

	
	private static abstract class TestVisitor {
		protected abstract void doit(ITestElement e);
		public void visit(ITestElement e) {
			doit(e);
			if (e instanceof TestSuiteElement) {
				ITestElementContainer container = (ITestElementContainer) e;
				ITestElement[] cs = container.getChildren();
				if (cs!=null){
					for (ITestElement c : cs) {
						visit(c);
					}
				}
			}
		}
	}

	public static String getName(ITestElement e) {
		if (e instanceof TestCaseElement) {
			ITestCaseElement tce = (ITestCaseElement) e;
			return tce.getTestClassName() + "."+tce.getTestMethodName();
		} else if (e instanceof ITestSuiteElement) {
			ITestSuiteElement se = (ITestSuiteElement) e;
			return se.getSuiteTypeName();
		} else if (e instanceof ITestRunSession) {
			ITestRunSession trs = (ITestRunSession) e;
			return trs.getTestRunName();
		}
		return null; //Don't know how to obtain a name for this kind of thing.
	}

	private void dump(TestRunSession testResult) {
		new TestVisitor() {
			protected void doit(ITestElement e) {
				System.out.println("testNode: "+getName(e));
			}
		}.visit(testResult.getTestRoot());
	}
	
	private void assertNodeStartingWith(TestRunSession testResult, final String prefix) {
		TestRoot root = testResult.getTestRoot();
		final StringBuilder summary = new StringBuilder();
		final boolean[] found = {false};
		new TestVisitor() {
			protected void doit(ITestElement e) {
				String name = getName(e);
				summary.append(name+"\n");
				if (name.startsWith(prefix)) {
					found[0] = true;
				}
			}
		}.visit(root);
		assertTrue(prefix + " not found in ... \n"+summary, found[0]);
	}

	static class MockTestOpener extends OpenInterestingNewResourceListener {
		public MockTestOpener(IProject project) {
			super(project);
		}

		TestRunSession getTestResults() throws CoreException {
			if (interestingResource!=null) {
				System.out.println("Most interesting report: "+interestingResource);
				
				File testReport = interestingResource.getLocation().toFile();
				return JUnitModel.importTestRunSession(testReport);
			}
			throw new Error("No test reports");
		}
	}
	
	/**
	 * Loads the 'most interesting' new test report 
	 * @return
	 * @throws Exception
	 */
	private TestRunSession loadTestResult() throws Exception {
		IFolder reportsFolder = getTestReportsFolder();
		reportsFolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		MockTestOpener opener = new MockTestOpener(project);
		IResource[] reports = reportsFolder.members();
		for (IResource r : reports) {
			System.out.println("Found test report: "+r);
			opener.newResource(r);
		}
		return opener.getTestResults();
	}
	
	/**
	 * Test that does nothing, we run this test just to ensure that all the setup / scaffolding code works.
	 */
	public void testScaffolding() {
		assertTrue(project.getFile("test/unit/"+G_TUNES+"/domain/SongTests.groovy").exists());
	}
	
	public void testTestAppOnProject() throws Exception {
		IResource target = project;
		
		ILaunchConfiguration launchConf = shortCut.findLaunchConfiguration(target);
		assertEquals("test-app", getScript(launchConf));
		
		TestRunSession testResult = run(launchConf);

		assertEquals(2, testResult.getStartedCount());
		assertEquals(0, testResult.getErrorCount());
		assertEquals(0, testResult.getFailureCount());
		
		assertNodeStartingWith(testResult, ""+G_TUNES+".SongITests");
		assertNodeStartingWith(testResult, ""+G_TUNES+".domain.SongTests");
	}

	private TestRunSession run(ILaunchConfiguration launchConf)
			throws CoreException, Exception {
		final ILaunch launch = launchConf.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		new ACondition() {
			public boolean test() throws Exception {
				return launch.isTerminated();
			}
		}.waitFor(TIMEOUT_TEST_APP);
		TestRunSession testResult = loadTestResult();
		dump(testResult); //So we can see something interesting in the output.
		return testResult;
	}

	public void testTestAppOnIntegrationTestFolder() throws Exception {
		IFolder target = project.getFolder(new Path("test/integration"));
		
		ILaunchConfiguration launchConf = shortCut.findLaunchConfiguration(target);
		assertEquals("test-app -integration", getScript(launchConf));
		
		TestRunSession testResult = run(launchConf);

		assertEquals(1, testResult.getStartedCount());
		assertEquals(0, testResult.getErrorCount());
		assertEquals(0, testResult.getFailureCount());
		
		assertNodeStartingWith(testResult, ""+G_TUNES+".SongITests");
	}
	
	public void testTestAppOnUnitTestFolder() throws Exception {
		IFolder target = project.getFolder(new Path("test/unit"));
		
		ILaunchConfiguration launchConf = shortCut.findLaunchConfiguration(target);
		assertEquals("test-app -unit", getScript(launchConf));
		
		TestRunSession testResult = run(launchConf);
		
		assertEquals(1, testResult.getStartedCount());
		assertEquals(0, testResult.getFailureCount());
		assertEquals(0, testResult.getErrorCount());
		assertNodeStartingWith(testResult, ""+G_TUNES+".domain.SongTests");

	}

	public void testTestAppOnTestFile() throws Exception {
		IFile target = project.getFile(new Path("test/integration/"+G_TUNES+"/SongITests.groovy"));
		
		ILaunchConfiguration launchConf = shortCut.findLaunchConfiguration(target);
		assertEquals("test-app -integration "+G_TUNES+".SongITests", getScript(launchConf));
		
		TestRunSession testResult = run(launchConf);
		
		assertEquals(1, testResult.getStartedCount());
		assertEquals(0, testResult.getFailureCount());
		assertEquals(0, testResult.getErrorCount());
		assertNodeStartingWith(testResult, ""+G_TUNES+".SongITests");
		
	}
	
	public void testTestAppOnPackage() throws Exception {
		IFolder target = project.getFolder(new Path("grails-app/domain/"+G_TUNES+"/domain"));
		
		ILaunchConfiguration launchConf = shortCut.findLaunchConfiguration(target);
		assertEquals("test-app "+G_TUNES+".domain.*", getScript(launchConf));
		
		TestRunSession testResult = run(launchConf);
		
		assertEquals(1, testResult.getStartedCount());
		assertEquals(0, testResult.getFailureCount());
		assertEquals(0, testResult.getErrorCount());
		assertNodeStartingWith(testResult, ""+G_TUNES+".domain.SongTests");
	}
	
	public void testTestAppOnPackageInUnitTests() throws Exception {
		IFolder target = project.getFolder(new Path("test/unit/"+G_TUNES+"/domain"));
		
		ILaunchConfiguration launchConf = shortCut.findLaunchConfiguration(target);
		assertEquals("test-app -unit "+G_TUNES+".domain.*", getScript(launchConf));
		
		TestRunSession testResult = run(launchConf);
		
		assertEquals(1, testResult.getStartedCount());
		assertEquals(0, testResult.getFailureCount());
		assertEquals(0, testResult.getErrorCount());
		
		assertNodeStartingWith(testResult, ""+G_TUNES+".domain.SongTests");
	}
	
	public void testTestAppOnSourceFile() throws Exception {
		IFile target = project.getFile(new Path("grails-app/domain/"+G_TUNES+"/domain/Song.groovy"));
		
		ILaunchConfiguration launchConf = shortCut.findLaunchConfiguration(target);
		assertEquals("test-app "+G_TUNES+".domain.Song", getScript(launchConf));
		
		TestRunSession testResult = run(launchConf);
		
		assertEquals(1, testResult.getStartedCount());
		assertEquals(0, testResult.getFailureCount());
		assertEquals(0, testResult.getErrorCount());
		assertNodeStartingWith(testResult, ""+G_TUNES+".domain.SongTests");
		
	}
}
