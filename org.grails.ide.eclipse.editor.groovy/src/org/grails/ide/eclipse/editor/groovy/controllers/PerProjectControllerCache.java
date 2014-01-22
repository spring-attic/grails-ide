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
package org.grails.ide.eclipse.editor.groovy.controllers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.core.internal.plugins.IGrailsProjectInfo;

import org.grails.ide.eclipse.editor.groovy.elements.ControllerClass;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.editor.groovy.types.PerProjectTypeCache;

/**
 * Keeps track of controllers and their actions so that gsp views can 
 * use their contents to help with type inferencing
 * 
 * @author Andrew Eisenberg
 * @since 2.7.0
 */
public class PerProjectControllerCache implements IGrailsProjectInfo {

    private class TargetSet {
        public TargetSet(ControllerTarget target, Set<ActionTarget> actions) {
            this.target = target;
            this.actions = actions;
        }
        final ControllerTarget target;
        final Set<ActionTarget> actions;
    }
    
    private IProject project;
    
    private IJavaProject javaProject;
    
    // view folder name to a controller cache
    private Map<String, ControllerCache> controllerCaches = new HashMap<String, ControllerCache>();
    
    // view folder name to set of field (action) names
    private Map<String, TargetSet> controllerToTargets = null;
    
    public void dispose() {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            project = null;
            controllerCaches = null;
            controllerToTargets = null;
        }
    }

    public IProject getProject() {
        return project;
    }

    public void setProject(IProject project) {
        this.project = project;
        if (project != null) {
            javaProject = JavaCore.create(project);
        }
    }

    public void projectChanged(GrailsElementKind[] changeKinds,
            IResourceDelta change) {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            boolean foundRelevantChange = false;
            for (GrailsElementKind changeKind : changeKinds) {
                if (changeKind == GrailsElementKind.CONTROLLER_CLASS) {
                    foundRelevantChange = true;
                    break;
                }
            }
            if (foundRelevantChange) {
                controllerCaches.clear();
                controllerToTargets = null;
            }
        }
    }
    
    public Map<String, ClassNode> findReturnValuesForAction(String viewFolderName, String actionName) {
        if (project == null) {
            return null;
        }
        
        if (actionName.endsWith(".gsp")) {
            actionName = actionName.substring(0, actionName.length()-".gsp".length());
        }
        
        
        synchronized (GrailsCore.get().getLockForProject(project)) {
            ControllerCache controllerCache = controllerCaches.get(viewFolderName);
            if (controllerCache == null) {
                controllerCache = createControllerCache(viewFolderName);
                if (controllerCache != null) {
                    controllerCaches.put(viewFolderName, controllerCache);
                }
            }
            if (controllerCache == null) {
                return null;
            }
            
            Map<String, ClassNode> values = controllerCache.findReturnValuesForAction(actionName);
            return values;
        }
    }
    
    public String findReturnValuesAsDeclarations(String viewFolderName, String actionName) {
        // don't do anything for the views at the root view folder
        if (viewFolderName.equals("views")) {
            return "";
        }
        Map<String, ClassNode> valuesForAction = findReturnValuesForAction(viewFolderName, actionName);
        if (valuesForAction == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Entry<String, ClassNode> entry : valuesForAction.entrySet()) {
            String className = extractClassName(entry.getValue());
            sb.append(className + " " + entry.getKey() + ";\n");
        }
        return sb.toString();
    }

    private String extractClassName(ClassNode clazz) {
        StringBuilder sb = new StringBuilder();
        ClassNode componentType = clazz;
        int arrayCount = 0;
        while (componentType.getComponentType() != null) {
            arrayCount++;
            componentType = componentType.getComponentType();
        }
        // use the wrapper class since it is not legal to have a primitive type as a type argument
        sb.append(ClassHelper.getWrapper(componentType).getName());
        
        // now look for type parameters
        GenericsType[] tps = clazz.getGenericsTypes();
        if (tps != null && tps.length > 0) {
            StringBuilder sb2 = new StringBuilder();
            boolean doit = false;
            sb2.append("<");
            for (int i = 0; i < tps.length; i++) {
                if (tps[i].isPlaceholder()) {
                    continue;
                }
                sb2.append(extractClassName(tps[i].getType()));
                sb2.append(",");
                doit = true;
            }
            if (doit) {
                sb2.replace(sb2.length()-1, sb2.length(), ">");
                sb.append(sb2);
            }
        }

        for (int i = 0; i < arrayCount; i++) {
            sb.append("[]");
        }
        return sb.toString();
    }
    
    private ControllerCache createControllerCache(String viewFolderName) {
        String controllerName = convertToControllerName(viewFolderName);
        if (controllerName == null) {
            return null;
        }
        GroovyCompilationUnit controllerUnit = findControllerUnit(controllerName);
        if (controllerUnit == null) {
            return null;
        }
        
        return new ControllerCache(controllerUnit);
    }

    
    private String convertToControllerName(String viewFolderName) {
        return String.valueOf(Character.toUpperCase(viewFolderName.charAt(0))) + viewFolderName.substring(1) + "Controller.groovy";
    }
    
    private GroovyCompilationUnit findControllerUnit(String controllerName) {
        ControllerClass cc = GrailsWorkspaceCore.get().create(javaProject).getControllerClass(controllerName);
        if (cc != null) {
            return (GroovyCompilationUnit) cc.getCompilationUnit();
        }
        return null;
    }
    
    public Set<ControllerTarget> getAllControllerTargets() {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            ensureInitialized();
            Set<ControllerTarget> targets = new HashSet<ControllerTarget>(controllerToTargets.size(), 1);
            for (TargetSet controllerSet : controllerToTargets.values()) {
                targets.add(controllerSet.target);
            }
            return targets;
        }
    }
    
    public Set<ActionTarget> getActionsForController(String controller) {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            ensureInitialized();
            TargetSet targetSet = controllerToTargets.get(controller);
            return targetSet == null ? null : targetSet.actions;
        }
    }
    
    private void ensureInitialized() {
        if (controllerToTargets == null) {
            try {
                controllerToTargets = new HashMap<String, TargetSet>();
                Map<String, ClassNode> allControllers = GrailsWorkspaceCore.get().create(javaProject).findAllControllers();
                
                PerProjectTypeCache typeCache = GrailsCore.get().connect(project, PerProjectTypeCache.class);

                for (Entry<String, ClassNode> controllerEntry : allControllers.entrySet()) {
                    ClassNode controller = controllerEntry.getValue();
                    String controllerName = extractControllerName(controllerEntry);
                    
                    ControllerTarget target = new ControllerTarget(controller.getName(), controllerName, javaProject);
                    Set<ActionTarget> actions = new HashSet<ActionTarget>();
                    for (PropertyNode prop : controller.getProperties()) {
                        // assume all public fields are actions
                        if (!prop.isStatic() && prop.isPublic() && !prop.getName().equals("metaClass")) {
                            actions.add(new ActionTarget(target, prop.getName(), false));
                        }
                    }
                    
                    // in grails 2.0+, controller actions are now methods.  Need to look for those, too
                    for (MethodNode method : controller.getMethods()) {
                        // assume all public no-arg methods are actions
                        if (isMethodAction(method) && controller.equals(method.getDeclaringClass())) {
                            actions.add(new ActionTarget(target, method.getName(), true));
                        }
                    }
                    controllerToTargets.put(controllerName, new TargetSet(target, actions));
                    
                    // we don't want to keep this in the type cache in case there is a change
                    typeCache.clearFromCache(controllerEntry.getValue().getName());
                }
            } catch (JavaModelException e) {
                GrailsCoreActivator.log(e);
            }
        }
    }

    private boolean isMethodAction(MethodNode method) {
        return !method.isStatic() && method.isPublic() && 
                !ignoredMethodNames.contains(method.getName()) && !method.getName().contains("$");
    }
    
    private final static Set<String> ignoredMethodNames = new HashSet<String>();
    static {
        ignoredMethodNames.add("getProperty");
        ignoredMethodNames.add("setProperty");
        ignoredMethodNames.add("getMetaClass");
        ignoredMethodNames.add("setMetaClass");
        ignoredMethodNames.add("invokeMethod");
    }

    /**
     * @param controllerEntry
     * @return
     */
    private String extractControllerName(
            Entry<String, ClassNode> controllerEntry) {
        String controllerName = controllerEntry.getKey();
        int contIndex = controllerName.indexOf("Controller");
        if (contIndex > 0) {
            controllerName = controllerName.substring(0, contIndex);
        }
        return controllerName;
    }
}
