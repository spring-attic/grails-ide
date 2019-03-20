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

import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.core.model.IGrailsInstall;

import org.grails.ide.eclipse.ui.GrailsUiActivator;

/**
 * Based on the NewGrailsProjectWizard
 * @author Andy Clement
 * @author Kris De Volder
 * @author Christian Dupuis
 * @since 2.5.0
 */
public class NewGrailsPluginProjectWizard extends ANewGrailsProjectWizard {

	public NewGrailsPluginProjectWizard() {
		super();
		setWindowTitle("New Grails Plugin Project");
		setDefaultPageImageDescriptor(GrailsUiActivator.getImageDescriptor("icons/full/wizban/grails_wizban.png"));
		setNeedsProgressMonitor(true);
	}

	@Override
	protected GrailsCommand createCommand(IGrailsInstall install,
			String projectName) {
		return GrailsCommandFactory.createPlugin(install, projectName);
	}

	@Override
	protected String getPageTitle() {
		return org.grails.ide.eclipse.ui.internal.wizard.NewGrailsWizardMessages.NewGrailsPluginProjectWizardPageOne_createGrailsProject;
	}

	@Override
	protected String getPageDescription() {
		return org.grails.ide.eclipse.ui.internal.wizard.NewGrailsWizardMessages.NewGrailsPluginProjectWizardPageOne_createGrailsProjectInWorkspace;
	}

    @Override
    protected boolean isPluginWizard() {
        return true;
    }
	
}
