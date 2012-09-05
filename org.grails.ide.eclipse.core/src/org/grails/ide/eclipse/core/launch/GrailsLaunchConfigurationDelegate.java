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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.GrailsBuildSettingsHelper;
import org.grails.ide.eclipse.core.model.IGrailsInstall;


/**
 * @author Christian Dupuis
 * @since 2.2.0
 */
public class GrailsLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate { 

	private static final String SCRIPT_ATTR = GrailsCoreActivator.PLUGIN_ID + ".SCRIPT";
	private static final String ORG_SCRIPT_ATTR = GrailsCoreActivator.PLUGIN_ID + ".ORG_SCRIPT";

	static {
		LaunchListenerManager
				.promiseSupportForType(getLaunchConfigurationTypeId());
	}
	
	@Override
	public boolean preLaunchCheck(ILaunchConfiguration conf,
			String mode, IProgressMonitor monitor) throws CoreException {
		IGrailsInstall install = GrailsLaunchArgumentUtils.getGrailsInstall(conf);
		IStatus status = install.verify();
		if (!status.isOK()) {
			throw new CoreException(status);
		}
		return super.preLaunchCheck(conf, mode, monitor);
	}
	
	@Override
	public IVMInstall verifyVMInstall(ILaunchConfiguration conf) throws CoreException {
		IVMInstall javaInstall = super.verifyVMInstall(conf);
		IGrailsInstall grailsInstall = GrailsLaunchArgumentUtils.getGrailsInstall(conf);
		grailsInstall.verifyJavaInstall(javaInstall);
		return javaInstall;
	}
	
	@SuppressWarnings("unchecked")
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {

		IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 5);
		checkCancelled(subMonitor);
		subMonitor.beginTask("Starting Grails", 5);
		subMonitor.worked(1);
		checkCancelled(subMonitor);
		subMonitor.subTask("Configuring launch parameters...");

		// FIXKDV FIXADE Copies of this code exist in 
        // GrailsLaunchArgumentUtils.prepareClasspath()
        // and GrailsLaunchConfigurationDelegate.launch()
        // consider refactoring to combine

		IVMInstall vm = verifyVMInstall(configuration);
		IVMRunner runner = vm.getVMRunner(mode);
		if (runner == null) {
			runner = vm.getVMRunner(ILaunchManager.RUN_MODE);
		}

		String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
		IProject project = null;
		if (!"".equals(projectName)) {
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		}

		String grailsHome = GrailsLaunchArgumentUtils.getGrailsHome(configuration);
		String baseDir = configuration.getAttribute(GrailsLaunchArgumentUtils.PROJECT_DIR_LAUNCH_ATTR, "");
		if (baseDir.equals("")) {
			baseDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
		}
		String script = getScript(configuration);

		File workingDir = verifyWorkingDirectory(configuration);
		String workingDirName = null;
		if (workingDir != null) {
			workingDirName = workingDir.getAbsolutePath();
		}
		else {
			workingDirName = baseDir;
		}

		List<String> programArguments = new ArrayList<String>();
		programArguments.add("--conf");
		programArguments.add(grailsHome + "conf" + File.separatorChar + "groovy-starter.conf");
		programArguments.add("--main");
		programArguments.add("org.codehaus.groovy.grails.cli.GrailsScriptRunner");
		
		StringBuilder grailsCommand = new StringBuilder();
		String grailsWorkDir = configuration.getAttribute(
		GrailsLaunchArgumentUtils.GRAILS_WORK_DIR_LAUNCH_ATTR, "");
		if (!grailsWorkDir.equals("")) {
			grailsCommand.append("-Dgrails.work.dir=" + grailsWorkDir+" ");
		}
		grailsCommand.append(script+" ");
		programArguments.add(grailsCommand.toString().trim());
		
		List<String> vmArgs = new ArrayList<String>();
		//vmArgs.add("-agentlib:jdwp=transport=dt_socket,server=y,address=8123");


		// add manual configured vm options to the argument list
		String existingVmArgs = getVMArguments(configuration);
		if (existingVmArgs != null  && existingVmArgs.length() > 0) {
			StringTokenizer additionalArguments = new StringTokenizer(existingVmArgs, " ");
			while (additionalArguments.hasMoreTokens()) {
				vmArgs.add(additionalArguments.nextToken());
			}
		}
		
		Map<String, String> systemProps = GrailsCoreActivator.getDefault().getLaunchSystemProperties();
		GrailsLaunchArgumentUtils.setMaybe(systemProps, "base.dir", baseDir);
		GrailsLaunchArgumentUtils.setMaybe(systemProps, "grails.home", grailsHome);
		for (Map.Entry<String, String> prop : systemProps.entrySet()) {
			vmArgs.add("-D"+prop.getKey()+"="+prop.getValue());
		}

		// Grails uses some default memory settings that we want to use as well if no others have been configured
		vmArgs = GrailsLaunchArgumentUtils.addMemorySettings(vmArgs);
		vmArgs = GrailsLaunchArgumentUtils.addSpringLoadedArgs(configuration, vmArgs);

