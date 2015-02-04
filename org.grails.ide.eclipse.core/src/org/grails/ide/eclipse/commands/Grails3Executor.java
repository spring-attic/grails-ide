/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.commands;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.grails.ide.eclipse.core.launch.grails3.Grails3CommandLaunchConfigurationDelegate;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;

public class Grails3Executor extends GrailsExecutor {

	private GrailsVersion version;

	public Grails3Executor(GrailsVersion version) {
		this.version = version;
	}

	@Override
	protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(
			GrailsCommand cmd, IGrailsInstall grailsHome) throws CoreException,
			IOException {
		return Grails3CommandLaunchConfigurationDelegate.getLaunchConfiguration(grailsHome, cmd.getProject(), cmd.getCommand(), cmd.getPath());
	}


}
