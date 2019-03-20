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
package org.grails.ide.eclipse.ui.internal.actions;

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.GrailsResourceUtil;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.CommandFactory;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ICommandListener;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.IFrameworkCommand;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.IFrameworkCommandDescriptor;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.actions.AbstractCommandActionDelegate;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.wizard.GenericCommandWizard;

import org.grails.ide.eclipse.ui.internal.wizard.GrailsCommandWizard;

/**
 * @author Nieraj Singh
 * @author Kris De Volder
 * @author Andrew Eisenberg
 */
public class GrailsCommandWizardActionDelegate extends
		AbstractCommandActionDelegate {

	public enum GrailsCommandAction {

		CREATE_DOMAIN("org.grails.ide.eclipse.ui.createDomainClass"), 
		CREATE_CONTROLLER("org.grails.ide.eclipse.ui.createController"), 
		CREATE_SERVICE("org.grails.ide.eclipse.ui.createService"), 
		CREATE_TAGLIB("org.grails.ide.eclipse.ui.createTaglib"), 
		CREATE_FILTERS("org.grails.ide.eclipse.ui.createFilters"),
		COMMAND_WIZARD("org.grails.ide.eclipse.ui.grailsCommandWizard");
		

		private String actionName;

		private GrailsCommandAction(String actionName) {
			this.actionName = actionName;
		}

		public String getActionName() {
			return actionName;
		}
	}

	protected GrailsCommandAction getActionType(String actionName) {

		if (actionName == null) {
			return null;
		}

		GrailsCommandAction[] actions = GrailsCommandAction.values();
		for (GrailsCommandAction action : actions) {
			if (action.getActionName().equals(actionName)) {
				return action;
			}
		}
		return null;
	}

	protected Collection<IProject> getSelectionProjects(IProject selectedProject) {
		return GrailsResourceUtil.getAllGrailsProjects();
	}

	protected void addGrailsCommands(ICommandListener listener) {
		if (listener != null) {
			Collection<IFrameworkCommandDescriptor> commandDescriptors = GrailsCommandFactory
					.getAllCommands();
			if (commandDescriptors != null) {
				for (IFrameworkCommandDescriptor commandDescriptor : commandDescriptors) {
					listener.addCommandDescriptor(commandDescriptor);
				}
			}
		}
	}

	protected GenericCommandWizard getCommandWizard(
			Collection<IProject> projects, IFrameworkCommand command) {
		GrailsCommandWizard wizard = new GrailsCommandWizard(projects, command);
		addGrailsCommands(wizard);
		return wizard;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.springsource.sts.frameworks.ui.internal.actions.
	 * AbstractCommandActionDelegate
	 * #getSelectedCommand(org.eclipse.jface.action.IAction,
	 * org.eclipse.jface.viewers.ISelection)
	 */
	protected IFrameworkCommand getSelectedCommand(IAction action,
			ISelection selection) {
		GrailsCommandAction type = getActionType(action.getId());

		if (type == null) {
			return null;
		}
		IFrameworkCommandDescriptor commandDescriptor = null;
		switch (type) {
		case CREATE_DOMAIN:
			commandDescriptor = GrailsCommandFactory.CREATE_DOMAIN_CLASS;
			break;
		case CREATE_CONTROLLER:
			commandDescriptor = GrailsCommandFactory.CREATE_CONTROLLER;
			break;
		case CREATE_SERVICE:
			commandDescriptor = GrailsCommandFactory.CREATE_SERVICE;
			break;
		case CREATE_TAGLIB:
			commandDescriptor = GrailsCommandFactory.CREATE_TAGLIB;
			break;
		case CREATE_FILTERS:
			commandDescriptor = GrailsCommandFactory.CREATE_FILTERS;
			break;
		}
		if (commandDescriptor != null) {
			return CommandFactory.createCommandInstance(commandDescriptor);
		}
		return null;
	}

	protected boolean shouldAddToProjectList(IProject project) {
		boolean shouldAdd = super.shouldAddToProjectList(project);
		shouldAdd &= GrailsNature.isGrailsProject(project)
				&& GrailsResourceUtil.hasClasspathContainer(project);
		return shouldAdd;
	}

}
