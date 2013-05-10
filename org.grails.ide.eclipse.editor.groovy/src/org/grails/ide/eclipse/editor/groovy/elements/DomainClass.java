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
package org.grails.ide.eclipse.editor.groovy.elements;

import groovyjarjarasm.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.groovy.search.AbstractSimplifiedTypeLookup.TypeAndDeclaration;
import org.eclipse.jdt.groovy.search.VariableScope;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.core.internal.plugins.PerProjectPluginCache;
import org.grails.ide.eclipse.core.model.ContributedMethod;
import org.grails.ide.eclipse.core.model.ContributedProperty;
import org.grails.ide.eclipse.editor.groovy.types.DynamicFinderValidator;
import org.grails.ide.eclipse.editor.groovy.types.FinderValidatorFactory;
import org.grails.ide.eclipse.editor.groovy.types.PerProjectMemberCache;
import org.grails.ide.eclipse.editor.groovy.types.PerProjectNamedQueriesHolder;
import org.grails.ide.eclipse.editor.groovy.types.PerProjectTypeCache;


/**
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @author Nieraj Singh
 * @created Nov 23, 2009
 */
public class DomainClass extends AbstractGrailsElement implements INavigableGrailsElement {
    
    // these are extra references htat are only available in the mappings method
    private static final String[] MAPPINGS_FIELDS = new String[] { "tablePerHierarchy", "tablePerSubclass" /*, "id", "version"*/ }; // the version and id fields are inserted directly into the class, so no need to add it here
    private static final String[] CONTSTRAINTS_FIELDS = new String[] { "blank", "creditCard", "email", "inList", "length", "min", "minLength", "minSize", "matches", "max", "maxLength", "maxSize", "notEqual", "nullable", "range", "scale", "size", "unique", "url" };
    
    private static final String NAMED_CRITERIA_PROXY = "org.codehaus.groovy.grails.orm.hibernate.cfg.NamedCriteriaProxy";

    // these are default fields that can be created when in the SCRIPT context
    public static final String[] staticFields = new String[] {
        "belongsTo", // list
        "hasMany", // list
        "embedded", // list
        "transients", // list
        "mapping",  // closure
        "constraints", // closure
        "namedQueries", // closure
        "hasOne" // class
    };
    
    public class NamedQueryClassNode extends ClassNode {
        public NamedQueryClassNode() {
            super(NAMED_CRITERIA_PROXY, VariableScope.OBJECT_CLASS_NODE.getModifiers(), VariableScope.GROOVY_OBJECT_SUPPORT);
            this.isPrimaryNode = false;
            this.setRedirect(typeCache.getClassNode(NAMED_CRITERIA_PROXY));
            this.setGenericsPlaceHolder(true);
            GenericsType gt = new GenericsType();
            gt.setType(DomainClass.this.getGroovyClass());
            gt.setName(gt.getType().getName());
            this.setGenericsTypes(new GenericsType[] { gt });
        }
        
        public DomainClass getDomainClass() {
            return DomainClass.this;
        }
        /**
         * Search for other named queries and for dynamic finders
         * @param declaringType
         * @param name
         * @param scope
         * @return
         */
        public TypeAndDeclaration lookupTypeAndDeclaration(ClassNode declaringType,
                String name, VariableScope scope) {

            // dynamic finders
            DynamicFinderValidator internalFinderValidator = getFinderValidator();
            if (internalFinderValidator.isValidFinderName(name)) {
                // prefer the cached finder
                TypeAndDeclaration typeAndDeclaration = findCached(name);
                if (typeAndDeclaration != null) {
                    return typeAndDeclaration;
                }
                
                FieldNode field = internalFinderValidator.createFieldDeclaration(name);
                cacheGeneratedMember(field);
                return new TypeAndDeclaration(field.getType(), field, DomainClass.this.getGroovyClass(), "Dynamic finder");
            }
            
            // namedQueries
            for (String namedQuery : getNamedQueries()) {
                if (name.equals(namedQuery)) {
                    // prefer the cached query
                    TypeAndDeclaration typeAndDeclaration = findCached(name);
                    if (typeAndDeclaration != null) {
                        return typeAndDeclaration;
                    }
     
                    FieldNode criteria = createNamedCriteria(DomainClass.this.getGroovyClass(), namedQuery);
                    return new TypeAndDeclaration(criteria.getType(), criteria, DomainClass.this.getGroovyClass(), "Named Query");
                }
            }

            return null;
        }
        
    }
    
