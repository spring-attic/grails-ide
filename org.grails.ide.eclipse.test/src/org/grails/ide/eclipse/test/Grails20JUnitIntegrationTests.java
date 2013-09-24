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

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.junit.launcher.ITestFinder;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.launching.JavaRuntime;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.core.junit.Grails20AwareTestFinder;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.osgi.framework.Bundle;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;
import org.grails.ide.eclipse.test.util.AbstractGrailsJUnitIntegrationsTest;
import org.grails.ide.eclipse.test.util.GrailsTest;

/**
 * Tests for the 'run as >> Junit test' functionality provided by JDT. We are testing here whether
 * our hackery intended to make it work with Grails 2.0 actually does what it is intended to do,
 * while not breaking JUnit 3 style test finding and running.
 * 
 * @author Kris De Volder
 *
 * @since 2.9
 */
public class Grails20JUnitIntegrationTests extends AbstractGrailsJUnitIntegrationsTest {
	
	public final String TEST_PROJECT_NAME = this.getClass().getSimpleName();
	private IProject project;
	private IJavaProject javaProject;
	
	private String packageName; // name of the package most classes are in
	private String domainClassName; // name of the single domain class present in the 'base-line' test application.
	private String testDomainClassName; // name of the test class containing tests for the domain class.
	private String domainClassBaseName;
	private String generatedTestMethodName;

	private static boolean domainClassCreated = false;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		assertTrue("This test assumes Grails 20 but most recent Grails version is "+GrailsVersion.MOST_RECENT, 
				GrailsVersion.MOST_RECENT.compareTo(GrailsVersion.V_2_0_0)>=0);
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		setJava16Compliance();
		
		project = ensureProject(TEST_PROJECT_NAME);
		javaProject = JavaCore.create(project);

		//Add a generated domain class to make the base-line test project a bit more interesting.
		packageName = TEST_PROJECT_NAME.toLowerCase();
		domainClassBaseName = "Song";
		domainClassName =  packageName + "." + domainClassBaseName;
		if (!domainClassCreated) {
			GrailsCommand cmd = GrailsCommandFactory.createDomainClass(project, domainClassName);
			ILaunchResult result = cmd.synchExec();
			System.out.println(result.getOutput());
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			domainClassCreated = true;
		}
		
		if (GrailsVersion.MOST_RECENT.compareTo(GrailsVersion.V_2_3_)>=0) {
			testDomainClassName = domainClassName + "Spec";
			generatedTestMethodName = "test something";
			//Replace generated 2.3.0 spock test with a test that actually has some test code in it.
			createResource(project, "test/unit/grails20junitintegrationtests/SongSpec.groovy", 
					"package grails20junitintegrationtests\n" + 
					"\n" + 
					"import grails.test.mixin.TestFor\n" + 
					"import spock.lang.Specification\n" + 
					"\n" + 
					"/**\n" + 
					" * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions\n" + 
					" */\n" + 
					"@TestFor(Song)\n" + 
					"class SongSpec extends Specification {\n" + 
					"\n" + 
					"    def setup() {\n" + 
					"    }\n" + 
					"\n" + 
					"    def cleanup() {\n" + 
					"    }\n" + 
					"\n" + 
					"    void \"test something\"() {\n" + 
					"		expect: \"Implement me\"==\"not implemented\"\n" + 
					"    }\n" + 
					"}\n"
			);
			//Ensure project gets built with new SongSpec class.
			StsTestUtil.assertNoErrors(project);
		} else {
			testDomainClassName = domainClassName + "Tests";
			generatedTestMethodName = "testSomething";
		}
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testScaffolding() throws Exception {
		StsTestUtil.assertNoErrors(project);
	}
	
