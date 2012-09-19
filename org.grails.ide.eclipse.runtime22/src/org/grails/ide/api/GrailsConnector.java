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
package org.grails.ide.api;

import grails.build.logging.GrailsConsole;
import grails.util.BuildSettings;

import java.io.File;

public interface GrailsConnector {

	File getBaseDir();
	BuildSettings getBuildSettings(); //TODO: Shouldn't use BuildSettings directly, need to create an interface and adapter
	int executeCommand(String commandLine, GrailsConsole console); //TODO: Should have a IGrailsConsole argument.

}
