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
package org.grails.ide.eclipse.runtime.shared.longrunning;

public class GrailsProcessConstants {
	
	public static final String PROCESS_CLASS_NAME = "org.grails.ide.eclipse.longrunning.process.GrailsProcess";
	   //It would be nice if we could actually refer to the process class here, but we can't because its not in the
	   //the 'shared' bundle. It contains code dependent on the Grails API so it is inherently Grails version specific.
	   //We should be able to ensure that the class name is the same no matter which implementation we use.

	// First send a BEGIN_COMMAND
	public static final String BEGIN_COMMAND 			= "%beg ";
		
	// Then send command parameters
	public static final String COMMAND_SCRIPT_NAME 		= "%nam ";
	public static final String COMMAND_ENV 				= "%env ";
	public static final String COMMAND_ARGS 			= "%arg ";
	public static final String END_COMMAND 				= "%end ";
	public static final String EXIT						= "%exit";

	// Alternatively send a single unparsed command String for processing by Grails own parsing logic
	public static final String COMMAND_UNPARSED 		= "%unp ";
	
	// Additional to the above, can also send (optional) a dependecy file name. If sent, the
	// dependency data from the buildsettings are dumped to this file at the end of the command's execution
	public static final String COMMAND_DEPENDENCY_FILE 	= "%dep ";
	
	// Before sending a command a 'CHANGE_DIR' can be sent to change the directory that Grails is
	// using as its 'baseDir'. This is not guaranteed to work, if it doesn't work an ACK_BAD is
	// returned and the process terminates. Otherwise an ACK_OK is returned and the process is
	// ready to accept a command.
	public static final String CHANGE_DIR 				= "%cd  ";

	// STS should send the contents of the input stream that is to be sent to the command after it sends
	// end command. This input should be terminated by an %eof
	// The input is read 'asychronously' by the GrailsProcess while the command is executing.
	// An %eof must be received before the process will accept new commands.
	public static final String CONSOLE_INPUT			= "%inp ";
	public static final String CONSOLE_EOF				= "%eof ";
	
	// Grails process output is sent to the client, prefixed with either one of the following (depending on whether
	// came from System.out or System.err
	public static final String CONSOLE_OUT 				= "%out ";
	public static final String CONSOLE_ERR 				= "%err ";
	
	/**
	 * All the Strings declared above should be the same length. This is that length.
	 */
	public static final int PROTOCOL_HEADER_LEN = END_COMMAND.length();
	
	
	// Acknowledgement sent back for an operation like "%cd "
	public static final String ACK_BAD 	= "%BAD "; // Couldn't do what was asked and will exit soon
	public static final String ACK_OK	= "%OK  "; // OK process did what was asked

}
