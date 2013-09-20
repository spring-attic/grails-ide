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
package org.grails.ide.eclipse.editor.groovy.types;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.ast.ClassNode;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.jdt.groovy.search.VariableScope;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.core.internal.plugins.IGrailsProjectInfo;
import org.grails.ide.eclipse.core.model.GrailsVersion;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;

/**
 * This cache keeps track of extra domain class and controller class methods and properties.
 * This cache is refreshed whenever the classpath changes
 * @author Andrew Eisenberg
 * @created Sep 28, 2010
 */
public class PerProjectMemberCache implements IGrailsProjectInfo {
    private static final String HIBERNATE_CRITERIA_BUILDER = "grails.orm.HibernateCriteriaBuilder";
    private static final String ERRORS = "org.springframework.validation.Errors";
    private static final String MODEL_AND_VIEW = "org.springframework.web.servlet.ModelAndView";
    private static final String LOG = "org.apache.commons.logging.Log";
    private static final String HTTP_SESSION = "javax.servlet.http.HttpSession";
    private static final String SERVLET_CONTEXT = "javax.servlet.ServletContext";
    private static final String RESPONSE = "javax.servlet.http.HttpServletResponse";
    private static final String REQUEST = "javax.servlet.http.HttpServletRequest";
    private static final String GRAILS_APPLICATION_ATTRIBUTES = "org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes";
    private static final String GRAILS_APPLICATION = "org.codehaus.groovy.grails.commons.GrailsApplication";

    private IProject project;
    private GrailsProject grailsProject;

    private Map<String, ClassNode> extraControllerReferences = new HashMap<String, ClassNode>();
    private Map<String, ClassNode[]> staticDomainMembers = new HashMap<String, ClassNode[]>();
    private Map<String, ClassNode> nonstaticDomainMembers = new HashMap<String, ClassNode>();
    private Map<String, ClassNode> dynamicDomainMembers = new HashMap<String, ClassNode>();
    private Map<String, ClassNode> tagLibMembers = new HashMap<String, ClassNode>();
    private Boolean isGrails2OrLater;

    public IProject getProject() {
        return project;
    }

    public void setProject(IProject project) {
        this.project = project;
        if (project != null) {
            this.grailsProject = GrailsWorkspaceCore.get().create(project);
        }
    }

