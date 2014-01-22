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

import org.eclipse.wst.sse.core.internal.provisional.events.StructuredDocumentEvent;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.core.internal.text.TextRegionListImpl;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;

/**
 * @author Andrew Eisenberg
 */
class AllDocumentRegion implements IStructuredDocumentRegion {

    private IStructuredDocument document;
    
    AllDocumentRegion(IStructuredDocument document) {
        this.document = document;
    }

    public void addRegion(ITextRegion aRegion) { }

    public IStructuredDocumentRegion getNext() {
        return null;
    }

    public IStructuredDocument getParentDocument() {
        return this.document;
    }

    public IStructuredDocumentRegion getPrevious() {
        return null;
    }

    public boolean isDeleted() {
        return false;
    }

    public boolean isEnded() {
        return false;
    }

    public boolean sameAs(IStructuredDocumentRegion region, int shift) {
        return region instanceof AllDocumentRegion && region.getParentDocument().equals(this.getParentDocument());
    }

    public boolean sameAs(ITextRegion oldRegion,
            IStructuredDocumentRegion documentRegion,
            ITextRegion newRegion, int shift) {
        return sameAs(documentRegion, shift);
    }

    public void setDeleted(boolean deleted) { }

    public void setEnded(boolean hasEnd) { }

    public void setLength(int newLength) { }

    public void setNext(IStructuredDocumentRegion newNext) { }

    public void setParentDocument(IStructuredDocument document) { }

    public void setPrevious(IStructuredDocumentRegion newPrevious) { }

    public void setStart(int newStart) { }

    public boolean containsOffset(int offset) {
        return this.getParentDocument().getLength() > offset;
    }

    public boolean containsOffset(ITextRegion region, int offset) {
        return containsOffset(offset);
    }

    public int getEndOffset() {
        return this.getParentDocument().getLength();
    }

    public int getEndOffset(ITextRegion containedRegion) {
        return getEndOffset();
    }

    public ITextRegion getFirstRegion() {
        return null;
    }

    public String getFullText() {
        return getParentDocument().get();
    }

    public String getFullText(ITextRegion containedRegion) {
        return getFullText().substring(containedRegion.getStart(), containedRegion.getEnd());
    }

    public ITextRegion getLastRegion() {
        return null;
    }

    public int getNumberOfRegions() {
        return 0;
    }

    public ITextRegion getRegionAtCharacterOffset(int offset) {
        return null;
    }

    public ITextRegionList getRegions() {
        return new TextRegionListImpl();
    }

    public int getStartOffset() {
        return 0;
    }

    public int getStartOffset(ITextRegion containedRegion) {
        return 0;
    }

    public String getText() {
        return getFullText();
    }

    public String getText(ITextRegion containedRegion) {
        return getFullText(containedRegion);
    }

    public int getTextEndOffset() {
        return 0;
    }

    public int getTextEndOffset(ITextRegion containedRegion) {
        return getEndOffset();
    }

    public void setRegions(ITextRegionList containedRegions) { }

    public void adjustLength(int i) { }

    public void adjustStart(int i) { }

    public void adjustTextLength(int i) { }

    public void equatePositions(ITextRegion region) { }

    public int getEnd() {
        return getEndOffset();
    }

    public int getLength() {
        return getEndOffset();
    }

    public int getStart() {
        return 0;
    }

    public int getTextEnd() {
        return getEndOffset();
    }

    public int getTextLength() {
        return getEndOffset();
    }

    public String getType() {
        return DOMRegionContext.XML_CONTENT;
    }

    public StructuredDocumentEvent updateRegion(Object requester,
            IStructuredDocumentRegion parent, String changes,
            int requestStart, int lengthToReplace) {
        return null;
    }
    
}
