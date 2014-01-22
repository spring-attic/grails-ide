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
package org.grails.ide.eclipse.core.internal.plugins;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;


/**
 * An {@link IGrailsProjectInfo} is registered with {@link GrailsCore} on a per
 * project basis.  It will receive notification whenever a Grails element changes
 * (with some caveates, see {@link GrailsCore#notifyGrailsProjectInfos(org.eclipse.core.resources.IProject,IGrailsElement)})
 * <p>
 * {@link IGrailsProjectInfo} classes should not be instantiated directly.  Instead, they should have a 
 * no argument constructor and be created by a call to {@link GrailsCore#connect(IProject,Class)}.
 * <p>
 * Many operations on {@link IGrailsProjectInfo} objects require a synchronized block.  If this is 
 * required, you must grab a lock object from {@link GrailsCore} using the method {@link GrailsCore#getLockForProject(IProject)}.
 * 
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Jan 14, 2010
 */
public interface IGrailsProjectInfo {

    /**
     * Should return the project initially set
     * @return
     */
    public IProject getProject();
    
    /**
     * Called by the framework on initialization
     * @param project
     */
    public void setProject(IProject project);
    
    /**
     * Notification of a particular set of changes that has affected this project.
     * @param changeKinds
     * @param change
     */
    public void projectChanged(GrailsElementKind[] changeKinds, IResourceDelta change);
    
    /**
     * Called when the attached project is close, deleted, or its grails nature is removed.
     * At this point, the project info will no longer receive change events from {@link GrailsWorkspaceCore}.
     */
    public void dispose();
}
