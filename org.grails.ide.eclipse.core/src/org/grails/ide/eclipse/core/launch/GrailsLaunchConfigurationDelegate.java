/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.launch;

import static org.grails.ide.eclipse.core.launch.LaunchListenerManager.getLaunchListener;

import java.io.File;
import java.io.IOException;
import java.net.URI;
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
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.internal.launching.LaunchingMessages;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.osgi.util.NLS;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.GrailsBuildSettingsHelper;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.core.util.PortFinder;
import org.grails.ide.eclipse.core.workspace.GrailsWorkspace;
import org.grails.ide.eclipse.runtime.shared.DependencyData;
import org.springsource.ide.eclipse.commons.core.HttpUtil;

/**
 * @author Christian Dupuis
 * @since 2.2.0
 */
public class GrailsLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate { 

	private static final String SCRIPT_ATTR = GrailsCoreActivator.PLUGIN_ID + ".SCRIPT";
	private static final String ORG_SCRIPT_ATTR = GrailsCoreActivator.PLUGIN_ID + ".ORG_SCRIPT";
	
	private PortFinder portFinder = new PortFinder();

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

	/**
	 * Create debugging target similar to a remote debugging session would and add them to the launch.
	 * This is to support debugging of 'forked mode' run-app and test-app processes. These are
	 * processes spun-off by Grails in new JVM. 
	 * @param port the remote launch will be listening on for forked process to connect to.
	 */
	private void launchRemote(int port, ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		if (port<0) {
			return;
		}
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		monitor.beginTask(NLS.bind(LaunchingMessages.JavaRemoteApplicationLaunchConfigurationDelegate_Attaching_to__0_____1, new String[]{configuration.getName()}), 3); 
		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}						
		try {			
			monitor.subTask(LaunchingMessages.JavaRemoteApplicationLaunchConfigurationDelegate_Verifying_launch_attributes____1); 
							
			String connectorId = "org.eclipse.jdt.launching.socketListenConnector";//getVMConnectorId(configuration);
			IVMConnector connector = JavaRuntime.getVMConnector(connectorId);
			if (connector == null) {
				abort(LaunchingMessages.JavaRemoteApplicationLaunchConfigurationDelegate_Connector_not_specified_2, null, IJavaLaunchConfigurationConstants.ERR_CONNECTOR_NOT_AVAILABLE); 
			}
			
			Map<String, String> argMap = new HashMap<String, String>();
	        
//	        int connectTimeout = Platform.getPreferencesService().getInt(
//	        		LaunchingPlugin.ID_PLUGIN, 
//	        		JavaRuntime.PREF_CONNECT_TIMEOUT, 
//	        		JavaRuntime.DEF_CONNECT_TIMEOUT, 
//	        		null);
	        argMap.put("timeout", "120000"); // Give grails run-app command enough time to build the app and kick off a forked process.
	        argMap.put("port", ""+port);
	
			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}
			
			monitor.worked(1);

			//Don't think we need to set source location since the main launch method already does this.
			
//			monitor.subTask(LaunchingMessages.JavaRemoteApplicationLaunchConfigurationDelegate_Creating_source_locator____2); 
//			// set the default source locator if required
//			setDefaultSourceLocator(launch, configuration);
//			monitor.worked(1);		
			
			// connect to remote VM
			connector.connect(argMap, monitor, launch);
			
