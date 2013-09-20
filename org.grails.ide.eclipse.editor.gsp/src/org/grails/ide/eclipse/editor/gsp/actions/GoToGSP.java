/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.editor.gsp.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.window.Window;

import org.grails.ide.eclipse.editor.actions.AbstractGotoClass;
import org.grails.ide.eclipse.editor.groovy.elements.GSPArtifact;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.editor.groovy.elements.IGrailsArtifact;
import org.grails.ide.eclipse.editor.groovy.elements.INavigableGrailsElement;

/**
 * @author Andrew Eisenberg
 */
public class GoToGSP extends AbstractGotoClass {

    @Override
    protected IGrailsArtifact navigateTo(INavigableGrailsElement elt) {
        GoToGSPPopDialog dialog = new GoToGSPPopDialog(editor.getEditorSite().getShell(), elt);
        if (dialog.open() == Window.OK) {
            if (dialog.getResult() != null) {
                IFile file = dialog.getResult();
                if (file != null && file.isAccessible()) {
                    return new GSPArtifact(file);
                }
            }
        }
        
        return null;
    }

    @Override
    protected boolean hasRelated() {
        return GrailsWorkspaceCore.hasRelatedGSP(unit);
    }

    @Override
    protected String errorMessage() {
        return "No GSP selected";
    }

    @Override
    public String getCommandName() {
        return null;
    }
}
