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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.model.GrailsBuildSettingsException;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.runtime.shared.DependencyData;
import org.osgi.framework.Bundle;
import org.springsource.ide.eclipse.commons.core.SpringCorePreferences;


/**
 * @author Christian Dupuis
 * @author Andrew Eisenberg
 * @author Andy Clement
 * @author Nieraj Singh
 * @author Kris De Volder
 * @since 2.2.0
 */
public class GrailsClasspathContainer implements IClasspathContainer {

    /**
     * DSL support folder relative to the current grails install
     */
    private static final String DSL_SUPPORT_FOLDER = "dsl-support";

    public static final String PLUGIN_SOURCEFOLDER_ATTRIBUTE_NAME = "org.grails.ide.eclipse.core.SOURCE_FOLDER";

	public static final String CLASSPATH_ATTRIBUTE_VALUE = GrailsCoreActivator.PLUGIN_ID + ".CLASSPATH_ENTRY";

	public final static IClasspathAttribute[] NO_EXTRA_ATTRIBUTES = {};

	public final static IAccessRule[] NO_ACCESS_RULES = {};

	/** Classpath container */
	public static final String CLASSPATH_CONTAINER_DESCRIPTION = "Grails Dependencies";

	/** Name of this class path container to be stored by JDT */
	private static final String CLASSPATH_CONTAINER = GrailsCoreActivator.PLUGIN_ID + ".CLASSPATH_CONTAINER";

	/** Unique path of this class path container */
	public static final IPath CLASSPATH_CONTAINER_PATH = new Path(CLASSPATH_CONTAINER);

	private String description = "";

	/** The calculated and stored {@link IClasspathEntry}s */
	private IClasspathEntry[] entries;

	/** The internal flag to indicate the container has been initialized */
	private volatile boolean initialized = false;

	/**
	 * The {@link IJavaProject} this class path container instance is responsible for
	 */
	IJavaProject javaProject;

	/** Set of full file paths to plugin.xml files */
	private Set<String> pluginDescriptors;

	/**
	 * Constructor to create a new class path container
	 * 
	 * @param javaProject the {@link IJavaProject} that this container is responsible for
	 */
	public GrailsClasspathContainer(IJavaProject javaProject) {
		this.javaProject = javaProject;
		this.entries = new IClasspathEntry[0];
	}

	/**
	 * Constructor to create a new class path container
	 * 
	 * @param javaProject the {@link IJavaProject} that this container is responsible for
	 * @param entries populate the list of {@link IClasspathEntry}s with the given list
	 */
	public GrailsClasspathContainer(IJavaProject javaProject, IClasspathEntry[] entries) {
		this.javaProject = javaProject;
		this.entries = entries;
	}

	/**
	 * Returns the {@link IClasspathEntry}s calculated by this class path container
	 */
	public synchronized IClasspathEntry[] getClasspathEntries() {
		// make sure that the container is initialized on first access
		if (!initialized) {
			// refresh container before giving out the empty entries list
			refreshClasspathEntries();
		}
		return this.entries;
	}

	/**
	 * Returns the description for this class path container
	 */
	public String getDescription() {
		return CLASSPATH_CONTAINER_DESCRIPTION;
	}

	public String getDescriptionSuffix() {
		return description;
	}

	/**
	 * Returns the kind of this class path container
	 */
	public int getKind() {
		return K_APPLICATION;
	}

	/**
	 * Returns the path of the class path container
	 */
	public IPath getPath() {
		return CLASSPATH_CONTAINER_PATH;
	}

	public synchronized void invalidate() {
		entries = null;
		initialized = false;
	}

