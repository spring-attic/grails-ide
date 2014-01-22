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
package org.grails.ide.eclipse.editor.groovy.elements;

import groovyjarjarasm.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.groovy.search.AbstractSimplifiedTypeLookup.TypeAndDeclaration;
import org.eclipse.jdt.groovy.search.TypeLookupResult.TypeConfidence;
import org.eclipse.jdt.groovy.search.VariableScope;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.core.internal.plugins.PerProjectPluginCache;
import org.grails.ide.eclipse.core.model.ContributedMethod;
import org.grails.ide.eclipse.core.model.ContributedProperty;
import org.grails.ide.eclipse.core.util.GrailsNameUtils;
import org.grails.ide.eclipse.editor.groovy.controllers.PerProjectControllerCache;
import org.grails.ide.eclipse.editor.groovy.types.PerProjectMemberCache;


/**
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @author Nieraj Singh
 * @created Nov 23, 2009
 */
@SuppressWarnings("nls")
public class ControllerClass extends AbstractGrailsElement implements INavigableGrailsElement {
    
    /**
     * A wrapper around object class that knows about the return values of the controller action
     */
    class ControllerActionClass extends ClassNode {

        private final String action;
        private List<FieldNode> extraFields;
        
        public ControllerActionClass(String action) {
            super(VariableScope.MAP_CLASS_NODE.getName(), VariableScope.MAP_CLASS_NODE.getModifiers(), VariableScope.MAP_CLASS_NODE.getSuperClass());
            this.action = action;
            this.isPrimaryNode = false;
            this.setRedirect(VariableScope.MAP_CLASS_NODE);
            if (cache == null) {
                cache = GrailsCore.get().connect(ControllerClass.this.getCompilationUnit().getJavaProject().getProject(), PerProjectControllerCache.class);
            }
        }

        @Override
        public FieldNode getField(String name) {
            List<FieldNode> fields = internalGetFields();
            for (FieldNode field : fields) {
                if (field.getName().equals(name)) {
                    return field;
                }
            }
            return null;
        }
        
        @Override
        public List<FieldNode> getFields() {
            return internalGetFields();
        }
        
        @Override
        public FieldNode getDeclaredField(String name) {
            return getField(name);
        }
        
        @Override
        public PropertyNode getProperty(String name) {
            FieldNode field = getField(name);
            if (field != null) {
                PropertyNode propertyNode = new PropertyNode(field, ACC_PUBLIC, null, null);
                propertyNode.setDeclaringClass(getGroovyClass());
                return propertyNode;
            } else {
                return null;
            }
        }
        
        @Override
        public List<PropertyNode> getProperties() {
            // consider caching
            List<FieldNode> fields = internalGetFields();
            List<PropertyNode> properties = new ArrayList<PropertyNode>(fields.size());
            for (FieldNode field : fields) {
                PropertyNode p = new PropertyNode(field, ACC_PUBLIC, null, null);
                p.setDeclaringClass(getGroovyClass());
                properties.add(p);
            }
            return properties;
        }
        
        
        private List<FieldNode> internalGetFields() {
            if (extraFields == null) {
                // prevent infinite recursion by setting to empty list first
                extraFields = Collections.emptyList();
                if (cache != null) {
                    Map<String, ClassNode> mappedValues = cache.findReturnValuesForAction(getLogicalName(), action);
                    if (mappedValues != null) {
                        extraFields = new ArrayList<FieldNode>(mappedValues.size());
                        for (Entry<String, ClassNode> entry : mappedValues.entrySet()) {
                            String name = entry.getKey();
                            ClassNode type = entry.getValue();
                            FieldNode f = new FieldNode(name, ACC_PUBLIC, type, getGroovyClass(), null);
                            f.setDeclaringClass(getGroovyClass());
                            extraFields.add(f);
                        }
                    }
                }
            }
            return extraFields;
        }
    }
    
    private static final Set<String> extraMethods = new HashSet<String>();
	public static final String CONTROLLER = "Controller"; 
	
