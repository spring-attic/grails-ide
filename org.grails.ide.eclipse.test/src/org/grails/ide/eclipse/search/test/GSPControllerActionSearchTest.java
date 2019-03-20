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
package org.grails.ide.eclipse.search.test;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.util.GrailsNameUtils;

import org.grails.ide.eclipse.editor.groovy.elements.ControllerClass;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.search.action.ControllerActionSearch;

/**
 * Test searching for controller actions inside GSP files.
 * 
 * @author Kris De Volder
 * @since 2.9
 */
public class GSPControllerActionSearchTest extends AbstractGrailsSearchParticipantTest {
	
	//Examples from the Grails docs relevant to the tests in this class (from https://www.grails.org/doc/latest/ref/Tags/link.html)
//	<g:link action="show" id="1">Book 1</g:link>
//	<g:link action="show" id="${currentBook.id}">${currentBook.name}</g:link>
//	<g:link controller="book">Book Home</g:link>
//	<g:link controller="book" action="list">Book List</g:link>
//	<g:link url="[action:'list',controller:'book']">Book List</g:link>
//	<g:link action="list" params="[sort:'title',order:'asc',author:currentBook.author]">
//	     Book List
//	</g:link>
//	<g:link controller="book" absolute="true">Book Home</g:link>
//	<g:link controller="book" base="https://admin.mygreatsite.com">Book Home</g:link>
//  <%= link(action:'list',controller:'book') { 'Book List' }%>
	
	
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

	public void testActionOnly() throws Exception {
		String contextContollerName = "book"; //The name of the 'current' controller in which we are 'visiting'.
		String targetControllerName = "book"; //The name of the controller who's action is being renamed
		String oldActionName = "show"; //The old name of the action
		
		String snippet =       "<g:link action=\"show\" id=\"1\">Book 1</g:link>";
		String expectSnippet = "<g:link action=\"####\" id=\"1\">Book 1</g:link>";
		
		doTestSnippet(contextContollerName, targetControllerName, oldActionName, snippet, expectSnippet);
	}
	
	public void testActionOnlyAndNonMatchingContext() throws Exception {
		String contextContollerName = "different"; //The name of the 'current' controller in which we are 'visiting'.
		String targetControllerName = "book"; //The name of the controller who's action is being renamed
		String oldActionName = "show"; //The old name of the action
		
		String snippet =       "<g:link action=\"show\" id=\"1\">Book 1</g:link>";
		String expectSnippet = "<g:link action=\"show\" id=\"1\">Book 1</g:link>";
		
		doTestSnippet(contextContollerName, targetControllerName, oldActionName, snippet, expectSnippet);
	}
	
	public void testActionAndController() throws Exception {
		String contextContollerName = "book"; //The name of the 'current' controller in which we are 'visiting'.
		String targetControllerName = "book"; //The name of the controller who's action is being renamed
		String oldActionName = "list"; //The old name of the action
		
		String snippet =       "<g:link controller=\"book\" action=\"list\">Book List</g:link>";
		String expectSnippet = "<g:link controller=\"book\" action=\"####\">Book List</g:link>";
		
		doTestSnippet(contextContollerName, targetControllerName, oldActionName, snippet, expectSnippet);
	}
	
	public void testActionAndNonMatcingController() throws Exception {
		String contextContollerName = "movy"; //The name of the 'current' controller in which we are 'visiting'.
		String targetControllerName = "book"; //The name of the controller who's action is being renamed
		String oldActionName = "list"; //The old name of the action
		
		String snippet =       "<g:link controller=\"movy\" action=\"list\">Movy List</g:link>";
		String expectSnippet = "<g:link controller=\"movy\" action=\"list\">Movy List</g:link>";
		
		doTestSnippet(contextContollerName, targetControllerName, oldActionName, snippet, expectSnippet);
	}
	
	public void doTestSnippet(String contextContollerName,
			String targetControllerName, String oldActionName,
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
		
		String targetControllerClassName = GrailsNameUtils.getClassName(targetControllerName, ControllerClass.CONTROLLER);
		ControllerActionSearch search = new ControllerActionSearch(grailsProject, targetControllerClassName, oldActionName);
		assertMatches(search, determineExpectedMatches(gspFile, template, expectSnippet));
	}
}
