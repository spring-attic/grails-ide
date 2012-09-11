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
package org.grails.ide.eclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathContainer;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathUtils;
import org.grails.ide.eclipse.core.internal.classpath.GrailsPluginVersion;
import org.grails.ide.eclipse.core.internal.classpath.PerProjectDependencyDataCache;
import org.grails.ide.eclipse.core.internal.classpath.SourceFolderJob;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.PerProjectPluginCache;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.model.GrailsBuildSettingsHelper;
import org.grails.ide.eclipse.core.model.GrailsInstallManager;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.core.workspace.GrailsClassPath;
import org.grails.ide.eclipse.core.workspace.GrailsProject;
import org.grails.ide.eclipse.core.workspace.GrailsWorkspace;


/**
 * Utility class where we can place methods that provide a variety of Eclipse
 * related bookkeeping operations, such as refreshing resources, recomputing
 * classpath container etc.
 * <p>
 * These operations are not part of the Grails command itself, but often need to
 * be executed as post-processing step with Grails commands.
 * @author Kris De Volder
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 */
public class GrailsCommandUtils {

    private static final String M2E_NATURE = "org.eclipse.m2e.maven2Nature";

    /**
	 * Defines the output folder that eclipsify project will configure a project with by default.
	 */
	public static final String DEFAULT_GRAILS_OUTPUT_FOLDER
		= "target-eclipse/classes";
		//old value: 
		//= "web-app/WEB-INF/classes";
	
	public static boolean DEBUG = false;
	private static void debug(String msg) {
		if (DEBUG) {
			System.out.println(msg);
		}
	}

	/**
	 * Newly created Grails projects (created by create-app / create-plugin)
	 * have a number of issues with their setup (classpath, project natures
	 * etc.). This method fixes those issues.
	 *  
	 * @param grailsInstall
	 *            The grails install that should be stored in the configuration
	 *            of the project, can be null, if null the default Grails
	 *            install will be used.
	 * @param isDefault
	 *            Configures whether this project uses a global default grails
	 *            install or uses a project specific Grails install. Is true,
	 *            the grailsInstall parameter will be ignored.
	 * @param path
	 *            absolute location of the project (where the .project file is
	 *            located). Must not be null and must point to a physical
	 *            location in the file system
	 * @throws CoreException
	 */
	public static IProject eclipsifyProject(IGrailsInstall grailsInstall,
			boolean isDefault, IPath projectAbsolutePath) throws CoreException {
		return eclipsifyProject(grailsInstall, isDefault, projectAbsolutePath,
				null);
	}

