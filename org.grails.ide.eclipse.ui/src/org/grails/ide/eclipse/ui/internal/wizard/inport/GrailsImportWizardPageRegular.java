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

import java.util.Arrays;
import java.util.List;

import org.springsource.ide.eclipse.commons.livexp.ui.CommentSection;
import org.springsource.ide.eclipse.commons.livexp.ui.WizardPageSection;
import org.springsource.ide.eclipse.commons.livexp.ui.WizardPageWithSections;

public class GrailsImportWizardPageRegular extends GrailsImportWizardPage {

	public GrailsImportWizardPageRegular(GrailsImportWizard grailsImportWizard) {
		super(grailsImportWizard, "grailsImportWizardPageRegular", "Import Grails Project");
	}
	
	@Override
	protected List<WizardPageSection> createSections() {
		return Arrays.asList(
				(WizardPageSection)new CommentSection(this, "Import as regular project")
		);
	}

}
