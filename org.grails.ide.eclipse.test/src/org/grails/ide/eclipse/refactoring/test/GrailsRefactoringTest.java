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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jdt.groovy.model.GroovyNature;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jdt.core.tests.compiler.regression.Requestor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.grails.ide.eclipse.commands.test.AbstractCommandTest;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.springsource.ide.eclipse.commons.frameworks.test.util.ACondition;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

/**
 * @author Kris De Volder
 * @since 2.7
 */
public abstract class GrailsRefactoringTest extends AbstractCommandTest {

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
	 * Line-based version of junit.framework.Assert.assertEquals(String, String)
	 * without considering line delimiters.
	 * @param expected the expected value
	 * @param actual the actual value
	 */
	public static void assertEqualLines(String expected, String actual) {
		assertEquals("", lineStrip(expected.trim()), lineStrip(actual.trim()));
	}

//	/**
//	 * Line-based version of junit.framework.Assert.assertEquals(String, String, String)
//	 * without considering line delimiters.
//	 * @param message the message
//	 * @param expected the expected value
//	 * @param actual the actual value
//	 */
//	public static void assertEqualLines(String message, String expected, String actual) {
////		String[] expectedLines= Strings.convertIntoLines(expected);
////		String[] actualLines= Strings.convertIntoLines(actual);
////	
////		String expected2= (expectedLines == null ? null : Strings.concatenate(expectedLines, "\n"));
////		String actual2= (actualLines == null ? null : Strings.concatenate(actualLines, "\n"));
//		assertEquals(message, expected., actual2);
//	}

	/** 
	 * Remove trailing white space from all lines in 'content'.
	 * Also removes lines that only contain whitespace.
	 **/
	private static String lineStrip(String content) {
		Pattern trailingWhiteSpace = Pattern.compile("(\\s)+$", Pattern.MULTILINE);
		Matcher matcher = trailingWhiteSpace.matcher(content);
		return matcher.replaceAll("");
	}

	protected Change undo = null; //Will be set by 'performRefactoring' if an undo is available.

	/**
	 * Checks a bunch of stuff about the imported test project.
	 * 
	 * @throws Throwable
	 */
	protected void checkImportedProject() throws Exception {
	
			//Check project config, like classpath related stuff. 
			// The config may not be right initially... but should eventually become correct as a background
			// refresh dependency job should get scheduled. 
			// ACondition
			new ACondition() {
				@Override
				public boolean test() throws Exception {
					assertJobManagerIdle(); //Wait for jobs... like Refresh dependencies to all complete.
					System.out.println("Checking project config...");
					IJavaProject javaProject = JavaCore.create(project);
					
					assertDefaultOutputFolder(javaProject);
					assertTrue(project.hasNature(JavaCore.NATURE_ID)); // Should have Java Nature at this point
					assertTrue(GroovyNature.hasGroovyNature(project)); // Should also have Groovy nature
					assertTrue(GrailsNature.isGrailsAppProject(project)); // Should look like a Grails app to grails tooling
					
					///////////////////////////////////
					// Check resolved classpath stuff
					IClasspathEntry[] classPath = javaProject.getResolvedClasspath(false);
					
					// A whole bunch of libraries should be there, check for just a few of those
					
					assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, "/jsse.jar", classPath);
					assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, "grails-core", classPath);
					assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, "grails-bootstrap", classPath);
					assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, "groovy-all", classPath);