	/**
	 * Refresh the class path entries of the given {@link IJavaProject}.
	 * <p>
	 * This will install the new class path entries on the java project only if the entries have changed since the last
	 * refresh.
	 */
	private void refreshClasspathEntries() {
		final List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();

		IProject project = javaProject.getProject();
		try {

			PerProjectDependencyDataCache info = GrailsCore.get().connect(project, PerProjectDependencyDataCache.class);
			PerProjectAttachementsCache attachements = PerProjectAttachementsCache.get(project);
			IGrailsInstall install = GrailsCoreActivator.getDefault().getInstallManager()
					.getGrailsInstall(javaProject.getProject());
			if (install != null) {
				description = " [" + install.getName() + "]";
			}
			// info can null if classpath container is a on project without the nature; so be extra careful
			if (info != null && info.getData() != null) {
				DependencyData data = info.getData();

				// These are the paths to the source folders
				// Class path entries are created from this data
				Set<String> dependencies = data.getDependencies();

				// These are the locations for the plugin.xml files, but
				// are NOT used to create class path entries. They are simply
				// parsed at the same time as the dependencies to ensure
				// that whoever requests descriptors will be getting them
				// from the same class path container that also created the
				// class path entries, since both pieces of information
				// are obtained from the same dependency parser
				this.pluginDescriptors = data.getPluginDescriptors();

				for (String fileDescriptor : dependencies) {
					// don't link in .svn and CVS folders; would eventually make
					// sense to not link in folders starting
					// with '.'; see STS-745
					File file = new File(fileDescriptor);
					String name = file.getName();
					if (name.startsWith(".svn") || name.equals("CVS")) {
						continue;
					}
					if (!(file.exists() && file.canRead())) {
						continue;
					}

					IPath sourceFile = getSourceAttachement(project, attachements,  file);

					IClasspathAttribute[] attributes = NO_EXTRA_ATTRIBUTES;
					String javaDocPath = GrailsClasspathContainer.getJavaDocLocation(project, file);
					if (javaDocPath != null) {
						attributes = new IClasspathAttribute[] { JavaCore.newClasspathAttribute(
								IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javaDocPath) };
					}

					entries.add(JavaCore.newLibraryEntry(new Path(file.getAbsolutePath()), sourceFile, null,
							NO_ACCESS_RULES, attributes, false));
				}
				
				//STS-2084: temporary workaround, revisit after M2 is no longer interesting
				//Note: still a problem in build snapshot since M2 but should reevaluate on each milestone release
				if (install != null && GrailsVersion.V_1_3_7.compareTo(install.getVersion())<=0
						&& GrailsVersion.V_2_0_3.compareTo(install.getVersion())>0) {
					//if (GrailsNature.isGrailsPluginProject(project)) {
						File ivyLib = getIvyLib(install);
						if (ivyLib!=null) {
							entries.add(JavaCore.newLibraryEntry(new Path(ivyLib.getAbsolutePath()), null, null,
									NO_ACCESS_RULES, NO_EXTRA_ATTRIBUTES, false));
						}
					//}
				}
				
				//At the very end at the 'plugin-classes' directory as a library
				String pluginClassesStr = data.getPluginClassesDirectory();
				if (pluginClassesStr!=null) {
					File pluginClassesDir = new File(pluginClassesStr);
					if (pluginClassesDir.exists()) {
						entries.add(JavaCore.newLibraryEntry(Path.fromOSString(pluginClassesStr), null, null));
					}
				}
			}
			else {
				//Dependency file doesn't exist and we cannot build it now, since that takes too long.
				description += " (uninitialized)";
				//The project will get compiled by JDT with errors. See STS-1347
				//We must make sure that sometime soon the dependencies are going to be refreshed:
				if (!isInWonkyState(javaProject)) {
					//Don't auto-refresh projects that are in a 'wonky' state. This will probably just cause spurious errors
					//in the error log, as well work that need not be done at all.
					GrailsClasspathContainerUpdateJob.scheduleClasspathContainerUpdateJob(javaProject, false);
				}
			}
		}
		catch (GrailsBuildSettingsException e) {
			GrailsCoreActivator.log("Issue with external Grails installation", e);

		}
		finally {
		    // add the grails.dsld here.  It is appropriate to place it outside of the try-catch block
		    // since it should be added regardless of whether or not the rest of the classpath container
		    // was built successfully.
		    IClasspathEntry dsldClasspathEntry = findGrailsDsld();
		    if (dsldClasspathEntry != null) {
		        entries.add(dsldClasspathEntry);
		    }
		    
			this.entries = entries.toArray(new IClasspathEntry[entries.size()]);
			this.initialized = true;
		}
	}