	public void testOurTestFinderIsInstalled() {
		ITestFinder finder = getJUnit4TestFinder();
		assertTrue(finder instanceof Grails20AwareTestFinder);
	}

//	/**
//	 * For some unkown reason, JUnit 4 classpath container isn't resolving on the build server.
//	 * This test is trying to determine why. It does so by using a local copy of the P2Utils class
//	 * that is used by the JUnit support in jdt to find the junit 4 bundle and extract a jar file location from it.
//	 */
//	public void testJUnit4BundlesAndStuff() throws Exception {
//		IClasspathEntry junit4lib = BuildPathSupport.getJUnit4LibraryEntry();
//		System.out.println("Junit 4 library entry = "+junit4lib);
//		
//		BundleInfo bundleInfo = P2Utils.findBundle("org.junit", new VersionRange("[4.7.0,5.0.0)"), false);
//		System.out.println("bundleInfo = "+bundleInfo); //Example of what we expect: BundleInfo(org.junit, 4.8.2.v4_8_2_v20110321-1705, location=file:/home/kdvolder/Applications/springsource-2.9.0.M1/sts-2.9.0.M1/plugins/org.junit_4.8.2.v4_8_2_v20110321-1705/, startLevel=4, toBeStarted=false, resolved=false, id=-1,no manifest)
//		
//		IPath bundleLocation = P2Utils.getBundleLocationPath(bundleInfo);
//		System.out.println("bundle location = "+bundleLocation);
//		
//		assertNotNull(bundleLocation);
//		
//		//What this test is printing on my machine (where the JUnit tests are passing ok)
//		/*
//		 Junit 4 library entry = /home/kdvolder/Applications/springsource-2.9.0.M1/sts-2.9.0.M1/plugins/org.junit_4.8.2.v4_8_2_v20110321-1705/junit.jar[CPE_LIBRARY][K_BINARY][sourcePath:/home/kdvolder/Applications/springsource-2.9.0.M1/sts-2.9.0.M1/plugins/org.junit.source_4.8.2.v4_8_2_v20110321-1705.jar][isExported:false][attributes:javadoc_location=http://www.junit.org/junit/javadoc/4.5]
//		 bundleInfo = BundleInfo(org.junit, 4.8.2.v4_8_2_v20110321-1705, location=file:/home/kdvolder/Applications/springsource-2.9.0.M1/sts-2.9.0.M1/plugins/org.junit_4.8.2.v4_8_2_v20110321-1705/, startLevel=4, toBeStarted=false, resolved=false, id=-1,no manifest)
//         /home/kdvolder/Applications/springsource-2.9.0.M1/sts-2.9.0.M1/plugins/org.junit_4.8.2.v4_8_2_v20110321-1705/
//		 */
//	}

	/**
	 * Test whether test finder still works for a pretty standard JDT Java project.
	 */
	public void testDontBreakJDT() throws Exception {
		ITestFinder testFinder = getJUnit4TestFinder();
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("sts2312");
		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());
		
