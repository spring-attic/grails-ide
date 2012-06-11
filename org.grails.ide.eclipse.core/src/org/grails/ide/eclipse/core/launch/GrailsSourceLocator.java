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
package org.grails.ide.eclipse.core.launch;

import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;

/**
 * @author Christian Dupuis
 * @author Andrew Eisenberg
 * @since 2.2.0
 */
public class GrailsSourceLocator extends AbstractSourceLookupDirector {

	public void initializeParticipants() {
	    ISourceLookupParticipant participant = GrailsSourceLookupParticipantAdapter.getParticipant();
	    if (participant != null) {
	        addParticipants(new ISourceLookupParticipant[] { participant, new JavaSourceLookupParticipant(), new GrailsSourceLookupParticipant() });
	    } else {
	        addParticipants(new ISourceLookupParticipant[] { new JavaSourceLookupParticipant(), new GrailsSourceLookupParticipant() });
	    }
	}

}
