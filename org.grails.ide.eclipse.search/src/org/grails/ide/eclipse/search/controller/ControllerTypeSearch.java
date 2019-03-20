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
package org.grails.ide.eclipse.search.controller;

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.jdt.ui.search.QuerySpecification;
import org.grails.ide.eclipse.core.GrailsCoreActivator;

import org.grails.ide.eclipse.editor.groovy.elements.ControllerClass;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.search.AbstractGrailsSearch;
import org.grails.ide.eclipse.search.FileSearcher;
import org.grails.ide.eclipse.search.GSPSearcher;
import org.grails.ide.eclipse.search.SearchUtil;
import org.grails.ide.eclipse.search.SearchingCodeVisitor;

/**
 * Searches for controller references inside a Grails project.
 * 
 * @author Kris De Volder
 *
 * @since 2.9
 */
public class ControllerTypeSearch extends AbstractGrailsSearch {
	
	/**
	 * The logical (i.e. lower-cased, unquallifed name of the controller being searched for.
	 */
	private String targetControllerName;
	
	public ControllerTypeSearch(QuerySpecification specification) {
		if (specification instanceof ElementQuerySpecification) {
			ElementQuerySpecification spec = (ElementQuerySpecification) specification;
			if (SearchUtil.wantsReferences(spec)) {
				IJavaSearchScope scope = spec.getScope();
				IJavaElement el = spec.getElement();
				if (el.getElementType()==IJavaElement.TYPE) {
					IType targetType = (IType) el;
					IJavaProject project = targetType.getJavaProject();
					if (project!=null) {
						GrailsProject grailsProject = GrailsWorkspaceCore.get().create(project);
						if (grailsProject!=null) {
							ControllerClass targetController = grailsProject.getControllerClass(targetType);
							if (targetController!=null) {
								init(grailsProject, targetController.getLogicalName(), scope);
							}
						}
					}
				}
			}
		}
	}

	private void init(GrailsProject grailsProject, String controllerName, IJavaSearchScope scope) {
		this.targetControllerName = controllerName;
		addControllersFrom(grailsProject.getJavaProject(), scope);
		addGspFilesFrom(grailsProject, scope);
		addUrlMappingsFrom(grailsProject, scope);
	}

	public ControllerTypeSearch(GrailsProject grailsProject, String targetControllerName) {
		init(grailsProject, targetControllerName, null);
	}

	public SearchingCodeVisitor createSearchingVisitor(ISearchRequestor requestor, ICompilationUnit cu) {
		return new ControllerReferenceSearchingVisitor(cu, requestor);
	}
	
	@Override
	protected GSPSearcher createGSPSearcher(ISearchRequestor requestor, IFile gspFile) {
		return new GSPControllerTypeSearcher(gspFile, targetControllerName, requestor);
	}
	
	@Override
	protected FileSearcher createURLMappingSearcher(ISearchRequestor requestor, IFile urlMappingsFile) {
		return new URLMappingControllerTypeSearcher(requestor, targetControllerName, urlMappingsFile);
	}
	
	public class ControllerReferenceSearchingVisitor extends SearchingCodeVisitor {

		public ISearchRequestor javaRequestor;

		public ControllerReferenceSearchingVisitor(ICompilationUnit cu, ISearchRequestor requestor) {
			super(cu);
			this.javaRequestor = requestor;
		}

		@Override
		protected SearchingCodeVisitor.MethodCallAction getMethodCallAction(String methodName) {
			if (methodName!=null) {
				if (methodName.equals("redirect")) {
					return new SearchRedirectCall();
				}
			}
			return null;
		}

		public class SearchRedirectCall extends SearchingCodeVisitor.FindNamedArgumentAction {
			public SearchRedirectCall() {
				super("redirect", "controller", targetControllerName);
			}
			@Override
			protected void matchFound(SearchingCodeVisitor visitor, MethodCallExpression call, Expression valueExpression) {
				try {
					javaRequestor.reportMatch(createMatch(valueExpression, oldValue));
				} catch (CoreException e) {
					GrailsCoreActivator.log(e);
				}
			}
		}
	}
}
