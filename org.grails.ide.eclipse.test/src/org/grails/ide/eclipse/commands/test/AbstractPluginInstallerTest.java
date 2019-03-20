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
package org.grails.ide.eclipse.commands.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.grails.ide.eclipse.core.internal.classpath.GrailsPlugin;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginsListManager;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion;


/**
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class AbstractPluginInstallerTest extends AbstractCommandTest {

	public static Collection<PluginVersion> getLatestPluginVersions(GrailsPluginsListManager man, String ... names) {
		Collection<GrailsPlugin> fromList = man.generateList(false);
	    Collection<PluginVersion> plugins = new ArrayList<PluginVersion>(names.length);
	    for (String name : names) {
	        plugins.add(getLatestPluginVersion(fromList, name));
	    }
	    return plugins;
	}

	public static PluginVersion getLatestPluginVersion(Collection<GrailsPlugin> fromList, String name) {
	    for (GrailsPlugin gp : fromList) {
	        if (gp.getName().equals(name)) {
	            return gp.getLatestReleasedVersion();
	        }
	    }
	    fail("Could not find plugin " + name);
	    // won't get here
	    return null;
	}

	public static void assertPluginsInstalled(GrailsPluginsListManager manager, String ... names) {
	   Collection<GrailsPlugin> installed = manager.getDependenciesAsPluginModels();
	   assertEquals("Wrong number of plugins installed.  Expecting:\n" + Arrays.toString(names) + "\nbut found:\n" + installed,
	           names.length, installed.size());
	   for (String name : names) {
	       if (! containsPlugin(installed, name)) {
	           fail("Could not find plugin " + name + " in installed plugins:\n" + installed);
	       }
	   }
	}

	protected static boolean containsPlugin(Collection<GrailsPlugin> installed, String name) {
	    for (GrailsPlugin plugin : installed) {
	        if (plugin.getName().equals(name)) {
	            return true;
	        }
	    }
	    return false;
	}

}
