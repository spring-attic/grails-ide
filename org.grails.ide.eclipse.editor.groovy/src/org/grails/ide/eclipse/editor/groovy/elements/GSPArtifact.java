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
package org.grails.ide.eclipse.editor.groovy.elements;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;


/**
 * @author Andrew Eisenberg
 * @created Dec 3, 2010
 */
public class GSPArtifact implements IGrailsArtifact {
    
    private final IFile gspFile;
    
    public GSPArtifact(IFile gspFile) {
        this.gspFile = gspFile;
    }

    public GrailsElementKind getKind() {
        return GrailsElementKind.GSP;
    }

    public IResource getResource() {
        return gspFile;
    }

}
