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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * @author Kris De Volder
 */
public class ClasspathLocalizer {

	private boolean fInDevelopmentMode;

	public ClasspathLocalizer() {
		fInDevelopmentMode = Platform.inDevelopmentMode();
	}

	public List<String> localizeClasspath(EclipsePluginClasspathEntry... entries) {
		List<String> junitEntries= new ArrayList<String>();

		for (int i= 0; i < entries.length; i++) {
			try {
				addEntry(junitEntries, entries[i]);
			} catch (IOException e) {
				Assert.isTrue(false, entries[i].getPluginId() + " is not available (required JAR)"); //$NON-NLS-1$
			}
		}
		return junitEntries;
	}

	private void addEntry(List<String> junitEntries, final EclipsePluginClasspathEntry entry) throws IOException, MalformedURLException {
		String entryString= entryString(entry);
		if (entryString != null)
			junitEntries.add(entryString);
	}

	private String entryString(final EclipsePluginClasspathEntry entry) throws IOException, MalformedURLException {
		if (inDevelopmentMode()) {
			try {
				return localURL(entry.developmentModeEntry());
			} catch (IOException e3) {
				// fall through and try default
			}
		}
		return localURL(entry);
	}

	private boolean inDevelopmentMode() {
		return fInDevelopmentMode;
	}
	
	private String localURL(EclipsePluginClasspathEntry jar) throws IOException, MalformedURLException {
		Bundle bundle= Platform.getBundle(jar.getPluginId());
		URL url;
		if (jar.getPluginRelativePath() == null)
			url= bundle.getEntry("/"); //$NON-NLS-1$
		else
			url= bundle.getEntry(jar.getPluginRelativePath());
		if (url == null)
			throw new IOException();
		return FileLocator.toFileURL(url).getFile();
	}

}