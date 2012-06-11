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

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import junit.framework.TestCase;

import org.grails.ide.eclipse.ui.console.StackTraceConsoleLineTracker;
import org.grails.ide.eclipse.ui.console.StackTraceConsoleLineTracker.StackFrameInfo;
import org.grails.ide.eclipse.ui.internal.launch.GrailsConsoleLineTracker;

/**
 * Tests that the {@link GrailsConsoleLineTracker} properly creates 
 * links
 * @author Andrew Eisenberg
 * @since 2.8.0.M2
 */
public class StackFrameConsoleLineTrackerTests extends TestCase {
    class MockStackTraceConsoleLineTracker extends StackTraceConsoleLineTracker {
        // make accessible
        @Override
        protected void initDocument(IDocument document) {
            super.initDocument(document);
        }
        @Override
        protected StackFrameInfo extractStackFrame(IRegion region, int depth) {
            return super.extractStackFrame(region, depth);
        }
    }
    public void testStackFrameConsole1() throws Exception {
        assertStackFrame("Line | Method\n" + 
        		"->> 303 | innerRun in java.util.concurrent.FutureTask$Sync\n" + 
        		"- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - \n" + 
        		"|   138 | run      in java.util.concurrent.FutureTask\n" + 
        		"|   886 | runTask  in java.util.concurrent.ThreadPoolExecutor$Worker\n" + 
        		"|   908 | run      in     ''\n" + 
        		"^   680 | run . .  in java.lang.Thread", 
        		680, "run", "java.lang.Thread");
    }
    
    public void testStackFrameConsole2() throws Exception {
        assertStackFrame("Line | Method\n" + 
                "->> 303 | innerRun in java.util.concurrent.FutureTask$Sync\n" + 
                "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - \n" + 
                "|   138 | run      in java.util.concurrent.FutureTask\n" + 
                "|   886 | runTask  in java.util.concurrent.ThreadPoolExecutor$Worker\n" + 
                "|   908 | run      in     ''", 
                908, "run", "java.util.concurrent.ThreadPoolExecutor.Worker");
    }
    
    public void testStackFrameConsole3() throws Exception {
        assertStackFrame("Line | Method\n" + 
                "->> 303 | innerRun in java.util.concurrent.FutureTask$Sync\n" + 
                "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - \n" + 
                "|   138 | run      in java.util.concurrent.FutureTask\n" + 
                "|   886 | runTask  in java.util.concurrent.ThreadPoolExecutor$Worker", 
                886, "runTask", "java.util.concurrent.ThreadPoolExecutor.Worker");
    }
    
    public void testStackFrameConsole4() throws Exception {
        assertStackFrame("Line | Method\n" + 
                "->> 303 | innerRun in java.util.concurrent.FutureTask$Sync\n" + 
                "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - \n" + 
                "|   138 | run      in java.util.concurrent.FutureTask", 
                138, "run", "java.util.concurrent.FutureTask");
    }
    
    public void testStackFrameConsole5() throws Exception {
        assertStackFrame("Line | Method\n" + 
                "->> 303 | innerRun in java.util.concurrent.FutureTask$Sync", 
                303, "innerRun", "java.util.concurrent.FutureTask.Sync");
    }
    
    public void testStackFrameConsole6() throws Exception {
        assertStackFrame("Line | Method\n" + 
                "->> 303 | innerRun in java.util.concurrent.FutureTask$hello__closure", 
                303, "innerRun", "java.util.concurrent.FutureTask");
    }
    
    public void testStackFrameConsole7() throws Exception {
        assertStackFrame("Line | Method\n" + 
                "->> 303 | innerRun in java.util.concurrent.FutureTask$Sync\n" + 
                "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - \n" + 
                "|   908 | run      in     ''", 
                908, "run", "java.util.concurrent.FutureTask.Sync");
    }

    public void testStackFrameConsole8() throws Exception {
        assertStackFrame("Line | Method\n" + 
                "->> 303 | innerRun in java.util.concurrent.FutureTask$Sync\n" + 
                "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - \n" + 
                "|   0   | other      in     ''\n" +
                "|   908 | run      in     ''", 
                908, "run", "java.util.concurrent.FutureTask.Sync");
    }
    
    public void testStackFrameConsole9() throws Exception {
        assertNoStackFrame("Line | Method\n" + 
                "->> 303 | innerRun in java.util.concurrent.FutureTask$Sync\n" + 
                "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - \n" + 
                "|   0   | other      in     ''\n" +
                "|   0   | other      in     ''\n" +
                "|   0   | other      in     ''\n" +
                "|   0   | other      in     ''\n" +
                "|   0   | other      in     ''\n" +
                "|   0   | other      in     ''\n" +
                "|   0   | other      in     ''\n" +
                "|   908 | run      in     ''");
    }
    
    public void testStackFrameConsole10() throws Exception {
        assertNoStackFrame("Line | Method\n" + 
                "->> 303 | innerRun in java.util.concurrent.FutureTask$Sync\n" + 
                "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - "); 
    }
    
    void assertStackFrame(String doc, int lineNum, String method, String className) {
        StackFrameInfo info = createInfo(doc);
        assertEquals("Invalid line number for stack frame: " + info, lineNum, info.lineNum);
        assertEquals("Invalid method for stack frame: " + info, method, info.method);
        assertEquals("Invalid class name for stack frame: " + info, className, info.fixedClassName);
    }

    private void assertNoStackFrame(String doc) {
        StackFrameInfo info = createInfo(doc);
        assertNull("Should not have found a stack frame, but found " + info, info);
    }

    private StackFrameInfo createInfo(String doc) {
        MockStackTraceConsoleLineTracker tracker = new MockStackTraceConsoleLineTracker();
        tracker.initDocument(new Document(doc));
        int lineStart = doc.lastIndexOf('\n') + 1;
        int len = doc.length() - lineStart;
        StackFrameInfo info = tracker.extractStackFrame(new Region(lineStart, len), 0);
        return info;
    }
}
