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

import static org.eclipse.debug.core.DebugPlugin.ATTR_PROCESS_FACTORY_ID;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.service.environment.Constants;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.model.DefaultGrailsInstall;
import org.grails.ide.eclipse.core.model.GrailsInstallManager;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;


/**
 * Utility class that deals with command line arguments and Launch configurations and their attributes.
 * 
 * @author Christian Dupuis
 * @author Kris De Volder
 * @since 2.2.0
 */
public class GrailsLaunchArgumentUtils {
	
	//TODO: KDV: (cleanup) none of the constants below should not need to be public. We should encapsulate all code dealing with
	// them into nice utility methods to access/compute properties from a Grails LaunchConfiguration. No code outside
	// of this class should need to handle these attributes directly.
	
	private static final String GRAILS_INSTALL_LAUNCH_ATTR = GrailsCoreActivator.PLUGIN_ID+".GRAILS_INSTALL";
	public static final String PROJECT_DIR_LAUNCH_ATTR = GrailsCoreActivator.PLUGIN_ID + ".PROJECT_DIR";
	
	/**
	 * Optional attribute: can be set to the name of a class that implements {@link GrailsBuildListener}. 
	 * The class must be in the org.grails.ide.eclipse.core plugin, which will be added to the classpath
	 * of the launch if this attribute is set.
	 */
	private static final String GRAILS_BUILD_LISTENER = GrailsCoreActivator.PLUGIN_ID + ".GRAILS_BUILD_LISTENER";
	
	/**
	 * Launch configuration attribute to set the location of the ".grails" folder.
	 * See also {@link IGrailsInstall} or {@link DefaultGrailsInstall} method getGrailsMetadataHome(). 
	 */
	public static final String GRAILS_WORK_DIR_LAUNCH_ATTR = GrailsCoreActivator.PLUGIN_ID + ".GRAILS_WORK_DIR";
	
	
	/**
	 * Launch configuration attribute with extra system properties (should be retrievable by System.getProperty from
	 * the external process).
	 */
	private static String SYSTEM_PROPERTIES_LAUNCH_ATTR = GrailsCoreActivator.PLUGIN_ID + ".SYSTEM_PROPS";

	public static String getGrailsBuildListener(ILaunchConfiguration conf) {
		try {
			return conf.getAttribute(GRAILS_BUILD_LISTENER, (String)null);
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
			return null;
		}
	}
	
	public static void setGrailsBuildListener(ILaunchConfigurationWorkingCopy conf, String className) {
	    // STS-1262, cannot load any grails classes in STS
	    //		try {
//			Assert.isLegal(GrailsBuildListener.class.isAssignableFrom(Class.forName(className)),
//					"Grails build listener does not implement "+GrailsBuildListener.class.getName());
//		} catch (ClassNotFoundException e) {
//			GrailsCoreActivator.log(e);
//		}
		conf.setAttribute(GRAILS_BUILD_LISTENER, className);
	}

