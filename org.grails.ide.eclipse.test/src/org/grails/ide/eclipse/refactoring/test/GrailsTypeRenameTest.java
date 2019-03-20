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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.grails.ide.eclipse.core.model.GrailsVersion;

import org.grails.ide.eclipse.refactoring.rename.type.GrailsTypeRenameRefactoring;
import org.grails.ide.eclipse.refactoring.rename.type.ITypeRenaming;
import org.grails.ide.eclipse.ui.internal.importfixes.GrailsProjectVersionFixer;

/**
 * @author Kris De Volder
 * @since 2.7
 */
public class GrailsTypeRenameTest extends GrailsRefactoringTest {
	
	private static class GSPFile {
		final String name;
		final String contents;
		
		public GSPFile(IFile resource) throws IOException, CoreException {
			this.name = resource.getName();
			this.contents = getContents(resource);
		}
		private GSPFile(String name, String contents) {
			this.name = name;
			this.contents = contents;
		}
		public GSPFile replace(String oldName, String newName) {
			return new GSPFile(name, contents.replace(oldName, newName));
		}
	}

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
	}
	
	public void testImportGtunes() throws Exception {
		if (GrailsVersion.MOST_RECENT.isSnapshot()) {
			//Don't run for snapshots, too much work to create test projects for moving target
			return;
		}
		importZippedProject("gTunez");
		
		/////////////////////////////////////////////////////////////////////////////////////////////////
		//Check a few things about this test project
		checkImportedProject();
	}

	public void testRelatedTypeDiscoveryFromDomainType() throws Exception {
		if (GrailsVersion.MOST_RECENT.isSnapshot()) {
			//Don't run for snapshots, too much work to create test projects for moving target
			return;
		}
		importZippedProject("gTunez");
		checkImportedProject();
		
		IType target = getType("gtunez.Song");
		GrailsTypeRenameRefactoring refactoring = new GrailsTypeRenameRefactoring(target);
		
		Collection<ITypeRenaming> extras = refactoring.getExtraRenamingsComputer().getExtraRenamings(new NullProgressMonitor());
		assertRenamings(extras,
				"gtunez.SongTests => SongTests",
				"gtunez.SongController => SongController",
				"gtunez.SongControllerTests => SongControllerTests",
				"gtunez.SongService => SongService",
				"gtunez.SongServiceTests => SongServiceTests",
				"gtunez.SongTagLib => SongTagLib",
				"gtunez.SongTagLibTests => SongTagLibTests"
		);
		
		refactoring.setNewName("Banana");
		extras = Arrays.asList(refactoring.getChosenAdditionalRenamings()); 
		
		assertRenamings(extras,
				"gtunez.SongTests => BananaTests",
				"gtunez.SongController => BananaController",
				"gtunez.SongControllerTests => BananaControllerTests",
				"gtunez.SongService => BananaService",
				"gtunez.SongServiceTests => BananaServiceTests",
				"gtunez.SongTagLib => BananaTagLib",
				"gtunez.SongTagLibTests => BananaTagLibTests"
		);
		
		assertTrue("update gsps", refactoring.getUpdateGSPs());
		assertTrue("update services", refactoring.getUpdateServiceRefs());
		
		deleteResource("/gTunez/test/unit/gtunez/SongTagLibTests.groovy");
		refactoring.setNewName("Coconut");
		extras = Arrays.asList(refactoring.getChosenAdditionalRenamings()); 
		assertRenamings(extras,
				"gtunez.SongTests => CoconutTests",
				"gtunez.SongController => CoconutController",
				"gtunez.SongControllerTests => CoconutControllerTests",
				"gtunez.SongService => CoconutService",
				"gtunez.SongServiceTests => CoconutServiceTests",
				"gtunez.SongTagLib => CoconutTagLib"
				//"gtunez.SongTagLibTests => CoconutTagLibTests" should be DROPPED: it was deleted!
		);
	}
	
	public void testPerformRefactoring() throws Exception {
		if (GrailsVersion.MOST_RECENT.isSnapshot()) {
			//Don't run for snapshots, too much work to create test projects for moving target
			return;
		}
		importZippedProject("gTunez");
		checkImportedProject();
		
		IType target = getType("gtunez.Song");
		GrailsTypeRenameRefactoring refactoring = new GrailsTypeRenameRefactoring(target);
		
		refactoring.setNewName("Banana");
		
		IFolder oldGspFolder = project.getFolder("grails-app/views/song");
		assertTrue(oldGspFolder.exists()); //Exists before the refactoring
		List<GSPFile> gspFiles = getGSPFiles(oldGspFolder);
		assertFalse(gspFiles.isEmpty()); //Should be some there or its not much of a test!
		
		String extraControllerContents = 
				"package gtunez\n" + 
				"\n" + 
				"class ExtraController {\n" + 
				"    def index() { \n" + 
				"		redirect(controller: \"song\", action: \"index\")\n" + 
				"	}\n" + 
				"}";
		createResource(project, "grails-app/controllers/gtunez/ExtraController.groovy", 
				extraControllerContents);
		
		RefactoringStatus status = performRefactoring(refactoring, true, false);
		assertOK(status);
		
		// Now check whether the all the changes we think are supposed to happen did happen.
		
		//Check the files have moved and the types renamed.
		assertRenamingsPerformed("Song", "Banana", 
				"gtunez.Song",
				"gtunez.SongTests",
				"gtunez.SongController",
				"gtunez.SongControllerTests",
				"gtunez.SongService",
				"gtunez.SongServiceTests",
				"gtunez.SongTagLib",
				"gtunez.SongTagLibTests"
		);
		
		IFolder newGspFolder = project.getFolder("grails-app/views/banana");
		assertRenamedGSPFiles(newGspFolder, gspFiles, "Song", "Banana" );
		
		//Checking a few of the more interesting files... but not all of them
		
		assertFile("/gTunez/grails-app/domain/gtunez/Banana.groovy",
				"package gtunez\n" + 
				"\n" + 
				"class Banana {\n" + 
				"	\n" + 
				"	static Banana example = null\n" + 
				"	static BananaController myController\n" + 
				"	\n" + 
				"	def bananaService\n" + 
				"	\n" + 
				"    static constraints = {\n" + 
				"    }\n" + 
				"	\n" + 
				"	String title\n" + 
				"	String genre\n" + 
				"	\n" + 
				"	def play() {\n" + 
				"		bananaService.play(this)\n" + 
				"	} \n" + 
				"}\n");
		
		assertFile("/gTunez/grails-app/conf/BootStrap.groovy",
				"class BootStrap {\n" + 
				"\n" + 
				"	def bananaService\n" + 
				"\n" + 
				"    def init = { servletContext ->\n" + 
				"		bananaService.start()\n" + 
				"    }\n" + 
				"    def destroy = {\n" + 
				"		bananaService.stop()\n" + 
				"    }\n" + 
				"}\n");
		
		assertFile("/gTunez/grails-app/controllers/gtunez/ExtraController.groovy",
				extraControllerContents.replace(
						'"'+target.getElementName().toLowerCase()+'"', 
						'"'+refactoring.getNewName().toLowerCase()+'"'));
		
	}
		
	private void assertRenamedGSPFiles(IFolder newGspFolder, List<GSPFile> oldGspFiles, String oldName, String newName) throws IOException, CoreException {
		for (GSPFile oldGspFile : oldGspFiles) {
			GSPFile expectedGspFile = oldGspFile.replace(oldName, newName)
					//Exceptions that we don't expect to be renamed:
					.replace("default: '"+newName+"'", "default: '"+oldName+"'");
			GSPFile newGspFile = new GSPFile(newGspFolder.getFile(new Path(oldGspFile.name)));
			assertEqualLines(expectedGspFile.contents, newGspFile.contents);
		}
	}

	private List<GSPFile> getGSPFiles(final IFolder oldGspFolder) throws CoreException {
		final List<GSPFile> result = new ArrayList<GrailsTypeRenameTest.GSPFile>();
		oldGspFolder.accept(new IResourceVisitor() {
			public boolean visit(IResource resource) throws CoreException {
				if (resource.getName().endsWith(".gsp")) {
					try {
						result.add(new GSPFile((IFile)resource));
					} catch (IOException e) {
						throw new Error(e);
					}
				}
				return resource.equals(oldGspFolder);
			}
		});
		return result;
	}

	/**
	 * Checks whether Files and Types were renamed as expected.
	 */
	private void assertRenamingsPerformed(String oldName, String newName, String... fqOldNames) throws JavaModelException {
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IJavaProject jp = JavaCore.create(project);
		for (String fqOldName : fqOldNames) {
			assertTrue(fqOldName.contains(oldName));
			String fqNewName = fqOldName.replace(oldName, newName);
			IType newType = jp.findType(fqNewName);
			assertNotNull(fqNewName, newType);
			assertNull(fqOldName, jp.findType(fqOldName));
			IResource newRsrc = newType.getCompilationUnit().getCorrespondingResource();
			IPath newPath = newRsrc.getFullPath();
			assertTrue(newRsrc instanceof IFile);
			assertTrue(newRsrc.exists());
			IPath oldPath = newPath.removeLastSegments(1).append(newPath.segment(newPath.segmentCount()-1).replace(newName, oldName));
			IFile oldRsrc = root.getFile(oldPath);
			assertFalse(oldRsrc.exists());
		}
	}

	private void assertOK(RefactoringStatus status) {
		if (!status.isOK()) {
			fail(status.getEntryWithHighestSeverity().getMessage());
		}
	}

	private void assertRenamings(Collection<ITypeRenaming> extras, 
			String... _expected) {
		HashSet<String> expected = new HashSet<String>();
		for (String string : _expected) {
			expected.add(string);
		}
		
		StringBuffer unexpected = new StringBuffer();
		
		for (ITypeRenaming renaming : extras) {
			String label = renaming.getTarget().getFullyQualifiedName() + " => "+renaming.getNewName();
			if (expected.contains(label)) {
				expected.remove(label); //Seen it now!
			} else {
				unexpected.append(label+"\n");
			}
		}
		
		assertEquals("Unexpected renamings", "", unexpected.toString());
	
		if (!expected.isEmpty()) {
			StringBuffer missing = new StringBuffer();
			for (String string : expected) {
				missing.append(string+"\n");
			}
			fail("Missing renamings\n"+missing);
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
	
	public static String getContents(IFile file) throws IOException, CoreException {
		return getContents(file.getContents());
	}

	public static String getContents(InputStream in) throws IOException {
		BufferedReader br= new BufferedReader(new InputStreamReader(in));

		StringBuffer sb= new StringBuffer(300);
		try {
			int read= 0;
			while ((read= br.read()) != -1)
				sb.append((char) read);
		} finally {
			br.close();
		}
		return sb.toString();
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
