/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * Fixes the source encoding (charset) for i18n folders.
 * This should be run after a refresh dependencies.
 * 
 * There are certain files in a grails proejct that have different encodings.
 * Specifically, these are the gsp files and the message.properties files.
 * 
 * See STS-1234 for details.h
 * 
 * @author Andrew Eisenberg
 * @since 2.8.0
 */
public class CharsetFixer {
    
    private static final String UTF_8 = "UTF-8";
    private static final String LINK_TO_GRAILS_PLUGINS = ".link_to_grails_plugins";
    private static final String GRAILS_APP_I18N = "grails-app/i18n";
    
    
    private final IProject grailsProject;

    public CharsetFixer(IProject grailsProject) {
        this.grailsProject = grailsProject;
    }

    /**
     * Fixes encodings for the i18n files in the entire project
     * Only sets encodings if the correct "UTF-8" encoding is not already set.
     * @param monitor
     * @throws CoreException if {@link IFolder#setDefaultCharset(String, IProgressMonitor)} fails
     */
    public void fixEncodings(IProgressMonitor monitor) throws CoreException {
        monitor.beginTask("Fix i18n encodings for project " + grailsProject.getName(), 2);
        if (monitor.isCanceled()) { throw new OperationCanceledException(); }
        
        IFolder i18nFolder = grailsProject.getFolder(GRAILS_APP_I18N);
        fixEncoding(i18nFolder, monitor);
        
        // now go through plugins
        IFolder pluginsFolder = grailsProject.getFolder(LINK_TO_GRAILS_PLUGINS);
        if (pluginsFolder.exists()) {
            for (IResource resource : pluginsFolder.members()) {
                if (resource.exists() && resource.getType() == IResource.FOLDER) {
                    fixEncoding(((IFolder) resource).getFolder(GRAILS_APP_I18N), monitor);
                }
            }
        }
    }

    /**
     * Set the encodings if they have not already been explicitly set.
     * @param i18nFolder
     * @param monitor
     * @throws CoreException
     */
    private void fixEncoding(IFolder i18nFolder, IProgressMonitor monitor) throws CoreException {
        if (i18nFolder.exists()) {
            if (!UTF_8.equals(i18nFolder.getDefaultCharset(true))) {
                i18nFolder.setDefaultCharset(UTF_8, monitor);
            }
            for (IResource member : i18nFolder.members()) {
                if (member.exists()) {
                    // STS-1234 message_da.properties has a different encoding
                    if (member.getType() == IResource.FILE && ((IFile) member).getCharset(false) == null) {
                        ((IFile) member).setCharset(UTF_8, monitor);
                    } else if (member.getType() == IResource.FOLDER && ((IFolder) member).getDefaultCharset(false) == null) {
                        ((IFolder) member).setDefaultCharset(UTF_8, monitor);
                    }
                }
            }
        }
        monitor.worked(1);
    }
}
