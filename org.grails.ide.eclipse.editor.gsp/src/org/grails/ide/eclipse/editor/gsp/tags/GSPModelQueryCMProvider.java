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
package org.grails.ide.eclipse.editor.gsp.tags;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jst.jsp.core.internal.domdocument.DOMDocumentForJSP;
import org.eclipse.jst.jsp.core.internal.domdocument.ElementImplForJSP;
import org.eclipse.jst.jsp.core.internal.modelquery.JSPModelQueryCMProvider;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.w3c.dom.Node;

import org.grails.ide.eclipse.editor.gsp.model.GSPStructuredModel;

/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Nov 27, 2009
 */
public class GSPModelQueryCMProvider extends JSPModelQueryCMProvider {
    
    private PerProjectTagProvider tagProvider;
    

    public GSPModelQueryCMProvider(IStructuredModel model) {
        super();
        GSPStructuredModel gspModel = model instanceof GSPStructuredModel ? (GSPStructuredModel) model : null;
        if (gspModel != null) {
            IProject project = gspModel.getProject();
            if (project != null) {
                tagProvider = GrailsCore.get().connect(project, PerProjectTagProvider.class);
                // will be null if not a grails project
                if (tagProvider != null) {
                    tagProvider.connect(model);
                }
            }
        }
    }
    
    
    GSPTagLibDocument getDocumentForTagName(String name) {
        return tagProvider != null ? tagProvider.getDocumentForTagName(name) : null;
    }

    public List<GSPTagLibDocument> getGroovyAllTagLibs() {
        if (tagProvider != null) {
            return tagProvider.getAllGroovyTagLibs();
        } else {
            return null;
        }
    }
    
    @Override
    public CMDocument getCorrespondingCMDocument(Node node) {
        Node docNode;
        if (node instanceof DOMDocumentForJSP) {
            docNode = ((DOMDocumentForJSP) node).getDocumentElement();
        } else {
            docNode = node;
        }
        
        CMDocument doc = docNode instanceof ElementImplForJSP && tagProvider != null ? 
                tagProvider.getCorrespondingCMDocument(docNode) : null;
        if (doc != null) return doc;
        
        return docNode != null ? super.getCorrespondingCMDocument(docNode) : null;
    }
}
