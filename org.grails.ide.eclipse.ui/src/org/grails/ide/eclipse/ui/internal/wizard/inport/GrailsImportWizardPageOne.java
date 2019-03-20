/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.ui.internal.wizard.inport;

import java.util.Arrays;
import java.util.List;

import org.springsource.ide.eclipse.commons.livexp.ui.GroupSection;
import org.springsource.ide.eclipse.commons.livexp.ui.WizardPageSection;

/**
 * @author Kris De Volder
 */
public class GrailsImportWizardPageOne extends GrailsImportWizardPage {

	public GrailsImportWizardPageOne(GrailsImportWizard grailsImportWizard) {
		super(grailsImportWizard, "grailsImportWizardPage1", "Import Grails Project");
	}

	@Override
	protected List<WizardPageSection> createSections() {
		return Arrays.asList(
				new RootFolderSection(this),
				new CopyToWorkspaceSection(this),
				new MavenWarningSection(this, "Mavenized Project Warning"),
				new GroupSection(this, "Grails Install",
						new DisplayGrailsVersionSection(this),
						new GrailsInstallSection(this)
				)
		);
	}

//	public IWizardPage getNextPage() {
//		if (model.isMaven()) {
//			return wizard.getMavenPage();
//		} else {
//			return null; //We are the last page since maven page is supposed to be hidden.
//			//return wizard.getRegularPage();
//		}
//	}

}
