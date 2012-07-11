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
package org.grails.ide.eclipse.runonserver;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.util.ModuleFile;
import org.eclipse.wst.server.core.util.ModuleFolder;
import org.eclipse.wst.server.core.util.ProjectModule;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.GrailsBuildSettingsHelper;
import org.springsource.ide.eclipse.commons.core.ZipFileUtil;


/**
 * @author Kris De Volder
 * @author Andrew Eisenberg
 * @author Andy Clement
 * @author Christian Dupuis
 * @since 2.5.1
 */
public class GrailsAppModuleDelegate extends ProjectModule implements IWebModule {
	
	/**
	 * The target directory used by Grails itself to build compile for a war build.
	 */
	private static final String WAR_OUTPUT_FOLDER = "web-app/WEB-INF/classes";

	/**
	 * If this flag is turned on, we will try to incrementally replace stuff from the workspace into
	 * the deployed app. 
	 * <p>
	 * If it is turned of, every redeploy will require a full rebuild of the war file and we also
	 * run 'grails clean' before building the war.
	 * <p>
	 * The flag turned of is more robust / reliable and uses only exactly what Grails would 
	 * create in the war file when invoked from the commandline. However, it builds war files
	 * much more frequently, which can be annoying to users.
	 */
	private boolean incremental = true; //Can be set via preferences page (setting this to false, 
	 // provides a workaround for users experiencing problems for (yet to discover) similar to
	 // STS-1518, STS-1539, STS-1913, ... 

	public interface IResourceMatcher {
		boolean isMatch(IModuleResource child);
	}

	private static final boolean DEBUG = (""+Platform.getLocation()).contains("kdvolder"); //false;
	private void debug(String string) {
		if (DEBUG)
			System.out.println("GrailsAppModuleDelegate: " + this.getProject().getName()+": " + string);
	}

	/**
	 * Reference to the warFile if it has been created. (So we only create it once unless something has changed).
	 */
	private File cachedWarFile = null;

	/**
	 * Where we keep an "exploded" copy of the war file.
	 */
	private File explodedWarFile = null;
	
	
	/**
	 * Flag is used to avoid repeated war build failures.
	 */
	boolean warBuildFailed = false;
	
	/**
	 * Cached copy of the "deployed" IModuleResource references.
	 */
	private IModuleResource[] cachedMembers = null;

	public GrailsAppModuleDelegate(IModule module) {
		super(module.getProject());
		incremental = RunOnServerProperties.getIncremental(getProject());
	}

	@Override
	public boolean isSingleRootStructure() {
		return false;
	}
	
	@Override
	public IModuleResource[] members() throws CoreException {
		debug("members() called");
		cacheMembers();
		if (cachedMembers==null) {
			return new IModuleResource[0];
		} else {
			return cachedMembers;
		}
	}
	
	/**
	 * This method gets called whenever any resources in the project get changed. The root of the provided delta
	 * is the project associated with this GrailsApp.
	 */
	public void projectChanged(IResourceDelta delta) {
		try {
			if (!cacheAlreadyClear()) {
				IResourceDeltaVisitor handler = new GrailsAppModuleDeltaVisitor();
				delta.accept(handler);
			}
		} catch (CoreException e) {
			GrailsCoreActivator.log("Problem processing changes to project", e);
		}
	}
	
	/** Resource changes in paths starting with these Strings will be ignored */
	private String[] getIgnoredPaths() {
		String javaOutputFolder = null; 
		try {
			IPath outPath = JavaCore.create(getProject()).getOutputLocation();
			javaOutputFolder = outPath.removeFirstSegments(1).toString();
		} catch (JavaModelException e) {
			GrailsCoreActivator.log(e);
		}
		List<String> ignore = new ArrayList<String>(5);
		ignore.add("target");
		ignore.add("test");
		ignore.add(WAR_OUTPUT_FOLDER);
		if (javaOutputFolder!=null) {
			ignore.add(javaOutputFolder);
		}
		return ignore.toArray(new String[ignore.size()]);
 	}
		
	/** Resource changes in paths starting with these Strings will be treated as "selectively reloadable" 
	 * (but only if INCREMENTAL is turned on */
	private String[] getReloadablePaths() {
		return new String[] {
				"src",
				"grails-app/domain",
				"grails-app/controllers",
				"grails-app/views"
		};
	};