	/**
	 * When project is in a 'wonky' state we don't do auto-refreshes because it can cause much work and spurious errors.
	 * Since project is in wonky state it probably already needs repairing so even if the refresh succeeds it
	 * probably populates the container with the wrong information! So this work is just not worth it.
	 */
	private boolean isInWonkyState(IJavaProject project) {
		try {
			GrailsVersion eclipseVersion = GrailsVersion.getEclipseGrailsVersion(project.getProject());
			GrailsVersion grailVersion = GrailsVersion.getGrailsVersion(project.getProject());
			boolean looksFine = !eclipseVersion.equals(GrailsVersion.UNKNOWN) 
					&& eclipseVersion.equals(grailVersion);
			return !looksFine;
		} catch (Exception e) {
			GrailsCoreActivator.log(e);
			return true; //Errors where caught. Not sure why but we'll call that wonky too :-)
		}
	}

	private File getIvyLib(IGrailsInstall install) {
		String[] locations = {
			"lib/ivy-2.2.0.jar", //1.3.x
			"lib/org.apache.ivy/ivy/jars/ivy-2.2.0.jar", //2.0.1 and 2.0.3
			"lib/org.apache.ivy/ivy/2.2.0/jar/ivy-2.2.0.jar" //2.0.2
		};
		for (String loc : locations) {
			File file = new File(install.getHome(), loc);
			if (file.exists()) {
				return file;
			}
		}
		return null;
	}

	public IPath getSourceAttachement(IProject project, PerProjectAttachementsCache attachements, File file) {
		IPath sourceFile = getSourceAttachmentFromUserPrefs(project, file);
		if (sourceFile == null) {
			sourceFile = attachements.getSourceAttachement(file.toString());
			if (sourceFile==null) {
				sourceFile = getDefaultSourceAttachment(project, file);
			}
		}
		return sourceFile;
	}

	/**
	 * Returns the classpath entry corresponding to the external location where
	 * the grails.dsld is located.  This is in the c.s.s.grails.resources plugin
     * @return the classpath entry or null if it does not exist
     */
    private IClasspathEntry findGrailsDsld() {
        IGrailsInstall install = GrailsVersion.getEclipseGrailsVersion(javaProject.getProject()).getInstall();
        // only use for 2.0 or greater
        if (install != null && install.getVersion().compareTo(GrailsVersion.V_1_3_7) > 0) {
        	Bundle b = Platform.getBundle(GrailsCoreActivator.GRAILS_RESOURCES_PLUGIN_ID);
            if (b != null) {
            	URL entry = b.getEntry(DSL_SUPPORT_FOLDER);
            	URL resolvedEntry;
                try {
                    resolvedEntry = FileLocator.resolve(entry);
                    IPath jarPath = new Path(resolvedEntry.getPath());
                    return JavaCore.newLibraryEntry(jarPath, null, null);
                } catch (IOException e) {
                	GrailsCoreActivator.log(e);
                }
            }
        }
        return null;
    }

    public Set<String> getPluginDescriptors() {
		return pluginDescriptors;
	}

	/**
	 * Stores the configured source attachments paths in the projects settings area.
	 * @param project the java project to store the preferences for
	 * @param containerSuggestion the configured classpath container entries
	 */
	public static void storeSourceAttachments(IJavaProject project, IClasspathContainer containerSuggestion) {
		SpringCorePreferences prefs = SpringCorePreferences.getProjectPreferences(project.getProject(),
				GrailsCoreActivator.PLUGIN_ID);
		for (IClasspathEntry entry : containerSuggestion.getClasspathEntries()) {
			IPath path = entry.getPath();
			IPath sourcePath = entry.getSourceAttachmentPath();
			if (sourcePath != null) {
				prefs.putString("source.attachment-" + path.lastSegment().toString(), sourcePath.toString());
			}
			for (IClasspathAttribute attribute : entry.getExtraAttributes()) {
				if (attribute.getName().equals(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME)) {
					String value = attribute.getValue();
					prefs.putString("javadoc.location-" + path.lastSegment().toString(), value);
				}
			}
		}
	}