		String[] envp = getEnvironment(configuration);
		Map<String, String> extra = new HashMap<String, String>();
		extra.put("JAVA_HOME", vm.getInstallLocation().getAbsolutePath());
		extra.put("GROOVY_PAGE_ADD_LINE_NUMBERS", "true"); // Enables line number info for GSP debugging
		envp = GrailsLaunchArgumentUtils.addToEnvMaybe(envp, extra);

		Map<String, Object> vmAttributesMap = getVMSpecificAttributesMap(configuration);

		String[] classpath = getClasspath(configuration);
		String mainTypeName = verifyMainTypeName(configuration);

		VMRunnerConfiguration runConfiguration = new VMRunnerConfiguration(mainTypeName, classpath);
		runConfiguration.setProgramArguments(programArguments.toArray(new String[programArguments.size()]));
		runConfiguration.setVMArguments(vmArgs.toArray(new String[vmArgs.size()]));
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
			DebugPlugin.getDefault().addDebugEventListener(new GrailsProcessListener(launch.getProcesses()[0], project));
			subMonitor.worked(1);
		}
		catch (Exception e) {
			GrailsCoreActivator.log(e);
		}
	}

	protected void checkCancelled(IProgressMonitor monitor) throws CoreException {
		if (monitor.isCanceled()) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
	}

	public static String getScript(ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(
				SCRIPT_ATTR, "run-app");
	}
	
	/**
	 * Gets the 'original' value of the script attribute. Note that this can't be set directly but it is set
	 * as a side effect of setting the script attribute.
	 */
	public static String getOrgScript(ILaunchConfiguration configuration) throws CoreException {
		String value = configuration.getAttribute(ORG_SCRIPT_ATTR, (String)null);
		if (value==null) {
			//For legacy configurations that don't have ORG_SCRIPT_ATTR just use the regular script attribute
			value = getScript(configuration);
		}
		return value;
	}

	public static void setScript(ILaunchConfigurationWorkingCopy wc,
			String script) {
		wc.setAttribute(SCRIPT_ATTR, script);
		try {
			String orgScript = wc.getAttribute(ORG_SCRIPT_ATTR, (String)null);
			if (orgScript==null) {
				//org sript should be set same value as script, but only the first time that script value is set.
				wc.setAttribute(ORG_SCRIPT_ATTR, script);
			}
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
		}
	}
	
	private class GrailsProcessListener implements IDebugEventSetListener {

		private final IProject project;

		private final IProcess newProcess;

		public GrailsProcessListener(IProcess process, IProject project) {
			this.project = project;
			this.newProcess = process;
		}

		public void handleDebugEvents(DebugEvent[] events) {
			if (events != null && project != null) {
				int size = events.length;
				for (int i = 0; i < size; i++) {
					if (newProcess != null && newProcess.equals(events[i].getSource())
							&& events[i].getKind() == DebugEvent.TERMINATE) {

						DebugPlugin.getDefault().removeDebugEventListener(this);

						Job job = new Job("refresh project") {

							@Override
							protected IStatus run(IProgressMonitor monitor) {
								try {
									project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
								}
								catch (CoreException e) {
								}
								GrailsCoreActivator.getDefault().notifyCommandFinish(project);
								return Status.OK_STATUS;
							}

						};
						job.setSystem(true);
						job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
						job.setPriority(Job.INTERACTIVE);
						job.schedule();
					}
				}
			}
		}
	}

	public static ILaunchConfiguration getLaunchConfiguration(IProject project, String script, boolean persist) throws CoreException {

		ILaunchConfigurationType configType = getLaunchConfigurationType();
		IGrailsInstall install = GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(project);
		if (install == null) {
			return null;
		}
		String nameAndScript = (script != null ? "(" + script + ")" : "");
		if (project!=null) {
			nameAndScript = project.getName()+" "+nameAndScript;
		}
		nameAndScript = sanitize(nameAndScript);
		ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, nameAndScript);
		GrailsLaunchArgumentUtils.prepareLaunchConfiguration(project, script, install, GrailsBuildSettingsHelper.getBaseDir(project), wc);
		if (persist) {
			return wc.doSave();
		} else {
			return wc;
		}
	}

	public static ILaunchConfigurationType getLaunchConfigurationType() {
		return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(
				getLaunchConfigurationTypeId());
	}

	public static String sanitize(String name) {
		final char[] DISALLOWED_CONFIG_NAME_CHARS = new char[] { '@', '&','\\', '/', ':', '*', '?', '"', '<', '>', '|', '\0' };
		// The disallowed list of chars is copied from 
		//  	org.eclipse.debug.internal.core.LaunchManager
		// Have to copy it here because it isn't public
		for (char c : DISALLOWED_CONFIG_NAME_CHARS) {
			name = name.replace(c, ' ');
		}
		return name;
	}

	private static String getLaunchConfigurationTypeId() {
		return "org.grails.ide.eclipse.core.launchconfig";
	}

}
