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
package org.grails.ide.eclipse.maven.test;

import static org.grails.ide.eclipse.commands.GrailsCommandFactory.createDomainClass;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.codehaus.jdt.groovy.model.GroovyNature;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.GrailsResourceUtil;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.test.util.GrailsTest;
import org.grails.ide.eclipse.ui.internal.importfixes.GrailsProjectVersionFixer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 
 * @author Andrew Eisenberg
 * @created Sep 11, 2012
 */
@SuppressWarnings("nls")
public class GrailsMavenTests extends AbstractLifecycleMappingTest {
    
    private final static String MAVEN_BUILDER_ID = "org.eclipse.m2e.core.maven2Builder";
    private final static String FACET_BUILDER_ID = "org.eclipse.wst.common.project.facet.core.builder";
    
    private final static Dependency FEEDS_DEPENDENCY = createDependency("org.grails.plugins", "feeds", "1.5", "zip");
    private static Dependency createDependency(String group, String artifact, String version, String type) {
        Dependency d = new Dependency();
        d.setArtifactId(artifact);
        d.setGroupId(group);
        d.setVersion(version);
        d.setType(type);
        return d;
    }

    // all entries on the classpath...commented nes are those we are not interested in
    private static String[] EXPECTED_CLASSPATH_ENTRIES = {
        "/ggts-maven-test/src/main/java",
        "/ggts-maven-test/grails-app/conf",
        "/ggts-maven-test/grails-app/controllers",
        "/ggts-maven-test/grails-app/domain",
        "/ggts-maven-test/grails-app/services",
        "/ggts-maven-test/grails-app/taglib",
        "/ggts-maven-test/grails-app/utils",
        "/ggts-maven-test/src/groovy",
        "/ggts-maven-test/src/java",
        "/ggts-maven-test/src/main/resources",
        "/ggts-maven-test/src/test/java",
        "/ggts-maven-test/test/unit",
        "/ggts-maven-test/test/integration",
//        "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.6",
        "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER",
        "/ggts-maven-test/.link_to_grails_plugins/database-migration-1.1/grails-app/conf",
        "/ggts-maven-test/.link_to_grails_plugins/database-migration-1.1/grails-app/controllers",
        "/ggts-maven-test/.link_to_grails_plugins/database-migration-1.1/grails-app/views",
        "/ggts-maven-test/.link_to_grails_plugins/database-migration-1.1/src/groovy",
        "/ggts-maven-test/.link_to_grails_plugins/hibernate-2.1.1/grails-app/i18n",
        "/ggts-maven-test/.link_to_grails_plugins/jquery-1.7.1/grails-app/conf",
        "/ggts-maven-test/.link_to_grails_plugins/jquery-1.7.1/grails-app/i18n",
        "/ggts-maven-test/.link_to_grails_plugins/jquery-1.7.1/grails-app/services",
        "/ggts-maven-test/.link_to_grails_plugins/jquery-1.7.1/grails-app/taglib",
        "/ggts-maven-test/.link_to_grails_plugins/jquery-1.7.1/src/groovy",
        "/ggts-maven-test/.link_to_grails_plugins/resources-1.1.6/grails-app/conf",
        "/ggts-maven-test/.link_to_grails_plugins/resources-1.1.6/grails-app/i18n",
        "/ggts-maven-test/.link_to_grails_plugins/resources-1.1.6/grails-app/resourceMappers",
        "/ggts-maven-test/.link_to_grails_plugins/resources-1.1.6/grails-app/taglib",
        "/ggts-maven-test/.link_to_grails_plugins/resources-1.1.6/src/groovy",
        "/ggts-maven-test/.link_to_grails_plugins/resources-1.1.6/src/java",
        "org.grails.ide.eclipse.core.CLASSPATH_CONTAINER",
        "GROOVY_DSL_SUPPORT"
    };
    
