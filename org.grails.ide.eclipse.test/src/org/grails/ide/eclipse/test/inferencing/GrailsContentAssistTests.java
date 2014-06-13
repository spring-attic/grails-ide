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
package org.grails.ide.eclipse.test.inferencing;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.AssertionFailedError;

import org.codehaus.groovy.eclipse.codeassist.tests.CompletionTestCase;
import org.codehaus.groovy.eclipse.editor.GroovyEditor;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.tests.util.GroovyUtils;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.internal.Workbench;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.editor.groovy.elements.DomainClass;
import org.grails.ide.eclipse.test.MockGrailsTestProjectUtils;
import org.grails.ide.eclipse.test.util.GrailsTest;
import org.grails.ide.eclipse.test.util.GrailsTestUtilActivator;
import org.grails.ide.eclipse.ui.internal.importfixes.GrailsProjectVersionFixer;
import org.springsource.ide.eclipse.commons.frameworks.core.FrameworkCoreActivator;
import org.springsource.ide.eclipse.commons.frameworks.test.util.ACondition;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

/**
 * Tests content assist in grails artifact files
 * @author Andrew Eisenberg
 * @since 2.6.1
 */
public class GrailsContentAssistTests extends CompletionTestCase {

    private GrailsVersion grailsVersion;

	public GrailsContentAssistTests(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
    	GrailsTest.assertEquals(true, true); //touch the GrailsTest class force GTUA to get started. This matters mostly when running
    											// these test in isolation where otherwise GrailsVersions may not yet be configured when test starts.
        GrailsProjectVersionFixer.testMode();
        grailsVersion = GrailsVersion.MOST_RECENT;
        GrailsTest.ensureDefaultGrailsVersion(grailsVersion);
        super.setUp();
    }
    

	@Override
    protected void tearDown() throws Exception {
        // be sure to close all editors now
        try {
            ((GroovyEditor) Workbench.getInstance().getActiveWorkbenchWindow().getPages()[0].getActiveEditor()).close(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        avoidUIThreadStarvation();
        super.tearDown();
    }

    private void avoidUIThreadStarvation() {
    	try {
	    	//Suspect that this test suite is completely 'owning' the UI thread. This leads to things that need to run on it
	    	// piling up and eventually crashing the JVM with OME exception.
	    	new ACondition("Wait for Jobs") {
				@Override
				public boolean test() throws Exception {
					assertJobManagerIdle();
					return true;
				}
			}.waitFor(3 * 60 * 1000);
    	} catch (Throwable e) {
    		//Log exception but don't fail tests because of it
    		FrameworkCoreActivator.log(e);
    	}
	}

	public void testDomainClassFields1() throws Exception {
        for (String fieldName : DomainClass.staticFields) {
            checkDomainField1(fieldName);
        }
    }
    private void checkDomainField1(String fieldName) throws Exception {
        String contents = "class MyDomain {\n\n}";
        int offset = contents.indexOf('\n')+1;
        String expected = contents.replaceFirst("\n\n", "\nstatic " + fieldName + " = null\n");
        checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, fieldName, false);
    }
    
    public void testDomainClassFields2() throws Exception {
        for (String fieldName : DomainClass.staticFields) {
            checkDomainField2(fieldName);
        }
    }
    private void checkDomainField2(String fieldName) throws Exception {
        String contents = "class MyDomain {\nst\n}";
        int offset = contents.indexOf("\nst")+3;
        String expected = contents.replaceFirst("\nst\n", "\nstatic " + fieldName + " = null\n");
        checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, "static " + fieldName, false);
    }
    
