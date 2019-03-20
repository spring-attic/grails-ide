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
package org.grails.ide.eclipse.explorer.providers;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.GrailsResourceUtil;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes;

import org.grails.ide.eclipse.explorer.elements.GrailsDependencyPluginFolder;
import org.grails.ide.eclipse.explorer.elements.ILogicalFolder;
import org.grails.ide.eclipse.explorer.types.GrailsContainerTypeManager;

/**
 * Label and icon provider for Grails logical folders for the common navigator
 * @author Nieraj Singh
 * @author Andy Clement
 */
public class GrailsNavigatorLabelProvider extends LabelProvider {


	public Image getImage(Object element) {
		if (element instanceof GrailsDependencyPluginFolder) {
			GrailsDependencyPluginFolder dependencyPluginFolder = (GrailsDependencyPluginFolder) element;
			if (dependencyPluginFolder.isInPlacePlugin()) {
				return GrailsContainerTypeManager
						.getInstance()
						.getOverlayedImage(
								dependencyPluginFolder.getType(),
								GrailsContainerTypeManager.LOCAL_PLUGIN_OVERLAY);
			}
		}

		if (element instanceof ILogicalFolder) {
			GrailsProjectStructureTypes type = ((ILogicalFolder) element).getType();
			if (type != null) {
				return GrailsContainerTypeManager.getInstance().getIcon(
						type);
			}
		} else if (element instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root = (IPackageFragmentRoot) element;
			if (GrailsResourceUtil.isReimagedGrailsSourceFolder(root)) {
				try {
					IResource resource = root.getCorrespondingResource();
					if (resource instanceof IFolder) {
						IFolder folder = (IFolder) resource;
						GrailsProjectStructureTypes type = GrailsResourceUtil
								.getGrailsContainerType(folder);
						if (type != null) {
							return GrailsContainerTypeManager.getInstance()
									.getIcon(type);
						}
					}
				} catch (JavaModelException e) {
					GrailsCoreActivator.log(e);
				}
			}

		} else if (element instanceof IFolder) {
			IFolder folder = (IFolder) element;

			if (GrailsNature.isGrailsProject(folder.getProject())
					&& GrailsResourceUtil
							.isReimagedGrailsProjectFileFolder(folder)) {
				GrailsProjectStructureTypes type = GrailsResourceUtil
						.getGrailsContainerType(folder);
				if (type != null) {
					return GrailsContainerTypeManager.getInstance()
							.getIcon(type);
				}
			}
		}
		return null;
	}

	public String getText(Object element) {
		if (element instanceof ILogicalFolder) {
			ILogicalFolder logicalFolder = (ILogicalFolder) element;
			String name = logicalFolder.getName();
			if (name == null) {
				GrailsProjectStructureTypes type = logicalFolder.getType();
				if (type != null) {
					name = type.getDisplayName();
				}
			}
			return name;

		} else if (element instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root = (IPackageFragmentRoot) element;

			if (GrailsResourceUtil.isReimagedGrailsSourceFolder(root)) {
				try {
					IResource resource = root.getCorrespondingResource();
					if (resource instanceof IFolder) {
						IFolder folder = (IFolder) resource;
						GrailsProjectStructureTypes type = GrailsResourceUtil
								.getGrailsContainerType(folder);
						if (type != null) {
							return type.getDisplayName();
						}
					}
				} catch (JavaModelException e) {
					GrailsCoreActivator.log(e);
				}
			} else if (GrailsResourceUtil
					.isGrailsDependencyPackageFragmentRoot(root)) {
				return GrailsResourceUtil.convertRootName(root
						.getElementName());
			}
		} else if (element instanceof IFolder) {
			IFolder folder = (IFolder) element;

			if (GrailsNature.isGrailsProject(folder.getProject())
					&& GrailsResourceUtil
							.isReimagedGrailsProjectFileFolder(folder)) {
				GrailsProjectStructureTypes type = GrailsResourceUtil
						.getGrailsContainerType(folder);
				if (type != null) {
					return type.getDisplayName();
				}
			}
		}
		return null;
	}

}
