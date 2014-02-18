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
package org.grails.ide.eclipse.test.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.classpath.SourceFolderJob;
import org.grails.ide.eclipse.core.internal.model.DefaultGrailsInstall;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.model.GrailsInstallManager;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.longrunning.LongRunningProcessGrailsExecutor;
import org.grails.ide.eclipse.ui.internal.importfixes.GrailsProjectVersionFixer;
import org.osgi.service.prefs.BackingStoreException;
import org.springsource.ide.eclipse.commons.core.ZipFileUtil;
import org.springsource.ide.eclipse.commons.frameworks.test.util.ACondition;
import org.springsource.ide.eclipse.commons.tests.util.DownloadManager;
import org.springsource.ide.eclipse.commons.tests.util.DownloadManager.DownloadRequestor;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;


/**
 * @author Kris De Volder
 */
public class GrailsTest extends TestCase {
	
	public abstract class TearDownAction {
		public abstract void doit() throws CoreException;
	}

	public List<TearDownAction> tearDownActions = new ArrayList<TearDownAction>();

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		GrailsProjectVersionFixer.testMode();
	}
	
	public static void clearGrailsState() {
		File home = GrailsCoreActivator.getDefault().getUserHome();
		//deleteDir(new File(home, ".ivy2"));
		deleteDir(new File(home, ".grails"));
		//Long running Grails processes may hold on to state as well if they are still running.
		// Plus.... they will break if the files in .grails are all deleted. So kill em all :-)
		LongRunningProcessGrailsExecutor.shutDownIfNeeded();
	}

	private static void deleteDir(File dir) {
		try {
			FileUtils.deleteDirectory(dir);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		for (TearDownAction td : tearDownActions) {
			td.doit();
		}
	}
	
	protected void assertPluginSourceFolder(IProject proj, String plugin,
			String... pathElements) throws JavaModelException {
				IFolder folder = sourceFolderFor(proj, plugin, pathElements);
				assertClassPathEntry(IClasspathEntry.CPE_SOURCE, folder.getFullPath().toString(), getRawClassPath(proj));
				assertTrue("Source folder not found: "+folder, folder.exists());
	}

	protected IClasspathEntry[] getRawClassPath(IProject proj) throws JavaModelException {
		return JavaCore.create(proj).getRawClasspath();
	}

	protected void assertAbsentPluginSourceFolder(IProject proj, String plugin, String... pathElements) throws JavaModelException {
		IFolder folder = sourceFolderFor(proj, plugin, pathElements);
		assertFalse("Source folder exists but shouldn't: "+folder, folder.exists());
		assertAbsentClassPathEntry(IClasspathEntry.CPE_SOURCE, folder.getFullPath().toString(), getRawClassPath(proj));
	}

	protected IFolder sourceFolderFor(IProject proj, String plugin, String... pathElements) {
		IFolder folder = proj.getFolder(SourceFolderJob.PLUGINS_FOLDER_LINK);
		folder = folder.getFolder(plugin);
		for (String e : pathElements) {
			folder = folder.getFolder(e);
		}
		return folder;
	}

	/**
	 * Verify that an expected classPath entry with given kind and path exists in the classpath.
	 */
	protected void assertClassPathEntry(int kind, String path, IClasspathEntry[] classPath) {
		StringBuffer found = new StringBuffer();
		for (IClasspathEntry entry : classPath) {
			found.append(kindString(entry.getEntryKind())+": "+entry.getPath()+"\n");
			if (entry.getEntryKind()==kind) {
				if (entry.getEntryKind()==IClasspathEntry.CPE_CONTAINER) {
					if  (entry.getPath().equals(Path.EMPTY.append(path))) {
						return; //OK
					}
				} else if (entry.getEntryKind()==IClasspathEntry.CPE_LIBRARY) {
					// Library paths will vary a lot in location and version numbers, so use an 'approximate' 
					// check to see if it contains some key string.
					if  (entry.getPath().toString().contains(path)) {
						return; //OK
					}
				} else {
					if  (entry.getPath().toString().endsWith(path)) {
						return; //OK
					}
				}
			}
		}
		fail("No classpath entry found for: "+kindString(kind)+": "+path+"\n" +
				"found entries are: \n"+found.toString());
	}

	protected void assertClassPathEntry(int kind, Pattern path, IClasspathEntry[] classPath) {
		StringBuffer found = new StringBuffer();
		for (IClasspathEntry entry : classPath) {
			found.append(kindString(entry.getEntryKind())+": "+entry.getPath()+"\n");
			if (entry.getEntryKind()==kind) {
				Matcher matcher = path.matcher(entry.getPath().toString());
				if (matcher.find()) {
					return; //OK
				}
			}
		}
		fail("No classpath entry found for: "+kindString(kind)+": "+path+"\n" +
				"found entries are: \n"+found.toString());
	}
	
	/**
	 * Verify that an expected classPath entry with given kind and path exists in the classpath.
	 */
	protected void assertAbsentClassPathEntry(int kind, String path,
			IClasspathEntry[] classPath) {
		for (IClasspathEntry entry : classPath) {
			if (entry.getEntryKind()==kind) {
				if (entry.getEntryKind()==IClasspathEntry.CPE_CONTAINER) {
					if  (entry.getPath().equals(Path.EMPTY.append(path))) {
						fail("Class path entry should not be there: "+entry);
					}
				} else if (entry.getEntryKind()==IClasspathEntry.CPE_LIBRARY) {
					// Library paths will vary a lot in location and version numbers, so use an 'approximate' 
					// check to see if it contains some key string.
					if  (entry.getPath().toString().contains(path)) {
						fail("Class path entry should not be there: "+entry);
					}
				} else {
					if  (entry.getPath().toString().endsWith(path)) {
						fail("Class path entry should not be there: "+entry);
					}
				}
			}
		}
		// OK
	}

	/**
	 * @param kind
	 * @return
	 */
	protected String kindString(int kind) {
		switch (kind) {
		case IClasspathEntry.CPE_SOURCE:
			return "src";
		case IClasspathEntry.CPE_CONTAINER:
			return "con";
		case IClasspathEntry.CPE_LIBRARY:
			return "lib";
		default:
			return ""+kind;
		}
	}

	/**
	 * Ensure a project with the given name exists, so we can use it as a test Fixture.
	 */
	public static IProject ensureProject(String name) throws Exception {
	    return ensureProject(name, false);
	}

	/**
	 * Ensure a project with the given name exists, so we can use it as a test Fixture.
	 * Note that when creating a plugin project, this method expects a camel case name.
	 * @param name name of the project
	 * @param isPluginProject whether this project should be a plugin project
	 * @return the created Grails projet
	 * @throws Throwable 
	 * @throws CoreException
	 */
	protected static IProject ensureProject(final String name, final boolean isPluginProject)
			throws Exception {
		try {
			final IProject project = StsTestUtil.getProject(name);
			if (project.exists()) {
				System.out.println("Reusing test project '"+name+"'");
				return project;
			}
	
			final Job job = new Job("EnsureProject "+name) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						System.out.println("Creating test project '"+name+"' with Grails " + GrailsVersion.getDefault());
						GrailsCommand cmd;
						if (isPluginProject) {
							cmd = GrailsCommandFactory.createPlugin(name);
						} else {
							cmd = GrailsCommandFactory.createApp(name);
						}
						ILaunchResult result = cmd.synchExec();
						IGrailsInstall install = cmd.getGrailsInstall();
						try {
			                GrailsCommandUtils.eclipsifyProject(install, project);
			            } catch (Exception e) {
			                System.err.println("Ugh...tried to Eclipsify project, but failed.  Maybe a network error.  Retrying");
			                System.err.println("Project is: " + project.getName());
			                // try again maybe something from the server
			                GrailsCommandUtils.eclipsifyProject(install, project);
			            }
						if (!isPluginProject) {
							assertContains("Created Grails Application at "+workspacePath()+"/"+name, result.getOutput());
						} else {
							String pluginName = GrailsNature.createPluginName(name);
							String projectName = pluginName.substring(0, pluginName.indexOf("GrailsPlugin.groovy"));
							assertContains("Created plugin "+projectName, result.getOutput());
							assertTrue("Plugin file " + pluginName + " does not exist", GrailsNature.isGrailsPluginProject(project));
						}
						return Status.OK_STATUS;
					} catch (Throwable e) {
						return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "problem creating test project", e);
					}
				}
			};
			waitForWorkspaceJob(job);
			assertTrue(project.exists());
			assertNoErrors(project);
			assertTrue(project.hasNature(GrailsNature.NATURE_ID));

			System.out.println("Created project '"+project.getName()+" uses Grails "+GrailsVersion.getEclipseGrailsVersion(project));
			
			return project;
		} catch (Throwable e) {
			throw new Error(e);
		}
	}

	/**
	 * Helper method to Schedule a Job with a 'buildRule' and then wait for it to complete
	 * while keeping the UI thread alive.
	 */
	public static void waitForWorkspaceJob(final Job job) throws Exception {
		job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		job.schedule();
		new ACondition(job.getName()) {
			@Override
			public boolean test() throws Exception {
				return job.getResult()!=null;
			}
		}.waitFor(400000);
		assertStatusOK(job.getResult());
	}

	public static void assertNoErrors(IProject project) throws CoreException {
		try {
			StsTestUtil.assertNoErrors(project);
		} catch (Throwable e) {
			//This retry is compensating for http://jira.grails.org/browse/GRAILS-9263
			GrailsCommandUtils.refreshDependencies(JavaCore.create(project), false);
			StsTestUtil.assertNoErrors(project);
		}
	}

	public static void assertContains(String needle, String haystack) {
		if (!haystack.contains(needle)) {
			fail("Couldn't find expected substring: '"+needle+"'\n in '"+haystack+"'");
		}
	}

	public static String workspacePath() {
		return ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
	}

	/**
	 * Creates a 'temporary' resource for testing purpose. The resource is deleted during the 
	 * test tearDown.
	 */
	public void createTmpResource(IProject project, String path, String contents)
			throws CoreException {
		final IFile file = project.getFile(new Path(path));
		assertFalse("Can't create temporary resource because it already exists: "+file, file.exists());
		createResource(project, path, contents);
		tearDownActions.add(new TearDownAction() {
			@Override
			public void doit() throws CoreException {
				file.delete(true, new NullProgressMonitor());
			};
		});
	}

	/**
	 * Replace a file 'temporarily' with a different contents. The original contents of the file will
	 * be reinstated in the test teardown.
	 * @throws IOException 
	 * @throws CoreException 
	 */
	protected void tmpReplaceResource(final IProject project, final String path, String newContents) throws IOException, CoreException {
		final IFile file = project.getFile(new Path(path));
		File realFile = file.getLocation().toFile();
		final byte[] oldContents = file.exists() ? getBytesFromFile(realFile) : null;
		createResource(project, path, newContents);
		tearDownActions.add(new TearDownAction() {
			@Override
			public void doit() throws CoreException {
				if (oldContents!=null) {
					//File existed before:
					createResource(project, path, new String(oldContents));
				} else {
					//File didn't exist before:
					file.delete(true, new NullProgressMonitor());
				}
			}
		});
	}

	public static void assertElements(Set<Object> actualSet, Object... expecteds) {
		HashSet<Object> expectedSet = new HashSet<Object>(Arrays.asList(expecteds));
		StringBuilder msg = new StringBuilder();
		for (Object expected : expectedSet) {
			if (!actualSet.contains(expected)) {
				msg.append("Expected but not found: "+expected);
			}
		}
		for (Object actual : actualSet) {
			if (!expectedSet.contains(actual)) {
				msg.append("Found but not expected: "+actual);
			}
		}
		if (!"".equals(msg.toString())) {
			fail(msg.toString());
		}
	}

	public static String[] join(String[] base, String... extra) {
		String[] result = new String[base.length + extra.length];
		for (int i = 0; i < base.length; i++) {
			result[i] = base[i];
		}
		for (int i = 0; i < extra.length; i++) {
			result[base.length+i] = extra[i];
		}
		return result;
	}

	public static String[] defaultPlugins() {
		return defaultPlugins(false);
	}
	
	private static String[] defaultPlugins(boolean isPlugin) {
		if (GrailsVersion.MOST_RECENT.compareTo(GrailsVersion.V_2_0_0)<0) {
			//Grails 1.3
			return new String[] {
					"tomcat", "hibernate"
			};
		} else if (GrailsVersion.MOST_RECENT.compareTo(GrailsVersion.V_2_1_0)<0){
			//Grails 2.0
			if (isPlugin) {
				//Default plugins for plugin projects
				return new String[] {
						"tomcat", "release", "svn"
				};
			} else {
				//Default plugins for regular projects
				return new String[] {
						"tomcat", "hibernate", "jquery", "resources", "webxml" 
				};
			}
		} else if (GrailsVersion.MOST_RECENT.compareTo(GrailsVersion.V_2_3_)<0) {
			//Grails 2.1, 2.2
			if (isPlugin) {
				//Default plugins for plugin projects
				return new String[] {
						"tomcat", "release", "rest-client-builder"
				};
			} else {
				//Default plugins for regular projects
				return new String[] {
						"cache", "database-migration", "hibernate", "jquery", "resources", "tomcat", "webxml" 
				};
			}
		} else {
			//Grails 2.3
			if (isPlugin) {
				//Default plugins for plugin projects
				return new String[] {
						"tomcat", "release", "rest-client-builder"
				};
			} else {
				//Default plugins for regular projects
				return new String[] {
						"scaffolding", "cache", "database-migration", "hibernate", "jquery", "resources", "tomcat", "webxml" 
				};
			}
		}
	}
	
	public static String[] defaultPlugins(IProject project) {
		return defaultPlugins(GrailsNature.isGrailsPluginProject(project));
	}

	public static void assertStatusOK(IStatus status) {
	    if (!status.isOK()) {
	        fail("Expecting OK Status, but was:\n" + status);
	    }
	}

	/**
	 * If Java compiler compliance is set below 1.5 will get Groovy compilation errors 
	 * having to do with generics. Solution: call this method in the test setup.
	 */