		IProjectDescription desc = project.getDescription();
		String[] natures = desc.getNatureIds();
		String[] newNatures = new String[natures.length+1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = JavaCore.NATURE_ID;
		desc.setNatureIds(newNatures);
		project.setDescription(desc, new NullProgressMonitor());
		
		javaProject = JavaCore.create(project);
		
		IPath outputLocation = new Path("/"+project.getName()+"/bin");
		IClasspathEntry[] entries = new IClasspathEntry[] {
				JavaCore.newContainerEntry(JavaRuntime.newDefaultJREContainerPath()),
				getJUnit4JarEntry(),
				JavaCore.newSourceEntry(new Path("/"+project.getName()+"/src")),
				JavaCore.newSourceEntry(new Path("/"+project.getName()+"/test")),
		};
		javaProject.setRawClasspath(entries, outputLocation, new NullProgressMonitor());
		
		createResource(project, "src/foo/Main.java", 
				"package foo;\n" +
				"public class Main {\n" +
				"  public static void main(String[] args) {\n" +
				"    System.out.println(\"Hello\");\n" +
				"  }" +
				"}");
		
		createResource(project, "test/foo/Test1.java", 
				"package foo;\n" +
				"\n" +
				"import junit.framework.TestCase;\n" +
				"\n" +
				"public class Test1 extends TestCase {\n" +
				"  public void testSomething(String[] args) {\n" +
				"    System.out.println(\"Hello\");\n" +
				"  }" +
				"}");
		
		createResource(project, "src/foo/Test2.java", 
				"package foo;\n" +
				"\n" +
				"import junit.framework.TestCase;\n" +
				"\n" +
				"public class Test2 extends TestCase {\n" +
				"  public void testSomething(String[] args) {\n" +
				"    System.out.println(\"Hello\");\n" +
				"  }" +
				"}");
		
		StsTestUtil.dumpClasspathInfo(javaProject);
		
		StsTestUtil.assertNoErrors(project);
		IType test1 = javaProject.findType("foo.Test1");
		IType test2 = javaProject.findType("foo.Test2");
		
		assertNotNull(test1);
		assertNotNull(test2);
		assertTrue(testFinder.isTest(test1));
		assertTrue(testFinder.isTest(test2));
		{	// find in project
			HashSet result = new HashSet();
			testFinder.findTestsInContainer(javaProject, result, new NullProgressMonitor());
			assertElements(result, test1, test2);
		}
		{   //find in type
			HashSet result = new HashSet();
			testFinder.findTestsInContainer(test1, result, new NullProgressMonitor());
			assertElements(result, test1);
		}
		{   //find in type
			HashSet result = new HashSet();
			testFinder.findTestsInContainer(test2, result, new NullProgressMonitor());
			assertElements(result, test2);
		}
	}
	
	
	private IClasspathEntry getJUnit4JarEntry() throws IOException {
		Bundle[] bundles = Platform.getBundles("org.junit", "4.7.0");
		File bundleFile = FileLocator.getBundleFile(bundles[0]);
		assertTrue("bundleFile doesn't exist: "+bundleFile, bundleFile.exists());
		assertTrue("bundleFile isn't a directory: "+bundleFile, bundleFile.isDirectory());
		for (File file : bundleFile.listFiles()) {
			if (file.getName().equals("junit.jar")) {
				return JavaCore.newLibraryEntry(new Path(file.toString()), null, null, true);
			}
		}
		fail("Couldn't find Junit jar file");
		return null; //unreachable code
	}

	/**
	 * Test basic function of the 'isTest' method in the test finder:
	 *   - a domain class isn't a test
	 *   - a class in the test/unit source folder is a test
	 *    
	 * @throws Exception
	 */
	public void testTestFinderIsTest() throws Exception {
		ITestFinder testFinder = getJUnit4TestFinder();
		
		IType domainClass = javaProject.findType(domainClassName);
		assertNotNull(domainClass);
		assertFalse(testFinder.isTest(domainClass));
		
		IType testClass = javaProject.findType(testDomainClassName);
		assertNotNull(testClass);
		assertTrue(testFinder.isTest(testClass));
		
		createTmpResource(project, "test/integration/"+packageName+"/FoobarTests.groovy", 
				"package grails20junitintegrationtests\n" + 
				"\n" + 
				"import grails.test.mixin.*\n" + 
				"\n" + 
				"@TestFor("+domainClassBaseName+")\n" + 
				"class FoobarTests {\n" + 
				"\n" + 
				"	void testThisAndThat() {\n" + 
				"		assertEquals 4, 4\n" + 
				"	}\n" + 
				"	\n" + 
				"}");
		StsTestUtil.waitForAutoBuild();
		
		IType integTests = javaProject.findType(packageName+".FoobarTests");
		assertNotNull(integTests);
		assertTrue(testFinder.isTest(integTests));
	}


	/**
	 * Test slightly more 'advanced' cases. 
	 *  - a helper class present in the unit testing source folder shouldn't be
	 *    recognized as a test class.
	 *  
	 * @throws Exception
	 */
	public void testTestFinderIsTest2() throws Exception {
		ITestFinder testFinder = getJUnit4TestFinder();
		
		createTmpResource(project, "test/unit/"+packageName+"/Helper.groovy", 
				"package "+packageName+"\n" + 
				"\n" + 
				"class Helper {\n" + 
				"\n" + 
				"	void help() {\n" + 
				"		println 'help!'\n" + 
				"	}\n" + 
				"}");
		StsTestUtil.waitForAutoBuild();
		IType clazz = javaProject.findType(packageName+".Helper");
		assertNotNull(clazz);
		assertFalse(testFinder.isTest(clazz));
	}
	
