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

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;

import org.grails.ide.eclipse.editor.gsp.tags.PerProjectTagProvider;

/**
 * Tests that the creation of GSP structured models
 * have their tag libs properly initialized and uninitialized 
 * in response to changes to the project
 * @author Andrew Eisenberg
 * @author Nieraj Singh
 * @created Jan 19, 2010
 */
public class GSPStructuredModelCreationTests extends AbstractGSPTagsTest {
    
    public void testCreateModel() throws Exception {
        createModel("foo.gsp", "TTT");
        assertFalse("Should have no errors on the log, but had:\n" + logger.getErrorEntriesAsText(),
                logger.hasErrors());
        
        // should have a connect entry and that's it
        List<IStatus> statuses = logger.getAllEntriesWithPrefix("PerProjectTagProvider");
        assertEquals("Should have found one connect status, but instead found:\n" + logger.getSomeEntriesAsText(statuses), 
                1, statuses.size());
        int i = 0;
        assertConnectStatus(statuses, i++);
    }
    public void testCreateModel2() throws Exception {
        createModel("foo.gsp", "TTT");
        createModel("foo2.gsp", "TTT");
        assertFalse("Should have no errors on the log, but had:\n" + logger.getErrorEntriesAsText(),
                logger.hasErrors());
        
        // should have a connect entry and that's it
        // the model should be shared between the two 
        List<IStatus> statuses = logger.getAllEntriesWithPrefix("PerProjectTagProvider");
        assertEquals("Should have found one connect status, but instead found:\n" + logger.getSomeEntriesAsText(statuses), 
                2, statuses.size());
        int i = 0;
        assertConnectStatus(statuses, i++);
        assertConnectStatus(statuses, i++);
    }

    public void testCreateModel3() throws Exception {
        IStructuredModel model = createModel("foo.gsp", "TTT");
        model.releaseFromEdit();
        model = reopenModel(model.getBaseLocation());
        assertFalse("Should have no errors on the log, but had:\n" + logger.getErrorEntriesAsText(),
                logger.hasErrors());
        
        // should have a connect entry and that's it
        // the model should be shared between the two 
        List<IStatus> statuses = logger.getAllEntriesWithPrefix("PerProjectTagProvider");
        assertEquals("Found wrong number of statuses:\n" + logger.getSomeEntriesAsText(statuses), 
                3, statuses.size());
        int i = 0;
        assertConnectStatus(statuses, i++);
        assertDisconnectStatus(statuses, i++);
        assertConnectStatus(statuses, i++);
    }
    
    // test that getting the document for a tag name forces initialization 
    // of tags
    public void testCreateModel4() throws Exception {
        createModel("foo.gsp", "TTT");
        PerProjectTagProvider provider = getTagProvider();
        provider.getDocumentForTagName("nuthin");
        // it looks like on the build server uninitialize can be called twice
        // that will increase the number of statuses from 7 to 9
        List<IStatus> statuses = logger.getAllEntriesWithPrefix("PerProjectTagProvider");
        assertTrue("Found wrong number of statuses:\n" + logger.getSomeEntriesAsText(statuses), 
                statuses.size() == 3 || statuses.size() == 5);
        int i = 0;
        assertConnectStatus(statuses, i++);
        assertInitializeStatus(statuses, i++);
        assertInitializeTagTrackersStatus(statuses, i++);
        if (statuses.size() == 5) {
            assertUninitializeStatus(statuses, i++);
            assertUninitializeTagTrackersStatus(statuses, i++);
        }
    }
    // Test that touching the classpath will force an uninitialization 
    // of the tag libs
    public void testCreateModel5() throws Exception {
        createModel("foo.gsp", "TTT");
        testProject.getProject().getFile(".classpath").touch(null);
        
        List<IStatus> statuses = logger.getAllEntriesWithPrefix("PerProjectTagProvider");
        assertEquals("Found wrong number of statuses:\n" + logger.getSomeEntriesAsText(statuses), 
                3, statuses.size());
        int i = 0;
        assertConnectStatus(statuses, i++);
        assertUninitializeStatus(statuses, i++);
        assertUninitializeTagTrackersStatus(statuses, i++);
        
    }
    

    // test that getting the document for a tag name forces initialization 
    // of tags and that touching the classpath will force uninitialization
    public void testCreateModel6() throws Exception {
        createModel("foo.gsp", "TTT");
        PerProjectTagProvider provider = getTagProvider();
        provider.getDocumentForTagName("nuthin");
        testProject.getProject().getFile(".classpath").touch(null);
        provider.getDocumentForTagName("nuthin");

        // it looks like on the build server uninitialize can be called twice
        // that will increase the number of statuses from 7 to 9
        List<IStatus> statuses = logger.getAllEntriesWithPrefix("PerProjectTagProvider");
        assertTrue("Found wrong number of statuses:\n" + logger.getSomeEntriesAsText(statuses), 
                statuses.size() == 7 || statuses.size() == 9);
        int i = 0;
        assertConnectStatus(statuses, i++);
        assertInitializeStatus(statuses, i++);
        assertInitializeTagTrackersStatus(statuses, i++);
        assertUninitializeStatus(statuses, i++);
        assertUninitializeTagTrackersStatus(statuses, i++);
        if (statuses.size() == 9) {
            assertUninitializeStatus(statuses, i++);
            assertUninitializeTagTrackersStatus(statuses, i++);
        }
        assertInitializeStatus(statuses, i++);
        assertInitializeTagTrackersStatus(statuses, i++);
    }
    
