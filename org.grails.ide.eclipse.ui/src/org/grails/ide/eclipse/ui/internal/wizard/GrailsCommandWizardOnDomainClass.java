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

import java.util.List;

import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ICommandParameter;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.IFrameworkCommand;

import org.grails.ide.eclipse.ui.internal.utils.GrailsSelectionUtil;

/**
 * Common super class for all the wizards that expect a domain class for a parameter.
 * 
 * @author Kris De Volder
 * @since 2.6
 */
public abstract class GrailsCommandWizardOnDomainClass extends GrailsCommandWizard {
	
	public GrailsCommandWizardOnDomainClass(IFrameworkCommand command) {
		super(command);
	}

	/**
	 * Domain class aware version of customize command.
	 */
	@Override
	protected void customizeCommand(IStructuredSelection selection) {
		IType type = GrailsSelectionUtil.getDomainClass(selection);
		if (type!=null) {
			IFrameworkCommand command = getCommandInstance();
			List<ICommandParameter> params = command.getParameters();
			ICommandParameter theParam = params.get(0);
			theParam.setValue(type.getFullyQualifiedName('.'));
		} else {
			super.customizeCommand(selection);
		}
	}

}
