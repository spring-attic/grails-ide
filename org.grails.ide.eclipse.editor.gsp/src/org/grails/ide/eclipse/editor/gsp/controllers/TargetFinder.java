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
package org.grails.ide.eclipse.editor.gsp.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.jsp.core.internal.domdocument.AttrImplForJSP;
import org.eclipse.jst.jsp.core.internal.domdocument.ElementImplForJSP;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;

import org.grails.ide.eclipse.editor.groovy.controllers.ActionTarget;
import org.grails.ide.eclipse.editor.groovy.controllers.ControllerTarget;
import org.grails.ide.eclipse.editor.groovy.controllers.ITarget;
import org.grails.ide.eclipse.editor.groovy.controllers.PerProjectControllerCache;

/**
 * 
 * @author Andrew Eisenberg
 * @created Jul 14, 2011
 */
public class TargetFinder {
    
    private final static String NO_PREFIX = "";
    
    String prefix = NO_PREFIX;

    private final boolean usePrefix;
    
    public TargetFinder(boolean usePrefix) {
        this.usePrefix = usePrefix;
    }
    
    /**
     * Finds controller and action targets in the given document at the
     * specified location.  If usePrefix is specified, then the prefix
     * is calculated 
     * @param document
     * @param offset
     * @param usePrefix
     * @return
     */
    public List<ITarget> findTargets(IStructuredDocument document, int offset) {
        IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForRead(document);
        try {
            if (model != null) {
                IndexedRegion indexedRegion = model.getIndexedRegion(offset);
                if (indexedRegion instanceof ElementImplForJSP) {
                    ElementImplForJSP element = (ElementImplForJSP) indexedRegion;
                    if (element.getNodeName().equals("g:link")) {
                        
                        // first check to see if content assist inside of controller attribute
                        // else grab the controller name
                        AttrImplForJSP controllerAttr = (AttrImplForJSP) element.getAttributeNode("controller");
                        int nodeRelativeOffset = offset - indexedRegion.getStartOffset();
                        String controllerName = null;
                        if (isPositionInsideRegion(controllerAttr, nodeRelativeOffset)) {
                            prefix = usePrefix ? findPrefix(controllerAttr, nodeRelativeOffset) : controllerAttr.getValue();
                            return findControllers(prefix, offset, model);
                        } else if (controllerAttr != null) {
                            controllerName = controllerAttr.getValue();
                        } else {
                            // try to get the controller name from the base location
                            controllerName = extractControllerName(model);
                        }
                        
                        
                        // check to see if content assist in action attribute
                        if (controllerName != null) {
                            // can't do anything unless there is already a controller name
                            AttrImplForJSP actionAttr = (AttrImplForJSP) element.getAttributeNode("action");
                            if (isPositionInsideRegion(actionAttr, nodeRelativeOffset)) {
                                // content assist inside of action region
                                prefix = usePrefix ? findPrefix(actionAttr, nodeRelativeOffset) : actionAttr.getValue();
                                return findActions(controllerName, prefix, offset, model);
                            }
                        }
                    }
                }
            }
        } finally {
            if (model != null) {
                model.releaseFromRead();
            }
        }   
        return Collections.emptyList();
    }
    

    /**
     * Guess the controller name from the base location
     * @param model
     * @return
     */
    private String extractControllerName(IStructuredModel model) {
        String baseLocation = model.getBaseLocation();
        if (baseLocation != null) {
            String[] segments = baseLocation.split("/");
            if (segments.length > 2) {
                return segments[segments.length-2];
            }
        }
        return null;
    }
    
    private List<ITarget> findControllers(String prefix,
            int invocationOffset, IStructuredModel model) {
        IProject project = findProject(model);
        if (project != null) {
            PerProjectControllerCache controllerCache = GrailsCore.get().connect(project, PerProjectControllerCache.class);
            if (controllerCache != null) {
                Set<ControllerTarget> controllerTargets = controllerCache.getAllControllerTargets();
                if (controllerTargets != null) {
                    List<ITarget> completions = new ArrayList<ITarget>(controllerTargets.size());
                    for (ControllerTarget controllerTarget : controllerTargets) {
                        if (controllerTarget.getName().startsWith(prefix)) {
                            completions.add(controllerTarget);
                        }
                    }
                    return completions;
                }
            }
        }
        return Collections.emptyList();
    }
    private List<ITarget> findActions(String controllerName, String prefix,
            int invocationOffset, IStructuredModel model) {
        IProject project = findProject(model);
        if (project != null) {
            PerProjectControllerCache controllerCache = GrailsCore.get().connect(project, PerProjectControllerCache.class);
            if (controllerCache != null) {
                Set<ActionTarget> actions = controllerCache.getActionsForController(controllerName);
                if (actions != null) {
                    List<ITarget> completions = new ArrayList<ITarget>(actions.size());
                    for (ActionTarget action : actions) {
                        if (action.getName().startsWith(prefix)) {
                            completions.add(action);
                        }
                    }
                    return completions;
                }
            }
        }
        return Collections.emptyList();
    }
    
    private IProject findProject(IStructuredModel model) {
        if (model.getBaseLocation() == null) {
            return null;
        }
        
        IPath p = new Path(model.getBaseLocation());
        if (p.segmentCount() == 0) {
            return null;
        }
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(p.segment(0));
        if (!project.isAccessible()) {
            return null;
        }
        return project;
    }
    private String findPrefix(AttrImplForJSP attr, int nodeRelativeOffset) {
        int start = attr.getValueRegion().getStart();
        String fullText = attr.getValueSource();
        return fullText.substring(0, nodeRelativeOffset-start-1);
    }
    private boolean isPositionInsideRegion(AttrImplForJSP attr, int nodeRelativeOffset) {
        if (attr == null) {
            return false;
        }
        ITextRegion valueRegion = attr.getValueRegion();
        if (valueRegion == null) {
            return false;
        }
        return nodeRelativeOffset > valueRegion.getStart() && nodeRelativeOffset < valueRegion.getEnd();
    }
}
