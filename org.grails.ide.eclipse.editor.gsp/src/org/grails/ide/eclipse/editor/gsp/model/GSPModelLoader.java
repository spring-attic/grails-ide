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

import java.util.List;

import org.eclipse.jst.jsp.core.internal.modelhandler.JSPModelLoader;
import org.eclipse.jst.jsp.core.internal.modelquery.ModelQueryAdapterFactoryForJSP;
import org.eclipse.wst.sse.core.internal.document.IDocumentLoader;
import org.eclipse.wst.sse.core.internal.ltk.parser.RegionParser;
import org.eclipse.wst.sse.core.internal.provisional.IModelLoader;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;

import org.grails.ide.eclipse.editor.gsp.parser.GSPDocumentLoader;
import org.grails.ide.eclipse.editor.gsp.parser.GSPSourceParser;
import org.grails.ide.eclipse.editor.gsp.tags.ModelQueryAdapterFactoryForGSP;

/**
 * Make {@link GSPTranslation} create .groovy compilation units instead of .java.
 * @author Andrew Eisenberg
 * @created Nov 6, 2009
 */
public class GSPModelLoader extends JSPModelLoader implements IModelLoader {


    @Override
    public IStructuredModel newModel() {
        return new GSPStructuredModel();
    }

    /**
     * Swap the JSP adapter factory with GSP adapter factory
     */
    @Override
    public List<INodeAdapterFactory> getAdapterFactories() {
        List<INodeAdapterFactory> result = super.getAdapterFactories();
        int i = 0;
        for (INodeAdapterFactory factory : result) {
            if (factory instanceof ModelQueryAdapterFactoryForJSP) {
                break;
            }
            i++;
        }
        if (i < result.size()) {
            result.remove(i);
            result.add(i, new ModelQueryAdapterFactoryForGSP());
        }
        return result;
    }

    /**
     * stub for when we have our gsp parser ready
     */
    @Override
    public RegionParser getParser() {
        return new GSPSourceParser();
    }
    
    /**
     * stub for when we have our gsp parser ready
     */
    public IDocumentLoader getDocumentLoader() {
        return new GSPDocumentLoader();
    }
}