			// check for cancellation
			if (monitor.isCanceled()) {
				IDebugTarget[] debugTargets = launch.getDebugTargets();
	            for (int i = 0; i < debugTargets.length; i++) {
	                IDebugTarget target = debugTargets[i];
	                if (target.canDisconnect()) {
	                    target.disconnect();
	                }
	            }
	            return;
			}
		}
		finally {
			monitor.done();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {

		try {
			
			GrailsVersion version = GrailsLaunchArgumentUtils.getGrailsVersion(configuration);
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
	
			IVMRunner runner;
			IVMInstall vm = verifyVMInstall(configuration);
			if (GrailsVersion.V_2_3_.compareTo(version) <= 0) {
				//We'll be debugging the forked process, not the run-app command.
				runner = vm.getVMRunner(ILaunchManager.RUN_MODE);
			} else {
				runner = vm.getVMRunner(mode);
			}
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
			//vmArgs.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8123");
	
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
			int forkedProcessDebugPort = addForkedModeDebuggingArgs(configuration, mode, vmArgs);
			ArrayList<Integer> killPorts = addKillPortArg(project, vmArgs);
	
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

			if (ILaunchManager.DEBUG_MODE.equals(mode)) {
				launchRemote(forkedProcessDebugPort, configuration, mode, launch, subMonitor); 
			}
			GrailsCoreActivator.getDefault().notifyCommandStart(project);
			runner.run(runConfiguration, launch, monitor);
			AbstractLaunchProcessListener listener = getLaunchListener(configuration);
			if (listener != null) {
				listener.init(launch.getProcesses()[0]);
			}
			DebugPlugin.getDefault().addDebugEventListener(new GrailsProcessListener(launch.getProcesses(), project, killPorts));
			subMonitor.worked(1);
		}
		catch (Exception e) {
			GrailsCoreActivator.log(e);
		}
	}

	/**
	 * Add a system property arg to set killport for Grails 2.3 and higher.
	 * @param project 
	 * @return An array of kill ports to try in order to ask Grails forked process to terminate.
	 */
	private ArrayList<Integer> addKillPortArg(IProject project, List<String> vmArgs) {
		ArrayList<Integer> ports = null;
		if (project!=null) {
			if (GrailsVersion.V_2_3_.compareTo(GrailsVersion.getEclipseGrailsVersion(project))<=0) {
				ports = new ArrayList<Integer>(2); //Will have 1 or two elements not more.
				int serverPort = GrailsWorkspace.get().create(project).getServerPort();
				if (serverPort!=DependencyData.UNKNOWN_PORT) {
					ports.add(serverPort+1);
				}
				//The next bit really only expected to work in in Grails 2.3
				try {
					int allocatedKillPort = portFinder.findUniqueFreePort();
					vmArgs.add("-Dgrails.forked.kill.port="+allocatedKillPort);
					ports.add(allocatedKillPort);
				} catch (IOException e) {
					//non fatal... log and proceed
					GrailsCoreActivator.log(e);
				}
			}
		}
		return ports;
	}

	/**
	 * Helper function to add system properties that tell Grails to debug run-app or test-app in 
	 * as a forked process in debugging mode. 
	 * <p>
	 * These properties are added only if applicable. I.e. recent enough graisl version and
	 * this is a debug-mode launch.
	 * <p>
	 * In the process of adding the arguments, we will pick a free port. 
     *
	 * @return Chosen debug port or -1 if not launching in forked debug mode.
	 */
	private int addForkedModeDebuggingArgs(ILaunchConfiguration conf,
			String mode, List<String> args) throws IOException {
		// TODO Auto-generated method stub
		if (!ILaunchManager.DEBUG_MODE.equals(mode)) {
			return -1;
		}
		GrailsVersion version = GrailsLaunchArgumentUtils.getGrailsVersion(conf);
		if (GrailsVersion.V_2_3_.compareTo(version)>0) {
			return -1; //Disable this feature for pre Grails 2.3.
		}
		int debugPort = portFinder.findUniqueFreePort();
		args.add("-Dgrails.project.fork.run.debug=true");
		args.add("-Dgrails.project.fork.test.debug=true");
		String debugArgs = "-Xrunjdwp:transport=dt_socket,server=n,suspend=y,address=" + debugPort;
		args.add("-Dgrails.project.fork.run.debugArgs="+debugArgs);
		args.add("-Dgrails.project.fork.test.debugArgs="+debugArgs);
		return debugPort;
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
		private List<Integer> killPorts;
		private IProcess[] processes; //Can be 1 or 2 processes. If debugging forked mode grails one is the parent process the other the child.

		public GrailsProcessListener(IProcess[] processes, IProject project, ArrayList<Integer> killPorts) {
			this.project = project;
			this.processes = processes;
			this.killPorts = killPorts;
		}

		public void handleDebugEvents(DebugEvent[] events) {
			if (events != null && project != null) {
				int size = events.length;
				for (int i = 0; i < size; i++) {
					for (IProcess process : processes) {
						if (process != null && process.equals(events[i].getSource())
								&& events[i].getKind() == DebugEvent.TERMINATE) {

							DebugPlugin.getDefault().removeDebugEventListener(this);
							terminateForked();
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

		private void terminateForked() {
			if (killPorts!=null) {
				for (int killPort : killPorts) {
					try {
						URI killUrl = new URI("http://localhost:"+killPort);
						HttpUtil.ping(killUrl);
					} catch (Throwable e) {
					}
				}
			}
			if (processes!=null && processes.length>1) {
				//Make sure all processes are terminated
				for (IProcess process : processes) {
					try {
						if (process.canTerminate() && !process.isTerminated()) {
							process.terminate();
						}
					} catch (Throwable e) {
						GrailsCoreActivator.log(e);
					}
				}
				processes = null;
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
