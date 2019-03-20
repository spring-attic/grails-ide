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

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.ui.search.IQueryParticipant;
import org.eclipse.jdt.ui.search.QuerySpecification;

import org.grails.ide.eclipse.search.AbstractGrailsSearch;
import org.grails.ide.eclipse.search.AbstractQueryParticipant;

/**
 * Grails aware search participant capable of finding extra references to Grails controller types in things such as 
 * 'redirect(controller: "name" ...).
 * 
 * @author Kris De Volder
 *
 * @since 2.9
 */
public class ControllerTypeQueryParticipant extends AbstractQueryParticipant implements IQueryParticipant {
	
	private static boolean DEBUG = Platform.getLocation().toString().contains("kdvolder");
	public void debug(String string) {
		if (DEBUG) {
			System.out.println(string);
		}
	}
	
	public AbstractGrailsSearch createSearch(QuerySpecification specification) {
		return new ControllerTypeSearch(specification);
	}

}
