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
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.search.ui.text.Match;

import org.grails.ide.eclipse.search.GSPSearcher;

/**
 * Searches for controller *type* references inside GSP file.
 * 
 * @author Kris De Volder
 *
 * @since 2.9
 */
public class GSPControllerTypeSearcher extends GSPSearcher {

	private String targetControllerName;
	private ISearchRequestor requestor;
	
	public GSPControllerTypeSearcher(IFile gspFile, String targetControllerName, ISearchRequestor requestor) {
		super(gspFile);
		this.requestor = requestor;
		this.targetControllerName = targetControllerName;
	}
	
	@Override
	protected void visit(GLinkTag link) {
		if (targetControllerName.equals(link.getControllerName())) {
			Match match = link.createControllerMatch(targetControllerName);
			if (match!=null) {
				requestor.reportMatch(match);
			}
		}
	}

}
