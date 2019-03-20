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
package org.grails.ide.eclipse.ui.console;

import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jdt.internal.debug.ui.console.JavaStackTraceHyperlink;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.console.TextConsole;
import org.grails.ide.eclipse.core.GrailsCoreActivator;


/**
 * Startgin with Grails 2.0 the stack traces are formatted differently.  We need to ensure that the hyperlinks are still available 
 * to go to the source of the stack frame.
 * 
 * Should look something like this:
 * <pre>
Line | Method
->> 303 | innerRun in java.util.concurrent.FutureTask$Sync
- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
|   138 | run      in java.util.concurrent.FutureTask
|   886 | runTask  in java.util.concurrent.ThreadPoolExecutor$Worker
|   908 | run      in     ''
^   680 | run . .  in java.lang.Thread
</pre>
 * 
 * @author Andrew Eisenberg
 * @since 2.8.0.M2
 */
public class StackTraceConsoleLineTracker implements IConsoleLineTracker {
    public final class StackFrameInfo {
        public final String lineText;
        public final int lineNum;
        public final String fixedClassName;
        public final String method;
        public final String lineNumStr;
        public final String origClassName;
        StackFrameInfo(String lineText, int lineNum, String fixedClassName, String method, String lineNumStr, String origClassName) {
            this.lineNum = lineNum;
            this.lineNumStr = lineNumStr;
            this.fixedClassName = fixedClassName;
            this.method = method;
            this.origClassName = origClassName;
            this.lineText = lineText;
        }
        @Override
        public String toString() {
            return "StackFrameInfo [lineText=" + lineText + ", lineNum="
                    + lineNum + ", fixedClassName=" + fixedClassName
                    + ", method=" + method + ", lineNumStr=" + lineNumStr
                    + ", origClassName=" + origClassName + "]";
        }
    }
    
    
    private final class GrailsStackTraceHyperlink extends JavaStackTraceHyperlink {
        
        private final StackFrameInfo info;

        public GrailsStackTraceHyperlink(StackFrameInfo info, TextConsole console) {
            super(console);
            this.info = info;
        }
        
        @Override
        protected int getLineNumber(String linkText) throws CoreException {
            return info.lineNum;
        }
        @Override
        protected String getTypeName(String linkText) throws CoreException {
            return info.fixedClassName;
        }
        @Override
        protected String getLinkText() throws CoreException {
            return info.fixedClassName;
        }
    }

    private final static String[] START_SEQUENCES = new String[] { "|", "->>", "^" };
    
    private TextConsole console;
    private IDocument document;

    public void init(IConsole console) {
        if (console instanceof TextConsole) {
            this.console = (TextConsole) console;
            this.document = console.getDocument();
        } else {
            this.console = null;
            this.document = null;
        }
    }
    
    protected void initDocument(IDocument document) {
        this.document = document;
    }

    public void lineAppended(IRegion line) {
        if (console == null) {
            return;
        }
        try {
            StackFrameInfo info = extractStackFrame(line, 0);
            if (info != null) {
                createLinks(line, info);
            }
        } catch (BadLocationException e) {
            GrailsCoreActivator.log(e);
        }
    }

    private String getLineText(IRegion line) throws BadLocationException {
        return document.get(line.getOffset(), line.getLength());
    }

    /**
     * create 3 hyperlinks.  One for the line number, one for the method name and
     * one for the type nme
     * @param line
     * @param info
     * @param text
     * @throws BadLocationException
     */
    private void createLinks(IRegion line, StackFrameInfo info)
            throws BadLocationException {
        int lineStart = info.lineText.indexOf(info.lineNumStr);
        int methodStart = info.lineText.indexOf(info.method, lineStart);
        int classNameStart = info.lineText.indexOf(info.origClassName);
        if (info.origClassName.endsWith(".gsp")) {
            // must handle gsp files differently
            IFile file = findFile(info);
            if (file != null) {
                console.addHyperlink(new FileLink(file, null, -1, -1, info.lineNum), line.getOffset() + lineStart, info.lineNumStr.length());
                console.addHyperlink(new FileLink(file, null, -1, -1, info.lineNum), line.getOffset() + methodStart, info.method.length());
                console.addHyperlink(new FileLink(file, null, -1, -1, info.lineNum), line.getOffset() + classNameStart, info.origClassName.length());
            }
        } else {
            console.addHyperlink(new GrailsStackTraceHyperlink(info, console), line.getOffset() + lineStart, info.lineNumStr.length());
            console.addHyperlink(new GrailsStackTraceHyperlink(info, console), line.getOffset() + methodStart, info.method.length());
            console.addHyperlink(new GrailsStackTraceHyperlink(info, console), line.getOffset() + classNameStart, info.origClassName.length());
        }
    }

