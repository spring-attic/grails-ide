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
package org.grails.ide.eclipse.editor.groovy.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.core.internal.plugins.IGrailsProjectInfo;

import org.grails.ide.eclipse.editor.groovy.elements.DomainClass;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;

/**
 * Manages the Named Criteria queries for domain classes in a project
 * @author Andrew Eisenberg
 * @created Nov 25, 2011
 */
public class PerProjectNamedQueriesHolder implements IGrailsProjectInfo {

    private static final String[] NO_QUERIES = new String[0];
    private IProject project;

    private Map<String, String[]> domainClassToQueries = new HashMap<String, String[]>();;
    
    public IProject getProject() {
        return project;
    }
    
    public void setProject(IProject project) {
        this.project = project;
    }

    public void projectChanged(GrailsElementKind[] changeKinds,
            IResourceDelta change) {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            boolean foundRelevantChange = false;
            for (GrailsElementKind changeKind : changeKinds) {
                if (changeKind == GrailsElementKind.PROJECT || 
                        changeKind == GrailsElementKind.CLASSPATH ||
                        changeKind == GrailsElementKind.DOMAIN_CLASS) {
                    foundRelevantChange = true;
                    break;
                }
            }
            
            if (foundRelevantChange) {
                domainClassToQueries.clear();
            }
        }
    }
    
    public String[] findNamedQueries(DomainClass domainClass) {
        if (domainClass.getGroovyClass() == null) {
            return NO_QUERIES;
        }
        String name = domainClass.getGroovyClass().getName();
        String[] queries = domainClassToQueries.get(name);
        if (queries == null) {
            queries = ensureInitialized(domainClass.getGroovyClass());
            synchronized (GrailsCore.get().getLockForProject(project)) {
                domainClassToQueries.put(name, queries);
            }
        }
        return queries;
    }
    
    private String[] ensureInitialized(ClassNode domainClass) {
        FieldNode field = domainClass.getField("namedQueries");
        if (field != null && field.isStatic()) {
            Expression initialExpression = field.getInitialExpression();
            if (initialExpression instanceof ClosureExpression) {
                Statement code = ((ClosureExpression) initialExpression).getCode();
                if (code instanceof BlockStatement) {
                    List<Statement> statements = ((BlockStatement) code).getStatements();
                    if (statements != null) {
                        List<String> namedQueries = new ArrayList<String>(statements.size());
                        for (Statement s : statements) {
                            if (s instanceof ExpressionStatement) {
                                Expression expr = ((ExpressionStatement) s).getExpression();
                                if (expr instanceof MethodCallExpression) {
                                    MethodCallExpression call = (MethodCallExpression) expr;
                                    namedQueries.add(call.getMethodAsString());
                                }
                            }
                        }
                        return namedQueries.toArray(NO_QUERIES);
                    }
                }
            }
        }
        return NO_QUERIES;
    }

    public void dispose() {
        project = null;
        domainClassToQueries = null;
    }

}
