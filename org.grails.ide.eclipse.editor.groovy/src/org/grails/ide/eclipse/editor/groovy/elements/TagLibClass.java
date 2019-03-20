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
package org.grails.ide.eclipse.editor.groovy.elements;

import groovyjarjarasm.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.groovy.search.AbstractSimplifiedTypeLookup.TypeAndDeclaration;
import org.eclipse.jdt.groovy.search.VariableScope;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.editor.groovy.types.PerProjectMemberCache;


/**
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @author Nieraj Singh
 * @created Dec 4, 2009
 */
public class TagLibClass extends AbstractGrailsElement implements INavigableGrailsElement {
    
    public static final String DEFAULT_NAMESPACE = "g"; //$NON-NLS-1$
    
    private class AttributeCollector extends CodeVisitorSupport {
        Set<String> attrs;
        
        Set<String> findAttrs(ClosureExpression c) {
            attrs = new HashSet<String>();
            c.visit(this);
            return attrs;
        }
        
        /**
         * Finds attributes that look like:
         * attrs.myAttr
         */
        @Override
        public void visitPropertyExpression(PropertyExpression expression) {
            boolean isAttr = false;
            Expression objectExpression = expression.getObjectExpression();
            if (objectExpression instanceof Variable) {
                isAttr = ((Variable) objectExpression).getName().equals("attrs"); //$NON-NLS-1$
            } else if (objectExpression instanceof ConstantExpression) {
                isAttr = ((ConstantExpression) objectExpression).getText().equals("attrs"); //$NON-NLS-1$
            }
            if (isAttr) {
                Expression propertyExpression = expression.getProperty();
                if (propertyExpression instanceof ConstantExpression) {
                    attrs.add(((ConstantExpression) propertyExpression).getText());
                } else if (propertyExpression instanceof MethodCallExpression) {
                }
            }
            super.visitPropertyExpression(expression);
        }
        
        /**
         * Finds attributes that look like:
         * attrs.get('myAttr') or attrs.remove('myAttr')
         */
        @Override
        public void visitMethodCallExpression(MethodCallExpression call) {
            super.visitMethodCallExpression(call);
            String methodName = call.getMethodAsString();
            
            // check to see if we have a get or remove
            if ("get".equals(methodName) || "remove".equals(methodName)) { //$NON-NLS-1$ //$NON-NLS-2$
                Expression expr = call.getArguments();
                
                // attempt to find the first argument
                Expression firstArg = null;
                if (expr != null) {
                    if (expr instanceof ArgumentListExpression) {
                        ArgumentListExpression args = (ArgumentListExpression) expr;
                        if (args.getLength() > 0) {
                            firstArg = args.getExpression(0);
                        }
                    } else {
                        firstArg = expr;
                    }
                }
                
                if (firstArg instanceof ConstantExpression) {
                    ConstantExpression constArg = (ConstantExpression) firstArg;
                    attrs.add(constArg.getText());
                }
            }

        }
        
        /**
         * Finds attributes that look like:
         * attrs['myAttr']
         */
        @Override
        public void visitBinaryExpression(BinaryExpression expression) {
            boolean isAttr = false;
            Expression objectExpression = expression.getLeftExpression();
            if (objectExpression instanceof Variable) {
                isAttr = ((Variable) objectExpression).getName().equals("attrs"); //$NON-NLS-1$
            } else if (objectExpression instanceof ConstantExpression) {
                isAttr = ((ConstantExpression) objectExpression).getText().equals("attrs"); //$NON-NLS-1$
            }
            if (isAttr) {
                Expression propertyExpression = expression.getRightExpression();
                if (propertyExpression instanceof ConstantExpression) {
                    attrs.add(((ConstantExpression) propertyExpression).getText());
                }
            }
            super.visitBinaryExpression(expression);
        }
    }

    private String namespace;
    
    protected IField[] cachedFields;
    