	public static String mergeArguments(String originalArg, String[] vmArgs,
			String[] excludeArgs, boolean keepActionLast) {
		if (vmArgs == null) {
			return originalArg;
		}

		if (originalArg == null) {
			originalArg = "";
		}

		// replace and null out all vmargs that already exist
		int size = vmArgs.length;
		for (int i = 0; i < size; i++) {
			int ind = vmArgs[i].indexOf(" ");
			int ind2 = vmArgs[i].indexOf("=");
			if (ind >= 0 && (ind2 == -1 || ind < ind2)) { // -a bc style
				int index = originalArg
						.indexOf(vmArgs[i].substring(0, ind + 1));
				if (index == 0
						|| (index > 0 && originalArg.charAt(index - 1) == ' ')) {
					// replace
					String s = originalArg.substring(0, index);
					int index2 = getNextToken(originalArg, index + ind + 1);
					if (index2 >= 0) {
						originalArg = s + vmArgs[i]
								+ originalArg.substring(index2);
					} else {
						originalArg = s + vmArgs[i];
					}
					vmArgs[i] = null;
				}
			} else if (ind2 >= 0) { // a=b style
				int index = originalArg.indexOf(vmArgs[i]
						.substring(0, ind2 + 1));
				if (index == 0
						|| (index > 0 && originalArg.charAt(index - 1) == ' ')) {
					// replace
					String s = originalArg.substring(0, index);
					int index2 = getNextToken(originalArg, index);
					if (index2 >= 0) {
						originalArg = s + vmArgs[i]
								+ originalArg.substring(index2);
					} else {
						originalArg = s + vmArgs[i];
					}
					vmArgs[i] = null;
				}
			} else { // abc style
				int index = originalArg.indexOf(vmArgs[i]);
				if (index == 0
						|| (index > 0 && originalArg.charAt(index - 1) == ' ')) {
					// replace
					String s = originalArg.substring(0, index);
					int index2 = getNextToken(originalArg, index);
					if (!keepActionLast || i < (size - 1)) {
						if (index2 >= 0) {
							originalArg = s + vmArgs[i]
									+ originalArg.substring(index2);
						} else {
							originalArg = s + vmArgs[i];
						}
						vmArgs[i] = null;
					} else {
						// The last VM argument needs to remain last,
						// remove original arg and append the vmArg later
						if (index2 >= 0) {
							originalArg = s + originalArg.substring(index2);
						} else {
							originalArg = s;
						}
					}
				}
			}
		}

		// remove excluded arguments
		if (excludeArgs != null && excludeArgs.length > 0) {
			for (int i = 0; i < excludeArgs.length; i++) {
				int ind = excludeArgs[i].indexOf(" ");
				int ind2 = excludeArgs[i].indexOf("=");
				if (ind >= 0 && (ind2 == -1 || ind < ind2)) { // -a bc style
					int index = originalArg.indexOf(excludeArgs[i].substring(0,
							ind + 1));
					if (index == 0
							|| (index > 0 && originalArg.charAt(index - 1) == ' ')) {
						// remove
						String s = originalArg.substring(0, index);
						int index2 = getNextToken(originalArg, index + ind + 1);
						if (index2 >= 0) {
							// If remainder will become the first argument,
							// remove leading blanks
							while (index2 < originalArg.length()
									&& originalArg.charAt(index2) == ' ') {
								index2 += 1;
							}
							originalArg = s + originalArg.substring(index2);
						} else {
							originalArg = s;
						}
					}
				} else if (ind2 >= 0) { // a=b style
					int index = originalArg.indexOf(excludeArgs[i].substring(0,
							ind2 + 1));
					if (index == 0
							|| (index > 0 && originalArg.charAt(index - 1) == ' ')) {
						// remove
						String s = originalArg.substring(0, index);
						int index2 = getNextToken(originalArg, index);
						if (index2 >= 0) {
							// If remainder will become the first argument,
							// remove leading blanks
							while (index2 < originalArg.length()
									&& originalArg.charAt(index2) == ' ') {
								index2 += 1;
							}
							originalArg = s + originalArg.substring(index2);
						} else {
							originalArg = s;
						}
					}
				} else { // abc style
					int index = originalArg.indexOf(excludeArgs[i]);
					if (index == 0
							|| (index > 0 && originalArg.charAt(index - 1) == ' ')) {
						// remove
						String s = originalArg.substring(0, index);
						int index2 = getNextToken(originalArg, index);
						if (index2 >= 0) {
							// Remove leading blanks
							while (index2 < originalArg.length()
									&& originalArg.charAt(index2) == ' ') {
								index2 += 1;
							}
							originalArg = s + originalArg.substring(index2);
						} else {
							originalArg = s;
						}
					}
				}
			}
		}

		// add remaining vmargs to the end
		for (int i = 0; i < size; i++) {
			if (vmArgs[i] != null) {
				if (originalArg.length() > 0 && !originalArg.endsWith(" ")) {
					originalArg += " ";
				}
				originalArg += vmArgs[i];
			}
		}

		return originalArg;
	}

	public static void mergeClasspath(List<IRuntimeClasspathEntry> cp,
			IRuntimeClasspathEntry entry) {
		Iterator<IRuntimeClasspathEntry> iterator = cp.iterator();
		while (iterator.hasNext()) {
			IRuntimeClasspathEntry entry2 = iterator.next();
			if (entry2.getPath().equals(entry.getPath())) {
				return;
			}
		}
		cp.add(entry);
	}

	public static void replaceJREContainer(List<IRuntimeClasspathEntry> cp,
			IRuntimeClasspathEntry entry) {
		int size = cp.size();
		for (int i = 0; i < size; i++) {
			IRuntimeClasspathEntry entry2 = cp.get(i);
			if (entry2.getPath().uptoSegment(2).isPrefixOf(entry.getPath())) {
				cp.set(i, entry);
				return;
			}
		}
		cp.add(0, entry);
	}