//					assertClassPathEntry(IClasspathEntry.CPE_LIBRARY, "servlet-api", classPath);
					
	//				System.out.println(">>>Resolved classpath");
	//				for (IClasspathEntry entry : classPath) {
	//					System.out.println(kindString(entry.getEntryKind())+": "+entry.getPath());
	//				}
	//				System.out.println("<<<Resolved classpath");
					
					///////////////////////////////////
					// Check raw classpath stuff
					classPath = javaProject.getRawClasspath();
	//				for (IClasspathEntry classpathEntry : classPath) {
	//					System.out.println(kindString(classpathEntry.getEntryKind())+": "+classpathEntry.getPath());
	//				}
	
					//The usual source folders:
					assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "src/java", classPath);
					assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "src/groovy", classPath);
					assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "grails-app/conf", classPath);
					assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "grails-app/controllers", classPath);
					assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "grails-app/domain", classPath);
					assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "grails-app/services", classPath);
					assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "grails-app/taglib", classPath);
					assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "test/integration", classPath);
					assertClassPathEntry(IClasspathEntry.CPE_SOURCE, "test/unit", classPath);
	
					//The classpath containers for Java and Grails
					assertClassPathEntry(IClasspathEntry.CPE_CONTAINER, "org.eclipse.jdt.launching.JRE_CONTAINER", classPath);
					assertClassPathEntry(IClasspathEntry.CPE_CONTAINER, "org.grails.ide.eclipse.core.CLASSPATH_CONTAINER", classPath);
					
					//Installed plugins source folders
					assertDefaultPluginSourceFolders(project);
					
					System.out.println("Checking project config => OK!");
					return true;
				}
	
			}.waitFor(4*60000);
		}

	protected IType getType(String fqName) throws JavaModelException {
		IJavaProject javaProject = JavaCore.create(project);
		IType target = javaProject.findType(fqName);
		assertNotNull("Couldn't find type: "+fqName, target);
		return target;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	protected void importZippedProject(String projectName) throws Exception {
		final URL zipFileURL = getProjectZip(projectName, GrailsVersion.getDefault());
		importProject(zipFileURL, projectName);
	}

	protected void assertFile(String path, String contents) throws IOException,
			CoreException {
				IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
				assertEqualLines(contents, getContents(file));
			}
	
	public void assertFile(String path) {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
		assertTrue(file.exists());
	}
	
	public void assertFileDeleted(String path) {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
		assertFalse("File exists: "+file, file.exists());
	}

	public static void performDummySearch(IJavaElement element) throws Exception{
		new SearchEngine().searchAllTypeNames(
			null,
			SearchPattern.R_EXACT_MATCH,
			"XXXXXXXXX".toCharArray(), // make sure we search a concrete name. This is faster according to Kent
			SearchPattern.R_EXACT_MATCH,
			IJavaSearchConstants.CLASS,
			SearchEngine.createJavaSearchScope(new IJavaElement[]{element}),
			new Requestor(),
			IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
			null);
	}
	
	private static class Requestor extends TypeNameRequestor {
	}

	public static abstract class RefactoringJob extends Job {
		
		public RefactoringJob(String name) {
			super(name);
		}

		public RefactoringStatus refactoringStatus;

		protected abstract RefactoringStatus perform(IProgressMonitor mon) throws Throwable;
		
		@Override
		final protected IStatus run(IProgressMonitor mon) {
			try {
				this.refactoringStatus = perform(mon);
			} catch (Throwable e) {
				return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Problem performing refactoring", e);
			}
			if (this.refactoringStatus==null) {
				return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, "Refactoring status was not set");
			} else {
				if (!this.refactoringStatus.isOK()) {
					GrailsCoreActivator.log(refactoringStatus.toString());
					return new Status(IStatus.ERROR, GrailsCoreActivator.PLUGIN_ID, refactoringStatus.toString());
				}
			}
			return Status.OK_STATUS;
		}

	}

	
	protected final RefactoringStatus performRefactoring(final Refactoring ref, final boolean providesUndo,
			final boolean performOnFail) throws Exception {
		performDummySearch(); //Why?
		RefactoringJob job = new RefactoringJob("Perform Refactoring") {

			@Override
			protected RefactoringStatus perform(IProgressMonitor monitor) throws Throwable {
				IUndoManager undoManager= getUndoManager();
				final CreateChangeOperation create= new CreateChangeOperation(
						new CheckConditionsOperation(ref, CheckConditionsOperation.ALL_CONDITIONS),
						RefactoringStatus.FATAL);
				final PerformChangeOperation perform= new PerformChangeOperation(create);
				perform.setUndoManager(undoManager, ref.getName());
				IWorkspace workspace= ResourcesPlugin.getWorkspace();
				executePerformOperation(perform, workspace);
				RefactoringStatus status = create.getConditionCheckingStatus();
				if (!status.hasError() && !performOnFail)
					return status;
				assertTrue("Change wasn't executed", perform.changeExecuted());
				undo = perform.getUndoChange();
				if (providesUndo) {
					assertNotNull("Undo doesn't exist", undo);
					assertTrue("Undo manager is empty", undoManager.anythingToUndo());
				} else {
					assertNull("Undo manager contains undo but shouldn't", undo);
				}
				return status;
			}
		};
		waitForWorkspaceJob(job);
		assertTrue(job.getResult().isOK());
		return job.refactoringStatus;
	}
	
	private void performDummySearch() throws Exception {
		performDummySearch(JavaCore.create(project));
	}

	protected void undoLastRefactoring() throws CoreException {
		assertNotNull("No undo found", undo);
		undo.perform(new NullProgressMonitor());
	}

	protected IUndoManager getUndoManager() {
		IUndoManager undoManager= RefactoringCore.getUndoManager();
		undoManager.flush();
		return undoManager;
	}

	protected void executePerformOperation(final PerformChangeOperation perform, IWorkspace workspace)
			throws CoreException {
				workspace.run(perform, new NullProgressMonitor());
			}

	protected void deleteResource(String path) throws CoreException {
		IFile rsrc = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
		rsrc.delete(true, new NullProgressMonitor());
	}

}
