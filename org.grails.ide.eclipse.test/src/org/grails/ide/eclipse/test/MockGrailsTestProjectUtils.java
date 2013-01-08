/*
 * Copyright 2011 SpringSource, a division of VMware, Inc
 * 
 * andrew - Initial API and implementation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.ide.eclipse.test;

import java.io.File;
import java.util.Hashtable;
import java.util.Set;

import org.codehaus.groovy.eclipse.core.model.GroovyRuntime;
import org.codehaus.groovy.eclipse.dsl.RefreshDSLDJob;
import org.codehaus.groovy.eclipse.test.TestProject;
import org.codehaus.jdt.groovy.model.GroovyNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.classpath.PerProjectDependencyDataCache;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.test.util.GrailsTest;

/**
 * 
 * @author Andrew Eisenberg
 * @created Sep 11, 2012
 */
public class MockGrailsTestProjectUtils {

    /**
     * Ensures that the project passed in has a classpath compatible with a grails project.
     * Sets up project natures and compliance levels
     * Sets up the grails dsld file
     * @param project
     * @throws Exception
     */
    public static void mockGrailsProject(IProject project) throws Exception {
        boolean wasAutobuilding = isAutoBuilding();
        try {
            setAutoBuilding(false);
            
            //Ensure Java compliance level is set to something that supports generics
            @SuppressWarnings("rawtypes")
            Hashtable options = JavaCore.getDefaultOptions();
            options.put(JavaCore.COMPILER_COMPLIANCE, "1.5");
            options.put(JavaCore.COMPILER_SOURCE, "1.5");
            JavaCore.setOptions(options);
        
            addGrailsNature(project);
            
            Set<String> dependencies = findDependencies();
            for (String file : dependencies) {
                if (new File(file).exists()) {
                    // adding the xalan jar also adds another jar to 
                    // to classpath.  so just ignore this jar
                    if (file.indexOf("xalan") < 0) {
                        addExternalLibrary(project, file);
                    }
                } else {
                    System.out.println("Warning file does not exist, but was returned by refresh dependencies: " + file);
                }
            }
            
            GroovyRuntime.addGroovyClasspathContainer(JavaCore.create(project));
            
            // now get the grails.dsld, if it exists
            File grailsDSLD = GrailsTestsActivator.getGrailsDSLD();
            addExternalLibrary(project, grailsDSLD.getCanonicalPath());
            // force refresh dslds
            new RefreshDSLDJob(project).run(null);
        } finally {
            setAutoBuilding(wasAutobuilding);
        }
    }

    /**
     * Gets all dependencies from an empty "Real" grails project
     * @return
     * @throws Exception
     */
    private static Set<String> findDependencies() throws Exception {
        IProject grailsProject = GrailsTest.ensureProject(MockGrailsTestProjectUtils.class.getSimpleName());
        PerProjectDependencyDataCache info = GrailsCore.get().connect(grailsProject, PerProjectDependencyDataCache.class);
        Set<String> dependencies = info.getData().getDependencies();
        return dependencies;
    }

    /**
     * FIXADE move to {@link TestProject}
     */
    public static void addEntry(IProject project, IClasspathEntry entryPath) throws JavaModelException {
        IClasspathEntry[] classpath = getClasspath(project);
        IClasspathEntry[] newClaspath = new IClasspathEntry[classpath.length + 1];
        System.arraycopy(classpath, 0, newClaspath, 0, classpath.length);
        newClaspath[classpath.length] = entryPath;
        setClasspath(project, newClaspath);
    }
    
    /**
     * FIXADE move to {@link TestProject}
     */
    public static IClasspathEntry[] getClasspath(IProject project) {
        try {
            JavaProject javaProject = (JavaProject) JavaCore.create(project);
            return javaProject.getExpandedClasspath();
        } catch (JavaModelException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * FIXADE move to {@link TestProject}
     */
    public static void addExternalLibrary(IProject project, String jar) throws JavaModelException {
        addExternalLibrary(project, jar, false);
    }
    

    /**
     * FIXADE move to {@link TestProject}
     */
    public static void addExternalLibrary(IProject project, String jar, boolean isExported) throws JavaModelException {
        addEntry(project, JavaCore.newLibraryEntry(new Path(jar), null, null, isExported));
    }
    
    /**
     * FIXADE move to {@link TestProject}
     */
    public static void setClasspath(IProject project, IClasspathEntry[] entries) throws JavaModelException {
        IJavaProject javaProject = JavaCore.create(project);
        javaProject.setRawClasspath(entries, null);
    }
    
    /**
     * FIXADE move to {@link TestProject}
     */
    public static void setAutoBuilding(boolean value) {
        try {
            IWorkspace w = ResourcesPlugin.getWorkspace();
            IWorkspaceDescription d = w.getDescription();
            d.setAutoBuilding(value);
            w.setDescription(d);
        } catch (CoreException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static boolean isAutoBuilding() {
        IWorkspace w = ResourcesPlugin.getWorkspace();
        IWorkspaceDescription d = w.getDescription();
        return d.isAutoBuilding();
    }

    public static void addGrailsNature(IProject project) throws CoreException {
        IProjectDescription description = project.getDescription();
        description.setNatureIds(new String[] { GrailsNature.NATURE_ID, JavaCore.NATURE_ID, GroovyNature.GROOVY_NATURE });
        project.setDescription(description, null);
    }
}
