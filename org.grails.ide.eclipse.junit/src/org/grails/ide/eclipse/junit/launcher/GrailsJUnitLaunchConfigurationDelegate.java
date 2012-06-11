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
package org.grails.ide.eclipse.junit.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.launch.ClasspathLocalizer;
import org.grails.ide.eclipse.core.launch.EclipsePluginClasspathEntry;
import org.grails.ide.eclipse.core.launch.GrailsLaunchArgumentUtils;
import org.grails.ide.eclipse.core.model.IGrailsInstall;


/**
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class GrailsJUnitLaunchConfigurationDelegate extends JUnitLaunchConfigurationDelegate {
	
	@Override
	public String verifyMainTypeName(ILaunchConfiguration configuration)
			throws CoreException {
		if (GrailsJUnitLaunchShortcut.SPYING_MODE)
			return "org.grails.ide.eclipse.junit.runner.SpyingRemoteTestRunner";
		else
			return "org.grails.ide.eclipse.junit.runner.GrailsRemoteTestRunner"; //$NON-NLS-1$
	}

	@Override
	public String[] getEnvironment(ILaunchConfiguration configuration)
			throws CoreException {
		String[] _env = super.getEnvironment(configuration);
		if (_env==null) _env = new String[0];
		List<String> env = new ArrayList<String>(Arrays.asList(_env));

		List<String> runtimeClassPath = getGrailsJUnitRuntimeClasspath();
		StringBuffer extraEnv = new StringBuffer("GRAILS_JUNIT_RUNTIME_CLASSPATH=");
		boolean firstOne = true;
		for (String cpe : runtimeClassPath) {
			if (!firstOne)
				extraEnv.append(File.pathSeparator);
			extraEnv.append(cpe);
			firstOne = false;
		}
		env.add(extraEnv.toString());
		env.add("GRAILS_HOME="+GrailsLaunchArgumentUtils.getGrailsHome(configuration));
		env.add("JAVA_HOME="+GrailsLaunchArgumentUtils.getJavaHome(configuration));
		
		return env.toArray(new String[env.size()]);
	}
	
	@Override
	public String[] getClasspath(ILaunchConfiguration configuration)
			throws CoreException {
		String[] cpArr = super.getClasspath(configuration);
		List<String> cp = new ArrayList<String>(Arrays.asList(cpArr));
		
		//Add this plugin to the CP, because it contains the GrailsRemoteTestRunner that is used
		//for running the Grails tests:
		List<String> junitEntries = getGrailsJUnitRuntimeClasspath();
		cp.addAll(junitEntries);
		
		System.err.println(">>> classpath");
		for (String cpe : cp) {
			System.err.println(cpe);
		}
		System.err.println("<<< classpath");
		
		return cp.toArray(new String[cp.size()]);
	}

	protected List<String> getGrailsJUnitRuntimeClasspath() {
		return new ClasspathLocalizer().localizeClasspath(
				new EclipsePluginClasspathEntry("org.grails.ide.eclipse.junit.runtime", null),
// These entries will be added by the superclass: (if Junit4 runner is selected)
// TODO: KDV: (junit) ensure that JUnit4 runner is the one selected somehow (i.e. make changes to UI
//   to remove the option of selecting JUnit3)
				new EclipsePluginClasspathEntry("org.eclipse.jdt.junit.runtime", null),
				new EclipsePluginClasspathEntry("org.eclipse.jdt.junit4.runtime", null)
		);
	}
	
	protected IGrailsInstall getGrailsInstall(IJavaProject javaProject) {
		return GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(javaProject.getProject());
	}

	@Override
	public synchronized void launch(ILaunchConfiguration configuration,
			String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		super.launch(configuration, mode, launch, monitor);
	}
	
}
