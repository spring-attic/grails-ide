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

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.search.IQueryParticipant;
import org.eclipse.jdt.ui.search.QuerySpecification;

import org.grails.ide.eclipse.search.AbstractGrailsSearch;
import org.grails.ide.eclipse.search.AbstractQueryParticipant;

public class ControllerActionQueryParticipant extends AbstractQueryParticipant implements IQueryParticipant {

	public AbstractGrailsSearch createSearch(QuerySpecification specification) throws JavaModelException {
		return new ControllerActionSearch(specification);
	}

}
