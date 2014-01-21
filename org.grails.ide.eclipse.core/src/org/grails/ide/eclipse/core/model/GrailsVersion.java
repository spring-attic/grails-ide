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
package org.grails.ide.eclipse.core.model;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;


/**
 * An instance of this class represents a grails version number, parsed from a String.
 * @author Kris De Volder
 */
public class GrailsVersion implements Comparable<GrailsVersion> {
	
	/**
	 * An object suitable to represent an "unknown" grails version. This version
	 * is "smaller" than any known grails version.
	 */
	public static final GrailsVersion UNKNOWN = new GrailsVersion(null);
	
	String versionString;
	
	/**
	 * Greater than any 1.1.x version but smaller than any 1.2.x version.
	 */
	public static final GrailsVersion V_1_2 = new GrailsVersion("1.2");
	public static final GrailsVersion V_1_3_5 = new GrailsVersion("1.3.5");
	public static final GrailsVersion V_1_3_6 = new GrailsVersion("1.3.6");
	public static final GrailsVersion V_1_3_7 = new GrailsVersion("1.3.7");
	public static final GrailsVersion V_1_3_8 = new GrailsVersion("1.3.8");
	public static final GrailsVersion BUILDSNAPHOT_2_0_2 = new GrailsVersion("2.0.2.BUILD-SNAPSHOT", 
			"http://hudson.grails.org/view/Grails%202.0.x/job/grails_core_2.0.x/lastStableBuild/artifact/build/distributions/grails-2.0.2.BUILD-SNAPSHOT.zip");

	public static final GrailsVersion V_2_0_ = new GrailsVersion("2.0"); //Any '2.0' version *including* milestones is 'greater' than this one
	public static final GrailsVersion V_2_0_0 = new GrailsVersion("2.0.0"); //Any 2.0 version *excluding* milestones is greater than this one
	public static final GrailsVersion V_2_0_1 = new GrailsVersion("2.0.1");
	public static final GrailsVersion V_2_0_2 = new GrailsVersion("2.0.2");
	public static final GrailsVersion V_2_0_3 = new GrailsVersion("2.0.3");
	public static final GrailsVersion V_2_0_4 = new GrailsVersion("2.0.4");

	public static final GrailsVersion V_2_1_ = new GrailsVersion("2.1"); //Any '2.1' version *including* milestones is 'greater' than this one
	public static final GrailsVersion V_2_1_0 = new GrailsVersion("2.1.0");
	public static final GrailsVersion V_2_1_1 = new GrailsVersion("2.1.1");
	public static final GrailsVersion V_2_1_0_revisit = V_2_1_0; // references to this constant should be reviewed when new version of Grails comes out.

	public static final GrailsVersion V_2_2_ = new GrailsVersion("2.2"); //Any '2.2' version *including* milestones is 'greater' than this one
	public static final GrailsVersion V_2_2_0 = new GrailsVersion("2.2.0");
	public static final GrailsVersion V_2_2_1 = new GrailsVersion("2.2.1");
	public static final GrailsVersion V_2_2_2 = new GrailsVersion("2.2.2");
	public static final GrailsVersion V_2_2_3 = new GrailsVersion("2.2.3");
	public static final GrailsVersion V_2_2_4 = new GrailsVersion("2.2.4");
	
//	public static final GrailsVersion V_2_0_0_M1 = new GrailsVersion("2.0.0.M1");
	public static final GrailsVersion V_2_0_0_M2 = new GrailsVersion("2.0.0.M2");
//	public static final GrailsVersion V_2_0_0_RC1 = new GrailsVersion("2.0.0.RC1");
//	public static final GrailsVersion V_2_0_0_RC2 = new GrailsVersion("2.0.0.RC2");
//	public static final GrailsVersion V_2_0_0_RC3 = new GrailsVersion("2.0.0.RC3");
	
//	public static final GrailsVersion V_2_2_2_BUILDSNAP = new GrailsVersion("2.2.2.BUILD-SNAPSHOT",
//			"http://hudson.grails.org/job/grails_core_2.2.x/lastSuccessfulBuild/artifact/build/distributions/grails-2.2.2.BUILD-SNAPSHOT.zip");

