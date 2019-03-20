/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.ui.internal.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsResourceUtil;
import org.grails.ide.eclipse.core.model.GrailsVersion;


/**
 * Similar to GrailsLaunchShortcut, but specifically adapted to create a test-app command that
 * allows running the command with different parameters depending on the selected resource.
 * @author Kris De Volder
 * @since 3.5.0
 */
public class GrailsTestLaunchShortcut extends GrailsLaunchShortcut {

	private static final boolean DEBUG = false;

	private static void debug(String string) {
		if (DEBUG) {
			System.out.println(string);
		}
	}

	
	private static final String TEST_APP = "test-app";
	
	@Override
	protected String getScriptFor(IResource resource) {
		if (GrailsResourceUtil.isTestFolder(resource)) {
			String script = TEST_APP+" -"+resource.getName();
			System.out.println("grails command = "+script);
			return script;
		}
		else {
			String script = TEST_APP;
			String testType = GrailsResourceUtil.typeOfTest(resource);
			if (testType!=null) {
				script += " -"+testType;
			}
			if (GrailsResourceUtil.isSourceFile(resource)) {
				IJavaElement javaElement = JavaCore.create(resource);
				if (javaElement!=null) {
					int elementType = javaElement.getElementType();
					switch (elementType) {
					case IJavaElement.PACKAGE_FRAGMENT:
						IPackageFragment pkg = (IPackageFragment) javaElement;
						script += " "+pkg.getElementName()+".*";
						break;
					case IJavaElement.COMPILATION_UNIT:
						String pathIncludingSourceFolder = resource.getProjectRelativePath().toString();
						IPackageFragmentRoot sourceRoot = (IPackageFragmentRoot) javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
						String sourceFolderPath = sourceRoot.getResource().getProjectRelativePath().toString();
						Assert.isTrue(pathIncludingSourceFolder.startsWith(sourceFolderPath+"/"));
						String pathWithoutSourceFolder = pathIncludingSourceFolder.substring(sourceFolderPath.length()+1);
						if (isRecentGrails(resource.getProject())) {
							pathWithoutSourceFolder = removeSuffix(pathWithoutSourceFolder, ".groovy");
						} else {
							pathWithoutSourceFolder = removeSuffix(pathWithoutSourceFolder, "Test.groovy", "Tests.groovy", ".groovy");
						}
						if (pathWithoutSourceFolder!=null) {
							String testTarget = pathWithoutSourceFolder.replace('/', '.');
							script += " " + testTarget;
						}
					default:
						break;
					}
				}
			}
			debug("grails command = "+script);
			return script;
		}
	}

	/**
	 * From 1.3.6 and on, we use slightly different rules for "Test" class naming conventions. So
	 * 'recent' here means 1_3_6 or above.
	 */
	private boolean isRecentGrails(IProject project) {
		if (project!=null) {
			GrailsVersion version = GrailsVersion.getGrailsVersion(project);
			return GrailsVersion.V_1_3_6.compareTo(version)<=0;
		}
		return false;
	}

	/**
	 * If given String ends with one of the expected suffixes, this suffix is removed.
	 * @param string
	 * @param expectedSuffixes
	 * @return String without suffix, or null if the String didn't have one of the expected suffixes. 
	 */
	private String removeSuffix(String string, String... expectedSuffixes) {
		for (String suffix : expectedSuffixes) {
			if (string.endsWith(suffix)) 
				return string.substring(0,string.length()-suffix.length());
		}
		return null;
	}

	public ILaunchConfiguration findLaunchConfiguration(IResource resource) throws CoreException {
		ILaunchConfiguration result = super.findLaunchConfiguration(resource);
		if (result!=null) {
			GrailsCoreActivator.getDefault().addGrailsCommandResourceListener(new OpenInterestingNewResourceListener(resource.getProject()));
		}
		return result;
	}

}
