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
package org.grails.ide.eclipse.test;

import java.util.Arrays;

import junit.framework.Test;

import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.UIPlugin;

import org.grails.ide.eclipse.editor.actions.JavaElementHyperlink;
import org.grails.ide.eclipse.editor.actions.UrlMappingHyperlinkDetector;
import org.grails.ide.eclipse.editor.actions.WorkspaceFileHyperlink;

/**
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Jan 19, 2010
 */
public class UrlMappingTests extends AbstractGrailsCoreTests {
    public static Test suite() {
        return buildTestSuite(UrlMappingTests.class);
    }
    public UrlMappingTests(String name) {
        super(name);
    }
    
    @Override
    protected void tearDown() throws Exception {
        UIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
        super.tearDown();
    }
    
    public void testControllerMapping1() throws Exception {
        GroovyCompilationUnit unit = createControllerClass("FlarController.groovy", "class FlarController { }");
        assertJaveElementHyperlink("\"flar\"",  "class UrlMappings { static mappings = {\n" +
        		"  \"/\"(controller:\"flar\")" +
        		"\n} }", unit.getTypes()[0]);
    }
    public void testControllerMapping2() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { }");
        assertNoHyperlinks("\"flar2\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(controller:\"flar2\")" +
                "\n} }");
    }
    public void testControllerMapping3() throws Exception {
        GroovyCompilationUnit unit = createControllerClass("FlarController.groovy", "class FlarController { }");
        assertJaveElementHyperlink("\"flar\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(controller:\"flar\", action:\"fsfsd\")" +
                "\n} }", unit.getTypes()[0]);
    }
    
    public void testControllerMapping4() throws Exception {
        GroovyCompilationUnit unit = createControllerClass("FlarController.groovy", "class FlarController { }");
        assertJaveElementHyperlink("\"flar\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(controller:\"flar\", view:\"fsfsd\")" +
                "\n} }", unit.getTypes()[0]);
    }
    
    public void testActionMapping1() throws Exception {
        GroovyCompilationUnit unit = createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        assertJaveElementHyperlink("\"myAction1\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(controller:\"flar\", action:\"myAction1\")" +
                "\n} }", unit.getTypes()[0].getField("myAction1"));
    }

    public void testActionMapping2() throws Exception {
        GroovyCompilationUnit unit = createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        assertJaveElementHyperlink("\"myAction1\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(action:\"myAction1\", controller:\"flar\")" +
                "\n} }", unit.getTypes()[0].getField("myAction1"));
    }
    
    public void testActionMapping3() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        assertNoHyperlinks("\"myAction2\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(action:\"myAction2\", controller:\"flar\")" +
                "\n} }");
    }
    public void testActionMapping4() throws Exception {
        GroovyCompilationUnit unit = createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        assertJaveElementHyperlink("\"myAction1\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(action:\"myAction1\", view:\"myAction1\", controller:\"flar\")" +
                "\n} }", unit.getTypes()[0].getField("myAction1"));
    }
    
    public void testViewMapping1() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        IFile file = createGsp("flar", "myAction1.gsp", "");
        assertFileHyperlink("\"myAction1\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(view:\"myAction1\", controller:\"flar\")" +
                "\n} }", file);
    }
    
    public void testViewMapping2() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        IFile file = createGsp("flar", "myAction1.gsp", "");
        assertFileHyperlink("\"/myAction1\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(view:\"/myAction1\", controller:\"flar\")" +
                "\n} }", file);
    }
    
    public void testViewMapping3() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        IFile file = createGsp("flar", "myAction1.gsp", "");
        assertFileHyperlink("\"/myAction1.gsp\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(view:\"/myAction1.gsp\", controller:\"flar\")" +
                "\n} }", file);
    }
    
    public void testViewMapping4() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        IFile file = createGsp("flar", "myAction1.gsp", "");
        assertFileHyperlink("\"myAction1.gsp\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(view:\"myAction1.gsp\", controller:\"flar\")" +
                "\n} }", file);
    }
    
    public void testViewMapping5() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        IFile file = createGsp("flar", "myAction1.gsp", "");
        assertFileHyperlink("\"flar/myAction1.gsp\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(view:\"flar/myAction1.gsp\", controller:\"flar\")" +
                "\n} }", file);
    }
    
    public void testViewMapping6() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        IFile file = createGsp("flar", "myAction1.gsp", "");
        assertFileHyperlink("\"/flar/myAction1.gsp\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(view:\"/flar/myAction1.gsp\", controller:\"flar\")" +
                "\n} }", file);
    }
    
    public void testViewMapping7() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        IFile file = createGsp("flar", "myAction1.gsp", "");
        assertFileHyperlink("\"/flar/myAction1\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(view:\"/flar/myAction1\", controller:\"flar\")" +
                "\n} }", file);
    }
    
    public void testViewMapping8() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        IFile file = createGsp("flar", "myAction1.gsp", "");
        assertFileHyperlink("\"/flar/myAction1\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(view:\"/flar/myAction1\")" +
                "\n} }", file);
    }
    
    public void testViewMapping9() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        IFile file = createGsp("flar", "myAction1.gsp", "");
        assertFileHyperlink("\"/flar/myAction1.gsp\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(view:\"/flar/myAction1.gsp\")" +
                "\n} }", file);
    }
    
    public void testViewMapping10() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        IFile file = createGsp("", "index.gsp", "");
        assertFileHyperlink("\"/index.gsp\"",  "class UrlMappings { static mappings = {\n" +
                "  \"/\"(view:\"/index.gsp\")" +
                "\n} }", file);
    }
    
    public void testClosureStyleMapping1() throws Exception {
        GroovyCompilationUnit unit = createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        createGsp("flar", "myAction1.gsp", "");
        assertJaveElementHyperlink("'flar'",  "class UrlMappings { static mappings = {\n" +
                " '/product' {\n" + 
                "        controller = 'flar'\n" + 
                "        action = 'myAction1'\n" + 
                "    }\n" + 
                "\n} }", unit.getTypes()[0]);
    }
    
    public void testClosureStyleMapping2() throws Exception {
        GroovyCompilationUnit unit = createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { } }");
        createGsp("flar", "myAction1.gsp", "");
        assertJaveElementHyperlink("'myAction1'",  "class UrlMappings { static mappings = {\n" +
                " '/product' {\n" + 
                "        controller = 'flar'\n" + 
                "        action = 'myAction1'\n" + 
                "    }\n" + 
                "\n} }", unit.getTypes()[0].getField("myAction1"));
    }
    
    public void testClosureStyleMapping3() throws Exception {
        GroovyCompilationUnit unit = createControllerClass("FlarController.groovy", "class FlarController { def myAction1() { } }");
        createGsp("flar", "myAction1.gsp", "");
        assertJaveElementHyperlink("'myAction1'",  "class UrlMappings { static mappings = {\n" +
                " '/product' {\n" + 
                "        controller = 'flar'\n" + 
                "        action = 'myAction1'\n" + 
                "    }\n" + 
                "\n} }", unit.getTypes()[0].getMethod("myAction1", new String[0]));
    }
    
    public void testClosureStyleMapping4() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1() { } }");
        IFile file = createGsp("flar", "myAction1.gsp", "");
        assertFileHyperlink("'myAction1'",  "class UrlMappings { static mappings = {\n" +
                " '/product' {\n" + 
                "        controller = 'flar'\n" + 
                "        view = 'myAction1'\n" + 
                "    }\n" + 
                "\n} }", file);
    }
    
    public void testClosureStyleMapping5() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1() { } }");
        IFile file = createGsp("flar", "myAction1.gsp", "");
        assertFileHyperlink("'/flar/myAction1.gsp'",  "class UrlMappings { static mappings = {\n" +
                " '/product' {\n" +
                "        fsfdsfadsf = 'myAction1'\n" + 
                "        view = '/flar/myAction1.gsp'\n" + 
                "    }\n" + 
                "\n} }", file);
    }
    
    public void testNamedClosureStyleMapping1() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1() { } }");
        IFile file = createGsp("flar", "myAction1.gsp", "");
        assertFileHyperlink("'/flar/myAction1.gsp'",  "class UrlMappings { static mappings = {\n" +
                " name personList: '/product' {\n" +
                "        fsfdsfadsf = 'myAction1'\n" + 
                "        view = '/flar/myAction1.gsp'\n" + 
                "    }\n" + 
                "\n} }", file);
    }
    
    public void testComplexClosure1() throws Exception {
        GroovyCompilationUnit unit = createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { }\ndef myAction2 = { }}");
        createGsp("flar", "myAction1.gsp", "");
        createGsp("flar", "myAction2.gsp", "");
        assertJaveElementHyperlink("\"myAction1\"",  "class UrlMappings { static mappings = {\n" +
                "       \"/product/$id\"(controller:\"flar\") {\n" + 
                "            action = [GET:\"myAction1\", PUT:\"myAction2\", DELETE:\"nothing\"]\n" + 
                "        }\n" + 
                "\n} }", unit.getTypes()[0].getField("myAction1"));
    }
    
    public void testComplexClosure2() throws Exception {
        GroovyCompilationUnit unit = createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { }\ndef myAction2 = { }}");
        createGsp("flar", "myAction1.gsp", "");
        createGsp("flar", "myAction2.gsp", "");
        assertJaveElementHyperlink("\"myAction2\"",  "class UrlMappings { static mappings = {\n" +
                "       \"/product/$id\"(controller:\"flar\") {\n" + 
                "            action = [GET:\"myAction1\", PUT:\"myAction2\", DELETE:\"nothing\"]\n" + 
                "        }\n" + 
                "\n} }", unit.getTypes()[0].getField("myAction2"));
    }
    
    public void testComplexClosure3() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { }\ndef myAction2 = { }}");
        createGsp("flar", "myAction1.gsp", "");
        createGsp("flar", "myAction2.gsp", "");
        assertNoHyperlinks("\"nothing\"",  "class UrlMappings { static mappings = {\n" +
                "       \"/product/$id\"(controller:\"flar\") {\n" + 
                "            action = [GET:\"myAction1\", PUT:\"myAction2\", DELETE:\"nothing\"]\n" + 
                "        }\n" + 
                "\n} }");
    }
    
    public void testComplexClosure4() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { }\ndef myAction2 = { }}");
        IFile file = createGsp("flar", "myAction1.gsp", "");
        createGsp("flar", "myAction2.gsp", "");
        assertFileHyperlink("\"myAction1\"",  "class UrlMappings { static mappings = {\n" +
                "       \"/product/$id\"(controller:\"flar\") {\n" + 
                "            view = [GET:\"myAction1\", PUT:\"myAction2\", DELETE:\"nothing\"]\n" + 
                "        }\n" + 
                "\n} }", file);
    }
    
    public void testComplexClosure5() throws Exception {
        createControllerClass("FlarController.groovy", "class FlarController { def myAction1 = { }\ndef myAction2 = { }}");
        createGsp("flar", "myAction1.gsp", "");
        IFile file = createGsp("flar", "myAction2.gsp", "");
        assertFileHyperlink("\"myAction2\"",  "class UrlMappings { static mappings = {\n" +
                "       \"/product/$id\"(controller:\"flar\") {\n" + 
                "            view = [GET:\"myAction1\", PUT:\"myAction2\", DELETE:\"nothing\"]\n" + 
                "        }\n" + 
                "\n} }", file);
    }
    
    
    
    private void assertFileHyperlink(String regionName, String contents, IFile file) throws JavaModelException, CoreException {
        IRegion region = createRegion(regionName, contents);
        IHyperlink[] hyperlinks = createLinks(contents, region);
        assertNotNull("Should have found exactly one hyperlink.", hyperlinks);
        assertEquals("Should have found exactly one hyperlink.\n" + Arrays.toString(hyperlinks), 1, hyperlinks.length);
        assertEquals("Invalid hyperlink region", region, hyperlinks[0].getHyperlinkRegion());
        assertTrue("Unexpected hyperlink type: " + hyperlinks[0], hyperlinks[0] instanceof WorkspaceFileHyperlink);
        assertEquals("Invalid target of hyperlink", file, ((WorkspaceFileHyperlink) hyperlinks[0]).getFile());
    }
    private void assertNoHyperlinks(String regionName, String contents)
            throws CoreException {
        IRegion region = createRegion(regionName, contents);
        IHyperlink[] hyperlinks = createLinks(contents, region);
        assertNull("Should not have found any hyperlinks.\n" + (hyperlinks != null ? Arrays.toString(hyperlinks) : null), hyperlinks);
    }
    
    private void assertJaveElementHyperlink(String regionName, String contents, IJavaElement target)
            throws CoreException {
        IRegion region = createRegion(regionName, contents);
        IHyperlink[] hyperlinks = createLinks(contents, region);
        assertNotNull("Should have found exactly one hyperlink.", hyperlinks);
        assertEquals("Should have found exactly one hyperlink.\n" + Arrays.toString(hyperlinks), 1, hyperlinks.length);
        assertEquals("Invalid hyperlink region", region, hyperlinks[0].getHyperlinkRegion());
        assertTrue("Unexpected hyperlink type: " + hyperlinks[0], hyperlinks[0] instanceof JavaElementHyperlink);
        assertEquals("Invalid target of hyperlink", target, ((JavaElementHyperlink) hyperlinks[0]).getElement());
    }
    public IHyperlink[] createLinks(String contents, IRegion region)
            throws JavaModelException, CoreException {
        GroovyCompilationUnit unit = createUrlMapping(contents);
        UrlMappingHyperlinkDetector detector = new UrlMappingHyperlinkDetector();
        JavaEditor editor = openEditor(unit);
        detector.setContext(editor);
        IHyperlink[] hyperlinks = detector.detectHyperlinks(editor.getViewer(), region, true);
        return hyperlinks;
    }
    
    private JavaEditor openEditor(GroovyCompilationUnit unit) throws CoreException {
        // ensure opens with Groovy editor
        unit.getResource().setPersistentProperty(IDE.EDITOR_KEY, "org.codehaus.groovy.eclipse.editor.GroovyEditor");
        return (JavaEditor) EditorUtility.openInEditor(unit);
    }
    private IRegion createRegion(String regionName, String contents) {
        return new Region(contents.indexOf(regionName), regionName.length());
    }
}
