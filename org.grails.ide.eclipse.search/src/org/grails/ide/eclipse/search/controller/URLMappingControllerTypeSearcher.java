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
public class URLMappingControllerTypeSearcher extends URLMappingsSearcher {
	
	private ISearchRequestor requestor;
	private String targetControllerName;
	
	public URLMappingControllerTypeSearcher(ISearchRequestor requestor, String targetControllerName, IFile urlMappingsFile) {
		super(urlMappingsFile);
		this.requestor = requestor;
		this.targetControllerName = targetControllerName;
	}

	@Override
	protected void visitURLMappingCall(SearchingCodeVisitor visitor, URLMappingCall call) {
		for (Attribute a : call.getAttributes()) {
			if ("controller".equals(a.name)) {
				String value = SearchUtil.getStringValue(a.value);
				if (targetControllerName.equals(value)) {
					try {
						requestor.reportMatch(visitor.createMatch(a.value, targetControllerName));
					} catch (CoreException e) {
						GrailsCoreActivator.log(e);
					}
				}
			}
		}
	}
}
