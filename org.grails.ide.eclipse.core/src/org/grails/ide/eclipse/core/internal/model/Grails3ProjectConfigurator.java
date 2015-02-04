/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.internal.model;

import static org.grails.ide.eclipse.commands.GrailsCommandUtils.addNaturesAndBuilders;
import static org.grails.ide.eclipse.commands.GrailsCommandUtils.setDefaultOutputFolder;

import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.core.model.IProjectConfigurator;
import org.grails.ide.eclipse.core.workspace.GrailsClassPath;
import org.grails.ide.eclipse.core.workspace.GrailsProject;
import org.grails.ide.eclipse.core.workspace.GrailsWorkspace;
import org.springframework.util.StringUtils;

public class Grails3ProjectConfigurator implements IProjectConfigurator {
	
	private static final String GRADLE_CLASSPATHCONTAINER_PATH = "org.springsource.ide.eclipse.gradle.classpathcontainer";
    public static final String GRADLE_NATURE = "org.springsource.ide.eclipse.gradle.core.nature";

	public static IProjectConfigurator INSTANCE = new Grails3ProjectConfigurator();
	 
	/**
	 * Don't call, this is a singleton
	 */
	private Grails3ProjectConfigurator() {
	}

	public IProject configureNewlyCreatedProject(IGrailsInstall grailsInstall,
			IProject project) throws CoreException {
		Assert.isNotNull(grailsInstall);
		Assert.isLegal(!project.exists());
		
		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());
		
		IProjectDescription desc = project.getDescription();
		addNaturesAndBuilders(desc); //Same stuff as pre Grails 3
		addNature(desc, GRADLE_NATURE); //... and also this
		project.setDescription(desc, new NullProgressMonitor());

		GrailsClassPath entries = new GrailsClassPath();
		IJavaProject javaProject = JavaCore.create(project);
		GrailsProject grailsProject = GrailsWorkspace.get().create(project);
			
		// Add output folder
		setDefaultOutputFolder(javaProject);

		// Add source entries to classpath
		final String[] sourcePaths = { 
				"src/main/java", 
				"src/main/groovy",
				"src/test/java",
				"src/test/groovy",
				"grails-app/conf",
				"grails-app/controllers",
				"grails-app/domain",
				"grails-app/services",
				"grails-app/taglib",
				"grails-app/utils",
		};
		for (String srcPath : sourcePaths) {
			IFolder srcFolder = project.getFolder(srcPath);
			if (srcFolder.exists()) {
				entries.add(JavaCore.newSourceEntry(srcFolder.getFullPath()));
			}
		}
		//Add the Java libraries
		entries.add(JavaCore.newContainerEntry(Path.EMPTY.append(JavaRuntime.JRE_CONTAINER)));

		// Add the Gradle classpath container
		entries.add(JavaCore.newContainerEntry(
					new Path(GRADLE_CLASSPATHCONTAINER_PATH), null,
					null, false));
		grailsProject.setClassPath(entries, new NullProgressMonitor());

		//TODO: stuff below still necessary?
// Make sure class path container and source folders are up-to-date
//			try {
//				refreshDependencies(javaProject, true);
//			} catch (Exception e) {
//				//Sometimes Grails throws exceptions because incomplete classpath and it 
//				//needs a second refresh before it gets the classpath right.
//				refreshDependencies(javaProject, true);
//			}
//
//			javaProject.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, null);
//			return project;
//		}
		return project;
	}

	private void addNature(IProjectDescription desc, String extraNature) {
		LinkedHashSet<String> natures = new LinkedHashSet<String>(Arrays.asList(desc.getNatureIds()));
		if (!natures.contains(extraNature)) {
			natures.add(extraNature);
			desc.setNatureIds(natures.toArray(new String[natures.size()]));
		}
	}

	public static GrailsVersion getGrailsVersion(IProject project) {
		try {
			IFile propsFile = project.getFile("gradle.properties");
			if (propsFile.exists()) {
				Properties props = new Properties();
				InputStream input = propsFile.getContents();
				try {
					props.load(input);
					String versionString = props.getProperty("grailsVersion");
					if (StringUtils.hasText(versionString)) {
						return new GrailsVersion(versionString);
					}
				} finally {
					input.close();
				}
			}
		} catch (Exception e) {
			GrailsCoreActivator.log(e);
		}
		return GrailsVersion.UNKNOWN;
	}

}
