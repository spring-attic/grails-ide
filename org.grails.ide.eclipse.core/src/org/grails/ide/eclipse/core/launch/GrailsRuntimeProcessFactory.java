/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.launch;

import java.util.Map;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IProcessFactory;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.grails.ide.eclipse.core.GrailsCoreActivator;


public class GrailsRuntimeProcessFactory implements IProcessFactory {
	
	public static final String ID = "org.grails.ide.eclipse.core.processFactory";

	public IProcess newProcess(ILaunch launch, Process process, String label, Map attributes) {
		if (GrailsCoreActivator.getDefault().getCleanOutput()) {
			return new GrailsRuntimeProcess(launch, process, label, attributes);
		} else {
			return new RuntimeProcess(launch, process, label, attributes);
		}
	}

}