    public void projectChanged(GrailsElementKind[] changeKinds,
            IResourceDelta change) {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            boolean foundRelevantChange = false;
            for (GrailsElementKind changeKind : changeKinds) {
                if (changeKind == GrailsElementKind.PROJECT || 
                        changeKind == GrailsElementKind.CLASSPATH) {
                    foundRelevantChange = true;
                    break;
                }
            }
            if (foundRelevantChange) {
                extraControllerReferences.clear();
                staticDomainMembers.clear();
                nonstaticDomainMembers.clear();
                dynamicDomainMembers.clear();
            }
        }
    }

    public Map<String, ClassNode> getExtraControllerReferences() {
        if (extraControllerReferences.isEmpty() && project != null) {
            synchronized (GrailsCore.get().getLockForProject(project)) {
                PerProjectTypeCache typeCache = GrailsCore.get().connect(project, PerProjectTypeCache.class);
                if (typeCache == null) {
                    return Collections.emptyMap();
                }
                ClassNode uriClassNode = typeCache.getClassNode("java.net.URI");

                extraControllerReferences.put("log", typeCache.getClassNode(LOG));
                extraControllerReferences.put("actionName", VariableScope.STRING_CLASS_NODE);
                extraControllerReferences.put("actionUri", uriClassNode);
                extraControllerReferences.put("controllerName", VariableScope.STRING_CLASS_NODE);
                extraControllerReferences.put("controllerUri", uriClassNode);
                extraControllerReferences.put("flash", VariableScope.clonedMap());
                extraControllerReferences.put("chainModel", VariableScope.clonedMap());
                extraControllerReferences.put("grailsApplication", typeCache.getClassNode(GRAILS_APPLICATION));
                extraControllerReferences.put("grailsAttributes", typeCache.getClassNode(GRAILS_APPLICATION_ATTRIBUTES));
                extraControllerReferences.put("params", VariableScope.clonedMap());
                extraControllerReferences.put("request", typeCache.getClassNode(REQUEST));
                extraControllerReferences.put("response", typeCache.getClassNode(RESPONSE));
                extraControllerReferences.put("servletContext", typeCache.getClassNode(SERVLET_CONTEXT));
                extraControllerReferences.put("session", typeCache.getClassNode(HTTP_SESSION));
                extraControllerReferences.put("pluginContextPath", VariableScope.STRING_CLASS_NODE);
                extraControllerReferences.put("message", VariableScope.STRING_CLASS_NODE);
                extraControllerReferences.put("modelAndView", typeCache.getClassNode(MODEL_AND_VIEW));

                // methods
                extraControllerReferences.put("getTemplateUri", uriClassNode);
                extraControllerReferences.put("getViewUri", uriClassNode);
                extraControllerReferences.put("bindData", VariableScope.VOID_CLASS_NODE);
                extraControllerReferences.put("chain", VariableScope.VOID_CLASS_NODE);
                extraControllerReferences.put("render", VariableScope.VOID_CLASS_NODE);
                extraControllerReferences.put("redirect", VariableScope.VOID_CLASS_NODE);
            }
        }
        return extraControllerReferences;
    }


    /**
     * returns member name and a 2 element array consisting of return type and declaring type (null
     * means it should be replaced by current type and list will be parameterized by current type)
     * @return
     */
    public Map<String, ClassNode[]> getStaticDomainMembers() {
        // only do when < Grails 1.4
        if (staticDomainMembers.isEmpty() && project != null && !isGrails2OrLater()) {
            synchronized (GrailsCore.get().getLockForProject(project)) {
                PerProjectTypeCache typeCache = GrailsCore.get().connect(project, PerProjectTypeCache.class);
                if (typeCache == null) {
                    return Collections.emptyMap();
                }

                ClassNode criteriaBuilder = typeCache.getClassNode(HIBERNATE_CRITERIA_BUILDER);
                staticDomainMembers.put("log", pair(typeCache.getClassNode(LOG), null));
                
                // hmmmm....should this be commented out???
                staticDomainMembers.put("createCriteria", pair(criteriaBuilder, criteriaBuilder));

                staticDomainMembers.put("count", pair(VariableScope.NUMBER_CLASS_NODE, null));
                staticDomainMembers.put("executeQuery", pair(VariableScope.clonedList(), criteriaBuilder));
                staticDomainMembers.put("exists", pair(VariableScope.BOOLEAN_CLASS_NODE, null));
                staticDomainMembers.put("findWhere", pair(null, null));  // null to be replaced with the current type
                staticDomainMembers.put("findAllWhere", pair(VariableScope.clonedList(), null));
                staticDomainMembers.put("get", pair(null, null));  // null to be replaced with the current type
                staticDomainMembers.put("getAll", pair(VariableScope.clonedList(), null));
                staticDomainMembers.put("list", pair(VariableScope.clonedList(), null));
                staticDomainMembers.put("withCriteria", pair(VariableScope.clonedList(), criteriaBuilder));
                staticDomainMembers.put("withTransaction", pair(VariableScope.VOID_CLASS_NODE, null));
            }
        }
        return staticDomainMembers;
    }
    
    private boolean isGrails2OrLater() {
        if (isGrails2OrLater == null) {
            isGrails2OrLater = grailsProject.getEclipseGrailsVersion().compareTo(GrailsVersion.V_2_0_0) >= 0;
        }
        return isGrails2OrLater;
    }
    
    private ClassNode[] pair(ClassNode ret, ClassNode decl) {
        return new ClassNode[] { ret, decl };
    }

    public Map<String, ClassNode> getNonstaticDomainMembers() {
        if (nonstaticDomainMembers.isEmpty() && project != null) {
            synchronized (GrailsCore.get().getLockForProject(project)) {
                PerProjectTypeCache typeCache = GrailsCore.get().connect(project, PerProjectTypeCache.class);
                if (typeCache == null) {
                    return Collections.emptyMap();
                }

                nonstaticDomainMembers.put("add", VariableScope.VOID_CLASS_NODE);
                nonstaticDomainMembers.put("addTo", VariableScope.VOID_CLASS_NODE); // * a dynamic adder
                nonstaticDomainMembers.put("clearErrors", VariableScope.VOID_CLASS_NODE);
                nonstaticDomainMembers.put("hasErrors", VariableScope.BOOLEAN_CLASS_NODE);
                nonstaticDomainMembers.put("ident", VariableScope.NUMBER_CLASS_NODE); // could be any type, but probably a number

                // fields
                nonstaticDomainMembers.put("constraints", VariableScope.clonedList());
                nonstaticDomainMembers.put("properties", VariableScope.clonedList());
                nonstaticDomainMembers.put("id", VariableScope.LONG_CLASS_NODE);
                nonstaticDomainMembers.put("version", VariableScope.STRING_CLASS_NODE);

                nonstaticDomainMembers.put("errors", typeCache.getClassNode(ERRORS));
            }
        }
        return nonstaticDomainMembers;
    }

    public Map<String, ClassNode> getDynamicDomainMembers() {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            if (dynamicDomainMembers.isEmpty() && project != null) {
                dynamicDomainMembers.put("listOrderBy", VariableScope.clonedList());  // dynamic lister
                dynamicDomainMembers.put("findAllBy", VariableScope.clonedList());  // dynamic finder
                dynamicDomainMembers.put("findBy", null);  // null to be replaced with the current type, dynamic finder
                dynamicDomainMembers.put("countBy", VariableScope.NUMBER_CLASS_NODE);  // dynamic counter
                if (isGrails2OrLater()) {
                    dynamicDomainMembers.put("findOrCreateBy", null);  // null to be replaced with the current type, dynamic finder
                    dynamicDomainMembers.put("findOrSaveBy", null);  // null to be replaced with the current type, dynamic finder
                }
            }
            return dynamicDomainMembers;
        }
    }


    public Map<String, ClassNode> getTagLibMembers() {
        if (tagLibMembers.isEmpty() && project != null) {
            synchronized (GrailsCore.get().getLockForProject(project)) {
                PerProjectTypeCache typeCache = GrailsCore.get().connect(project, PerProjectTypeCache.class);
                if (typeCache == null) {
                    return Collections.emptyMap();
                }
                tagLibMembers.put("out", typeCache.getClassNode("java.io.Writer"));
                tagLibMembers.put("grailsApplication", typeCache.getClassNode(GRAILS_APPLICATION));
                tagLibMembers.put("request", typeCache.getClassNode(REQUEST));
                tagLibMembers.put("response", typeCache.getClassNode(RESPONSE));
                tagLibMembers.put("servletContext", typeCache.getClassNode(SERVLET_CONTEXT));
                tagLibMembers.put("session", typeCache.getClassNode(HTTP_SESSION));

                tagLibMembers.put("params", VariableScope.clonedMap());
                tagLibMembers.put("pageScope", VariableScope.clonedMap());
                tagLibMembers.put("flash", VariableScope.clonedMap());
                tagLibMembers.put("controllerName", VariableScope.STRING_CLASS_NODE);
                tagLibMembers.put("pluginContextPath", VariableScope.STRING_CLASS_NODE);
                
                // should be a method with one argument
                tagLibMembers.put("throwTagError", VariableScope.VOID_CLASS_NODE);
            }
        }
        return tagLibMembers;
    }

    public void dispose() {
        project = null;
        grailsProject = null;
        extraControllerReferences = new HashMap<String, ClassNode>();
        staticDomainMembers = new HashMap<String, ClassNode[]>();
        nonstaticDomainMembers = new HashMap<String, ClassNode>();
        dynamicDomainMembers = new HashMap<String, ClassNode>();
    }
}