    static {
        extraMethods.add("getTemplateUri");
        extraMethods.add("getViewUri");
        extraMethods.add("bindData");
        extraMethods.add("chain");
        extraMethods.add("render");
        extraMethods.add("redirect");

        // standard actions...make sure they exist
        extraMethods.add("show");
        extraMethods.add("create");
        extraMethods.add("list");
        extraMethods.add("index");
    }
    
    private DomainClass cachedDomainClass;
    private PerProjectPluginCache pluginCache;
    private PerProjectMemberCache memberCache;
    private PerProjectControllerCache cache;

    ControllerClass(GroovyCompilationUnit unit) {
        super(unit);
        pluginCache = GrailsCore.get().connect(unit.getJavaProject().getProject(), PerProjectPluginCache.class);
        memberCache = GrailsCore.get().connect(unit.getJavaProject().getProject(), PerProjectMemberCache.class);
    }

    public GrailsElementKind getKind() {
        return GrailsElementKind.CONTROLLER_CLASS;
    }
    
    public DomainClass getDomainClass() {
        if (cachedDomainClass != null) {
            return cachedDomainClass;
        }
        String origName = unit.getElementName();
        int controllerIndex = origName.lastIndexOf(CONTROLLER);
        String domainName = origName.substring(0, controllerIndex) + ".groovy";
        String packageName = unit.getParent().getElementName();

        IJavaProject javaProject = unit.getJavaProject();
        GrailsProject gp = new GrailsProject(javaProject);
        cachedDomainClass = gp.getDomainClass(packageName, domainName);
        return cachedDomainClass;
    }

    public TagLibClass getTagLibClass() {
        String origName = unit.getElementName();
        int controllerIndex = origName.lastIndexOf(CONTROLLER);
        String tagLibName = origName.substring(0, controllerIndex) + "TagLib.groovy";
        String packageName = unit.getParent().getElementName();
        
        IJavaProject javaProject = unit.getJavaProject();
        GrailsProject gp = GrailsWorkspaceCore.get().create(javaProject);
        return gp.getTagLibClass(packageName, tagLibName);
    }
    
    public ServiceClass getServiceClass() {
        String origName = unit.getElementName();
        int controllerIndex = origName.lastIndexOf(CONTROLLER);
        String serviceName = origName.substring(0, controllerIndex) + "Service.groovy";
        String packageName = unit.getParent().getElementName();
        
        IJavaProject javaProject = unit.getJavaProject();
        GrailsProject gp = GrailsWorkspaceCore.get().create(javaProject);
        return gp.getServiceClass(packageName, serviceName);
    }
    

    public ControllerClass getControllerClass() {
        return this;
    }

    public Map<String, ClassNode> getExtraControllerReferences() {
        return memberCache.getExtraControllerReferences();
    }
    
    public boolean isSpecialMethodReference(String name) {
        return extraMethods.contains(name);
    }

