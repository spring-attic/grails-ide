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
package org.grails.ide.eclipse.refactoring.test;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.internal.core.refactoring.resource.RenameResourceProcessor;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

import org.grails.ide.eclipse.ui.internal.importfixes.GrailsProjectVersionFixer;

/**
 * @author Kris De Volder
 * @since 2.7
 */
public class GrailsViewRenameTest extends GrailsRefactoringTest {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		//clearGrailsState();
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);
		GrailsProjectVersionFixer.globalAskToConvertToGrailsProjectAnswer = true;
		GrailsProjectVersionFixer.globalAskToUpgradeAnswer = false;
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

	public void testPerformRefactoring() throws Exception {
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

		String oldActionName = "list";
		String newActionName = "catalog";
		
		IResource target = project.getFile(new Path("grails-app/views/song/"+oldActionName+".gsp"));
		RenameResourceProcessor processor = new RenameResourceProcessor(target);
		RenameRefactoring refactoring = new RenameRefactoring(processor);
		processor.setNewResourceName(newActionName+".gsp");
		
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
	
	/**
	 * This test uses actions represented as fields (1.3.x style). To see if these actions also get
	 * visited and renamed properly.
	 */
	public void testPerformFieldActionRefactoring() throws Exception {
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
		
		IResource target = project.getFile(new Path("grails-app/views/extra/"+oldActionName+".gsp"));
		RenameResourceProcessor processor = new RenameResourceProcessor(target);
		RenameRefactoring refactoring = new RenameRefactoring(processor);
		processor.setNewResourceName(newActionName+".gsp");
		
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
	
	/**
	 * Tests whether references to methods are being renamed automatically when a method itself is being
	 * renamed.
	 */
	public void testSTS2032MethodReferencesRenamed() throws Exception {
		if (GrailsVersion.MOST_RECENT.compareTo(GrailsVersion.V_2_0_0)>=0) {
			importZippedProject("gTunez");
			checkImportedProject();

			String contents = "package gtunez\n" + 
			"\n" + 
			"import org.junit.*\n" + 
			"import grails.test.mixin.*\n" + 
			"import javax.servlet.http.HttpServletResponse\n" + 
			"\n" + 
			"@TestFor(SongController)\n" + 
			"@Mock(Song)\n" + 
			"class SongControllerTests {\n" + 
			"    void testEdit() {\n" + 
			"        controller.edit()\n" + 
			"\n" + 
			"        assert flash.message != null\n" + 
			"        assert response.redirectedUrl == '/song/list'\n" + 
			"\n" + 
			"\n" + 
			"        def song = new Song()\n" + 
			"\n" + 
			"        // TODO: populate valid domain properties\n" + 
			"\n" + 
			"        assert song.save() != null\n" + 
			"\n" + 
			"        params.id = song.id\n" + 
			"\n" + 
			"        def model = controller.edit()\n" + 
			"\n" + 
			"        assert model.songInstance == song\n" + 
			"    }\n" +
			"}";
			
			createResource(project, "test/unit/gtunez/SongControllerTests.groovy", 
					contents);
			
			StsTestUtil.assertNoErrors(project);//Ensure project is built
			
			String oldActionName = "edit";
			String newActionName = "modify";

			IResource target = project.getFile(new Path("grails-app/views/song/"+oldActionName+".gsp"));
			RenameResourceProcessor processor = new RenameResourceProcessor(target);
			RenameRefactoring refactoring = new RenameRefactoring(processor);
			processor.setNewResourceName(newActionName+".gsp");
			
			RefactoringStatus status = performRefactoring(refactoring, true, false);
			assertOK(status);
			
			assertFile("/gTunez/test/unit/gtunez/SongControllerTests.groovy", 
					contents.replace("controller."+oldActionName, "controller."+newActionName));
		}
	}
	
	/**
	 * Tests whether references to methods are being renamed automatically when a method itself is being
	 * renamed.
	 */
	public void testSTS2032FieldReferencesRenamed() throws Exception {
		if (GrailsVersion.MOST_RECENT.compareTo(GrailsVersion.V_2_0_0)>=0) {
			importZippedProject("gTunez");
			checkImportedProject();

			String controllerContents = "package gtunez\n" + 
					"\n" + 
					"import org.springframework.dao.DataIntegrityViolationException\n" + 
					"\n" + 
					"class SongController {\n" + 
					"\n" + 
					"    def edit = {\n" + 
					"        def songInstance = Song.get(params.id)\n" + 
					"        if (!songInstance) {\n" + 
					"            flash.message = message(code: 'default.not.found.message', args: [message(code: 'song.label', default: 'Song'), params.id])\n" + 
					"            redirect(action: \"list\")\n" + 
					"            return\n" + 
					"        }\n" + 
					"\n" + 
					"        [songInstance: songInstance]\n" + 
					"    }\n" + 
					"\n" + 
					"}\n";
			createResource(project, "grails-app/controllers/gtunez/SongController.groovy", controllerContents);
			
			String contents = "package gtunez\n" + 
			"\n" + 
			"import org.junit.*\n" + 
			"import grails.test.mixin.*\n" + 
			"import javax.servlet.http.HttpServletResponse\n" + 
			"\n" + 
			"@TestFor(SongController)\n" + 
			"@Mock(Song)\n" + 
			"class SongControllerTests {\n" + 
			"    void testEdit() {\n" + 
			"        controller.edit()\n" + 
			"\n" + 
			"        assert flash.message != null\n" + 
			"        assert response.redirectedUrl == '/song/list'\n" + 
			"\n" + 
			"\n" + 
			"        def song = new Song()\n" + 
			"\n" + 
			"        // TODO: populate valid domain properties\n" + 
			"\n" + 
			"        assert song.save() != null\n" + 
			"\n" + 
			"        params.id = song.id\n" + 
			"\n" + 
			"        def model = controller.edit()\n" + 
			"\n" + 
			"        assert model.songInstance == song\n" + 
			"    }\n" +
			"}";
			
			createResource(project, "test/unit/gtunez/SongControllerTests.groovy", 
					contents);
			
			StsTestUtil.assertNoErrors(project);//Ensure project is built
			
			String oldActionName = "edit";
			String newActionName = "modify";

			IResource target = project.getFile(new Path("grails-app/views/song/"+oldActionName+".gsp"));
			RenameResourceProcessor processor = new RenameResourceProcessor(target);
			RenameRefactoring refactoring = new RenameRefactoring(processor);
			processor.setNewResourceName(newActionName+".gsp");
			
			RefactoringStatus status = performRefactoring(refactoring, true, false);
			assertOK(status);
			
			assertFile("/gTunez/test/unit/gtunez/SongControllerTests.groovy", 
					contents.replace("controller."+oldActionName, "controller."+newActionName));
		}
	}
	
	private void assertOK(RefactoringStatus status) {
		if (!status.isOK()) {
			fail(status.getEntryWithHighestSeverity().getMessage());
		}
	}
	
}
