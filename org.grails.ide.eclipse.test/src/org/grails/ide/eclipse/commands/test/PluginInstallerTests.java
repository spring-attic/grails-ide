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
package org.grails.ide.eclipse.commands.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPlugin;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginInstaller;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginUtil;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginsListManager;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion;
import org.springsource.ide.eclipse.commons.frameworks.test.util.ACondition;

import org.grails.ide.eclipse.test.util.GrailsTest;

/**
 * @author Andrew Eisenberg
 * @author Kris De Volder
 * @since 2.5.3
 */
public class PluginInstallerTests extends AbstractPluginInstallerTest {
    
    GrailsPluginsListManager manager;
    Collection<GrailsPlugin> plugins;
    private IProject installable;
    private IProject common;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
        installable = ensureProject("installable");
        common = ensureProject("common", true);
        manager = GrailsPluginsListManager.getGrailsPluginsListManager(installable);
        plugins = manager.generateList(false);
    }
    
    public void testInstallExternalPlugin() throws Exception {
        assertPluginsInstalled(GrailsTest.defaultPlugins());
        IStatus status = GrailsPluginInstaller.performPluginChanges(null, getLatestPluginVersions("feeds"), installable, null);
        GrailsTest.assertStatusOK(status);
        assertPluginsInstalled(GrailsTest.join(GrailsTest.defaultPlugins(), "feeds"));
        status = GrailsPluginInstaller.performPluginChanges(getLatestPluginVersions("feeds"), null, installable, null);
        GrailsTest.assertStatusOK(status);
    }
    
    public void testInstallInPlacePlugin() throws Exception {
        assertPluginsInstalled(GrailsTest.defaultPlugins());
        IStatus status = GrailsPluginInstaller.performPluginChanges(null, getLatestPluginVersions("common"), installable, null);
        GrailsTest.assertStatusOK(status);
        assertPluginsInstalled(GrailsTest.join(GrailsTest.defaultPlugins(), "common"));
        status = GrailsPluginInstaller.performPluginChanges(getLatestPluginVersions("common"), null, installable, null);
        GrailsTest.assertStatusOK(status);
        assertPluginsInstalled(GrailsTest.defaultPlugins());
    }
    
    public void testMultiInstallPlugin() throws Exception {
        assertPluginsInstalled(GrailsTest.defaultPlugins());
        IStatus status = GrailsPluginInstaller.performPluginChanges(null, getLatestPluginVersions("common", "feeds"), installable, null);
        GrailsTest.assertStatusOK(status);
        assertPluginsInstalled(GrailsTest.join(GrailsTest.defaultPlugins(), "common", "feeds"));
        status = GrailsPluginInstaller.performPluginChanges(getLatestPluginVersions("common", "feeds"), null, installable, null);
        GrailsTest.assertStatusOK(status);
        assertPluginsInstalled(GrailsTest.defaultPlugins());
    }
    
    private Collection<PluginVersion> getLatestPluginVersions(String... names) {
    	return getLatestPluginVersions(manager, names);
	}

	public void testInstallPluginwithDependentPlugin() throws Exception {
    	//TODO: This test fails in Grails 2.0 because multi-tenant core plugin isn't compatible (yet?) with 2.0.
    	// See http://jira.grails.org/browse/GRAILS-8198
    	if (GrailsVersion.MOST_RECENT.equals(GrailsVersion.V_2_2_1)) {
    		return;
    	}
    	
    	assertPluginsInstalled(GrailsTest.defaultPlugins());
    	IStatus status = GrailsPluginInstaller.performPluginChanges(null, getLatestPluginVersions("multi-tenant-core"), installable, null);
    	GrailsTest.assertStatusOK(status);
    	assertPluginsInstalled(GrailsTest.join(GrailsTest.defaultPlugins(), "multi-tenant-core", "falcone-util"));
    	status = GrailsPluginInstaller.performPluginChanges(getLatestPluginVersions("multi-tenant-core", "falcone-util"), null, installable, null);
    	GrailsTest.assertStatusOK(status);
    	assertPluginsInstalled(GrailsTest.defaultPlugins());
    }
    
    public void testInstallInPlaceWithProblem() throws Exception {
        assertPluginsInstalled(GrailsTest.defaultPlugins());
        // add a mangled version of a dependency to common in the BuildConfig.groovy
        GrailsPluginUtil.addPluginDependency(installable, common, true);
        GrailsCommandUtils.refreshDependencies(
                JavaCore.create(installable), true);
        assertPluginsInstalled(GrailsTest.join(GrailsTest.defaultPlugins(),"common"));

        // should not be able to install common any more
        IStatus status = GrailsPluginInstaller.performPluginChanges(null, getLatestPluginVersions("common", "feeds"), installable, null);
        // no warning since the effect is to another line in the BuildConfig.groovy file
        GrailsTest.assertStatusOK(status);
        // common should already be installed
        assertPluginsInstalled(GrailsTest.join(GrailsTest.defaultPlugins(), "common", "feeds"));
        
        status = GrailsPluginInstaller.performPluginChanges(getLatestPluginVersions("common", "feeds"), null, installable, null);
        // we think we uninstalled because we successfully removed the line in BuildConfig.groovy
        GrailsTest.assertStatusOK(status);
        // common should still be installed
        assertPluginsInstalled(GrailsTest.join(GrailsTest.defaultPlugins(),"common"));

        // try again, but this time get a warning 
        
        // Note: unless using 'FastGrailsPluginInstaller' the feeds plugin will have been uninstalled already.
        status = GrailsPluginInstaller.performPluginChanges(getLatestPluginVersions("common" /*, "feeds"*/), null, installable, null);
        // we think we uninstalled because we successfully removed the line in BuildConfig.groovy
        assertStatusWarning(status);
        // common should still be installed
        assertPluginsInstalled(GrailsTest.join(GrailsTest.defaultPlugins(),"common"));
        
        // now, remove the mangled dependency 
        GrailsPluginUtil.removePluginDependency(installable, common, true);
        GrailsCommandUtils.refreshDependencies(
                JavaCore.create(installable), true);
        assertPluginsInstalled(GrailsTest.defaultPlugins());
    }

    /**
     * Regression test for STS-1530, a bug that happens when one installs the 'export' plugin into Grails 1.3.6.
     * THe export plugin somehow causes our dependency extractor to fail... presumably because the
     * plugin modifies the classpath causing us to have problems using the JAXP libraries inside of Grails.
     */
    public void testSTS_1530() throws Exception {
    	//Note this test is failing but that appears to be not our problem... it also fails when I do the
    	//same steps as outlined in this test on the commandline.
    	//See http://jira.codehaus.org/browse/GRAILSPLUGINS-2944
        assertPluginsInstalled(GrailsTest.defaultPlugins());

        //Install the 'export' plugin
        // final Collection<PluginVersion> latestPluginVersions = getLatestPluginVersions("export"); //TODO: reinstate this when GRAILSPLUGINS-2944 is fixed
        final Collection<PluginVersion> latestPluginVersions = Arrays.asList(getPluginVersion("export", "0.8"));
        IStatus status = GrailsPluginInstaller.performPluginChanges(null, latestPluginVersions, installable, null);
        GrailsTest.assertStatusOK(status);
        assertPluginsInstalled(GrailsTest.join(GrailsTest.defaultPlugins(), "export"));
        
        //Uninstall the 'export' plugin
        status = GrailsPluginInstaller.performPluginChanges(latestPluginVersions, null, installable, null);
        GrailsTest.assertStatusOK(status);
        assertPluginsInstalled(GrailsTest.defaultPlugins());
    }
    
	private void assertPluginsInstalled(String[] plugins) {
		assertPluginsInstalled(manager, plugins);
	}

	public void testInstallExternalWithProblem() throws Exception {
        // not implemented yet
    }
    
    private boolean isPluginInstalled(String name) {
		Collection<GrailsPlugin> installed = manager.getDependenciesAsPluginModels();
		return containsPlugin(installed, name);
	}
    
    private void assertStatusWarning(IStatus status) {
        if (status.getSeverity() != IStatus.WARNING) {
            fail("Expecting Warning Status, but was:\n" + status);
        }
    }
    
    private PluginVersion getPluginVersion(String name, String version) {
        for (GrailsPlugin gp : plugins) {
            if (gp.getName().equals(name)) {
            	return gp.getVersion(version);
            }
        }
        fail("Could not find plugin " + name + " " + version);
        // won't get here
        return null;
	}
	
    /**
     * Regression test for.
     * https://issuetracker.springsource.com/browse/STS-1502
     */
    public void testSTS1502PluginWithJarDependency() throws Exception {
        assertPluginsInstalled(GrailsTest.defaultPlugins());
        IStatus status = GrailsPluginInstaller.performPluginChanges(null, getLatestPluginVersions("spring-security-core"), installable, null);
        GrailsTest.assertStatusOK(status);
        new ACondition() {
			@Override
			public boolean test() throws Exception {
		        IClasspathEntry[] classpath = JavaCore.create(installable).getResolvedClasspath(true);
		        assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, 
		        		Pattern.compile("spring-security-core|org.springframework.security.core"), classpath);
				return true;
			}

		}.waitFor(500000);

		boolean webxml = isPluginInstalled("webxml");
		if (webxml) {
			//in later versions, spring-security-core plugin pulls in the "webxml" plugin?
			status = GrailsPluginInstaller.performPluginChanges(
					getLatestPluginVersions("spring-security-core", "webxml"), null, installable, null);
		} else {
			status = GrailsPluginInstaller.performPluginChanges(
					getLatestPluginVersions("spring-security-core"), null, installable, null);
		}
        GrailsTest.assertStatusOK(status);
        assertPluginsInstalled(GrailsTest.defaultPlugins());
    }
    
}
