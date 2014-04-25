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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.junit.launcher.ITestFinder;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.JUnitMigrationDelegate;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestElementContainer;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.springsource.ide.eclipse.commons.frameworks.test.util.ACondition;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;


/**
 * @author Kris De Volder
 *
 * @since 2.9
 */
public class AbstractGrailsJUnitIntegrationsTest extends GrailsTest {

	public static class MockTestRunListener extends TestRunListener {
		public ITestRunSession session = null; //captured when the session ends.
		@Override
		public void sessionFinished(ITestRunSession session) {
			assertNull(this.session);
			this.session = session;
		}
	}

	public static ITestFinder getJUnit4TestFinder() {
		String testKind = TestKindRegistry.JUNIT4_TEST_KIND_ID;
		return getTestFinder(testKind);
	}

	public static ITestFinder getTestFinder(String testKind) {
		TestKindRegistry registry = TestKindRegistry.getDefault();
		ITestKind junit4kind = registry.getKind(testKind);
		ITestFinder finder = junit4kind.getFinder();
		return finder;
	}

	/**
	 * Constructs a class loader based on the given javaProject's resolved runtime classpath, using
	 * a launch configuration equivalent to the one created by "run as >> JUnit test" launch shortcut.
	 */
	public static URLClassLoader getRuntimeClassLoader(IJavaProject javaProject)
			throws CoreException, MalformedURLException {
				ILaunchConfigurationWorkingCopy wc = createLaunchConfiguration(javaProject);
				JUnitLaunchConfigurationDelegate delegate = new JUnitLaunchConfigurationDelegate();
				String[] classpath = delegate.getClasspath(wc);
//				System.out.println(">>> Creating JUnit runtime classloader with entries:");
				URL[] classpathURLs = new URL[classpath.length];
				for (int i = 0; i < classpathURLs.length; i++) {
//					System.out.println(classpath[i]);
					classpathURLs[i] = new File(classpath[i]).toURI().toURL();
				}
//				System.out.println("<<< Creating JUnit runtime classloader");
				URLClassLoader classLoader = new URLClassLoader(classpathURLs);
				return classLoader;
			}

	/**
	 * COPIED from JUnitLaunchShortcut... create a JUnit lauch config just like the one the JUnit UI would.
	 */
	public static ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element)
			throws CoreException {
				final String testName;
				final String mainTypeQualifiedName;
				final String containerHandleId;
			
				switch (element.getElementType()) {
					case IJavaElement.JAVA_PROJECT:
					case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					case IJavaElement.PACKAGE_FRAGMENT: {
						String name= element.getElementName();
						containerHandleId= element.getHandleIdentifier();
						mainTypeQualifiedName= "";
						testName= name.substring(name.lastIndexOf(IPath.SEPARATOR) + 1);
					}
					break;
					case IJavaElement.TYPE: {
						containerHandleId= "";
						mainTypeQualifiedName= ((IType) element).getFullyQualifiedName('.'); // don't replace, fix for binary inner types
						testName= element.getElementName();
					}
					break;
					case IJavaElement.METHOD: {
						IMethod method= (IMethod) element;
						containerHandleId= "";
						mainTypeQualifiedName= method.getDeclaringType().getFullyQualifiedName('.');
						testName= method.getDeclaringType().getElementName() + '.' + method.getElementName();
					}
					break;
					default:
						throw new IllegalArgumentException("Invalid element type to create a launch configuration: " + element.getClass().getName()); //$NON-NLS-1$
				}
			
				String testKindId= TestKindRegistry.getContainerTestKindId(element);
			
				ILaunchConfigurationType configType= getLaunchManager().getLaunchConfigurationType(JUnitLaunchConfigurationConstants.ID_JUNIT_APPLICATION);
				ILaunchConfigurationWorkingCopy wc= configType.newInstance(null, getLaunchManager().generateLaunchConfigurationName(testName));
			
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainTypeQualifiedName);
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, element.getJavaProject().getElementName());
				wc.setAttribute(JUnitLaunchConfigurationConstants.ATTR_KEEPRUNNING, false);
				wc.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, containerHandleId);
				wc.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, testKindId);
				JUnitMigrationDelegate.mapResources(wc);
				//AssertionVMArg.setArgDefault(wc);
				if (element instanceof IMethod) {
					wc.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_METHOD_NAME, element.getElementName()); // only set for methods
				}
				return wc;
			}

	/**
	 * COPIED from JUnitLaunchShortcut... create a JUnit launch config just like the one the JUnit UI would.
	 */
	public static ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}

	/**
	 * Assert that a given test run session contains a certain test, which failed, and who's
	 * stacktrace contains a given String.
	 * <p>
	 * The test is identified by a 'path' where each segment of the path is the name of a Test element in
	 * the test tree, leading to a test of interest.
	 */
	public static void assertTestFailure(ITestRunSession session, String expectedFailureSnippet,
			String... pathToTest) {
		ITestElement node = findTestNode(session, pathToTest, 0);
		TestCaseElement element = (TestCaseElement) node;
		assertEquals(TestElement.Status.FAILURE, element.getStatus());
		assertContains(expectedFailureSnippet, element.getTrace());
	}

	public static ITestElement findTestNode(ITestElement node, String[] pathToTest, int pos) {
		if (pos<pathToTest.length) {
			String head = pathToTest[pos];
			if (node instanceof ITestElementContainer) {
				ITestElementContainer container = (ITestElementContainer) node;
				for (ITestElement child : container.getChildren()) {
					if (head.equals(getName(child))) {
						return findTestNode(child, pathToTest, pos+1);
					}
					System.out.println(child);
				}
			}
			fail("Test not found: "+head);
		} else {
			return node;
		}
		return null; //Unreachable because of 'fail' calls above, but Java compiler doesn't know this.
	}

	public static String getName(ITestElement child) {
		if (child instanceof TestElement) {
			String name = ((TestElement) child).getTestName();
			int chop = name.indexOf('(');
			if (chop>=0) {
				name = name.substring(0, chop);
			}
			return name;
		}
		return null;
	}

	public static TestRunSession runAsJUnit(IJavaElement element) throws CoreException, Exception, DebugException {
		StsTestUtil.assertNoErrors(element.getJavaProject().getProject()); //This is to avoid trying to run a project with errors...
																			// Trying to do so would hang the test when debug UI pops up a dialog.
		ILaunchConfigurationWorkingCopy launchConf = createLaunchConfiguration(element);
		final MockTestRunListener listener = new MockTestRunListener();
		JUnitCore.addTestRunListener(listener);
		try {
			final ILaunch launch = launchConf.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor(), true);

			new ACondition() {
				@Override
				public boolean test() throws Exception {
					return listener.session!=null && launch.isTerminated();
				}
			}.waitFor(40000);
			assertEquals(0, launch.getProcesses()[0].getExitValue());

		} finally {
			JUnitCore.removeTestRunListener(listener);
		}
		return (TestRunSession) listener.session;
	}

}
