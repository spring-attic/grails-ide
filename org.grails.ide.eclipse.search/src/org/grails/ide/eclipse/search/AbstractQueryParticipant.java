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


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jdt.ui.search.IQueryParticipant;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.jdt.ui.search.QuerySpecification;
import org.grails.ide.eclipse.core.GrailsCoreActivator;



/**
 * @author Kris De Volder
 *
 * @since 2.9
 */
public abstract class AbstractQueryParticipant implements IQueryParticipant {

	public IMatchPresentation getUIParticipant() {
		return null;
	}

	public void search(ISearchRequestor requestor, QuerySpecification specification, IProgressMonitor monitor) throws CoreException {
		AbstractGrailsSearch search = createInterestingSearch(specification);
		if (search!=null) {
			search.perform(requestor);
		}
	}

	/**
	 * Checks whether a given query specification is 'interesting' to the participant and returns a representation of
	 * the interesting query if so. If the query is deemed 'not interesting' this method will return null.
	 */
	protected AbstractGrailsSearch createInterestingSearch(QuerySpecification specification) {
		try {
			AbstractGrailsSearch search = createSearch(specification);
			if (search.isInteresting()) {
				return search;
			}
		} catch (JavaModelException e) {
			//Don't log, assume already logged a same/similar exception in estimateTicks
		}
		return null;
	}

	protected abstract AbstractGrailsSearch createSearch(QuerySpecification specification) throws JavaModelException;

	public int estimateTicks(QuerySpecification specification) {
		try {
			AbstractGrailsSearch search = createSearch(specification);
			if (search.isInteresting()) {
				return 100; // Just a stab in the dark.. this means its half as much time as the Java search.
			}
		} catch (JavaModelException e) {
			GrailsCoreActivator.log(e);
		}
		return 0;
	}

}
