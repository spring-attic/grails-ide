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
package org.grails.ide.eclipse.core.internal.classpath;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.CharsetFixer;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.workspace.GrailsClassPath;
import org.grails.ide.eclipse.core.workspace.GrailsProject;
import org.grails.ide.eclipse.core.workspace.GrailsWorkspace;

/**
 * This class should be renamed.
 * This class refreshes the classpath of a project so that it includes
 * all source folders contributed from plugins.
 * @author Andrew Eisenberg
 * @author Kris De Volder
 * @author Andy Clement
 */
public class SourceFolderJob {

	/**
	 * Name of the linked folder that points to the folder containing plugin source folders 
	 * inside the .grails folder.
	 */
	public static final String PLUGINS_FOLDER_LINK = ".link_to_grails_plugins";

	private final static boolean DEBUG = false;

	
	private static void debug(String string) {
		if (DEBUG) {
			System.out.println("SourceFolderJob: "+string);
		}
	}

    private final IClasspathAttribute[] CLASSPATH_ATTRIBUTE = new IClasspathAttribute[] { JavaCore
			.newClasspathAttribute(GrailsClasspathContainer.PLUGIN_SOURCEFOLDER_ATTRIBUTE_NAME, "true") }; //$NON-NLS-1$

	private final IJavaProject javaProject;

	private Set<IProject> handledInPlaceProjects = new HashSet<IProject>();
	
	public SourceFolderJob(IJavaProject javaProject) {
		this.javaProject = javaProject;
	}

	private void addToClasspath(IJavaProject javaProject,
			List<IClasspathEntry> oldEntries,
			List<IClasspathEntry> newEntries, IProgressMonitor monitor) {
		try {
			debug("addToClassPath");
			debug("  old = "+oldEntries);
			debug("  new = "+newEntries);
			if (oldEntries.equals(newEntries)) {
				debug("exit old and new are the same");
				return; // Nothing to do!
			}

			GrailsProject grailsProject = GrailsWorkspace.get().create(javaProject);
			GrailsClassPath current = grailsProject.getClassPath();
			// first remove the old ones
			current.removeAll(oldEntries);

			// add new ones
			current.addAll(newEntries);

//			if (DEBUG) {
//				debug("setting classpath for "+javaProject.getElementName());
//				for (IClasspathEntry entry : current) {
//					debug("  "+entry);
//				}
//			}
			grailsProject.setClassPath(current, monitor);
			if (DEBUG) {
				debug("classpath is now");
				for (IClasspathEntry entry : javaProject.getRawClasspath()) {
					debug("  "+entry);
				}
			}

			cleanupLegacyLinkedSourceFolders(oldEntries);
			
		} catch (JavaModelException e) {
			GrailsCoreActivator.log(e);
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
		}
	}
	
	public static void cleanupLegacyLinkedSourceFolders(List<IClasspathEntry> oldEntries) {
		
		IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
		for (IClasspathEntry entry : oldEntries) {
			//Note: only entries passed here are those that are in fact plugin source entries.
			// So we don't need to check that they are.
			try {
				IPath sourcePath = entry.getPath();
				IFolder sourceFolder = wsRoot.getFolder(sourcePath);
				if (sourceFolder.isLinked()) {
					// In the current implementation, plugin source folders are not themselves linked source folders, they
					// are instead folders nested inside a single linked folder.
					// If we find any 'oldEntry' that is linked, it must be a legacy folder
					debug("Deleting legacy source folder: "+sourceFolder);
					sourceFolder.delete(true, new NullProgressMonitor());
				}
			} catch (Exception e) {
				// Don't report... just don't clean it up since it obviously isn't what
				// we expected.
			}
		}
	}

