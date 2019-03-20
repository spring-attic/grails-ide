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
package org.grails.ide.eclipse.test.gsp;

import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.validation.ValidationResults;
import org.eclipse.wst.validation.ValidatorMessage;
import org.eclipse.wst.validation.internal.ValOperation;
import org.eclipse.wst.validation.internal.ValType;
import org.eclipse.wst.validation.internal.ValidationRunner;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.editor.gsp.tags.PerProjectTagProvider;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

/**
 * Tests on gsps that validate content
 * @author Andrew Eisenberg
 * @since 2.5.2
 */
public class GSPValidationTests extends AbstractGSPTagsTest {

    public void testValidationOK1() throws Exception {
        String contents = "";
        validate(contents);
    }
    
    public void testValidationOK2() throws Exception {
        String contents = "<g:link />";
        validate(contents);
    }
    
    public void testValidationOK3() throws Exception {
        // standard taglibs not being found since there is no source code for the grails-web jar
        // so create one instead
        createTagLib("def select = { }");
        String contents = "<g:select from=\"['INSERT','UPDATE','DELETE']\" " +
        		"name=\"eventTypeSelected\" multiple='multiple' " +
        		"noSelection=\"${['null':'']}\" ></g:select>";
        validate(contents);
    }
    
    public void testValidationUnknownTag() throws Exception {
        String contents = "<g:x></g:x>";
        validate(contents, "Unknown tag (g:x).");
    }

    public void testValidationMissingAttribute() throws Exception {
        // standard taglibs not being found since there is no source code for the grails-web jar
        // so create one instead
        createTagLib("/** ffsad\n @attr value REQUIRED fff */\ndef actionSubmit = { }");
        String contents = "<g:actionSubmit></g:actionSubmit>";
        validate(contents, "Missing required attribute \"value\"");
    }
    
    public void testValidationContentEncoding() throws Exception {
        String contents = "<%@ page contentType=\"text/html;charset=ISO-8859-1\" >";
        //Eclipse 4.4
        validate(contents, 
        		"Start tag (<jsp:directive.page>) not closed properly, expected '>'.",
        		"Invalid location of text (>) in tag (<jsp:directive.page>)."
        );
//        if (StsTestUtil.ECLIPSE_3_6_OR_LATER) {
//            validate(contents, "Start tag (<jsp:directive.page>) not closed.");
//        } else {
//        	validate(contents, 
//        			"Start tag (<jsp:directive.page>) not closed properly, expected >.",
//        			"Invalid location of text (>) in tag (<jsp:directive.page>).");
//        }
    }
    
    public void testValidationNoPage() throws Exception {
        String contents = "<jsp:include page=\"not_here.gsp\"/>";
        validate(contents, "Fragment \"not_here.gsp\" was not found at expected path /TestProject/not_here.gsp");
    }
    
    public void testValidationCustomTag1() throws Exception {
        createTagLib("def trag = { }");
        String contents = "<g:trag></g:trag>";
        validate(contents);
    }
    
    public void testValidationCustomTag2() throws Exception {
        createTagLib("static namespace = \"temp\"\n def trag = { }");
        String contents = "<g:trag></g:trag>";
        validate(contents, "Unknown tag (g:trag).");
    }
    
    public void testValidationCustomTag3() throws Exception {
        createTagLib("static namespace = \"temp\"\n def trag = { }");
        String contents = "<temp:trag></temp:trag>";
        validate(contents);
    }
    
    public void testValidationCustomTag4() throws Exception {
        createTagLib("static namespace = \"temp\"\n/** fsadfds\n@attr value REQUIRED nuthin */\ndef trag = { }");
        String contents = "<temp:trag></temp:trag>";
        validate(contents, "Missing required attribute \"value\"");
    }
    
    public void testValidationCustomTag5() throws Exception {
        createTagLib("static namespace = \"temp\"\n/** @attr value REQUIRED nuthin */\ndef trag = { }");
        String contents = "<temp:trag value=\"\"></temp:trag>";
        validate(contents);
    }
    
    
    
    private void validate(String contents, String...expectedErrorMessages) throws Exception,
            CoreException {
        String name = "foo.gsp";
        IFile file = testProject.createFile(name, contents);
        IProject project = file.getProject();
        PerProjectTagProvider info = GrailsCore.get().connect(project, PerProjectTagProvider.class);
        info.connect(createModel(file));
        performValidationAndCheck(file, expectedErrorMessages);
        GrailsCore.get().disconnectProject(project);
    }

    private void performValidationAndCheck(IFile file,
            String... expectedErrorMessages) throws CoreException {
        ValOperation op = ValidationRunner.validate(file, ValType.Manual, new NullProgressMonitor(), true);
        ValidationResults res = op.getResults();
        ValidatorMessage[] messages = res.getMessages();
        assertEquals("Wrong number of expected result messages." + printMessages(expectedErrorMessages, messages), expectedErrorMessages.length, messages.length);
        for (String expected : expectedErrorMessages) {
            boolean found = false;
            for (ValidatorMessage message : messages) {
                if (message.getAttribute("message", "").equals(expected)) {
                    found = true;
                    break;
                }
            }
            if (! found) {
                fail("Could not find message: '" + expected + "' in" + printMessages(expectedErrorMessages, messages));
            }
        }
    }

    private String printMessages(String[] expectedErrorMessages,
            ValidatorMessage[] messages) {
        StringBuilder sb = new StringBuilder();
        for (ValidatorMessage message : messages) {
            sb.append(message.getAttributes()).append("\n");
        }
        return "\nExpected Messages:\n" + Arrays.toString(expectedErrorMessages) + "\nActual error messages:\n" + sb;
    }
}
