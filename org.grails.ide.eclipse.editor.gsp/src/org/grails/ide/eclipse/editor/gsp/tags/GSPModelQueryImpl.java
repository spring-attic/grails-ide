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
package org.grails.ide.eclipse.editor.gsp.tags;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jst.jsp.core.internal.modelquery.JSPModelQueryImpl;
import org.eclipse.wst.common.uriresolver.internal.provisional.URIResolver;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQueryAction;
import org.eclipse.wst.xml.core.internal.contentmodel.modelqueryimpl.SimpleAssociationProvider;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;


/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Nov 27, 2009
 */
public class GSPModelQueryImpl extends JSPModelQueryImpl {
    
    class DelegatingModelQueryAction implements ModelQueryAction {
        private final ModelQueryAction delegate;
        private final AbstractGSPTag tag;
        

        DelegatingModelQueryAction(ModelQueryAction delegate,
                AbstractGSPTag tag) {
            super();
            this.delegate = delegate;
            this.tag = new DelegatingGSPTag(tag);
        }

        public int getKind() {
            return delegate.getKind();
        }

        public int getStartIndex() {
            return delegate.getStartIndex();
        }

        public int getEndIndex() {
            return delegate.getEndIndex();
        }

        public Node getParent() {
            return delegate.getParent();
        }

        public CMNode getCMNode() {
            return tag;
        }

        public Object getUserData() {
            return delegate.getUserData();
        }

        public void setUserData(Object object) {
            delegate.setUserData(object);
        }
        
    }

    private final GSPModelQueryCMProvider modelQueryCMProvider;

    public GSPModelQueryImpl(IStructuredModel model, URIResolver resolver) {
        super(model, resolver);
        modelQueryCMProvider = new GSPModelQueryCMProvider(model);
        modelQueryAssociationProvider = new SimpleAssociationProvider(modelQueryCMProvider);
    }
    
    @Override
    public CMDocument getCorrespondingCMDocument(Node node) {
        CMDocument doc = modelQueryCMProvider.getDocumentForTagName(node.getNodeName());
        if (doc != null) {
            return doc;
        }
        return super.getCorrespondingCMDocument(node);
    }

    
    public List<CMNode> getAvailableContent(Element element, CMElementDeclaration ed, int includeOptions) {
        List<CMNode> content = super.getAvailableContent(element, ed, includeOptions);
        if ((includeOptions & INCLUDE_CHILD_NODES) != 0) {
            List<GSPTagLibDocument> tagLibDocs = modelQueryCMProvider.getGroovyAllTagLibs();
            if (tagLibDocs != null) {
                for (GSPTagLibDocument tagLibDoc : tagLibDocs) {
                    for (Iterator<CMNode> iter = tagLibDoc.getElements().iterator(); iter.hasNext(); ) {
                        CMNode next = iter.next();
                        content.add(next);
                    }
                }
            }
        }
        return content;
    }
    
    /**
     * Ensure to wrap each proposal name with its namespace if we are not in the top of the document
     */
    public void getInsertActions(Element parent, CMElementDeclaration ed, int index, int includeOptions, int validityChecking, List actionList) {
        super.getInsertActions(parent, ed, index, includeOptions, validityChecking, actionList);
        if (parent.getNodeType() != Node.DOCUMENT_NODE) {
            // iterate through each node and replace with a delegate if it is a GSPTag
            List<ModelQueryAction> newActionList = new ArrayList<ModelQueryAction>(actionList.size());
            for (ModelQueryAction action : (Iterable<ModelQueryAction>) actionList) {
                CMNode cmNode = action.getCMNode();
                if (cmNode instanceof AbstractGSPTag) {
                    newActionList.add(new DelegatingModelQueryAction(action, (AbstractGSPTag) cmNode));
                } else {
                    newActionList.add(action);
                }
            }
            actionList.clear();
            actionList.addAll(newActionList);
        }
    }
}