    public TagLibClass(GroovyCompilationUnit unit) {
        super(unit);
    }
    
    
    public DomainClass getDomainClass() {
        String origName = unit.getElementName();
        int tagLibIndex = origName.lastIndexOf("TagLib"); //$NON-NLS-1$
        String domainName = origName.substring(0, tagLibIndex) + ".groovy"; //$NON-NLS-1$
        String packageName = unit.getParent().getElementName();

        IJavaProject javaProject = unit.getJavaProject();
        GrailsProject gp = new GrailsProject(javaProject);
        return gp.getDomainClass(packageName, domainName);
    }

    public ControllerClass getControllerClass() {
        String origName = unit.getElementName();
        return ControllerClass.getControllerClassForElement(unit, origName.substring(0,origName.lastIndexOf("TagLib")));
    }
    
    public ServiceClass getServiceClass() {
    	String origName = unit.getElementName();
        return ServiceClass.getServiceClassForElement(unit, origName.substring(0,origName.lastIndexOf("TagLib")));
    }
    
    public TestClass getTestClass() {
    	return TestClass.getTestClassForElement(this, unit, getPrimaryTypeName());
    }
    
    public TagLibClass getTagLibClass() {
        return this;
    }


    public GrailsElementKind getKind() {
        return GrailsElementKind.TAGLIB_CLASS;
    }

    public void initializeTypeLookup(VariableScope scope) {
        populateInjectedServices(scope);
    }

    public TypeAndDeclaration lookupTypeAndDeclaration(ClassNode declaringType,
            String name, VariableScope scope) {
        // static members
        PerProjectMemberCache memberCache = GrailsCore.get().connect(unit.getJavaProject().getProject(), PerProjectMemberCache.class);
        Map<String, ClassNode> members = memberCache.getTagLibMembers();
        ClassNode node = members.get(name);
        if (node != null) {
            AnnotatedNode cached = getCachedMember(name);
            if (cached == null) {
                cached = new FieldNode(name, Opcodes.ACC_PUBLIC, node, declaringType, null);
                cached.setDeclaringClass(declaringType);
                super.cacheGeneratedMember(cached);
            }
            return new TypeAndDeclaration(node, cached);
        }
        
        return null;
    }

    public Map<String, ClassNode> getTagLibMembers() {
        PerProjectMemberCache memberCache = GrailsCore.get().connect(unit.getJavaProject().getProject(), PerProjectMemberCache.class);
        return memberCache.getTagLibMembers();
    }
    
    
    /**
     * Find all attributes used by this tag.
     * May not be able to find all tags.  Will
     * only be able to find tags that are explicitly 
     * referenced by the attr parameter.
     * <br><br>
     * Assumes that this field is a member of this tag lib
     * and is itself a tag definition
     * @param tag
     * @return a collection of attribute names used by this tag
     */
    public Collection<String> getAttributesForTag(FieldNode tag) {
        ClosureExpression c = (ClosureExpression) tag.getInitialExpression();
        return new AttributeCollector().findAttrs(c);
    }
    
