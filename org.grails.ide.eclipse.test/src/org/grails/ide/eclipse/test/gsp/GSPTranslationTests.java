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
package org.grails.ide.eclipse.test.gsp;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.groovy.tests.search.AbstractInferencingTest;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jst.jsp.core.internal.java.IJSPTranslation;
import org.eclipse.jst.jsp.core.internal.java.JSPTranslation;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.grails.ide.eclipse.editor.gsp.translation.GSPTranslationAdapter;
import org.grails.ide.eclipse.editor.gsp.translation.GSPTranslationAdapterFactory;
import org.grails.ide.eclipse.test.MockGrailsTestProjectUtils;


/**
 * Tests to ensure that GSP translation to Groovy works as
 * expected
 * @author Andrew Eisenberg
 * @created Dec 24, 2009
 */
public class GSPTranslationTests extends AbstractInferencingTest {


    public static Test suite() {
        return buildTestSuite(GSPTranslationTests.class);
    }

    public GSPTranslationTests(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockGrailsTestProjectUtils.addGrailsNature(project);
    }
    
    public void testBasicTranslation1() throws Exception {
        checkUserCode(translateText("${text}"), "text");
    }
    public void testBasicTranslation2() throws Exception {
        checkUserCode(translateText("text"), "");
    }
    public void testBasicTranslation3() throws Exception {
        checkUserCode(translateText("${def x = 9\nx++}"), "def x = 9\nx++");
    }
    public void testTagTranslation1() throws Exception {
        checkUserCode(translateText("<g:each>${text}</g:each>"), "\nif(true) { // <g:each>\ntext\n} // </g:each>\n");
    }

    public void testTagTranslation2() throws Exception {
        checkUserCode(translateText("<g:each></g:each>"), "\nif(true) { // <g:each>\n} // </g:each>\n");
    }
    public void testTagTranslation3() throws Exception {
        checkUserCode(translateText("<g:each/>"), "\nif(true) { // <g:each/>\n} // <g:each/>\n");
    }
    public void testImport1() throws Exception {
        String translated = translateText("<%@page import=\"javax.swing.text.html.HTML\"%>\n${new HTML()}");
        checkUserCode(translated, "new HTML()\n");
        checkImportCode(translated, "import javax.swing.text.html.HTML;");
    }
    
    public void testSet() throws Exception {
        checkUserCode(translateText("<g:set var=\"entityName\" value=\"${message(code: 'person.label', default: 'Person')}\" />"), 
                "\n" +
                "if(true) { // <g:set/>\n" +
                "} // <g:set/>\n" +
                "\n" +
                "def entityName\n" +
                "message(code: 'person.label', default: 'Person')\n"
                );
    }
    
    public void testSet2() throws Exception {
        checkUserCode(translateText("<g:set var=\"entityName\" value=\"${message(code: 'person.label', default: 'Person')}\">text</g:set>"), 
                "\n" +
                "if(true) { // <g:set>\n" +
                "\n" +
                "def entityName\n" +
                "message(code: 'person.label', default: 'Person')\n" +
                "} // </g:set>\n"
                );
    }
    
    public void testDef1() throws Exception {
        checkUserCode(translateText("<g:def var=\"entityName\" value=\"${message(code: 'person.label', default: 'Person')}\" />"), "\ndef entityName\n");
    }
    public void testDef2() throws Exception {
        checkUserCode(translateText("<g:def var=\"entityName\" value=\"${message(code: 'person.label', default: 'Person')}\" ></g:def>"), "\ndef entityName\n");
    }
    
    public void testEach1() throws Exception {
        // this one is a little strange since the g:each tag has no close.  The declaration is placed outside of the if statement.
        // maybe this is wrong.  The variable definition will be available after each tag is closed.
        checkUserCode(translateText("<g:each var=\"entityName\" in=\"[1, 2]\" />"), "\n" + 
        		"if(true) { // <g:each/>\n" + 
        		"} // <g:each/>\n" + 
        		"\n" + 
        		"def entityName\n");
    }
    public void testEach2() throws Exception {
        checkUserCode(translateText("<g:each var=\"entityName\" in=\"${[1, 2]}\" ></g:each>"), "\nif(true) { // <g:each>\n\ndef entityName\n[1, 2]\n} // </g:each>\n");
    }
    
    public void testEach3() throws Exception {
        checkUserCode(translateText("<g:each in=\"${[1, 2]}\" var=\"entityName\" ></g:each>"), "\nif(true) { // <g:each>\n\ndef entityName\n[1, 2]\n} // </g:each>\n");
    }
    