	public void testTestFinderFindTest() throws Exception {
		ITestFinder testFinder = getJUnit4TestFinder();
		
		createTmpResource(project, "test/unit/"+packageName+"/Helper.groovy", 
				"package "+packageName+"\n" + 
				"\n" + 
				"class Helper {\n" + 
				"\n" + 
				"	void help() {\n" + 
				"		println 'help!'\n" + 
				"	}\n" + 
				"}");
		StsTestUtil.waitForAutoBuild();
		IType clazz = javaProject.findType(packageName+".Helper");
		assertNotNull(clazz);
	
		//1) Search in a cu that doesn't have a test class inside.
		Set<Object> result = new HashSet<Object>();
		testFinder.findTestsInContainer(clazz.getCompilationUnit(), result, new NullProgressMonitor());
		assertEquals(0, result.size());
		
		//2) Search in a cu that does have a test class inside
		result = new HashSet<Object>();
		clazz = javaProject.findType(testDomainClassName);
		testFinder.findTestsInContainer(clazz.getCompilationUnit(), result, new NullProgressMonitor());
		GrailsTest.assertElements(result, clazz);
		
		//3) Search in test/unit package fragment root
		result = new HashSet<Object>();
		IJavaElement pfr = clazz.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		testFinder.findTestsInContainer(pfr, result, new NullProgressMonitor());
		GrailsTest.assertElements(result, clazz);
		
		//4) Same as above, but add an extra test class to see if that's also found.
		createTmpResource(project, "test/unit/"+packageName+"/FoobarTests.groovy", 
				"package grails20junitintegrationtests\n" + 
				"\n" + 
				"import grails.test.mixin.*\n" + 
				"\n" + 
				"@TestFor("+domainClassBaseName+")\n" + 
				"class FoobarTests {\n" + 
				"\n" + 
				"	void testThisAndThat() {\n" + 
				"		assertEquals 4, 4\n" + 
				"	}\n" + 
				"	\n" + 
				"}");
		StsTestUtil.waitForAutoBuild();
		IType extraTestClass = javaProject.findType(packageName+".FoobarTests");
		testFinder.findTestsInContainer(pfr, result, new NullProgressMonitor());
		GrailsTest.assertElements(result, clazz, extraTestClass);
		
		//5) One more time, now search the whole project
		result = new HashSet<Object>();
		testFinder.findTestsInContainer(javaProject, result, new NullProgressMonitor());
		GrailsTest.assertElements(result, clazz, extraTestClass);
	}
	
	public void testTestFinderFindTestInSourceFileWithoutATestClass() throws Exception {
		ITestFinder testFinder = getJUnit4TestFinder();
		
		createTmpResource(project, "test/unit/"+packageName+"/Helper.groovy", 
				"package "+packageName+"\n" + 
				"\n" + 
				"class Helper {\n" + 
				"\n" + 
				"	void help() {\n" + 
				"		println 'help!'\n" + 
				"	}\n" + 
				"}");
		StsTestUtil.waitForAutoBuild();
		IType clazz = javaProject.findType(packageName+".Helper");
		assertNotNull(clazz);
		
		Set<Object> result = new HashSet<Object>();
		testFinder.findTestsInContainer(clazz.getCompilationUnit(), result, new NullProgressMonitor());
		assertEquals(0, result.size());
	}
	
	@SuppressWarnings("rawtypes")
	public void testThatASTTransformGotExecuted() throws Exception {
		//We check this indirectly, by checking whether the TestFor annotation exists on the testMethod, as expected, in the
		//byte code... We do this by creating a URLClassLoader from the projects resolved runtime classpath and then loading
		//up the test class.
		
		//This test only applies to 'regular' tests with the @TestFor annotation on the class. 
		// It doesn't apply to the spock test generated in Grails 2.3 so we create a old style test
		// here to verify this still works.
		
		createTmpResource(project, "test/unit/"+packageName+"/FoobarTests.groovy", 
				"package grails20junitintegrationtests\n" + 
				"\n" + 
				"import grails.test.mixin.*\n" + 
				"\n" + 
				"@TestFor("+domainClassBaseName+")\n" + 
				"class FoobarTests {\n" + 
				"\n" + 
				"	void testThisAndThat() {\n" + 
				"		assertEquals 4, 4\n" + 
				"	}\n" + 
				"	\n" + 
				"}");
		//Build project is required for the new code to be compiled by Eclipse:
		StsTestUtil.assertNoErrors(project);
		
		URLClassLoader classLoader = getRuntimeClassLoader(javaProject);
		Class testClass = Class.forName(packageName+".FoobarTests", false, classLoader);
		Method m = getMethod(testClass, "testThisAndThat"); 
		assertAnnotation(m, "org.junit.Test");
	}

