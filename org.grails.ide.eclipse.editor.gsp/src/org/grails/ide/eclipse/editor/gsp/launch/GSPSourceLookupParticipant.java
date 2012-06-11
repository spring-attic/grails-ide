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
package org.grails.ide.eclipse.editor.gsp.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.jdt.debug.core.IJavaStackFrame;

/**
 * Translate a stack frame to the name of a gsp
 * @author Andrew Eisenberg
 * @created Jul 16, 2010
 */
public class GSPSourceLookupParticipant extends AbstractSourceLookupParticipant implements ISourceLookupParticipant {

    private static final String GRAILS_APP_VIEWS = "grails_app_views_"; //$NON-NLS-1$

    /**
     * Only return something if we are in a gsp
     */
    public String getSourceName(Object object) throws CoreException {
        String typeName = null;
        if (object instanceof String) {
            // assume it's a file name
            typeName = (String) object;
        }
        IJavaStackFrame frame = null;
        if (object instanceof IAdaptable) {
            frame = (IJavaStackFrame) ((IAdaptable)object).getAdapter(IJavaStackFrame.class);
        }
        if (frame != null) {
            typeName = frame.getReceivingTypeName();
        }
        
        String candidate = null;
        
        // The file name starts after the the grails_app_views portion
        // must convert all '_' into '/', except the last, which
        // becomes a '.'
        if (typeName != null) {
            int gspStart = typeName.indexOf(GRAILS_APP_VIEWS);
            if (gspStart > -1) {
                gspStart += GRAILS_APP_VIEWS.length();
                int gspEnd = typeName.indexOf('$', gspStart);
                if (gspEnd == -1) {
                    gspEnd = typeName.length();
                }
                candidate = typeName.substring(gspStart, gspEnd);
                // yikes!  if a '_' really does exist in the name, then we are screwed.
                candidate = candidate.replace('_', '/');
                candidate = candidate.replace("/gsp", ".gsp");
            }
        }
        return candidate;
    }
}
