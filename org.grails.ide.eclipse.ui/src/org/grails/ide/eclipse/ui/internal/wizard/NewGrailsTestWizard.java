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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.INewWizard;
import org.grails.ide.eclipse.core.model.GrailsCommandAdapter;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.IFrameworkCommand;

import org.grails.ide.eclipse.ui.internal.launch.OpenInterestingNewResourceListener;

/**
 * @author Kris De Volder
 */
public abstract class NewGrailsTestWizard extends GrailsCommandWizard implements INewWizard {

	public NewGrailsTestWizard(IFrameworkCommand command) {
		super(command);
	}

	@Override
	protected GrailsCommandAdapter getNewResourceListener(
			IProject project) {
		return new OpenInterestingNewResourceListener(project) {
			@Override
			public int howInteresting(IResource resource) {
				String name = resource.getName();
				//Tests are interesting
				if (name.endsWith("Tests.groovy") || name.endsWith("Test.groovy")) {
					return 1;
				}
				//Everything else is not interesting
				return 0;
			}
		};
	}
}
