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
package org.grails.ide.eclipse.editor.gsp.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.jsp.core.internal.Logger;
import org.eclipse.jst.jsp.core.internal.document.PageDirectiveAdapter;
import org.eclipse.jst.jsp.core.internal.domdocument.DOMModelForJSP;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.events.RegionsReplacedEvent;
import org.eclipse.wst.sse.core.internal.provisional.events.StructuredDocumentRegionsReplacedEvent;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;

import org.grails.ide.eclipse.editor.gsp.tags.PerProjectTagProvider;
import org.grails.ide.eclipse.editor.gsp.translation.GSPTranslationAdapterFactory;

/**
 * Make {@link GSPTranslation} create .groovy compilation units instead of .java.
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Nov 6, 2009
 */
public class GSPStructuredModel extends DOMModelForJSP implements IStructuredModel {

    
    public GSPStructuredModel() {
        if (getFactoryRegistry().getFactoryFor(PageDirectiveAdapter.class) == null) {
            getFactoryRegistry().addFactory(new GSPTranslationAdapterFactory());
        }
    }
    
    
    public IProject getProject() {
        IProject project = null;
        try {
            String baseLocation = this.getBaseLocation();
            if (baseLocation == null) {
                return null;
            }
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IPath filePath = new Path(baseLocation);
            if (filePath.segmentCount() > 0) {
                project = root.getProject(filePath.segment(0));
            }
        } catch (Exception ex) {
            if (this != null) {
                Logger.logException(
                        "(GSPModelQueryCMProvider) problem getting java project from the XMLModel's baseLocation > " + this.getBaseLocation(), ex); //$NON-NLS-1$
            } else {
                Logger.logException(
                        "(GSPModelQueryCMProvider) problem getting java project because model is null", ex); //$NON-NLS-1$
            }
        } 
        return project;
    }
    
    @Override
    public void nodesReplaced(StructuredDocumentRegionsReplacedEvent event) {
        super.nodesReplaced(event);
        ensureTagTrackersReinitialized();
    }

    @Override
    public void regionsReplaced(RegionsReplacedEvent event) {
        super.regionsReplaced(event);
        ensureTagTrackersReinitialized();
    }

    private void ensureTagTrackersReinitialized() {
        // also ensure that the taglib trackers are properly updated
        IProject project = getProject();
        if (project != null && GrailsNature.isGrailsProject(project)) {
            PerProjectTagProvider tagProvider = GrailsCore.get().getInfo(project, PerProjectTagProvider.class);
            if (tagProvider != null) {
                tagProvider.maybeReinitializeTagTrackers(this);
            }
        }
    }
    
    
    @Override
    public void setStructuredDocument(IStructuredDocument structuredDocument) {
        super.setStructuredDocument(structuredDocument);
        
        // also ensure that TagMarkers are set in the tokenizer
        IProject project = this.getProject();
        if (project != null) {
            PerProjectTagProvider provider = GrailsCore.get().getInfo(project, PerProjectTagProvider.class);
            if (provider != null && provider.isConnected(this)) {
                provider.addTagMarkers(this);
            }
        }
    }
}