    public void testDomainClassFields3() throws Exception {
        for (String fieldName : DomainClass.staticFields) {
            checkDomainField3(fieldName);
        }
    }
    private void checkDomainField3(String fieldName) throws Exception {
        String completion = "static " + fieldName.substring(0, fieldName.length()- 3);
        String contents = "class MyDomain {\n" + completion + "\n}";
        int offset = contents.indexOf(completion) + completion.length();
        String expected = contents.replaceFirst(completion, "static " + fieldName + " = null");
        checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, "static " + fieldName, false);
    }
    
    public void testServiceClassDefinition() throws Exception {
        createGrailsElement(GrailsElementKind.SERVICE_CLASS, "JustSomeService", "class JustSomeService {  }");
        String completion = "justSomeService";
        String contents = "class MyDomain {\n\n}";
        int offset = contents.indexOf('\n') + 1;
        String expected = contents.replaceFirst("\n\n", "\ndef " + completion + "\n");
        checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, completion, false);
        
        contents = "class MyDomain {\nde\n}";
        offset = contents.indexOf("\nde") + "\nde".length();
        checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, "def " + completion, false);
        
        contents = "class MyDomain {\ndef ju\n}";
        offset = contents.indexOf("\ndef ju") + "\ndef ju".length();
        checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, "def " + completion, false);
    }
    
    public void testNamedQuery1() throws Exception {
        // failing on older versions of Groovy/Grails
        if (GroovyUtils.GROOVY_LEVEL < 18) {
            return;
        }
        String contents = 
                "class MyDomain {\n" +
                "  String name\n" +
                "  static namedQueries = {\n" +
                "    firstQuery { }\n" +
                "    secondQuery { }\n" +
                "  }\n" +
                "  def x = { MyDomain. }\n" +
        		"\n}";
        String str = "MyDomain.";
        String strRegex = "MyDomain\\.";
        
        int offset = contents.indexOf(str)+str.length();
        String expected = contents.replaceFirst(strRegex, "MyDomain.firstQuery");
        checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, "firstQuery", false);
        expected = contents.replaceFirst(strRegex, "MyDomain.secondQuery");
        checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, "secondQuery", false);
    }
    
    public void testNamedQuery2() throws Exception {
        // failing on older versions of Groovy/Grails
        if (GroovyUtils.GROOVY_LEVEL < 18) {
            return;
        }
        String contents = 
                "class MyDomain {\n" +
                "  String name\n" +
                "  static namedQueries = {\n" +
                "    firstQuery { }\n" +
                "    secondQuery { }\n" +
                "  }\n" +
                "  def x = { MyDomain.firstQuery. }\n" +
                "\n}";
        String str = "firstQuery.";
        String strRegex = "firstQuery\\.";
        int offset = contents.indexOf(str)+str.length();
        String expected = contents.replaceFirst(strRegex, "firstQuery.firstQuery");
        checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, "firstQuery", false);
        expected = contents.replaceFirst(strRegex, "firstQuery.secondQuery");
        checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, "secondQuery", false);
    }
    
    public void testNamedQuery3() throws Exception {
        // failing on older versions of Groovy/Grails
        if (GroovyUtils.GROOVY_LEVEL < 18) {
            return;
        }
        String contents = 
                "class MyDomain {\n" +
                "  String name\n" +
                "  static namedQueries = {\n" +
                "    firstQuery { }\n" +
                "    secondQuery { }\n" +
                "  }\n" +
                "  def x = { MyDomain.firstQuery. }\n" +
                "\n}";
        String str = "firstQuery.";
        String strRegex = "firstQuery\\.";
        int offset = contents.indexOf(str)+str.length();
        String expected;
        
        // seems to be a bit random whether the result has a closure as a last param or not
        // so try both ways
        try {
            expected = contents.replaceFirst(strRegex, "firstQuery.list(parameterTypes) {");
            checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, "list", false);
        } catch (AssertionFailedError e) {
            expected = contents.replaceFirst(strRegex, "firstQuery.list(parameterTypes)");
            checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, "list", false);
        }
        
        // seems to be a bit random whether the result is 'null' or 'this'
        // so try both ways
        try {
            expected = contents.replaceFirst(strRegex, "firstQuery.get(null)");
            checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, "get", false);
        } catch (AssertionFailedError e) {
            expected = contents.replaceFirst(strRegex, "firstQuery.get(this)");
            checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, "get", false);
        }
        try {
            expected = contents.replaceFirst(strRegex, "firstQuery.findAllWhere");
            checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, "findAllWhere", false);
        } catch (AssertionFailedError e) {
            expected = contents.replaceFirst(strRegex, "firstQuery.findAllWhere(null)");
            checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, "findAllWhere", false);
        }
    }
    public void testNamedQuery4() throws Exception {
        String contents = 
                "class MyDomain {\n" +
                "  String name\n" +
                "  static namedQueries = {\n" +
                "    firstQuery { }\n" +
                "    secondQuery { }\n" +
                "  }\n" +
                "  def x = { MyDomain.firstQuery.findAllByName }\n" +
                "\n}";
        String str = "firstQuery.findAllByName";
        int offset = contents.indexOf(str)+str.length();
        String expected = contents.replaceFirst(str, "firstQuery.findAllByNameBetween");
        checkProposalApplication("MyDomain", contents, GrailsElementKind.DOMAIN_CLASS, expected, offset, "findAllByNameBetween", false);
    }
    protected void checkProposalApplication(String name, String contents, GrailsElementKind kind, 
            String expected, int completionOffset, String proposalName, boolean isType) throws Exception {
        
        ICompletionProposal[] proposals = createProposalsAtOffset(name, contents, kind, completionOffset);
        ICompletionProposal firstProposal = findFirstProposal(proposals, proposalName, isType);
        if (firstProposal == null) {
            fail("Expected at least one proposal, but found none");
        }
        applyProposalAndCheck(new Document(contents), firstProposal, expected);
    }
    
    @Override
    protected IPath createGenericProject() throws Exception {
        if (genericProjectExists()) {
            return env.getProject("Project").getFullPath();
        }
        IPath projectPath = super.createGenericProject();
        IProject project = env.getProject(projectPath);
        MockGrailsTestProjectUtils.mockGrailsProject(project, grailsVersion);
        
        fullBuild(projectPath);
        env.addPackageFragmentRoot(projectPath, "grails-app/services"); //$NON-NLS-1$
        env.addPackageFragmentRoot(projectPath, "grails-app/domain"); //$NON-NLS-1$
        env.addPackageFragmentRoot(projectPath, "grails-app/controllers"); //$NON-NLS-1$
        env.addPackageFragmentRoot(projectPath, "grails-app/taglib"); //$NON-NLS-1$
        env.setOutputFolder(projectPath, "bin"); //$NON-NLS-1$
        return projectPath;
    }
    
    
    protected ICompletionProposal[] createProposalsAtOffset(String name, String contents, GrailsElementKind kind,
            int completionOffset) throws Exception {
        GroovyCompilationUnit unit = createGrailsElement(kind, name, contents);
        fullBuild();
        unit.becomeWorkingCopy(null);
        
        return super.createProposalsAtOffset(unit, completionOffset);
    }
    
    protected GroovyCompilationUnit createGrailsElement(GrailsElementKind kind, String name, String contents) throws Exception {
        String path = null;
        switch (kind) {
            case DOMAIN_CLASS:
                path = "grails-app/domain";
                break;
            case CONTROLLER_CLASS:
                path = "grails-app/controllers";
                break;
            case TAGLIB_CLASS:
                path = "grails-app/taglib";
                break;
            case SERVICE_CLASS:
                path = "grails-app/services";
                break;

            default:
                fail("Invalid element kind to be created: " + kind);
                break;
        }
        IPath genericProject = createGenericProject();
        IProject project = env.getProject(genericProject);
        IPath pathToClass = project.getFile(path).getFullPath().append(name + ".groovy");
        IFile file = env.getWorkspace().getRoot().getFile(pathToClass);
        
        if (file.exists()) {
            setContents(file, contents);
        } else {
            env.addGroovyClass(pathToClass.removeLastSegments(1), "", name, contents);
        }
        return (GroovyCompilationUnit) JavaCore.create(file);
    }
    
    protected void setContents(IFile orig, String newContents) throws Exception {
        InputStream stream = new ByteArrayInputStream(newContents.getBytes());
        orig.setContents(stream, true, false, null);
    }

}
