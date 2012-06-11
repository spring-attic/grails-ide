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
package org.grails.ide.eclipse.explorer.providers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.GrailsResourceUtil;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureManager;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes;

import org.grails.ide.eclipse.explorer.GrailsExplorerPlugin;
import org.grails.ide.eclipse.explorer.elements.GrailsClasspathContainersFolder;
import org.grails.ide.eclipse.explorer.elements.GrailsFolderElementFactory;
import org.grails.ide.eclipse.explorer.elements.ILogicalFolder;
import org.grails.ide.eclipse.explorer.preferences.GrailsExplorerPreferences;
import org.grails.ide.eclipse.explorer.preferences.OrderingConfig;

/**
 * Providers Grails logical folders to the common navigator
 * 
 * @author Nieraj Singh
 * @author Andy Clement
 * @author Christian Dupuis
 * @author Andrew Eisenberg
 */
public class GrailsNavigatorContentProvider implements ITreeContentProvider {
	
	private Viewer viewer;
	private GrailsExplorerPreferences.Listener preferencesListener;

	public GrailsNavigatorContentProvider() {
	}

	public Object[] getChildren(Object parentElement) {

		if (parentElement instanceof IProject
				&& GrailsNature.isGrailsProject((IProject) parentElement)) {
			
			IProject project = (IProject) parentElement;

			Collection<GrailsProjectStructureTypes> types = GrailsProjectStructureManager
					.getInstance().getAllTopLevelLogicalFolders().values();
			List<Object> topLevelFolders = new ArrayList<Object>();

			GrailsFolderElementFactory factory = getFolderElementFactory();
			for (GrailsProjectStructureTypes type : types) {
				ILogicalFolder element = factory
						.getElement(project, project.getFolder(new Path(type
								.getFolderName())), type);
				if (element != null) {
					topLevelFolders.add(element);
				}
			}
			
			//Add a logical folder that contains the classpath containers
			topLevelFolders.add(new GrailsClasspathContainersFolder(project));

			// Now add the top level package fragment roots that are
			// non-dependency source folders
			IJavaProject javaProject = JavaCore.create(project);
			try {			
				IPackageFragmentRoot[] roots = javaProject
						.getPackageFragmentRoots();
				if (roots != null) {
					for (IPackageFragmentRoot root : roots) {
						if (root.getRawClasspathEntry().getEntryKind() == IClasspathEntry.CPE_SOURCE
								&& !GrailsResourceUtil
										.isGrailsDependencyPackageFragmentRoot(root)) {
							topLevelFolders.add(root);
						}
					}
				}
			} catch (JavaModelException e) {
				GrailsCoreActivator.log(e);
			}

			// Add the file folders that are reimaged
			Set<GrailsProjectStructureTypes> fileFolders = GrailsProjectStructureManager
					.getInstance().getGrailsFileFolders();
			if (fileFolders != null) {
				for (GrailsProjectStructureTypes type : fileFolders) {
					IFolder folder = project.getFolder(new Path(type
							.getFolderName()));
					if (folder != null && folder.exists()) {
						topLevelFolders.add(folder);
					}
				}
			}

			try {
				IResource[] children = project.members();
				if (children != null) {
					for (IResource resource : children) {
						// Skip the linked folders that correspond to
						// Grails dependency package fragment roots
						if (!isLinkedDependencyPackageFragmentRoot(resource)
								&& !isReimagedResource(resource)) {
							topLevelFolders.add(resource);
						}
					}
				}
			} catch (CoreException e) {
				GrailsCoreActivator.log(e);
			}

			return topLevelFolders.toArray();
		} else if (parentElement instanceof ILogicalFolder) {
			ILogicalFolder element = (ILogicalFolder) parentElement;
			List<?> children = element.getChildren();
			if (children != null) {
				return children.toArray();
			}
		} else if (parentElement instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root = (IPackageFragmentRoot) parentElement;
			GrailsProjectStructureTypes type = GrailsResourceUtil
					.getGrailsContainerType(root);
			if (type == GrailsProjectStructureTypes.CONF) {
				try {
					IJavaElement[] children = root.getChildren();
					if (children != null) {
						List<IJavaElement> elements = new ArrayList<IJavaElement>();
						for (IJavaElement child : children) {

							IPackageFragment frag = (child instanceof IPackageFragment) ? (IPackageFragment) child
									: null;

							if (frag == null || !frag.isDefaultPackage()) {
								elements.add(child);
							} else {
								IJavaElement[] defaultChildren = frag
										.getChildren();
								for (IJavaElement defaultChild : defaultChildren) {
									elements.add(defaultChild);
								}
							}
						}
						return elements.toArray();
					}
				} catch (JavaModelException e) {
					GrailsCoreActivator.log(e);
				}
			}
		}
		return null;
	}