    private IFile findFile(StackFrameInfo info) {
        // this is the name of the file without the project name
        // need to gues the project name.
        // assume that this file is inside of the project (and not coming from an in place plugin)
        String name = info.origClassName;
        String consoleName = console.getName();
        IPath path = new Path(consoleName);
        if (path.segmentCount() > 1) {
            // STS-2506 project name has a slash or some other unreadable character.
            return null;
        }
        int nameEnd = consoleName.indexOf(" (");
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(consoleName.substring(0, nameEnd));
        if (! project.exists()) {
            return null;
        }
        
        IFile file = project.getFile(name);
        return file.exists() ? file : null;
    }

    /**
     * @param region the region of the current line
     * @param depth not my favorite way of doing things, but use this param to ensure that recursion doesn't go too deep
     * @return enough information about the stack frame to create a link, or null if there is none.
     */
    protected StackFrameInfo extractStackFrame(IRegion region, int depth) {
        if (depth >= 4) {
            // avoid extensive recursion
            return null;
        }
        String text = null;
        try {
            text = getLineText(region);
        } catch (BadLocationException e) {
            GrailsCoreActivator.log(e);
            return null;
        }
        if (!isValidStart(text)) {
            return null;
        }
        StringTokenizer tokenizer = new StringTokenizer(text);
        // ignore the first element
        tokenizer.nextToken();
        if (!tokenizer.hasMoreElements()) {
            return null;
        }
        int lineNum = -1;
        String origClassName = null;
        String methodName = null;
        
        String next = tokenizer.nextToken();
        String lineNumStr;
        String fixedClassName;
        // should be a number
        try {
            lineNumStr = next;
            lineNum = Integer.parseInt(next);
        } catch (NumberFormatException nfe) {
            return null;
        }
        if (!tokenizer.hasMoreElements()) {
            return null;
        }
        // column separator
        if (!tokenizer.nextToken().equals("|")) {
            return null;
        }
        if (!tokenizer.hasMoreElements()) {
            return null;
        }
        // method name
        methodName = tokenizer.nextToken();
        if (!tokenizer.hasMoreElements()) {
            return null;
        }
        
        next = tokenizer.nextToken();
        // there can be some dots here.  just consume them
        while (next.equals(".") && tokenizer.hasMoreTokens()) {
            next = tokenizer.nextToken();
        }
        
        // the word 'in'
        if (!next.equals("in")) {
            return null;
        }
        if (!tokenizer.hasMoreElements()) {
            return null;
        }
        // class name or ''
        origClassName = tokenizer.nextToken();
        if (tokenizer.hasMoreElements()) {
            return null;
        }
        
        if (!origClassName.equals("''")) {
            fixedClassName = fixTypeName(origClassName);
        } else {
            // need to recursively check the previous line for the real type name
            fixedClassName = null;
            IRegion previous = getPrevious(region, depth);
            if (previous != null) {
                StackFrameInfo info = extractStackFrame(previous, depth+1);
                if (info != null) {
                    fixedClassName = info.fixedClassName;
                }
            }
        }
        
        if (fixedClassName== null || fixedClassName.equals("''")) {
            // recursion has bottomed out
            return null;
        }
        return new StackFrameInfo(text, lineNum, fixedClassName, methodName, lineNumStr, origClassName);
    }

    private IRegion getPrevious(IRegion region, int depth) {
        if (depth > 4) { // prevent excessive recursion
            return null;
        }
        if (region.getOffset() <= 0) {
            return null;
        }
        try {
            int lineOfOffset = document.getLineOfOffset(region.getOffset());
            if (lineOfOffset <= 0) {
                return null;
            }
            IRegion candidate = document.getLineInformation(lineOfOffset-1);
            if (document.get(candidate.getOffset(), candidate.getLength()).startsWith("- - -")) {
                return getPrevious(candidate, depth+1);
            }
            return candidate;
        } catch (BadLocationException e) {
            GrailsCoreActivator.log(e);
            return null;
        }
    }

    private String fixTypeName(String className) {
        if (className.contains("__closure")) {
            // assume synthetic closure call
            int index = className.indexOf('$');
            if (index > 0) {
                className = className.substring(0, index);
            }
        }
        return className.replace('$', '.');
    }

    public void dispose() {
        console = null;
        document = null;
    }

    
    private boolean isValidStart(String text) {
        for (String prefix : START_SEQUENCES) {
            if (text.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
