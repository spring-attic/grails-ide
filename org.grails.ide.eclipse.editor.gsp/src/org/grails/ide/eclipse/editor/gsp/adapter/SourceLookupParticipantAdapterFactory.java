/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.editor.gsp.adapter;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

import org.grails.ide.eclipse.editor.gsp.launch.GSPSourceLookupParticipant;

/**
 * Adapter factory for GSP Source Lookup participant
 * @author Andrew Eisenberg
 * @created Aug 4, 2010
 */
@SuppressWarnings("rawtypes")
public class SourceLookupParticipantAdapterFactory implements IAdapterFactory {

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == ISourceLookupParticipant.class) {
            return new GSPSourceLookupParticipant();
        } else {
            return null;
        }
    }

    public Class[] getAdapterList() {
        return new Class[] { ISourceLookupParticipant.class };
    }

}
