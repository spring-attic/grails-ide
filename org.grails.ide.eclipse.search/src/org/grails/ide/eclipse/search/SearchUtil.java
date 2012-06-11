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
package org.grails.ide.eclipse.search;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.QuerySpecification;

/**
 * @author Kris De Volder
 *
 * @since 2.9
 */
public class SearchUtil {

	/**
	 * @return true if the query is asking for results to include references.
	 */
	public static boolean wantsReferences(ElementQuerySpecification q) {
		int limit = q.getLimitTo();
		return limit == IJavaSearchConstants.REFERENCES || limit == IJavaSearchConstants.ALL_OCCURRENCES;
	}

	/**
	 * For a call of the form 
	 *   <exp>(..., <name>:<value>)
	 *retrieves the <key>:<value> node with given name.
	 *@return the value or null if the call doesn't follow the expected pattern or doesn't have an argument with that
	 *name.
	 */
	public static MapEntryExpression getNamedArgument(MethodCallExpression call, String name) {
		Expression args = call.getArguments();
		if (args!=null && args instanceof TupleExpression) {
			TupleExpression argsTuple = (TupleExpression) args;
			List<Expression> exps = argsTuple.getExpressions();
			if (exps!=null && exps.size()==1) {
				Expression theArg = exps.get(0);
				if (theArg instanceof NamedArgumentListExpression) {
					NamedArgumentListExpression namedArgs = (NamedArgumentListExpression) theArg;
					for (MapEntryExpression pair : namedArgs.getMapEntryExpressions()) {
						String key = SearchUtil.getStringValue(pair.getKeyExpression());
						if (name.equals(key)) {
							return pair; //Found!
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * @return the value of an expression as a String, if the expression is a String constant.
	 *  Otherwise returns null.
	 */
	public static String getStringValue(Expression exp) {
		if (exp instanceof ConstantExpression) {
			ConstantExpression konst = (ConstantExpression) exp;
			Object value = konst.getValue();
			if (value instanceof String) {
				return (String) value;
			}
		}
		return null;
	}

	public static QuerySpecification createReferencesQuery(IJavaElement element)
			throws JavaModelException {
		//This code based on code in org.eclipse.jdt.ui.actions.FindReferencesAction.createQuery(IJavaElement)
		//So it emulates the behavior of query created by the 'References' search in the JDT UI.
		JavaSearchScopeFactory factory= JavaSearchScopeFactory.getInstance();
		boolean isInsideJRE = factory.isInsideJRE(element);
		IJavaSearchScope scope= factory.createWorkspaceScope(isInsideJRE);
		String description= factory.getWorkspaceScopeDescription(isInsideJRE);
		return new ElementQuerySpecification(element, IJavaSearchConstants.REFERENCES , scope, description);
	}

	public static ClosureExpression getClosureValue(FieldNode node) {
		Expression valueExp = node.getInitialExpression();
		if (valueExp instanceof ClosureExpression) {
			return (ClosureExpression) valueExp;
		}
		return null;
	}

	public static Expression getNamedArgumentWithValue(MethodCallExpression call, String argName, String expectedValue) {
		MapEntryExpression arg = SearchUtil.getNamedArgument(call, argName);
		if (arg!=null) {
			final Expression valueExpression = arg.getValueExpression();
			String argValue = SearchUtil.getStringValue(valueExpression);
			if (expectedValue.equals(argValue)) {
				return valueExpression;
			}
		}
		return null;
	}

	public static List<Statement> getStatements(ClosureExpression closure) {
		List<Statement> result = new ArrayList<Statement>();
		collectStatements(closure.getCode(), result);
		return result;
	}

	private static void collectStatements(Statement code, List<Statement> result) {
		if (code instanceof BlockStatement) {
			BlockStatement block = (BlockStatement) code;
			for (Statement statement : ((BlockStatement) code).getStatements()) {
				collectStatements(statement, result);
			}
		} else {
			result.add(code);
		}
	}

}
