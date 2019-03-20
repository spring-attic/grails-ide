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
package org.grails.ide.eclipse.ui.internal.launch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.EnvironmentTab;

/**
 * @author Christian Dupuis
 * @since 2.2.7
 */
public class GrailsEnvironmentTab extends EnvironmentTab {
	
	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		super.performApply(configuration);
		if (appendEnvironment.getSelection()) {
			configuration.setAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
		} else {
			configuration.removeAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES);
		}
		//It doesn't look like we should do this, it causes spurious 'save requests' because JDT erases the attribute if the
		//map is empty, making this look like an 'edit' when it is not.
//		try {
//			if (configuration.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, (Map<String, String>) null) == null) {
//				configuration.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, new HashMap<String, String>());
//			}
//		}
//		catch (CoreException e) {
//		}
	}
		
}