    public TypeAndDeclaration lookupTypeAndDeclaration(ClassNode declaringType,
            String name, VariableScope scope) {
        
        // if this is a controller action, then must wrap the result in 
        // a special type that knows the return values of the action
        AnnotatedNode action = this.getControllerAction(name);
        if (action != null) {
            return new TypeAndDeclaration(new ControllerActionClass(name), action, 
                    getGroovyClass(), "Controller Action", 
                    // must override the default LOOSELY_INFERRED confidence
                    TypeConfidence.INFERRED);
        }
        
        // special controller references
        Map<String, ClassNode> extraControllerReferences = getExtraControllerReferences();
        ClassNode type = extraControllerReferences.get(name);
        if (type != null) {
            AnnotatedNode declaration;
            if (type.getName().equals(VariableScope.VOID_WRAPPER_CLASS_NODE)) {
                declaration = new MethodNode(name, Opcodes.ACC_PUBLIC, type, new Parameter[0], new ClassNode[0], null);
                declaration.setDeclaringClass(getGroovyClass());
            } else {
                declaration = new FieldNode(name, Opcodes.ACC_PUBLIC, type, getGroovyClass(), null);
                declaration.setDeclaringClass(getGroovyClass());
            }
            return new TypeAndDeclaration(type, declaration, declaringType);
        }
        
        
        // Only used in 1.3.7 and earlier
        Map<String, Set<ContributedMethod>> contributedMethods = pluginCache.getAllControllerMethods();
        if (contributedMethods.containsKey(name)) {
            ClassNode returnType = contributedMethods.get(name).iterator().next().getReturnType();
            if (returnType == null) {
                returnType = getGroovyClass();
                if (returnType == null) {
                    returnType = VariableScope.OBJECT_CLASS_NODE;
                }
            }
            return new TypeAndDeclaration(returnType == null ? getGroovyClass() : returnType, declaringType);
        }
        
        // Only used in 1.3.7 and earlier
        // now look at properties and methods contributed by plugins
        Map<String, ContributedProperty> contributedProperties = pluginCache.getAllControllerProperties();
        if (contributedProperties.containsKey(name)) {
            ClassNode contribType = contributedProperties.get(name).getType();
            if (contribType == null) {
                contribType = getGroovyClass();
                if (contribType == null) {
                    contribType = VariableScope.OBJECT_CLASS_NODE;
                }
            }
            return new TypeAndDeclaration(contribType, declaringType);
        }
        

        return null;
    }
    
    public Map<String, Set<ContributedMethod>> getAllContributedMethods() {
        return pluginCache.getAllControllerMethods();
    }

    public Map<String, ContributedProperty> getAllContributedProperties() {
        return pluginCache.getAllControllerProperties();
    }     

    
    public void initializeTypeLookup(VariableScope scope) {
        ClassNode node = getGroovyClass();
        if (node != null) {
            populateInjectedServices(scope);
            for (Entry<String, ClassNode> entry : memberCache.getExtraControllerReferences().entrySet()) {
                scope.addVariable(entry.getKey(), entry.getValue(), node);
            }
        }
    }

    public IFolder getGSPFolder() {
        DomainClass d = getDomainClass();
        return d != null ? d.getGSPFolder() : null;
    }

    public String getAssociatedDomainClassName() {
        String className = getClassName();
        int cIndex = className.lastIndexOf(CONTROLLER);
        className = className.substring(0, cIndex);
        return className;
    }

	public String getClassName() {
		return getGroovyClass().getName();
	}

	public String getLogicalName() {
		return GrailsNameUtils.getLogicalPropertyName(getClassName(), CONTROLLER);
	}

	public IType getType() throws JavaModelException {
		String className = getClassName();
		for (IType candidate : unit.getAllTypes()) {
			if (candidate.getFullyQualifiedName().equals(className)) {
				return candidate;
			}
		}
		return null;
	}
	
	/**
	 * Finds the controller action with the given name.  Looks for both method style and property style actions.
	 * @param name the action name
	 * @return {@link AnnotatedNode} corresponding to the action declaration, or null if not an action
	 */
	public AnnotatedNode getControllerAction(String name) {
	    List<MethodNode> methods = getGroovyClass().getMethods(name);
	    if (methods != null && methods.size() > 0) {
	        // arbitrarily choose first method.  I don't think it is possible to have overloaded controller actions
	        MethodNode method = methods.get(0);
    	    if (method != null && !method.isStatic() && method.getReturnType().equals(VariableScope.OBJECT_CLASS_NODE)) {
    	        return method;
    	    }
	    }
	    PropertyNode property = getGroovyClass().getProperty(name);
	    if (property != null && !property.isStatic() && !isServiceReference(property) && 
	            property.getType().equals(VariableScope.OBJECT_CLASS_NODE) && 
	            property.getInitialExpression() instanceof ClosureExpression) {
	        return property;
	    }
	    return null;
	}
	
	public boolean isControllerAction(String name) {
	    return getControllerAction(name) != null;
	}
}
