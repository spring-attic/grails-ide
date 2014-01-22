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
package org.grails.ide.eclipse.core;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.grails.ide.eclipse.core.internal.model.DefaultGrailsInstall;
import org.grails.ide.eclipse.core.model.GrailsInstallManager;
import org.grails.ide.eclipse.core.model.IGrailsInstall;


/**
 * This application takes a grails installation on the file system, 
 * configures it in this STS and makes it the default
 * 
 * @author Andrew Eisenberg
 * @since 2.8.0
 */
public class GrailsInstallerApplication implements IApplication {
    
    private String location;

    public Object start(IApplicationContext context) throws Exception {
        IStatus status = processCommandLine((String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS));
        if (!status.isOK()) {
            System.err.println(status.getMessage());
            return -1;
        }
        
        File grailsHome = new File(location);
        if (!grailsHome.exists()) {
            System.err.println("Grails home does not exist: " + location);
        }
        
        boolean alreadyFound = false;
        GrailsInstallManager manager = GrailsCoreActivator.getDefault().getInstallManager();
        Collection<IGrailsInstall> available = manager.getAllInstalls();
        Set<IGrailsInstall> newInstalls = new LinkedHashSet<IGrailsInstall>();
        for (IGrailsInstall candidate : available) {
            if (candidate.getHome().equals(location)) {
                newInstalls.add(new DefaultGrailsInstall(candidate.getHome(), candidate.getName(), true));
                alreadyFound = true;
            } else {
                newInstalls.add(new DefaultGrailsInstall(candidate.getHome(), candidate.getName(), false));
            }
        }
        if (!alreadyFound) {
            newInstalls.add(new DefaultGrailsInstall(grailsHome.getCanonicalPath(), grailsHome.getName(), true));
        }
        manager.setGrailsInstalls(newInstalls);
        
        if (! manager.getDefaultGrailsInstall().getName().equals(grailsHome.getName())) {
            System.err.println("Grails home not installed as default...odd.");
            return -1;
        }
        
        String versionString = manager.getDefaultGrailsInstall().getVersionString();
        if (versionString.equals("<unknown>")) {
            System.err.println("Grails not installed properly...don't know why.");
            return -1;
        }
        System.out.println("Grails successfully installed! Version: " + versionString);
        return 0;
    }

    /**
     * Searches 
     * @param strings
     */
    private IStatus processCommandLine(String[] strings) {
        for (int i = 0; i < strings.length; i++) {
            if (strings[i].equals("-location") && i < strings.length -1) {
                location = strings[i+1];
                return Status.OK_STATUS;
            }
        }
        
        return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, 
                "This application configures a Grails installation for this instance of STS.\n" +
        		"Invalid arguments.  Expecting: STS -application org.grails.ide.eclipse.core.install -location <FILE_SYSTEM_LOCATION>");
    }

    public void stop() {

    }

}
