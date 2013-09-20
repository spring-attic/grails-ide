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
package org.grails.ide.eclipse.editor.groovy.elements;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.groovy.search.VariableScope;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;

import org.grails.ide.eclipse.editor.groovy.types.PerProjectServiceCache;
import org.grails.ide.eclipse.editor.groovy.types.PerProjectTypeCache;

/**
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @created Nov 23, 2009
 */
public abstract class AbstractGrailsElement implements IGrailsElement  {
    protected final ICompilationUnit unit;
    protected ClassNode cachedGroovyClass = null;
    private Set<String> serviceReferences = null;
    private String primaryTypeName;
    
    private Map<String, AnnotatedNode> cachedGeneratedMembers = new HashMap<String, AnnotatedNode>();
    
    
    public AbstractGrailsElement(ICompilationUnit unit) {
        Assert.isNotNull(unit, "Null ICompilationUnit");
        this.unit = unit;
    }

    /**
     * Gets the Groovy {@link ClassNode} associated with this Grails Element
     * May return <code>null</code> if no {@link ClassNode} exists.
     */
    @SuppressWarnings("cast")
    public ClassNode getGroovyClass() {
        if (cachedGroovyClass != null) {
            return cachedGroovyClass;
        }
        if (unit instanceof GroovyCompilationUnit) {
            if (unit.isWorkingCopy()) {
                ModuleNode module = ((GroovyCompilationUnit) unit).getModuleNode();
                if (module != null) {
                    String typeName = getPrimaryTypeName();
                    for (ClassNode clazz : (Iterable<ClassNode>) module.getClasses()) {
                        // ensure that the default is the first class unless a better match is found
                        if (clazz.getNameWithoutPackage().equals(typeName) || cachedGroovyClass == null) {
                            cachedGroovyClass = clazz;
                        }
                    }
                }
            } else {
                // do not get the module node if this is not a working copy.
                // this prevents temporarily turning the unit into a working copy
                cachedGroovyClass = 
                    GrailsCore.get().connect(unit.getJavaProject().getProject(), PerProjectTypeCache.class).createClassNodeFromSource(unit);
            }
            
        } else {
            // not sure about this...
            // should only get here for OtherGrailsKinds
            // eventually could use the PerProjectTypeCache here
            return null;
        }
        
        // might still be null at this point if there were no matching type names found
        return cachedGroovyClass;
//        throw new AssertionFailedException("Invalid name for compilation unit " + unit.getElementName()); //$NON-NLS-1$
    }
    
    protected String getPrimaryTypeName() {
        if (primaryTypeName == null) {
            String unitName = unit.getElementName();
            int dotIndex = unitName.indexOf('.');
            if (dotIndex > 0) {
                primaryTypeName = unitName.substring(0, dotIndex);
            } else {
                throw new AssertionFailedException("Invalid name for compilation unit " + unitName); //$NON-NLS-1$
            }
        }
        return primaryTypeName;
    }

    public ICompilationUnit getCompilationUnit() {
        return unit;
    }

    public IResource getResource() {
        return unit.getResource();
    }
    
    /**
     * get a cached method.  Not only saves processing time, but also 
     * ensures that FindOccurrences visitor works properly
     * @param toCache
     */
    protected void cacheGeneratedMember(AnnotatedNode toCache) {
        if (toCache instanceof MethodNode) {
            cachedGeneratedMembers.put(((MethodNode) toCache).getName(), toCache);
        } else if (toCache instanceof PropertyNode) {
            cachedGeneratedMembers.put(((PropertyNode) toCache).getName(), toCache);
        } else if (toCache instanceof FieldNode) {
            cachedGeneratedMembers.put(((FieldNode) toCache).getName(), toCache);
        } else {
            throw new IllegalArgumentException("Expecting a method, property, or field node.  Instead got " + toCache);
        }
    }
    
    protected AnnotatedNode getCachedMember(String name) {
        return cachedGeneratedMembers.get(name);
    }
    
    /**
     * Populates the scope with dependency injected services
     * @param scope
     */
    protected void populateInjectedServices(VariableScope scope) {
        ClassNode groovyClass = getGroovyClass();
        if (groovyClass != null) {
            PerProjectServiceCache serviceCache = GrailsCore.get().connect(getResource().getProject(), PerProjectServiceCache.class);
            List<FieldNode> fields = groovyClass.getFields();
            serviceReferences = new HashSet<String>();
            Map<String, ClassNode> services = serviceCache.findServicesFor(fields);
            for (Entry<String, ClassNode> entry : services.entrySet()) {
                scope.addVariable(entry.getKey(), entry.getValue(), groovyClass);
                serviceReferences.add(entry.getKey());
            }
        }
    }
    
    @Override
    public String toString() {
    	return getKind()+"("+getCompilationUnit()+")";
    }
    
    protected boolean isServiceReference(PropertyNode prop) {
        if (serviceReferences == null) {
            return false;
        } else {
            return serviceReferences.contains(prop.getName());
        }
    }
    
    
}