	private static final String TIME_STAMP_FILE_NAME = "__STS_TIME_STAMP__";
	
	private class GrailsAppModuleDeltaVisitor implements IResourceDeltaVisitor {
		
		boolean abort = false;
		
		//Compute ignored paths only once per visitor
		private String[] ignoredPaths = getIgnoredPaths();

		//Compute reloadable paths only once per visitor and only if we need it
		private String[] reloadablePaths = incremental ? getReloadablePaths() : null;
		
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (abort) { 
				debug("Delta: "+delta.getResource()+ " ABORTED");
				return false; //Stop visitor NOW!
			}
			if (ignoreChangesIn(delta.getResource())) {
				debug("Delta: "+delta.getResource()+ " => IGNORE");
				return false;
			} else if (isReloadable(delta.getKind(), delta.getResource())) {
				debug("Delta: "+delta.getResource()+ " => clear only the module tree cache");
				clearMembersCache();
				return false;
			} else if (isRebuildWar(delta.getResource())) {
				debug("Delta: "+delta.getResource()+ " => clear all caches");
				clearCaches();
				abort = true;
				return false;
			} else {
				debug("Delta: "+delta.getResource()+ " => look deeper");
				return true; // Look deeper in the tree.
			}
		}

		/**
		 * If this returns true, it means any changes to this resource can be handled by
		 * selective reloading, so it doesn't require a rebuild of the war file.
		 * @param deltaKind 
		 */
		private boolean isReloadable(int deltaKind, IResource resource) {
			if (!incremental) return false;
			// Ignore any changes below "target"
			for (String prefix : reloadablePaths) {
				if (new Path(prefix).isPrefixOf(resource.getProjectRelativePath())) {
					//In present implementation, we can't properly deal with added or removed
					// resources, so only changed resource are handled incrementally.
					// See: https://issuetracker.springsource.com/browse/STS-1339
					return resource.getType()==IResource.FILE && deltaKind==IResourceDelta.CHANGED;
				}
			}
			return false;
		}

		/**
		 * We'll prune deltas below if this returns true.
		 */
		private boolean ignoreChangesIn(IResource resource) {
			// Ignore any changes below "target"
			for (String ignore : ignoredPaths) {
				if (new Path(ignore).isPrefixOf(resource.getProjectRelativePath())) {
					return true;
				}
			}
			return false;
		}
		