    private final PerProjectPluginCache pluginCache;
    private final PerProjectMemberCache memberCache;
    private final PerProjectTypeCache typeCache;
    
    private DynamicFinderValidator finderValidator;
    
    private List<PropertyNode> cachedDomainProperties = null;
    
    private String[] cachedNamedQueries = null;

    
    protected DomainClass(GroovyCompilationUnit unit) {
        super(unit);
        pluginCache = GrailsCore.get().connect(unit.getJavaProject().getProject(), PerProjectPluginCache.class);
        memberCache = GrailsCore.get().connect(unit.getJavaProject().getProject(), PerProjectMemberCache.class);
        typeCache = GrailsCore.get().connect(unit.getJavaProject().getProject(), PerProjectTypeCache.class);
    }

    public GrailsElementKind getKind() {
        return GrailsElementKind.DOMAIN_CLASS;
    }
    
    public ControllerClass getControllerClass() {
        String controllerName = getPrimaryTypeName() + "Controller.groovy";
        String packageName = unit.getParent().getElementName();
        
        IJavaProject javaProject = unit.getJavaProject();
        GrailsProject gp = GrailsWorkspaceCore.get().create(javaProject);
        return gp.getControllerClass(packageName, controllerName);
    }
    
    public TagLibClass getTagLibClass() {
        String tagLibName = getPrimaryTypeName() + "TagLib.groovy";
        String packageName = unit.getParent().getElementName();
        
        IJavaProject javaProject = unit.getJavaProject();
        GrailsProject gp = GrailsWorkspaceCore.get().create(javaProject);
        return gp.getTagLibClass(packageName, tagLibName);
    }
    
    public ServiceClass getServiceClass() {
        String serviceName = getPrimaryTypeName() + "Service.groovy";
        String packageName = unit.getParent().getElementName();
        
        IJavaProject javaProject = unit.getJavaProject();
        GrailsProject gp = GrailsWorkspaceCore.get().create(javaProject);
        return gp.getServiceClass(packageName, serviceName);
    }
    
    public DomainClass getDomainClass() {
        return this;
    }

    public List<PropertyNode> getDomainProperties() {
        if (cachedDomainProperties != null) {
            return cachedDomainProperties;
        }
        ClassNode clazz = getGroovyClass();
        if (clazz == null) {
            return Collections.emptyList();
        }
        
        cachedDomainProperties = new ArrayList<PropertyNode>();
        internalGetDomainProperties(clazz, cachedDomainProperties);
        return cachedDomainProperties;
    }

    public void internalGetDomainProperties(ClassNode clazz,
            List<PropertyNode> domainProperties) {
        List<PropertyNode> thisDomainProperties = clazz.getProperties();
        for (PropertyNode property : thisDomainProperties) {
            if (!isStatic(property) && ! property.getName().equals("metaClass")) {
                domainProperties.add(property);
            }
        }
        ClassNode superClass = clazz.getSuperClass();
        if (superClass != null && !superClass.getName().equals("java.lang.Object") && !superClass.getName().equals("groovy.lang.GroovyObjectSupport")) {
            internalGetDomainProperties(superClass, domainProperties);
        }
    }

    public boolean isStatic(PropertyNode property) {
        FieldNode field = property.getField();
        if (field != null) {
            return field.isStatic();
        }
        return property.isStatic();
    }
    
    // all the extra fields that can go in the mappings class
    public static String[] getMappingsFields() {
        return MAPPINGS_FIELDS;
    }
    
    public static String[] getContstraintsFields() {
        return CONTSTRAINTS_FIELDS;
    }
    
    // not quite right...need a way to specify that these fields come from grails in the assist window
    // also need to distinguish between lists and closures in the initialization area
    public List<String> getUnimplementedStaticFields() {
        List<String> unimplementedFields = new ArrayList<String>(staticFields.length);
        ClassNode clazz = getGroovyClass();
        if (clazz != null) {
            for (String field : staticFields) {
                if (!clazz.hasProperty(field)) {
                    unimplementedFields.add(field);
                }
            }
        }
        return unimplementedFields;
    }
    
    public boolean isMappingField(AnnotatedNode node) {
        if (node instanceof PropertyNode) {
            node = ((PropertyNode) node).getField();
        }
        return node instanceof FieldNode && 
            ((FieldNode) node).isStatic() && 
            ((FieldNode) node).getName().equals("mapping");
    }
    
    public boolean isConstraintsField(AnnotatedNode node) {
        if (node instanceof PropertyNode) {
            node = ((PropertyNode) node).getField();
        }
        return node instanceof FieldNode && 
        ((FieldNode) node).isStatic() && 
        ((FieldNode) node).getName().equals("constraints");
    }
    
