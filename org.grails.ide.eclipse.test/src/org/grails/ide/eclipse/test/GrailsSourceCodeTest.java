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
package org.grails.ide.eclipse.test;

import junit.framework.AssertionFailedError;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.commands.GroovyCompilerVersionCheck;
import org.grails.ide.eclipse.core.model.GrailsVersion;

import org.grails.ide.eclipse.commands.test.AbstractCommandTest;
import org.grails.ide.eclipse.ui.internal.actions.DownloadSourcesActionDelegate;
import org.grails.ide.eclipse.ui.internal.importfixes.GrailsProjectVersionFixer;

/**
 * Tests related to checking validity of source attachements in Grails
 * classpath container jars.
 * 
 * @author Kris De Volder
 * @since 2.7
 */
public class GrailsSourceCodeTest extends AbstractCommandTest {
	
	private static String emptyProjectName(GrailsVersion version) {
		return "emptyGrailsProject"+version;
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		GroovyCompilerVersionCheck.testMode();
		GrailsProjectVersionFixer.globalAskToUpgradeAnswer = false;
		GrailsProjectVersionFixer.globalAskToConfigureAnswer = true;
	}
	
	@Override
	protected void tearDown() throws Exception {
	    super.tearDown();
	}
	
	///////// test 'templates' which require some test parameters for running
	
	/**
	 * The source code for type 'ApplicationTagLib' is important for GSP support. This test
	 * checks whether source code is available for it.
	 */
	public void doTestApplicationTagLib(GrailsVersion version) throws Exception {
		String typeName = "org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib";
		doTestType(version, typeName, "@author Graeme Rocher");
	}

	private void doTestType(GrailsVersion version, String typeName, String expectedSnippet) throws Exception {
		ensureDefaultGrailsVersion(version);
		project = ensureProject(emptyProjectName(version));
		IJavaProject javaProject = JavaCore.create(project);
		
		IType type = javaProject.findType(typeName);
		assertNotNull("Type not found on classpath: "+typeName, type);
		
		IClassFile classFile = type.getClassFile();
		assertNotNull("Couldn't obtain .class file for type: "+type, classFile);
		
		IBuffer sourceCode = classFile.getBuffer();
		assertNotNull("Couldn't obtain buffer (sourceCode) for .class file: "+classFile, sourceCode);
		try {
			System.out.println(sourceCode.getContents());

			assertContains(expectedSnippet, sourceCode.getContents());
		} finally {
			sourceCode.close();
		}
	}
	
	/////// Concrete tests, with parameters filled in
	
	//The tests below are commented out, they are known to fail for Grails 1.4.M1.
	// Source code is no longer packaged with distribution?
	
//	public void testApplicationTagLib14M1() throws Exception {
//		//This class comes from grails-plugin-gsp-1.4.0.M1.jar
//		doTestApplicationTagLib(GrailsVersion.V_1_4_0_M1);
//	}
//
//	public void testSTS1778ScriptSources14M1() throws Exception {
//		doTestType(GrailsVersion.V_1_4_0_M1, "CreateController");
//	}
	
	public void testApplicationTagLib() throws Exception {
		doTestApplicationTagLib(GrailsVersion.MOST_RECENT);
	}
	
	public void testSTS1778ScriptSources() throws Exception {
		doTestType(GrailsVersion.MOST_RECENT, "CreateController", "@author Graeme Rocher");
	}
	
	public void testBootstrapSources() throws Exception {
		//Have source code for a type that is from grails-bootstrap.jar?
		doTestType(GrailsVersion.MOST_RECENT, "grails.util.GrailsNameUtils", "Licensed under the Apache License");
	}
	
	public void testGrails20SourceAttachements() throws Exception {
		// Diabled for now: not working in Grails 2.2.1 *and* Grails 2.2.2 and 2.2.3
		// See http://jira.grails.org/browse/GRAILS-9940
		if (GrailsVersion.MOST_RECENT.compareTo(GrailsVersion.V_2_2_3)>0) {
			try {
				doTestType(GrailsVersion.MOST_RECENT, "org.springframework.uaa.client.UaaService", "@author Ben Alex");
				fail("Source code already available beforehand? That's not a good test then!");
			} catch (AssertionFailedError e) {
				assertTrue(e.getMessage().contains("Couldn't obtain buffer"));
			}
			DownloadSourcesActionDelegate.doit(project, new NullProgressMonitor());
			doTestType(GrailsVersion.MOST_RECENT, "org.springframework.uaa.client.UaaService", "@author Ben Alex");
		}
	}

	public void testApplicationTagLib137() throws Exception {
		//clearGrailsState(); //Workaround for http://jira.grails.org/browse/GRAILS-7655 (ivy cache corruption)
	    doTestApplicationTagLib(GrailsVersion.V_1_3_7);
	}
	
}
