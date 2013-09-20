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
package org.grails.ide.eclipse.core.internal.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;


/**
 * @author Christian Dupuis
 * @author Nieraj Singh
 * @author Kris De Volder
 */
public class DefaultGrailsInstall implements IGrailsInstall {

	private static final String UNKNOWN_VERSION_STRING = "<unknown>";
	private static String defaultGrailsWorkDir = null;

	/**
	 * If this is set to a non-null value then it will be used to initialize
	 * each instance of DefaultGrailsInstall created. This is mainly useful for
	 * testing purposes, to ensure that when running JUnit tests, the ".grails"
	 * folder is where we want it and can be easily cleared before running
	 * tests.
	 */
	public static void setDefaultGrailsWorkDir(String value) {
		Assert.isLegal(
				value == null || !value.contains(" "),
				"Grails commandLine parser will get confused by spaces. Paths with ' ' in them are not allowed!");
		defaultGrailsWorkDir = value;
	}

	public static String getDefaultGrailsWorkDir() {
		return defaultGrailsWorkDir;
	}

	private final String home;
	private boolean isDefault = false;
	private final String name;
	private SpringloadedJarFinder loadedJarFinder = new SpringloadedJarFinder();

	public DefaultGrailsInstall(String home, String name, boolean isDefault) {
		this.home = (home != null && !home.endsWith(File.separator) ? home
				+ File.separator : home);
		this.name = name;
		this.isDefault = isDefault;
	}

	public File[] getBootstrapClasspath() {
		if (home == null || home.length() == 0) {
			return new File[0];
		}

		Set<File> jars = new LinkedHashSet<File>();

		File grailsHome = new File(home);
		if (grailsHome.exists()) {
			addBootstrapJar(jars, new File(home, "lib"));
//			addBootstrapJar(jars, new File(home, "lib/org.codehaus.groovy/groovy-all/jars"));
			addBootstrapJar(jars, new File(home, "dist"));
		}
		if (jars.isEmpty()) {
			//That's probably an issue. Rather than have this produce cryptic problems / errors much later.
			//Throw an error now that includes 'home' dir. 
			//May help diagnose problems like:
			//http://forum.springsource.org/showthread.php?138848-Grails-2-2-2-problem-with-STS-2-9&p=449005#post449005
			throw new Error("Couldn't find bootstrap classpath jars in Grails install at: '"+home+"'");
		}
		return jars.toArray(new File[jars.size()]);
	}

	public File[] getDependencyClasspath() {
		if (home == null || home.length() > 0) {
			return new File[0];
		}

		Set<File> jars = new LinkedHashSet<File>();

		File grailsHome = new File(home);
		if (grailsHome.exists()) {

			for (File jar : new File(grailsHome + "/dist").listFiles()) {
				if (jar.isFile() && jar.getName().endsWith(".jar")) {
					jars.add(jar);
				}
			}
			for (File jar : new File(grailsHome + "/lib").listFiles()) {
				if (jar.isFile() && jar.getName().endsWith(".jar")) {
					jars.add(jar);
				}
			}

			String version = GrailsCoreActivator.getDefault()
					.getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
			if (version.endsWith("qualifier")) {
				addBundleFile(jars, "/bin");
			} else {
				addBundleFile(jars, "/");
			}
		}
		return jars.toArray(new File[jars.size()]);
	}

	public String getHome() {
		return home;
	}

	public String getName() {
		return name;
	}

	public String getPluginHome(IProject project) {
		return getGrailsWorkDir() + File.separator + "projects"
				+ File.separator + project.getName() + File.separator
				+ "plugins";
	}

	/**
	 * Warning, don't use this method to determine the .grails work dir that grails
	 * actually uses. This method only returns a "correct" result if either
	 *   - the grails work dir is set by calling setDefaultGrailsWorkDir => used during testing
	 *   - the grails workdir is the default grails computes itself (i.e. it was not set by the user in some
	 *     other way (e.g. by using settings.groovy to change it from the grails default))
	 * Instead, you can use the utility method in GrailsPluginUtil.getGrailsWorkDir.
	 * 
	 * @return The location of the ".grails" folder (on the command line is set
	 *         by -Dgrails.work.dir=...)
	 */
	public String getGrailsWorkDir() {
		if (defaultGrailsWorkDir != null)
			return defaultGrailsWorkDir;
		else {
			String userHome = System.getProperty("user.home");
			return userHome + File.separator + ".grails" + File.separator
					+ getVersionString();
		}
	}

	public String getVersionString() {
		if (home != null) {
			File buildProperties = new File(home, "build.properties");
			if (buildProperties.exists()) {
				Properties props = new Properties();
				try {
					props.load(new FileInputStream(buildProperties));
					return props.getProperty("grails.version");
				} catch (FileNotFoundException e) {
				} catch (IOException e) {
				}
			}
		}
		return UNKNOWN_VERSION_STRING;
	}

	public boolean isDefault() {
		return isDefault;
	}

