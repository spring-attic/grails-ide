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
package org.grails.ide.eclipse.editor.groovy.controllers;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.groovy.search.ITypeRequestor;
import org.eclipse.jdt.groovy.search.TypeInferencingVisitorFactory;
import org.eclipse.jdt.groovy.search.TypeInferencingVisitorWithRequestor;
import org.eclipse.jdt.groovy.search.TypeLookupResult;
import org.grails.ide.eclipse.core.GrailsCoreActivator;

/**
 * Keeps track of a single controller class, all of its actions, and the names/inferred types of the return values
 * @author Andrew Eisenberg
 * @since 2.7.0
 */
public class ControllerCache {
    
    private class ActionReturnValueRequestor implements ITypeRequestor {
        private final IMember targetMember;
        
        private MapExpression targetMapExpression;
        
        Map<String, ClassNode> returnValues;
        List<String> redirects = null;  // FIXADE should be Set
        private Map<Expression, String> reverse;

        public ActionReturnValueRequestor(IMember targetMember, Map<String, ClassNode> returnValues) {
            this.targetMember = targetMember;
            this.returnValues = returnValues;
        }
        
        public VisitStatus acceptASTNode(ASTNode node, TypeLookupResult result,
                IJavaElement enclosingElement) {
            if (enclosingElement.getElementType() != targetMember.getElementType()) {
                return VisitStatus.CONTINUE;
            }
            if (!enclosingElement.equals(targetMember)) {
                return VisitStatus.CANCEL_MEMBER;
            }
            
            
            // assume only one return statement
            if (node instanceof ReturnStatement) {
                Expression returnExpr = ((ReturnStatement) node).getExpression();
                if (returnExpr instanceof MapExpression && ((MapExpression) returnExpr).getMapEntryExpressions() != null) {
                    targetMapExpression = (MapExpression) returnExpr;
                    reverse = new HashMap<Expression, String>(targetMapExpression.getMapEntryExpressions().size()*2);
                    for (MapEntryExpression expr : targetMapExpression.getMapEntryExpressions()) {
                        Expression valueExpression = expr.getValueExpression();
                        // navigate down to the actual expression that we are interested in
                        while (valueExpression instanceof MethodCallExpression || valueExpression instanceof PropertyExpression) {
                            if (valueExpression instanceof MethodCallExpression) {
                                valueExpression = ((MethodCallExpression) valueExpression).getMethod();
                            } else if (valueExpression instanceof PropertyExpression) {
                                valueExpression = ((PropertyExpression) valueExpression).getProperty();
                            }
                        }
                        
                        reverse.put(valueExpression, expr.getKeyExpression().getText());
                    }
                }
            } else if (node instanceof MethodCallExpression) {
                MethodCallExpression call = (MethodCallExpression) node;
                if (call.getMethodAsString().equals("redirect")) {
                    // remember the redirect call so that we can traverse the redirect target later
                    Expression args = call.getArguments();
                    if (args instanceof TupleExpression && ((TupleExpression) args).getExpression(0) instanceof NamedArgumentListExpression) {
                        NamedArgumentListExpression named = (NamedArgumentListExpression) ((TupleExpression) args).getExpression(0);
                        MapEntryExpression entry = named.getMapEntryExpressions().get(0);
                        if (entry.getKeyExpression().getText().equals("action")) {
                            if (redirects == null) {
                                redirects = new LinkedList<String>();
                            }
                            redirects.add(entry.getValueExpression().getText());
                        }
                    }
                }
            } else if (targetMapExpression != null) {
                String currentValue = reverse.get(node);
                if (currentValue != null) {
                    returnValues.put(currentValue, result.type);
                }
            }
            
            return VisitStatus.CONTINUE;
        }
        
    }
    
    private final GroovyCompilationUnit controllerUnit;
    private final IType controllerType;
    
    private Map<String, Map<String, ClassNode>> actionToReturnValue = new HashMap<String, Map<String, ClassNode>>();
    
    public ControllerCache(GroovyCompilationUnit controllerUnit) {
        this.controllerUnit = controllerUnit;
        this.controllerType = controllerUnit.findPrimaryType();
    }

    /**
     * Returns all the return parameters and inferred types for this controller action.  Will return null
     * if the aciton name cannot be found.
     * @param actionName
     * @return
     */
    public Map<String, ClassNode> findReturnValuesForAction(String actionName) {
        Map<String, ClassNode> returnValues = actionToReturnValue.get(actionName);
        if (returnValues == null) {
            returnValues = calculateValuesForAction(actionName);
        }
        return returnValues;
    }

    /**
     * Here is where the real work gets done.
     * 1. get the module node
     * 2. find the relevant action name
     * 3. run the inferencing engine down that action
     * 4. make assumption that the last return statement is the one we are interested in
     *    assume that it will be in the form of a {@link MapEntryExpression}.
     *    assume that the keys are strings and the values have inferred types.
     * @param actionName
     * @return
     */
    private Map<String, ClassNode> calculateValuesForAction(String actionName) {
        if (controllerType == null || !controllerType.exists()) {
            return Collections.emptyMap();
        }
        
        Map<String, ClassNode> existing = new HashMap<String, ClassNode>();
        actionToReturnValue.put(actionName, existing);
        
        // Here is a little problem.
        // in Grails 1.3.7 and earlier, controller actions were defined as fields with closure initializers
        // in Grails 2.0 and later, they are no-arg methods.
        // Try looking for both kinds here.  First look for method, and if not found, then go for field.
        // If a controller class has both, then that's a problem.
        IMember actionMember = findMember(actionName);
        if (actionMember == null) {
            return Collections.emptyMap();
        }
        
        ActionReturnValueRequestor requestor = new ActionReturnValueRequestor(actionMember, existing);
        TypeInferencingVisitorWithRequestor visitor = new TypeInferencingVisitorFactory().createVisitor(controllerUnit);
        visitor.visitCompilationUnit(requestor);

        if (requestor.redirects != null) {
            for (String redirectedAction : requestor.redirects) {
                existing.putAll(findReturnValuesForAction(redirectedAction));
            }
        }        
        if (requestor.returnValues != null) {
            existing.putAll(requestor.returnValues);
        }
        return existing;
    }

    /**
     * Finds the associated {@link IJavaElement} for this action name.
     * @param actionName
     * @return
     */
    private IMember findMember(String actionName) {
        // first go for method...can have arbitrary number of parameters
        IMember member;
        try {
            IMethod[] allMethods = controllerType.getMethods();
            member = null;
            for (IMethod maybeMethod : allMethods) {
                if (maybeMethod.getElementName().equals(actionName)) {
                    member = maybeMethod;
                    break;
                }
            }
            if (member != null) {
                return member;
            }
        } catch (JavaModelException e) {
            GrailsCoreActivator.log(e);
        }
        member = controllerType.getField(actionName);
        if (member.exists()) {
            return member;
        }
        return null;
    }
}
