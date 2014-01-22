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
package org.grails.ide.eclipse.explorer.providers;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.GrailsResourceUtil;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes;


/**
 * Filters out Grails folders and top-level source package fragment roots in the
 * common navigator as these are replaced by or contained in Grails logical
 * folders
 * @author Nieraj Singh
 * @author Andy Clement
 */
public class GrailsNavigatorFilter extends ViewerFilter {

	public boolean select(Viewer arg0, Object parent, Object element) {
		if (element instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root = ((IPackageFragmentRoot) element);

			try {
				// allow binary content like libraries
				if (root.getKind() == IPackageFragmentRoot.K_BINARY) {
					return true;
				}
			} catch (JavaModelException e) {
				GrailsCoreActivator.log(e);
			}
			IProject project = ((IPackageFragmentRoot) element)
					.getJavaProject().getProject();
			Object immediateParent = getImmediateParent(parent);

			// Filter out all top-level level plugin dependency package
			// fragment roots. Filter in all other package fragment roots
			if (GrailsNature.isGrailsProject(project)
					&& isGrailsDependencyPackageFragmentRoot(immediateParent,
							root)) {
				return false;
			}
		} else if (element instanceof IFolder) {
			IFolder folder = (IFolder) element;
			IProject project = folder.getProject();

			if (GrailsNature.isGrailsProject(project)) {
				// For reimages Grails file folders, if they are top level,
				// filter them in. If they are child of other folders, filter
				// them out
				Object immediateParent = getImmediateParent(parent);
				if (GrailsResourceUtil
						.isReimagedGrailsProjectFileFolder(folder)
						&& !(immediateParent instanceof IProject)) {
					return false;
				} else if (GrailsResourceUtil.isFilteredGrailsFolder(folder)) {
					return false;
				} else if (GrailsResourceUtil.isDefaultOutputFolder(folder)) {
					return false;
				} else {
					// Do a further check to see if any children may be filtered
					// out. If so, don't show the parent folder
					try {
						IResource[] children = folder.members();
						// Filtering algorithm:
						// If a folder contains AT LEAST one filtered child, it
						// means
						// it's a Grails folder, in which case if all children
						// are filtered
						// ONLY then is the parent folder filtered out.
						// Note to be careful not to filter out folders that
						// contain anything
						// else.
						if (children != null) {
							boolean filterOutParentFolder = false;
							for (IResource child : children) {
								if ((child instanceof IFolder)
										&& (GrailsResourceUtil
												.isFilteredGrailsFolder((IFolder) child) || GrailsResourceUtil
												.isReimagedGrailsProjectFileFolder((IFolder) child))) {
									// For now decide to filter out the parent
									// folder
									// as it contains filtered Grails content
									filterOutParentFolder = true;
								} else {
									filterOutParentFolder = false;
									break;
								}
							}
							// All the children in the folder is filtered Grail
							// content, therefore
							// filter out the parent folder.
							if (filterOutParentFolder) {
								return false;
							}
						}
					} catch (CoreException e) {
						GrailsCoreActivator.log(e);
					}
				}
			}
		} else if (element instanceof IPackageFragment) {
			IPackageFragment frag = (IPackageFragment) element;
			IJavaProject javaProject = frag.getJavaProject();
			if (GrailsNature.isGrailsProject(javaProject.getProject())
					&& frag.isDefaultPackage()) {
				IJavaElement javaParent = frag.getParent();
				if (javaParent instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot root = (IPackageFragmentRoot) javaParent;
					// Only filter out the default package fragment from CONF
					// folder
					GrailsProjectStructureTypes type = GrailsResourceUtil
							.getGrailsContainerType(root);
					if (type == GrailsProjectStructureTypes.CONF) {
						return false;
					}
				}
			}
		} else if (element instanceof ClassPathContainer) {
			//Hide toplevel classpath containers.
			if (parent instanceof TreePath) {
				TreePath parentPath = (TreePath) parent;
				if (parentPath.getLastSegment() instanceof IProject) {
					IProject project = (IProject) parentPath.getLastSegment();
					return !GrailsNature.isGrailsProject(project);
				}
			}
		}

		return true;
	}

	protected boolean isGrailsDependencyPackageFragmentRoot(Object parent,
			IPackageFragmentRoot root) {
		return parent instanceof IProject
				&& GrailsResourceUtil
						.isGrailsDependencyPackageFragmentRoot(root);
	}

	protected Object getImmediateParent(Object possiblePath) {
		if (possiblePath instanceof TreePath) {
			return ((TreePath) possiblePath).getLastSegment();
		}
		// if not a treepath, then it may already be the parent
		return possiblePath;
	}

}
