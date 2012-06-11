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
package org.grails.ide.eclipse.editor.gsp.search;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.PartInitException;
/**
 * This class is currently a no-op class since we are just passing
 * around simple {@link Match} objects.  If we wanted to get fancier,
 * then this class could become useful.
 * @author Andrew Eisenberg
 * @since 2.7.0
 */
public class GSPMatchPresentation implements IMatchPresentation {

    public ILabelProvider createLabelProvider() {
        return new JavaElementLabelProvider();
    }
    
    public void showMatch(Match match, int currentOffset, int currentLength,
            boolean activate) throws PartInitException {
    }

}
