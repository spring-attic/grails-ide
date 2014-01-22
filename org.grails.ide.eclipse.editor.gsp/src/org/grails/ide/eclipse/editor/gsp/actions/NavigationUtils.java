/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.editor.gsp.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * @author Andrew Eisenberg
 * @created Dec 3, 2010
 */
public class NavigationUtils {
    public static List<IFile> findGSPsInFolder(IFolder folder) throws CoreException {
        IResource[] rs = folder.members();
        List<IFile> files = new ArrayList<IFile>(rs.length);
        for (IResource r : rs) {
            if (r.isAccessible() && r.getType() == IResource.FILE && "gsp".equalsIgnoreCase(((IFile) r).getFileExtension())) {
                files.add((IFile) r);
            }
        }
        return files;
    }
}
