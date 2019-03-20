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
package org.grails.ide.eclipse.ui.internal.wizard;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkbench;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsResourceUtil;
import org.grails.ide.eclipse.core.model.GrailsCommandAdapter;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ICommandParameter;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ICommandParameterDescriptor;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.IFrameworkCommand;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.IFrameworkCommandDescriptor;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ParameterFactory;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ParameterKind;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.utils.ProjectFilter;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.utils.SelectionUtils;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.wizard.GenericCommandWizard;
import org.springsource.ide.eclipse.commons.frameworks.ui.internal.wizard.GenericWizardCommandListPage;

import org.grails.ide.eclipse.ui.internal.utils.OpenNewResourcesCommandListener;

/**
 * Grails-specific command wizard with support for Grails environment variable.
 * @author Nieraj Singh
 * @author Kris De Volder
 */
public class GrailsCommandWizard extends GenericCommandWizard {

	public static final String WIZARD_TITLE = "Grails Command Wizard";
	public static final String WIZARD_IMAGE_LOCATION = "platform:/plugin/org.grails.ide.eclipse.ui/icons/full/wizban/grails_wizban.png";

	public GrailsCommandWizard(IFrameworkCommand command) {
		this(GrailsResourceUtil.getAllGrailsProjects(), command);
	}

	public GrailsCommandWizard(Collection<IProject> projects, IFrameworkCommand command) {
		super(command, WIZARD_TITLE, null, WIZARD_IMAGE_LOCATION, projects, false);
	}

	protected GenericWizardCommandListPage createCommandListPage() {
		return new GrailsCommandListPage(getWindowTitle());
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		customizeCommand(selection);
	}
	
	/**
	 * Behaviour:
	 * 
	 * <p>
	 * 
	 * Create a parameters page ONLY under these conditions:
	 * <li>A command has been selected and a parameter page doesn't currently
	 * exist</li>
	 * <li>a parameter page exists but corresponds to a command different than
	 * what is currently selected in the wizard</li>
	 * <li>a parameter page exists, and it corresponds to the selected command,
	 * but a user has requested that the Grails environment variable be added or
	 * removed, in which case the parameter page controls have to be recreated</li>
	 * </p>
	 * <p>
	 * Otherwise, use the existing parameters page as to preserve the current
	 * control values.
	 * </p>
	 */
	public IWizardPage getNextPage(IWizardPage page) {

		IFrameworkCommand commandInstance = getCommandInstance();
		if (page == commandListPage) {
			boolean hasSystemEnv = hasGrailsSystemEnv();
			boolean hasCommandChanged = false;
			if (shouldAddGrailsSystemEnv() && !hasSystemEnv) {
				addGrailsSystemEnv();
				hasCommandChanged = true;
			} else if (!shouldAddGrailsSystemEnv() && hasSystemEnv) {
				removeGrailsSystemEnv();
				hasCommandChanged = true;
			}

			if (parameterPage != null
					&& (parameterPage.getCommand() == commandInstance)
					&& !hasCommandChanged) {
				// If no changes, use the existing page.
				return parameterPage;
			} else if (commandInstance != null) {
				parameterPage = createParameterPage(commandInstance);
				return parameterPage;
			}
		}
		return super.getNextPage(page);

	}

	/**
	 * If displaying the command list page (first page), update the Grails env
	 * variable check box.
	 */
	public IWizardPage getPreviousPage(IWizardPage page) {
		IWizardPage previousPage = super.getPreviousPage(page);
		if (previousPage instanceof GrailsCommandListPage) {
			GrailsCommandListPage grailsListPage = (GrailsCommandListPage) previousPage;
			grailsListPage.setGrailsSystemEnv(hasGrailsSystemEnv());
		}
		return previousPage;
	}

	/**
	 * 
	 * @return true if the UI control in the command list page indicates that
	 *         the user would like to configure the Grails env variable. False
	 *         otherwise
	 */
	protected boolean shouldAddGrailsSystemEnv() {

		if (commandListPage instanceof GrailsCommandListPage) {
			return ((GrailsCommandListPage) commandListPage)
					.shouldAddGrailsSystemEnv();
		}
		return false;
	}

