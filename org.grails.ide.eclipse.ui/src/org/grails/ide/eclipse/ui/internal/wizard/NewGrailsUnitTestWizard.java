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

import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.CommandFactory;


/**
 * @author Kris De Volder
 */
public class NewGrailsUnitTestWizard extends NewGrailsTestWizard {

	public NewGrailsUnitTestWizard() {
		super(CommandFactory.createCommandInstance(GrailsCommandFactory.CREATE_UNIT_TEST));
	}
	
}
