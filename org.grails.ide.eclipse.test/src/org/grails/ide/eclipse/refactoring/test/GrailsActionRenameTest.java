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
package org.grails.ide.eclipse.refactoring.test;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.grails.ide.eclipse.core.model.GrailsVersion;

import org.grails.ide.eclipse.ui.internal.importfixes.GrailsProjectVersionFixer;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

/**
 * @author Kris De Volder
 * @since 2.8
 */
public class GrailsActionRenameTest extends GrailsRefactoringTest {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		//clearGrailsState();
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		GrailsProjectVersionFixer.globalAskToConvertToGrailsProjectAnswer = true;
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (undo!=null) {
			undoLastRefactoring(); //Return the test project to original state
		}
	}
	
//	public void testImportGtunes() throws Exception {
//		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
//		importZippedProject("gTunez");
//		
//		/////////////////////////////////////////////////////////////////////////////////////////////////
//		//Check a few things about this test project
//		checkImportedProject();
//	}

	public void testPerformRenameMethodRefactoring() throws Exception {
		if (GrailsVersion.MOST_RECENT.isSnapshot()) {
			//Don't run for snapshots, too much work to create test projects for moving target
			return;
		}
		if (GrailsVersion.getDefault().compareTo(GrailsVersion.V_2_0_0)>=0) {
			//This test is only valid for Grails 2.0 where controller actions are methods 
			
			importZippedProject("gTunez");
			checkImportedProject();

			createResource(project, "grails-app/controllers/gtunez/ExtraController.groovy", 
					"package gtunez\n" + 
					"\n" + 
					"class ExtraController {\n" + 
					"\n" + 
					"    def index() { \n" + 
					"		redirect(action:\"list\")\n" + 
					"	}\n" + 
					"	\n" + 
					"	def list() {\n" + 
					"		redirect(action:\"list\", controller: \"song\")\n" + 
					"	}\n" + 
					"}\n");

			StsTestUtil.assertNoErrors(project); // This forces a build as well...
			
			String oldActionName = "list";
			String newActionName = "catalog";

			IType controller = getType("gtunez.SongController");
			IMethod target = getMethod(controller, "list");
			//		controller.getMethod(oldActionName, new String[] {});
			assertTrue("Method doesn't exist: ", target.exists());

			RenameMethodProcessor processor = new RenameVirtualMethodProcessor(target);
			processor.setNewElementName(newActionName);
			RenameRefactoring refactoring = new RenameRefactoring(processor);

			RefactoringStatus status = performRefactoring(refactoring, true, false);
			assertOK(status);

			// Now check whether the all the changes we think are supposed to happen did happen.

			//The gsp file was renamed?
			assertFileDeleted("/gTunez/grails-app/views/song/"+oldActionName+".gsp");
			assertFile("/gTunez/grails-app/views/song/"+newActionName+".gsp"); //TODO: contents of the file!

			assertFile("/gTunez/grails-app/controllers/gtunez/ExtraController.groovy",
					"package gtunez\n" + 
							"\n" + 
							"class ExtraController {\n" + 
							"\n" + 
							"    def index() { \n" + 
							"		redirect(action:\"list\")\n" + 
							"	}\n" + 
							"	\n" + 
							"	def list() {\n" + 
							"		redirect(action:\"catalog\", controller: \"song\")\n" + 
							"	}\n" + 
					"}\n");

			String songControllerPath = "/gTunez/grails-app/controllers/gtunez/SongController.groovy";
			String songController = getContents(ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(songControllerPath)));
			songController = songController.replace('"'+oldActionName+'"', '"'+newActionName+'"');
			songController = songController.replace("def "+oldActionName, "def "+newActionName);

			assertFile(songControllerPath,
					songController);
		}
	}
	
	private IMethod getMethod(IType controller, String name) throws JavaModelException {
		IMethod[] methods = controller.getMethods();
		for (IMethod m : methods) {
			if (m.getElementName().equals(name)) {
				return m;
			}
		}
		return null;
	}

	/**
	 * It is possible to have controller without associated views. This shouldn't break the refactoring.
	 */
	public void testPerformRenameActionWithoutViewRefactoring() throws Exception {
		if (GrailsVersion.MOST_RECENT.isSnapshot()) {
			//Don't run for snapshots, too much work to create test projects for moving target
			return;
		}
		importZippedProject("gTunez");
		checkImportedProject();

		createResource(project, "grails-app/controllers/gtunez/ExtraController.groovy", 
				"package gtunez\n" + 
				"\n" + 
				"class ExtraController {\n" + 
				"\n" + 
				"    def index() { \n" + 
				"		redirect(action:\"list\")\n" + 
				"	}\n" + 
				"	\n" + 
				"	def list() {\n" + 
				"		redirect(action:\"list\", controller: \"song\")\n" + 
				"	}\n" + 
				"}\n");

		StsTestUtil.assertNoErrors(project); // This forces a build as well...

		String oldActionName = "list";
		String newActionName = "catalog";

		IType controller = getType("gtunez.ExtraController");
		IMethod target = controller.getMethod(oldActionName, new String[] {});

		RenameMethodProcessor processor = new RenameVirtualMethodProcessor(target);
		processor.setNewElementName(newActionName);
		RenameRefactoring refactoring = new RenameRefactoring(processor);
		
		RefactoringStatus status = performRefactoring(refactoring, true, false);
		assertOK(status);

		//Check changes
		
		assertFile("/gTunez/grails-app/controllers/gtunez/ExtraController.groovy",
				"package gtunez\n" + 
				"\n" + 
				"class ExtraController {\n" + 
				"\n" + 
				"    def index() { \n" + 
				"		redirect(action:\"catalog\")\n" + 
				"	}\n" + 
				"	\n" + 
				"	def catalog() {\n" + 
				"		redirect(action:\"list\", controller: \"song\")\n" + 
				"	}\n" + 
				"}\n");
		
		String songControllerPath = "/gTunez/grails-app/controllers/gtunez/SongController.groovy";
		String songController = getContents(ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(songControllerPath)));
		
		assertFile(songControllerPath,
				songController);
	}
	
	/**
	 * This test uses actions represented as fields (1.3.x style). To see if these actions also get
	 * visited and renamed properly.
	 */
	public void testPerformFieldActionRefactoring() throws Exception {
		if (GrailsVersion.MOST_RECENT.isSnapshot()) {
			//Don't run for snapshots, too much work to create test projects for moving target
			return;
		}
		importZippedProject("gTunez");
		checkImportedProject();

		createResource(project, "grails-app/controllers/gtunez/ExtraController.groovy", 
				"package gtunez\n" + 
				"\n" + 
				"class ExtraController {\n" + 
				"\n" + 
				"    def index = { \n" + 
				"		redirect(action:\"list\")\n" + 
				"	}\n" + 
				"	\n" + 
				"	def list = {\n" + 
				"		redirect(action:\"list\", controller: \"song\")\n" + 
				"	}\n" + 
				"}\n");
		
		String oldActionName = "list";
		String newActionName = "catalog";

		createResource(project, "grails-app/views/extra/"+oldActionName+".gsp", 
				"<%@ page import=\"gtunez.Song\" %>\n" + 
				"<!doctype html>\n" + 
				"<html>\n" + 
				"	<head>\n" + 
				"		<meta name=\"layout\" content=\"main\">\n" + 
				"		<g:set var=\"entityName\" value=\"${message(code: 'song.label', default: 'Banana')}\" />\n" + 
				"		<title><g:message code=\"default.list.label\" args=\"[entityName]\" /></title>\n" + 
				"	</head>\n" + 
				"	<body>\n" + 
				"		<h1>A few links:</h1>\n" +
				"		<g:link action=\"list\" id=\"1\">blah</g:link>\n" + 
				"		<g:link action=\"show\" id=\"${currentBook.id}\">blah</g:link>\n" + 
				"		<g:link controller=\"book\">Book Home</g:link>\n" + 
				"		<g:link controller=\"book\" action=\"list\">blah</g:link>\n" + 
				"		<g:link controller=\"extra\">Book Home</g:link>\n" + 
				"		<g:link controller=\"extra\" action=\"list\">blah</g:link>\n" + 
				"		<g:link controller=\"extra\" action=\"show\">blah</g:link>\n" + 
				"	</body>\n" + 
				"</html>\n");
		
		StsTestUtil.assertNoErrors(project); // This forces a build as well...
		
		IType controller = getType("gtunez.ExtraController");
		IField target = controller.getField(oldActionName);

		RenameFieldProcessor processor = new RenameFieldProcessor(target);
		processor.setNewElementName(newActionName);
		RenameRefactoring refactoring = new RenameRefactoring(processor);
		
		RefactoringStatus status = performRefactoring(refactoring, true, false);
		assertOK(status);

		// Now check whether the changes we think are supposed to happen did happen.

		//The gsp file was renamed?
		assertFileDeleted("/gTunez/grails-app/views/extra/"+oldActionName+".gsp");
		assertFile("/gTunez/grails-app/views/extra/"+newActionName+".gsp",
				"<%@ page import=\"gtunez.Song\" %>\n" + 
				"<!doctype html>\n" + 
				"<html>\n" + 
				"	<head>\n" + 
				"		<meta name=\"layout\" content=\"main\">\n" + 
				"		<g:set var=\"entityName\" value=\"${message(code: 'song.label', default: 'Banana')}\" />\n" + 
				"		<title><g:message code=\"default.list.label\" args=\"[entityName]\" /></title>\n" + 
				"	</head>\n" + 
				"	<body>\n" + 
				"		<h1>A few links:</h1>\n" +
				"		<g:link action=\"catalog\" id=\"1\">blah</g:link>\n" + 
				"		<g:link action=\"show\" id=\"${currentBook.id}\">blah</g:link>\n" + 
				"		<g:link controller=\"book\">Book Home</g:link>\n" + 
				"		<g:link controller=\"book\" action=\"list\">blah</g:link>\n" + 
				"		<g:link controller=\"extra\">Book Home</g:link>\n" + 
				"		<g:link controller=\"extra\" action=\"catalog\">blah</g:link>\n" + 
				"		<g:link controller=\"extra\" action=\"show\">blah</g:link>\n" + 
				"	</body>\n" + 
				"</html>\n");

		assertFile("/gTunez/grails-app/controllers/gtunez/ExtraController.groovy",
				"package gtunez\n" + 
				"\n" + 
				"class ExtraController {\n" + 
				"\n" + 
				"    def index = { \n" + 
				"		redirect(action:\"catalog\")\n" + 
				"	}\n" + 
				"	\n" + 
				"	def catalog = {\n" + 
				"		redirect(action:\"list\", controller: \"song\")\n" + 
				"	}\n" + 
				"}\n");		
		String songControllerPath = "/gTunez/grails-app/controllers/gtunez/SongController.groovy";
		String songController = getContents(ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(songControllerPath)));
		//songController has only references to its own actions so renaming an action in the extra controller should leave it unchanged
		assertFile(songControllerPath,
				songController); 
	}
	
	private void assertOK(RefactoringStatus status) {
		if (!status.isOK()) {
			fail(status.getEntryWithHighestSeverity().getMessage());
		}
	}

	/**
	 * Line-based version of junit.framework.Assert.assertEquals(String, String)
	 * without considering line delimiters.
	 * @param expected the expected value
	 * @param actual the actual value
	 */
	public static void assertEqualLines(String expected, String actual) {
		assertEqualLines("", expected, actual);
	}
	
	
	/**
	 * Line-based version of junit.framework.Assert.assertEquals(String, String, String)
	 * without considering line delimiters.
	 * @param message the message
	 * @param expected the expected value
	 * @param actual the actual value
	 */
	public static void assertEqualLines(String message, String expected, String actual) {
		String[] expectedLines= Strings.convertIntoLines(expected);
		String[] actualLines= Strings.convertIntoLines(actual);

		String expected2= (expectedLines == null ? null : Strings.concatenate(expectedLines, "\n"));
		String actual2= (actualLines == null ? null : Strings.concatenate(actualLines, "\n"));
		assertEquals(message, expected2, actual2);
	}
}
