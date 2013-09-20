/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.commands;

import java.util.ArrayList;
import java.util.List;

import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.CompositeParameterDescriptor;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ICommandParameterDescriptor;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ParameterFactory;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.SystemPropertyParameterDescriptor;


/**
 * @author Nieraj Singh
 */
public class GrailsEnvParameterDescriptor extends CompositeParameterDescriptor {

	public static final String PARAM_NAME = "grails.env";
	public static final String DESCRIPTION = "Grails environment system property";

	public GrailsEnvParameterDescriptor() {

		super(PARAM_NAME, DESCRIPTION, false, null, false, null, null,
				createEnvParameterDescriptor());
	}

	private static List<ICommandParameterDescriptor> createEnvParameterDescriptor() {
		List<ICommandParameterDescriptor> descriptor = new ArrayList<ICommandParameterDescriptor>();

		descriptor.add(new SystemPropertyParameterDescriptor(PARAM_NAME,
				DESCRIPTION, false, null));

		descriptor.add(ParameterFactory.createComboParameterDescriptor(
				PARAM_NAME, DESCRIPTION, false, null, false, null, null,
				new String[] { "prod", "test", "dev" }));

		return descriptor;

	}
}
