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
package org.grails.ide.eclipse.search.action;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.jdt.ui.search.QuerySpecification;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.util.GrailsNameUtils;

import org.grails.ide.eclipse.editor.groovy.elements.ControllerClass;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.search.AbstractGrailsSearch;
import org.grails.ide.eclipse.search.FileSearcher;
import org.grails.ide.eclipse.search.GSPSearcher;
import org.grails.ide.eclipse.search.SearchUtil;
import org.grails.ide.eclipse.search.SearchingCodeVisitor;

/**
 * Searches for controller action references inside of grails projects.
 * 
 * @author Kris De Volder
 *
 * @since 2.9
 */
public class ControllerActionSearch extends AbstractGrailsSearch {
	
	/**
	 * The name of the targetAction
	 */
	private String targetActionName;
	
//	/**
//	 * The type this action is declared in, should be a controller.
//	 */
//	private ControllerClass targetController;
	
	/**
	 * Logical controller name. (e.g 'song' for the class "foo.SongController")
	 */
	private String targetControllerName;
	
	public ControllerActionSearch(QuerySpecification specification) throws JavaModelException {
		if (specification instanceof ElementQuerySpecification) {
			ElementQuerySpecification spec = (ElementQuerySpecification) specification;
			if (SearchUtil.wantsReferences(spec)) {
				IJavaSearchScope scope = spec.getScope();
				IJavaElement el = spec.getElement();
				if (el.getElementType()==IJavaElement.METHOD || el.getElementType()==IJavaElement.FIELD) {
					IMember targetAction = (IMember) el;
					IType targetType = (IType) el.getParent();
					String targetActionName = targetAction.getElementName();
					IJavaProject project = targetAction.getJavaProject();
					if (project!=null) {
						GrailsProject grailsProject = GrailsWorkspaceCore.get().create(project);
						if (grailsProject!=null) {
							init(grailsProject, targetType.getElementName(), targetActionName, scope);
						}
					}
				}
			}
		}
	}

//	public ControllerClass getTargetController() {
//		return targetController;
//	}

	
	public ControllerActionSearch(GrailsProject grailsProject, String targetControllerClassName, String oldActionName) throws JavaModelException {
		init(grailsProject, targetControllerClassName, oldActionName, null);
	}
	
	private void init(GrailsProject grailsProject, String targetControllerClassName, String oldActionName, IJavaSearchScope scope) throws JavaModelException {
		if (grailsProject!=null) {
			if (targetControllerClassName.endsWith(ControllerClass.CONTROLLER)) {
				targetControllerName = GrailsNameUtils.getLogicalPropertyName(targetControllerClassName, ControllerClass.CONTROLLER);
				targetActionName = oldActionName;
				addControllersFrom(grailsProject.getJavaProject(), scope);
				addGspFilesFrom(grailsProject, scope);
				addUrlMappingsFrom(grailsProject, scope);
			}
		}
	}

	private static String getCurrentController(SearchingCodeVisitor visitor) {
		ClassNode currentClass = visitor.getCurrentTopLevelClass();
		String className = currentClass.getNameWithoutPackage();
		if (className.endsWith(ControllerClass.CONTROLLER)) {
			return GrailsNameUtils.getLogicalPropertyName(currentClass.getNameWithoutPackage(), ControllerClass.CONTROLLER);
		} else {
			return null;
		}
	}

	public SearchingCodeVisitor createSearchingVisitor(ISearchRequestor requestor, ICompilationUnit cu) {
		return new ControllerReferenceSearchingVisitor(cu, requestor);
	}

	@Override
	protected GSPSearcher createGSPSearcher(ISearchRequestor requestor, IFile gspFile) {
		return new GSPControllerActionSearcher(gspFile, getTargetControllerName(), getTargetActionName(), requestor);
	}
	
	@Override
	protected FileSearcher createURLMappingSearcher(ISearchRequestor requestor, IFile urlMappingsFile) {
		return new URLMappingControllerActionSearcher(requestor, targetControllerName, targetActionName, urlMappingsFile);
	}
	
	/**
	 * Logical name of 'target' controller the action we are searching for belongs to.
	 */
	public String getTargetControllerName() {
		return targetControllerName;
	}

	private String getTargetActionName() {
		return targetActionName;
	}

	public class ControllerReferenceSearchingVisitor extends SearchingCodeVisitor {

		public ISearchRequestor javaRequestor;

		public ControllerReferenceSearchingVisitor(ICompilationUnit cu, ISearchRequestor requestor) {
			super(cu);
			this.javaRequestor = requestor;
		}
		
		public class SearchRedirectCall extends SearchingCodeVisitor.FindNamedArgumentAction {

			public SearchRedirectCall() {
				super("redirect", "action", targetActionName);
			}

			private String getController(SearchingCodeVisitor visitor, MethodCallExpression call) {
				MapEntryExpression arg = SearchUtil.getNamedArgument(call, "controller");
				if (arg!=null) {
					//There is a controller argument!
					String argValue = SearchUtil.getStringValue(arg.getValueExpression());
					if (argValue != null) {
						//We could determine the value of the argument
						return argValue;
					} else {
						//We could not determine the value of the argument (it's probably dynamic and could be anything!)
						return null; //Indicates we don't know what the target controller is.
					}
				} else {
					//There is no controller argument
					return getCurrentController(visitor);
				}
			}

			@Override
			protected void matchFound(SearchingCodeVisitor visitor, MethodCallExpression call,  Expression valueExpression) {
				String controller = getController(visitor, call);
				if (controller!=null && controller.equals(targetControllerName)) {
					try {
						javaRequestor.reportMatch(createMatch(valueExpression, oldValue));
					} catch (CoreException e) {
						GrailsCoreActivator.log(e);
					}
				}
			}

		}

		@Override
		protected SearchingCodeVisitor.MethodCallAction getMethodCallAction(String name) {
			//If there are many... we could index in a map, but for now this is good enough.
			if (name!=null) {
				if ("render".equals(name)) {
					return new SearchRenderCall();
				} else if ("redirect".equals(name)) {
					return new SearchRedirectCall();
				}
			}
			return null;
		}

		public class SearchRenderCall extends SearchingCodeVisitor.FindNamedArgumentAction {

			public SearchRenderCall() {
				super("render", "view", targetActionName);
			}

			@Override
			protected void matchFound(SearchingCodeVisitor _visitor, MethodCallExpression call, Expression valueExpression) {
				ControllerReferenceSearchingVisitor visitor = (ControllerReferenceSearchingVisitor) _visitor;
				String controller = getCurrentController(visitor);
				if (controller!=null && controller.equals(targetControllerName)) {
					try {
						javaRequestor.reportMatch(createMatch(valueExpression, oldValue));
					} catch (CoreException e) {
						GrailsCoreActivator.log(e);
					}
				}
			}
		}

	}

}