	protected static int getNextToken(String s, int start) {
		int i = start;
		int length = s.length();
		char lookFor = ' ';

		while (i < length) {
			char c = s.charAt(i);
			if (lookFor == c) {
				if (lookFor == '"') {
					return i + 1;
				}
				return i;
			}
			if (c == '"') {
				lookFor = '"';
			}
			i++;
		}
		return -1;
	}

	public static List<String> addMemorySettings(List<String> args) {
		boolean mxFound = false;
		boolean permSizeFound = false;
		for (String arg : args) {
			if (arg.startsWith("-Xmx")) {
				mxFound = true;
			} else if (arg.startsWith("-XX:MaxPermSize=")) {
				permSizeFound = true;
			}
		}

		List<String> newArgs = new ArrayList<String>(args);
		if (!permSizeFound) {
			newArgs.add(0, "-XX:MaxPermSize=256m");
		}
		if (!mxFound) {
			newArgs.add(0, "-Xmx768M");
		}

		// Add in -server
		if (!newArgs.contains("-server") && !newArgs.contains("-client")) {
			newArgs.add(0, "-server");
		}
		return newArgs;
	}

	public static List<String> addSpringLoadedArgs(ILaunchConfiguration conf, List<String> vmArgs) throws CoreException {
		String command = GrailsLaunchConfigurationDelegate.getScript(conf);
		// STS-2638 include reloading agent when launching in interactive mode
		// only way to launch in interactive mode is to run with an empty command
		if (command == null || command.contains("-noreloading")) {
		    return vmArgs;
		}
		if (command.length() == 0 || command.contains("run-app") || command.contains("interactive")) {
			IGrailsInstall install = GrailsLaunchArgumentUtils.getGrailsInstall(conf);
			if (install != null && install.getVersion().compareTo(GrailsVersion.V_2_0_0) >=0) {
				File loadedJar = install.getSpringLoadedJar();
				File cacheDir = install.getSpringLoadedCacheDir();
				ArrayList<String> newArgs = new ArrayList<String>(vmArgs);
				newArgs.add("-javaagent:"+loadedJar); 
				newArgs.add("-noverify");
				newArgs.add("-Dspringloaded=profile=grails;cacheDir="+cacheDir);
				return newArgs;
			}
		}
		return vmArgs;
	}
	
