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
package org.grails.ide.eclipse.ui.test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleHyperlink;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.IPatternMatchListener;

import org.grails.ide.eclipse.ui.internal.launch.GrailsConsoleLineTracker;
import org.grails.ide.eclipse.ui.internal.launch.GrailsConsoleLineTracker.GrailsHyperLink;

/**
 * Tests that the {@link GrailsConsoleLineTracker} properly creates 
 * links
 * @author Andrew Eisenberg
 * @created Aug 24, 2010
 */
@SuppressWarnings("deprecation")
public class GrailsConsoleLineTrackerTests extends TestCase {
    
    private static final String RUNNING_MARKER = "Server running. Browse to ";

    private static final String TEST_PASSED_MARKER = "Tests PASSED - view reports in ";

    private static final String TEST_FAILED_MARKER = "Tests FAILED - view reports in ";

    // Typical line: (grails 1.1.1)
    // "Cobertura Code Coverage Complete (view reports in: N:\workspaces\grails_play\gTunes\test\reports/cobertura)"
    // Typical line: (grails 1.2m3)
    // "Cobertura Code Coverage Complete (view reports in: target\test-reports/cobertura)"
    private static final String CODE_COVERAGE_MARKER = "Cobertura Code Coverage Complete (view reports in: ";

    
    private static final String MOCK_BASE_DIR = "/tests";
    
    MockLineTracker lineTracker;
    MockConsole console;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        lineTracker = new MockLineTracker();
        console = new MockConsole();
        lineTracker.init(console);
    }
    
    public void testRunApp1() throws Exception {
        lineTracker.createRunAppHyperlink("ffkjafsdkj " + RUNNING_MARKER + "http://foobar", 0);
        console.assertLinks("http://foobar");
    }
    
    public void testRunApp2() throws Exception {
        lineTracker.createRunAppHyperlink("ffkjafsdkj " + RUNNING_MARKER + "http://foobar or https://foobar", 0);
        console.assertLinks("http://foobar", "https://foobar");
    }
    
    public void testTestPassed1() throws Exception {
        lineTracker.createResultsHyperlink(TEST_PASSED_MARKER, TEST_PASSED_MARKER + "/bee/bop/woo/bop", 0, MOCK_BASE_DIR);
        console.assertLinks("file:/bee/bop/woo/bop/html/index.html");
    }
    
    public void testTestPassed2() throws Exception {
        lineTracker.createResultsHyperlink(TEST_PASSED_MARKER, TEST_PASSED_MARKER + "bee/bop/woo/bop", 0, MOCK_BASE_DIR);
        console.assertLinks("file:/tests/bee/bop/woo/bop/html/index.html");
    }
    
    public void testTestFailed1() throws Exception {
        lineTracker.createResultsHyperlink(TEST_FAILED_MARKER, TEST_FAILED_MARKER + "/bee/bop/woo/bop", 0, MOCK_BASE_DIR);
        console.assertLinks("file:/bee/bop/woo/bop/html/index.html");
    }
    
    public void testTestFailed2() throws Exception {
        lineTracker.createResultsHyperlink(TEST_FAILED_MARKER, TEST_FAILED_MARKER + "bee/bop/woo/bop", 0, MOCK_BASE_DIR);
        console.assertLinks("file:/tests/bee/bop/woo/bop/html/index.html");
    }
    
    public void testTestCoverage1() throws Exception {
        lineTracker.createResultsHyperlink(CODE_COVERAGE_MARKER, CODE_COVERAGE_MARKER + "/bee/bop/woo/bop", 0, MOCK_BASE_DIR);
        console.assertLinks("file:/bee/bop/woo/bop/index.html");
    }
    
    public void testTestCoverage2() throws Exception {
        lineTracker.createResultsHyperlink(CODE_COVERAGE_MARKER, CODE_COVERAGE_MARKER + "bee/bop/woo/bop", 0, MOCK_BASE_DIR);
        console.assertLinks("file:/tests/bee/bop/woo/bop/index.html");
    }
    
    private class MockLineTracker extends GrailsConsoleLineTracker {
        /*
         * make accessible
         */
        @Override
        protected void createResultsHyperlink(String marker, String text,
                int offset, String baseDir) {
            super.createResultsHyperlink(marker, text, offset, baseDir);
        }
        
        /*
         * make accessible
         */
        @Override
        protected void createRunAppHyperlink(String lineStr, int lineOffset) {
            super.createRunAppHyperlink(lineStr, lineOffset);
        }
    }
    private class MockConsole implements IConsole {
        
        List<String> links = new ArrayList<String>();

        public void connect(IStreamsProxy streamsProxy) {
            //nop
        }

        public void connect(IStreamMonitor streamMonitor, String streamIdentifer) {
            //nop
        }

        public void addLink(IConsoleHyperlink link, int offset, int length) {
            //nop
        }

        public void addLink(IHyperlink link, int offset, int length) {
            if (link instanceof GrailsHyperLink) {
                links.add(((GrailsHyperLink) link).getUrl());
            } else {
                fail("Invalid hyperlink created: " + link);
            }
        }

        public IRegion getRegion(IConsoleHyperlink link) {
            //nop
            return null;
        }

        public IRegion getRegion(IHyperlink link) {
            // nop
            return null;
        }

        public IDocument getDocument() {
            //nop
            return null;
        }

        public IProcess getProcess() {
            //nop
            return null;
        }

        public void addPatternMatchListener(IPatternMatchListener matchListener) {
            //nop
        }

        public void removePatternMatchListener(
                IPatternMatchListener matchListener) {
            //nop
        }

        public IOConsoleOutputStream getStream(String streamIdentifier) {
            //nop
            return null;
        }
        
        void assertLinks(String...urls) {
            assertEquals("Wrong number of URLs found", urls.length, links.size());
            for (int i = 0; i < urls.length; i++) {
                assertEquals("Wrong URL found", urls[i], links.get(i));
            }
        }
        
    }
}