    public static final GrailsVersion V_2_3_ = new GrailsVersion("2.3"); //Any '2.3' version *including* milestones is 'greater' than this one
	public static final GrailsVersion V_2_3_0 = new GrailsVersion("2.3.0");
	public static final GrailsVersion V_2_3_0_RC1 = new GrailsVersion("2.3.0.RC1");

	public static final GrailsVersion V_2_3_1 = new GrailsVersion("2.3.1");
	public static final GrailsVersion V_2_3_2 = new GrailsVersion("2.3.2");
	public static final GrailsVersion V_2_3_4 = new GrailsVersion("2.3.4");
	public static final GrailsVersion V_2_3_5 = new GrailsVersion("2.3.5");
	
//	public static final GrailsVersion V_2_3_2_SNAPSHOT = new GrailsVersion("2.3.2.BUILD-SNAPSHOT", 
//			"http://hudson.grails.org/view/Grails%202.3.x/job/grails_core_2.3.x/lastStableBuild/artifact/build/distributions/grails-2.3.0.BUILD-SNAPSHOT.zip");
    
    public static final GrailsVersion MOST_RECENT_1_3 = V_1_3_8;
	
	//////////////////////////////////////////
	//For running test with 1.3.X
	
//	public static final GrailsVersion PREVIOUS_PREVIOUS = V_1_3_5;
//	public static final GrailsVersion PREVIOUS = V_1_3_6; 
//	public static final GrailsVersion MOST_RECENT = V_1_3_7;

	//////////////////////////////////////////
	//For running test with 2.0:
	
	// Note: These 'constants' are now initialised in GrailsTestUtilActivator to run tests with different
	// GrailsVersions
	
	public static GrailsVersion PREVIOUS_PREVIOUS = V_1_3_7;
	public static GrailsVersion PREVIOUS = V_2_1_1; 
	public static GrailsVersion MOST_RECENT = V_2_2_4;

	private int[] numbers;
	private String qualifier;
	
	private boolean parseError = false;
	public static final GrailsVersion SMALLEST_SUPPORTED_VERSION = V_1_2;

	private URI downloadLocation = null;
	
	public GrailsVersion(String versionString) {
		this(versionString, (URI)null);
	}

	public GrailsVersion(String version, String downloadLocationURI) {
		this(version, uri(downloadLocationURI));
	}
	
	private static URI uri(String str) {
		try {
			return new URI(str);
		} catch (URISyntaxException e) {
			throw new Error(e);
		}
	}

	public GrailsVersion(String versionString, URI downloadLocation) {
		this.downloadLocation = downloadLocation;
		if (versionString==null || versionString.equals("<unknown>")) {
			this.versionString = "<unknown>";
			qualifier = "";
			numbers = new int[0];
		} else {
			this.versionString = versionString;
			StringTokenizer tokenIzer = new StringTokenizer(versionString, ".");
			numbers = new int[tokenIzer.countTokens()];
			int i = 0;
			for (; i < numbers.length;) {
				String token  = tokenIzer.nextToken();
				try {
					numbers[i] = Integer.valueOf(token);
					i++;
				} catch(NumberFormatException e) {
					qualifier = token;
					if (i!=numbers.length-1) {
						parseError = true; //Only at most one qualifier is expected 
					}
					break;
				}
			}
			if (i < numbers.length) {
				int[] copy = new int[i];
				System.arraycopy(numbers, 0, copy, 0, i);
				numbers = copy;
			}
		}
	}
	
	public URI getDownloadLocation() {
		return downloadLocation;
	}
	
	@Override
	public String toString() {
		return getVersionString();
	}
	
	public String getVersionString() {
		if (parseError) {
			return "<##unparseable##>("+versionString+")";
		} else {
			return versionString;
		}
	}

	public int compareTo(GrailsVersion other) {
		int result = compareNoQualifier(other);
		if (result==0) {
			// The numeric part is the same, must consider qualifiers if any
			if (this.qualifier!=null) {
				if (other.qualifier!=null) {
					if (this.qualifier.equals(other.qualifier)) {
						return 0;
					} else {
						// Both have qualifiers and they are different.
						if (this.qualifier.equals("BUILD-SNAPSHOT")) {
							return 1;
						} else if (other.qualifier.equals("BUILD-SNAPSHOT")) {
							return -1;
						}
						return this.qualifier.compareTo(other.qualifier);
					}
				} else {
					// Only this has qualifier
					return -1; // a qualifier makes it "smaller" (version on the way towards numeric version)
				}
			} else {
				if (other.qualifier!=null) {
					//only other has qualifier
					return +1;
				} else {
					//neither one has qualifier
					return result;
				}
			}
		} else {
			//If numbers differ, qualifier doesn't matter.
			return result;
		}
	}

