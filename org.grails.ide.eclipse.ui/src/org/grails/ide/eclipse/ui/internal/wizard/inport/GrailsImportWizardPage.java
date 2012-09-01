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
package org.grails.ide.eclipse.ui.internal.wizard.inport;

import org.eclipse.jface.resource.ImageDescriptor;
import org.grails.ide.eclipse.core.wizard.GrailsImportWizardCore;
import org.springsource.ide.eclipse.commons.livexp.ui.WizardPageWithSections;

public abstract class GrailsImportWizardPage extends WizardPageWithSections {
	
	protected GrailsImportWizardPage(GrailsImportWizard owner, String pageName, String title) {
		super(pageName, title, GrailsImportWizard.WIZBAN_IMAGE);
		this.wizard = owner;
		this.model = owner.model;
	}
	public final GrailsImportWizard wizard;
	public final GrailsImportWizardCore model;
	
	

}
