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
package org.grails.ide.eclipse.commands.test;

import java.io.File;

import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.classpath.GrailsClasspathUtils;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.BasePluginData;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.plugins.PluginVersion;


/**
 * Quick tests for the GrailsCommandFactory's static factory methods.
 * 
 * @author Kris De Volder
 *
 * @since 2.9
 */
public class GrailsCommandFactoryTest extends AbstractCommandTest {
	
	private static final char QUOTE = '"';

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
	}
	
	public void testCreateApp() throws Exception {
		assertEquals("create-app foo", GrailsCommandFactory.createApp("foo").getCommand());
	}

	public void testCreatePlugin() throws Exception {
		assertEquals("create-plugin foo", GrailsCommandFactory.createPlugin("foo").getCommand());
	}
	
	public void testCreatePlugin2() throws Exception {
		GrailsCommand cmd = GrailsCommandFactory.createPlugin(GrailsCoreActivator.getDefault().getInstallManager().getDefaultGrailsInstall(),"foo");		
		assertEquals("create-plugin foo", cmd.getCommand());
	}
	
	public void testCreateDomainClass() throws Exception {
		project = ensureProject("bork");
		assertEquals("create-domain-class MyDomain", GrailsCommandFactory.createDomainClass(project, "MyDomain").getCommand());
	}
	
	public void testRefreshDependencyFile() throws Exception {
		project = ensureProject("bork");
		GrailsCommand cmd = GrailsCommandFactory.refreshDependencyFile(project);
		assertEquals("compile --non-interactive", cmd.getCommand());
	}
	
	public void testDownloadSourceJars() throws Exception {
		project = ensureProject("bork");
		String sourceFileLocation = GrailsClasspathUtils.getDependencySourcesDescriptorName(project);
		if (sourceFileLocation.contains(" ")) {
			sourceFileLocation = QUOTE+sourceFileLocation+QUOTE;
		}
		GrailsCommand cmd = GrailsCommandFactory.downloadSourceJars(project);
		assertEquals("refresh-dependencies --include-source "+sourceFileLocation, cmd.getCommand());
	}
	
	public void testWar() throws Exception {
		project = ensureProject("bork");
		GrailsCommand cmd = GrailsCommandFactory.war(project, null, null);
		assertEquals("prod war", cmd.getCommand());
	}
	
	public void testDevWar() throws Exception {
		project = ensureProject("bork");
		GrailsCommand cmd = GrailsCommandFactory.war(project, "dev", null);
		assertEquals("dev war", cmd.getCommand());
	}

	public void testDevWarWithFile() throws Exception {
		project = ensureProject("bork");
		GrailsCommand cmd = GrailsCommandFactory.war(project, "dev", new File("/tmp/crud123.war"));
		assertEquals("dev war /tmp/crud123.war", cmd.getCommand());
	}
	
	public void testDevWarWithCustomEnv() throws Exception {
		project = ensureProject("bork");
		GrailsCommand cmd = GrailsCommandFactory.war(project, "somethingSpecial", new File("/tmp/crud123.war"));
		assertEquals("war /tmp/crud123.war", cmd.getCommand());
		assertEquals("somethingSpecial", cmd.getSystemProperties().get("grails.env"));
	}
	
	public void testWarWithSpaces() throws Exception {
		project = ensureProject("bork");
		GrailsCommand cmd = GrailsCommandFactory.war(project, null, new File("/tmp/has space.war"));
		assertEquals("prod war "+QUOTE+"/tmp/has space.war"+QUOTE, cmd.getCommand());
	}
	
	public void testUpgrade() throws Exception {
		project = ensureProject("bork");
		GrailsCommand cmd = GrailsCommandFactory.upgrade(project);
		assertEquals("upgrade --non-interactive", cmd.getCommand());
	}
	
	public void testUninstallPlugin() throws Exception {
		project = ensureProject("bork");
		BasePluginData data = new BasePluginData();
		data.setName("plugThat");
		data.setVersion("1.2.3");
		PluginVersion pluginVersion = new PluginVersion(data);
		GrailsCommand cmd = GrailsCommandFactory.uninstallPlugin(project, pluginVersion);
		assertEquals("uninstall-plugin --non-interactive plugThat", cmd.getCommand());
	}
	
	public void testInstallPlugin() throws Exception {
		project = ensureProject("bork");
		BasePluginData data = new BasePluginData();
		data.setName("plugThat");
		data.setVersion("1.2.3");
		PluginVersion pluginVersion = new PluginVersion(data);
		GrailsCommand cmd = GrailsCommandFactory.installPlugin(project, pluginVersion);
		assertEquals("install-plugin --non-interactive plugThat 1.2.3", cmd.getCommand());
	}
	
	public void testInstallPlugin2() throws Exception {
		project = ensureProject("bork");
		GrailsCommand cmd = GrailsCommandFactory.installPlugin(project, "plugger");
		assertEquals("install-plugin --non-interactive plugger", cmd.getCommand());
	}
	
	public void testClean() throws Exception {
		project = ensureProject("bork");
		GrailsCommand cmd = GrailsCommandFactory.clean(project);
		assertEquals("clean", cmd.getCommand());
	}
	
	public void testListPlugins() throws Exception {
		project = ensureProject("bork");
		GrailsCommand cmd = GrailsCommandFactory.listPlugins(project);
		assertEquals("list-plugins", cmd.getCommand());
	}
}