    // this method is called if the target CU is a domain class
    public void initializeTypeLookup(VariableScope scope) {
        ClassNode node = getGroovyClass();
        if (node == null) return;
        populateInjectedServices(scope);
        FieldNode field = scope.getEnclosingFieldDeclaration();
        if (field != null) {
            if (isMappingField(field)) {
                for (String fieldName : MAPPINGS_FIELDS) {
                    scope.addVariable(fieldName, VariableScope.VOID_CLASS_NODE, node);
                }
            }
        }
    }

    // this method is called whenever the current type is a domain class
    public TypeAndDeclaration lookupTypeAndDeclaration(ClassNode declaringType,
            String name, VariableScope scope) {
        // first check to see if we've already come across this name 
        // before in the current Domain class
        TypeAndDeclaration typeAndDeclaration = findCached(name);
        if (typeAndDeclaration != null) {
            return typeAndDeclaration;
        }
        
        // static members
        Map<String, ClassNode[]> staticDomainMembers = memberCache.getStaticDomainMembers();
        // must check for containsKey since some of the values are purposely null
        if (staticDomainMembers.containsKey(name)) {
            ClassNode[] staticDomainTypes = staticDomainMembers.get(name);
            ClassNode retType;
            ClassNode inferredDeclingType;
            if (staticDomainTypes[0] == null) {
                retType = declaringType;
            } else if (staticDomainTypes[0].redirect() == VariableScope.LIST_CLASS_NODE) {
                retType = VariableScope.clone(staticDomainTypes[0]);
                GenericsType genericsType = retType.getGenericsTypes()[0];
                genericsType.setType(declaringType);
                genericsType.setName(declaringType.getName());
                genericsType.setUpperBounds(null);
                genericsType.setLowerBound(null);
            } else {
                retType = staticDomainTypes[0];
            }
            
            if (staticDomainTypes[1] == null) {
                inferredDeclingType = declaringType;
            } else {
                inferredDeclingType = staticDomainTypes[1];
            }
            FieldNode cached = new FieldNode(name, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, retType, inferredDeclingType, null);
            cached.setDeclaringClass(inferredDeclingType);
            cacheGeneratedMember(cached);
            return new TypeAndDeclaration(retType, cached, inferredDeclingType);
        }
        
        // dynamic finder prefixes
        // must do a containsKey() first because some of the values are null
        Map<String, ClassNode> dynamicDomainMembers = memberCache.getDynamicDomainMembers();
        if (dynamicDomainMembers.containsKey(name)) {
            ClassNode dynamicDomainType = dynamicDomainMembers.get(name);
            if (dynamicDomainType == null) {
                dynamicDomainType = declaringType;
            } else if (dynamicDomainType.redirect() == VariableScope.LIST_CLASS_NODE) {
                dynamicDomainType = VariableScope.clone(dynamicDomainType);
                GenericsType genericsType = dynamicDomainType.getGenericsTypes()[0];
                genericsType.setType(declaringType);
                genericsType.setName(declaringType.getName());
                genericsType.setUpperBounds(null);
                genericsType.setLowerBound(null);
            }
                
            FieldNode cached = new FieldNode(name, Opcodes.ACC_PUBLIC, dynamicDomainType, declaringType, null);
            cached.setDeclaringClass(declaringType);
            cacheGeneratedMember(cached);
            return new TypeAndDeclaration(dynamicDomainType, cached);
        }
        
        // non-static members
        Map<String, ClassNode> nonstaticDomainMembers = memberCache.getNonstaticDomainMembers();
        ClassNode nonStaticType = nonstaticDomainMembers.get(name);
        if (nonStaticType != null) {
            FieldNode cached = new FieldNode(name, Opcodes.ACC_PUBLIC, nonStaticType, declaringType, null);
            cached.setDeclaringClass(declaringType);
            cacheGeneratedMember(cached);
            return new TypeAndDeclaration(nonStaticType, cached);
        }
        
        // look for dynamic finders
        DynamicFinderValidator internalFinderValidator = getFinderValidator();
        if (internalFinderValidator.isValidFinderName(name)) {
            FieldNode field = internalFinderValidator.createFieldDeclaration(name);
            cacheGeneratedMember(field);
            return new TypeAndDeclaration(field.getType(), field, declaringType, "Dynamic finder");
        }
        
        // look for namedQueries
        for (String namedQuery : getNamedQueries()) {
            if (name.equals(namedQuery)) {
                FieldNode criteria = createNamedCriteria(declaringType, namedQuery);
                return new TypeAndDeclaration(criteria.getType(), criteria, declaringType, "Named Query");
            }
        }
        
        // now look at properties and methods contributed by plugins
        Map<String, ContributedProperty> contributedProperties = pluginCache.getAllDomainProperties();
        if (contributedProperties.containsKey(name)) {
            ClassNode type = contributedProperties.get(name).getType();
            return new TypeAndDeclaration(type == null ? declaringType : type, declaringType);
        }
        Map<String, Set<ContributedMethod>> contributedMethods = pluginCache.getAllDomainMethods();
        if (contributedMethods.containsKey(name)) {
            ClassNode returnType = contributedMethods.get(name).iterator().next().getReturnType();
            return new TypeAndDeclaration(returnType == null ? declaringType : returnType, declaringType);
        }
        
        // now check to see if we are in the constraints field
        if (isConstraintsField(scope.getEnclosingFieldDeclaration())) {
            for (PropertyNode domainProp : getDomainProperties()) {
                if (domainProp.getName().equals(name)) {
                    return new TypeAndDeclaration(VariableScope.VOID_CLASS_NODE, domainProp, declaringType, "Constraints property");
                }
            }
        }
        
        return null;
    }

