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

import java.util.HashSet;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.search.ui.text.Match;
import org.grails.ide.eclipse.core.GrailsCoreActivator;


/**
 * Abstract superclass for code visitors that are used to find stuff in Groovy AST.
 * 
 * @author Kris De Volder
 *
 * @since 2.8
 */
public abstract class SearchingCodeVisitor extends ClassCodeVisitorSupport {

	/**
	 * Action that is called by visitor when it finds a method call node that has a 
	 * given method name.
	 */
	public static abstract class MethodCallAction {
		
		//Consider using generics if more node types need to be supported:
		//NodeAction<N extends ASTNode>
	
		public final String methodName;
	
		public MethodCallAction(String methodName) {
			Assert.isLegal(methodName!=null);
			this.methodName = methodName;
		}
		
		/**
		 * This method is called by the visitor when it finds a call node where the
		 * target method name matched the String we expect.
		 * <p>
		 * The visitor is responsible to make sure the same node is not visited twice
		 * and to ensure that the method being passed to this method has the stipulated
		 * method name.
		 */
		public abstract void doit(SearchingCodeVisitor visitor, MethodCallExpression call);
	
	}

	/**
	 * An abstract MethodCallAction that matches calls of the form and delegates to
	 * an abstratc 'matchFound' method when the pattern is matched.
	 */
	public static abstract class FindNamedArgumentAction extends MethodCallAction {
		
		public final String argName;
		public final String oldValue;

		public FindNamedArgumentAction(String methodName, String argName, String oldValue) {
			super(methodName);
			this.argName = argName;
			this.oldValue = oldValue;
		}

		public String toString() {
			return methodName +"(... "+argName+": "+"\""+oldValue+"\" ...)";
		}
		
		public void doit(SearchingCodeVisitor visitor, MethodCallExpression call) {
			//Recognize and handle <methodName>(..., <argName>: "<oldName>", ...)
			MapEntryExpression arg = SearchUtil.getNamedArgument(call, argName);
			if (arg!=null) {
				final Expression valueExpression = arg.getValueExpression();
				String argValue = SearchUtil.getStringValue(valueExpression);
				if (oldValue.equals(argValue)) {
					matchFound(visitor, call, valueExpression);
				}
			}
		}

		/**
		 * Override to implement an action that is executed only when a match to the pattern is found.
		 * @param call -- the call containing the argument
		 * @param valueExpression -- the expression corresponding to the argument's value.
		 */
		protected abstract void matchFound(SearchingCodeVisitor visitor, MethodCallExpression call, Expression valueExpression);
	}

	/**
	 * Bit of info that identifies a node, so we can avoid visiting same node twice.
	 * 
	 * @author Kris De Volder.
	 * @since 2.8
	 */
	protected static class NodeKey {

		private int pos;
		private int len;

		public NodeKey(ASTNode node) {
			pos = node.getStart();
			len = node.getLength();
		}

		@Override
		public String toString() {
			return "["+pos+","+len+"]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + len;
			result = prime * result + pos;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NodeKey other = (NodeKey) obj;
			if (len != other.len)
				return false;
			if (pos != other.pos)
				return false;
			return true;
		}
	}

	private GroovyCompilationUnit cu = null;
	private String cuText;

	public SearchingCodeVisitor(ICompilationUnit cu) {
		if (cu instanceof GroovyCompilationUnit) {
			this.cu = (GroovyCompilationUnit)cu;
			this.cuText = new String(((GroovyCompilationUnit) cu).getContents());
		}
	}

	/**
	 * Creates a 'match'. It also checks whether the 'found' expression's text actually
	 * contains the searched text and ensures to make the match cover exactly the searchedText.
	 */
	public Match createMatch(Expression valueExpression, String searchedText) throws CoreException {
		int start = valueExpression.getStart();
		int len = valueExpression.getLength();
		String foundText = cuText.substring(start, start+len);
		int displace = foundText.indexOf(searchedText);
		if (displace>=0) {
			IJavaElement el = cu.getElementAt(start);
			return new Match(el, start+displace, searchedText.length());
		} else {
			throw new CoreException(new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, 
					"Found AST node, but it doesn't contain searched text"));
		}
	}

	
	
	@SuppressWarnings("restriction")
	public void start() {
		if (cu instanceof GroovyCompilationUnit) {
			GroovyCompilationUnit gcu = (GroovyCompilationUnit) cu;
			
			try {
				ModuleNode node = gcu.getModuleNode();
				if (node==null) {
					recordError("Could not obtain Groovy parse tree for '"+cu.getElementName()+"'. Fix compilation problems and try again.");
				} else {
					this.visit(node);
				}
			} finally {
				this.cuText = null;
			}
		}
	}

	public void recordError(String msg) {
		GrailsCoreActivator.log(msg);
	}

	@Override
	protected SourceUnit getSourceUnit() {
		return null;
	}

	public void visit(ModuleNode node) {
		for (ClassNode clazz : node.getClasses()) {
			visitClass(clazz);
		}
	}

	@Override
	public void visitMethodCallExpression(MethodCallExpression call) {
		String name = call.getMethodAsString(); //Could be null (for 'funny' calls where target name is dynamic)
		SearchingCodeVisitor.MethodCallAction action = getMethodCallAction(name);
		if (action!=null) {
			Assert.isLegal(action.methodName.equals(name));
			if (!isVisited(call)) {
				action.doit(this, call);
			}
		}
		super.visitMethodCallExpression(call);
	}

	/**
	 * Concrete subclass can provide some way to determine action to take when
	 * a method with some given name is being visited.
	 */
	protected MethodCallAction getMethodCallAction(String methodName) {
		return null;
	}

	/**
	 * To avoid processing same node twice. Nodes are only kept in here if they actually matter
	 * for the traversal (i.e. if they have actions associated with them, so this set should be
	 * quite small assuming the set of interesting nodes in the tree is quite small).
	 */
	private HashSet<NodeKey> visited = new HashSet<NodeKey>();
	private ClassNode currentTopLevelClass;

	/**
	 * @return True if the method was never called on a given node before. I.e. it returns false only the first 
	 * time it is called on this node. 
	 */
	private boolean isVisited(MethodCallExpression node) {
		final NodeKey key = new NodeKey(node);
		if (!visited.contains(key)) {
			visited.add(key);
			return false;
		}
		return true;
	}

	@Override
	public void visitClass(ClassNode node) {
		boolean isToplevel = false;
		if (currentTopLevelClass==null) {
			//entering top level class
			currentTopLevelClass = node;
			isToplevel = true;
		}
		try {
			super.visitClass(node);
		} finally {
			if (isToplevel) {
				//Exiting top level class
				currentTopLevelClass = null;
			}
		}
	}

	public ClassNode getCurrentTopLevelClass() {
		return currentTopLevelClass;
	}

}
