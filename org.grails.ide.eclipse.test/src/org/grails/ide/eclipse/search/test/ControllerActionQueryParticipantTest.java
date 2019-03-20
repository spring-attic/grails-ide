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


import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.ui.search.QuerySpecification;
import org.eclipse.text.edits.MultiTextEdit;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.util.GrailsNameUtils;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsProject;
import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.search.AbstractQueryParticipant;
import org.grails.ide.eclipse.search.SearchUtil;
import org.grails.ide.eclipse.search.action.ControllerActionQueryParticipant;
import org.grails.ide.eclipse.search.action.ControllerActionSearch;

/**
 * Test the visitor that is in charge of renaming view/action references inside of controller classes.
 * 
 * @author Kris De Volder
 * @since 2.8
 */
public class ControllerActionQueryParticipantTest extends AbstractGrailsSearchParticipantTest {
	
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
	
	/**
	 * Basic test, search for something that isn't found (i.e. there are no references to it).
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
		
		IMethod targetAction = method(javaProject, PACKAGE_NAME, controllerClassName, "index");
		QuerySpecification query = SearchUtil.createReferencesQuery(targetAction);
		AbstractQueryParticipant searchParticipant = new ControllerActionQueryParticipant();
		
		int ticks = searchParticipant.estimateTicks(query);
		assertTrue("Ticks must be between 0 and 1000 but is "+ticks, 0 < ticks &&  ticks < 1000);
		assertMatches(searchParticipant, query /*no matches*/);
	}
	
	/**
	 * Test a search where the participant is not expected to participate (i.e. the target type
	 * is not actually a controller.
	 * @throws Exception 
	 */
	public void testNotParticipating() throws Exception {
		createTmpResource(project, "src/groovy/foobar/Foo.groovy", 
				"package foobar;\n" +
				"class Foo {\n" +
				"    def doit() {}\n"+
				"}\n");
		
		IMethod target = method(javaProject, "foobar", "Foo", "doit");
		QuerySpecification query = SearchUtil.createReferencesQuery(target);
		
		AbstractQueryParticipant searchParticipant = new ControllerActionQueryParticipant();
		
		int ticks = searchParticipant.estimateTicks(query);
		assertEquals("Ticks", 0, ticks);
		assertMatches(searchParticipant, query /*no matches*/);
	}
	
	/**
	 * A simple test where the controllerClass itself contains an action refering an action is the same controller.
	 */
	public void testSimpleMethodSearch() throws Exception {
		String controllerClassName = "SongController";
		String controllerLogicalName = "song";
		String contents = "package "+PACKAGE_NAME+"\n" + 
		"\n" + 
		"class "+controllerClassName+" {\n" + 
		"    def index() {\n" +
		"        redirect(controller:\""+controllerLogicalName+ "\", action: \"foo\")\n" +
		"    }\n" + 
		"    def foo() {\n" +
		"    }\n"+
		"}\n";
		createTmpResource(project, "grails-app/controllers/"+PACKAGE_NAME+"/"+controllerClassName+".groovy",
				contents);
		
		IMethod targetAction = method(javaProject, PACKAGE_NAME, controllerClassName, "foo");
		QuerySpecification query = SearchUtil.createReferencesQuery(targetAction);
		AbstractQueryParticipant searchParticipant = new ControllerActionQueryParticipant();
		
		int ticks = searchParticipant.estimateTicks(query);
		assertTrue("Ticks must be between 0 and 1000 but is "+ticks, 0 < ticks &&  ticks < 1000);
		assertMatches(searchParticipant, query,
				methodMatch(javaProject, PACKAGE_NAME, controllerClassName, "index", "foo"));
	}

	/**
	 * A simple test where the controllerClass itself contains an action refering an action is the same controller.
	 */
	public void testSimpleFieldSearch() throws Exception {
		String controllerClassName = "SongController";
		String controllerLogicalName = "song";
		String contents = "package "+PACKAGE_NAME+"\n" + 
		"\n" + 
		"class "+controllerClassName+" {\n" + 
		"    def index = {\n" +
		"        redirect(controller:\""+controllerLogicalName+ "\", action: \"foo\")\n" +
		"    }\n" + 
		"    def foo = {\n" +
		"    }\n"+
		"}\n";
		createTmpResource(project, "grails-app/controllers/"+PACKAGE_NAME+"/"+controllerClassName+".groovy",
				contents);
		
		IField targetAction = field(javaProject, PACKAGE_NAME, controllerClassName, "foo");
		QuerySpecification query = SearchUtil.createReferencesQuery(targetAction);
		AbstractQueryParticipant searchParticipant = new ControllerActionQueryParticipant();
		
		int ticks = searchParticipant.estimateTicks(query);
		assertTrue("Ticks must be between 0 and 1000 but is "+ticks, 0 < ticks &&  ticks < 1000);
		assertMatches(searchParticipant, query,
				fieldMatch(javaProject, PACKAGE_NAME, controllerClassName, "index", "foo"));
	}
	
	
	/**
	 * This tests the ControllerActionSearch used by the participant separately. (As used
	 * by the rename refactoring participants.
	 * 
	 * @throws Exception
	 */
	public void testControllerActionSearchSeparately() throws Exception {
		String findActionName = "create";
		String targetPath = "grails-app/controllers/"+PACKAGE_NAME+"/FooController.groovy";
		final String contents = 
				"package "+PACKAGE_NAME+"\n" + 
				"\n" + 
				"class FooController {\n" + 
				"\n" +
				"    def save = {\n" + 
				"        def bananaInstance = new Song(params)\n" + 
				"        if (bananaInstance.save(flush: true)) {\n" + 
				"            flash.message = \"${message(code: 'default.created.message', args: [message(code: 'banana.label', default: 'Banana'), bananaInstance.id])}\"\n" + 
				"            redirect(action: \"show\", id: bananaInstance.id)\n" + 
				"        }\n" + 
				"        else {\n" + 
				"            render(view: \"create\", model: [bananaInstance: bananaInstance])\n" + 
				"        }\n" + 
				"    }\n" +
				"}\n";
		createTmpResource(project, targetPath, contents);
		
		ControllerActionSearch search = new ControllerActionSearch(grailsProject, "FooController", findActionName);
		MatchInfo[] expectedMatches = new MatchInfo[] {
				fieldMatch(javaProject, PACKAGE_NAME, "FooController", "save", "\""+findActionName+"\"")
		};
		assertMatches(search, expectedMatches);
	}
	
	/**
	 * Similar to above test but initializes the search from a QuerySpecification
	 * 
	 * @throws Exception
	 */
	public void testControllerActionSearchFieldAction() throws Exception {
		String findActionName = "create";
		String targetPath = "grails-app/controllers/"+PACKAGE_NAME+"/FooController.groovy";
		final String contents = 
				"package "+PACKAGE_NAME+"\n" + 
				"\n" + 
				"class FooController {\n" + 
				"    def create = {\n" +
				"       render(\"dummy\"\n"+
				"    }\n"+
				"\n" +
				"    def save = {\n" + 
				"        def bananaInstance = new Song(params)\n" + 
				"        if (bananaInstance.save(flush: true)) {\n" + 
				"            flash.message = \"${message(code: 'default.created.message', args: [message(code: 'banana.label', default: 'Banana'), bananaInstance.id])}\"\n" + 
				"            redirect(action: \"show\", id: bananaInstance.id)\n" + 
				"        }\n" + 
				"        else {\n" + 
				"            render(view: \"create\", model: [bananaInstance: bananaInstance])\n" + 
				"        }\n" + 
				"    }\n" +
				"}\n";
		createTmpResource(project, targetPath, contents);
		
		QuerySpecification query = SearchUtil.createReferencesQuery(field(javaProject, PACKAGE_NAME, "FooController", findActionName));
		
		ControllerActionSearch search = new ControllerActionSearch(query);
		MatchInfo[] expectedMatches = new MatchInfo[] {
				fieldMatch(javaProject, PACKAGE_NAME, "FooController", "save", "\""+findActionName+"\"")
		};
		assertMatches(search, expectedMatches);
	}

	/**
	 * Similar to above test but this time the actions are methods instead of fields.
	 * 
	 * @throws Exception
	 */
	public void testControllerActionSearchMethodAction() throws Exception {
		String findActionName = "create";
		String targetPath = "grails-app/controllers/"+PACKAGE_NAME+"/FooController.groovy";
		final String contents = 
				"package "+PACKAGE_NAME+"\n" + 
				"\n" + 
				"class FooController {\n" + 
				"    def create() {\n" +
				"       render(\"dummy\")\n"+
				"    }\n"+
				"\n" +
				"    def save() {\n" + 
				"        def bananaInstance = new Song(params)\n" + 
				"        if (bananaInstance.save(flush: true)) {\n" + 
				"            flash.message = \"${message(code: 'default.created.message', args: [message(code: 'banana.label', default: 'Banana'), bananaInstance.id])}\"\n" + 
				"            redirect(action: \"show\", id: bananaInstance.id)\n" + 
				"        }\n" + 
				"        else {\n" + 
				"            render(view: \"create\", model: [bananaInstance: bananaInstance])\n" + 
				"        }\n" + 
				"    }\n" +
				"}\n";
		createTmpResource(project, targetPath, contents);
		
		QuerySpecification query = SearchUtil.createReferencesQuery(method(javaProject, PACKAGE_NAME, "FooController", findActionName));
		
		ControllerActionSearch search = new ControllerActionSearch(query);
		MatchInfo[] expectedMatches = new MatchInfo[] {
				methodMatch(javaProject, PACKAGE_NAME, "FooController", "save", "\""+findActionName+"\"")
		};
		assertMatches(search, expectedMatches);
	}
	
	
	/**
	 * If both action and controller in the call, should not replace if controller does *not* match.
	 */
	public void testRedirectControllerNotMatching() throws Exception {
		String controllerName = "foo";
		String findName = "list";
		String inSnippet = "redirect(controller:\"book\", action:\"list\")";
		String expectSnippet = "redirect(controller:\"book\", action:\"list\")";
		
		doSnippetTest(controllerName, controllerName, findName, inSnippet, expectSnippet);
	}
	
	/**
	 * If both action and controller in the call, should not replace if controller *does* match.
	 */
	public void testRedirectControllerMatching() throws Exception {
		String controllerName = "foo";
		String oldName = "list";
		String inSnippet = "redirect(controller:\"foo\", action:\"list\")";
		String expectSnippet = "redirect(controller:\"foo\", action:\"####\")";
		
		doSnippetTest(controllerName, controllerName, oldName, inSnippet, expectSnippet);
	}
	
	/**
	 * If only action in the call, should match only if action matches AND the 'context controller' matches
	 */
	public void testRedirectNoController1() throws Exception {
		doSnippetTest(
				//String contextControllerName
				"foo",
				//String controllerName
				"foo",
				//String oldName,
				"list",
				//String inSnippet
				"redirect(action:\"list\")",
				//String expectSnippet
				"redirect(action:\"####\")"
		);
	}
		
	/**
	 * If only action in the call, should match only if action matches AND the 'context controller' matches
	 */
	public void testRedirectNoController2() throws Exception {
		doSnippetTest(
				//String contextControllerName
				"foo",
				//String controllerName
				"foo",
				//String oldName,
				"golly",
				//String inSnippet
				"redirect(action:\"list\")",
				//String expectSnippet
				"redirect(action:\"list\")"
		);
	}
		
	/**
	 * If only action in the call, should match only if action matches AND the 'context controller' matches
	 */
	public void testRedirectNoController3() throws Exception {
		doSnippetTest(
				//String contextControllerName
				"someOtherplace",
				//String controllerName
				"foo",
				//String oldName,
				"golly",
				//String inSnippet
				"redirect(action:\"list\")",
				//String expectSnippet
				"redirect(action:\"list\")"
		);
	}
	
	/**
	 * Test that we don't replace "view" parameters if we are not inside the correct controller.
	 */
	public void testRenderOutOfContext1() throws Exception {
		doSnippetTest(
				//String contextControllerName
				"samePlace",
				//String controllerName
				"samePlace",
				//String oldName,
				"golly",
				//String inSnippet
				"render(view:\"golly\")",
				//String expectSnippet
				"render(view:\"#####\")"
		);
	}
		
	/**
	 * Test that we don't replace "view" parameters if we are not inside the correct controller.
	 */
	public void testRenderOutOfContext2() throws Exception {
		doSnippetTest(
				//String contextControllerName
				"otherPlace",
				//String controllerName
				"samePlace",
				//String oldName,
				"golly",
				//String inSnippet
				"render(view:\"golly\")",
				//String expectSnippet
				"render(view:\"golly\")"
		);
	}
	
	/**
	 * Expect snippet should be a copy of 'inSnippet' but with the expected matches replaced by a sequence of #.
	 * The expectSnippet will be searched for hashes to determine the expected matches for the search.
	 */
	public void doSnippetTest(String contextControllerName, String targetControllerName, String oldActionName, String inSnippet, String expectSnippet)
			throws CoreException {
		//This a 'test template' to do tests to see if these kinds of snippets are handled corretctly
		//		redirect(url:"http://www.blogjava.net/BlueSUN") (NOT supported)
		//		redirect(action:"show")
		//		redirect(controller:"book",action:"list")
		//		redirect(action:"show",id:4, params:[author:"Stephen King"])
		//		redirect(controller: "book", action: "show", fragment: "profile")
		
		String contextControllerClassName = GrailsNameUtils.getClassName(contextControllerName, "Controller");
		String targetControllerClassName = GrailsNameUtils.getClassName(targetControllerName, "Controller");
		String contextControllerPath = "grails-app/controllers/"+PACKAGE_NAME+"/"+contextControllerClassName+".groovy";
		final String template = 
				"package gtunez\n" + 
				"\n" + 
				"class "+contextControllerClassName+" {\n" + 
				"\n" +
				"    def anAction = {\n" + 
				"        ***\n" +
				"    }\n" +
				"}\n";
		createTmpResource(project, contextControllerPath,
				template.replace("***", inSnippet));
		
		ControllerActionSearch search = new ControllerActionSearch(grailsProject, targetControllerClassName, oldActionName);
		assertMatches(search, determineExpectedMatches(field(javaProject, PACKAGE_NAME, contextControllerClassName, "anAction"), template, expectSnippet));
	}

	private static IMethod method(IJavaProject javaProject, String pkgName, String clsName, String methodName) throws JavaModelException {
		IType type = javaProject.findType(pkgName+"."+clsName);
		IMethod method = null;
		for (IMethod m : type.getMethods()) {
			if (m.getElementName().equals(methodName)) {
				if (method!=null) {
					fail("Multiple methods with name '"+methodName+"' in '"+clsName+"'");
				} else {
					method = m;
				}
			}
		}
		if (method == null) {
			fail("No method with name '"+methodName+"' in '"+clsName+"'");
		}
		return method;
	}
 
	