/*	public static void setJava15Compliance() {
		@SuppressWarnings("rawtypes")
		Hashtable options = JavaCore.getDefaultOptions();
	    options.put(JavaCore.COMPILER_COMPLIANCE, "1.5");
	    options.put(JavaCore.COMPILER_SOURCE, "1.5");
	    options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.5");
	    JavaCore.setOptions(options);
	}
 */	
	public static void setJava16Compliance() {
		@SuppressWarnings("rawtypes")
		Hashtable options = JavaCore.getDefaultOptions();
	    options.put(JavaCore.COMPILER_COMPLIANCE, "1.6");
	    options.put(JavaCore.COMPILER_SOURCE, "1.6");
	    options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.6");
	    JavaCore.setOptions(options);
	}
	
	
//	/**
//	 * If Java compiler compliance is set below 1.5 will get Groovy compilation errors 
//	 * having to do with generics. Solution: call this method in the test setup.
//	 */
//	public static void setJava16Compliance() {
//		@SuppressWarnings("rawtypes")
//		Hashtable options = JavaCore.getDefaultOptions();
//	    options.put(JavaCore.COMPILER_COMPLIANCE, "1.5");
//	    options.put(JavaCore.COMPILER_SOURCE, "1.5");
//	    options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.5");
//	    JavaCore.setOptions(options);
//	}	

	public static URL getProjectZip(String name, GrailsVersion version) {
		final String location = "projects/"+name+"-"+version+".zip";
		URL result = GrailsTest.class.getClassLoader().getResource(location);
		assertNotNull("Couldn't find zipped test project: "+location, result);
		return result;
	}

	public static IResource createResource(IProject project, String pathStr, String contents) throws CoreException {
		Path path = new Path(pathStr);
		for (int i = 0; i < path.segmentCount() - 1; i++) {
			IFolder folder = project.getFolder(path.removeLastSegments(path
					.segmentCount() - i - 1));
			if (!folder.exists()) {
				folder.create(true, true, null);
			}
		}
		IFile file = project.getFile(path);
		if (!file.exists()) {
			file.create(new ByteArrayInputStream(contents.getBytes()), true, null);
		} else {
		    setContents(file, contents);
		}
		return file;
	}
	
	public static void setContents(IFile file, String newContents) throws CoreException {
	    file.setContents(new ByteArrayInputStream(newContents.getBytes()), true, false, null);
	}

	public static void assertRegexp(String regexpExpect, String actual) {
		Matcher matcher = Pattern.compile(regexpExpect).matcher(actual);
		if (!matcher.find()) {
			fail("Expected (regexp) '"+regexpExpect+":\n"+actual);
		}
	}

	/**
	 * When a test requires a specific version of Grails to be the default Grails install in the
	 * workspace, call this method to ensure the workspace is in fact setup with a matching
	 * install.
	 */
	public static void ensureDefaultGrailsVersion(final GrailsVersion version) throws Exception {
//		waitForGrailsIntall();
		if (version.equals(GrailsVersion.getDefault())) {
			return; // OK!
		}
		GrailsInstallManager manager = GrailsCoreActivator.getDefault().getInstallManager();
		Collection<IGrailsInstall> available = manager.getAllInstalls();

		boolean matchingInstallFound = false;
		final Set<IGrailsInstall> newInstalls = new LinkedHashSet<IGrailsInstall>();
		for (IGrailsInstall candidate : available) {
			if (candidate.getVersion().equals(version)) {
				matchingInstallFound = true;
				newInstalls.add(new DefaultGrailsInstall(candidate.getHome(), candidate.getName(), true));
			} else {
				newInstalls.add(new DefaultGrailsInstall(candidate.getHome(), candidate.getName(), false));
			}
		}
		if (!matchingInstallFound) {
			final String installName = "grails-"+version.getVersionString();
			//We'll try to install one from a downloaded zip.
			URI distro = getDistributionZipURI(version);
			DownloadManager.getDefault().doWithDownload(distro, new DownloadRequestor() {
				public void exec(File downloadedFile) throws Exception {
					File unzipDir = StsTestUtil.createTempDirectory(installName+"-", ".home");
					File grailsHome = new File(unzipDir, installName);
					ZipFileUtil.unzip(downloadedFile.toURI().toURL(), unzipDir, new NullProgressMonitor());
					DefaultGrailsInstall install = new DefaultGrailsInstall(grailsHome.getCanonicalPath(), installName, true);
					IStatus status = install.verify();
					if (!status.isOK()) {
						throw new CoreException(status);
					}
					assertEquals(version, install.getVersion());
					newInstalls.add(install);
				}
			});
			matchingInstallFound = true;
		}
		Assert.assertTrue("Couldn't find a Grails install of the required version "+version, matchingInstallFound);
		manager.setGrailsInstalls(newInstalls);
	}

	/**
	 * Computes a URI from where it should be possible to download a Grails distribution.
	 */
	public static URI getDistributionZipURI(GrailsVersion version) throws URISyntaxException {
		final String installName = "grails-"+version.getVersionString();
		
		if (version.getDownloadLocation()!=null) {
			return version.getDownloadLocation();
		}
		
		if (version.equals(GrailsVersion.BUILDSNAPHOT_2_0_2)) {
			//For build snapshot we assume that you built them locally, so they could be anywhere. Add locations to look for it
			// in the String array below.
			for (String buildDirStr : new String[] {"/home/kdvolder/commandline-dev/grails-core/build/distributions"}) {
				File buildDir = new File(buildDirStr); 
				if (buildDir.exists()) {
					File grailsZip = new File(buildDir, installName+".zip");
					if (grailsZip.exists()) {
						return grailsZip.toURI();
					}
				}
			}
		}
		
		String releaseType = version.isRelease() ? "release" : "milestone";
		String downloadUrl = "http://dist.springframework.org.s3.amazonaws.com/"+releaseType+"/GRAILS/grails-"+version+".zip";
		
		return new URI(downloadUrl);
	}


	/** 
	 * Flag to record whether the 'wait' has timed out before. If it has, don't try it again. I will just fail again anyway.
	 * This flag provides the flexibility for waitForGrailsInstall and ensureDefaultGrailsVersion to both call eachother,
	 * without getting stuck in an infinite loop.
	 */
	private static boolean waitForGrailsInstallHasFailed = false;
	
	/**
	 * When Eclipse starts, it sets up configuration stuff in a Job, we may start running the tests
	 * before this Job is finished. So wait until we know that Grails was configured properly.
	 */
	public static void waitForGrailsIntall() throws Exception {
		if (!waitForGrailsInstallHasFailed) {
			long time = System.currentTimeMillis();
			long endTime = time+30000;
			while (System.currentTimeMillis()<=endTime) {
				GrailsInstallManager installMan = GrailsCoreActivator.getDefault().getInstallManager();
				IGrailsInstall install = installMan.getDefaultGrailsInstall();
				if (install!=null) {
					System.out.println("Found grails install: "+install.getHome());
					return;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				System.out.println("Waiting for Grails to be configured...");
			}
			waitForGrailsInstallHasFailed = true;
			ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		}
		//throw new Error("No Grails install was found");
	}
	
	public GrailsTest(String name) {
		super(name);
	}

	public GrailsTest() {
		super();
	}
	
	public static byte[] getBytesFromFile(File file) throws IOException {
	    InputStream is = new FileInputStream(file);

	    // Get the size of the file
	    long length = file.length();

	    // You cannot create an array using a long type.
	    // It needs to be an int type.
	    // Before converting to an int type, check
	    // to ensure that file is not larger than Integer.MAX_VALUE.
	    if (length > Integer.MAX_VALUE) {
	        // File is too large
	    }

	    // Create the byte array to hold the data
	    byte[] bytes = new byte[(int)length];

	    // Read in the bytes
	    int offset = 0;
	    int numRead = 0;
	    while (offset < bytes.length
	           && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
	        offset += numRead;
	    }

	    // Ensure all the bytes have been read in
	    if (offset < bytes.length) {
	        throw new IOException("Could not completely read file "+file.getName());
	    }

	    // Close the input stream and return bytes
	    is.close();
	    return bytes;
	}
	
    public static String getContents(IFile file) throws CoreException, IOException {
        InputStream is = file.getContents();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuffer buffer= new StringBuffer();
        char[] readBuffer= new char[2048];
        int n= br.read(readBuffer);
        while (n > 0) {
            buffer.append(readBuffer, 0, n);
            n= br.read(readBuffer);
        }
        return buffer.toString();
    }

	
	/**
	 * Set a bunch of preferences so that m2eclipse hopefully isn't doing a lot of time consuming stuff in the
	 * background.
	 */
	public static void mavenOffline() throws Error {
		System.out.println("Pacifying m2eclipse...");
		IEclipsePreferences m2EclipsePrefs = new InstanceScope()
				.getNode("org.eclipse.m2e.core");
		m2EclipsePrefs.putBoolean("eclipse.m2.offline", true);
		m2EclipsePrefs.putBoolean("eclipse.m2.globalUpdatePolicy", false);
		m2EclipsePrefs.putBoolean("eclipse.m2.updateIndexes", false);
		try {
			m2EclipsePrefs.flush();
		} catch (BackingStoreException e) {
			throw new Error(e);
		}

//		LegacyProjectChecker.NON_BLOCKING = true;
		System.out.println("Pacifying m2eclipse...DONE");
	}

}
