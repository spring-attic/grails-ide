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
package org.grails.ide.eclipse.maven;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.configurator.AbstractCustomizableLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

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
        super.configure(request, mon);
    }
}
