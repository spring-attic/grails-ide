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
package org.grails.ide.eclipse.editor.gsp.actions;

import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jst.jsp.core.text.IJSPPartitions;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredPartitioning;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.w3c.dom.Node;

import org.grails.ide.eclipse.editor.actions.JavaElementHyperlink;
import org.grails.ide.eclipse.editor.groovy.controllers.ITarget;
import org.grails.ide.eclipse.editor.gsp.controllers.TargetFinder;
import org.grails.ide.eclipse.editor.gsp.model.GSPStructuredModel;
import org.grails.ide.eclipse.editor.gsp.tags.AbstractGSPTag;
import org.grails.ide.eclipse.editor.gsp.tags.PerProjectTagProvider;

/**
 * Navigates to the definition of a tag from a usage inside a GSP
 * @author Andrew Eisenberg
 * @since 2.6.0
 */
public class GSPHyperlinkDetector extends AbstractHyperlinkDetector {
    
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
            IRegion region, boolean canShowMultipleHyperlinks) {
        if (textViewer == null && region == null) {
            return null;
        }
        IDocument doc = textViewer.getDocument();
        if (doc == null) {
            return null;
        }
        int offset = region.getOffset();
        IHyperlink[] hyperlinks = searchForTagLinks(doc, offset);
        if (hyperlinks == null) {
            try {
                hyperlinks = searchForControllerActionLinks(doc, offset);
            } catch (JavaModelException e) {
                GrailsCoreActivator.log("Problem finding hyperlink at region: " + region.toString(), e);
            }
        }
        return hyperlinks;
    }

    /**
     * @param doc
     * @param offset
     * @return
     * @throws JavaModelException 
     */
    private IHyperlink[] searchForControllerActionLinks(IDocument doc,
            int offset) throws JavaModelException {
        if (! (doc instanceof IStructuredDocument)) {
            return null;
        }
         
        TargetFinder finder = new TargetFinder(false);
        List<ITarget> targets = finder.findTargets((IStructuredDocument) doc, offset);
        if (targets.size() > 0) {
            IHyperlink[] hyperlinks = new IHyperlink[targets.size()];
            int i = 0;
            for (ITarget target : targets) {
                hyperlinks[i] = new JavaElementHyperlink(selectWord(doc, offset), target.toJavaElement());
            }
            return hyperlinks;
        }
        return null;
    }

    /**
     * Finds hyperlinks to the definitions of GSP tags
     * @param doc the document
     * @param region the selected region
     * @return the hyperlink to the tag definition (only one link) or null if doesn't exist
     */
    private IHyperlink[] searchForTagLinks(IDocument doc, int offset) {
        IHyperlink[] hyperlinks = null;
        IStructuredModel sModel = null;
        try {
            // check if jsp tag/directive first
            ITypedRegion partition = TextUtilities
                    .getPartition(
                            doc,
                            IStructuredPartitioning.DEFAULT_STRUCTURED_PARTITIONING,
                            offset, false);
            if (partition != null
                    && partition.getType() == IJSPPartitions.JSP_DIRECTIVE) {
                sModel = StructuredModelManager.getModelManager()
                        .getExistingModelForRead(doc);
                // check if jsp taglib directive
                Node currentNode = getCurrentNode(sModel,
                        offset);
                
                if (currentNode != null) {
                    PerProjectTagProvider provider = getTagProvider(sModel);
                    if (provider != null) {
                        AbstractGSPTag tag = provider.getTagForName(currentNode.getNodeName());
                        if (tag != null) {
                            IJavaElement elt = JavaCore.create(tag.getTagDefinitionHandle());
                            if (elt != null) {
                                hyperlinks = new IHyperlink[] { new JavaElementHyperlink(selectWord(doc, offset), elt) };
                            }
                        }
                    }
                }
            }
        } catch (BadLocationException e) {
            GrailsCoreActivator.log(e);
        } finally {
            if (sModel != null) {
                sModel.releaseFromRead();
            }
        }
        return hyperlinks;
    }

    private Node getCurrentNode(IStructuredModel model, int offset) {
        // get the current node at the offset (returns either: element,
        // doctype, text)
        IndexedRegion inode = null;
        if (model != null) {
            inode = model.getIndexedRegion(offset);
            if (inode == null) {
                inode = model.getIndexedRegion(offset - 1);
            }
        }

        if (inode instanceof Node) {
            return (Node) inode;
        }
        return null;
    }

    private PerProjectTagProvider getTagProvider(IStructuredModel sModel) {
        if (sModel instanceof GSPStructuredModel) {
            GSPStructuredModel gModel = (GSPStructuredModel) sModel;
            if (gModel != null && gModel.getProject() != null) {
                return GrailsCore.get().getInfo(gModel.getProject(), PerProjectTagProvider.class);
            }
        }
        return null;
    }
    
    private IRegion selectWord(IDocument document, int anchor) {

        try {
            int offset = anchor;
            char c;

            while (offset >= 0) {
                c = document.getChar(offset);
                if (!Character.isJavaIdentifierPart(c) && c != ':')
                    break;
                --offset;
            }

            int start = offset;

            offset = anchor;
            int length = document.getLength();

            while (offset < length) {
                c = document.getChar(offset);
                if (!Character.isJavaIdentifierPart(c) && c != ':')
                    break;
                ++offset;
            }

            int end = offset;

            if (start == end)
                return new Region(start, 0);

            return new Region(start + 1, end - start - 1);

        }
        catch (BadLocationException x) {
            return null;
        }
    }
}
