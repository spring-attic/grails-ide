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
package org.grails.ide.api.impl;

import java.io.File;
import java.util.Map;

import org.grails.ide.api.GrailsConnector;
import org.grails.ide.api.GrailsToolingAPI;

public class GrailsToolingAPIImpl implements GrailsToolingAPI {
	
	public static final GrailsToolingAPI INSTANCE = new GrailsToolingAPIImpl();
	
	/**
	 * Private constructor, class is a singleton!
	 */
	private GrailsToolingAPIImpl() {
	}

	Map<String,String> savedSystemProps = null;

	public GrailsConnector connect(File baseDir) {
		return new GrailsConnectorImpl(baseDir);
	}
}