    @SuppressWarnings({ "nls", "cast" })
    public List<FieldNode> getAllTagFields() {
        ClassNode groovyClass = getGroovyClass();
        if (groovyClass != null) {
            List<FieldNode> fields = (List<FieldNode>) groovyClass.getFields();
            List<FieldNode> tagFields = new ArrayList<FieldNode>(fields.size()); 
            for (FieldNode field : fields) {
                if (!field.isStatic() && field.hasInitialExpression()) {
                    Expression expr = field.getInitialExpression();
                    if (expr instanceof ClosureExpression) {
                        ClosureExpression c = (ClosureExpression) expr;
                        Parameter[] params = getParameters(c);
                        // tags can have 0, 1, or 2 arguments.
                        // the first argument if it exists must be attrs
                        // the second argument, if it exists must be body
                        // order does not matter
                        if (params.length == 0) { 
                            tagFields.add(field);
                        } else if (params.length == 1 && params[0].getName().equals("attrs")) {
                            tagFields.add(field);
                        } else if (params.length == 2 && (
                                (params[0].getName().equals("attrs") && params[1].getName().equals("body")) ||
                                (params[0].getName().equals("body") && params[1].getName().equals("attrs")) )) {
                            tagFields.add(field);
                        }
                    }
                }
            }
            return tagFields;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * @param c
     * @return
     */
    private Parameter[] getParameters(ClosureExpression c) {
        return c.getParameters() == null ? new Parameter[0] : c.getParameters();
    }
    
    @SuppressWarnings({ "nls", "cast" })
    public String getNamespace() {
        if (namespace == null) {
            ClassNode tagClass = getGroovyClass();
            if (tagClass == null) {
                // something bad happened
                return DEFAULT_NAMESPACE;
            }
            FieldNode field = tagClass.getField("namespace");
            if (field != null && field.isStatic()) {
                // often the initializer for static fields is moved to clinit
                if (field.hasInitialExpression()) {
                    Expression expr = field.getInitialExpression();
                    if (expr instanceof ConstantExpression) {
                        namespace = ((ConstantExpression) expr).getText();
                    }
                } else {
                    MethodNode clinit = tagClass.getMethod("<clinit>", new Parameter[0]);
                    if (clinit != null) {
                        Statement block = clinit.getCode();
                        if (block instanceof BlockStatement) {
                            for (Statement state : (Iterable<Statement>) ((BlockStatement) block).getStatements()) {
                                if (state instanceof ExpressionStatement) {
                                    Expression exprStat = ((ExpressionStatement) state).getExpression();
                                    if (exprStat instanceof BinaryExpression) {
                                        BinaryExpression binaryExpr = (BinaryExpression) exprStat;
                                        if (binaryExpr.getOperation().getType() == Types.EQUALS && isNamespaceReference(binaryExpr.getLeftExpression()) && binaryExpr.getRightExpression() instanceof ConstantExpression) {
                                            namespace = ((ConstantExpression) binaryExpr.getRightExpression()).getText();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (namespace == null) {
                namespace = DEFAULT_NAMESPACE;
            }
        }
        return namespace;
    }
    
    private boolean isNamespaceReference(Expression expr) {
        if (expr instanceof VariableExpression) {
            VariableExpression var = (VariableExpression) expr;
            return var.getName().equals("namespace"); //$NON-NLS-1$
        } else if (expr instanceof FieldExpression) {
            FieldExpression fieldExpre = (FieldExpression) expr;
            return fieldExpre.getField().getName().equals("namespace"); //$NON-NLS-1$
        } else {
            return false;
        }
        
    }
    
    public String getBaseLocation() {
        IResource resource = getCompilationUnit().getResource();
        if (resource == null) {
            resource = getCompilationUnit().getJavaProject().getResource();
        }
        return resource.getLocation().toOSString();
    }
    public String getUri() {
        IResource resource = getCompilationUnit().getResource();
        if (resource == null) {
            resource = getCompilationUnit().getJavaProject().getResource();
        }
        return resource.getLocationURI().toString();
    }
    
    public IField getTagField(String fieldName) {
        if (cachedFields == null) {
            initializeCachedFields();
        }
        if (cachedFields != null) {
            for (IField tagField : cachedFields) {
                if (tagField.getElementName().equals(fieldName)) {
                    return tagField;
                }
            }
        }
        return null;
    }


    /**
     * 
     */
    protected void initializeCachedFields() {
        try {
            cachedFields = unit.getType(this.getPrimaryTypeName()).getFields();
        } catch (JavaModelException e) {
            GrailsCoreActivator.log(e);
        }
    }
    
    public IFolder getGSPFolder() {
        DomainClass d = getDomainClass();
        return d != null ? d.getGSPFolder() : null;
    }
    
    public String getAssociatedDomainClassName() {
        String className = getGroovyClass().getName();
        int cIndex = className.lastIndexOf("TagLib");
        className = className.substring(0, cIndex);
        return className;
    }
    
    /**
     * Finds a corresponding TagLib class for the given type name
     * @param unit {@link ICompilationUnit} of the original class
     * @param typeName simple name of the original class
     * @return a corresponding TagLib class
     */
    public static TagLibClass getTagLibClassForElement(ICompilationUnit unit, String typeName) {
		String controllerName = typeName + "TagLib.groovy"; //$NON-NLS-1$
		String packageName = unit.getParent().getElementName();

		IJavaProject javaProject = unit.getJavaProject();
		GrailsProject gp = GrailsWorkspaceCore.get().create(javaProject);
		return gp.getTagLibClass(packageName, controllerName);
	}
}