	/**
	 * Newly created Grails projects (created by create-app / create-plugin)
	 * have a number of issues with their setup (classpath, project natures
	 * etc.). This method fixes those issues.
	 * 
	 * @param grailsInstall
	 *            The grails install that should be stored in the configuration
	 *            of the project, can be null, if null the default Grails
	 *            install will be used.
	 * @param isDefault
	 *            Configures whether this project uses a global default grails
	 *            install or uses a project specific Grails install. Is true,
	 *            the grailsInstall parameter will be ignored.
	 * @param path
	 *            absolute location of the project (where the .project file is
	 *            located). If not specified, then an actual IProject must be
	 *            passed.
	 * @param project
	 *            The project to configure. If no project is specified, the an
	 *            absolute path to the project location (where the .project file
	 *            is), must be specified
	 * @throws CoreException
	 */
	private static IProject eclipsifyProject(IGrailsInstall grailsInstall,
			boolean isDefault, IPath projectAbsolutePath, IProject project)
			throws CoreException {

		if (grailsInstall == null) {
			grailsInstall = GrailsCoreActivator.getDefault()
					.getInstallManager().getDefaultGrailsInstall();
			if (grailsInstall == null) {
			    GrailsCoreActivator
                .log("Failed to create Grails project. No default grails version specified",
                        null);
			    return null;
			}
		}
		
		
		String grailsInstallName = grailsInstall.getName();

		IPath projectDescPath = projectAbsolutePath;

		// The project has higher priority than the path argument.
		if (project != null) {
			projectDescPath = project.getLocation();
			if (projectDescPath==null) {
				// Can be null, if project isn't created yet.
				projectDescPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(project.getName());
			}
		}

		if (projectDescPath == null) {
			GrailsCoreActivator
					.log("Failed to create Grails project. No path or project specified",
							null);
			return null;
		}

		projectDescPath = projectDescPath.append(".project");

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProjectDescription desc = workspace
				.loadProjectDescription(projectDescPath);

		if (desc != null) {

			if (project==null) {
				project = workspace.getRoot().getProject(desc.getName());
			}
			
//			boolean addJavaNature = 
			addNaturesAndBuilders(desc);

			if (!project.exists()) {
				project.create(desc, new NullProgressMonitor());
			}
			project.open(0, new NullProgressMonitor());
			project.setDescription(desc, new NullProgressMonitor());
			
			// save selected grails install
			GrailsInstallManager.setGrailsInstall(project, isDefault, grailsInstallName);
			
			GrailsClassPath entries = new GrailsClassPath();
			IJavaProject javaProject = JavaCore.create(project);
			GrailsProject grailsProject = GrailsWorkspace.get().create(project);
			
			//Nowadays, we always create all classpath entries from scratch...
			
			//But to avoid breaking test 
			//GrailsProjectVersionFixerTest.testCleanupLegacyLinkedSourceFolders()
			//We must ensure to cleanup the 'legacy' linked source folders from
			//before we changed this into using a single link to the plugins folder.
			SourceFolderJob.cleanupLegacyLinkedSourceFolders(SourceFolderJob.getGrailsSourceClasspathEntries(javaProject));
			
			// Add output folder
			setDefaultOutputFolder(javaProject);

			// Add source entries to classpath
			final String[] sourcePaths = { 
					"src/java", 
					"src/groovy",
					"grails-app/conf",
					"grails-app/controllers",
					"grails-app/domain",
					"grails-app/services",
					"grails-app/taglib",
					"grails-app/utils",
					"test/integration",
					"test/unit"
			};
			for (String srcPath : sourcePaths) {
				IFolder srcFolder = project.getFolder(srcPath);
				if (srcFolder.exists()) {
					entries.add(JavaCore.newSourceEntry(srcFolder.getFullPath()));
				}
			}
			//Add the Java libraries
			entries.add(JavaCore.newContainerEntry(Path.EMPTY.append(JavaRuntime.JRE_CONTAINER)));

			// Add the Grails classpath container
			entries.add(JavaCore.newContainerEntry(
					GrailsClasspathContainer.CLASSPATH_CONTAINER_PATH, null,
					null, false));
			grailsProject.setClassPath(entries, new NullProgressMonitor());
			
			// Make sure class path container and source folders are up-to-date
			refreshDependencies(javaProject, true); 

			javaProject.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, null);
			return project;
		}
		return null;
	}

	/**
	 * (Re)sets a given grails project's output folder to the default.
	 */
	public static void setDefaultOutputFolder(IJavaProject javaProject) throws JavaModelException {
		IProject project = javaProject.getProject();
		IFolder binDir = project.getFolder(DEFAULT_GRAILS_OUTPUT_FOLDER);
		IPath binPath = binDir.getFullPath();
		javaProject.setOutputLocation(binPath, null);
	}

	/**
	 * Adds natures and builders to a project descriptor.
	 * @param desc
	 * @return true if a Java nature was added, false if Java nature was already present.
	 */
	private static boolean addNaturesAndBuilders(IProjectDescription desc) {
		// prepare natures
		Set<String> natures = new LinkedHashSet<String>();
		natures.add(GrailsNature.NATURE_ID);
		natures.add("org.eclipse.jdt.groovy.core.groovyNature");
		for (String nature : desc.getNatureIds()) {
			if (!nature.contains("groovy")) {
				natures.add(nature);
			}
		}
		boolean addJavaNature = !natures.contains(JavaCore.NATURE_ID);
		if (addJavaNature) {
			natures.add(JavaCore.NATURE_ID);
		}
		
		natures.remove(GrailsNature.OLD_NATURE_ID);
		
		desc.setNatureIds(natures.toArray(new String[natures
				.size()]));

		// prepare builder
		Set<ICommand> builders = new LinkedHashSet<ICommand>();
		for (ICommand builder : desc.getBuildSpec()) {
			if (!builder.getBuilderName().contains("groovy")) {
				builders.add(builder);
			}
		}
		desc.setBuildSpec(builders.toArray(new ICommand[builders.size()]));
		return addJavaNature;
	}

	private static void setGrailsInstall(IProject project, IGrailsInstall grailsInstall) {
		GrailsInstallManager.setGrailsInstall(project, grailsInstall.isDefault() && GrailsInstallManager.inheritsDefaultInstall(project), grailsInstall.getName());
	}

	/**
	 * Newly created Grails projects (created by create-app / create-plugin)
	 * have a number of issues with their setup (classpath, project natures
	 * etc.). This method fixes those issues.
	 * 
	 * @param grailsInstall
	 *            The grails install that should be stored in the configuration
	 *            of the project, can be null, if null the default Grails
	 *            install will be used.
	 * @param isDefault
	 *            Configures whether this project uses a global default grails
	 *            install or uses a project specific Grails install. Is true,
	 *            the grailsInstall parameter will be ignored.
	 * @param project
	 *            The project to configure. Cannot be null
	 * @throws CoreException
	 */
	public static IProject eclipsifyProject(IGrailsInstall grailsInstall,
			boolean isDefault, IProject project) throws CoreException {
		return eclipsifyProject(grailsInstall, isDefault, null, project);
	}

	/**
	 * Recompute the Grails class path container. Essentially this performs the
	 * same action as the "Refresh Dependencies" Grails menu command.
	 * <p>
	 * This is a potentially long running process and so it should not be called
	 * directly from the UI thread. When running in the UI thread you should
	 * wrap calls to this (and other work you are possibly doing alongside with
	 * this in some type of background Job.).
	 */
	public static void refreshDependencies(final IJavaProject javaProject, boolean showOutput) throws CoreException {
		debug("Refreshing dependencies for "+javaProject.getElementName()+" ...");
		GroovyCompilerVersionCheck.check(javaProject);
		deleteOutOfSynchPlugins(javaProject.getProject());

		// Create the external process part and launch it synchronously...
		GrailsCommand refreshFileCmd = GrailsCommandFactory.refreshDependencyFile(javaProject.getProject());
		refreshFileCmd.setShowOutput(showOutput);
		ILaunchResult result = refreshFileCmd.synchExec();
		debug(result.toString());

		//TODO: KDV: (depend) if we do it right, we should be able to remove the call to the refreshFileCmd below. However, this
		//   assumes that 
		//    a) we ensure that any command that may change the state of the dependencies also forces
		//      the regeneration of the data file as part of its own execution. (Currently this isn't the case)
		//    b) RefreshGrailsDependencyActionDelegate also forces the data file to be regenerated somehow
		// Making this change is desirable (executing the command below takes a long time).
		// Making this change is difficult at the moment because many commands do not go via the GrailsCommand
		// class. In particular, commands executed via the command prompt still directly use the old GrailsLaunchConfigurationDelegate,

		//		ILaunchConfiguration configuration = GrailsDependencyLaunchConfigurationDelegate
		//		.getLaunchConfiguration(javaProject.getProject());
		//		SynchLaunch sl = new SynchLaunch(configuration, GrailsCoreActivator.getDefault().getGrailsCommandTimeOut());
		//		sl.setShowOutput(showOutput);
		//		sl.synchExec();

		// ensure that this operation runs without causing multiple builds
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.run(new IWorkspaceRunnable() {

			public void run(IProgressMonitor monitor) throws CoreException {
			    // This job is a no-op for maven projects since maven handles the source folders
                if (isMavenProject(javaProject)) {
                    return;
                }
			    
				// Grails "compile" command may have changed resources...?
				// TODO: KDV: (depend) find out why this refresh is necessary. See STS-1263.
				// Note: if this line is removed, it *will* break STS-1270. We should revisit
				// where calls are being made to refresh the resource tree. Suspect we may doing this more than 
				// once in some cases.
				javaProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);

				// Now that we got here the data file should be available and 
				// we can ask GrailsClasspathContainer to refresh its dependencies.
				GrailsClasspathContainer container = GrailsClasspathUtils.getClasspathContainer(javaProject);
				// reparse classpath entries from dependencies file on next request
				if (container != null) {
					container.invalidate();
				}

				// ensure that the dependency and plugin data is forgotten
				GrailsCore.get().connect(javaProject.getProject(), PerProjectDependencyDataCache.class).refreshData();
				GrailsCore.get().connect(javaProject.getProject(), PerProjectPluginCache.class).refreshDependencyCache();

                // recompute source folders now
				SourceFolderJob updateSourceFolders = new SourceFolderJob(javaProject);
				updateSourceFolders.refreshSourceFolders(new NullProgressMonitor());
				
				// This will force the JDT to re-resolve the CP, even if only the "contents" of class path container changed see STS-1347
				javaProject.setRawClasspath(javaProject.getRawClasspath(), monitor);
			}

            private boolean isMavenProject(IJavaProject javaProject) throws CoreException {
            	try {
            		return javaProject.getProject().hasNature(M2E_NATURE);
            	} catch (CoreException e) {
            		GrailsCoreActivator.log(e);
            		return false;
            	}
            }
		}, new NullProgressMonitor());
		debug("Refreshing dependencies for "+javaProject.getElementName()+" DONE");
	}
	
	/**
	 * Grails 1.3.5 and 1.3.6 won't update plugin versions during grails compile when the update is
	 * a "downgrade" to an older version. To remedy this, a workaround is to delete the
	 * plugins folders in the .grails folder that are causing problems.
	 * <p>
	 * See STS-1263
	 */
	public static void deleteOutOfSynchPlugins(IProject project) {
		IGrailsInstall install = GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(project);
		if (install == null || GrailsVersion.UNKNOWN.equals(install.getVersion())) {
		    throw new IllegalArgumentException("Could not find a grails install for '"+project.getName()+"'. " +
		    		"Please configure an install from the Grails preferences page.");
		}
		GrailsVersion grailsVersion = install.getVersion();
		if (true/*GrailsVersion.V_1_3_5.compareTo(grailsVersion)<=0*/) {
			//This workaround is only required for grails 1.3.5 (until the bug that requires it is fixed)
			//The workaround should not be harmful even if the bug it addresses is fixed.
			PerProjectPluginCache pluginCache = GrailsCore.get().connect(project, PerProjectPluginCache.class);
			PerProjectDependencyDataCache depDataCache = GrailsCore.get().connect(project, PerProjectDependencyDataCache.class);
			Map<String, GrailsPluginVersion> pluginMap = pluginCache.getPluginDataMap();

			Properties props = GrailsBuildSettingsHelper.getApplicationProperties(project);

			for (Map.Entry<String, GrailsPluginVersion> entry : pluginMap.entrySet()) {
				String pluginXml = entry.getKey();
				GrailsPluginVersion grailsPluginVersion = entry.getValue();
				//Now we need to decide if this plugin should be deleted from the .grails folder
				String pluginName = grailsPluginVersion.getName();
				String pluginInstalledVersion = grailsPluginVersion.getVersion();
				String propVersion = (String) props.get("plugins."+pluginName);
				debug("Current plugin: "+pluginName+" version: "+pluginInstalledVersion + " application.properties = "+propVersion);
				if (propVersion!=null) {
					if (!propVersion.equals(pluginInstalledVersion)) {
						//Plugin exists in both the .grails folder and application.properties
						//and versions are out of synch ==> Delete it!
						
						// Complication: if a user adds the inplace plugin to application.properties the code below may end
						// up deleting the inplace plugin (which is extremely bad, since that is the user's code!).
						// This scenario is unlikely since inplace plugins are not usually added to application.properties 
						// (since this doesn't even work), but we check for it anyway (since deleting the user's code is extremely
						// undesirable).
						
						boolean inPluginsFolder = pluginXml.startsWith(depDataCache.getData().getPluginsDirectory());
						debug("Plugin inPluginsFolder = "+inPluginsFolder);
						if (inPluginsFolder) {
							//One of the above checks would suffice, but better be safe than sorry!
							debug("Should delete this plugin: "+pluginXml);
							File pluginXmlFile = new File(pluginXml);
							File pluginDir = pluginXmlFile.getParentFile();
							try {
								FileUtils.deleteDirectory(pluginDir);
								debug("Deleted");
							} catch (IOException e) {
								GrailsCoreActivator.log(e);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Execute a "grails upgrade" command to upgrade a grails project to the given grails install.
	 * @throws CoreException 
	 */
	public static void upgradeProject(IProject project, IGrailsInstall install) throws CoreException {
		debug("upgrade "+project+" ...");
		setGrailsInstall(project, install);
		ensureNaturesAndBuilders(project);  // This is needed to avoid upgrade from crashing in the 'old style'
		   // executor when imported project is missing Java nature.
		CoreException error = null;
		try {
			ILaunchResult result = GrailsCommandFactory.upgrade(project).synchExec();
			debug(""+result);
			debug("upgrade "+project+" DONE");
		} catch (CoreException e) {
			debug("upgrade "+project+" FAILED");
			error = e;
		}
		//Even if above command exec had some error, we can try to proceed...
		try {
			eclipsifyProject(install, install.isDefault() && GrailsInstallManager.inheritsDefaultInstall(project), project);
		} catch (CoreException e) {
			if (error==null) {
				error = e;
			}
		}
		if (error!=null) {
			throw error;
		}
	}

	public static void ensureNaturesAndBuilders(IProject project) throws CoreException {
		IProjectDescription desc = project.getDescription();
		addNaturesAndBuilders(desc);
		project.setDescription(desc, new NullProgressMonitor());
	}

	/**
	 * Refreshes the dependencies of a given project and all projects that depend on it. 
	 * Note: this is not truely transitive, it only looks one level deep in the dependencies, 
	 * assuming that transitive dependencies are already added as dependencies to a project.
	 */
	public static void transitiveRefreshDependencies(GrailsProject gp, boolean showOutput) throws CoreException {
		if (!gp.isPlugin()) { 
			//Shortcut: the transitive bit only matters for plugin projects.
			refreshDependencies(gp.getJavaProject(), showOutput);
		} else {
			transitiveRefreshDependencies(gp, showOutput, new HashSet<GrailsProject>());
		}
	}
 
	/**
	 * Helper method to perform transitive refreshing. An 'already' refreshed Set is passed around and used
	 * to avoid refreshing the same project multiple times.
	 */
	private static void transitiveRefreshDependencies(GrailsProject gp, boolean showOutput, HashSet<GrailsProject> alreadyRefreshed) throws CoreException {
		if (!alreadyRefreshed.contains(gp)) {
			refreshDependencies(gp, showOutput);
			alreadyRefreshed.add(gp);
			Set<GrailsProject> needsRefreshing = gp.getProjectsDependingOn();
			for (GrailsProject dependor : needsRefreshing) {
				transitiveRefreshDependencies(dependor, showOutput, alreadyRefreshed);
			}
		}
	}

	private static void refreshDependencies(GrailsProject gp, boolean showOutput) throws CoreException {
		refreshDependencies(gp.getJavaProject(), showOutput);
	}

}