	/** 
	 * Test a simple JUnit test run, while registering a test listener to capture the results.
	 * 
	 * @throws Exception
	 */
	public void testSimpleRun() throws Exception {
		TestRunSession session = runAsJUnit(javaProject);
		assertTestFailure(session, "Implement me", testDomainClassName, generatedTestMethodName);
		
		//Also check the numbers of test run, failed etc.
		assertEquals(1, session.getStartedCount());
		assertEquals(0, session.getErrorCount());
		assertEquals(1, session.getFailureCount());
		assertEquals(1, session.getTotalCount());
	}

	/**
	 * Verify whether test finder and runner is still capable of identifying, finding and running 
	 * JUnit 3 style test types.
	 */
	public void testFinderJUnit3Compatibility() throws Exception {
		createTmpResource(project, "test/unit/"+packageName+"/ExtraTests.groovy", 
				"package "+packageName+"\n" + 
				"\n" + 
				"import grails.test.GrailsUnitTestCase;\n" + 
				"\n" + 
				"class ExtraTests extends GrailsUnitTestCase {\n" + 
				"	\n" + 
				"	void testSomething() {\n" + 
				"		fail('Testing JUnit3 compatibility')\n" + 
				"	}\n" + 
				"\n" + 
				"}"
		);
		
		StsTestUtil.waitForAutoBuild();
		
		//Is it a test?
		IType type = javaProject.findType(packageName+".ExtraTests");
		ITestFinder testFinder = getJUnit4TestFinder();
		assertTrue(testFinder.isTest(type));

		//Can we find the test?
		HashSet<Object> result = new HashSet<Object>();
		testFinder.findTestsInContainer(javaProject, result, new NullProgressMonitor());
		assertElements(result, 
				type,
				javaProject.findType(testDomainClassName)
		);

		//Can we run the test and get expected result?
		TestRunSession session = runAsJUnit(type);
		assertTestFailure(session, "Testing JUnit3 compatibility", 
				type.getFullyQualifiedName(), "testSomething");

	}

	public void testDoIncludeIntegrationTestsIfJUnit3() throws Exception {
		createTmpResource(project, "test/integration/"+packageName+"/ExtraTests.groovy", 
				"package "+packageName+"\n" + 
				"\n" + 
				"import grails.test.GrailsUnitTestCase;\n" + 
				"\n" + 
				"class ExtraTests extends GrailsUnitTestCase {\n" + 
				"	\n" + 
				"	void testSomething() {\n" + 
				"		fail('Testing JUnit3 compatibility')\n" + 
				"	}\n" + 
				"\n" + 
				"}"
		);
		
		StsTestUtil.waitForAutoBuild();
		
		//Is it a test? (Should say YES) see https://issuetracker.springsource.com/browse/STS-2481
		IType type = javaProject.findType(packageName+".ExtraTests");
		ITestFinder testFinder = getJUnit4TestFinder();
		assertTrue(testFinder.isTest(type));
		
		//Can we find the test? (SHOULD) see https://issuetracker.springsource.com/browse/STS-2481
		HashSet<Object> result = new HashSet<Object>();
		testFinder.findTestsInContainer(javaProject, result, new NullProgressMonitor());
		assertElements(result, 
				javaProject.findType(testDomainClassName),
				type
		);
	}
	
