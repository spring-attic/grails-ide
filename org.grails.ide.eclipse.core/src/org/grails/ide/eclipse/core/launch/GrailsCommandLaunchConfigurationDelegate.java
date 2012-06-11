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
package org.grails.ide.eclipse.core.launch;

import static org.grails.ide.eclipse.core.launch.LaunchListenerManager.getLaunchListener;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.GrailsBuildSettingsHelper;
import org.grails.ide.eclipse.core.model.IGrailsInstall;


/**
 * LaunchConfigurationDelegate used by the {@link GrailsCommand} class. It
 * duplicates the behaviour of its superclass, except for the fact that it
 * provides support for the LaunchListener infrastructure (see
 * {@link LaunchListenerManager}), and that it doesn't do any automatic
 * post-processing of commands (but post-processing can be added externally, via 
 * {@link LaunchListenerManager})
 * 
 * @author Kris De Volder
 * @since 2.5
 */
public class GrailsCommandLaunchConfigurationDelegate extends
		GrailsLaunchConfigurationDelegate {

	static {
		LaunchListenerManager
				.promiseSupportForType(getLaunchConfigurationTypeId());
	}

	public static boolean DEBUG = false;

	/**
	 * This method is a slightly modified copy of the one in the superclass.
	 * Changes relate to LaunchListenerManager support.
	 */
	@SuppressWarnings("unchecked")
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {

		IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 5);
		checkCancelled(subMonitor);
		subMonitor.beginTask("Starting Grails", 5);
		subMonitor.worked(1);
		checkCancelled(subMonitor);
		subMonitor.subTask("Configuring launch parameters...");

		// FIXKDV FIXADE Copies of this code exist in 
		// GrailsLaunchArgumentUtils.prepareClasspath()
        // and GrailsCommandLaunchConfigurationDelegate.launch()
        // consider refactoring to combine

		IVMInstall vm = verifyVMInstall(configuration);
		IVMRunner runner = vm.getVMRunner(mode);
		if (runner == null) {
			runner = vm.getVMRunner(ILaunchManager.RUN_MODE);
		}

		String projectName = configuration.getAttribute(
				IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
		IProject project = null;
		if (!"".equals(projectName)) {
			project = ResourcesPlugin.getWorkspace().getRoot()
					.getProject(projectName);
		}

		String grailsHome = GrailsLaunchArgumentUtils.getGrailsHome(configuration);
		configuration.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, new HashMap<String, String>()).put(
				"JAVA_HOME", vm.getInstallLocation().getAbsolutePath());
		configuration.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, new HashMap<String, String>()).put(
				"GRAILS_HOME", grailsHome);
		// ensure that extra line number information is added to GSPs.
		configuration.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, new HashMap<String, String>()).put(
		        "GROOVY_PAGE_ADD_LINE_NUMBERS", "true");
		String baseDir = configuration.getAttribute(
				GrailsLaunchArgumentUtils.PROJECT_DIR_LAUNCH_ATTR, "");
		
		if (baseDir.equals("")) {
			baseDir = ResourcesPlugin.getWorkspace().getRoot().getLocation()
					.toString();
		}
		String script = GrailsLaunchConfigurationDelegate.getScript(configuration);
		configuration.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES,
				new HashMap<String, String>()).put("GRAILS_HOME", grailsHome);

		File workingDir = verifyWorkingDirectory(configuration);
		String workingDirName = null;
		if (workingDir != null) {
			workingDirName = workingDir.getAbsolutePath();
		} else {
			workingDirName = baseDir;
		}

		List<String> programArguments = new ArrayList<String>();
		
		String buildListener = GrailsLaunchArgumentUtils.getGrailsBuildListener(configuration);
		List<String> buildListenerClassPathList = GrailsLaunchArgumentUtils.getBuildListenerClassPath(configuration);
		if (buildListenerClassPathList!=null && !buildListenerClassPathList.isEmpty()) {
			//Note: It is important that buildListenerClassPath entries are added by using --classpath argument to
			//the GrailsStarter. If the entries are added to the boot class path or user classpath in the configuration
			//itself, it will end up resolving the GrailsBuildListener interface to one loaded by a different classloader
			//than the one resolved inside of grails itself. This will make grails complain that our class does not implement
			//that interface (because the interfaces loaded by different classloaders are not considered equal).
			programArguments.add("--classpath");
			programArguments.add(GrailsLaunchArgumentUtils.toPathsString(buildListenerClassPathList));
		}
		
		programArguments.add("--conf");
		programArguments.add(grailsHome + "conf" + File.separatorChar
				+ "groovy-starter.conf");
		programArguments.add("--main");
		programArguments
				.add("org.codehaus.groovy.grails.cli.GrailsScriptRunner");
		
		StringBuilder grailsCommand = new StringBuilder();
		String grailsWorkDir = configuration.getAttribute(
		GrailsLaunchArgumentUtils.GRAILS_WORK_DIR_LAUNCH_ATTR, "");
		if (!grailsWorkDir.equals("")) {
			grailsCommand.append("-Dgrails.work.dir=" + grailsWorkDir+" ");
		}
		if (buildListener!=null) {
			grailsCommand.append("-Dgrails.build.listeners="+buildListener+" ");
		}
		grailsCommand.append(script+" ");
		programArguments.add(grailsCommand.toString().trim());
		
		List<String> vmArgs = new ArrayList<String>();
		
		if (DEBUG) {
			vmArgs.add("-agentlib:jdwp=transport=dt_socket,server=y,address=8123");
		}

		// add manual configured vm options to the argument list
		String existingVmArgs = getVMArguments(configuration);
		if (existingVmArgs != null  && existingVmArgs.length() > 0) {
			StringTokenizer additionalArguments = new StringTokenizer(
					existingVmArgs, " ");
			while (additionalArguments.hasMoreTokens()) {
				vmArgs.add(additionalArguments.nextToken());
			}
		}
		vmArgs.add("-Dbase.dir=" + baseDir);
		vmArgs.add("-Dgrails.home=" + grailsHome);
		
		Map<String,String> systemProps = GrailsLaunchArgumentUtils.getSystemProperties(configuration);
		if (systemProps!=null) {
			for (Map.Entry<String, String> entry : systemProps.entrySet()) {
				vmArgs.add("-D"+entry.getKey()+"="+entry.getValue());
			}
		}

		// Grails uses some default memory settings that we want to use as well
		// if on others have been configured
		vmArgs = GrailsLaunchArgumentUtils.addMemorySettings(vmArgs);

		String[] envp = getEnvironment(configuration);

		Map<String, Object> vmAttributesMap = getVMSpecificAttributesMap(configuration);

		String[] classpath = getClasspath(configuration);
		String mainTypeName = verifyMainTypeName(configuration);

		VMRunnerConfiguration runConfiguration = new VMRunnerConfiguration(
				mainTypeName, classpath);
		runConfiguration.setProgramArguments(programArguments
				.toArray(new String[programArguments.size()]));
		runConfiguration.setVMArguments(vmArgs
				.toArray(new String[vmArgs.size()]));
		runConfiguration.setWorkingDirectory(workingDirName);
		runConfiguration.setEnvironment(envp);
		runConfiguration.setVMSpecificAttributesMap(vmAttributesMap);

		String[] bootpath = getBootpath(configuration);
		if (bootpath != null && bootpath.length > 0) {
			runConfiguration.setBootClassPath(bootpath);
		}
		
		subMonitor.worked(1);
		checkCancelled(subMonitor);

		subMonitor.subTask("Setting up source locator...");
		setDefaultSourceLocator(launch, configuration);
		subMonitor.worked(1);
		checkCancelled(subMonitor);

		subMonitor.worked(1);
		checkCancelled(subMonitor);

		subMonitor.subTask("Launching Grails...");

		try {
			GrailsCoreActivator.getDefault().notifyCommandStart(project);
			runner.run(runConfiguration, launch, monitor);
			AbstractLaunchProcessListener listener = getLaunchListener(configuration);
			if (listener != null) {
				listener.init(launch.getProcesses()[0]);
			}
			subMonitor.worked(1);
		} catch (Exception e) {
			GrailsCoreActivator.log(e);
		}
		
		/*
		 * When I run grails 2.0.2. on the command line it use this to launch create app command:
		 * 
		 * exec /usr/lib/jvm/java-6-sun//bin/java 
		 * -server -Xmx768M -Xms768M -XX:PermSize=256m -XX:MaxPermSize=256m -Dfile.encoding=UTF-8 
		 * -classpath /home/kdvolder/Applications/grails-distros/grails-2.0.2.BUILD-SNAPSHOT/lib/org.codehaus.groovy/groovy-all/1.8.6/jar/groovy-all-1.8.6.jar:/home/kdvolder/Applications/grails-distros/grails-2.0.2.BUILD-SNAPSHOT/dist/grails-bootstrap-2.0.2.BUILD-SNAPSHOT.jar 
		 * -Dgrails.home=/home/kdvolder/Applications/grails-distros/grails-2.0.2.BUILD-SNAPSHOT 
		 * -Dtools.jar=/usr/lib/jvm/java-6-sun//lib/tools.jar org.codehaus.groovy.grails.cli.support.GrailsStarter 
		 * --main org.codehaus.groovy.grails.cli.GrailsScriptRunner 
		 * --conf /home/kdvolder/Applications/grails-distros/grails-2.0.2.BUILD-SNAPSHOT/conf/groovy-starter.conf --classpath   
		 * create-app bork
		 */
	}

	public static ILaunchConfigurationWorkingCopy getLaunchConfiguration(IGrailsInstall install, IProject project, String script, String baseDirectory) throws CoreException {
		ILaunchConfigurationType configType = DebugPlugin.getDefault()
				.getLaunchManager()
				.getLaunchConfigurationType(getLaunchConfigurationTypeId());
		if (install==null) {
			install = GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(project);
			if (install == null) {
				return null;
			}
		}
		if (baseDirectory==null) {
			if (project!=null) {
				baseDirectory = GrailsBuildSettingsHelper.getBaseDir(project);
			}
			//If not set, launch will use workspace as baseDir
		}
		String nameAndScript = (script != null ? "(" + script + ")" : "");
		if (project != null) {
			nameAndScript = project.getName() + " " + nameAndScript;
		}
		nameAndScript = sanitize(nameAndScript);
		ILaunchConfigurationWorkingCopy wc = configType.newInstance(null,
				nameAndScript);
		GrailsLaunchArgumentUtils.prepareLaunchConfiguration(project, script,
				install, baseDirectory, wc);
		return wc;
	}

	private static String getLaunchConfigurationTypeId() {
		return "org.grails.ide.eclipse.core.launchCommandConfig";
	}
	
	@Override
	public boolean finalLaunchCheck(ILaunchConfiguration configuration,
			String mode, IProgressMonitor monitor) throws CoreException {
		// We don't do the one from super (which checks for build errors). Grails command does its own  compiling and checking if it needs to.
		return true;
	}

}
