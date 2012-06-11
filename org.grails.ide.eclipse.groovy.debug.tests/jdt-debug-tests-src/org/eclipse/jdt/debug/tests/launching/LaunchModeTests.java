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
package org.eclipse.jdt.debug.tests.launching;


import java.util.Arrays;
import java.util.HashSet;

import junit.framework.AssertionFailedError;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationPresentationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsDialog;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.testplugin.launching.TestModeLaunchDelegate;
import org.eclipse.jdt.debug.tests.AbstractDebugTest;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaMainTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaUI;

/**
 * LaunchModeTests
 * @author Andrew Eisenberg
 */
public class LaunchModeTests extends AbstractDebugTest {
	
	private ILaunchConfiguration fConfiguration;
	private String fMode;

	/**
	 * @param name
	 */
	public LaunchModeTests(String name) {
		super(name);
	}

	/**
	 * Called by launch "TestModeLaunchDelegate" delegate when launch method invoked.
	 * 
	 * @param configuration
	 * @param mode
	 */
	public synchronized void launch(ILaunchConfiguration configuration, String mode) {
		fConfiguration = configuration;
		fMode = mode;
		notifyAll();
	}

	/**
	 * Tests that launch delegate for "TEST_MODE" and Java applications is invoked when
	 * "TEST_MODE" is used.
	 * 
	 * @throws CoreException
	 * @see TestModeLaunchDelegate
	 */
	public void testContributedLaunchMode() throws CoreException {
		ILaunch launch = null;
		try {
			fConfiguration = null;
			fMode = null;
			TestModeLaunchDelegate.setTestCase(this);
			ILaunchConfiguration configuration = getLaunchConfiguration("Breakpoints"); //$NON-NLS-1$
			assertNotNull(configuration);
			launch = configuration.launch("TEST_MODE", null); //$NON-NLS-1$
			assertEquals("Launch delegate not invoked", configuration, getLaunchConfigurationSetByDelegate()); //$NON-NLS-1$
			assertEquals("Launch delegate not invoked in correct mode", "TEST_MODE", fMode); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			TestModeLaunchDelegate.setTestCase(null);
			fConfiguration = null;
			fMode = null;
			if (launch != null) {
				getLaunchManager().removeLaunch(launch);
			}
		}
	}
	
	/**
	 * Returns the launch configuration set by the TestModeLaunchDelegate, or <code>null</code>
	 * if no launch configuration was set (the correct delegate was not called).
	 * A wait must occur due to the use of Jobs for launching.
	 */
	private synchronized Object getLaunchConfigurationSetByDelegate() {
		if (fConfiguration == null) {
			try {
				wait(10000);
			} catch (InterruptedException ie) {
				System.err.println("Interrupted waiting for launch configuration"); //$NON-NLS-1$
			}
		}
		
		return fConfiguration;
	}
	
	/**
	 * Ensure our contributed launch mode exists.
	 */
	public void testLaunchModes() {
		ILaunchMode[] modes = getLaunchManager().getLaunchModes();
		String[] ids = new String[modes.length];
		for (int i = 0; i < modes.length; i++) {
			ILaunchMode mode = modes[i];
			ids[i] = mode.getIdentifier();
		}
		assertContains("Missing TEST_MODE", ids, "TEST_MODE"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains("Missing debug mode", ids, ILaunchManager.DEBUG_MODE); //$NON-NLS-1$
		assertContains("Missing run mode", ids, ILaunchManager.RUN_MODE); //$NON-NLS-1$
		assertContains("Missing TEST_MODE", ids, "alternate"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Asserts that the array contains the given object
	 * @param message
	 * @param array
	 * @param object
	 */
	static public void assertContains(String message, Object[] array, Object object) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(object)) {
				return;
			}			
		}
		throw new AssertionFailedError(message);
	}	