    public void testIf() throws Exception {
        String translated = translateText("<g:if test=\"${flash.message}\"><div class=\"message\">${flash.message}</div></g:if>");
        
        // slight difference on 35 vs 36 +
        String expected35 = "\nwhile(true) { // <g:if>\nflash.message\nflash.message\n} // </g:if>\n";
        String expected36 = "\nif(true) { // <g:if>\nflash.message\nflash.message\n} // </g:if>\n";
        checkUserCode(translated, expected35, expected36);
    }
    
    // Hmmm...how do I check for compile problems?
    public void testProblemTranslation1() throws Exception {
//        expectingProblemsFor(unit.getResource().getFullPath(), 
//                "Problem : ...");
        checkUserCode(translateText("${new NoExist()}"), "new NoExist()");
    }
    
    public void testNestedClosure() throws Exception {
        checkUserCode(translateText("${grailsApplication.controllerClasses. sort{ it.fullName }}"), "grailsApplication.controllerClasses. sort{ it.fullName }\n");
    }
    public void testNestedClosure2() throws Exception {
        checkUserCode(translateText(
                "<img src=\"${ while(true) {}\n" + 
        		"       fdsafsd}\" /> "), 
        		" while(true) {}\n" + 
        				"       fdsafsd");
    }
    
    public void testSTS1489() throws Exception {
        checkUserCode(translateText(
                "<img src=\"${ }\" />"), " ");
    }

    /**
     * Like 'checkUserCode(String, String), but accepts multiple alternate 'expectedUserCodes'.
     * Either one of the alternates are considered acceptable.
     */
    private void checkUserCode(String actualJavaContents, String... expectedUserCode) {
    	for (int i = 0; i < expectedUserCode.length; i++) {
    		try {
    			checkUserCode(actualJavaContents, expectedUserCode[i]);
    			return; //no need to check others, we have an acceptable match
    		} catch (AssertionFailedError e) {
    			if (i+1<expectedUserCode.length) {
    				//try next alternative
    			} else {
    				throw e;
    			}
    		}
		}
    }
    
    /**
     * @param unit
     * @param expectedUserCode
     */
    private void checkUserCode(String actualJavaContents, String expectedUserCode) {
        System.out.println(actualJavaContents);
        int userCodeStart = actualJavaContents.indexOf("try {\n") + "try {\n".length();
        int userCodeEnd = actualJavaContents.lastIndexOf(" } catch (java.lang.Exception e) {} ");
        String actualUserCode = actualJavaContents.substring(userCodeStart, userCodeEnd);
        assertEquals("Expecting:\n======\n" + expectedUserCode + "\n======\nbut found:\n======\n" + actualUserCode + "\n======\n", userCodeStart, actualJavaContents.indexOf(expectedUserCode, userCodeStart));
    }
    
    private void checkImportCode(String javaContents, String importCode) {
        int importCodeStart = javaContents.indexOf("import javax.servlet.jsp.*;\n\n") + "import javax.servlet.jsp.*;\n\n".length();
        int importCodeEnd = javaContents.lastIndexOf("\npublic class ");
        assertEquals("Expecting:\n======\n" + importCode + "\n======\nbut found:\n======\n" + javaContents.substring(importCodeStart, importCodeEnd) + "\n======\n", importCodeStart, javaContents.indexOf(importCode, importCodeStart));
    }
    
    private String translateText(String content) throws Exception {
        IFile file = project.getFile(new Path("file.gsp"));
        if (file.exists()) {
            file.delete(true, null);
        }
        
        InputStream is = new ByteArrayInputStream(content.getBytes("UTF-8"));
        
        file.create(is, true, null);
        IStructuredModel model = StructuredModelManager.getModelManager().getModelForRead(file);
        try {
            IDOMModel jspModel = (IDOMModel) model;
    
            String jspSource = model.getStructuredDocument().get();
    
            assertTrue("line delimiters have been converted to Windows [CRLF]", jspSource.indexOf("\r\n") < 0);
            assertTrue("line delimiters have been converted to Mac [CR]", jspSource.indexOf("\r") < 0);
    
            if (model.getFactoryRegistry().getFactoryFor(IJSPTranslation.class) == null) {
                GSPTranslationAdapterFactory factory = new GSPTranslationAdapterFactory();
                model.getFactoryRegistry().addFactory(factory);
            }
            IDOMDocument xmlDoc = jspModel.getDocument();
            GSPTranslationAdapter translationAdapter = (GSPTranslationAdapter) xmlDoc.getAdapterFor(IJSPTranslation.class);
            JSPTranslation translation = translationAdapter.getJSPTranslation();
            
            ICompilationUnit unit = translation.getCompilationUnit();
            expectingNoProblemsFor(unit.getResource().getFullPath());
            
            return String.valueOf(((CompilationUnit) unit).getContents());
        } finally {  
            model.releaseFromRead();
        }
    }
}
