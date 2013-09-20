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
import java.util.Map;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.eclipse.core.compiler.GroovySnippetCompiler;
import org.codehaus.groovy.eclipse.core.model.GroovyProjectFacade;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.groovy.search.VariableScope;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.core.internal.plugins.IGrailsProjectInfo;


/**
 * Caches {@link ClassNode}s that are generally needed by the project.
 * The cache is refreshed whenever the classpath changes
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Jan 28, 2010
 */
public class PerProjectTypeCache implements IGrailsProjectInfo {
	
	private final Map<String, ClassNode> classNodeCache = new HashMap<String, ClassNode>();
	
	private IProject project;
	
	private GroovySnippetCompiler snippetCompiler;
	
	
    public void dispose() {
        synchronized (GrailsCore.get().getLockForProject(project)) {
            project = null;
            snippetCompiler.cleanup();
            snippetCompiler = null;
            classNodeCache.clear();
        }
    }

    public IProject getProject() {
        return project;
    }

    /**
     * The cache is flushed when the classpath changes or there is a refresh dependencies
     * Cache is selectively flushed when a service class changes
     */
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
        		classNodeCache.clear();
        		// force resetting of the snippet compiler
        		setProject(project);
        	}
        }
    }
    
    public void clearFromCache(String fullyQualifiedName) {
//        classNodeCache.remove(fullyQualifiedName);
        classNodeCache.clear();
        // force resetting of the snippet compiler
        setProject(project);
    }

    public void setProject(IProject project) {
        this.project = project;
        GroovyProjectFacade groovyProject = new GroovyProjectFacade(JavaCore.create(project));
        if (snippetCompiler != null) {
        	snippetCompiler.cleanup();
        }
        snippetCompiler = new GroovySnippetCompiler(groovyProject);
    }

    /**
     * Returns a ClassNode for the given qualified name.
     * If the class is not found, then Object is returned.
     * If class is found, then cache the class and return it
     * 
     * @param qualifiedName
     * @return a {@link ClassNode} for the given qualified name, or {@link Object} 
     * if not found.
     */
    public ClassNode getClassNode(String qualifiedName) {
        synchronized (GrailsCore.get().getLockForProject(project)) {
        	ClassNode node = classNodeCache.get(qualifiedName);
        	if (node == null) {
        	    node = createClassNode(qualifiedName);
        	    if (node != null) {
        	        classNodeCache.put(qualifiedName, node);
        	    } else {
        	        // will be null if cache is not initialized
        	        // or if class can't be found.
        	        node = VariableScope.OBJECT_CLASS_NODE;
        	    }
        	}
        	return node;
        }
    }

    /**
     * Attempt to find a reference to this qualified name in the project
     * Does not cache
     * @param qualifiedName the fully qualified name of the type to find
     * @return a {@link ClassNode} for this qualified name, or null
     * if not found.
     */
    public ClassNode createClassNode(String qualifiedName) {
        if (snippetCompiler == null) {
            // not initialized
            return null;
        }
        
        // FIXADE I don't *think* we need this any more since we are using a 
        // SearchableEnvironment for the snippet compiler.
//        Job[] jobs = Job.getJobManager().find(ResourcesPlugin.FAMILY_AUTO_BUILD);
//        for (Job job : jobs) {
//            int i = 0;
//            while (job.getState() == Job.RUNNING && i++ < 8) {
//                System.out.println("Build job running...");
//                try {
//                    synchronized (this) {
//                        wait(1000);
//                    }
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
        
        try {
            ModuleNode module = snippetCompiler.compile(qualifiedName);
            BlockStatement statements = module.getStatementBlock();
            if (statements != null && statements.getStatements() != null && statements.getStatements().size() == 1) {
                Statement s = statements.getStatements().get(0);
                Expression expr;
                if (s instanceof ReturnStatement) {
                     expr = ((ReturnStatement) s).getExpression();
                } else if (s instanceof ExpressionStatement) {
                    expr = ((ExpressionStatement) s).getExpression();
                } else {
                    expr = null;
                }
                
                if (expr instanceof ClassExpression) {
                    ClassNode node = ((ClassExpression) expr).getType();
                    return node;
                }
            }
        } catch (Exception e) {
            GrailsCoreActivator.log("Exception compiling snippet " + qualifiedName, e); //$NON-NLS-1$
        }
        return null;
    }

    /**
     * Creates a groovy ClassNode from the source code passed in by the compilation unit.
     * Assumes that the desired class name matches the name of the compilation unit.
     * Assumes this is a groovy file.
     * @param unit create a {@link ClassNode} for this compilation unit
     * @return the parsed class node or null if not found or there is a parsing error
     */
    public ClassNode createClassNodeFromSource(ICompilationUnit unit) {
        if (snippetCompiler == null) {
            // not initialized
            return null;
        }
        String contents = String.valueOf(((CompilationUnit) unit).getContents());
        ModuleNode module = snippetCompiler.compile(contents);
        String primaryClassName = unit.getElementName();
        int nameEnd = primaryClassName.indexOf('.');
        primaryClassName = primaryClassName.substring(0, nameEnd);
        for (ClassNode clazz : module.getClasses()) {
            if (primaryClassName.equals(clazz.getNameWithoutPackage())) {
                return clazz;
            }
        }
        
        return module.getScriptClassDummy();
    }
}
