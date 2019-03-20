/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

public abstract class URLMappingsSearcher extends FileSearcher {

	/**
	 * Abstracts away from the detailed syntax of a URL mapping but provides a way to access its controller
	 * and action attributes, regardless of how they are represented (at least two forms exist: 
	 *   "/foo(controller: 'name')
	 *   "/foo {
	 *       controller = 'name'
	 *   }
	 * It is also possible to mix these notations in one call
	 *   "/foo(controller: 'name')
	 *   "/foo(controller: 'bell') {
	 *       action = 'ring'
	 *   }
	 */
	public class URLMappingCall {
		
		private Collection<Attribute> attributes = new ArrayList<URLMappingsSearcher.Attribute>();
		
		public URLMappingCall(MethodCallExpression call) {
			Expression args = call.getArguments();
			if (args instanceof TupleExpression) {
				TupleExpression argList = (TupleExpression) args;
				for (Expression arg : argList.getExpressions()) {
					if (arg instanceof MapExpression) {
						MapExpression map = (MapExpression) arg;
						for (MapEntryExpression namedArg : map.getMapEntryExpressions()) {
							String name = SearchUtil.getStringValue(namedArg.getKeyExpression());
							if (name!=null) {
								Expression value = namedArg.getValueExpression();
								add(new Attribute(name, value));
							}
						}
					} else if (arg instanceof ClosureExpression) {
						ClosureExpression closure = (ClosureExpression) arg;
						List<Statement> stms = SearchUtil.getStatements(closure);
						for (Statement s : stms) {
							if (s instanceof ExpressionStatement) {
								ExpressionStatement es = (ExpressionStatement) s;
								Expression e = es.getExpression();
								if (e instanceof BinaryExpression) {
									BinaryExpression be = (BinaryExpression) e;
									Token op = be.getOperation();
									if ("=".equals(op.getText())) {
										Expression _varExp = be.getLeftExpression();
										if (_varExp instanceof VariableExpression) {
											VariableExpression varExp = (VariableExpression) _varExp;
											Expression valExp = be.getRightExpression();
											String name = varExp.getName();
											if (name!=null) {
												add(new Attribute(name, valExp));
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		public Collection<Attribute> getAttributes() {
			return attributes;
		}
		
		private void add(Attribute a) {
			attributes.add(a);
		}
		
	}

	public class Attribute {
		public final String name;
		public final Expression value;
		public Attribute(String name, Expression value) {
			this.name = name;
			this.value = value;
		}
	}
	
	public class URLMappingsVisitor extends SearchingCodeVisitor {
		public URLMappingsVisitor(ICompilationUnit cu) {
			super(cu);
		}
		
		@Override
		public void visitMethodCallExpression(MethodCallExpression call) {
			String methodName = SearchUtil.getStringValue(call.getMethod());
			if ("name".equals(methodName)) {
				visitNamedURLMapping(this, call);
			} else {
				visitURLMappingCall(this, new URLMappingCall(call));
			}
		}

		private void visitNamedURLMapping(URLMappingsVisitor visitor, MethodCallExpression call) {
			Expression _args = call.getArguments();
			if (_args instanceof TupleExpression) {
				TupleExpression args = (TupleExpression) _args;
				if (args.getExpressions().size()==1) {
					Expression arg = args.getExpression(0);
					if (arg instanceof MapExpression) {
						MapExpression map = (MapExpression) arg;
						if (map.getMapEntryExpressions().size()==1) {
							MapEntryExpression entry = map.getMapEntryExpressions().get(0);
							//String name = SearchUtil.getStringValue(entry.getKeyExpression());
							Expression actualMapping = entry.getValueExpression();
							actualMapping.visit(this);
						}
					}
				}
			}
		}
	}

	public URLMappingsSearcher(IFile file) {
		super(file);
	}

	@Override
	public void perform() throws IOException, CoreException {
		IJavaElement jel = JavaCore.create(file);
		searchIn((GroovyCompilationUnit)jel);
	}

	private void searchIn(GroovyCompilationUnit cu) {
		if (cu!=null) {
			ModuleNode module = cu.getModuleNode();
			if (module!=null) {
				List<ClassNode> classes = module.getClasses();
				for (ClassNode classNode : classes) {
					if (classNode.getName().equals("UrlMappings")) {
						searchIn(cu, classNode);
					}
				}
			}
		}
	}
	
	protected abstract void visitURLMappingCall(SearchingCodeVisitor visitor, URLMappingCall call);

	private void searchIn(ICompilationUnit cu, ClassNode classNode) {
		FieldNode mappings = classNode.getField("mappings");
		if (mappings!=null && mappings.isStatic()) {
			searchIn(cu, SearchUtil.getClosureValue(mappings));
		}
	}

	private void searchIn(ICompilationUnit cu, ClosureExpression closureValue) {
		SearchingCodeVisitor visitor = new URLMappingsVisitor(cu);
		visitor.visitClosureExpression(closureValue);
	}
}
