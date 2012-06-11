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
package org.grails.ide.eclipse.search.action;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.search.ui.text.Match;

import org.grails.ide.eclipse.search.GSPSearcher;

/**
 * Searches for controller action references inside GSPfiles
 * 
 * @author Kris De Volder
 *
 * @since 2.9
 */
public class GSPControllerActionSearcher extends GSPSearcher {
	
	private String targetControllerName;
	private String targetActionName;
	private ISearchRequestor requestor;

	public GSPControllerActionSearcher(IFile gspFile, String targetControllerName, String targetActionName, ISearchRequestor requestor) {
		super(gspFile);
		this.requestor = requestor;
		this.targetControllerName = targetControllerName;
		this.targetActionName = targetActionName;
	}
	
	@Override
	public void visit(GLinkTag link) {
		if (targetControllerName.equals(link.getControllerName()) && targetActionName.equals(link.getActionName())) {
			Match match = link.createActionMatch(targetActionName);
			if (match!=null) {
				requestor.reportMatch(match);
			}
		}
	}

}