	/**
	 * Returns the default source attachment for a given jar, if one exists. Should only exist for the grails jars
	 * @param project the java project which preferences needs to be checked.
	 * @param file the jar that needs a source attachment
	 * @return path to the default source attachment or null if none exists
	 */
	public static IPath getDefaultSourceAttachment(IProject project, File file) {
		if (file.getName().startsWith("grails-")) {
			IGrailsInstall install = GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(project);
			if (install!=null) {
				if (file.getName().startsWith("grails-script")) {
					// STS-1778: this jar is special! Different source folder!
					return new Path(install.getHome()).append("scripts");
				} else {
				    if (GrailsVersion.V_2_0_0.compareTo(install.getVersion()) <= 0) {
				        // >= 2.0.0 return the sources jar in the src folder
				        return new Path(install.getHome()).append("src").append(file.getName().replace(".jar", "-sources.jar"));
				    } else {
				        // <= 1.3.7 return the grails/src/java path
				        return new Path(install.getHome()).append("src").append("java");
				    }
				}
			}
		}
		return null;
	}

	/**
	 * Returns configured javadoc attachment paths for a given jar resource path.
	 * @param project the java project which preferences needs to be checked.
	 * @param file the jar that needs a source attachment
	 */
	public static String getJavaDocLocation(IProject project, File file) {
		SpringCorePreferences prefs = SpringCorePreferences.getProjectPreferences(project,
				GrailsCoreActivator.PLUGIN_ID);
		return prefs.getString("javadoc.location-" + file.getName(), null);
	}

	/**
	 * Returns configured source attachment paths for a given jar resource path.
	 * @param project the java project which preferences needs to be checked.
	 * @param file the jar that needs a source attachment
	 */
	public static IPath getSourceAttachmentFromUserPrefs(IProject project, File file) {
		SpringCorePreferences prefs = SpringCorePreferences.getProjectPreferences(project,
				GrailsCoreActivator.PLUGIN_ID);
		String value = prefs.getString("source.attachment-" + file.getName(), null);
		if (value != null) {
			return new Path(value);
		}
		return null;
	}

	/**
	 * Two Grails class path containers are equal if all class path entries are the same AND all plugin descriptors are
	 * also the same. If both are null, it also returns true. It return false in other cases.
	 */
	public static boolean areContainersEqual(GrailsClasspathContainer container1, GrailsClasspathContainer container2) {
		if (container1 == container2) {
			return true;
		}

		if (container1 != null && container2 != null) {
			Set<String> thisPluginDescriptors = container1.getPluginDescriptors();
			Set<String> otherPluginDescriptors = container2.getPluginDescriptors();
			if (!areDescriptorsEqual(thisPluginDescriptors, otherPluginDescriptors)) {
				return false;
			}

			return Arrays.deepEquals(container1.getClasspathEntries(), container2.getClasspathEntries());

		}

		return false;

	}

	/**
	 * Returns true if either both descriptor sets are null, both are empty, or both contain the exact same content.
	 * Returns false otherwise.
	 */
	protected static boolean areDescriptorsEqual(Set<String> set1, Set<String> set2) {

		if (set1 == set2) {
			return true;
		}

		if (set1 != null && set2 != null && set1.size() == set2.size()) {
			for (String descriptor : set1) {
				if (!set2.contains(descriptor)) {
					return false;
				}
			}
			return true;
		}

		return false;
	}

	public static boolean isGrailsClasspathContainer(IClasspathEntry entry) {
		if (entry.getEntryKind()==IClasspathEntry.CPE_CONTAINER) {
			return entry.getPath().equals(CLASSPATH_CONTAINER_PATH);
		}
		return false;
	}
}
