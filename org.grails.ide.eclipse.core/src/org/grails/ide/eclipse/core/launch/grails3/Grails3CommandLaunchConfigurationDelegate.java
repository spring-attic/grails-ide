/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.launch.grails3;

import static org.grails.ide.eclipse.core.launch.LaunchListenerManager.getLaunchListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.launch.AbstractLaunchProcessListener;
import org.grails.ide.eclipse.core.launch.GrailsCommandLaunchConfigurationDelegate;
import org.grails.ide.eclipse.core.launch.GrailsLaunchArgumentUtils;
import org.grails.ide.eclipse.core.launch.GrailsLaunchConfigurationDelegate;
import org.grails.ide.eclipse.core.launch.LaunchListenerManager;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.springframework.util.StringUtils;
import org.springsource.ide.eclipse.commons.core.util.OsUtils;
import org.springsource.ide.eclipse.commons.frameworks.core.ExceptionUtil;

public class Grails3CommandLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

	private static final String JAVA_HOME = "JAVA_HOME";
	private static final String GRAILS_HOME = "GRAILS_HOME";
	private static final String LAUNCH_CONFIG_ID = "org.grails.ide.eclipse.core.launchCommandConfig3";
	
	static {
		LaunchListenerManager.promiseSupportForType(LAUNCH_CONFIG_ID);
	}

	public void launch(ILaunchConfiguration conf, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Launching "+conf.getName(), 1);
		try {
			ProcessBuilder builder = new ProcessBuilder();
			Map<String, String> env = builder.environment();
			env.put(JAVA_HOME, getJavaHome(conf).toString());
			
			String grailsHome = getGrailsHome(conf);
			String grailsCommand = getGrailsExecutable(grailsHome);
			env.put(GRAILS_HOME, grailsHome);
			env.put("GROOVY_PAGE_ADD_LINE_NUMBERS", "true");
			
			builder.directory(getBaseDir(conf));
			setDefaultSourceLocator(launch, conf);
			
			String projectName = conf.getAttribute(
					IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
			IProject project = null;
			if (!"".equals(projectName)) {
				project = ResourcesPlugin.getWorkspace().getRoot()
						.getProject(projectName);
			}

			String script = GrailsLaunchConfigurationDelegate
					.getScript(conf);
			ArrayList<String> commandLine = new ArrayList<String>();
			commandLine.add(grailsCommand);
			commandLine.addAll(Arrays.asList(DebugPlugin.parseArguments(script)));
			builder.command(commandLine);

//TODO: system props from grails preferences
//			Map<String, String> systemProps = GrailsLaunchArgumentUtils
//					.getSystemProperties(conf);
//			if (systemProps != null) {
//				for (Map.Entry<String, String> entry : systemProps.entrySet()) {
//					vmArgs.add("-D" + entry.getKey() + "=" + entry.getValue());
//				}
//			}

			
			try {
				GrailsCoreActivator.getDefault().notifyCommandStart(project);
				Process systemProcess = builder.start();
				AbstractLaunchProcessListener listener = getLaunchListener(conf);
				IProcess process = new RuntimeProcess(launch, systemProcess, conf.getName(), null);
				launch.addProcess(process);
				if (listener != null) {
					listener.init(process);
				}
			} catch (Exception e) {
				GrailsCoreActivator.log(e);
			}
		} finally {
			monitor.done();
		}
	}

	private String getGrailsExecutable(String grailsHome) {
		IPath executablePath = new Path(grailsHome).append("bin/grails");
		if (OsUtils.isWindows()) {
			//TODO: windows case untested... does it work?
			executablePath = executablePath.addFileExtension(".bat");
		} else {
			File exec = executablePath.toFile();
			if (exec.isFile()) {
				if (exec.canExecute()) {
					//ok
				} else {
					//try to repare exec permissions.
					exec.setExecutable(true);
				}
			}
		}
		return executablePath.toOSString();
	}

	File getJavaHome(ILaunchConfiguration conf) throws CoreException {
		IVMInstall vm = verifyVMInstall(conf);
		return vm.getInstallLocation();
	}
	
	String getGrailsHome(ILaunchConfiguration conf) throws CoreException {
		String grailsHome = GrailsLaunchArgumentUtils.getGrailsHome(conf);
		if (grailsHome==null) {
			ExceptionUtil.coreException("Grails Home cannot be deterimed");
		}
		return grailsHome;
	}
	
	File getBaseDir(ILaunchConfiguration conf) throws CoreException {
		String baseDir = conf.getAttribute(GrailsLaunchArgumentUtils.PROJECT_DIR_LAUNCH_ATTR, "");
		if (StringUtils.hasText(baseDir)) {
			return new File(baseDir);
		} else {
			//commands like 'create-app' aren't associated with a project. Execute at
			// workspace root.
			return ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
		}
	}

	public static ILaunchConfigurationWorkingCopy getLaunchConfiguration(
			IGrailsInstall install, IProject project, String script,
			String baseDirectory) throws CoreException {
		return GrailsCommandLaunchConfigurationDelegate.getLaunchConfiguration(
				install, project, script, baseDirectory,
				LAUNCH_CONFIG_ID);
	}

	
}
