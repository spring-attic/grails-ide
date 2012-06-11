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
package org.grails.ide.eclipse.search.test;


import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.search.QuerySpecification;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

import org.grails.ide.eclipse.search.AbstractQueryParticipant;
import org.grails.ide.eclipse.search.SearchUtil;
import org.grails.ide.eclipse.search.controller.ControllerTypeQueryParticipant;

/**
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class ControllerTypeQueryParticipantTests extends AbstractGrailsSearchParticipantTest {
	
	protected IProject project;
	protected IJavaProject javaProject;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		project = ensureProject(TEST_PROJECT_NAME);
		javaProject = JavaCore.create(project);
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	/**
	 * Basic test, search for a simple controller type, but there are no actual references in the project that
	 * the participant is supposed to find.
	 */
	public void testNoResultsSearch() throws Exception {
		String controllerClassName = "SongController";
		String contents = "package "+PACKAGE_NAME+"\n" + 
		"\n" + 
		"class "+controllerClassName+" {\n" + 
		"    def index() { }\n" + 
		"}\n";
		createTmpResource(project, "grails-app/controllers/"+PACKAGE_NAME+"/"+controllerClassName+".groovy",
				contents);
		
		IType targetType = javaProject.findType(PACKAGE_NAME+"."+controllerClassName);
		QuerySpecification query = SearchUtil.createReferencesQuery(targetType);
		AbstractQueryParticipant searchParticipant = new ControllerTypeQueryParticipant();
		
		int ticks = searchParticipant.estimateTicks(query);
		assertTrue("Ticks must be between 0 and 1000 but is "+ticks, 0 < ticks &&  ticks < 1000);
		assertMatches(searchParticipant, query);
	}
	
	/**
	 * A simple test where the controllerClass itself contains a reference to itself.
	 */
	public void testSimpleSearch() throws Exception {
		String controllerClassName = "SongController";
		String controllerLogicalName = "song";
		String contents = "package "+PACKAGE_NAME+"\n" + 
		"\n" + 
		"class "+controllerClassName+" {\n" + 
		"    def index() {\n" +
		"        redirect(controller:\""+controllerLogicalName+ "\", action: \"foo\")\n" +
		"    }\n" + 
		"}\n";
		createTmpResource(project, "grails-app/controllers/"+PACKAGE_NAME+"/"+controllerClassName+".groovy",
				contents);
		StsTestUtil.waitForAutoBuild();
		IType targetType = javaProject.findType(PACKAGE_NAME+"."+controllerClassName);
		QuerySpecification query = SearchUtil.createReferencesQuery(targetType);
		AbstractQueryParticipant searchParticipant = new ControllerTypeQueryParticipant();
		
		int ticks = searchParticipant.estimateTicks(query);
		assertTrue("Ticks must be between 0 and 1000 but is "+ticks, 0 < ticks &&  ticks < 1000);
		assertMatches(searchParticipant, query,
				methodMatch(javaProject, PACKAGE_NAME, controllerClassName, "index", "\""+controllerLogicalName+"\""));
	}
	
	public void testMultipleMatchesSearch() throws Exception {
		createTmpResource(project, "grails-app/controllers/"+PACKAGE_NAME+"/SongController.groovy",
				"package "+PACKAGE_NAME+"\n" + 
				"\n" + 
				"class SongController {\n" + 
				"    def index() {\n" +
				"        redirect(controller:\"song\", action: \"foo\")\n" +
				"    }\n" + 
				"}\n");

		createTmpResource(project, "grails-app/controllers/"+PACKAGE_NAME+"/ExtraController.groovy",
				"package "+PACKAGE_NAME+"\n" + 
				"\n" + 
				"class ExtraController {\n" + 
				"    def zip() {\n" +
				"        redirect(controller:\"song\", action: \"index\")\n" +
				"    }\n" + 
				"}\n");
		
		IType targetType = javaProject.findType(PACKAGE_NAME+".SongController");
		QuerySpecification query = SearchUtil.createReferencesQuery(targetType);
		AbstractQueryParticipant searchParticipant = new ControllerTypeQueryParticipant();
		
		int ticks = searchParticipant.estimateTicks(query);
		assertTrue("Ticks must be between 0 and 1000 but is "+ticks, 0 < ticks &&  ticks < 1000);
		assertMatches(searchParticipant, query,
				methodMatch(javaProject, PACKAGE_NAME, "SongController", "index", "\"song\""),
				methodMatch(javaProject, PACKAGE_NAME, "ExtraController", "zip", "\"song\"")
		);
	}
	
	public void testFieldMatches() throws Exception {
		createTmpResource(project, "grails-app/controllers/"+PACKAGE_NAME+"/SongController.groovy",
				"package "+PACKAGE_NAME+"\n" + 
				"\n" + 
				"class SongController {\n" + 
				"    def index = {\n" +
				"        redirect(controller:\"song\", action: \"foo\")\n" +
				"    }\n" + 
				"}\n");

		createTmpResource(project, "grails-app/controllers/"+PACKAGE_NAME+"/ExtraController.groovy",
				"package "+PACKAGE_NAME+"\n" + 
				"\n" + 
				"class ExtraController {\n" + 
				"    def zip = {\n" +
				"        redirect(controller:\"song\", action: \"index\")\n" +
				"    }\n" + 
				"}\n");
		
		StsTestUtil.assertNoErrors(project);
		
		IType targetType = javaProject.findType(PACKAGE_NAME+".SongController");
		QuerySpecification query = SearchUtil.createReferencesQuery(targetType);
		AbstractQueryParticipant searchParticipant = new ControllerTypeQueryParticipant();
		
		int ticks = searchParticipant.estimateTicks(query);
		assertTrue("Ticks must be between 0 and 1000 but is "+ticks, 0 < ticks &&  ticks < 1000);
		assertMatches(searchParticipant, query,
				fieldMatch(javaProject, PACKAGE_NAME, "SongController", "index", "\"song\""),
				fieldMatch(javaProject, PACKAGE_NAME, "ExtraController", "zip", "\"song\"")
		);
	}
	
	/**
	 * Search for a  non-controller type, in this case, the participant shouldn't participate in
	 * the search. This is indicated by having 0 from estimateTicks.
	 */
	public void testNotParticipatingInSearch() throws Exception {
		String controllerClassName = "SongController";
		String contents = "package controllertypequeryparticipanttests\n" + 
		"\n" + 
		"class "+controllerClassName+" {\n" + 
		"    def index() { }\n" + 
		"}\n";
		createTmpResource(project, "grails-app/controllers/"+PACKAGE_NAME+"/"+controllerClassName+".groovy",
				contents);
		
		IType targetType = javaProject.findType("BootStrap");
		QuerySpecification query = SearchUtil.createReferencesQuery(targetType);
		AbstractQueryParticipant searchParticipant = new ControllerTypeQueryParticipant();
		
		int ticks = searchParticipant.estimateTicks(query);
		assertEquals(0, ticks);
	}
	
}