	/**
	 * Ensure our contributed mode is supported.
	 * @throws CoreException
	 */
	public void testSupportsMode() throws CoreException {
		ILaunchConfiguration configuration = getLaunchConfiguration("Breakpoints"); //$NON-NLS-1$
		assertNotNull(configuration);
		assertTrue("Java application configuration should support TEST_MODE", configuration.supportsMode("TEST_MODE")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Java application type should support TEST_MODE", configuration.getType().supportsMode("TEST_MODE")); //$NON-NLS-1$ //$NON-NLS-2$
		
		assertTrue("Java application configuration should support debug mode", configuration.supportsMode(ILaunchManager.DEBUG_MODE)); //$NON-NLS-1$
		assertTrue("Java application type should support debug mode", configuration.getType().supportsMode(ILaunchManager.DEBUG_MODE)); //$NON-NLS-1$
		
		assertTrue("Java application configuration should support run mode", configuration.supportsMode(ILaunchManager.RUN_MODE)); //$NON-NLS-1$
		assertTrue("Java application type should support run mode", configuration.getType().supportsMode(ILaunchManager.RUN_MODE));		 //$NON-NLS-1$
	}
	
	/**
	 * Tests that mode specific tab group contributions work.
	 * @throws CoreException
	 */
	public void testModeSpecificTabGroups() throws CoreException {
		ILaunchConfigurationType javaType = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION); 
		ILaunchConfigurationTabGroup standardGroup = LaunchConfigurationPresentationManager.getDefault().getTabGroup(javaType, ILaunchManager.DEBUG_MODE);
		ILaunchConfigurationTabGroup testGroup = LaunchConfigurationPresentationManager.getDefault().getTabGroup(javaType, "TEST_MODE"); //$NON-NLS-1$
		ILaunchConfigurationDialog dialog = new LaunchConfigurationsDialog(null, DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP));
		standardGroup.createTabs(dialog, ILaunchManager.DEBUG_MODE);
		testGroup.createTabs(dialog, "TEST_MODE"); //$NON-NLS-1$
		
		ILaunchConfigurationTab[] tabs = standardGroup.getTabs();
		HashSet tabset = new HashSet();
		for(int i = 0; i< tabs.length; i++) {
			tabset.add(tabs[i].getClass());
		}
		Class[] classes = new Class[] {JavaMainTab.class, JavaArgumentsTab.class, JavaJRETab.class, JavaClasspathTab.class,
				SourceLookupTab.class, EnvironmentTab.class, CommonTab.class};
		assertTrue("Tab set does not contain all default java tabs", tabset.containsAll(new HashSet(Arrays.asList(classes)))); //$NON-NLS-1$
		
