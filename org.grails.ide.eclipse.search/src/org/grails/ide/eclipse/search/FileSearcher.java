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
package org.grails.ide.eclipse.search;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

/**
 * Abstract base class for search algorithms that find matches inside of an {@link IFile}.
 * 
 * @author Kris De Volder
 *
 * @since 2.9
 */
public abstract class FileSearcher {
	
	/**
	 * The file in which to search.
	 */
	protected IFile file;

	public FileSearcher(IFile file) {
		this.file = file;
	}

	public abstract void perform() throws IOException, CoreException;
		
}
