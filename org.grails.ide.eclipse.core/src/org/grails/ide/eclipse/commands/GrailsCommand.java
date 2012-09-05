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
package org.grails.ide.eclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathUtils;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.launch.SynchLaunch.LaunchResult;
import org.grails.ide.eclipse.core.model.GrailsBuildSettingsHelper;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.runtime.shared.DependencyData;
import org.grails.ide.eclipse.runtime.shared.SharedLaunchConstants;


/**
 * Class to help executing Grails commands. Contains various settings needed to
 * execute a given grails command and does whatever is necessary to execute it.
 * It provides a mechanism to execute the command synchronously.
 * @author Kris De Volder
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 */
public class GrailsCommand {
		
	/**
	 * Timeout value used for grails commands if the preference is not set in the
	 * preferences page.
	 */
	public static final int DEFAULT_TIMEOUT = 60000;
	
	private String command;
	private IProject project;
	private String path; 
	private IGrailsInstall install;
	
	/**
	 * Optional attribute: a class that should be attached to the external process as a
	 * grails build listener.
	 */
	private String buildListener;

	/**
	 * Optional attribute: contains key, value pairs that will be passed on to the
	 * external grails process in such a way so that it can access them via 
	 * {@link System}.getProperty().
	 */
	private Map<String, String> systemProperties;

	/**
	 * Output from this command shows in the console view?
	 */
	private boolean isShowOutput = true;

	/**
	 * Method is now package private. Please use GrailsCommandFactory to create commands.
	 */
	GrailsCommand(IProject project, String command) {
		this(null, project, command);
	}

	/** 
	 * This constructor is private and shouldn't be called by clients. However it should
	 * be called by all other constructors because it contains some work that should
	 * always be done regardless of the parameters.
	 */
	private GrailsCommand(IGrailsInstall install, IProject project, String command) {
		this(install, project, command, false);
	}

	/**
	 * When we no longer require the 'dirty' flag (all code is abiding by the intended
	 * api usage and doesn't pass in command strings that have spaces in them). This constructor
	 * should be removed so the assert is always executed.
	 * @deprecated
	 */
	private GrailsCommand(IGrailsInstall install, IProject project, String command, boolean dirty) {
		this.systemProperties = GrailsCoreActivator.getDefault().getLaunchSystemProperties();
		if (!dirty) {
			Assert.isLegal(command.indexOf(' ')<0, 
					"Initial command String should only contain the name of a command. " +
							"Add extra arguments with the addArgument method to ensure that proper " +
					"escape sequences are used for ' ' inside of arguments.");
		}
		this.install = install;
		this.project = project;
		this.command = command;
	}
	
	/**
	 * Launch a command, outside the scope of a particular project. This command
	 * will be launched using the Default grails install and the current
	 * directory set to the workspace root.
	 * <p>
	 * Suitable for running commands like "create-app" and "create-plugin" which
	 * can not be executed in the context of a project, because their purpose is
	 * the creation of the project.
	 * <p>
	 * Method is now package private. Please use GrailsCommandFactory to create commands.
	 */
	GrailsCommand(String command) {
		this(null, null, command);
	}

	/**
	 * Launch a command, outside the scope of a particular project. This command
	 * will be launched using the provided grails install. The current directory
	 * will be set to the workspace root.
	 * <p>
	 * Suitable for running commands like "create-app" and "create-plugin" which
	 * can not be executed in the context of a project, because their purpose is
	 * the creation of the project.
	 * <p>
	 * Method is now package private. Please use GrailsCommandFactory to create commands.
	 */
	GrailsCommand(IGrailsInstall install, String command) {
		this(install, null, command);
	}

	GrailsCommand(IProject proj) {
		this(null, proj, "");
	}

	/**
	 * This constructor is deprecated. It allows 'old style' code that creates a command 
	 * string by simply splicing all the bits together with '+'. Such code is considered 'dirty'
	 * because it provides little guarantee that spaces inside of command 
	 * argument will not cause trouble. Such code should be restructured to 
	 * us GrailsCommand.addArgument instead.
	 *
	 * @deprecated 
	 */
	GrailsCommand(IProject project, String command, boolean dirty) {
		this(null, project, command, dirty);
	}

	/**
	 * Execute the command "synchronously" i.e. block the current thread until
	 * the command finishes.
	 * 
	 * @return An object containing information about the command's result, such
	 *         as the output/error stream contents.
	 * @throws CoreException
	 *             if something went wrong.
	 */
	public ILaunchResult synchExec() throws CoreException {
		if (JDKCheck.check(getProject())) {
			return GrailsExecutor.getInstance(getGrailsVersion()).synchExec(this);
		} else {
			throw new LaunchResult(-1) {
				public IStatus getStatus() {
					return new Status(IStatus.CANCEL, GrailsCoreActivator.PLUGIN_ID, "User canceled command");
				}
			}.getCoreException();
		}
	}

	public GrailsVersion getGrailsVersion() {
		return getGrailsInstall().getVersion();
	}

	public void runPostOp() {
        // empty for now
    }

	/**
	 * Get timeout value that is used to determine if the external process executing this
	 * command is "stuck". A process is deemed to be stuck if it does not return new 
	 * input within the timeout. Each time new input is seen the timeout is reset.
	 * 
	 * @return Timeout value in milliseconds.
	 */
    public int getGrailsCommandTimeOut() {
		return GrailsCoreActivator.getDefault().getGrailsCommandTimeOut();
	}

