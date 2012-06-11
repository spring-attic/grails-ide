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

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

/**
 * Empty implementation.  Exists only so that the GSP editor plugin 
 * can create an adapter for {@link ISourceLookupParticipant}
 * @author Andrew Eisenberg
 * @created Aug 4, 2010
 */
public class GrailsSourceLookupParticipantAdapter extends PlatformObject {
    static ISourceLookupParticipant getParticipant() {
        return (ISourceLookupParticipant) new GrailsSourceLookupParticipantAdapter().getAdapter(ISourceLookupParticipant.class);
    }
}