	/**
     * 
     * @return all grails source class path entries. List may be empty or
     *         partial complete if error encountered.
     */
    public static List<IClasspathEntry> getGrailsSourceClasspathEntries(IJavaProject javaProject) {
        List<IClasspathEntry> oldEntries = new ArrayList<IClasspathEntry>();
        try {
            for (IClasspathEntry entry : javaProject.getRawClasspath()) {
                // CPE_PROJECT is for in-place plugins that are present as
                // project entries, as opposed to CPE_SOURCE that are for source
                // folders
                if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE
                        || entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                    for (IClasspathAttribute attr : entry.getExtraAttributes()) {
                        if (attr.getName().equals(GrailsClasspathContainer.PLUGIN_SOURCEFOLDER_ATTRIBUTE_NAME)) {
                            oldEntries.add(entry);
                        }
                    }
                }
            }
        } catch (JavaModelException e) {
            GrailsCoreActivator.log(e);
        }
        return oldEntries;
    }


	public IStatus refreshSourceFolders(IProgressMonitor monitor) {
        IProject project = javaProject.getProject();
        DependencyData data = GrailsCore.get().connect(project, PerProjectDependencyDataCache.class).getData();
        if (data == null) {
            return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Invalid plugin descriptor for " + project.getName()); //$NON-NLS-1$
        }
        IPath pluginsDirectory = toPath(data.getPluginsDirectory());
        Set<String> pluginSources = data.getSources();

		try {
			IFolder linkToPluginsFolder = project.getFolder(PLUGINS_FOLDER_LINK);
			ensureLink(linkToPluginsFolder, pluginsDirectory);

			List<IClasspathEntry> oldEntries = getGrailsSourceClasspathEntries(javaProject);
			
			List<IClasspathEntry> newEntries = new ArrayList<IClasspathEntry>();
			List<IClasspathEntry> newInplaceEntries = new ArrayList<IClasspathEntry>();
			
			for (String fileDescriptor : pluginSources) {
				debug("processing "+pluginSources);
				IProject inPlaceProject = GrailsPluginUtil.findContainingProject(fileDescriptor);
				if (inPlaceProject != null && !inPlaceProject.equals(project)) {
					// In-place Grails plugins are treated specially: 
					// project entries are created as opposed to source folder entries
					IClasspathEntry entry = createInPlaceProjectClassPath(inPlaceProject);
					if (entry != null) {
						debug("inplace: "+pluginSources);
						newInplaceEntries.add(entry);
					}
				} else {
					// Add source folder entry to the classpath. 
					// Note: classpath containers can't contain source entries.
					File sourceFile = new File(fileDescriptor);
					if (sourceFile.exists() && sourceFile.isDirectory()) {
						debug("normal: "+pluginSources);

						Path pathToSourceFolder = new Path(sourceFile.toString());
						if (!pluginsDirectory.isPrefixOf(pathToSourceFolder)) {
							GrailsCoreActivator.log(new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Broken assumption \n" +
									"plugin source folder: "+pathToSourceFolder+"\n" +
									"is not nested inside plugins directory: "+pluginsDirectory));
						} else {
							// Only execute if assumption holds: pluginsDirectory is prefix of pathToSourceFolder
							IPath pluginDirRelativePath = pathToSourceFolder.removeFirstSegments(pluginsDirectory.segmentCount());
							IFolder sourceFolder = linkToPluginsFolder.getFolder(pluginDirRelativePath);
							newEntries.add(JavaCore.newSourceEntry(
									sourceFolder.getFullPath(), null,
									getExclusionsForFolder(sourceFolder), 
									null,
									CLASSPATH_ATTRIBUTE));
						}
					}
					else {
						debug("skipped: "+sourceFile);
					}
				}
			}
			newEntries.addAll(newInplaceEntries);
			if (newEntries.size() > 0 || oldEntries.size() > 0) {
				addToClasspath(javaProject, oldEntries, newEntries, monitor);
			}
			
			// ensure that the i18n folders have correct charsets
			new CharsetFixer(javaProject.getProject()).fixEncodings(new SubProgressMonitor(monitor, 2));
		} catch (Exception e) {
			GrailsCoreActivator.log(e);
		}

		return Status.OK_STATUS;
	}

	/**
	 * Ensure that a given folder is a link to a given target location.
	 * @throws CoreException 
	 */
	private void ensureLink(IFolder folder, IPath target) throws CoreException {
		target = getPathWithVariablePrefix(target); // Try to use path variable if possible
		if (folder.exists()) {
			if (folder.isLinked()) {
				IPath currentLocation = folder.getLocation();
				if (target.equals(currentLocation)) {
					folder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
					return; //OK: It exists and links to the right place.
				} else {
					folder.delete(true, new NullProgressMonitor());
				}
			} else {
				throw new CoreException(new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Couldn't create link because a resource already exists: "+folder));
			}
		}
		// We should only get here if folder doesn't yet exist, or has been deleted because it was pointing to incorrect location.
		folder.createLink(target, IFolder.ALLOW_MISSING_LOCAL, new NullProgressMonitor());
	}

	private Set<IPath> toPath(Set<String> pathStrings) {
		LinkedHashSet<IPath> paths = new LinkedHashSet<IPath>();
		for (String s : pathStrings) {
			paths.add(new Path(s));
		}
		return paths;
	}

	private IPath toPath(String filePathName) {
		return new Path(filePathName);
	}

	private Map<String, IPath> variables = null; // Caches result of getExistingPathVariables
	
	private Map<String, IPath> getExistingPathVariables() {
		if (variables==null) {
			variables = new HashMap<String, IPath>();
			IPathVariableManager variableManager = ResourcesPlugin.getWorkspace().getPathVariableManager();
			for (String variableName : variableManager.getPathVariableNames()) {
				IPath path = variableManager.getValue(variableName);
				File file = path.toFile();
				if (file.exists() && file.isDirectory()) {
					variables.put(variableName, path);
				}
			}
		}
		return variables;
	}

	/**
	 * Creates a class path entry for an in-place plugin dependency. The
	 * entry should be of CPE_PROJECT kind.
	 */
	protected IClasspathEntry createInPlaceProjectClassPath(IProject project) {
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject == null || handledInPlaceProjects.contains(project)) {
			return null;
		}
		handledInPlaceProjects.add(project);
		return JavaCore.newProjectEntry(project.getFullPath(), null, true,
				CLASSPATH_ATTRIBUTE, true);
	}

	/**
	 * Return a new IPath that contains the original sourcePath with leading
	 * segments replaced with an appropriate Path variable. If no variables
	 * are substituted, return the original path.
	 * 
	 * @param variables
	 *            containing all Path variables in workspace
	 * @param sourcePath
	 *            that needs to have leading segments replaced with a Path
	 *            variable
	 * @return new IPath with Path variable, or null if no changes
	 */
	protected IPath getPathWithVariablePrefix(IPath sourcePath) {
		Map<String, IPath> variables = getExistingPathVariables();
		if (variables == null || sourcePath == null) {
			return sourcePath;
		}
		// Check if we have a matching variable defined

		IPath sourcePathWithVariable = null;
		int segmentMatch = 0;
		for (Map.Entry<String, IPath> entry : variables.entrySet()) {
			if (entry.getValue().isPrefixOf(sourcePath)) {
				// We have a match; now check that we
				// use the most specific match
				int currentSegmentMatch = sourcePath
						.matchingFirstSegments(entry.getValue());
				if (segmentMatch < currentSegmentMatch) {
					sourcePathWithVariable = new Path(entry.getKey())
							.append(sourcePath.removeFirstSegments(entry
									.getValue().segmentCount()));
					segmentMatch = currentSegmentMatch;
				}
			}
		}
		return sourcePathWithVariable==null ? sourcePath : sourcePathWithVariable;
	}

	/**
	 * Return the set of exclusions that should apply to the named folder.
	 * The exclusions follow the guidelines in
	 * grails.util.PluginBuildSettings.EXCLUDED_RESOURCES. These resources
	 * should be excluded from the linked source folder because they would
	 * not be part of the packaged plugin. These resources only need
	 * filtering for local plugins that are linking to folders in another
	 * project in the workspace. If installing a non-local plugin it is
	 * guaranteed not to have these things in anyway.<br>
	 * 
	 * As of grails 1.3.1, the exclusions are: public static final
	 * EXCLUDED_RESOURCES = [ "web-app/WEB-INF/**", "web-app/plugins/**",
	 * "grails-app/conf/spring/resources.groovy",
	 * "grails-app/conf/*DataSource.groovy",
	 * "grails-app/conf/DataSource.groovy",
	 * "grails-app/conf/BootStrap.groovy", "grails-app/conf/Config.groovy",
	 * "grails-app/conf/BuildConfig.groovy",
	 * "grails-app/conf/UrlMappings.groovy", "**<slash>.svn<slash>**",
	 * "test/**", "**<slash>CVS<slash>**" ]
	 */
	private IPath[] getExclusionsForFolder(IFolder sourceFolder) {
		IPath sourcePath = sourceFolder.getFullPath();
		if (sourcePath!=null && sourcePath.segmentCount()>0) {
			if (sourcePath.lastSegment().equals("conf")) {
				return exclusionsForConfFolder;
			}
			if (sourcePath.segmentCount()>=3) {
				//path looks like "/<project>/.link-to-plugins/<plugin-name>-<version>/...
				String pluginNameAndVersion = sourcePath.segment(2);
				if (pluginNameAndVersion.startsWith("spring-security-acl-") && sourcePath.lastSegment().equals("domain")) {
					return getExclusionsForSpringSecurityAcl();
				}
			}
		}
		return null;
	}

	/**
	 * Relates to https://issuetracker.springsource.com/browse/STS-1832 and
	 * https://issuetracker.springsource.com/browse/STS-1799
	 */
	private IPath[] getExclusionsForSpringSecurityAcl() {
		IFolder folder = javaProject.getProject().getFolder("grails-app/domain/org/codehaus/groovy/grails/plugins/springsecurity/acl");
		if (!folder.exists()) { 
			//Return quickly... classes not copied!
			return null;
		} else {
			ArrayList<IPath> exclusions = new ArrayList<IPath>(exclusionsForSpringSecurityAclDomain.length);
			for (IPath exclude : exclusionsForSpringSecurityAclDomain) {
				//looks like **/<some-groovy-file>
				String fileName = exclude.segment(1);
				IFile copiedFile = folder.getFile(fileName);
				if (copiedFile.exists()) {
					exclusions.add(exclude);
				}
			}
			return exclusions.toArray(new IPath[exclusions.size()]);
		}
	}

	private IPath[] exclusionsForConfFolder = new IPath[] {
			new Path("BuildConfig.groovy"), new Path("*DataSource.groovy"), //$NON-NLS-1$ //$NON-NLS-2$
			new Path("UrlMappings.groovy"), new Path("Config.groovy"), //$NON-NLS-1$ //$NON-NLS-2$
			new Path("BootStrap.groovy"), //$NON-NLS-1$
			new Path("spring/resources.groovy") }; //$NON-NLS-1$
	
	private IPath[] exclusionsForSpringSecurityAclDomain = new IPath[] {
			new Path("**/AclClass.groovy"),
			new Path("**/AclEntry.groovy"),
			new Path("**/AclObjectIdentity.groovy"),
			new Path("**/AclSid.groovy")
	};

}