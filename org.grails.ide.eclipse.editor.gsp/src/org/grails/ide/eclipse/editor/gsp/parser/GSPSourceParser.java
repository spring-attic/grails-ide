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
package org.grails.ide.eclipse.editor.gsp.parser;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jst.jsp.core.internal.parser.JSPSourceParser;
import org.eclipse.wst.sse.core.internal.ltk.parser.BlockTokenizer;
import org.eclipse.wst.sse.core.internal.ltk.parser.RegionParser;
import org.eclipse.wst.sse.core.internal.ltk.parser.TagMarker;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;

/**
 * @author Andrew Eisenberg
 * @created Dec 4, 2009
 */
public class GSPSourceParser extends JSPSourceParser {
    
    /**
     * Override so that references are to GSPTokenizer, not JSPTokenizer
     */
    protected class GSPNestablePrefixHandler extends NestablePrefixHandler {
        protected void enableForTaglib(String prefix, IStructuredDocumentRegion anchorFlatNode) {
            if (prefix == null)
                return;
            List<TagMarker> tagmarkers = ((GSPTokenizer) getTokenizer()).getNestablePrefixes();
            for (int i = 0; i < tagmarkers.size(); i++) {
                if (tagmarkers.get(i).getTagName().equals(prefix))
                    return;
            }
            ((GSPTokenizer) getTokenizer()).getNestablePrefixes().add(new TagMarker(prefix, anchorFlatNode));
        }
        
        public void resetNodes() {
            Iterator<TagMarker> tagmarkers = ((GSPTokenizer) getTokenizer()).getNestablePrefixes().iterator();
            while (tagmarkers.hasNext()) {
                if (!tagmarkers.next().isGlobal())
                    tagmarkers.remove();
            }
        }


    }

    /**
     * Override so that references are to GSPTokenizer, not JSPTokenizer
     */
    @SuppressWarnings("deprecation")
    @Override
    protected BlockTokenizer getTokenizer() {
        if (fTokenizer == null) {
            fTokenizer = new GSPTokenizer();
            getStructuredDocumentRegionHandlers().add(new GSPNestablePrefixHandler());
        }
        return fTokenizer;
    }

    /**
     * Override so that references are to GSPTokenizer, not JSPTokenizer
     */
    public void addNestablePrefix(TagMarker marker) {
        ((GSPTokenizer) getTokenizer()).addNestablePrefix(marker);
    }

    public void removeNestablePrefix(TagMarker marker) {
        List<TagMarker> prefixes = getNestablePrefixes();
        for (Iterator<TagMarker> iterator = prefixes.iterator(); iterator.hasNext();) {
            TagMarker prefix = iterator.next();
            if (prefix.getTagName().equals(marker.getTagName())) {
                iterator.remove();
                break;
            }
        }
    }
    
    /**
     * Override so that references are to GSPTokenizer, not JSPTokenizer
     */
    public List<TagMarker> getNestablePrefixes() {
        return ((GSPTokenizer) getTokenizer()).getNestablePrefixes();
    }
    
    /**
     * Override so that references are to GSPTokenizer, not JSPTokenizer
     */
    public void removeNestablePrefix(String tagName) {
        ((GSPTokenizer) getTokenizer()).removeNestablePrefix(tagName);
    }

    /**
     * Override so that references are to GSPTokenizer, not JSPTokenizer
     */
    public RegionParser newInstance() {
        GSPSourceParser newInstance = new GSPSourceParser();
        newInstance.setTokenizer(getTokenizer().newInstance());
        return newInstance;
    }
}
