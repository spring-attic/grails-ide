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

import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.grails.ide.eclipse.core.GrailsCoreActivator;

import org.grails.ide.eclipse.search.SearchUtil;
import org.grails.ide.eclipse.search.SearchingCodeVisitor;
import org.grails.ide.eclipse.search.URLMappingsSearcher;

/**
 * @author Kris De Volder
 *
 * @since 2.9
 */
public class URLMappingControllerActionSearcher extends URLMappingsSearcher {

	
	private ISearchRequestor requestor;
	private String targetControllerName;
	private String targetActionName;
	
	public URLMappingControllerActionSearcher(ISearchRequestor requestor, String targetControllerName, String targetActionName, IFile urlMappingsFile) {
		super(urlMappingsFile);
		this.requestor = requestor;
		this.targetControllerName = targetControllerName;
		this.targetActionName = targetActionName;
	}

	@Override
	protected void visitURLMappingCall(SearchingCodeVisitor visitor, URLMappingCall call) {
		Attribute controllerAttr = null;
		Attribute actionAttr = null;
		for (Attribute a : call.getAttributes()) {
			if ("controller".equals(a.name)) {
				controllerAttr = a;
			} else if ("action".equals(a.name)) {
				actionAttr = a;
			} else if ("view".equals(a.name)) {
				actionAttr = a;
			}
		}
		if (controllerAttr!=null) {
			String actualController = SearchUtil.getStringValue(controllerAttr.value);
			if (targetControllerName.equals(actualController)) {
				if (actionAttr!=null) {
					String actualAction = SearchUtil.getStringValue(actionAttr.value);
					try {
						if (targetActionName.equals(actualAction)) {
							requestor.reportMatch(visitor.createMatch(actionAttr.value,targetActionName));
						} else if (actionAttr.value instanceof MapExpression) {
							//action = [GET:"show", PUT:"update", DELETE:"delete", POST:"save"]
							MapExpression map = (MapExpression) actionAttr.value;
							for (MapEntryExpression e : map.getMapEntryExpressions()) {
								String actionName = SearchUtil.getStringValue(e.getValueExpression());
								if (targetActionName.equals(actionName)) {
									requestor.reportMatch(visitor.createMatch(e.getValueExpression(), targetActionName));
								}
							}
						}
					} catch (CoreException e) {
						GrailsCoreActivator.log(e);
					}
				} 
			}
		}
	}

}
