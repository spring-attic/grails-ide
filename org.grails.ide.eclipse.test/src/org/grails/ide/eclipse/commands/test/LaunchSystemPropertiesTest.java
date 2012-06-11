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
package org.grails.ide.eclipse.commands.test;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.launch.GrailsLaunchConfigurationDelegate;
import org.grails.ide.eclipse.core.launch.SynchLaunch;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.longrunning.LongRunningProcessGrailsExecutor;


public class LaunchSystemPropertiesTest extends AbstractCommandTest {
	
	String[] testValues = {
			"Not-so-special",
			"Got spaces",
			"Got quotes \"double quotes\"",
			"Got 'single' quotes",
			"Got backslashes \\\\",
			"Got funnies ~!@#$%^&*()_+{{}",
			"Got conrol '\n\t' characters"
	};
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		GrailsCoreActivator.getDefault().setKeepGrailsRunning(false);
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		GrailsCoreActivator.getDefault().setKeepGrailsRunning(false);
		GrailsCoreActivator.getDefault().setUserSupliedLaunchSystemProperties(new HashMap<String, String>());
	}

	/**
	 * A quick test to see whether in GrailsCore, the methods for getting and setting
	 * these properties from/into the preferences work as expected.
	 */
	public void testSetAndGetSystemProps() throws Exception {
		GrailsCoreActivator activator = GrailsCoreActivator.getDefault();
		Map<String, String> props = activator.getUserSupliedLaunchSystemProperties();
		assertEquals(0, props.size());
		
		props.put("my.test.property", "Hallo daar!");
		props.put("my.other.test.property", "World!!@#$%^&*(");
		
		activator.setUserSupliedLaunchSystemProperties(props);
		
		Map<String, String> newProps = activator.getUserSupliedLaunchSystemProperties();
		
		for (String newProp : newProps.keySet()) {
			assertTrue("Extra property: ", props.containsKey(newProp));
			assertEquals("property value wrong"+newProp, props.get(newProp), newProps.get(newProp));
		}
		
		for (String oldProp : props.keySet()) {
			assertTrue("Missing property: ", newProps.containsKey(oldProp));
			assertEquals("property value wrong"+oldProp, props.get(oldProp), newProps.get(oldProp));
		}
	}
	
	/**
	 * A test that runs a command with 'GrailsCommand' class and check to see if it set the "grails.env" property
	 * takes effect.
	 */
	public void testGrailsCommandClassOldStyle() throws CoreException {
		for (String propVal : testValues) {
			System.out.println("value: "+propVal);
			setProperty("grails.env", propVal);
			GrailsCommand cmd = GrailsCommand.forTest("help");
			ILaunchResult result = cmd.synchExec();
			assertContains("Environment set to "+propVal, result.getOutput());
		}
	}

	/**
	 * Does the same as 'testGrailsCommandClassOldStyle' but with {@link LongRunningProcessGrailsExecutor}
	 */
	public void testGrailsCommandLongRunning() throws CoreException {
		GrailsCoreActivator.getDefault().setKeepGrailsRunning(true);
		testGrailsCommandClassOldStyle();
	}
	
	/**
	 * Test that uses the same launcher that the Grails command prompt uses
	 */
	public void testGrailsLaunchConfigurationDelegate() throws Exception {
		IProject project = ensureProject(this.getClass().getSimpleName());
		for (String propVal : testValues) {
			System.out.println("value: "+propVal);
			setProperty("grails.env", propVal);
			ILaunchConfiguration launchConf = GrailsLaunchConfigurationDelegate.getLaunchConfiguration(project,
					"help", false);
			ILaunchResult result = new SynchLaunch(launchConf, 10000, 100000).synchExec();
			assertContains("Environment set to "+propVal, result.getOutput());
		}
	}
	
	//TODO: test-app launcher, run-app launcher
	
	private void setProperty(String key, String value) {
		Map<String, String> props = GrailsCoreActivator.getDefault().getUserSupliedLaunchSystemProperties();
		if (value!=null) {
			props.put(key, value);
		} else {
			props.remove(key);
		}
		GrailsCoreActivator.getDefault().setUserSupliedLaunchSystemProperties(props);
	}

}
