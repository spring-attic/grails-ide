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
//package org.grails.ide.eclipse.ui.internal.wizard.inport;
//
//import java.util.Arrays;
//import java.util.List;
//
//import org.grails.ide.eclipse.ui.internal.wizard.CommentSection;
//import org.springsource.ide.eclipse.commons.livexp.ui.WizardPageSection;
//
//public class GrailsImportWizardPageMaven extends GrailsImportWizardPage {
//
//	public GrailsImportWizardPageMaven(GrailsImportWizard owner) {
//		super(owner, "grailsImportWizardPageMaven", "Import Grails Maven Project");
//	}
//
//	@Override
//	protected List<WizardPageSection> createSections() {
//		return Arrays.asList(
//				(WizardPageSection)new GroupSection(this, "Mavenized Project Warning",
//					new CommentSection(this, 
//							"The project you selected looks like a Mavenized project and should be imported using m2e's " +
//							"'Import Existing Maven Project' wizard. You can proceed to import as a regular Grails project " +
//							"but the project will not be configured correctly"),
//					new IgnoreMavenWarning(this, "I want to proceed anwyay")
//				)
//		);
//	}
//		
//}
