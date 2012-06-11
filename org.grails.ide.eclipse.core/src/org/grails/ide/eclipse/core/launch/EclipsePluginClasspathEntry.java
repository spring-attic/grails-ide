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
package org.grails.ide.eclipse.core.launch;

/**
 * This is based on a copy of org.eclipse.jdt.internal.junit.launcher.JUnitRuntimeClasspathEntry. An instance of
 * this class represents a reference to a plugin in the hosting STS/Eclipse which can be 'localized' by ClasspathLocalizer
 * and added to the classpath of a launch (typically used in the .getClassPath method of a LaunchConfguration delegate
 * to add stuff from the hosting Eclipse instance (e.g. a GrailsBuildListener implementation) to the classpath of an 
 * external process (e.g. a grails command).
 * Most of the code in this class is unmodified from that copied form Eclipse, except for being renamed for clarity (not
 * just used for JUnit related classpath entries).
 * @author Kris De Volder
 */
public class EclipsePluginClasspathEntry {
	
	private final String fPluginId;
	private final String fPluginRelativePath;

	public EclipsePluginClasspathEntry(String pluginId, String jarFile) {
		fPluginId = pluginId;
		fPluginRelativePath = jarFile;
	}
	
	public EclipsePluginClasspathEntry(String pluginId) {
		this(pluginId, null);
	}
	
	public String getPluginId() {
		return fPluginId;
	}

	public String getPluginRelativePath() {
		return fPluginRelativePath;
	}

	public EclipsePluginClasspathEntry developmentModeEntry() {
		return new EclipsePluginClasspathEntry(getPluginId(), "bin"); //$NON-NLS-1$
	}

	public String toString() {
		return "ClasspathEntry(" + fPluginId + "/" + fPluginRelativePath + ")"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof EclipsePluginClasspathEntry))
			return false;
		EclipsePluginClasspathEntry other = (EclipsePluginClasspathEntry) obj;
		return fPluginId.equals(other.getPluginId())
				&& ( (fPluginRelativePath == null && other.getPluginRelativePath() == null)
						|| fPluginRelativePath.equals(other.getPluginRelativePath()) );
	}

	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}
}