    private final static String[] FEEDS_PATHS = {
        "/ggts-maven-test/.link_to_grails_plugins/feeds-1.5/grails-app/i18n",
        "/ggts-maven-test/.link_to_grails_plugins/feeds-1.5/grails-app/taglib",
        "/ggts-maven-test/.link_to_grails_plugins/feeds-1.5/src/groovy",
    };
    
    @Override
    protected void setUp() throws Exception {
        GrailsProjectVersionFixer.testMode();
        GrailsTest.ensureDefaultGrailsVersion(GrailsVersion.V_2_1_1);
        GrailsTest.waitForGrailsIntall();
        super.setUp();
    }

    public void testGrailsApp() throws Exception {
        ResolverConfiguration configuration = new ResolverConfiguration();
        IProject project = importProject("testProjects/ggts-maven-test/pom.xml", configuration);

        project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
        assertNaturesAndBuilders(project);
        assertClasspath(project, false);
        
        // add a dependency
        IFile pomFile = project.getFile("pom.xml");
        addDependency(FEEDS_DEPENDENCY, pomFile);
        MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
        
        project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
        assertClasspath(project, true);

        // remove dependency
        removeDependency(FEEDS_DEPENDENCY, pomFile);
        cleanProject(project);
        assertClasspath(project, false);

        // issue a grails command
        GrailsCommand cmd = createDomainClass(project, "gTunes.Song");
        ILaunchResult result = cmd.synchExec();
        System.out.println(result.getOutput());
        GrailsTest.assertRegexp("Created.*Song", result.getOutput());
        project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        assertTrue("Expected domain class to exist", project.getFile("/grails-app/domain/gTunes/Song.groovy").exists());
        
        project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
        assertClasspath(project, false);
    }

    private void cleanProject(IProject project) throws CoreException, InterruptedException {
        // simulate a cleam
        ILaunchConfiguration configuration = createLaunchConfiguration(project, "clean");
        DebugEventWaiter waiter = new DebugEventWaiter(DebugEvent.TERMINATE);
        configuration.launch("run", monitor, false);
        waiter.waitForEvent();
        project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        waitForJobsToComplete(monitor);
        MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
        project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    }

    private void assertNaturesAndBuilders(IProject project)
            throws CoreException {
        assertTrue("Expected Grails nature", project.hasNature(GrailsNature.NATURE_ID));
        assertFalse("Expected no Old Grails nature", project.hasNature(GrailsNature.OLD_NATURE_ID));
        assertTrue("Expected Groovy nature", project.hasNature(GroovyNature.GROOVY_NATURE));
        assertTrue("Expected Java nature", project.hasNature(JavaCore.NATURE_ID));
        
        // check builders
        ICommand[] buildSpec = project.getDescription().getBuildSpec();
        boolean javaBuilder = false;
        boolean m2eBuilder = false;
        boolean facetBuilder = false;
        for (ICommand command : buildSpec) {
            if (command.getBuilderName().equals(JavaCore.BUILDER_ID)) {
                javaBuilder = true;
            } else if (command.getBuilderName().equals(MAVEN_BUILDER_ID)) {
                m2eBuilder = true;
            } else if (command.getBuilderName().equals(FACET_BUILDER_ID)) {
                facetBuilder = true;
            }
        }
        assertTrue("Expected Java builder", javaBuilder);
        assertTrue("Expected m2e builder", m2eBuilder);
        assertTrue("Expected facet builder", facetBuilder);
        assertEquals("Expected exactly 3 builders, but found:\n" + Arrays.toString(buildSpec), 3, buildSpec.length);
    }

    private void assertClasspath(IProject project, boolean feedsExists) throws CoreException {
        assertNull(getProblems(project));
        IClasspathEntry[] entries = JavaCore.create(project).getRawClasspath();
        for (String pathName : EXPECTED_CLASSPATH_ENTRIES) {
            assertClasspathEntry(pathName, entries);
        }
        for (String pathName : FEEDS_PATHS) {
            if (feedsExists) {
                assertClasspathEntry(pathName, entries);
            } else {
                assertNoClasspathEntry(pathName, entries);
            }
        }
        assertClasspathAttributes(entries);
    }
    