	/**
	 * 
	 * @return true if the command instance already has a Grails env parameter
	 *         present as the first entry of its parameter list. False
	 *         otherwise.
	 */
	protected boolean hasGrailsSystemEnv() {
		IFrameworkCommand command = getCommandInstance();
		if (command != null) {
			List<ICommandParameter> parameterInstances = command
					.getParameters();
			if (parameterInstances != null && parameterInstances.size() > 0) {
				// If it has a system env it should be the first parameter
				ICommandParameter potentialSystemEnvParameter = parameterInstances
						.get(0);
				if (GrailsCommandFactory.GRAILS_SYSTEM_ENV_PARAMETER_DESCRIPTOR
						.equals(potentialSystemEnvParameter
								.getParameterDescriptor())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Adds a Grails env parameter as the first parameter in a command's list.
	 * If successfully added return true. If not added, because the command
	 * already contains a Grails env parameter, return false;
	 * 
	 * @return true if a Grails environment variable was added as the first
	 *         parameter in a command instance's parameter list. False otherwise
	 */
	protected boolean addGrailsSystemEnv() {
		IFrameworkCommand command = getCommandInstance();

		if (command == null) {
			return false;
		}
		List<ICommandParameter> parameterInstances = command.getParameters();

		ICommandParameter possibleEnvParameter = null;
		if (parameterInstances.size() > 0) {
			// If it has a system env it should be the first parameter
			ICommandParameter potentialSystemEnvParameter = parameterInstances
					.get(0);
			if (GrailsCommandFactory.GRAILS_SYSTEM_ENV_PARAMETER_DESCRIPTOR
					.equals(potentialSystemEnvParameter
							.getParameterDescriptor())) {
				possibleEnvParameter = potentialSystemEnvParameter;
			}
		}

		if (possibleEnvParameter == null) {
			ICommandParameter systemParameter = ParameterFactory
					.getParameterInstance(GrailsCommandFactory.GRAILS_SYSTEM_ENV_PARAMETER_DESCRIPTOR);
			parameterInstances.add(0, systemParameter);
			return true;
		} else {
			return false;
		}

	}

	protected boolean removeGrailsSystemEnv() {
		IFrameworkCommand command = getCommandInstance();

		if (command == null) {
			return false;
		}
		List<ICommandParameter> parameterInstances = command.getParameters();
		if (parameterInstances != null && parameterInstances.size() > 0) {
			// If it has a system env it should be the first parameter
			ICommandParameter potentialSystemEnvParameter = parameterInstances
					.get(0);
			if (GrailsCommandFactory.GRAILS_SYSTEM_ENV_PARAMETER_DESCRIPTOR
					.equals(potentialSystemEnvParameter
							.getParameterDescriptor())) {
				parameterInstances.remove(0);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean canFinish() {
		return getSelectedProject()!=null && super.canFinish();
	}

	@Override
	protected void executeCommand(final IFrameworkCommand command) {
		final IProject project = getSelectedProject();
		if (project != null) {
			if (command != null) {
				Job worker = new Job(command.getCommandDescriptor().getName()) {
					protected IStatus run(IProgressMonitor monitor) {
						GrailsCommandAdapter newRsrcListener = null;
						try {
							GrailsCommand grailsCommand = GrailsCommandFactory
							.getExecutableCommand(command, project);
							newRsrcListener = getNewResourceListener(project);
							GrailsCoreActivator.getDefault().addGrailsCommandResourceListener(newRsrcListener);

							grailsCommand.synchExec();
							/**
							 * FIXADE don't always refresh everything.
							 * this should be put into a post op and maybe be able to 
							 * refresh only a selected set of resources
							 */
							project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
						} catch (CoreException e) {
							return e.getStatus();
						} finally {
							if (newRsrcListener != null) {
								newRsrcListener.finish();
								GrailsCoreActivator.getDefault()
								.removeGrailsCommandResourceListener(
										newRsrcListener);
							}
						}
						return Status.OK_STATUS;
					}
				};
				worker.setPriority(Job.INTERACTIVE);
				worker.schedule();
			}
		}
	}

	@Override
	protected ProjectFilter getProjectFilter() {
		// TODO Auto-generated method stub
		return super.getProjectFilter();
	}

	/**
	 * The OpenNewResourceCommandListener determines which new resources will automatically open after
	 * the command completes. The default will open any resource that is in a source folder, except
	 * for resources who's name ends with 'Tests.groovy'.
	 * <p>
	 * Subclass may override to change this behavior (e.g. for create-unit-test it is desirable that the
	 * created test resource be opened.
	 */
	protected GrailsCommandAdapter getNewResourceListener(
			final IProject project) {
		return new OpenNewResourcesCommandListener(
				project);
	}
	
	protected void customizeCommand(IStructuredSelection selection) {
		IFrameworkCommand command = getCommandInstance();
		if (command!=null) {
			IFrameworkCommandDescriptor desc = command.getCommandDescriptor();
			ICommandParameterDescriptor[] paramDescs = desc.getParameters();
			List<ICommandParameter> params = command.getParameters();
			for (int i = 0; i < paramDescs.length; i++) {
				ParameterKind paramType = paramDescs[i].getParameterKind();
				ICommandParameter param = params.get(i);
				if (paramType==ParameterKind.JAVA_TYPE) {
					IType type = SelectionUtils.getType(selection);
					if (type!=null) {
						param.setValue(type.getFullyQualifiedName());
					} else {
						//Try to get a package at least
						IPackageFragment pkg = SelectionUtils.getPackage(selection);
						if (pkg!=null) {
							param.setValue(pkg.getElementName());
						}
					}
				}
			}
		}
	}

}
