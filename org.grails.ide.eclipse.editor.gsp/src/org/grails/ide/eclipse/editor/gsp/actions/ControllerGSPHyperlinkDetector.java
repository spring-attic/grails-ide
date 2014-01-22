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
package org.grails.ide.eclipse.editor.gsp.actions;

import java.util.List;

import org.codehaus.groovy.eclipse.editor.GroovyEditor;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.ITextEditor;
import org.grails.ide.eclipse.core.GrailsCoreActivator;

import org.grails.ide.eclipse.editor.actions.WorkspaceFileHyperlink;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;

/**
 * @author Andrew Eisenberg
 */
public class ControllerGSPHyperlinkDetector extends AbstractHyperlinkDetector {

    public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
            IRegion region, boolean canShowMultipleHyperlinks) {
        ITextEditor textEditor= (ITextEditor)getAdapter(ITextEditor.class);
        if (region == null || !(textEditor instanceof GroovyEditor)) {
            return null;
        }
        GroovyEditor gEditor = (GroovyEditor) textEditor;
        GroovyCompilationUnit unit = gEditor.getGroovyCompilationUnit();
        if (unit == null) {
            return null;
        }
        
        GrailsProject grailsProject = GrailsWorkspaceCore.get().getGrailsProjectFor(unit);
        if (grailsProject == null) {
            return null;
        }
        if (grailsProject.getControllerClass(unit) == null) {
            return null;
        }
        
        try {
            return findAllGSPLinksForType(unit);
        } catch (CoreException e) {
            GrailsCoreActivator.log(e);
        }
        return null;
    }

    private IHyperlink[] findAllGSPLinksForType(GroovyCompilationUnit unit) throws CoreException {
        IFolder folder = findGSPFolder(unit);
        if (folder != null) {
            List<IFile> files = NavigationUtils.findGSPsInFolder(folder);
            IHyperlink[] links = new IHyperlink[files.size()];
            for (int i = 0; i < links.length; i++) {
                links[i] = new WorkspaceFileHyperlink(new Region(0,0), files.get(i));
            }
            return links.length > 0 ? links : null;
        } else {
            return null;
        }
    }
    
    private IFolder findGSPFolder(GroovyCompilationUnit unit) {
        // project name/grails-app/views/domainClassName/elementName.gsp
        StringBuilder sb = new StringBuilder();
        sb.append(unit.getJavaProject().getElementName()).append("/grails-app/views/");
        sb.append(gspFolderName(unit));
        IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path(sb.toString()));
        return folder.isAccessible() ? folder : null;
    }

    private String gspFolderName(GroovyCompilationUnit unit) {
        String name = unit.getElementName();
        int dotIndex = name.indexOf("Controller");
        if (dotIndex > 0) {
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1, dotIndex);
            return name;
        } else {
            return name;
        }
    }
}
