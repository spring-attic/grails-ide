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
package org.grails.ide.eclipse.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ILifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;
import org.eclipse.m2e.jdt.internal.AbstractJavaProjectConfigurator;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.core.internal.GrailsResourceUtil;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathContainer;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathUtils;
import org.grails.ide.eclipse.core.internal.classpath.PerProjectDependencyDataCache;
import org.grails.ide.eclipse.core.internal.classpath.SourceFolderJob;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.PerProjectPluginCache;
import org.grails.ide.eclipse.core.launch.ClasspathLocalizer;
import org.grails.ide.eclipse.core.launch.EclipsePluginClasspathEntry;
import org.grails.ide.eclipse.core.launch.GrailsLaunchArgumentUtils;
import org.grails.ide.eclipse.core.model.GrailsInstallManager;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.runtime.shared.SharedLaunchConstants;
import org.springsource.ide.eclipse.commons.frameworks.core.legacyconversion.IConversionConstants;

public class GrailsProjectConfigurator extends AbstractJavaProjectConfigurator implements IJavaProjectConfigurator {
    
    @Override
    public void configure(ProjectConfigurationRequest request,
            IProgressMonitor monitor) throws CoreException {
        IProject project = request.getProject();
        addJavaNature(project, monitor);
        IJavaProject javaProject = JavaCore.create(project);
        boolean noContainer = !GrailsClasspathUtils.hasClasspathContainer(javaProject);
        boolean hasOldContainer = GrailsClasspathUtils.hasOldClasspathContainer(javaProject);
        IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
        List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>(Arrays.asList(rawClasspath));
        if (hasOldContainer) {
            for (Iterator<IClasspathEntry> entryIter = entries.iterator(); entryIter.hasNext();) {
                IClasspathEntry entry = entryIter.next();
                if (entry.getPath().toPortableString().equals(IConversionConstants.GRAILS_OLD_CONTAINER)) {
                    entryIter.remove();
                    break;
                }
            }
        }
        if (noContainer) {
            entries.add(JavaCore.newContainerEntry(
                    GrailsClasspathContainer.CLASSPATH_CONTAINER_PATH, null,
                    null, false));
        }
        
        // also remove any grails source folders that have the grails classpath attribute
        for (Iterator<IClasspathEntry> entryIter = entries.iterator(); entryIter.hasNext();) {
            IClasspathEntry entry = entryIter.next();
            if (GrailsResourceUtil.isGrailsClasspathEntry(entry) && !GrailsResourceUtil.hasClasspathAttribute(entry, IClasspathManager.POMDERIVED_ATTRIBUTE)) {
                entryIter.remove();
            }
        }            
        javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), monitor);
        super.configure(request, monitor);
    }

    public void configureRawClasspath(ProjectConfigurationRequest request,
            IClasspathDescriptor classpath, IProgressMonitor monitor)
            throws CoreException {
        IProject project = request.getProject();
        GrailsCommandUtils.ensureNaturesAndBuilders(project);
        IJavaProject javaProject = JavaCore.create(project);

        // make sure that we have a dependency file
        String descriptorName = GrailsClasspathUtils.getDependencyDescriptorName(project);
        File f = new File(descriptorName);
        if (!f.exists()) {
            throw new CoreException(new Status(IStatus.ERROR, GrailsMavenActivator.PLUGIN_ID, "Could not find the grails dependency file." +
            		" This probably means that there is a bad dependency in the pom file.", new Exception()));
        }
        
        // ensure that the dependency and plugin data is forgotten
        GrailsCore.get().connect(project, PerProjectDependencyDataCache.class).refreshData();
        GrailsCore.get().connect(project, PerProjectPluginCache.class).refreshDependencyCache();
        
        // We now know that all of the dependency information is up to date
        // can't call refresh dependencies directly since that will mess up the source folders
        // source folders must be seen as coming from maven, not grails-ide
        SourceFolderJob folderJob = new SourceFolderJob(javaProject);
        List<IClasspathEntry> sourceEntries = folderJob.findSourceEntries(monitor);
        for (IClasspathEntry entry : sourceEntries) {
            classpath.addEntry(entry);
        }
        folderJob.fixCharSets(monitor);
        
        GrailsCommandUtils.deleteOutOfSynchPlugins(project);
        
        // we can ask GrailsClasspathContainer to refresh its dependencies.
        GrailsClasspathContainer container = GrailsClasspathUtils.getClasspathContainer(javaProject);
        // reparse classpath entries from dependencies file on next request
        if (container != null) {
            container.invalidate();
            container.getClasspathEntries();
        }
    }
    
    @Override
    protected void addJavaProjectOptions(Map<String, String> options,
            ProjectConfigurationRequest request, IProgressMonitor monitor)
            throws CoreException {
        super.addJavaProjectOptions(options, request, monitor);
        
        // hard code for now
        // not finding the values in maven-compiler-plugin
        options.put(JavaCore.COMPILER_SOURCE, "1.6");
        options.put(JavaCore.COMPILER_COMPLIANCE, "1.6");
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.6");
    }
    
    @Override
    public boolean hasConfigurationChanged(IMavenProjectFacade newFacade,
            ILifecycleMappingConfiguration oldProjectConfiguration,
            MojoExecutionKey key, IProgressMonitor monitor) {
        IProject project = newFacade.getProject();
        try {
            Xpp3Dom origOldConfiguration = oldProjectConfiguration == null ? null : oldProjectConfiguration.getMojoExecutionConfiguration(key);
            Xpp3Dom oldConfiguration = augmentConfiguration(project, origOldConfiguration);
            
            MojoExecution mojoExecution = newFacade.getMojoExecution(key, monitor);
            Xpp3Dom origNewConfigration = mojoExecution == null ? null : new Xpp3Dom(mojoExecution.getConfiguration());
            Xpp3Dom newConfigration = augmentConfiguration(project, origNewConfigration);
            
            return newConfigration != null ? !newConfigration.equals(oldConfiguration) : oldConfiguration != null;
          } catch(CoreException ex) {
            return true; // assume configuration update is required
          }
    }
    
    @Override
    public AbstractBuildParticipant getBuildParticipant(
            IMavenProjectFacade projectFacade, MojoExecution execution,
            IPluginExecutionMetadata executionMetadata) {
        if ("maven-compile".equals(execution.getGoal())) {
            IProject project = projectFacade.getProject();
            // delete old dependency info will be recreated
            String descriptorName = GrailsClasspathUtils.getDependencyDescriptorName(project);
            File f = new File(descriptorName);
            f.delete();
            
            Xpp3Dom newConfiguration = augmentConfiguration(project, execution.getConfiguration());
            execution.setConfiguration(newConfiguration);
        }
        return new MojoExecutionBuildParticipant(execution, false, true);
    }

    private Xpp3Dom augmentConfiguration(IProject project, Xpp3Dom orig) {
        if (orig == null) {
            return null;
        }
        Xpp3Dom configuration = new Xpp3Dom(orig);
        
        // extra classpath entry
        Xpp3Dom node = configuration.getChild("extraClasspathEntries");
        if (node == null) {
            node = new Xpp3Dom("extraClasspathEntries");
            configuration.addChild(node);
        }
        ClasspathLocalizer localizer = new ClasspathLocalizer();
        GrailsVersion version = GrailsVersion.getEclipseGrailsVersion(project);
        String pluginId = GrailsLaunchArgumentUtils.getRuntimeBundleFor(version);
        List<String> extraCp = localizer.localizeClasspath(
                new EclipsePluginClasspathEntry(pluginId, null),
                new EclipsePluginClasspathEntry("org.grails.ide.eclipse.runtime.shared", null)); 
        StringBuilder sb = new StringBuilder();
        for (String cpEntry : extraCp) {
            sb.append(cpEntry.replaceAll(",", "\\,")).append(",");
        }
        node.setValue(sb.toString());

        // build listeners
        node = configuration.getChild("grailsBuildListener");
        if (node == null) {
            node = new Xpp3Dom("grailsBuildListener");
            configuration.addChild(node);
        }
        node.setValue(SharedLaunchConstants.DependencyExtractingBuildListener_CLASS);
        
        // dependency file location
        node = configuration.getChild("dependencyFileLocation");
        if (node == null) {
            node = new Xpp3Dom("dependencyFileLocation");
            configuration.addChild(node);
        }
        node.setValue(GrailsClasspathUtils.getDependencyDescriptorName(project));
        
        
        node = configuration.getChild("fork");
        if (node == null) {
            node = new Xpp3Dom("fork");
            configuration.addChild(node);
        }
        // must run in forked mode
        node.setValue("true");
        
        return configuration;
    }

    public void configureClasspath(IMavenProjectFacade facade,
            IClasspathDescriptor classpath, IProgressMonitor monitor)
            throws CoreException {
        // do nuthin
    }
}
