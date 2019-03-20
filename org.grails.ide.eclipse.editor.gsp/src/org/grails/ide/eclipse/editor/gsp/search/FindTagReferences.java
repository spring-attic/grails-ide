/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.editor.gsp.search;

import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.document.ElementImpl;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.editor.gsp.model.GSPStructuredModel;
import org.grails.ide.eclipse.editor.gsp.tags.AbstractGSPTag;

/**
 * This class finds all references to a tag in the workspace
 * @author Andrew Eisenberg
 * @since 2.7.0
 */
class FindTagReferences {
    
    /** can be a field or a local variable */
    private IJavaElement tagField;
    private String tagName;
    
    /**
     * only check for tags if the java element states that 
     * is a field in a taglib
     * @param elt
     * @return the tag name if the specification corresponds to a tag, or else null
     */
    boolean shouldSearchForTagRefs(IJavaElement elt) {
        // strangely, if the referenced tag is from a class file, then the type is ILocalVariable
        if (elt.getElementType() == IJavaElement.LOCAL_VARIABLE) {
            elt = elt.getParent();
        }
        if (elt.getElementType() == IJavaElement.FIELD) {
            ICompilationUnit unit = (ICompilationUnit) elt.getAncestor(IJavaElement.COMPILATION_UNIT);
            if (unit instanceof GroovyCompilationUnit) {
                if (GrailsWorkspaceCore.isTagLibClass((GroovyCompilationUnit) unit)) {
                    tagField = elt;
                    tagName = tagField.getElementName();
                }
            } else {
                // could be a built in tag
                IType type = (IType) elt.getAncestor(IJavaElement.TYPE);
                if (type != null && type.isReadOnly() && type.getElementName().endsWith("TagLib")) {
                    IPackageFragment frag = (IPackageFragment) elt.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
                    if (frag.getElementName().equals("org.codehaus.groovy.grails.plugins.web.taglib")) {
                        tagField = elt;
                        tagName = tagField.getElementName();
                    }
                }
            }
        }
        return tagField != null;
    }
    
    void findTags(IStructuredModel model, IFile file, IGSPSearchRequestor requestor) {
        if (model instanceof GSPStructuredModel) {
            GSPStructuredModel gspModel = (GSPStructuredModel) model;
            IDOMDocument dom = gspModel.getDocument();
            visitDOMNode(dom, file, requestor);
        }
    }


    /**
     * @param domNode
     * @param matches
     */
    private void visitDOMNode(Node domNode, IFile file, IGSPSearchRequestor requestor) {
        if (domNode instanceof ElementImpl) {
            CMElementDeclaration decl = getDeclaration((Element) domNode);
            if (decl instanceof AbstractGSPTag) {
                if (matchesTag((AbstractGSPTag) decl)) {
                    ElementImpl impl = (ElementImpl) domNode;
                    IStructuredDocumentRegion region = impl.getStartStructuredDocumentRegion();
                    if (region.getNumberOfRegions() > 2 && region.getRegions().get(1).getType() == DOMRegionContext.XML_TAG_NAME) {
                        ITextRegion nameRegion = region.getRegions().get(1);
                        // since the name may include a prefix, start from the end of the name region and work back
                        requestor.acceptMatch(file, region.getStart() + nameRegion.getTextEnd() - tagName.length(), tagName.length());
                    }
                    // now find the end region
                    IStructuredDocumentRegion lastRegion = impl.getEndStructuredDocumentRegion();
                    if (lastRegion != null && region != lastRegion && region.getNumberOfRegions() > 2 && lastRegion.getRegions().get(1).getType() == DOMRegionContext.XML_TAG_NAME) {
                        ITextRegion nameRegion = lastRegion.getRegions().get(1);
                        // since the name may include a prefix, start from the end of the name region and work back
                        requestor.acceptMatch(file, lastRegion.getStart() + nameRegion.getTextEnd() - tagName.length(), tagName.length());
                    }
                }
            }
        }
        NodeList children = domNode.getChildNodes();
        if (children != null) {
            int length = children.getLength();
            for (int i = 0; i < length; i++) {
                visitDOMNode(children.item(i), file, requestor);
            }
        }
    }

    /**
     * @param decl
     * @return
     */
    private boolean matchesTag(AbstractGSPTag decl) {
        return decl.getTagDefinitionHandle() != null && decl.getTagDefinitionHandle().equals(tagField.getHandleIdentifier());
    }

    public CMElementDeclaration getDeclaration(Element target) {
        Document doc = target.getOwnerDocument();
        ModelQuery query = ModelQueryUtil.getModelQuery(doc);
        return query.getCMElementDeclaration(target);
    }

}