	protected boolean isReimagedResource(IResource resource) {
		if (!(resource instanceof IFolder)) {
			return false;
		}
		IFolder folder = (IFolder) resource;
		return GrailsResourceUtil.isReimagedGrailsProjectFileFolder(folder);

	}

	protected boolean isLinkedDependencyPackageFragmentRoot(IResource resource) {
		if (!(resource instanceof IFolder)) {
			return false;
		}

		IFolder folder = (IFolder) resource;
		if (!folder.isLinked()) {
			return false;
		}

		IJavaElement element = JavaCore.create(folder);
		if (element instanceof IPackageFragmentRoot
				&& GrailsResourceUtil
						.isGrailsDependencyPackageFragmentRoot((IPackageFragmentRoot) element)) {
			return true;
		}
		return false;
	}

	protected GrailsFolderElementFactory getFolderElementFactory() {
		return new GrailsFolderElementFactory();
	}

	public Object getParent(Object element) {
		if (element instanceof ILogicalFolder) {
			return ((ILogicalFolder) element).getParent();
		} else if (element instanceof IResource
				&& !(element instanceof IProject)) {
			return ((IResource) element).getParent();
		} else if (element instanceof ClassPathContainer) {
			ClassPathContainer classPathContainer = (ClassPathContainer) element;
			return new GrailsClasspathContainersFolder(classPathContainer.getJavaProject().getProject());
		}
		return null;
	}

	public boolean hasChildren(Object element) {
	    try {
    	    if (element instanceof ILogicalFolder) {
    	        List<Object> children = ((ILogicalFolder) element).getChildren();
                return children != null && children.size() > 0;
    	    } else if (element instanceof IContainer) {
                return ((IResource) element).isAccessible() && 
                        ((IContainer) element).members().length > 0;
    		} else if (element instanceof IPackageFragmentRoot) {
    		    // not really working since there is a JDT content provider that thinks children exist
                IResource underlyingResource = ((IPackageFragmentRoot) element).getUnderlyingResource();
                return underlyingResource instanceof IContainer && ((IContainer) underlyingResource).members().length > 0;
    		}
	    } catch (CoreException e) {
	        GrailsCoreActivator.log(e);
	    }
	    return false;
	}

	public void dispose() {
		if (this.preferencesListener!=null) {
			GrailsExplorerPreferences prefs = GrailsExplorerPlugin.getDefault().getPreferences();
			prefs.removeListener(preferencesListener);
		}
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = viewer;
		ensurePreferencesListener();
	}

	private void ensurePreferencesListener() {
		if (this.preferencesListener==null) {
			GrailsExplorerPreferences prefs = GrailsExplorerPlugin.getDefault().getPreferences();
			if (prefs!=null) {
				preferencesListener = new GrailsExplorerPreferences.Listener() {
					public void orderingChanged(OrderingConfig newConfig) {
						if (viewer!=null) {
							viewer.refresh();
						}
					}
				};
				prefs.addListener(preferencesListener);
			}
		}
	}

}
