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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;

/**
 * Grails has some odd ways of changing the source name after a groovy file is recompiled while 
 * the grails app is running.  Normally, this will prevent source files from being displayed after
 * a hotswap attempt is made.  This {@link ISourceLookupParticipant} attempts to get around that 
 * problem.
 * This lookup participant is only called if previous lookup participants fail
 * @author Andrew Eisenberg
 * @since 2.5.2
 */
public class GrailsSourceLookupParticipant extends JavaSourceLookupParticipant {

    
    /**
     * If we've gotten to this point, then we know that source lookup has so 
     * far failed.  There are two possibilities that I have seen so far:
     * 
     * 1. this is a Domain class that has been reloaded.  For some reason, the .groovy at the end has been replaced with .java
     * 2. this is a Controller class that has been reloaded.  If the original qualified class name was com.foo.BarController,
     *    the source name is now com/foo/com.foo.BarController, but should be com/foo/BarController.groovy
     */
    @Override
    public String getSourceName(Object object) throws CoreException {
        String maybeName = super.getSourceName(object);
        
        if (maybeName.endsWith(".java")) {
            // case 1 above
            maybeName = maybeName.replace(".java", ".groovy");
            
        } else if (!maybeName.endsWith(".groovy")) {
            // case 2 above
            int lastDot = maybeName.lastIndexOf('.');
            if (lastDot > 0) {
                int lastSlash = maybeName.lastIndexOf('/');
                if (lastSlash > 0  && lastSlash < lastDot) {
                    maybeName = maybeName.substring(0, lastSlash+1) + maybeName.substring(lastDot+1) + ".groovy";
                }
            }
        }
        return maybeName;
    }
}
