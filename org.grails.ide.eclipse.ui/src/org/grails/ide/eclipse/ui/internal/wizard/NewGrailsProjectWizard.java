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
package org.grails.ide.eclipse.ui.internal.wizard;

import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.core.model.IGrailsInstall;

import org.grails.ide.eclipse.ui.GrailsUiActivator;

/**
 * @author Christian Dupuis
 * @author Kris De Volder
 * @since 2.2.0
 */
public class NewGrailsProjectWizard extends ANewGrailsProjectWizard {

	public NewGrailsProjectWizard() {
		super();
		setWindowTitle("New Grails Project");
		setDefaultPageImageDescriptor(GrailsUiActivator.getImageDescriptor("icons/full/wizban/grails_wizban.png"));
		setNeedsProgressMonitor(true);
	}

	@Override
	protected String getPageTitle() {
		return org.grails.ide.eclipse.ui.internal.wizard.NewGrailsWizardMessages.NewGrailsProjectWizardPageOne_createGrailsProject;
	}

	@Override
	protected String getPageDescription() {
		return org.grails.ide.eclipse.ui.internal.wizard.NewGrailsWizardMessages.NewGrailsProjectWizardPageOne_createGrailsProjectInWorkspace;
	}
	
	@Override
	protected GrailsCommand createCommand(final IGrailsInstall install, String projectName) {
		return GrailsCommandFactory.createApp(install, projectName);
	}
	
    @Override
    protected boolean isPluginWizard() {
        return false;
    }

}
