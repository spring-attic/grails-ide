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
package org.grails.ide.eclipse.test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.core.internal.plugins.IGrailsProjectInfo;



/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 */
public class MockProjectInfo implements IGrailsProjectInfo {

    int disposeCount = 0;
    int getProjectCount = 0;
    int setProjectCount = 0;
    int projectChangedCount = 0;
    
    IProject project;
    
    public void dispose() {
        disposeCount++;
    }

    public IProject getProject() {
        getProjectCount++;
        return project;
    }

    public void projectChanged(GrailsElementKind[] changeKinds,
            IResourceDelta change) {
        projectChangedCount++;
    }

    public void setProject(IProject project) {
        setProjectCount++;
        this.project = project;
    }
    
    void reset() {
        disposeCount = 0;
        getProjectCount = 0;
        setProjectCount = 0;
        projectChangedCount = 0;
    }
}
