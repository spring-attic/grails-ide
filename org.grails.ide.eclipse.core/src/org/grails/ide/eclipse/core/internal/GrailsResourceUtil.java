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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathContainer;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathUtils;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureManager;
import org.grails.ide.eclipse.core.internal.plugins.GrailsProjectStructureTypes;
import org.springsource.ide.eclipse.commons.core.JdtUtils;


/**
 * @author Nieraj Singh
 * @author Kris De Volder
 * @author Andrew Eisenberg
 */
public class GrailsResourceUtil {

	private GrailsResourceUtil() {
		// util
	}

	/**
	 * Returns all accessible Grails projects in the workspace. If no accessible
	 * Grails projects are found, empty list is returned. <br/>
	 * Closed projects, or projects that cannot be read are not included in the
	 * list.
	 * 
	 * @return All accessible Grails projects in the workspace, or empty list if
	 *         nothing found
	 */
	public static final List<IProject> getAllGrailsProjects() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		List<IProject> grailsProjects = new ArrayList<IProject>();
		if (projects != null) {
			for (IProject project : projects) {
				if (GrailsNature.isGrailsProject(project)) {
					grailsProjects.add(project);
				}
			}
		}
		return grailsProjects;
	}

	public static boolean hasClasspathContainer(IResource context) {
		if (JdtUtils.isJavaProject(context)) {
			return GrailsClasspathUtils.hasClasspathContainer(JdtUtils
					.getJavaProject(context));
		}
		return false;
	}

	public static IPackageFragmentRoot[] getGrailsDependencyPackageFragmentRoots(
			IProject project, IPath path) {
		if (project == null) {
			return null;
		}
		IJavaProject javaProject = JavaCore.create(project);
		try {

			IClasspathEntry[] entries = javaProject.getRawClasspath();
			for (IClasspathEntry entry : entries) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					for (IClasspathAttribute attr : entry.getExtraAttributes()) {
						if (attr.getName().equals(
								GrailsClasspathContainer.PLUGIN_SOURCEFOLDER_ATTRIBUTE_NAME)) {
							IFolder folder = ResourcesPlugin.getWorkspace()
									.getRoot().getFolder(entry.getPath());
							if (folder.getLocation().equals(path)) {
								return javaProject
										.findPackageFragmentRoots(entry);
							}
						}
					}
				}
			}

		} catch (JavaModelException e) {
			GrailsCoreActivator.log(e);
		}
		return null;

	}

	public static boolean isGrailsDependencyPackageFragmentRoot(
			IPackageFragmentRoot root) {
		if (root == null) {
			return false;
		}

		try {
		    return isGrailsClasspathEntry(root.getRawClasspathEntry());
        } catch (JavaModelException e) {
            GrailsCoreActivator.log(e);
            return false;
        }

	}

    public static boolean isGrailsClasspathEntry(IClasspathEntry entry) {
        return hasClasspathAttribute(entry, GrailsClasspathContainer.PLUGIN_SOURCEFOLDER_ATTRIBUTE_NAME);
    }
    
    public static boolean hasClasspathAttribute(IClasspathEntry entry, String attributeName) {
        if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
            for (IClasspathAttribute attr : entry.getExtraAttributes()) {
                if (attr.getName().equals(attributeName)) {
                    return true;
                }
            }
        }
        return false;
    }

	/**
	 * Certain file folders containing source files are now filtered out as they
	 * are represented by reimaged source package fragment roots.
	 * <p>
	 * This may include folders that are NOT reimaged but have corresponding
	 * source class path entries
	 * </p>
	 * 
	 * @param folder
	 *            to check if it is a filtered file folder
	 * @return true if it is a filtered file folder, false otherwise
	 */
	public static boolean isFilteredGrailsFolder(IFolder folder) {
		if (isReimagedGrailsSourceFolder(folder)) {
			// Reimaged source folders are filtered, as
			// there are corresponding package fragment roots for them
			return true;
		} else if (isSourceFolder(folder)) {
			// if there is a source class path entry, it should be filtered out
			return true;
		}
		return false;
	}

	/**
	 * A grails folder id is represented by the last two segments in a given
	 * folder path.
	 * 
	 * @param folder
	 *            to determine the last 2 folder path segments
	 * @return the last two folder path segments
	 */
	public static String getGrailsFolderID(IFolder folder) {
		if (folder != null) {
			IProject project = folder.getProject();
			IPath projectLocation = project.getLocation();
			if (projectLocation==null) {
				GrailsCoreActivator.log(new Status(
						IStatus.WARNING, GrailsCoreActivator.PLUGIN_ID, "Couldn't compute project location for "+project+" in GrailsResourceUtil.getGrailsFolderID"));
			} else {
				IPath folderLocation = folder.getLocation();
				if (folderLocation==null) {
					GrailsCoreActivator.log(new Status(
							IStatus.WARNING, GrailsCoreActivator.PLUGIN_ID, "Couldn't compute folder location for "+folder+" in GrailsResourceUtil.getGrailsFolderID"));
				} else {
					folderLocation = folderLocation.makeRelativeTo(projectLocation);
					int segNumber = folderLocation.segmentCount();

					IPath comparison = (segNumber > 2) ? folderLocation.removeFirstSegments(segNumber - 2) : folderLocation;

					String segString = comparison.toPortableString();
					int device = segString.indexOf(IPath.DEVICE_SEPARATOR);
					if (device >= 0 && device + 1 < segString.length()) {
						return segString.substring(device + 1);
					}
					return segString;
				}
			}
		}
		return null;
	}

	/**
	 * Determines if the given folder is a grails project non-source file folder
	 * that is reimaged with a new icon and display name.
	 * 
	 * @param folder
	 *            to check if is a reimaged grails file folder
	 * @return true if it is a grails file folder, false otherwise
	 */
	public static boolean isReimagedGrailsProjectFileFolder(IFolder folder) {
		GrailsProjectStructureTypes type = getGrailsContainerType(folder);
		if (GrailsProjectStructureManager.getInstance().getGrailsFileFolders()
				.contains(type)) {
			return true;
		}
		return false;
	}

	/**
	 * Determines if the given source folder is a grails source folder that is
	 * reimaged with a different name and icon.
	 * 
	 * @param folder
	 *            to check if it is a reimaged grails source folder
	 * @return true if it is a reimaged folder, false otherwise
	 */
	public static boolean isReimagedGrailsSourceFolder(IFolder folder) {
		GrailsProjectStructureTypes type = getGrailsContainerType(folder);
		if (GrailsProjectStructureManager.getInstance()
				.getGrailsSourceFolders().contains(type)) {
			return true;
		}
		return false;
	}

	/**
	 * Determines if the given package fragment root is a grails source folder
	 * that is reimaged with a different name and icon.
	 * 
	 * @param root
	 *            to check if it is a reimaged grails package fragment root
	 * @return true if it is a reimaged folder, false otherwise
	 */
	public static boolean isReimagedGrailsSourceFolder(IPackageFragmentRoot root) {
		IFolder folder = getFolder(root);
		return isReimagedGrailsSourceFolder(folder);
	}

	/**
	 * Converts a grails dependency plugin name containing at least one dash
	 * into a path separator. Removes the plugin name and only returns the
	 * source package name with separators. <br>
	 * Example: <br>
	 * input: hibernate-10-src-groovy <br>
	 * output: src/groovy
	 * 
	 * 
	 * @param name
	 *            containing dashed plugin name to be converted to a OS path
	 *            separator
	 * @return converted name if it contains dashes, or the original name if it
	 *         contains no dashes, or null if original name is null
	 */
	public static String convertRootName(String name) {
		if (name == null) {
			return null;
		}

		int lastIndex = name.lastIndexOf('-');
		if (lastIndex < 0) {
			return name;
		}

		while (lastIndex > 0) {
			// find the second to last dash
			if (name.charAt(lastIndex - 1) == '-') {
				break;
			}
			lastIndex--;
		}

		String conversionPortion = name.substring(lastIndex);
		return conversionPortion.replace('-', IPath.SEPARATOR);
	}

	/**
	 * Given a folder, which may represent either a file folder, or source
	 * folder (package fragment or package fragment root), this will determine
	 * if there is a Grails container type definition for it. If so, returns
	 * true. False otherwise
	 * <p>
	 * Typically, a folder that is associated with a Grails container type is a
	 * reimaged folder that is shown in the Project Explorer with its own icon
	 * and label.
	 * </p>
	 * 
	 * @param folder
	 *            to check against a Grails container type
	 * @return true if there is an associated type for the folder. False
	 *         otherwise.
	 */
	public static GrailsProjectStructureTypes getGrailsContainerType(
			IFolder folder) {
		if (folder == null) {
			return null;
		}
		String id = getGrailsFolderID(folder);
		if (id!=null) {
			if (GrailsProjectStructureManager.getInstance()
					.getAllGrailsFolderNames().contains(id)) {
				GrailsProjectStructureTypes[] types = GrailsProjectStructureTypes.values();
				for (GrailsProjectStructureTypes type : types) {
					if (type.getFolderName().equals(id)) {
						return type;
					}
				}
			}
		}

		return null;
	}

	/**
	 * Determines if the given folder corresponds to a Java source folder (i.e.
	 * there is a source class path entry for it).
	 * <p>
	 * The folder could be either a package fragment or a package fragment root
	 * </p>
	 * 
	 * @param folder
	 *            to check if there is a corresponding source class path entry
	 *            for it
	 * @return true if there is a corresponding source class path entry for it.
	 *         False otherwise.
	 */
	public static boolean isSourceFolder(IFolder folder) {
		if (folder == null) {
			return false;
		}
		IJavaElement possiblePackageFragRoot = JavaCore.create(folder);
		if (possiblePackageFragRoot == null) {
			return false;
		}

		if (possiblePackageFragRoot instanceof IPackageFragment) {
			possiblePackageFragRoot = ((IPackageFragment) possiblePackageFragRoot)
					.getParent();
		}

		if (possiblePackageFragRoot instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root = (IPackageFragmentRoot) possiblePackageFragRoot;
			try {
				if (root.getRawClasspathEntry().getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					return true;
				}
			} catch (JavaModelException e) {
				GrailsCoreActivator.log(e);
			}
		}
		return false;
	}

	/**
	 * Given a package fragment root, this will determine if it has a Grails
	 * container type. If so, it most likely is a reimaged root.
	 * 
	 * @param root
	 *            to check for grails container type
	 * @return true if it has a type, false otherwise
	 */
	public static GrailsProjectStructureTypes getGrailsContainerType(
			IPackageFragmentRoot root) {
		IFolder folder = getFolder(root);

		if (folder != null) {
			return getGrailsContainerType(folder);
		}

		return null;
	}

	/**
	 * Retrieves the corresponding workspace folder for the given root, or null
	 * if it cannot be resolved.
	 * 
	 * @param root
	 *            to obtain workspace folder
	 * @return workspace folder, or null if it cannot be resolved
	 */
	protected static IFolder getFolder(IPackageFragmentRoot root) {
		if (root == null) {
			return null;
		}
		try {
			IResource resource = root.getCorrespondingResource();
			if (resource instanceof IFolder) {
				return (IFolder) resource;
			}
		} catch (JavaModelException e) {
			GrailsCoreActivator.log(e);
		}
		return null;

	}

	/**
	 * @return true if the resource is either one of the Grails source folders
	 *         containing unit/integration/functional tests.
	 */
	public static boolean isTestFolder(IResource resource) {
		IPath path = resource.getProjectRelativePath();
		String[] segments = path.segments();
		if (segments.length==2) {
			return typeOfTest(resource)!=null;
		}
		return false;
	}

	/**
	 * Checks whether a given resource is inside the "test" folder.
	 * @param resource
	 * @return The type of the test (unit/integration/functional) if the resource is inside
	 *         the test folder. Return null if the resource is not in the test folder.
	 */
	public static String typeOfTest(IResource resource) {
		IPath path = resource.getProjectRelativePath();
		String[] segments = path.segments();
		if (segments.length>=2 && segments[0].equals("test")) {
			return segments[1];
		}
		return null;
	}

	public static boolean isSourceFile(IResource resource) {
		IJavaElement javaElement = (IJavaElement) resource.getAdapter(IJavaElement.class);
		return javaElement!=null && isSourceFile(javaElement);
	}

	public static boolean isSourceFile(IJavaElement javaElement) {
		IPackageFragmentRoot root = (IPackageFragmentRoot) javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		return root!=null && isSourceFolder(root);
	}

	public static boolean isSourceFolder(IPackageFragmentRoot root) {
		IJavaProject jp = root.getJavaProject();
		try {
			IResource rootRsrc = root.getCorrespondingResource();
			if (rootRsrc==null) {
				return false;
			}
			IPath rootPath = rootRsrc.getFullPath();
			for (IClasspathEntry entry : jp.getRawClasspath()) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					if (entry.getPath() != null && entry.getPath().equals(rootPath)) {
						return true;
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * If object is a {@link IResource} or adapatable to {@link IResource} then this returns the corresponding
	 * IREsource otjerwise it returns null.
	 */
	public static IResource asResource(Object object) {
		if (object instanceof IResource) {
			return (IResource) object;
		} else if (object instanceof IAdaptable) {
			return (IResource) ((IAdaptable) object).getAdapter(IResource.class);
		}
		return null;
	}

	public static boolean isDefaultOutputFolder(IFolder folder) {
		IPath filteredPath = new Path(GrailsCommandUtils.DEFAULT_GRAILS_OUTPUT_FOLDER);
		IPath folderPath = folder.getProjectRelativePath();
		return folderPath.isPrefixOf(filteredPath);
	}

}
