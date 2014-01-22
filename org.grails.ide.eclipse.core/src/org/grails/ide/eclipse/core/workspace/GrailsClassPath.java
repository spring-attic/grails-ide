/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.workspace;

import static org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathContainer.isGrailsClasspathContainer;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathContainer;


/**
 * Grails centric view on an Eclipse jdt set of raw classpath entries. This class makes sure that
 * the typical entries found on a Grails project's class path are kept in the desired order.
 * 
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class GrailsClassPath {
	
	private LinkedHashSet<IClasspathEntry> entries;
	private IClasspathEntry grailsClasspathContainer = null; //This entry allways comes last.
	
	/**
	 * Extracts relevant info from a given IJavaProject, creating a 'snapshot' of its current classpath.
	 */
	GrailsClassPath(GrailsProject project) throws JavaModelException {
		IJavaProject javaProject = project.getJavaProject();
		IClasspathEntry[] rawEntries = javaProject.getRawClasspath();
		entries = new LinkedHashSet<IClasspathEntry>(rawEntries.length);
		for (IClasspathEntry entry : rawEntries) {
			if (isGrailsClasspathContainer(entry)) {
				grailsClasspathContainer = entry;
			} else {
				entries.add(entry);
			}
		}
	}

	/**
	 * Creates an empty GrailsClassPath containing no entries.
	 */
	public GrailsClassPath() {
		entries = new LinkedHashSet<IClasspathEntry>();
	}

	/** 
	 * convert into an array of {@link IClasspathEntry} acceptable to setRawClasspath method of IJavaProject.
	 * <p>
	 * Note package private: use {@link GrailsProject}.setClasspath instead.
	 */
	IClasspathEntry[] toArray() {
		int size = entries.size();
		if (grailsClasspathContainer!=null) {
			size++;
		}
		
		IClasspathEntry[] result = new IClasspathEntry[size];
		int i = 0;
		for (IClasspathEntry e : entries) {
			result[i++] = e;
		}
		if (grailsClasspathContainer!=null) { 
			result[size-1] = grailsClasspathContainer;
		}
		return result;
	}

	/**
	 * Adds a new entry. If an equal entry is already present, this method has no effect.
	 */
	public void add(IClasspathEntry newEntry) {
		if (isGrailsClasspathContainer(newEntry)) {
			//Special treatment for grails container entries
			Assert.isLegal(grailsClasspathContainer==null || newEntry.equals(grailsClasspathContainer));
			grailsClasspathContainer = newEntry;
		} else {
			//Adding some other type of entry
			entries.add(newEntry);
		}
	}

	public void removeAll(List<IClasspathEntry> toRemove) {
		for (IClasspathEntry e : toRemove) {
			remove(e);
		}
	}

	public void remove(IClasspathEntry e) {
		if (e.equals(grailsClasspathContainer)) {
			grailsClasspathContainer = null;
		} else {
			entries.remove(e);
		}
	}

	public void addAll(List<IClasspathEntry> newEntries) {
		for (IClasspathEntry e : newEntries) {
			add(e);
		}
	}

	public void removeGrailsClasspathContainer() {
		grailsClasspathContainer = null;
	}

	public void removePluginSourceFolders() {
		Iterator<IClasspathEntry> iter = entries.iterator();
		while (iter.hasNext()) {
			IClasspathEntry existingEntry = iter.next();
			if (isPluginSourceFolder(existingEntry)) {
				iter.remove();
			}
		}
	}

	public static boolean isPluginSourceFolder(IClasspathEntry e) {
		if (e.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
			for (IClasspathAttribute attr : e.getExtraAttributes()) {
				if (attr.getName().equals(GrailsClasspathContainer.PLUGIN_SOURCEFOLDER_ATTRIBUTE_NAME)) {
					return true;
				}
			}
		}
		return false;
	}

}
