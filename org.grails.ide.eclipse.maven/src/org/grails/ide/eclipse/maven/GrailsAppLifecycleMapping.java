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
package org.grails.ide.eclipse.maven;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.configurator.AbstractCustomizableLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.grails.ide.eclipse.core.GrailsCoreActivator;

/**
 * Stub for now
 * 
 * @author Andrew Eisenberg
 * @created Oct 5, 2012
 */
public class GrailsAppLifecycleMapping extends AbstractCustomizableLifecycleMapping 
        implements ILifecycleMapping {

    @Override
    public void configure(ProjectConfigurationRequest request,
            IProgressMonitor mon) throws CoreException {
        try {
            super.configure(request, mon);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Element not found:")) {
                // problem with initial creation of project. OK to ignore
                GrailsCoreActivator.logWarning("Problem importing project.  OK to ignore.", e);
            } else {
                throw e;
            }
        }
    }
}
