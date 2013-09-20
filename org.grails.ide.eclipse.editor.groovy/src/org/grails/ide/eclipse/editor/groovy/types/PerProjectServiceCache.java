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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
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

import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;

/**
 * Caches {@link ClassNode}s that correspond to grails services 
 * @since 2.6.0
 */
public class PerProjectServiceCache implements IGrailsProjectInfo {
	
	private Map<String, ClassNode> serviceCache;
	
	private IProject project;
	
	private IJavaProject javaProject;
	
    public void dispose() {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            project = null;
            javaProject = null;
            serviceCache = null;
        }
    }

    public IProject getProject() {
        return project;
    }

    /**
     * The cache is flushed when the classpath changes or there is a refresh dependencies
     */
    public void projectChanged(GrailsElementKind[] changeKinds,
            IResourceDelta change) {
        synchronized (GrailsCore.get().getLockForProject(project)) {
        	boolean foundRelevantChange = false;
        	for (GrailsElementKind changeKind : changeKinds) {
        		if (changeKind == GrailsElementKind.PROJECT || 
        				changeKind == GrailsElementKind.CLASSPATH ||
        				changeKind == GrailsElementKind.SERVICE_CLASS) {
        			foundRelevantChange = true;
        			break;
        		}
            }
        	if (foundRelevantChange) {
        	    // also remvoe references to service classes in the 
        	    // type cache
                PerProjectTypeCache typeCache = GrailsCore.get().connect(project, PerProjectTypeCache.class);
                
                if (serviceCache != null) {
                    for (ClassNode serviceClass : serviceCache.values()) {
                        typeCache.clearFromCache(serviceClass.getName());
                    }
                    serviceCache = null;
                }        	    
        	}
        }
    }

    public void setProject(IProject project) {
        this.project = project;
        this.javaProject = JavaCore.create(project);
    }
    
    public Map<String, ClassNode> findServicesFor(List<FieldNode> fields) {
        ensureInitialized();
        Map<String, ClassNode> existingServices = new HashMap<String, ClassNode>();
        for (FieldNode field : fields) {
            String name = field.getName();
            ClassNode serviceClass = serviceCache.get(name);
            if (serviceClass != null) {
                existingServices.put(name, serviceClass);
            }
        }
        return existingServices;
    }

    private void ensureInitialized() {
        if (serviceCache == null) {
            try {
                serviceCache = GrailsWorkspaceCore.get().create(javaProject).findAllServices();
            } catch (JavaModelException e) {
                GrailsCoreActivator.log(e);
            }
        }
    }



    
    
    /**
     * Not API!!! For testing only
     * 
     * @param type specify that this type is a service.
     */
    public void addService(GroovyCompilationUnit unit, String name) {
        ensureInitialized();
        List<ClassNode> classes = unit.getModuleNode().getClasses();
        ClassNode service = null;
        for (ClassNode clazz : classes) {
            if (clazz.getNameWithoutPackage().equals(name)) {
                service = clazz;
            }
        }
        if (service == null) {
            throw new IllegalArgumentException("Could not find '" + name + "' class in module"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        serviceCache.put(GrailsProject.getBeanName(unit.getType(name)), service);
    }
    
    public Map<String, ClassNode> getAllServices() {
        ensureInitialized();
        return serviceCache;
    }
}
