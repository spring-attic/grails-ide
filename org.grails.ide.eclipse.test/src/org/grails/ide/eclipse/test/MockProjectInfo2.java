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
package org.grails.ide.eclipse.test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.core.internal.plugins.IGrailsProjectInfo;


/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 */
public class MockProjectInfo2 implements IGrailsProjectInfo {

    public void dispose() {
        
    }

    public IProject getProject() {
        return null;
    }

    public void projectChanged(GrailsElementKind[] changeKinds,
            IResourceDelta change) {
        
    }

    public void setProject(IProject project) {
        
    }
    
}