	public void testDoIncludeGrailsIntegrationTests() throws Exception {
		String className = "SongITests";
		createTmpResource(project, "test/integration/"+packageName+"/"+className+".groovy", 
				"package "+packageName+"\n" + 
				"\n" + 
				"import static org.junit.Assert.*\n" + 
				"import grails.test.mixin.TestFor\n" + 
				"\n" + 
				"import org.junit.*\n" + 
				"\n" + 
				"@TestFor(Song)\n" + 
				"class "+className+ " {\n" + 
				"\n" + 
				"    void testSomething() {\n" + 
				"        fail \"Implement me\"\n" + 
				"    }\n" + 
				"}\n");
		StsTestUtil.waitForAutoBuild();
		
		//Is it a test? (Should say YES) see: https://issuetracker.springsource.com/browse/STS-2481
		IType type = javaProject.findType(packageName+"."+className);
		ITestFinder testFinder = getJUnit4TestFinder();
		assertTrue(testFinder.isTest(type));
		
		//Can we find the test? (SHOULD) see: https://issuetracker.springsource.com/browse/STS-2481
		HashSet<Object> result = new HashSet<Object>();
		testFinder.findTestsInContainer(javaProject, result, new NullProgressMonitor());
		assertElements(result, 
				javaProject.findType(testDomainClassName),
				type
		);
		
		//Run test with expected result?
		TestRunSession session = runAsJUnit(javaProject);
		assertTestFailure(session, "Implement me", type.getFullyQualifiedName(), "testSomething");
		
	}
	
	public void testDoIncludeIntegrationTestsIfJUnit4() throws Exception {
		String className = "SampleIntegrationTest";
		createTmpResource(project, "test/integration/"+packageName+"/"+className+".groovy", 
				"package "+packageName+"\n" +
				"\n" +
				"import org.junit.Test\n" + 
				"import static org.junit.Assert.*\n" + 
				"\n" + 
				"class "+className+" {\n" + 
				"	\n" + 
				"	@Test\n" + 
				"	void hello() {\n" + 
				"		assertTrue(true)\n" + 
				"	}\n" + 
				"\n" + 
				"}\n"
		);
		
		StsTestUtil.waitForAutoBuild();
		
		//Is it a test? (Should say YES) see: https://issuetracker.springsource.com/browse/STS-2481
		IType type = javaProject.findType(packageName+"."+className);
		ITestFinder testFinder = getJUnit4TestFinder();
		assertTrue(testFinder.isTest(type));
		
		//Can we find the test? (SHOULD) see: https://issuetracker.springsource.com/browse/STS-2481
		HashSet<Object> result = new HashSet<Object>();
		testFinder.findTestsInContainer(javaProject, result, new NullProgressMonitor());
		assertElements(result, 
				javaProject.findType(testDomainClassName),
				type
		);
	}
	
	public void testOrdinaryJUNit4TestsInGrailsProject() throws Exception {
		String className = "FooTest";
		createTmpResource(project, "src/java/"+packageName+"/"+className+".java", 
				"package "+packageName+";\n" + 
				"\n" + 
				"import static org.junit.Assert.*;\n" + 
				"\n" + 
				"import org.junit.Test;\n" + 
				"\n" + 
				"public class "+className+" {\n" + 
				"\n" + 
				"	@Test\n" + 
				"	public void oneThatFails() {\n" + 
				"		fail(\"not implemented\");\n" + 
				"	}\n" + 
				"\n" + 
				"}\n");
		
		StsTestUtil.waitForAutoBuild();
		
		//Is it a test? (Should say YES) see: https://issuetracker.springsource.com/browse/STS-2481
		IType type = javaProject.findType(packageName+"."+className);
		ITestFinder testFinder = getJUnit4TestFinder();
		assertTrue(testFinder.isTest(type));
		
		//Can we find the test? (SHOULD) see: https://issuetracker.springsource.com/browse/STS-2481
		HashSet<Object> result = new HashSet<Object>();
		testFinder.findTestsInContainer(javaProject, result, new NullProgressMonitor());
		assertElements(result, 
				javaProject.findType(testDomainClassName),
				type
		);
		
		//Can we run the test and get expected result?
		TestRunSession session = runAsJUnit(javaProject);
		assertTestFailure(session, "not implemented", type.getFullyQualifiedName(), "oneThatFails");
	}
	
