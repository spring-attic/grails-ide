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
package org.eclipse.jdt.debug.testplugin;

import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.IValueVariableInitializer;

/**
 * ValueVariableInitializer
 * @author Andrew Eisenberg
 */
public class ValueVariableInitializer implements IValueVariableInitializer {

	/**
	 * @see org.eclipse.core.variables.IValueVariableInitializer#initialize(org.eclipse.core.variables.IValueVariable)
	 */
	public void initialize(IValueVariable variable) {
		variable.setValue("initial-value");
	}
}
