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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;

import org.grails.ide.eclipse.editor.groovy.controllers.ControllerTarget;
import org.grails.ide.eclipse.editor.groovy.controllers.PerProjectControllerCache;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;

/**
 * @author Kris De Volder
 *
 * @since 2.9
 */
public abstract class AbstractGrailsSearch {
	
	public AbstractGrailsSearch() {
	}

	/**
	 * The types to search for references in (should be a list of toplevel types)
	 */
	protected List<IType> typesToSearch = new ArrayList<IType>();
	
	/**
	 * The gsp files to search for references in (should be a list of gsp files)
	 */
	protected List<IFile> gspFilesToSearch = new ArrayList<IFile>();
	
	/**
	 * The URLMappings file to search.
	 */
	protected IFile urlMappingsToSearch = null;
	
	/**
	 * If this method returns false, the search can be ignored since it won't find any results. If it returns true, it
	 * may or may not find results.
	 */
	public boolean isInteresting() {
		return !typesToSearch.isEmpty() || !gspFilesToSearch.isEmpty() || urlMappingsToSearch!=null;
	}
	
	public boolean add(IType elementToSearchIn) {
		return typesToSearch.add(elementToSearchIn);
	}

	public void perform(ISearchRequestor requestor) {
		for (IType searchType : typesToSearch) {
			ICompilationUnit cu = searchType.getCompilationUnit();
			if (cu!=null) {
				SearchingCodeVisitor visitor = createSearchingVisitor(requestor, cu);
				visitor.start();
			}
		}
		for (IFile gspFile : gspFilesToSearch) {
			FileSearcher searcher = createGSPSearcher(requestor, gspFile);
			safePerform(searcher);
		}
		if (urlMappingsToSearch!=null) {
			FileSearcher searcher = createURLMappingSearcher(requestor, urlMappingsToSearch);
			safePerform(searcher);
		}
	}

	public void safePerform(FileSearcher searcher) {
		try {
			if (searcher!=null) {
				searcher.perform();
			}
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
		} catch (IOException e) {
			GrailsCoreActivator.log(e);
		}
	}

	protected FileSearcher createGSPSearcher(ISearchRequestor requestor, IFile gspFile) {
		return null;
	}

	protected FileSearcher createURLMappingSearcher(ISearchRequestor requestor, IFile urlMappingsFile) {
		return null;
	}
	
	protected abstract SearchingCodeVisitor createSearchingVisitor(ISearchRequestor requestor, ICompilationUnit cu);

	/**
	 * Adds all controllers from a given JavaProject to the list of types to search in, but only if the types are
	 * included in the given search scope.
	 */
	public void addControllersFrom(IJavaProject project, IJavaSearchScope scope) {
		PerProjectControllerCache controllerCache = GrailsCore.get().connect(project.getProject(), PerProjectControllerCache.class);
		if (controllerCache != null) {
			for (ControllerTarget controller : controllerCache.getAllControllerTargets()) {
				try {
					IType controllerClass = controller.toJavaElement();
					if (encloses(scope, controllerClass)) {
						add(controllerClass);
					}
				} catch (JavaModelException e) {
					GrailsCoreActivator.log(e);
				}
			}
		}
	}
	
	public void addUrlMappingsFrom(GrailsProject project, IJavaSearchScope scope) {
		Assert.isLegal(urlMappingsToSearch==null); //Only expecting so search one file, so only have support to track one file and it can only be set once.
		IFile file = project.getJavaProject().getProject().getFile(new Path("grails-app/conf/UrlMappings.groovy"));
		if (file.exists() && encloses(scope, file)) {
			urlMappingsToSearch = file;
		}
	}
	
	public void addGspFilesFrom(GrailsProject project, IJavaSearchScope scope) {
		//Note: better to ignore the scope, because it typically doesn't include
		//the GSPfiles, but we do want to search them!
		gspFilesToSearch.addAll(project.getGSPFiles());
	}

	private boolean encloses(IJavaSearchScope scope, IFile gspFile) {
		if (scope!=null) {
			return scope.encloses(gspFile.getFullPath().toString());
		} else {
			return true;
		}
	}

	private boolean encloses(IJavaSearchScope scope, IType controllerClass) {
		if (scope!=null) {
			return scope.encloses(controllerClass);
		}
		return true;
	}
}