	public IGrailsInstall getGrailsInstall() {
		if (project != null) {
			return GrailsCoreActivator.getDefault().getInstallManager()
					.getGrailsInstall(project);
		} else if (install != null) {
			return install;
		} else {
			return GrailsCoreActivator.getDefault().getInstallManager()
					.getDefaultGrailsInstall();
		}
	}

	@Override
	public String toString() {
		if (project != null) {
			return "GrailsCommand(" + project + "> " + command + ")";
		} else {
			return "GrailsCommand(" + command + ")";
		}
	}
	
	/**
	 * Calling this method will enable the creation of a new {@link DependencyData} file in the
	 * default location for the associated project. The creating of the file will be triggered
	 * near the end of the command's execution. Using this is more efficient than creating the
	 * dependency data in a new grails process (since we would have to pay the cost of
	 * booting grails twice.
	 * <p>
	 * Note: this only forces a refresh of the data *file* but doesn't force the data file to actually
	 * be read by eclipse.
	 */
	public void enableRefreshDependencyFile() {
		attachBuildListener(SharedLaunchConstants.DependencyExtractingBuildListener_CLASS);
		setSystemProperty(SharedLaunchConstants.DEPENDENCY_FILE_NAME_PROP,
				GrailsClasspathUtils.getDependencyDescriptorName(project));
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Attach a given class as a "build listener" to the resulting grails process. Note that the process runs in
	 * an other JVM and the build listener is classloaded by it, so there is no way to directly share state with 
	 * this build listener!
	 * <p>
	 * When a build listener is attached to a command, a reference the {@link GrailsCoreActivator} plugin will be
	 * added to the launch's classpath. It is therefore assumed that the build listener class, is declared in that 
	 * plugin.
	 * <p>
	 * Any information the build listener wants to communicate back should be passed in some creative way, like
	 * saving this info into a file or sending it back over a socket.
	 * <p>
	 * For an example on how to use this, see GrailsCommandTest.
	 */
	public void attachBuildListener(String klass) {
		Assert.isLegal(buildListener==null || buildListener==klass, 
				"Attaching multiple build listeners is not (yet) supported");
		this.buildListener = klass;
	}

	/**
	 * Any key-value pairs registered by calling this method will be passed on to the external process,
	 * in such a way that they can be accessed by calling System.getProperty from the external process.
	 */
	public void setSystemProperty(String key, String value) {
		if (systemProperties==null) {
			systemProperties = new HashMap<String, String>();
		}
		systemProperties.put(key, value);
	}

	/**
	 * If this is set to 'true' it will cause the command's output to be shown in the console view.
	 * If false, the launch won't be registered with the UI and output won't be shown in the console view.
	 * <p>
	 * The default value for this property is 'true'.
	 */
	public void setShowOutput(boolean showOutput) {
		this.isShowOutput = showOutput;
	}

	/**
	 * Sets the path where the command is to execute. If this is not set then some default path
	 * will be used (if project is set the project determines the path, if not, the OS's current
	 * directory is used implicitly).
	 * 
	 * @param path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	public String getBuildListener() {
		return buildListener;
	}

	public Map<String, String> getSystemProperties() {
		return systemProperties;
	}

	public IProject getProject() {
		return project;
	}

	public String getPath() throws IOException {
		if (path==null) {
			path = GrailsBuildSettingsHelper.getBaseDir(project);
			if (path==null) {
				path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().getCanonicalPath();
			}
		}
		return path;
	}

	public String getCommand() {
		return command;
	}

	public boolean isShowOutput() {
		return isShowOutput;
	}

	public File getDependencyFile() {
		String file = getSystemProperty(SharedLaunchConstants.DEPENDENCY_FILE_NAME_PROP);
		if (file!=null) {
			return new File(file);
		}
		return null;
	}

	private String getSystemProperty(String propName) {
		if (systemProperties!=null) {
			return systemProperties.get(propName);
		}
		return null;
	}

	/**
	 * Append an argument to the command string. This method escapes spaces to avoid the argument
	 * being split into multiple arguments.
	 * <p>
	 * @return The receiver object to allow easy chaining of 'addArgument' calls.
	 */
	public GrailsCommand addArgument(String arg) {
		if (arg!=null) {
			if (!command.equals("")) {
				command = command + " ";
			}
			command = command+escapeArgument(arg);
		}
		return this;
	}

	private String escapeArgument(String argument) {
		Assert.isLegal(!(argument.contains("'") && argument.contains("\"")), "Can\'t handle single and double"
						+ " quotes in same argument");
		if (argument.contains("\"")) {
			return '\'' + argument + '\'';
		} else if (argument.contains("\'") || argument.contains(" ")) {
			return '\"' + argument + '\"';
		} else {
			return argument;
		}
	}

	/**
	 * This is a 'backdoor' to call the GrailsCommand constructor directly instead of via 
	 * a 'GrailsCommandFactory' method. This is only intended for unit testing
	 * purposes. Normal clients should use GrailsCommandFactory to create some specific
	 * type of command.
	 */
	public static GrailsCommand forTest(IProject project, String commandName) {
		return new GrailsCommand(project, commandName);
	}

	/**
	 * This is a 'backdoor' to call the GrailsCommand constructor directly instead of via 
	 * a 'GrailsCommandFactory' method. This is only intended for unit testing
	 * purposes. Normal clients should use GrailsCommandFactory to create some specific
	 * type of command.
	 */
	public static GrailsCommand forTest(String commandName) {
		return new GrailsCommand(commandName);
	}

}