		private boolean isRebuildWar(IResource resource) {
			//For now, if any file changes percolate through to this test (so they are not accounted for in any other way) then we
			// rebuild the .war file.
			return resource.getType() == IResource.FILE;
		}

	}

	/////////////////////////////////////////////////////////////////////////////////////
	// Helper code

	private void cacheMembers() throws CoreException {
		if ((cachedMembers==null || cachedWarFile==null)) {
			
			debug("Recomputing cachedMembers");
			cacheWarFile(); //Ensure the exploded war file is there.
			
			if (explodedWarFile==null || ! explodedWarFile.exists() ) {
				debug("For some reason there is no exploded war file... (war command failed?)");
				return;
			}
			
			cachedMembers = getDirectoryResources(Path.EMPTY, explodedWarFile);

			if (incremental) {
				IJavaProject javaProject = JavaCore.create(getProject());
				IPath outputLocation = javaProject.getOutputLocation();
				IFolder outputFolder = ResourcesPlugin.getWorkspace().getRoot().getFolder(outputLocation);
				IFolder viewsFolder = getProject().getFolder(new Path("grails-app/views"));

				// Replace the stuff from the grails war with whatever stuff we have in our output folder...
				//  However, any files that exist in the .war but not our folder will not be touched.

				replace(cachedMembers, "WEB-INF/classes", outputFolder);
				replace(cachedMembers, "WEB-INF/grails-app/views", viewsFolder);

				removePrecompiledGSPs();
				// TODO: KDV: (deploy) write some code to produce a report of what we are still borrowing from the grails war file.
				//    If we can make the report empty then the war file is obsolete.
				//			if (DEBUG) {
				//				debug(">>> entries in WEB-INF/classes taken from the grails war file");
				//				reportWarEntries(findFolder(cachedMembers, new Path("WEB-INF/classes")));
				//				debug("<<< entries in WEB-INF/classes taken from the grails war file");
				//			}
			}
		}
	}

	/**
	 * Removes stuff relating to precompiled .gsp files from the deployed resources. 
	 * This should have the effect of making grails fallback do dynamic gsp processing.
	 */
	private void removePrecompiledGSPs() {
		// Remove: the gsp folder that contains the "views.properties" file listing precompiled gsps
		cachedMembers = remove(cachedMembers, "WEB-INF/classes/gsp");
		
		// Remove: precompiled gsp classes and data
		ModuleFolder classes = findFolder(cachedMembers, new Path("WEB-INF/classes"));
		removeFiles(classes, new IResourceMatcher() {
			public boolean isMatch(IModuleResource resource) {
				String name = resource.getName();
				return name.startsWith("gsp_") || name.startsWith("___LineNumberPlaceholder");
			}
		});
	}

	private void removeFiles(ModuleFolder folder, IResourceMatcher remove) {
		IModuleResource[] members = folder.members();
		if (members!=null && members.length>0) {
			List<IModuleResource> keepMembers = new ArrayList<IModuleResource>(members.length);
			for (IModuleResource child : members) {
				if (!remove.isMatch(child)) {
					keepMembers.add(child);
				}
				if (child instanceof ModuleFolder) {
					removeFiles((ModuleFolder) child, remove);
				}
			}
			if (keepMembers.size()!=members.length) {
				folder.setMembers(keepMembers.toArray(new IModuleResource[keepMembers.size()]));
			}
		}
	}

	private boolean inCflow(String string) {
		StringWriter trace = new StringWriter();
		new Exception().printStackTrace(new PrintWriter(trace));
		return trace.toString().contains(string);
	}

	private void reportWarEntries(IModuleResource root) {
		if (root instanceof ModuleFile) {
			ModuleFile file = (ModuleFile)root;
			File jFile = (File) file.getAdapter(File.class);
			if (jFile!=null) {
				debug(file.getModuleRelativePath()+"/"+file.getName());
			}
		} else if (root instanceof ModuleFolder) {
			ModuleFolder folder = (ModuleFolder)root;
			IContainer container = (IContainer) folder.getAdapter(IContainer.class);
			if (container==null) {
				debug(folder.getModuleRelativePath()+"/"+folder.getName()+"/");
			}
			for (IModuleResource m : folder.members()) {
				reportWarEntries(m);
			}
		} else {
			debug("Unknown type: "+root);
		}
	}

	/**
	 * Similar to the method provided by {@link ProjectModule} but accepts an java.io.File instead on a IContainer.
	 * <p>
	 * Create IModuleResource objects for anything inside of a given directory.
	 */
	private IModuleResource[] getDirectoryResources(IPath pathToHere, File dir) {
		Assert.isLegal(dir.isDirectory());
		File[] files = dir.listFiles();
		IModuleResource[] resources = new IModuleResource[files.length];
		for (int i = 0; i < resources.length; i++) {
			File file = files[i];
			if (file.isDirectory()) {
				ModuleFolder folder = new ModuleFolder(null, file.getName(), pathToHere);
				folder.setMembers(getDirectoryResources(pathToHere.append(file.getName()), file));
				resources[i] = folder;
			} else {
				//Ordinary file (not dir)
				resources[i] = new ModuleFile(file, file.getName(), pathToHere);
			}
			//debug("warElement: "+resources[i]);
		}
		return resources;
	}

	private void replace(IModuleResource[] resources, String pathStr, IFolder outputFolder) throws CoreException {
		IPath path = new Path(pathStr);
		ModuleFolder folder = findFolder(resources, path);
		folder.setMembers(merge(folder.members(), getModuleResources(path, outputFolder)));
	}

	private IModuleResource[] merge(IModuleResource[] ls, IModuleResource[] rs) {
		for (IModuleResource r : rs) {
			ls = merge(ls, r);
		}
		return ls;
	}

	private IModuleResource[] merge(IModuleResource[] ls, IModuleResource r) {
		int i = findIndex(ls, r.getName());
		if (i < ls.length) {
			//There's an existing left element to replace or insert into
			IModuleResource l = ls[i];
			ls[i] = r;
			if (l instanceof ModuleFolder && r instanceof ModuleFolder) {
				//debug("Merging: "+l+" & "+r);
				// Merge the two if both are folders (otherwise replace)
				ModuleFolder l_folder = (ModuleFolder) l;
				ModuleFolder r_folder = (ModuleFolder) r;
				r_folder.setMembers(merge(l_folder.members(), r_folder.members()));
			} else {
				//debug("Replacing: "+l+" by "+r);
			}
			ls[i] = r;
		} else {
			//Add new left entry
			ls = RunOnServerPlugin.copyOf(ls, ls.length+1);
			//debug("Adding: "+r);
			ls[ls.length-1] = r;
		}
		return ls;
	}

	private ModuleFolder findFolder(IModuleResource[] resources, IPath path) {
		int i = findIndex(resources, path);
		Assert.isTrue(i < resources.length, "Couldn't find "+path);
		if (path.segmentCount()==1) 
			return (ModuleFolder) resources[i];
		else {
			Assert.isTrue(resources[i] instanceof IModuleFolder);
			return findFolder(((IModuleFolder)resources[i]).members(), path.removeFirstSegments(1));
		}
	}

	/**
	 * Returns true if our caches are completely clear, in that case there will be
	 * no need to do any change listening (since clearing the cache again will not
	 * have any effect anyway).
	 */
	private boolean cacheAlreadyClear() {
		//Note: only checking war cache is enough, since when clearing that cache is
		// we always also clear the members cache. 
		return cachedWarFile==null || !cachedWarFile.exists();
	}

	
	/**
	 * Build the war file, unless it was already build (and not invalidated since then).
	 */
	private void cacheWarFile() {
		if (!warBuildFailed && (cachedWarFile==null || !cachedWarFile.exists())) {
			//If we are only called to process a resource delta and the explodedWar folder exist, we can possibly 
			//get away with using a stale copy of the exploded war (most changes will still be detected because of the stuff we
			//substitute into the war from the workspace).
			//Caveat: it is possible some changes won't be detected and won't be published to the server. We have to work at
			// minimising these cases.
			try {
//				if (DEBUG) {
//					new Exception().printStackTrace(System.out);
//				}
				if (!exists(explodedWarFile) || !inCflow("Server$ResourceChangeJob.run")) {
					IProject project = getProject();
					debug("Rebuilding warFile for "+project);
//					ISchedulingRule rule = Job.getJobManager().currentRule();
//					debug("Current scheduling rule = "+rule);
//					if (rule!=null && !rule.contains(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule())) {
//						// This could be a problem.... race conditions likely if we do our thing while builds are possible!
//						GrailsCoreActivator.log(new Error("Possible race condition detected"));
//					}
					File warFile = getWarFile(project);
					if (!incremental) {
						if (isOverlappingOutputFolders()) {
							//We can skip this if we have properly setup project with our own private output folder
							GrailsCommand cleanCommand = GrailsCommandFactory.clean(getProject());
							cleanCommand.synchExec();
						}
					}
					GrailsCommand warCommand = GrailsCommandFactory.war(getProject(), getEnv(), warFile);
					warCommand.synchExec();
					Assert.isTrue(warFile.exists());

					explodedWarFile = getExplodedWarFile(getProject());
					if (explodedWarFile.exists()) {
						try {
							FileUtils.deleteDirectory(explodedWarFile);
						} catch (IOException e) {
							// Log and try to proceed anyway
							GrailsCoreActivator.log(e); 
						}
					}
					try {
						ZipFileUtil.unzip(warFile.toURI().toURL(), explodedWarFile, new NullProgressMonitor());
					} catch (Exception e) {
						throw new CoreException(new Status(IStatus.ERROR, RunOnServerPlugin.PLUGIN_ID, "Could not unpack the war file: "+warFile));
					}
					cachedWarFile = warFile;
					debug("DONE Rebuilding warFile for "+project);
				}
			} catch (Throwable e) {
				//Catch and log any problems, if they propagate to WTP they can get lost without a trace.
				GrailsCoreActivator.log("A problem occurred building the war file for "+getProject(), e);
			}
		}
	}
	
	/**
	 * @return true is Greclipse output folder and the grails war output folder are being shared.
	 * (This is really not desirable, but we have to deal with it).
	 */
	private boolean isOverlappingOutputFolders() {
		IJavaProject javaProject = JavaCore.create(getProject());
		try {
			IPath javaOutputLocation = javaProject.getOutputLocation().removeFirstSegments(1); //Drop project name
			IPath warOutputLocation = new Path(WAR_OUTPUT_FOLDER);
			return javaOutputLocation.equals(warOutputLocation);
		} catch (JavaModelException e) {
			GrailsCoreActivator.log(e);
			return true; //Assume the worst and proceed.
		}
	}

	private String getEnv() {
		IProject project = getProject();
		return RunOnServerProperties.getEnv(project);
	}

	private boolean exists(File file) {
		return file!=null && file.exists();
	}

	/**
	 * This method determines the location of (where to create) the .war file.
	 * <p>
	 * Note: public for testing purposes only.
	 */
	public static File getWarFile(IProject project) {
		IPath stage = getStagingArea();
		IPath explodedLocation = stage.append(project.getName()+".war");
		return explodedLocation.toFile();
	}

	/**
	 * This method determines the location of (where to create) the explodedWarFile
	 */
	private File getExplodedWarFile(IProject project) {
		IPath stagingArea = getStagingArea();
		IPath explodedLocation = stagingArea.append(project.getName()+"/exploded");
		return explodedLocation.toFile();
	}

	private static IPath getStagingArea() {
		return RunOnServerPlugin.getDefault().getStagingArea();
	}

	public void clearCaches() {
		debug("Clearing caches: members + warFile");
		touchTimeStamp();
//		cachedMembers = null; Note: not actually cleared, it is "implied" to be invalid because cachedWarFile == null
		cachedWarFile = null;
		warBuildFailed = false;
	}
	
	private void touchTimeStamp() {
		File file = getTimeStampFile();
		if (file!=null) {
			try {
				FileUtils.touch(file);
			} catch (IOException e) {
				GrailsCoreActivator.log("Problems touching time stamp file in Grails RunOnServer", e);
			}
		}
	}

	/**
	 * The time stamp file is a dummy file that we change whenever the war file cache is cleared.
	 * This will ensure that even when we don't rebuild the war when we really should (see cacheWarFile method)
	 * we still have at least one changed file that makes it into the result returned by 'members'.
	 * <p>
	 * We need to do this because otherwise if WTP doesn't see any changes, it won't ask again
	 * for the module members when time comes to actually deploy and build the war file.
	 * <p>
	 * Note: the time stamp file isn't deployed to the server because when the stamp is there,
	 * a war should be built before the next deploy and the stamp will be erased in the process.
	 */
	private File getTimeStampFile() {
		File file = explodedWarFile;
		//It's ok not to have a time stamp if the explodedWar doesn't exist, since this
		// fact alone will ensure that the war will be built.
		if (file!=null) {
			file = new File(file, TIME_STAMP_FILE_NAME);
		}
		return file;
	}

	public void clearMembersCache() {
		debug("Clearing caches: members");
		cachedMembers = null;
	}
	
	/**
	 * Given an array of "to be published" resources. Add a given IResource to that array at a specified location within
	 * the "publish tree".
	 * 
	 * @throws CoreException 
	 */
	private IModuleResource[] add(IModuleResource[] resources, IResource rsrcToAdd, String path) throws CoreException {
		return add(resources, rsrcToAdd, new Path(path), Path.EMPTY);
	}
	
	private IModuleResource[] add(IModuleResource[] resources, IResource rsrcToAdd, IPath path, IPath pathToHere) throws CoreException {
		int i = findIndex(resources, path);
		if (i < resources.length) {
			//Found what we were searching for
			Assert.isTrue(path.segmentCount()>1, "Adding something that is already there isn't supported/allowed");
			addToChildren((ModuleFolder)resources[i], rsrcToAdd, path.removeFirstSegments(1), pathToHere.append(path.segment(0)));
		} else {
			//Didn't find it so we must create something here.
			Assert.isTrue(path.segmentCount()==1, "Auto creationg of intervening folders is not supported (yet)");
			resources = RunOnServerPlugin.copyOf(resources, resources.length+1);
			if (rsrcToAdd instanceof IContainer) {
				ModuleFolder addIt = new ModuleFolder((IContainer) rsrcToAdd, path.segment(0), pathToHere);
				addIt.setMembers(getModuleResources(pathToHere.append(path), (IContainer) rsrcToAdd));
				resources[resources.length-1] = addIt;
			} else {
				ModuleFile addIt = new ModuleFile((IFile)rsrcToAdd, path.segment(0), pathToHere);
				resources[resources.length-1] = addIt;
			}
		}
		return resources;
	}

	/**
	 * Add a given resource to the children of a moduleFolder, at a given relative path location in the module tree.
	 * @param path			Path starting from the given moduleFolder (does not include the name of the moduleFolder itself)
	 * @param pathToHere	Path leading upto this location, including the name of the moduleFolder itself.
	 * @throws CoreException 
	 */
	private void addToChildren(ModuleFolder moduleFolder, IResource rsrcToAdd, IPath path, IPath pathToHere) throws CoreException {
		IModuleResource[] children = moduleFolder.members();
		children = add(children, rsrcToAdd, path, pathToHere);
		moduleFolder.setMembers(children);
	}

	/**
	 * Given an array of "to be published" resources, recursively search for and remove a given resource 
	 * (as indicated by a path String). 
	 * <p>
	 * If the resource this path String leads to is a folder than the folder and everything inside of it are removed.
	 * <p>
	 * If the path doesn't lead to a resource, then nothing is removed.
	 * 
	 * @return May return either a copy of the array or the same array modified by a side effect. (Depending on whether
	 *  the length of the array had to change (lenght may not need to change if resource was removed from a child
	 *  of one of the elements.
	 */
	private IModuleResource[] remove(IModuleResource[] resources, String pathToRemove) {
		return remove(resources, new Path(pathToRemove));
	}

	private IModuleResource[] remove(IModuleResource[] resources, IPath path) {
		int i = findIndex(resources, path);
		if (i < resources.length) {
			//Found what we were searching for
			if (path.segmentCount()==1) {
				//This is the one to delete!
				IModuleResource[] result = new IModuleResource[resources.length-1];
				System.arraycopy(resources, 0, result, 0, i); // Copy upto i into new array.
				System.arraycopy(resources, i+1, result, i, result.length-i);
				return result;
			} else {
				// Must delete something deeper down
				removeFromChildren((ModuleFolder)resources[i], path.removeFirstSegments(1));
				return resources;
			}
		} else {
			// We couldn't find the path so don't delete anything
			return resources;
		}
	}

	private int findIndex(IModuleResource[] resources, IPath path) {
		String search = path.segment(0);
		return findIndex(resources, search);
	}

	private int findIndex(IModuleResource[] resources, String search) {
		int i = 0;
		while (i < resources.length && !resources[i].getName().equals(search)) {
			i++;
		}
		return i;
	}

	/**
	 * Remove a resource from the children of this folder
	 * @param folder
	 * @param pathToRemove Path to target resource, not including the name of the folder.
	 */
	private void removeFromChildren(ModuleFolder folder, IPath pathToRemove) {
		IModuleResource[] children = folder.members();
		children = remove(children, pathToRemove);
		folder.setMembers(children);
	}

	///////////////////////////////////////////////////////////////////////////////////
	/// IWebModule implementation

	public IContainer[] getResourceFolders() {
		return new IContainer[0];
	}

	public IContainer[] getJavaOutputFolders() {
		return new IContainer[0];
	}

	public boolean isBinary() {
		return false;
	}

	public String getContextRoot() {
		Properties props = GrailsBuildSettingsHelper.getApplicationProperties(getProject());
		if (props!=null) {
			String root = props.getProperty("app.context");
			if (root!=null) {
				return root;
			}
		}
		return getName();
	}

	public String getContextRoot(IModule earModule) {
		return getContextRoot();
	}

	public IModule[] getModules() {
		return new IModule[0];
	}

	public String getURI(IModule module) {
		return null;
	}

	/**
	 * Called when the 'env' property for the project associated with this delegate changes.
	 */
	public void envChanged(String oldEnv, String newEnv) {
		clearCaches(); // Must force a 'hard' clear, because otherwise the env change won't be noticed by WTP.
	}

	/**
	 * Called when the 'incremental' property for the project associated with this delegate changes.
	 */
	public void incrementalChanged(boolean old, boolean isIncremental) {
		this.incremental = isIncremental;
		clearCaches();
	}

}