    public TypeAndDeclaration findCached(String name) {
        TypeAndDeclaration typeAndDeclaration = null;
        AnnotatedNode cached = getCachedMember(name);
        if (cached != null) {
            ClassNode type;
            ClassNode inferredDeclaringType = cached.getDeclaringClass();
            if (cached instanceof FieldNode) {
                type = ((FieldNode) cached).getType();
            } else if (cached instanceof MethodNode) {
                type = ((MethodNode) cached).getReturnType();
            } else {
                type = getGroovyClass();
            }
            typeAndDeclaration = new TypeAndDeclaration(type, cached, inferredDeclaringType);
        }
        return typeAndDeclaration;
    }
    
    

    public FieldNode createNamedCriteria(ClassNode declaringType,
            String namedQuery) {
        ClassNode criteriaType = getNamedQueryProxyType(namedQuery);
        FieldNode criteria = new FieldNode(namedQuery, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, criteriaType, declaringType, null);
        criteria.setDeclaringClass(declaringType);
        cacheGeneratedMember(criteria);
        return criteria;
    }

    private ClassNode getNamedQueryProxyType(String namedQuery) {
        return new NamedQueryClassNode();
    }
    
    public Map<String, Set<ContributedMethod>> getAllContributedMethods() {
       return pluginCache.getAllDomainMethods(); 
    }
    
    public Map<String, ContributedProperty> getAllContributedProperties() {
        return pluginCache.getAllDomainProperties(); 
    }
    
    public Map<String, ClassNode[]> getStaticMembers() {
        return memberCache.getStaticDomainMembers();
    }
    public Map<String, ClassNode> getDynamicFinderMembers() {
        return memberCache.getDynamicDomainMembers();
    }
    public Map<String, ClassNode> getNonstaticMembers() {
        return memberCache.getNonstaticDomainMembers();
    }
    
    public static boolean isFieldReference(String name) {
        return name.equals("contraints") ||
               name.equals("properties") ||
               name.equals("errors") ||
               name.equals("id");
    }

    public IFolder getGSPFolder() {
        // project name/grails-app/views/domainClassName/elementName.gsp
        StringBuilder sb = new StringBuilder();
        sb.append(unit.getJavaProject().getElementName()).append("/grails-app/views/");
        sb.append(gspFolderName());
        IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path(sb.toString()));
        return folder;
    }
    
    private String gspFolderName() {
        String name = unit.getElementName();
        int dotIndex = name.indexOf(".");
        if (dotIndex > 0) {
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1, dotIndex);
            return name;
        } else {
            return name;
        }
    }

    public String getAssociatedDomainClassName() {
        String className = getGroovyClass().getName();
        return className;
    }

    public String[] getNamedQueries() {
        if (cachedNamedQueries == null) {
            PerProjectNamedQueriesHolder namedQueriesCache = GrailsCore.get().connect(unit.getJavaProject().getProject(), PerProjectNamedQueriesHolder.class);
            cachedNamedQueries = namedQueriesCache.findNamedQueries(this);
        }
        return cachedNamedQueries;
    }

    public DynamicFinderValidator getFinderValidator() {
        if (finderValidator == null) {
            finderValidator = new FinderValidatorFactory().createValidator(getDomainClass());
        }
        return finderValidator;
    }
}
