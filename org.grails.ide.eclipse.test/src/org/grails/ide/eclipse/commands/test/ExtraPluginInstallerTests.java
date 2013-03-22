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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginInstaller;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginsListManager;


/**
 * Similar to the tests in {@link PluginInstallerTests}, but split off from them because
 * they are using more customised test fixtures.
 * 
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class ExtraPluginInstallerTests extends AbstractPluginInstallerTest {

    /**
     * Regression test for STS-1289 Transitive in place plugin dependencies not being properly refreshed.
     */
    public void testSTS1289TransitivePluginDependencies() throws Throwable {
    	IProject main = ensureProject("main");
    	IProject sub = ensureProject("sub", true);
    	IProject subsub = ensureProject("subsub", true);
    	
    	GrailsPluginsListManager mainManager = new GrailsPluginsListManager(main);
    	GrailsPluginsListManager subManager = new GrailsPluginsListManager(sub);
  
    	//Install plugin sub into main
        IStatus status = GrailsPluginInstaller.performPluginChanges(null, getLatestPluginVersions(mainManager, "sub"), main, null);
        assertStatusOK(status);
        assertPluginsInstalled(mainManager, join(defaultPlugins(), "sub"));
    	
        //Install plugin subsub into sub... should also appear in main
        status = GrailsPluginInstaller.performPluginChanges(null, getLatestPluginVersions(subManager, "subsub"), sub, null);
        assertStatusOK(status);
        
        assertPluginsInstalled(subManager, join(defaultPlugins(sub), "subsub"));
        assertPluginsInstalled(mainManager, join(defaultPlugins(), "sub", "subsub"));
        
        //Uninstall plugin subsub from sub... should also disapear from main
        status = GrailsPluginInstaller.performPluginChanges(getLatestPluginVersions(subManager, "subsub"), null, sub, null);
        assertStatusOK(status);
        
        assertPluginsInstalled(subManager, defaultPlugins(sub));
        assertPluginsInstalled(mainManager, join(defaultPlugins(), "sub"));
    }
	
}