	private int compareNoQualifier(GrailsVersion other) {
		int[] otherNumbers = other.numbers;
		for (int i = 0; i < numbers.length; i++) {
			if (i<otherNumbers.length) {
				if (numbers[i]<otherNumbers[i]) {
					return -1;
				} else if (numbers[i]>otherNumbers[i]) {
					return +1;
				}
				//This number matches, comparison requires looking at next number
			} else {
				//All numbers matching, but other has fewer numbers, fewer numbers is considered smaller, so "this" is greatest
				return +1;
			}
		}
		//All numbers matching, but maybe other has more numbers
		if (otherNumbers.length>numbers.length) {
			return -1;
		} else {
			return 0; // no differences found.
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GrailsVersion) {
			return this.compareTo((GrailsVersion) obj)==0;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return versionString.hashCode();
	}

	/**
	 * Convenience method for when we are comparing some minimum required version to 
	 * a version String.
	 */
	public boolean isSatisfiedBy(String versionString) {
		return this.compareTo(new GrailsVersion(versionString)) <= 0;
	}

	/**
	 * Retrieve the grails version, according to the project's aplication.properties file (i.e. what does grails think
	 * the grails version is for this project).
	 */
	public static GrailsVersion getGrailsVersion(IProject project) {
		if (project!=null) {
			try {
				Properties props = GrailsBuildSettingsHelper.getApplicationProperties(project);
				if (props!=null) {
					String versionString = (String) props.get("app.grails.version");
					return new GrailsVersion(versionString);
				}
				return UNKNOWN;
			} catch (Throwable e) {
				GrailsCoreActivator.log("Couldn't determine grails version for project "+project, e);
			}
		}
		return UNKNOWN;
	}

	public static GrailsVersion getGrailsVersion(File project) {
		if (project!=null && GrailsNature.looksLikeGrailsProject(project)) {
			try {
				Properties props = GrailsBuildSettingsHelper.getApplicationProperties(project);
				if (props!=null) {
					String versionString = (String) props.get("app.grails.version");
					return new GrailsVersion(versionString);
				}
				return UNKNOWN;
			} catch (Throwable e) {
				GrailsCoreActivator.log("Couldn't determine grails version for project "+project, e);
			}
		}
		return UNKNOWN;
	}
	
	
	/**
	 * Retrieve grails version of the associated grails install of this project.
	 * I.e. what does "eclipse think" the associated Grails version is of this project.
	 */
	public static GrailsVersion getEclipseGrailsVersion(IProject project) {
		IGrailsInstall install = GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(project);
		if (install!=null) {
			return new GrailsVersion(install.getVersionString());
		}
		return UNKNOWN;
	}

	/**
	 * Attempts to find a Grails install, configured in the workspace, that matches this particular grails
	 * version. If no such install is found this method may return null.
	 * @return A matching Grails install or null.
	 */
	public IGrailsInstall getInstall() {
		return GrailsCoreActivator.getDefault().getInstallManager().getInstallFor(this);
	}

	/**
	 * @return The grails version of the default grails install configured in the workspace, or {@link GrailsVersion}.UNKNOWN, 
	 * if no Grails install is configured in the workspace.
	 */
	public static GrailsVersion getDefault() {
		IGrailsInstall install = GrailsCoreActivator.getDefault().getInstallManager().getDefaultGrailsInstall();
		if (install!=null) {
			return install.getVersion();
		}
		return GrailsVersion.UNKNOWN;
	}

	public boolean isRelease() {
		return !parseError && qualifier==null;
	}
	
	public boolean isSnapshot() {
		if (!parseError && qualifier!=null) {
			return qualifier.contains("SNAPSHOT") || qualifier.contains("RC1");
		}
		return false;
	}
	
}
