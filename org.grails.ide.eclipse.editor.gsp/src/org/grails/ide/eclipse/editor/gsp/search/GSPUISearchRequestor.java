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
package org.grails.ide.eclipse.editor.gsp.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.internal.core.search.HierarchyScope;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.search.ui.text.Match;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;


/**
 * @author Andrew Eisenberg
 * @since 2.7.0
 */
public class GSPUISearchRequestor implements IGSPSearchRequestor {
    
    public GSPUISearchRequestor(ElementQuerySpecification spec) {
        this.spec = spec;
    }

    private ISearchRequestor requestor;
    private final ElementQuerySpecification spec;
    private List<IFile> gspsToSearch;
    

    public void setRequestor(ISearchRequestor requestor) {
        this.requestor = requestor;
    }
    public boolean searchForTags() {
        return true;
    }

    public void acceptMatch(IFile file, int start, int length) {
        requestor.reportMatch(new Match(file, start, length));
    }

    public int limitTo() {
        return spec.getLimitTo();
    }

    public IJavaElement elementToSearchFor() {
        return spec.getElement();
    }
    
    public List<IFile> getGSPsToSearch() {
        if (gspsToSearch == null) {
            // must refresh the cache
            gspsToSearch = initializeGspsToSearch();
        }
        return gspsToSearch;
    }
    
    private List<IFile> initializeGspsToSearch() {
        if (spec.getScope() instanceof HierarchyScope) {
            return Collections.emptyList();
        }
        if (spec.getLimitTo() == IJavaSearchConstants.DECLARATIONS) {
            // assume no declarations are in gsps
            return Collections.emptyList();
        }
        
        
        List<IProject> allGrailsProjects = findAllGrailsProjects();
        List<IFile> results = new ArrayList<IFile>();
        for (IProject grailsProject : allGrailsProjects) {
            IFolder viewsFolder = grailsProject.getFolder("grails-app/views");
            // urrrgh...I don't know how to check to see if the gsps should be included
            // we will assume that if the controller folder is included, then gsps will be searched
            if (viewsFolder.exists() && spec.getScope().encloses(grailsProject.getFolder("grails-app/controllers").getFullPath().toPortableString())) {
                List<IFile> foundGsps = findGspsInContainer(viewsFolder, spec.getScope());
                if (foundGsps != null) {
                    results.addAll(foundGsps);
                }
            }
        }
        return results;
    }

    
    private List<IProject> findAllGrailsProjects() {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        List<IProject> grailsProjects = new ArrayList<IProject>(projects.length);
        for (IProject project : projects) {
            if (GrailsNature.isGrailsProject(project)) {
                grailsProjects.add(project);
            }
        }
        return grailsProjects;
    }
    
    private List<IFile> findGspsInContainer(IContainer container, IJavaSearchScope scope) {
        if (true) {
            try {
                List<IFile> results = new ArrayList<IFile>();
                IResource[] children = container.members();
                for (IResource child : children) {
                    if (child instanceof IContainer) {
                        List<IFile> childResults = findGspsInContainer((IContainer) child, scope);
                        if (childResults != null) {
                            results.addAll(childResults);
                        }
                    } else if (child instanceof IFile) {
                        // use content type?
                        if (((IFile) child).getFileExtension().equals("gsp")) {
                            results.add((IFile) child);
                        }
                    }
                }
                return results;
            } catch (CoreException e) {
                GrailsCoreActivator.log(e);
            }
        }
        return null;
    }
}
