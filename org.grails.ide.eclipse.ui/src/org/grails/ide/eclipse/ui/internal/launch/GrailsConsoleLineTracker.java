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
package org.grails.ide.eclipse.ui.internal.launch;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.console.IHyperlink;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.launch.GrailsLaunchArgumentUtils;

import org.grails.ide.eclipse.ui.internal.utils.WebUiUtils;

/**
 * @author Christian Dupuis
 * @author Andy Clement
 * @author Andrew Eisenberg
 * @author Kris De Volder
 * @since 2.2.0
 */
public class GrailsConsoleLineTracker implements IConsoleLineTracker {

    private static final String OR_MARKER = " or ";

    private static final String RUNNING_MARKER = "Server running. Browse to ";

	private static final String TEST_PASSED_MARKER = "Tests PASSED - view reports in ";

	private static final String TEST_FAILED_MARKER_1 = "Tests FAILED - view reports in "; //Grails 1.3.X
	private static final String TEST_FAILED_MARKER_2 = "Tests FAILED  - view reports in "; //Grails 2.0.X
	
	// Typical line: (grails 1.1.1)
	// "Cobertura Code Coverage Complete (view reports in: N:\workspaces\grails_play\gTunes\test\reports/cobertura)"
	// Typical line: (grails 1.2m3)
	// "Cobertura Code Coverage Complete (view reports in: target\test-reports/cobertura)"
	private static final String CODE_COVERAGE_MARKER = "Cobertura Code Coverage Complete (view reports in: ";

	private IConsole console;

	public void init(IConsole console) {
		this.console = console;
	}

	public void dispose() {
	}

	public void lineAppended(IRegion line) {
		IProcess process = console.getProcess();
		ILaunch launch = process.getLaunch();
		ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();

		if (launchConfiguration != null && isGrailsLaunch(launchConfiguration)) {
			try {
				int offset = line.getOffset();
				int length = line.getLength();

				// open in browser hyperlink
				// if run-app --https, then there are 2 hyperlinks, check for that.
				String lineStr = console.getDocument().get(offset, length);
				String projectBaseDir = "";
				try {
					projectBaseDir = launchConfiguration.getAttribute(GrailsLaunchArgumentUtils.PROJECT_DIR_LAUNCH_ATTR, "");
				} catch (CoreException e) {
					// ignore for now
				}
				
				createRunAppHyperlink(lineStr, line.getOffset());
				createResultsHyperlink(TEST_PASSED_MARKER, lineStr, line.getOffset(), projectBaseDir);
				createResultsHyperlink(TEST_FAILED_MARKER_1, lineStr, line.getOffset(), projectBaseDir);
				createResultsHyperlink(TEST_FAILED_MARKER_2, lineStr, line.getOffset(), projectBaseDir);
				createResultsHyperlink(CODE_COVERAGE_MARKER, lineStr, line.getOffset(), projectBaseDir);

			} catch (BadLocationException ex) {
				// ignore
			}
		}
	}

    /**
     * @param lineNum
     * @param lineStr
     */
    protected void createRunAppHyperlink(String lineStr, int lineOffset) {
        int runningMarkerIndex = lineStr.indexOf(RUNNING_MARKER);
        if (runningMarkerIndex >= 0) {
            int startFirst = runningMarkerIndex + RUNNING_MARKER.length();
        	int endFirst = lineStr.indexOf(OR_MARKER, startFirst);
        	int startSecond;
        	int endSecond;
        	if (endFirst >= 0) {
        	    startSecond = endFirst + OR_MARKER.length();
        	    endSecond = lineStr.length();
        	} else {
        	    startSecond = -1;
        	    endSecond = -1;
        	    endFirst = lineStr.length();
        	}
        	
        	if (startFirst >= 0) {
        	    // add the http url
        	    String url = lineStr.substring(startFirst, endFirst).trim();
        		GrailsHyperLink link = new GrailsHyperLink(url);
        		console.addLink(link, lineOffset + startFirst, url.length());
        		if (startSecond >= 0) {
                    // add the https url
                    url = lineStr.substring(startSecond, endSecond).trim();
                    link = new GrailsHyperLink(url);
                    console.addLink(link, lineOffset + startSecond, url.length());
        		}
        	}
        }
    }

	protected void createResultsHyperlink(String marker, String text, int offset, String baseDir) {
		int index = text.indexOf(marker);
		if (index >= 0) {
		    int parenIndex = text.indexOf(')', index);
		    int end = parenIndex > 0 ? parenIndex : text.length();
			int beg = marker.length()+index;
			String path = text.substring(beg, end);
			String subFolder = marker.equals(CODE_COVERAGE_MARKER) ? "" : File.separator + "html";
            String url = "file:" + toAbsolute(path, baseDir) + subFolder + File.separator + "index.html";
            GrailsHyperLink link = new GrailsHyperLink(url);
            console.addLink(link, offset + beg, path.length());
		}
	}

	/**
	 * Poor first stab at detecting whether a path is relative and making it absolute.
	 * 
	 * @param path the path that may or may not be relative
	 * @param prefix the prefix to prepend to the path if it proves to be relative
	 * @return the absolute path
	 */
	private String toAbsolute(String path, String prefix) {
		if (path.length() > 1 && path.charAt(1) == ':') {
			// windows drive qualifier
		} else {
			if (path.length() > 0 && path.charAt(0) != File.separatorChar) {
				return prefix + File.separator + path;
			}
		}
		return path;
	}

	private boolean isGrailsLaunch(ILaunchConfiguration launchConfiguration) {
		try {
			ILaunchConfigurationType type = launchConfiguration.getType();
			return GrailsCoreActivator.PLUGIN_ID.equals(type.getPluginIdentifier());
		} catch (CoreException ex) {
			return false;
		}
	}

	public class GrailsHyperLink implements IHyperlink {

		private final String url;

		public GrailsHyperLink(String url) {
			this.url = url;
		}

		public void linkActivated() {
			WebUiUtils.openUrl(url);
		}

		public void linkEntered() {
		}

		public void linkExited() {
		}
		public String getUrl() {
            return url;
        }
	}
}