    /**
     * If the classpath entry has a grails source folder attribute, then it must also have a maven.pomderived attribite
     * @param entries
     */
    private void assertClasspathAttributes(IClasspathEntry[] entries) {
        for (IClasspathEntry entry : entries) {
            if (GrailsResourceUtil.isGrailsClasspathEntry(entry) && !GrailsResourceUtil.hasClasspathAttribute(entry, IClasspathManager.POMDERIVED_ATTRIBUTE)) {
                fail("Classpath entry is grails source folder, but was not added by m2e: " + entry);
            }
        }
    }

    private void assertClasspathEntry(String pathName, IClasspathEntry[] entries) {
        boolean found = false;
        for (IClasspathEntry entry : entries) {
            if (entry.getPath().toPortableString().equals(pathName)) {
                found = true;
                break;
            }
        }
        if (!found) {
            fail("Expected to have " + pathName + " as a classpath entry, but didn't find it. Found:\n" + Arrays.toString(entries));
        }
    }

    private void assertNoClasspathEntry(String pathName, IClasspathEntry[] entries) {
        boolean found = false;
        for (IClasspathEntry entry : entries) {
            if (entry.getPath().toPortableString().equals(pathName)) {
                found = true;
                break;
            }
        }
        if (found) {
            fail("Expected not to have " + pathName + " as a classpath entry, but found it. Found:\n" + Arrays.toString(entries));
        }
    }
    
    private String getProblems(IProject project) throws CoreException {
        StringBuilder sb = new StringBuilder();
        sb.append(createErrorString(project, IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER));
        sb.append(createErrorString(project, IJavaModelMarker.BUILDPATH_PROBLEM_MARKER));
        sb.append(createErrorString(project, IMavenConstants.MARKER_BUILD_ID));
        return sb.length() > 0 ? "Problems:\n" + sb.toString() : null;
    }

    private String createErrorString(IProject project, String markerName) throws CoreException {
        IMarker[] markers = project.findMarkers(markerName, true, IResource.DEPTH_INFINITE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < markers.length; i++) {
            if (((Integer) markers[i].getAttribute(IMarker.SEVERITY)).intValue() == IMarker.SEVERITY_ERROR) {
                sb.append("  ");
                sb.append(markers[i].getResource().getName()).append(" : ");
                sb.append(markers[i].getAttribute(IMarker.LOCATION)).append(" : ");
                sb.append(markers[i].getAttribute(IMarker.MESSAGE)).append("\n");
            }
        }
        return sb.toString();
    }
    
    // borrowed from org.eclipse.m2e.core.ui.internal.actions.AddDependencyAction
    private void addDependency(final Dependency dependency, IFile file)
            throws IOException, CoreException {
        PomEdits.performOnDOMDocument(new OperationTuple(file, new Operation() {
            public void process(Document document) {
                Element depsEl = PomEdits.getChild(
                        document.getDocumentElement(), PomEdits.DEPENDENCIES);
                Element dep = PomEdits.findChild(
                        depsEl,
                        PomEdits.DEPENDENCY,
                        PomEdits.childEquals(PomEdits.GROUP_ID,
                                dependency.getGroupId()),
                        PomEdits.childEquals(PomEdits.ARTIFACT_ID,
                                dependency.getArtifactId()));
                if (dep == null) {
                    dep = PomHelper.createDependency(depsEl,
                            dependency.getGroupId(),
                            dependency.getArtifactId(), dependency.getVersion());
                } else {
                    // only set version if already exists
                    if (dependency.getVersion() != null) {
                        PomEdits.setText(
                                PomEdits.getChild(dep, PomEdits.VERSION),
                                dependency.getVersion());
                    }
                }
                if (dependency.getType() != null //
                        && !"jar".equals(dependency.getType()) // //$NON-NLS-1$
                        && !"null".equals(dependency.getType())) { // guard against MNGECLIPSE-622 //$NON-NLS-1$

                    PomEdits.setText(PomEdits.getChild(dep, PomEdits.TYPE),
                            dependency.getType());
                }

                if (dependency.getClassifier() != null) {
                    PomEdits.setText(
                            PomEdits.getChild(dep, PomEdits.CLASSIFIER),
                            dependency.getClassifier());
                }

                if (dependency.getScope() != null
                        && !"compile".equals(dependency.getScope())) { //$NON-NLS-1$
                    PomEdits.setText(PomEdits.getChild(dep, PomEdits.SCOPE),
                            dependency.getScope());
                }

            }
        }));
    }
    
