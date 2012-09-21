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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathUtils;
import org.grails.ide.eclipse.core.internal.classpath.GrailsDependencyParser;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.BooleanParameterDescriptor;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.CommandFactory;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.CompositeCommandParameter;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ICommandParameter;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ICommandParameterDescriptor;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.IFrameworkCommand;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.IFrameworkCommandDescriptor;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.JavaParameterDescriptor;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ParameterFactory;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ParameterKind;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion;


/**
 * Class with factory methods to create different Grails commands. The general
 * idea here is that the created commands don't do anything besides running the
 * Grails command as an external/launch process. It is up to the caller to
 * handle any Eclipse specific post-processing.
 * <p>
 * If Eclipse specific post-processing needs to be done upon completion of the
 * command this can be done conveniently by calling the command's "synchExec"
 * method.
 * 
 * @author Kris De Volder
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 */
public abstract class GrailsCommandFactory {

	public static final String GRAILS_ARGUMENT_DELIMITER = "--";

	public static final ICommandParameterDescriptor[] EMPTY_DESCRIPTOR_LIST = new ICommandParameterDescriptor[] {};

	/*
	 * 
	 * START GRAILS command descriptors. These models define Grails commands.
	 * Developers should use these common static definitions when using Grails
	 * commands
	 */
	public static final IFrameworkCommandDescriptor CREATE_DOMAIN_CLASS = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"create-domain-class",
					"Create a domain and associated integration test for the given base name.",
					new ICommandParameterDescriptor[] { createGrailsJavaNameParameterDescriptor(
							"name",
							"Enter a base domain class name, or use content assist.",
							true, null) }));

	public static final IFrameworkCommandDescriptor CREATE_CONTROLLER = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"create-controller",
					"Create a controller and associated integration test for the given base name.",
					new ICommandParameterDescriptor[] { createGrailsJavaNameParameterDescriptor(
							"name",
							"Enter a base controller class name, or use content assist.",
							true, null) }));

	public static final IFrameworkCommandDescriptor CREATE_SERVICE = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"create-service",
					"Create a Grails service class for the given base name.",
					new ICommandParameterDescriptor[] { createGrailsJavaNameParameterDescriptor(
							"name",
							"Enter a base service class name, or use content assist",
							true, null) }));

	public static final IFrameworkCommandDescriptor CREATE_TAGLIB = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"create-tag-lib",
					"Creates a taglib.",
					new ICommandParameterDescriptor[] { createGrailsJavaNameParameterDescriptor(
							"name",
							"Enter a taglib class name, or use content assist",
							true, null) }));

	public static final IFrameworkCommandDescriptor GENERATE_ALL = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"generate-all",
					"Generates a controller and views for the given domain class",
					new ICommandParameterDescriptor[] { createGrailsJavaNameParameterDescriptor(
							"name",
							"Enter existing domain class name or use content assist.",
							true, null) }));

	public static final IFrameworkCommandDescriptor GENERATE_VIEWS = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"generate-views",
					"Generates a set of views for the given domain class",
					new ICommandParameterDescriptor[] { createGrailsJavaNameParameterDescriptor(
							"name",
							"Enter existing domain class name or use content assist.",
							true, null) }));

	public static final IFrameworkCommandDescriptor GENERATE_CONTROLLER = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"generate-controller",
					"Generates a controller for the given domain class",
					new ICommandParameterDescriptor[] { createGrailsJavaNameParameterDescriptor(
							"name",
							"Enter existing domain class name or use content assist.",
							true, null) }));

	public static final IFrameworkCommandDescriptor ADD_PROXY = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"add-proxy",
					"Adds a proxy configuration that Grails can use when communicating over the internet such as with the install-plugin command. "
							+ "The proxy configuration can be activated with the set-proxy command.",
					new ICommandParameterDescriptor[] {
							ParameterFactory.createBaseParameterDescriptor(
									"name",
									"The name of the proxy configuration",
									true, null, false, null, null),
							ParameterFactory
									.createBasePrefixedParameterDescriptor(
											"host", "The server host", true,
											null),
							ParameterFactory
									.createBasePrefixedParameterDescriptor(
											"port", "The server port", true,
											null),
							ParameterFactory
									.createBasePrefixedParameterDescriptor(
											"username", "The server username",
											false, null),
							ParameterFactory
									.createBasePrefixedParameterDescriptor(
											"password", "The server password",
											false, null) }));

	public static final IFrameworkCommandDescriptor BUG_REPORT = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"bug-report",
					"The bug-report command will package up only the sources of your application (excluding jars, static resources etc.)"
							+ " into a zip file with a timestamp appended. "
							+ " "
							+ "This is useful for reporting issues to the Grails JIRA installation",
					EMPTY_DESCRIPTOR_LIST));

	public static final IFrameworkCommandDescriptor CLEAN = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"clean",
					"The clean command will delete all compiled resources from"
							+ " the current Grails application. Since Groovy is a compiled language, as with Java,"
							+ " this is sometimes useful to clear old instances of classes out and ensure correct compilation",
					EMPTY_DESCRIPTOR_LIST));

	public static final IFrameworkCommandDescriptor CLEAR_PROXY = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"clear-proxy",
					"Clears the current proxy configuration Grails should use when"
							+ " communicating over the internet such as with the "
							+ "install-plugin command. Proxy configurations"
							+ " are defined using the add-proxy command",
					EMPTY_DESCRIPTOR_LIST));

	/**
	 * Special case boolean. The value cannot be added, only the name if true
	 */
	public static final IFrameworkCommandDescriptor COMPILE = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"compile",
					"The compile command will execute the compile"
							+ " phase of the Grails pre-packaging process, which "
							+ "pre-compiles Groovy and Java sources.",
					new ICommandParameterDescriptor[] {

					// This doesn't seem to do anything. Commenting out for now
					// ParameterFactory
					// .createBooleanParameterDescriptor(
					// "verboseCompile",
					// "You can enable verbose compilation for any Grails task by"
					// + " passing the flag -verboseCompile to the task "
					// +
					// "(e.g. grails run-app -verboseCompile), or by setting the "
					// + "verboseCompile Grails build setting. ",
					// false, false, "-")

					}));

	public static final IFrameworkCommandDescriptor INSTALL_TEMPLATES = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"install-templates",
					"Installs the templates used by Grails during code generation",
					EMPTY_DESCRIPTOR_LIST));

	public static final IFrameworkCommandDescriptor PLUGIN_INFO = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"plugin-info",
					"This command will display more detailed info about a given plug-in such as the author, location of documentation and so on.",
					new ICommandParameterDescriptor[] { ParameterFactory
							.createBaseParameterDescriptor("name",
									"Plugin name", true, null, false, null,
									null) }));

	public static final IFrameworkCommandDescriptor REMOVE_PROXY = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"remove-proxy",
					"Removes a proxy configuration that Grails can use when communicating over "
							+ "the internet such as with the install-plugin command. "
							+ "Proxy configurations can be added with the add-proxy command.",
					new ICommandParameterDescriptor[] { ParameterFactory
							.createBaseParameterDescriptor("name",
									"Proxy name", true, null, false, null, null) }));

	public static final IFrameworkCommandDescriptor CREATE_FILTERS = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"create-filters",
					"Creates a filters class for the give base name. "
							+ "For example for a base name logging a filters class "
							+ "called LoggingFilters will be created in the "
							+ "grails-app/conf directory.",
					new ICommandParameterDescriptor[] { ParameterFactory
							.createBaseParameterDescriptor("name",
									"Filter name", true, null, false, null,
									null) }));

	public static final IFrameworkCommandDescriptor CREATE_UNIT_TEST = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"create-unit-test",
					"Creates a unit test for the given base name. "
							+ "For example for a base name book an unit test called BookTests will be created "
							+ "in the test/unit directory. An unit test differs from an integration test in that "
							+ "the Grails environment is not loaded for each test execution and it is left up to "
							+ "you to perform the appropriate Mocking using GroovyMock or a Mock library such as EasyMock. "
							+ "Refer to the section on Unit Testing of the user guide for information on unit vs. integration testing.",
					new ICommandParameterDescriptor[] { createGrailsJavaNameParameterDescriptor(
							"name", "Base name of unit test", true, null) }));

	public static final IFrameworkCommandDescriptor CREATE_INTEGRATION_TEST = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"create-integration-test",
					"Creates a integration test for the given base name. "
							+ "For example for a base name book an integration test called BookTests will be created "
							+ "in the test/integration directory. An integration test differs from a unit test in that "
							+ "the Grails environment is fully loaded for each test execution. " 
							+ "Refer to the section on Unit Testing of the user guide for information on unit vs. integration testing.",
					new ICommandParameterDescriptor[] { createGrailsJavaNameParameterDescriptor(
							"name", "Base name of integration test", true, null) }));
	
	public static final IFrameworkCommandDescriptor WAR = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"war",
					"The war command will create a Web Application Archive (WAR)"
							+ " file which can be deployed on any Java EE compliant application server",
					new ICommandParameterDescriptor[] {
							ParameterFactory
									.createBaseParameterDescriptor(
											"file name",
											"Optional file name. If omitted, the application name and version will be used.",
											false, null, false, null, null),
							ParameterFactory
									.createBooleanParameterDescriptor(
											"nojars",
											"Packages the WAR with no jar files. Used for shared deployment",
											false, false, "--") }));

	public static final IFrameworkCommandDescriptor UPGRADE = addDescriptor(CommandFactory
			.createCommandDescriptor("upgrade",
					"The upgrade command will upgrade the current"
							+ " Grails application to currently installed"
							+ " version of Grails if possible.",
					EMPTY_DESCRIPTOR_LIST));

	public static final IFrameworkCommandDescriptor STATS = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"stats",
					"Outputs basic statistics about the current Grails application, "
							+ "including number of files, line count and so on",
					EMPTY_DESCRIPTOR_LIST));

	public static final IFrameworkCommandDescriptor SET_VERSION = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"set-version",
					"Sets the current application version inside the application metadata file (application.properties)",
					new ICommandParameterDescriptor[] { ParameterFactory
							.createBaseParameterDescriptor(
									"number",
									"When executing Grails retains metadata about the"
											+ " currently executing application inside the GrailsApplication "
											+ "object. The version of the application is one such item of metadata."
											+ " The version is used by the Grails war command to produce a versioned "
											+ "WAR archive and can also be used by the application at runtime to "
											+ "validate application state.",
									true, null, false, null, null) }));

	public static final IFrameworkCommandDescriptor SCHEMA_EXPORT = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"schema-export",
					"Uses Hibernate's SchemaExport tool to generate DDL or export the schema",
					new ICommandParameterDescriptor[] {
							ParameterFactory
									.createComboParameterDescriptor(
											"action",
											"Either 'generate' or 'export'. The default is 'generate'",
											false, "generate", false, null,
											null, new String[] { "export",
													"generate" }),
							ParameterFactory
									.createBooleanParameterDescriptor(
											"stdout",
											"Passing 'stdout' will cause the script to dump the ddl to stdout.",
											false, false, null),
							ParameterFactory
									.createBaseParameterDescriptor(
											"filename",
											"The name of the file to write the ddl to. The default is ddl.sql in the project root.",
											false, null, false, null, null) }));

	public static final IFrameworkCommandDescriptor PACKAGE = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"package",
					"Runs the packaging phase of Grails' runtime. This is mainly useful when used by other scripts.",
					EMPTY_DESCRIPTOR_LIST));

	public static final IFrameworkCommandDescriptor PACKAGE_PLUGIN = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"package-plugin",
					"Packages a plug-in as a zip archive which can then be installed into another application",
					EMPTY_DESCRIPTOR_LIST));

	public static final IFrameworkCommandDescriptor LIST_PLUGINS = addDescriptor(CommandFactory
			.createCommandDescriptor(
					"list-plugins",
					"Lists the plug-ins available from the Grails standard repository",
					new ICommandParameterDescriptor[] {
							ParameterFactory
									.createBaseParameterDescriptor(
											"repository",
											"The name of the repository used to"
													+ " produce the list of available plugins, otherwise"
													+ " all known repositories will be scanned. See the section"
													+ " on Plugin repositories in the user guide.",
											false, null, true, "-", "="),
							ParameterFactory
									.createBooleanParameterDescriptor(
											"installed",
											"List only the plugins that are installed into the current Grails application.",
											false, false, "-") }));

	public static final ICommandParameterDescriptor GRAILS_SYSTEM_ENV_PARAMETER_DESCRIPTOR = new GrailsEnvParameterDescriptor();

	private static Collection<IFrameworkCommandDescriptor> commandDescriptors;

	/**
	 * This method must be used in order to properly register a command. If not
	 * used, the command descriptor will not be registered and components that
	 * require Grails commands will not have access to it.
	 * <p>
	 * Returns the descriptor that was added. If the descriptor already exists,
	 * the existing one is returned instead.
	 * </p>
	 * 
	 * @param descriptor
	 *            to add
	 * @return descriptor that was added or existing one if it is already
	 *         registered
	 */
	private static IFrameworkCommandDescriptor addDescriptor(
			IFrameworkCommandDescriptor descriptor) {
		if (descriptor == null) {
			return null;
		}
		if (commandDescriptors == null) {
			commandDescriptors = new LinkedHashSet<IFrameworkCommandDescriptor>();
		}
		if (!commandDescriptors.contains(descriptor)) {
			commandDescriptors.add(descriptor);
		}
		return descriptor;
	}

	private static final IFrameworkCommandDescriptor[] COMMANDS_WITH_SYSTEM_ENV = new IFrameworkCommandDescriptor[] {
	// For now do not define any commands that require the system env parameter.
	/* GENERATE_ALL, GENERATE_CONTROLLER, GENERATE_VIEWS */

	};

	private static final HashSet<String> standardEnvs = new HashSet<String>();
	static {
		standardEnvs.add("dev");
		standardEnvs.add("prod");
		standardEnvs.add("test");
	}

	/*
	 * 
	 * END GRAILS COMMANDS
	 */

	/**
	 * All the registered commands. Never null.
	 */
	public static Collection<IFrameworkCommandDescriptor> getAllCommands() {
		return commandDescriptors;
	}

	public static Collection<IFrameworkCommandDescriptor> getCommandsWithSystemEnvParameter() {
		return new LinkedHashSet<IFrameworkCommandDescriptor>(
				Arrays.asList(COMMANDS_WITH_SYSTEM_ENV));
	}

	/*
	 * 
	 * GRAILS-specific parameter descriptors
	 */

	public static ICommandParameterDescriptor createGrailsJavaNameParameterDescriptor(
			String name, String description, boolean mandatory,
			String defaultValue) {
		return ParameterFactory.createJavaParameterDescriptor(name,
				description, mandatory, defaultValue, false, null, null,
				JavaParameterDescriptor.FLAG_CLASS);
	}

	/*
	 * Util methods
	 */

	/**
	 * Constructs the string representation of a command instance, including all
	 * parameters with values. Parameters with no values are omitted. The
	 * Command string may also contain system environment parameters appended
	 * BEFORE the actual command, if the system environment parameter is present
	 * as the first parameter in the command instance's parameter list.
	 */
	public static String constructCommandString(IFrameworkCommand command) {

		if (command == null) {
			return null;
		}

		List<ICommandParameter> parameters = command.getParameters();
		final StringBuffer actualCommand = new StringBuffer();
		if (parameters != null) {
			// Note that the first parameter may be a system env, therefore
			// append this first before
			// appending the actual command name

			int i = 0;
			if (parameters.size() > 0
					&& parameters.get(i).getParameterDescriptor()
							.equals(GRAILS_SYSTEM_ENV_PARAMETER_DESCRIPTOR)) {
				appendParameterString(actualCommand, command, parameters.get(i));
				i++;
			}

			// append the command name itself
			actualCommand.append(command.getCommandDescriptor().getName());
			actualCommand.append(" ");

			// Append remaining paramters
			for (; i < parameters.size(); i++) {
				ICommandParameter parameter = parameters.get(i);
				appendParameterString(actualCommand, command, parameter);
			}
		}
		return actualCommand.toString().trim();
	}

	/**
	 * Appends the string representation of a parameter to the given command
	 * buffer, adding a whitespace separator at the end of the parameter string
	 * that is appended.
	 * <p>
	 * This method also handles Composite parameters.
	 * </p>
	 * Null parameters or parameters with no values are not appended
	 * 
	 * @param commandBuffer
	 *            to append parameter
	 * @param command
	 *            corresponding to the string representation in the buffer
	 * @param parameter
	 *            parameter to append
	 * @return true if successfully appended, false if not appended (parameter
	 *         is null or has no value)
	 */
	public static boolean appendParameterString(StringBuffer commandBuffer,
			IFrameworkCommand command, ICommandParameter parameter) {
		if (commandBuffer == null || parameter == null || !parameter.hasValue()) {
			return false;
		}

		ICommandParameterDescriptor descriptor = parameter
				.getParameterDescriptor();
		Object value = parameter.getValue();
		// If it is a composite descriptor, find the child
		// descriptor
		// for the child parameter with a value
		if (descriptor.getParameterKind() == ParameterKind.COMPOSITE) {
			ICommandParameter childCommandParamter = ((CompositeCommandParameter) parameter)
					.getSetChildCommandParameter();
			if (childCommandParamter != null) {
				descriptor = childCommandParamter.getParameterDescriptor();
			}
		}

		// Something's wrong, continue to the next, and see if the
		// command
		// can still execute.
		if (descriptor == null) {
			GrailsCoreActivator
					.logWarning(
							"Unable to append parameter with value: "
									+ value
									+ " in command: "
									+ command.getCommandDescriptor().getName()
									+ ". No parameter descriptor found. Attempt will be made to execute the command without this parameter.",
							null);
			return false;
		}

		// Treat boolean values whose value is the parameter name itself
		// differently than other
		// parameters.
		if (descriptor.getParameterKind() == ParameterKind.BOOLEAN
				&& (descriptor instanceof BooleanParameterDescriptor)
				&& ((BooleanParameterDescriptor) descriptor).useNameAsValue()) {
			boolean booleanVal = ((Boolean) value).booleanValue();
			// Only include if the value is true
			if (booleanVal) {
				String prefix = descriptor.getParameterPrefix();
				String name = descriptor.getName();
				if (prefix != null) {
					commandBuffer.append(prefix);
				}
				commandBuffer.append(name);
			}

		} else {
			if (descriptor.requiresParameterNameInCommand()) {
				String prefix = descriptor.getParameterPrefix();
				String valueSeparator = descriptor.getValueSeparator();
				String name = descriptor.getName();
				if (prefix != null) {
					commandBuffer.append(prefix);
				}
				commandBuffer.append(name);
				if (valueSeparator != null) {
					commandBuffer.append(valueSeparator);
				}
			}

			commandBuffer.append(value);
			commandBuffer.append(" ");
		}

		return true;
	}

	/*
	 * 
	 * Executable commands
	 */

	/**
	 * Given an instance of a Grails command descriptor, return an executable Grails command
	 * for that command descriptor for the given project
	 */
	public static GrailsCommand getExecutableCommand(IFrameworkCommand command,
			IProject project) {
		String stringCommand = GrailsCommandFactory
				.constructCommandString(command);
		if (stringCommand != null) {
			// FIXADE: add logic for adding template source folder class path
			// entries
			// if (command.getCommandDescriptor() == INSTALL_TEMPLATES) {
			//
			// }
			return new GrailsCommand(project, stringCommand, true);
		}
		return null;
	}

	public static GrailsCommand createApp(IGrailsInstall install, String projectName) {
		return new GrailsCommand(install, "create-app").addArgument(projectName);
	}

	public static GrailsCommand createApp(String projectName) {
		return createApp(GrailsCoreActivator.getDefault().getInstallManager()
				.getDefaultGrailsInstall(), projectName);
	}

	public static GrailsCommand createPlugin(String projectName) {
        return createPlugin(GrailsCoreActivator.getDefault().getInstallManager()
                .getDefaultGrailsInstall(), projectName);
    }
	public static GrailsCommand createPlugin(IGrailsInstall install,
			String projectName) {
		return new GrailsCommand(install, "create-plugin").addArgument(projectName);
	}

	public static GrailsCommand createDomainClass(IProject project, String name) {
		return new GrailsCommand(project, "create-domain-class").addArgument(name);
	}
	
	/**
	 * A Grails command that forces a project's dependency data file (see {@link GrailsDependencyParser})
	 * to be recreated and brought in synch with the grails project's metadata.
	 * @param downloadSources 
	 */
	public static GrailsCommand refreshDependencyFile(IProject project) {
		GrailsCommand cmd = new GrailsCommand(project, "compile")
			.addArgument(nonInteractiveOption(project))
			.addArgument(refreshDependenciesOption(project));
			//TODO: KDV: (depend) Do we really need to do a full compile of all the code 
			//   to force the plugin dependencies to be brought up-to-date with application.properties file?
		cmd.enableRefreshDependencyFile(); //For now we always use the old mechanism for everything, except the source jars.
		return cmd;
	}
	
	private static String refreshDependenciesOption(IProject project) {
		if (GrailsVersion.V_2_0_0.compareTo(GrailsVersion.getEclipseGrailsVersion(project))<=0) {
			return "--refresh-dependencies";
		} else {
			return null;
		}
	}

	/**
	 * A command to download sourc jars for Grails dependencies in the classpath container.
	 */
	public static GrailsCommand downloadSourceJars(IProject project) {
		Assert.isTrue(GrailsVersion.V_2_0_0.compareTo(GrailsVersion.getEclipseGrailsVersion(project))<=0, 
				"Downloading source attachments is only supported for Grails version "+GrailsVersion.V_2_0_0+" or above");
		GrailsCommand cmd = new GrailsCommand(project, "refresh-dependencies");
		cmd.addArgument("--include-source");
//		cmd.addArgument("--include-javadoc");
		cmd.addArgument(GrailsClasspathUtils.getDependencySourcesDescriptorName(project));
		return cmd;
	}

	/**
	 * A Grails command that creates a deployable war file for a given project. 
	 * @param proj			The project, this should be a grails project.
	 * @param warFile		The to the war file. Can be null, if so, a default name will be generated by grails.
	 * 						If the file is a "relative" reference it will be interpreted relative to the project.
	 * @return
	 */
	public static GrailsCommand war(IProject proj, String env, File warFile) {
		if (env==null) {
			env = "prod";
		}
		if (standardEnvs.contains(env)) {
			GrailsCommand cmd = new GrailsCommand(proj)
				.addArgument(env)
				.addArgument("war");
			if (warFile!=null) {
				cmd.addArgument(warFile.getAbsolutePath());
			}
			return cmd;
		} else {
			GrailsCommand cmd = new GrailsCommand(proj,"war");
			if (warFile!=null) {
				cmd.addArgument(warFile.getAbsolutePath());
			}
			cmd.setSystemProperty("grails.env", env);
			return cmd;
		}
	}

	/**
	 * A Grails command that upgrades a given project to whatever grails install this project is associated with.
	 */
	public static GrailsCommand upgrade(IProject project) {
		return new GrailsCommand(project, "upgrade")
			.addArgument(nonInteractiveOption(project));
	}
	
	private static String nonInteractiveOption(IProject project) {
//		GrailsVersion projectsVersion = GrailsVersion.getEclipseGrailsVersion(project);
//Code below is temporary, option will go back to normal before release of 1.4
//		if (GrailsVersion.V_1_4.compareTo(projectsVersion) <=0) {
//			return " --nonInteractive";
//		} else {
			return "--non-interactive";
//		}
	}

	/** 
	 * A Grails command that uninstalls a given plugin.
	 */
	public static GrailsCommand uninstallPlugin(IProject project, PluginVersion plugin) {
		return new GrailsCommand(project, "uninstall-plugin")
			.addArgument(nonInteractiveOption(project))
			.addArgument(plugin.getName());
	}
	
	/** 
	 * A Grails command that installs a given plugin.
	 */
	public static GrailsCommand installPlugin(IProject project, PluginVersion plugin) {
		return new GrailsCommand(project, "install-plugin")
			.addArgument(nonInteractiveOption(project))
			.addArgument(plugin.getName())
			.addArgument(plugin.getVersion());
	}
	
	/**
	 * A grails command that installs a given plugin by name
	 */
	public static GrailsCommand installPlugin(IProject project, String pluginName) {
		return new GrailsCommand(project, "install-plugin")
			.addArgument(nonInteractiveOption(project))
			.addArgument(pluginName);
	}

	public static GrailsCommand clean(IProject project) {
		return new GrailsCommand(project, "clean");
	}

	public static GrailsCommand listPlugins(IProject project) {
		return new GrailsCommand(project, "list-plugins");
	}

	/**
	 * Used to create a command from a String entered by the user (i.e. in Grails command prompt or a similar UI for enterning and
	 * executing commands. This way of creating commands is completely unaward of command syntax and escape sequences etc.
	 * It is assumed that the user entering the command is familiar with the syntax and uses it correctly. This
	 */
	public static GrailsCommand fromString(IProject project, String script) {
		return new GrailsCommand(project, script, true); //Since the command is entered by the user, its up to the user to escape spaces etc.
		                                                // So, exceptionally, it is ok here to use the 'dirty' method of creating commands.
	}

}
