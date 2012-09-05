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
package org.grails.ide.eclipse.core.internal.classpath;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.runtime.shared.DependencyData;
import org.grails.ide.eclipse.runtime.shared.DependencyFileFormat;

/**
 * @author Nieraj Singh
 * @author Kris De Volder
 */
public class GrailsDependencyParser {
	
	private static final boolean DEBUG = false;
	
	public static GrailsDependencyParser forProject(IProject project) {
		return new GrailsDependencyParser(project);
	}

	private IProject project;
	private File file;

	private GrailsDependencyParser(IProject project) {
		this.project = project;
	}
	
	/**
	 * Returns non-null data, or throws exception if data building failed.
	 * 
	 * @return non-null data, or throws exception if data building failed.
	 */
	public DependencyData parse() {

		if (project == null && file == null) {
			return null;
		}

		if (project!=null) {
			file = GrailsClasspathUtils.getDependencyDescriptor(project);
		}
		
		if (file == null) {
			return null;
		}
		
		try {
			DependencyData data = DependencyFileFormat.read(file);
			debug(data);
			return data;
		} catch (IOException e) {
			GrailsCoreActivator.log("Error reading dependency file "+file, e);
			return null;
		}
	}

	private void debug(DependencyData data) {
		if (DEBUG) {
			System.out.println(">>> DependcyDataParser for "+project);
			for (String source : data.getSources()) {
				System.out.println(source);
			}
			System.out.println("<<< DependcyDataParser for "+project);
		}
	}

	public void deleteDataFile() {
		if (file==null) {
			file = GrailsClasspathUtils.getDependencyDescriptor(project);
		}
		if (file!=null && file.exists()) {
			file.delete();
		}
	}

}