    private void removeDependency(final Dependency dependency, IFile file)
            throws IOException, CoreException {
        PomEdits.performOnDOMDocument(new OperationTuple(file, new Operation() {
            public void process(Document document) {
                Element depsEl = PomEdits.getChild(
                        document.getDocumentElement(), PomEdits.DEPENDENCIES);
                Element dep = PomEdits.findChild(
                        depsEl,
                        PomEdits.DEPENDENCY,
                        PomEdits.childEquals(PomEdits.GROUP_ID,
                                dependency.getGroupId()),
                        PomEdits.childEquals(PomEdits.ARTIFACT_ID,
                                dependency.getArtifactId()));
                if (dep != null) {
                    PomEdits.removeChild(depsEl, dep);
                }
            }
        }));
    }
    
    
    // copied from org.grails.ide.eclipse.maven.test.GrailsMavenTests
    private ILaunchConfiguration createLaunchConfiguration(IContainer basedir,
            String goal) throws CoreException {
        try {
            ILaunchManager launchManager = DebugPlugin.getDefault()
                    .getLaunchManager();
            ILaunchConfigurationType launchConfigurationType = launchManager
                    .getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);

            ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType
                    .newInstance(null, "Test launch");
            workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, basedir
                    .getLocation().toOSString());
            workingCopy.setAttribute(MavenLaunchConstants.ATTR_GOALS, goal);
            workingCopy.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
            workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE,
                    "${project}"); //$NON-NLS-1$
            workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);
            workingCopy.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false);
            setProjectConfiguration(workingCopy, basedir);

            IPath path = getJREContainerPath(basedir);
            if (path != null) {
                workingCopy
                        .setAttribute(
                                IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
                                path.toPortableString());
            }
            return workingCopy;
        } catch (CoreException ex) {
            throw ex;
        }
    }   
    
    // copied from org.grails.ide.eclipse.maven.test.GrailsMavenTests
    private void setProjectConfiguration(
            ILaunchConfigurationWorkingCopy workingCopy, IContainer basedir) {
        IMavenProjectRegistry projectManager = MavenPlugin
                .getMavenProjectRegistry();
        IFile pomFile = basedir
                .getFile(new Path(IMavenConstants.POM_FILE_NAME));
        IMavenProjectFacade projectFacade = projectManager.create(pomFile,
                false, new NullProgressMonitor());
        if (projectFacade != null) {
            ResolverConfiguration configuration = projectFacade
                    .getResolverConfiguration();

            String selectedProfiles = configuration.getSelectedProfiles();
            if (selectedProfiles != null && selectedProfiles.length() > 0) {
                workingCopy.setAttribute(MavenLaunchConstants.ATTR_PROFILES,
                        selectedProfiles);
            }
        }
    }

    // copied from org.grails.ide.eclipse.maven.test.GrailsMavenTests
    private IPath getJREContainerPath(IContainer basedir) throws CoreException {
        IProject project = basedir.getProject();
        if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
            IJavaProject javaProject = JavaCore.create(project);
            IClasspathEntry[] entries = javaProject.getRawClasspath();
            for (int i = 0; i < entries.length; i++) {
                IClasspathEntry entry = entries[i];
                if (JavaRuntime.JRE_CONTAINER
                        .equals(entry.getPath().segment(0))) {
                    return entry.getPath();
                }
            }
        }
        return null;
    }


}