    // test that getting the document for a tag name forces initialization 
    // of tags and that touching the classpath will force uninitialization
    // checks the above, but with 2 models
    public void testCreateModel7() throws Exception {
        createModel("foo.gsp", "TTT");
        createModel("foo2.gsp", "TTT");
        PerProjectTagProvider provider = getTagProvider();
        provider.getDocumentForTagName("nuthin");
        testProject.getProject().getFile(".classpath").touch(null);
        provider.getDocumentForTagName("nuthin");

        // it looks like on the build server uninitialize can be called twice
        // that will increase the number of statuses from 11 to 14
        List<IStatus> statuses = logger.getAllEntriesWithPrefix("PerProjectTagProvider");
        assertTrue("Found wrong number of statuses:\n" + logger.getSomeEntriesAsText(statuses), 
                statuses.size() == 11 || statuses.size() == 14);
        int i = 0;
        assertConnectStatus(statuses, i++);
        assertConnectStatus(statuses, i++);
        assertInitializeStatus(statuses, i++);
        assertInitializeTagTrackersStatus(statuses, i++);
        assertInitializeTagTrackersStatus(statuses, i++);
        assertUninitializeStatus(statuses, i++);
        assertUninitializeTagTrackersStatus(statuses, i++);
        assertUninitializeTagTrackersStatus(statuses, i++);
        if (statuses.size() == 14) {
            assertUninitializeStatus(statuses, i++);
            assertUninitializeTagTrackersStatus(statuses, i++);
            assertUninitializeTagTrackersStatus(statuses, i++);
        }
        assertInitializeStatus(statuses, i++);
        assertInitializeTagTrackersStatus(statuses, i++);
        assertInitializeTagTrackersStatus(statuses, i++);
    }
    
    // test that a change to a custom tag lib forces a reinitialize
    public void testCreateModel8() throws Exception {
        // create this first so it doesn't force a second uninitialize
        IPackageFragmentRoot root = testProject.createSourceFolder("grails-app/taglib", null);
        IPackageFragment pack = root.createPackageFragment("nuthin", true, null);

        createModel("foo.gsp", "TTT");
        PerProjectTagProvider provider = getTagProvider();
        provider.getDocumentForTagName("nuthin");

        testProject.createGroovyType(pack, "NuthinTagLib.groovy", "class NuthinTagLib { }");
        
        provider.getDocumentForTagName("nuthin");
        
        // it looks like on the build server uninitialize can be called twice
        // that will increase the number of statuses from 7 to 9
        List<IStatus> statuses = logger.getAllEntriesWithPrefix("PerProjectTagProvider");
        assertTrue("Found wrong number of statuses:\n" + logger.getSomeEntriesAsText(statuses), 
                statuses.size() == 7 || statuses.size() == 9);
        int i = 0;
        assertConnectStatus(statuses, i++);
        assertInitializeStatus(statuses, i++);
        assertInitializeTagTrackersStatus(statuses, i++);
        assertUninitializeStatus(statuses, i++);
        assertUninitializeTagTrackersStatus(statuses, i++);
        if (statuses.size() == 9) {
            assertUninitializeStatus(statuses, i++);
            assertUninitializeTagTrackersStatus(statuses, i++);
        }
        assertInitializeStatus(statuses, i++);
        assertInitializeTagTrackersStatus(statuses, i++);
    }
    
    private void assertConnectStatus(List<IStatus> statuses, int index) {
        assertTrue("Should have found connect status at " + index + " , but instead found:\n" + logger.getSomeEntriesAsText(statuses), 
                statuses.get(index).getMessage().startsWith("PerProjectTagProvider.connect()"));
    }
    private void assertDisconnectStatus(List<IStatus> statuses, int index) {
        assertTrue("Should have found disconnect status at " + index + " , but instead found:\n" + logger.getSomeEntriesAsText(statuses), 
                statuses.get(index).getMessage().startsWith("PerProjectTagProvider.disconnect()"));
    }
    private void assertInitializeStatus(List<IStatus> statuses, int index) {
        assertTrue("Should have found initialize status at " + index + " , but instead found:\n" + logger.getSomeEntriesAsText(statuses), 
                statuses.get(index).getMessage().startsWith("PerProjectTagProvider.initialize()"));
    }
    private void assertUninitializeStatus(List<IStatus> statuses, int index) {
        assertTrue("Should have found uninitialize status at " + index + " , but instead found:\n" + logger.getSomeEntriesAsText(statuses), 
                statuses.get(index).getMessage().startsWith("PerProjectTagProvider.uninitialize()"));
    }
    private void assertInitializeTagTrackersStatus(List<IStatus> statuses, int index) {
        assertTrue("Should have found initialize tag tracker status at " + index + " , but instead found:\n" + logger.getSomeEntriesAsText(statuses), 
                statuses.get(index).getMessage().startsWith("PerProjectTagProvider.initializeTagTrackers()"));
    }
    private void assertUninitializeTagTrackersStatus(List<IStatus> statuses, int index) {
        assertTrue("Should have found uninitialize tag tracker status at " + index + " , but instead found:\n" + logger.getSomeEntriesAsText(statuses), 
                statuses.get(index).getMessage().startsWith("PerProjectTagProvider.uninitializeTagTrackers()"));
    }
    
    private PerProjectTagProvider getTagProvider() {
        PerProjectTagProvider provider = null;
        int count = 0;
        while (provider == null) {
            if (count >= 25) {
                fail("could not get tag provider for project " + testProject.getProject());
            }
            if (count > 0) {
                synchronized (this) {
                    try {
                        System.out.println("Could not find provider...waiting 1s and retrying");
                        wait(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
            count++;
            provider = GrailsCore.get().connect(testProject.getProject(), 
                    PerProjectTagProvider.class);
        }
        return provider;
    }

}