	private void addBundleFile(Set<File> jars, String path) {
		try {
			URL embeddedUrl = FileLocator.toFileURL(GrailsCoreActivator
					.getDefault().getBundle().getEntry(path));
			try {
				jars.add(new File(embeddedUrl.toURI()));
			} catch (Exception e) {
				jars.add(new File(embeddedUrl.getPath()));
			}
		} catch (IOException e1) {
		}
	}

	private void addBootstrapJar(Set<File> jars, File home) {
		if (home.isDirectory()) {
			File[] files = home.listFiles();
			if (files!=null) {
				for (File file : files) {
					String name = file.getName();
					if (file.isDirectory()) {
						addBootstrapJar(jars, file);
					} else if (name.endsWith(".jar") 
							&& (
								name.startsWith("groovy-all") || name.startsWith("grails-bootstrap")
							) && !(
								name.endsWith("sources.jar") || name.endsWith("javadoc.jar")
							)) {
						jars.add(file);
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		return "DefaultGrailsInstall(" + home + ")";
	}

	public GrailsVersion getVersion() {
		return new GrailsVersion(getVersionString());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((home == null) ? 0 : home.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultGrailsInstall other = (DefaultGrailsInstall) obj;
		if (home == null) {
			if (other.home != null)
				return false;
		} else if (!home.equals(other.home))
			return false;
		return true;
	}

	public IStatus verify() {
		String homeStr = getHome();
		if (homeStr==null) {
			return error("Install '"+getName()+"' home is not set");
		} 
		File home = new File(homeStr);
		if (!home.exists()) {
			return error("Install '"+getName()+"' at location '"+homeStr+"' does not exist. Was it deleted or moved?");
		}
		if (UNKNOWN_VERSION_STRING.equals(getVersionString()) || getVersionString()==null) {
			return error("Install '"+getName()+"' does not appear to be a valid Grails install.");
		}
		return Status.OK_STATUS;
	}

	private Status error(String msg) {
		return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, msg);
	}
	
	private class SpringloadedJarFinder {
		
		private final String[] searchIn = { 
				"lib/com.springsource.springloaded/springloaded-core",
				"lib/org.springsource.springloaded/springloaded-core"
		};
		
		private File foundJar = null;
		private Version foundVersion = null;
		
		/** Get the springloaded jar, search for it the first time. Cached after that */
		public File get() {
			//Only relevant for Grails 2.0.0 and up
			if (getVersion().compareTo(GrailsVersion.V_2_0_0)>=0) {
				if (foundJar==null) {
					for (String searchLoc : searchIn) {
						if (foundJar!=null) {
							break;
						}
						find(new File(getHome(), searchLoc));
					}
				}
			}
			return foundJar;
		}
		
		private synchronized void find(File libPath) {
			File[] files = libPath.listFiles();
			if (files!=null) {
				for (File jarCandidate : files) {
					if (jarCandidate.isFile()) {
						String fileName = jarCandidate.getName();
						if (fileName.startsWith("springloaded-core-") && fileName.endsWith(".jar") && fileName.indexOf("source")==-1) {
							//Found a springloaded jar.
							String versionString = fileName.substring(
									"springloaded-core-".length(),
									fileName.length()-4/*".jar".length()*/);
							Version version = new Version(versionString); //Not really an OSGi bundle, but this should work anyway.
							if (foundVersion==null || foundVersion.compareTo(version)<0) {
								//Only keep most recent version
								foundVersion = version;
								foundJar = jarCandidate;
							}
						}
					} else if (jarCandidate.isDirectory()) {
						//Extend search into sub directories (to support the mavenRepo-like file layout in Grails 2.0.2)
						find(jarCandidate);
					}
				}
			}
		}
	}

	public File getSpringLoadedJar() {
		return loadedJarFinder.get();
	}

	/**
	 * Check whether a given javaInstall meets requirements to run commands for this Grails install.
	 */
	public void verifyJavaInstall(IVMInstall _javaInstall) throws CoreException {
		GrailsVersion grailsVersion = getVersion();
		if (grailsVersion.compareTo(GrailsVersion.V_2_0_0)>=0) {
			//2.0 or above requires at least Java 1.6
			if (_javaInstall instanceof IVMInstall2) {
				IVMInstall2 javaInstall = (IVMInstall2) _javaInstall;
				String javaVersion = javaInstall.getJavaVersion();
				if (javaVersion!=null) {
					if (javaVersion.compareTo("1.6") < 0) {
						//String compare isn't strictly correct, but will work in this case (at least until we get to
						//Java version 1.10.x
						throw new CoreException(new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID,
								"Grails "+grailsVersion+" requires at least Java 1.6.\n" +
								"The Java install at "+ _javaInstall.getInstallLocation()+"\n" +
								"is version "+javaVersion));
					}
				}
			}
		}
	}

	public File getSpringLoadedCacheDir() {
		File wsMetadata = GrailsCoreActivator.getDefault().getStateLocation().toFile();
		File cacheDir = new File(wsMetadata, getVersionString());
		cacheDir.mkdirs();
		return cacheDir;
	}

}
