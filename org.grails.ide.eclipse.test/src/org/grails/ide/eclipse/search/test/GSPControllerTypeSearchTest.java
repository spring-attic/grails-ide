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

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.core.model.GrailsVersion;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.search.controller.ControllerTypeSearch;

/**
 * Test searching for controller type references inside GSP files.
 * 
 * @author Kris De Volder
 * @since 2.9
 */
public class GSPControllerTypeSearchTest extends AbstractGrailsSearchParticipantTest {
	
	protected IProject project;
	protected IJavaProject javaProject;
	protected GrailsProject grailsProject;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		project = ensureProject(TEST_PROJECT_NAME);
		javaProject = JavaCore.create(project);
		grailsProject = GrailsWorkspaceCore.get().create(project);
	}

	public void testSimple() throws Exception {
		String contextContollerName = "book"; //The name of the 'current' controller in which we are 'visiting'.
		String targetControllerName = "book"; //The name of the controller who's action is being renamed
		
		String snippet =       "<g:link controller=\"book\" action=\"show\" id=\"1\">Book 1</g:link>";
		String expectSnippet = "<g:link controller=\"####\" action=\"show\" id=\"1\">Book 1</g:link>";;
		
		doTestSnippet(contextContollerName, targetControllerName, snippet, expectSnippet);
	}
	
	public void testImplicitControllerAttribute() throws Exception {
		String contextContollerName = "book"; //The name of the 'current' controller in which we are 'visiting'.
		String targetControllerName = "book"; //The name of the controller who's action is being renamed
		
		//The snippet implicitly references the "book" controller, but it shouldn't be found as a search match
		//because there is no explicit text in the glink that can be considered a 'match'.
		//or should we count it as a match anyway and highlight it in some fashion? 
		//If so, then that may cause complications for refactoring. match should be identifiable by the refactoring as not replaceable.
		String snippet =       "<g:link action=\"show\" id=\"1\">Book 1</g:link>";
		String expectSnippet = "<g:link action=\"show\" id=\"1\">Book 1</g:link>";;
		
		doTestSnippet(contextContollerName, targetControllerName, snippet, expectSnippet);
	}
	
	public void doTestSnippet(String contextContollerName,
			String targetControllerName,
			String snippet, String expectSnippet)
			throws CoreException, IOException {
		String contextActionName = "index"; //The name of the 'current' view/action in which we are visiting.
		String gspFilePath = "grails-app/views/"+contextContollerName+"/"+contextActionName+".gsp";
		final String template = 
				"<%@ page import=\"gtunez.Song\" %>\n" + 
				"<!doctype html>\n" + 
				"<html>\n" + 
				"	<head>\n" + 
				"		<meta name=\"layout\" content=\"main\">\n" + 
				"		<g:set var=\"entityName\" value=\"${message(code: 'song.label', default: 'Banana')}\" />\n" + 
				"		<title><g:message code=\"default.list.label\" args=\"[entityName]\" /></title>\n" + 
				"	</head>\n" + 
				"	<body>\n" + 
				"	  ***\n" + 
				"	</body>\n" + 
				"</html>\n";
		createTmpResource(project, gspFilePath, template.replace("***", snippet));
		IFile gspFile = project.getFile(new Path(gspFilePath));
		
		ControllerTypeSearch search = new ControllerTypeSearch(grailsProject, targetControllerName);
		assertMatches(search, determineExpectedMatches(gspFile, template, expectSnippet));
	}
}
