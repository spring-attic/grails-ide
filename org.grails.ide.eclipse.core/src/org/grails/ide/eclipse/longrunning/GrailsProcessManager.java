/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.longrunning;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.longrunning.client.GrailsClient;


/**
 * An instance of this class manages a pool of long running Grails processes.
 * <p>
 * Note: This initial implementation only handles a pool of size 1.
 * 
 * @author Kris De Volder
 * @since 2.5.3
 */
public class GrailsProcessManager {
	
	private static GrailsProcessManager instance;
	public static ConsoleProvider consoleProvider;

	/**
	 * The process that may be reused. The process will only be reused if both of the following
	 * are true:
	 *   - the baseDir matches that for the next command.
	 *   - no changes have happened to certain config files.
	 */
	private GrailsClient theProcess = null; 
	
	public static GrailsProcessManager getInstance() {
		if (instance == null) {
			instance = new GrailsProcessManager();
		}
		return instance;
	}
	
	/**
	 * Gets a client side reference to an external GrailsProcess for a given Grails install and working directory.
	 * <p>
	 * Important: users of the process must add the necessary synchronized block around the code fetching the GrailsClient
	 * instance and using it. Normally, only the {@link LongRunningProcessGrailsExecutor} should be calling this method.
	 */
	GrailsClient getGrailsProcess(IGrailsInstall install, File workingDir) throws IOException, TimeoutException {
		//shutDown(); // can uncomment for testing purposes (process will not be reused at all)
		try {
			if (theProcess==null) {
				theProcess = new GrailsClient(install, workingDir);
			} else if (!theProcess.getInstall().equals(install)) {
				//Can't reuse process if its running with the wrong Grails install
				theProcess.shutDown();
				theProcess = new GrailsClient(install, workingDir);
			}
			theProcess.changeDir(workingDir);
			return theProcess;
		//Catch a bunch of errors and rethrow, but on the way out attempt to get rid of erratically behaving process.
		} catch (IOException e) {
			theProcess.shutDown();
			theProcess = null;
			throw e;
		} catch (TimeoutException e) {
			theProcess.shutDown();
			theProcess = null;
			throw e;
		} catch (RuntimeException e) {
			theProcess.shutDown();
			theProcess = null;
			throw e;
		}
	}
	
	/**
	 * Terminate all external processes.
	 */
	public void shutDown() {
		//TODO: make sure this gets called by some plugin activator when it shuts down.
		if (theProcess!=null) {
			theProcess.shutDown();
		}
	}
}