	@SuppressWarnings({ "deprecation", "unchecked" })
	public static void prepareClasspath(
			ILaunchConfigurationWorkingCopy configuration,
			IGrailsInstall install, IProject project, IVMInstall vmInstall)
			throws CoreException {
		// FIXKDV FIXADE Copies of this code exist in 
	    // GrailsCommandLaunchConfigurationDelegate.launch()
	    // and GrailsLaunchConfigurationDelegate.launch()
	    // consider refactoring to combine
		IJavaProject javaProject = null;
		if (project!=null && project.hasNature(JavaCore.NATURE_ID)) {
			javaProject = JavaCore.create(project);
		}

		List<IRuntimeClasspathEntry> cp = GrailsLaunchArgumentUtils
				.getBootstrapClasspath(install);

		IVMInstall vm = null;
		if (vmInstall == null) {
			if (javaProject != null) {
				vm = JavaRuntime.getVMInstall(javaProject);
			} else {
				vm = JavaRuntime.getDefaultVMInstall();
			}
		} else {
			vm = vmInstall;
		}
		if (vm != null) {
			if (vm!=JavaRuntime.getDefaultVMInstall()) {
				configuration.setAttribute(
						IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME,
						vm.getName());
				configuration.setAttribute(
						IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, vm
						.getVMInstallType().getId());
			}

			Map<String, String> envs = configuration.getAttribute(
					ILaunchManager.ATTR_ENVIRONMENT_VARIABLES,
					new HashMap<String, String>());
// This should probably not be set here, but only just before the launch. This to ensure using
//   current project settings to determine JAVA_HOME. Setting this here will persist the variable if configuration is saved
//   and it will become outdated when project settings are changed.
//			envs.put("JAVA_HOME", vm.getInstallLocation().getAbsolutePath());

			String typeId = vm.getVMInstallType().getId();
			replaceJREContainer(cp,
					JavaRuntime.newRuntimeContainerClasspathEntry(
							new Path(JavaRuntime.JRE_CONTAINER).append(typeId)
									.append(vm.getName()),
							IRuntimeClasspathEntry.BOOTSTRAP_CLASSES));

			IPath jrePath = new Path(vm.getInstallLocation().getAbsolutePath());
			if (jrePath != null) {
				IPath toolsPath = jrePath.append("lib").append("tools.jar");
				if (toolsPath.toFile().exists()) {
					IRuntimeClasspathEntry toolsJar = JavaRuntime
							.newArchiveRuntimeClasspathEntry(toolsPath);
					int toolsIndex;
					for (toolsIndex = 0; toolsIndex < cp.size(); toolsIndex++) {
						IRuntimeClasspathEntry entry = cp.get(toolsIndex);
						if (entry.getType() == IRuntimeClasspathEntry.ARCHIVE
								&& entry.getPath().lastSegment()
										.equals("tools.jar")) {
							break;
						}
					}
					if (toolsIndex < cp.size()) {
						cp.set(toolsIndex, toolsJar);
					} else {
						mergeClasspath(cp, toolsJar);
					}
				}
			}
		}

		Iterator<IRuntimeClasspathEntry> iterator = cp.iterator();
		List<String> list = new ArrayList<String>();
		while (iterator.hasNext()) {
			IRuntimeClasspathEntry entry = iterator.next();
			try {
				list.add(entry.getMemento());
			} catch (Exception e) {
			}
		}

		configuration.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, list);
		configuration
				.setAttribute(
						IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH,
						false);
	}

	public static List<IRuntimeClasspathEntry> getBootstrapClasspath(
			IGrailsInstall install) {
		List<IRuntimeClasspathEntry> cp = new ArrayList<IRuntimeClasspathEntry>();
		for (File file : install.getBootstrapClasspath()) {
			try {
				cp.add(JavaRuntime.newArchiveRuntimeClasspathEntry(new Path(
						file.getCanonicalPath())));
			} catch (IOException e) {
			}
		}
		return cp;
	}

	public static void prepareLaunchConfiguration(IProject project,
			String script, IGrailsInstall install, String baseDir,
			ILaunchConfigurationWorkingCopy wc) throws CoreException {
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_ALLOW_TERMINATE, true);
		wc.setAttribute(ATTR_PROCESS_FACTORY_ID, GrailsRuntimeProcessFactory.ID);
		if (project==null) {
			//Only add this explicitly to the config if outside of a project.
			//When inside a project we should just follow project settings instead (rather than
			//persistently store a specific install in the launch config, since it will become
			//"out of synch" if the user changes project settings).
			GrailsLaunchArgumentUtils.setGrailsInstall(wc, install);
		}
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
				"org.codehaus.groovy.grails.cli.support.GrailsStarter");
		if (project != null) {
			wc.setAttribute(
					IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
					project.getName());
		}
		GrailsLaunchConfigurationDelegate.setScript(wc, script);
		wc.setAttribute(GrailsLaunchArgumentUtils.PROJECT_DIR_LAUNCH_ATTR, baseDir);

		// is this correct? Should it be
		// if (install.getGrailsWorkDir() != null) {
		// 		wc.setAttribute(GrailsCoreActivator.ATTRIBUTE_GRAILS_WORK_DIR,
		// 		install.getGrailsWorkDir());
		// }
		if (DefaultGrailsInstall.getDefaultGrailsWorkDir() != null) {
			wc.setAttribute(GrailsLaunchArgumentUtils.GRAILS_WORK_DIR_LAUNCH_ATTR,
					DefaultGrailsInstall.getDefaultGrailsWorkDir());
		}

		// Need to put something that's non-null or problems picking up native env params!
		Map<String, String> env = wc.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, (Map<String, String>)null);
		if (env==null) {
			env = new HashMap<String, String>();
		}
		//Map needs to be non-empty so it actually gets persisted.
		//We will ensure a GRAILS_STS_RUNNING env var is always set. But if it is already set, we skip all of this
		//to avoid https://issuetracker.springsource.com/browse/STS-2372
		String stsRunning = env.get("GRAILS_STS_RUNNING");
		if (stsRunning==null) {
			env.put("GRAILS_STS_RUNNING", "true"); 
			wc.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, env);
			wc.setAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
		}
		prepareClasspath(wc, install, project, null);
	}

	/**
	 * Prepare a LaunchConfiguration to be able to execute classes in the Grails
	 * install. This method assumes that the LaunchConfiguration is associated
	 * with a Grails project, so we can get the details of the associated grails
	 * install and use that to setup the classpath and environment parameters.
	 * 
	 * @throws CoreException
	 */
	public static void prepareLaunchConfigurationWithProject(
			ILaunchConfigurationWorkingCopy wc) throws CoreException {
		IProject project = getProject(wc);
		Assert.isLegal(project != null,
				"No project associated with this launch config");
		String baseDir = project.getLocation().toString();
		IGrailsInstall install = GrailsCoreActivator.getDefault()
				.getInstallManager().getGrailsInstall(project);
		prepareLaunchConfiguration(project, "", install, baseDir, wc);
	}

	public static IJavaProject getJavaProject(ILaunchConfiguration wc)
			throws CoreException {
		IProject proj = getProject(wc);
		if (proj != null)
			return JavaCore.create(proj);
		return null;
	}

	/**
	 * Returns the Java project specified by the given launch configuration, or
	 * <code>null</code> if none.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the Java project specified by the given launch configuration, or
	 *         <code>null</code> if none
	 * @exception CoreException
	 *                if unable to retrieve the attribute
	 */
	public static IProject getProject(ILaunchConfiguration configuration) {
		try {
			String projectName = getJavaProjectName(configuration);
			if (projectName != null) {
				projectName = projectName.trim();
				if (projectName.length() > 0) {
					return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				}
			}
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
		}
		return null;
	}

	/**
	 * Returns the Java project name specified by the given launch
	 * configuration, or <code>null</code> if none.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the Java project name specified by the given launch
	 *         configuration, or <code>null</code> if none
	 */
	public static String getJavaProjectName(ILaunchConfiguration configuration)
			throws CoreException {
		return configuration.getAttribute(
				IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
				(String) null);
	}

	public static IGrailsInstall getGrailsInstall(ILaunchConfiguration conf) {
		GrailsInstallManager installMan = GrailsCoreActivator.getDefault().getInstallManager();
		String installName = null;
		try {
			installName = conf.getAttribute(GRAILS_INSTALL_LAUNCH_ATTR, (String)null);
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
		}
		if (installName!=null) {
			return installMan.getGrailsInstall(installName);
		} else {
			IProject project = getProject(conf);
			return installMan.getGrailsInstall(project);
		}
	}
	
	public static void setGrailsInstall(ILaunchConfigurationWorkingCopy conf, IGrailsInstall install) {
		conf.setAttribute(GRAILS_INSTALL_LAUNCH_ATTR, install.getName());
	}

	public static String getGrailsHome(ILaunchConfiguration configuration) {
		IGrailsInstall install = getGrailsInstall(configuration);
		if (install!=null) {
			return install.getHome();
		}
		return null;
	}

	public static String getJavaHome(ILaunchConfiguration configuration) {
		try {
			IJavaProject javaProject = getJavaProject(configuration);
			IVMInstall vm = null;
			if (javaProject != null) {
				vm = JavaRuntime.getVMInstall(javaProject);
			} else {
				vm = JavaRuntime.getDefaultVMInstall();
			}
			return vm.getInstallLocation().getAbsolutePath();
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
			return null;
		}
	}

	/**
	 * Extra classpath entries, telling grails where to classload our implementation of 
	 * GrailsBuildListener.
	 */
	public static List<String> getBuildListenerClassPath(ILaunchConfiguration conf) {
		String buildListener = getGrailsBuildListener(conf);
		if (buildListener==null) {
			return null; //If there's no build listener there's no need for this
		} else {
			IGrailsInstall install = getGrailsInstall(conf);
			Assert.isNotNull(install, "Can't determine Grails install for launch config");
			GrailsVersion version = install.getVersion();
			return getBuildListenerClassPath(version);
		}
	}

	/**
	 * Determine which extra entries should be added to the runtime classpath for a given GrailsVersion.
	 * This includes both Grails version specific as well as 'shared' bits of the BuildListener.
	 * 
	 * @return Bundle-id
	 */
	public static List<String> getBuildListenerClassPath(GrailsVersion version) {
		ClasspathLocalizer localizer = new ClasspathLocalizer();
		return localizer.localizeClasspath(
				new EclipsePluginClasspathEntry("org.grails.ide.eclipse.runtime.shared", null),
				new EclipsePluginClasspathEntry(getRuntimeBundleFor(version), null)
		);
	}

	/**
	 * Determine which version-specific runtime bundle should be added to the classpath for a given GrailsVersion.
	 * 
	 * @return Bundle-id
	 */
	public static String getRuntimeBundleFor(GrailsVersion version) {
		if (version.compareTo(GrailsVersion.V_2_2_)>=0) {
			return "org.grails.ide.eclipse.runtime22";
		} else if (version.compareTo(GrailsVersion.SMALLEST_SUPPORTED_VERSION)<=0) {
			throw new Error("This version of Grails no longer supported: "+version);
		} else {
			return "org.grails.ide.eclipse.runtime13";
		}
	}
	
	public static void setSystemProperties(
			ILaunchConfigurationWorkingCopy launchConf,
			Map<String, String> systemProperties) {
		launchConf.setAttribute(SYSTEM_PROPERTIES_LAUNCH_ATTR, systemProperties);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> getSystemProperties(ILaunchConfiguration conf) {
		try {
			return conf.getAttribute(SYSTEM_PROPERTIES_LAUNCH_ATTR, (Map<String, String>)null);
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
			return null;
		}
	}

	/**
	 * Join a list of paths into a single String, with File.pathSeparator characters used to
	 * separate the entries from one another.
	 */
	public static String toPathsString(List<String> paths) {
		StringBuffer buildListenerClassPath = new StringBuffer();
		boolean first = true;
		for (String entry : paths) {
			if (!first) {
				buildListenerClassPath.append(File.pathSeparatorChar);
			}
			buildListenerClassPath.append(entry);
			first=false;
		}
		return buildListenerClassPath.toString();
	}

	/**
	 * Set property value in map, only if the property is not yet set in the map.
	 */
	public static void setMaybe(Map<String, String> props, String k, String v) {
		if (v!=null) {
			String existing = props.get(k);
			if (existing==null) {
				props.put(k, v);
			}
		}
	}

	public static final String[] proxyPropNames = {
		"http.proxyHost",
		"http.proxyPort",
		"http.nonProxyHosts",
		"https.proxyHost",
		"https.proxyPort",
		"https.nonProxyHosts",
		"ftp.proxyHost",
		"ftp.proxyPort",
		"ftp.nonProxyHosts"
	};
	
	/**
	 * Add proxy related system properties if needed.
	 */
	public static void addProxyProperties(Map<String, String> props) {
		for (String propName : proxyPropNames) {
			String sysProp = System.getProperty(propName);
			if (sysProp!=null) {
				setMaybe(props, propName, sysProp);
			}
		}
	}

	/**
	 * Helper method to add some environment parameters to an existing String[] of env parameters.
	 * Each String in the array is of the form "<key>=<value>". The <key> String shouldn't contain '='.
	 * <p>
	 * Code in JDT doesn't handle key values that have '=' in them, and so don't we.
	 * <p>
	 * If an entry already exists in the original, then it won't be added. This is to ensure that if
	 * some user explicitly puts a customized env paramter setting in a launch config we will not
	 * override it with our automatically added default.
	 * 
	 * @param superEnv The original env we want to modify
	 * @param extra	   Extra variables we want to define, in map form.
	 * @return A copy of the original env with extra variables added.
	 */
	public static String[] addToEnvMaybe(String[] superEnv, Map<String, String> extra) {
		if (superEnv==null) {
			superEnv = new String[0];
		}
		
		//Peeking at JDT code for merging env maps... it seems we need special handling of win32 platform, where the
		//keys are to be treated as case insensitive, but while preserving case at the same time.
		boolean win32= Platform.getOS().equals(Constants.OS_WIN32);
		
		Set<String> seenKeys = new HashSet<String>(superEnv.length+extra.size()); // separate set of case converted keys (for win32 handling)

		//copy existing and add keys to seenKeys
		ArrayList<String> newEnv = new ArrayList<String>(superEnv.length+extra.size());
		for (String entry : superEnv) {
			int split = entry.indexOf('=');
			String key = entry.substring(0, split);
			String val = entry.substring(split+1);
			seenKeys.add(caseKey(key, win32));
			newEnv.add(key+"="+val);
		}
		
		//add extra if not already in seenKeys
		for (Entry<String, String> entry : extra.entrySet()) {
			if (!seenKeys.contains(caseKey(entry.getKey(), win32))) {
				newEnv.add(entry.getKey()+"="+entry.getValue());
			}
		}
		
		return newEnv.toArray(new String[newEnv.size()]);
	}

	/**
	 * Convert a key used as an environment parameter to case(in)sensitive form depending on platform.
	 * @param win32 True if we are on win32 platform
	 */
	private static String caseKey(String key, boolean win32) {
		return win32 ? key.toUpperCase() : key;
	}

	public static GrailsVersion getGrailsVersion(ILaunchConfiguration conf) {
		IGrailsInstall install = getGrailsInstall(conf);
		if (install!=null) {
			return install.getVersion();
		}
		return GrailsVersion.UNKNOWN;
	}

}