		tabs = testGroup.getTabs();
		assertEquals("Wrong number of tabs in the test group", 4, tabs.length); //$NON-NLS-1$
		tabset = new HashSet();
		for(int i = 0; i< tabs.length; i++) {
			tabset.add(tabs[i].getClass());
		}
		classes = new Class[] {JavaMainTab.class, JavaArgumentsTab.class, JavaJRETab.class, JavaClasspathTab.class};
		assertTrue("Test tab set does not contain all default tabs", tabset.containsAll(new HashSet(Arrays.asList(classes)))); //$NON-NLS-1$
		standardGroup.dispose();
		testGroup.dispose();
	}
	
	/**
	 * Tests that the default debug perspective for java applications is debug.
	 */
	public void testDefaultDebugLaunchPerspective() {
		ILaunchConfigurationType javaType = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		assertEquals("Java debug perspective should be debug", IDebugUIConstants.ID_DEBUG_PERSPECTIVE, DebugUITools.getLaunchPerspective(javaType, ILaunchManager.DEBUG_MODE)); //$NON-NLS-1$
	}
	
	/**
	 * Test that the default debug perspective for Java Application types is debug.
	 * Same notion as <code>testDefaultDebugLaunchPerspective()</code>, but using the new API
	 * for getting perspectives
	 * 
	 * @since 3.3
	 */
	public void testDefaultDebugLaunchPerspective2() {
		ILaunchConfigurationType javaType = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		HashSet modes =  new HashSet();
		modes.add(ILaunchManager.DEBUG_MODE);
		assertEquals("Java debug perspective should be debug", IDebugUIConstants.ID_DEBUG_PERSPECTIVE, DebugUITools.getLaunchPerspective(javaType, null, modes)); //$NON-NLS-1$
	}
	
	/**
	 * Tests that the default run perspective for java applications is none (<code>null</code>).
	 */
	public void testDefaultRunLaunchPerspective() {
		ILaunchConfigurationType javaType = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		assertNull("Java run perspective should be null", DebugUITools.getLaunchPerspective(javaType, ILaunchManager.RUN_MODE)); //$NON-NLS-1$
	}	
	
	/**
	 * Tests that the default run perspective for java applications is none (<code>null</code>).
	 * Same notion as <code>testDefaultRunLaunchPerspective()</code>, but using the new API for getting
	 * launch perspectives
	 * 
	 * @since 3.3
	 */
	public void testDefaultRunLaunchPerspective2() {
		ILaunchConfigurationType javaType = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		HashSet modes = new HashSet();
		modes.add(ILaunchManager.RUN_MODE);
		assertNull("Java run perspective should be null", DebugUITools.getLaunchPerspective(javaType, null, modes)); //$NON-NLS-1$
	}
	
	/**
	 * Tests that the default debug perspective can be over-ridden and reset
	 */
	public void testResetDebugLaunchPerspective() {
		ILaunchConfigurationType javaType = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		assertEquals("Java debug perspective should be debug", IDebugUIConstants.ID_DEBUG_PERSPECTIVE, //$NON-NLS-1$
			DebugUITools.getLaunchPerspective(javaType, ILaunchManager.DEBUG_MODE));
		// set to NONE
		DebugUITools.setLaunchPerspective(javaType, ILaunchManager.DEBUG_MODE, IDebugUIConstants.PERSPECTIVE_NONE);
		assertNull("Java debug perspective should now be null",  //$NON-NLS-1$
			DebugUITools.getLaunchPerspective(javaType, ILaunchManager.DEBUG_MODE));
		// re-set to default
		DebugUITools.setLaunchPerspective(javaType, ILaunchManager.DEBUG_MODE, IDebugUIConstants.PERSPECTIVE_DEFAULT);
		assertEquals("Java debug perspective should now be debug", IDebugUIConstants.ID_DEBUG_PERSPECTIVE, //$NON-NLS-1$
			DebugUITools.getLaunchPerspective(javaType, ILaunchManager.DEBUG_MODE));
				
	}	
	
	/**
	 * Tests that the default debug perspective can be over-ridden and reset.
	 * Same notion as <code>testResetDebugLaunchPerspective()</code>, but using the new API
	 * for setting an resetting perspectives.
	 * 
	 * @since 3.3
	 */
	public void testResetDebugLaunchPerspective2() {
		ILaunchConfigurationType javaType = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		HashSet modes = new HashSet();
		modes.add(ILaunchManager.DEBUG_MODE);
		assertEquals("Java debug perspective should be debug", IDebugUIConstants.ID_DEBUG_PERSPECTIVE, DebugUITools.getLaunchPerspective(javaType, null, modes)); //$NON-NLS-1$
		// set to NONE
		DebugUITools.setLaunchPerspective(javaType, null, modes, IDebugUIConstants.PERSPECTIVE_NONE);
		assertNull("Java debug perspective should now be null", DebugUITools.getLaunchPerspective(javaType, null, modes)); //$NON-NLS-1$
		// re-set to default
		DebugUITools.setLaunchPerspective(javaType, null, modes, IDebugUIConstants.PERSPECTIVE_DEFAULT);
		assertEquals("Java debug perspective should now be debug", IDebugUIConstants.ID_DEBUG_PERSPECTIVE, DebugUITools.getLaunchPerspective(javaType, null, modes)); //$NON-NLS-1$
				
	}	
	
	/**
	 * Tests that the default debug perspective can be over-ridden and reset
	 * Same notion as <code>testResetDebugLaunchPerspective2()</code>, but using the 
	 * JDT Java launch delegate instead of <code>null</code>
	 * 
	 * @since 3.3
	 */
	public void testResetDebugPerspectiveJavaLaunchDelegate() {
		ILaunchConfigurationType javaType = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		HashSet modes = new HashSet();
		modes.add(ILaunchManager.DEBUG_MODE);
		ILaunchDelegate delegate = ((LaunchManager)getLaunchManager()).getLaunchDelegate("org.eclipse.jdt.launching.localJavaApplication"); //$NON-NLS-1$
		assertNotNull("Java launch delegate should not be null", delegate); //$NON-NLS-1$
		assertEquals("Java debug perspective should be debug", IDebugUIConstants.ID_DEBUG_PERSPECTIVE, DebugUITools.getLaunchPerspective(javaType, delegate, modes)); //$NON-NLS-1$
		// set to NONE
		DebugUITools.setLaunchPerspective(javaType, null, modes, IDebugUIConstants.PERSPECTIVE_NONE);
		assertNull("Java debug perspective should now be null", DebugUITools.getLaunchPerspective(javaType, delegate, modes)); //$NON-NLS-1$
		// re-set to default
		DebugUITools.setLaunchPerspective(javaType, null, modes, IDebugUIConstants.PERSPECTIVE_DEFAULT);
		assertEquals("Java debug perspective should now be debug", IDebugUIConstants.ID_DEBUG_PERSPECTIVE, DebugUITools.getLaunchPerspective(javaType, delegate, modes)); //$NON-NLS-1$
	}
	
	/**
	 * Tests that the default run perspective can be over-ridden and reset
	 */
	public void testResetRunLaunchPerspective() {
		ILaunchConfigurationType javaType = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		assertNull("Java run perspective should be null",  //$NON-NLS-1$
			DebugUITools.getLaunchPerspective(javaType, ILaunchManager.RUN_MODE));
		// set to Java perspective
		DebugUITools.setLaunchPerspective(javaType, ILaunchManager.RUN_MODE, JavaUI.ID_PERSPECTIVE);
		assertEquals("Java run perspective should now be java", JavaUI.ID_PERSPECTIVE, //$NON-NLS-1$
					DebugUITools.getLaunchPerspective(javaType, ILaunchManager.RUN_MODE));
		// re-set to default
		DebugUITools.setLaunchPerspective(javaType, ILaunchManager.RUN_MODE, IDebugUIConstants.PERSPECTIVE_DEFAULT);
		assertNull("Java run perspective should now be null",  //$NON-NLS-1$
			DebugUITools.getLaunchPerspective(javaType, ILaunchManager.RUN_MODE));		
	}	
	
	/**
	 * Tests that the default run perspective can be over-ridden and reset.
	 * Same notion as <code>testResetRunLaunchPerspective()</code>, but using the new API
	 * for getting/setting/re-setting perspective settings.
	 * 
	 * @since 3.3
	 */
	public void testResetRunLaunchPerspective2() {
		ILaunchConfigurationType javaType = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		HashSet modes = new HashSet();
		modes.add(ILaunchManager.RUN_MODE);
		assertNull("Java run perspective should be null", DebugUITools.getLaunchPerspective(javaType, null, modes)); //$NON-NLS-1$
		// set to Java perspective
		DebugUITools.setLaunchPerspective(javaType, null, modes, JavaUI.ID_PERSPECTIVE);
		assertEquals("Java run perspective should now be java", JavaUI.ID_PERSPECTIVE, DebugUITools.getLaunchPerspective(javaType, null, modes)); //$NON-NLS-1$
		// re-set to default
		DebugUITools.setLaunchPerspective(javaType, ILaunchManager.RUN_MODE, IDebugUIConstants.PERSPECTIVE_DEFAULT);
		assertNull("Java run perspective should now be null", DebugUITools.getLaunchPerspective(javaType, null, modes));		 //$NON-NLS-1$
	}	
	
	/**
	 * Tests that the default debug perspective can be over-ridden and reset
	 * Same notion as <code>testResetRunLaunchPerspective2()</code>, but using the 
	 * JDT Java launch delegate instead of <code>null</code>
	 * 
	 * @since 3.3
	 */
	public void testResetRunPerspectiveJavaLaunchDelegate() {
		ILaunchConfigurationType javaType = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		HashSet modes = new HashSet();
		modes.add(ILaunchManager.RUN_MODE);
		ILaunchDelegate delegate = ((LaunchManager)getLaunchManager()).getLaunchDelegate("org.eclipse.jdt.launching.localJavaApplication"); //$NON-NLS-1$
		assertNotNull("Java launch delegate should not be null", delegate); //$NON-NLS-1$
		assertNull("Java run perspective should be null", DebugUITools.getLaunchPerspective(javaType, delegate, modes)); //$NON-NLS-1$
		// set to NONE
		DebugUITools.setLaunchPerspective(javaType, null, modes, IDebugUIConstants.PERSPECTIVE_NONE);
		assertNull("Java run perspective should now be null", DebugUITools.getLaunchPerspective(javaType, delegate, modes)); //$NON-NLS-1$
		// re-set to default
		DebugUITools.setLaunchPerspective(javaType, null, modes, IDebugUIConstants.PERSPECTIVE_DEFAULT);
		assertNull("Java run perspective should now be null", DebugUITools.getLaunchPerspective(javaType, delegate, modes)); //$NON-NLS-1$
	}
	
	/**
	 * Tests that the default launch perspective for the 'debug' modeset and java launch delegate is 
	 * <code>ILaunchManager.DEBUG_MODE</code> using the new API.
	 * 
	 * @since 3.3
	 */
	public void testDefaultDebugPerspectiveJavaLaunchDelegate() {
		ILaunchConfigurationType javaType = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		HashSet modes = new HashSet();
		modes.add(ILaunchManager.DEBUG_MODE);
		ILaunchDelegate delegate = ((LaunchManager)getLaunchManager()).getLaunchDelegate("org.eclipse.jdt.launching.localJavaApplication"); //$NON-NLS-1$
		assertNotNull("Java launch delegate should not be null", delegate); //$NON-NLS-1$
		String p = DebugUITools.getLaunchPerspective(javaType, delegate, modes);
		assertEquals("Java debug perspective should be debug", IDebugUIConstants.ID_DEBUG_PERSPECTIVE, p); //$NON-NLS-1$
	}
	
	/**
	 * Tests that the default launch perspective for the 'run' modeset and java launch delegate
	 * is <code>null/code> using the new API
	 * 
	 * @since 3.3
	 */
	public void testDefaultRunPerspectiveJavaLaunchDelegate() {
		ILaunchConfigurationType javaType = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		HashSet modes = new HashSet();
		modes.add(ILaunchManager.RUN_MODE);
		ILaunchDelegate delegate = ((LaunchManager)getLaunchManager()).getLaunchDelegate("org.eclipse.jdt.launching.localJavaApplication"); //$NON-NLS-1$
		assertNotNull("Java launch delegate should not be null", delegate); //$NON-NLS-1$
		String p = DebugUITools.getLaunchPerspective(javaType, delegate, modes);
		assertNull("Java run perspective should be null", p); //$NON-NLS-1$
	}
	
	/**
	 * Tests a perspective contributed with a launch tab group.
	 */
	public void testContributedLaunchPerspective() {
		ILaunchConfigurationType javaType = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		assertEquals("perspective for TEST_MODE should be 'java'", JavaUI.ID_PERSPECTIVE, //$NON-NLS-1$
			DebugUITools.getLaunchPerspective(javaType, "TEST_MODE")); //$NON-NLS-1$
	}
}