	public void testOrdinaryJUNit3TestsInGrailsProject() throws Exception {
		String className = "FooTest";
		createTmpResource(project, "src/java/"+packageName+"/"+className+".java", 
				"package grails20junitintegrationtests;\n" + 
				"\n" + 
				"import static org.junit.Assert.*;\n" + 
				"import junit.framework.TestCase;\n" + 
				"\n" + 
				"import org.junit.Test;\n" + 
				"\n" + 
				"public class "+className+ " extends TestCase {\n" + 
				"	\n" + 
				"	public void testThatFails() {\n" + 
				"		fail(\"not implemented\");\n" + 
				"	}\n" + 
				"	\n" + 
				"}\n" + 
				"");

		StsTestUtil.waitForAutoBuild();

		//Is it a test? (Should say YES) see: https://issuetracker.springsource.com/browse/STS-2481
		IType type = javaProject.findType(packageName+"."+className);
		ITestFinder testFinder = getJUnit4TestFinder();
		assertTrue(testFinder.isTest(type));

		//Can we find the test? (SHOULD) see: https://issuetracker.springsource.com/browse/STS-2481
		HashSet<Object> result = new HashSet<Object>();
		testFinder.findTestsInContainer(javaProject, result, new NullProgressMonitor());
		assertElements(result, 
				javaProject.findType(testDomainClassName),
				type
		);

		//Can we run the test and get expected result?
		TestRunSession session = runAsJUnit(javaProject);
		assertTestFailure(session, "not implemented", type.getFullyQualifiedName(), "testThatFails");
	}

	/**
	 * When a Grails unit test class doesn't have the @TestFor annotation, but it has 
	 * a name SomeDomainTests and there's a corresponding SomeDomain class, then
	 * it should be treated the same as if the @TestFor(SomeDomain) annotation was present.
	 */
	public void testNoAnnotationButCorrespondingDomainClassExists() throws Exception {
//		Grails20TestSupport.DEBUG = true; //Compile will fail until Greclipse update site has newer version.
		try {
			tmpReplaceResource(project, "test/unit/"+packageName+"/"+domainClassBaseName+"Tests.groovy", 
					"package "+packageName+"\n" + 
							"\n" + 
							//@TestFor !!!!ANNOTATION REMOVED!!!!
							"class "+domainClassBaseName+"Tests {\n" + 
							"	\n" + 
							"	void testSomething() {\n" + 
							"		fail('Testing JUnit3 compatibility')\n" + 
							"	}\n" + 
							"}"
					);
			StsTestUtil.waitForAutoBuild();
			StsTestUtil.assertNoErrors(project);

			//Is it a test?
			IType type = javaProject.findType(testDomainClassName);
			ITestFinder testFinder = getJUnit4TestFinder();
			assertTrue(testFinder.isTest(type));

			//Can we find the test?
			HashSet<Object> result = new HashSet<Object>();
			testFinder.findTestsInContainer(javaProject, result, new NullProgressMonitor());
			assertElements(result, 
					javaProject.findType(testDomainClassName)
					);

			//Did the transform run?
			URLClassLoader classLoader = getRuntimeClassLoader(javaProject);
			
			Class testClass = Class.forName(testDomainClassName, false, classLoader);
			Method m = getMethod(testClass, "testSomething"); 
			assertAnnotation(m, "org.junit.Test"); //should have been added by the transform.

			//Can we run the test
			TestRunSession session = runAsJUnit(javaProject);
			assertTestFailure(session, "Testing JUnit3 compatibility", testDomainClassName, "testSomething");

			//Also check the numbers of test run, failed etc.
			assertEquals(1, session.getStartedCount());
			assertEquals(0, session.getErrorCount());
			assertEquals(1, session.getFailureCount());
			assertEquals(1, session.getTotalCount());
		} finally {
//			Grails20TestSupport.DEBUG = false;
		}
	}
	
	private void assertAnnotation(Method m, String qName) {
		for (Annotation a : m.getAnnotations()) {
			String foundName = a.annotationType().getName();
			if (foundName.equals(qName)) {
				return;
			}
		}
		fail("Annotation "+qName+" not found on "+m);
	}

	private Method getMethod(Class<?> clazz, String name) {
		for (Method m : clazz.getMethods()) { 
			if (m.getName().equals(name)) {
				return m;
			}
		}
		return null;
	}
	
}
