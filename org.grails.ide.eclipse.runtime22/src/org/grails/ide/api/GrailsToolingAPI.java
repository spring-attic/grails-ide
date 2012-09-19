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

import java.io.File;

/**
 * An instance of this class represents a 'connection' to Grails tied to a specific 'working directory'.
 * 
 * @author Kris De Volder
 */
public interface GrailsToolingAPI {
	
	GrailsConnector connect(File baseDir);
	
}
