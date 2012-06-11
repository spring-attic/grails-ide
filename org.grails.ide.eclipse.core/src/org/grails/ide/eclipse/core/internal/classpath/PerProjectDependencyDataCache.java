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
package org.grails.ide.eclipse.core.internal.classpath;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.core.internal.plugins.IGrailsProjectInfo;


/**
 * @author Andrew Eisenberg
 * @created Sep 28, 2010
 */
public class PerProjectDependencyDataCache implements IGrailsProjectInfo {

    private IProject project;
    
    private DependencyData data;
    
    public IProject getProject() {
        return project;
    }

    public void setProject(IProject project) {
        this.project = project;
    }

    public void projectChanged(GrailsElementKind[] changeKinds,
            IResourceDelta change) {
        // don't care
    }

    public void dispose() {
        data = null;
        GrailsDependencyParser.forProject(project).deleteDataFile();
    }

    public void refreshData() {
        data = null;
    }
    
    public DependencyData getData() {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            if (data == null) {
                data = GrailsDependencyParser.forProject(project).parse();
            }
            return data;
        }
    }

	public static DependencyData get(IProject project) {
		PerProjectDependencyDataCache cache = GrailsCore.get().connect(project, PerProjectDependencyDataCache.class);
		if (cache!=null) {
			return cache.getData();
		}
		return null;
	}
}