//	public void testRenameCreate() throws Exception {
//		String oldName = "create";
//		doTestSearch(oldName);
//	}
//	
//	public void testRenameList() throws Exception {
//		String oldName = "list";
//		doTestSearch(oldName);
//	}

//	private void doTestRename(String oldName, String newName)
//			throws IOException, CoreException {
//		String targetPath = "grails-app/controllers/gtunez/SongController.groovy";
//		String contents = getContents(project.getFile(new Path(targetPath)));
//		ICompilationUnit cu = getCompilationUnit(project, targetPath);
//		ModuleNode ast = getAST(cu);
//		
//		CompilationUnitChange changes = new CompilationUnitChange("test change", cu);
//		RefactoringStatus status = new RefactoringStatus();
//		EditGeneratingCodeVisitor visitor = new GrailsViewRenamingVisitor(cu, "song", oldName, newName, changes, status);
//		visitor.visit(ast);
//		
//		assertTrue(status.isOK());
//		assertEquals(contents.replace("\""+ oldName +"\"", "\""+ newName+ "\""), changes.getPreviewContent(null));
//		assertSomeEdits(changes);
//	}

	/**
	 * Assert that a CU change set contains at least some edits. This is used to make sure that the
	 * test isn't somehow defective itself (generates and expects no edits because of typo :-)
	 */
	private void assertSomeEdits(CompilationUnitChange changes) {
		MultiTextEdit edit = (MultiTextEdit)changes.getEdit();
		assertTrue("Expected some edits but found none", edit!=null && edit.getChildrenSize()>0);
	}
	
}
