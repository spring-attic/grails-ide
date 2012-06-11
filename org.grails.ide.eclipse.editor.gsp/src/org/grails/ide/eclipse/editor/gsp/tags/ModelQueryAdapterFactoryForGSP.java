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
package org.grails.ide.eclipse.editor.gsp.tags;

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.jsp.core.internal.modelquery.JSPModelQueryAdapterImpl;
import org.eclipse.jst.jsp.core.internal.modelquery.JSPModelQueryImpl;
import org.eclipse.jst.jsp.core.internal.modelquery.ModelQueryAdapterFactoryForJSP;
import org.eclipse.wst.common.uriresolver.internal.provisional.URIResolver;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.util.Debug;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.util.CMDocumentCache;
import org.eclipse.wst.xml.core.internal.modelquery.XMLCatalogIdResolver;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;

/**
 * @author Andrew Eisenberg
 * @created Nov 27, 2009
 * Use this class to create our own version of {@link JSPModelQueryImpl}Need this so that we can add grails taglibs
 */
public class ModelQueryAdapterFactoryForGSP extends
        ModelQueryAdapterFactoryForJSP {

    /**
     * createAdapter method comment.
     */
    @SuppressWarnings("deprecation")
    protected INodeAdapter createAdapter(INodeNotifier target) {
        if (Debug.displayInfo)
            System.out.println("-----------------------ModelQueryAdapterFactoryForJSP.createAdapter" + target); //$NON-NLS-1$
        if (modelQueryAdapterImpl == null) {
            if (target instanceof IDOMNode) {
                IDOMNode xmlNode = (IDOMNode) target;
                IStructuredModel model = stateNotifier = xmlNode.getModel();
                if (model.getBaseLocation() != null) {
                    stateNotifier.addModelStateListener(this);
                }

                org.eclipse.wst.sse.core.internal.util.URIResolver resolver = model.getResolver();
                if (Debug.displayInfo)
                    System.out.println("----------------ModelQueryAdapterFactoryForJSP... baseLocation : " + resolver.getFileBaseLocation()); //$NON-NLS-1$

                /**
                 * XMLCatalogIdResolver currently requires a filesystem
                 * location string. Customarily this will be what is in the
                 * deprecated SSE URIResolver and required by the Common URI
                 * Resolver.
                 */
                URIResolver idResolver = null;
                if (resolver != null) {
                    idResolver = new XMLCatalogIdResolver(resolver.getFileBaseLocation(), resolver);
                }
                else {
                    /*
                     * 203649 - this block may be necessary due to ordering of
                     * setting the resolver into the model
                     */
                    String baseLocation = null;
                    String modelsBaseLocation = model.getBaseLocation();
                    if (modelsBaseLocation != null) {
                        File file = new Path(modelsBaseLocation).toFile();
                        if (file.exists()) {
                            baseLocation = file.getAbsolutePath();
                        }
                        else {
                            IPath basePath = new Path(model.getBaseLocation());
                            IResource derivedResource = null;
                            if (basePath.segmentCount() > 1)
                                derivedResource = ResourcesPlugin.getWorkspace().getRoot().getFile(basePath);
                            else
                                derivedResource = ResourcesPlugin.getWorkspace().getRoot().getProject(basePath.segment(0));
                            IPath derivedPath = derivedResource.getLocation();
                            if (derivedPath != null) {
                                baseLocation = derivedPath.toString();
                            }
                            else {
                                URI uri = derivedResource.getLocationURI();
                                if (uri != null) {
                                    baseLocation = uri.toString();
                                }
                            }
                        }
                        if(baseLocation == null) {
                            baseLocation = modelsBaseLocation;
                        }
                    }
                    idResolver = new XMLCatalogIdResolver(baseLocation, null);
                }

                ModelQuery modelQuery = createModelQuery(model, idResolver);
                modelQuery.setEditMode(ModelQuery.EDIT_MODE_UNCONSTRAINED);
                modelQueryAdapterImpl = new JSPModelQueryAdapterImpl(new CMDocumentCache(), modelQuery, idResolver);
            }
        }
        return modelQueryAdapterImpl;
    }
    
    protected ModelQuery createModelQuery(IStructuredModel model, URIResolver resolver) {
        return new GSPModelQueryImpl(model, resolver);
    }


}
