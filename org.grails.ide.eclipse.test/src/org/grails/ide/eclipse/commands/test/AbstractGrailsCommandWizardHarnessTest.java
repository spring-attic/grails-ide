/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.commands.test;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.CommandFactory;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ICommandParameter;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.IFrameworkCommand;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.IFrameworkCommandDescriptor;


/**
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 * @author Kris De Volder
 * @created Sep 17, 2010
 */
public class AbstractGrailsCommandWizardHarnessTest extends AbstractCommandTest {

	/**
	 * The current command instance that is being tested
	 */
	private static IFrameworkCommand currentCommandInstance;

	/**
	 * Verifies that the expected constructed command string matches what Grails
	 * tooling constructs for the given command instance.
	 * 
	 * @param instance
	 *            whose expression needs to be constructed
	 * @param expectedCommandExpression
	 *            expected expression
	 */
	protected void assertExpectedCommandString(IFrameworkCommand instance,
			String expectedCommandExpression) {
		String actualCommandExpression = GrailsCommandFactory
				.constructCommandString(instance);
		assertEquals(actualCommandExpression, expectedCommandExpression);
	}

	/**
	 * Gets the parameter instance for the given parameter name from the command
	 * instance. Null if it cannot find
	 * 
	 * @param name
	 * @param instance
	 * @return
	 */
	protected ICommandParameter getParameter(String name,
			IFrameworkCommand instance) {
		List<ICommandParameter> parameters = instance.getParameters();
		for (ICommandParameter parameter : parameters) {
			if (name.equals(parameter.getParameterDescriptor().getName())) {
				return parameter;
			}
		}
		return null;
	}

	/**
	 * Gets the parameter instance for the given parameter name from the command
	 * instance. Asserts that the parameter exists.
	 * 
	 * @param name
	 * @param instance
	 * @return
	 */
	protected ICommandParameter assertExistsAndGetParameter(String name,
			IFrameworkCommand instance) {
		ICommandParameter parameter = getParameter(name, instance);
		assertEquals(name, parameter.getParameterDescriptor().getName());
		return parameter;
	}

	/**
	 * Creates a command instance for the given command descriptor.
	 * 
	 * @param descriptor
	 * @return
	 */
	protected IFrameworkCommand createCommandInstance(
			IFrameworkCommandDescriptor descriptor) {
		currentCommandInstance = CommandFactory
				.createCommandInstance(descriptor);
		return currentCommandInstance;
	}

	protected IFrameworkCommand getCurrentlyTestedCommandInstance() {
		return currentCommandInstance;
	}

	protected void assertCommandName(IFrameworkCommand instance,
			String expectedName) {
		assertEquals(instance.getCommandDescriptor().getName(), expectedName);
	}

	protected void assertNumberOfParameters(IFrameworkCommand instance,
			int numberOfParameters) {
		assertEquals(numberOfParameters, instance.getParameters().size());
	}

	/**
	 * Verifies that the command has the correct name and number of parameter
	 * instances.
	 * 
	 * @param instance
	 * @param name
	 * @param numberOfParameters
	 */
	protected void assertCommandDefinition(IFrameworkCommand instance,
			String name, int numberOfParameters) {
		assertCommandName(instance, name);
		assertNumberOfParameters(instance, numberOfParameters);
	}

	protected ILaunchResult executeCommand(IFrameworkCommand instance, IProject project) throws CoreException {
	    GrailsCommand command = GrailsCommandFactory.getExecutableCommand(instance, project);
	    return command.synchExec();
	}
	protected void assertCommandExecution(IFrameworkCommand instance, IProject project) throws CoreException {
//	    System.out.println("Not executing command!  This command execution sporadically leads to tests hanging when Grails asks for user input.");
	    assertCommandExecution(instance, project, true);
	}
	protected void assertCommandExecution(IFrameworkCommand instance, IProject project, boolean expectingSuccess)
	        throws CoreException {
	    ILaunchResult result = executeCommand(instance, project);
	    assertEquals(
	            "Command failed to execute properly:\n======Output:\n"
	            + result.getOutput() + "\n======Error output:\n"
	            + result.getErrorOutput(), expectingSuccess, result.isOK());
	}

}
