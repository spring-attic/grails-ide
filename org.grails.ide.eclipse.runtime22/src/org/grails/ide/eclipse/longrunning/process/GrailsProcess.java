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
package org.grails.ide.eclipse.longrunning.process;

import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.ACK_BAD;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.ACK_OK;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.BEGIN_COMMAND;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.CHANGE_DIR;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.COMMAND_DEPENDENCY_FILE;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.COMMAND_UNPARSED;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.CONSOLE_ERR;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.CONSOLE_OUT;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.END_COMMAND;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.EXIT;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.PROTOCOL_HEADER_LEN;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;

import org.grails.ide.api.GrailsConnector;
import org.grails.ide.api.GrailsToolingAPI;
import org.grails.ide.api.impl.GrailsToolingAPIImpl;
import org.grails.ide.eclipse.runtime.GrailsBuildSettingsDependencyExtractor;
import org.grails.ide.eclipse.runtime.GrailsEclipseConsole;
import org.grails.ide.eclipse.runtime.shared.longrunning.CommandInput;
import org.grails.ide.eclipse.runtime.shared.longrunning.FlushingPrintStream;
import org.grails.ide.eclipse.runtime.shared.longrunning.PrefixedOutputStream;
import org.grails.ide.eclipse.runtime.shared.longrunning.ProtocolException;
import org.grails.ide.eclipse.runtime.shared.longrunning.SafeProcess;


/**
 * Implements a 'remote' GrailsProcess that can be kept running to execute sequences of
 * Grails commands, without having to restart and reinitialise Grails each time.
 * <p>
 * Note that this process runs outside of Eclipse, in a separate JVM. It should not be
 * instantiated directly by Eclipse/STS plugins. See the {@link GrailsClient} and
 * {@link GrailsProcessManager} to create and interact with external GrailsProcess
 * instances.
 * 
 * @since 2.6
 * @author Kris De Volder
 */
public class GrailsProcess extends SafeProcess {
	
	// We will be redirecting the "System.XXX" streams... To avoid confusion, keep a reference to
	// the originals so we can always get to them...
	
	private static final PrintStream system_out = System.out;
	//private static final PrintStream system_err = System.err;
	private static final InputStream system_in = System.in;
	
	private static boolean DEBUG = false;
	
	private void debug(String msg) {
		if (DEBUG) {
			System.out.println("%debug "+msg);
		}
	}

	public static void main(String[] args) {
		System.out.println("Starting process...");
		try {
			int i = 0;
			while (i < args.length) {
				if (args[i].equals("--debug")) {
					DEBUG = true;
					i++;
				}
				else {
					i++;
				}
			}
			GrailsProcess process = new GrailsProcess();
			process.run();
			System.exit(0); //Make sure we exit, even if Grails may have started some non-daemon threads (Yes, it seems like it did and without
			//calling System.exit the process doesn't terminate.
		} catch (Throwable e) {
			e.printStackTrace(System.out);
			System.exit(-1);
		}
	}

	private BufferedReader commands;
	private GrailsToolingAPI API = GrailsToolingAPIImpl.INSTANCE; //TODO: Do we need a way to 'switch' API implementations?
	private GrailsConnector grails = null; // Set once we know we have determined the baseDir.
	
	public GrailsProcess() throws IOException {
        System.setOut(new FlushingPrintStream(new PrefixedOutputStream(CONSOLE_OUT, system_out)));
        System.setErr(new FlushingPrintStream(new PrefixedOutputStream(CONSOLE_ERR, system_out)));
        
		commands = new BufferedReader(new InputStreamReader(system_in));
		
        if (!DEBUG) {
        	//Killing the process after a timeout interferes with debugging and stepping
        	new HeartBeatMonitor().start();
        }
	}
	
	private void run() throws IOException {
		try {
			heartBeat();
			boolean loop = true;
			while (loop) {
				String line = commands.readLine();
				if (line.startsWith(BEGIN_COMMAND)) {
					ExternalGrailsCommand cmd = readCommand();
					CommandInput cmdInput = new CommandInput(commands);
					System.setIn(cmdInput.getInputStream());
					int code = -999;
					try {
						code = grails().executeCommand(cmd.getCommandLine(), new GrailsEclipseConsole());
						heartBeat();
						if (code==0) {
							writeDependencyFile(cmd);
						}
					} catch (Throwable e) {
						e.printStackTrace(System.err);
					} finally {
						System.setIn(system_in);
						println(END_COMMAND+code);
						cmdInput.terminate();
					}
				} else if (line.startsWith(CHANGE_DIR)) {
					File newBaseDir = new File(line.substring(PROTOCOL_HEADER_LEN));
					if (changeDir(newBaseDir)) {
						println(ACK_OK); 
					} else {
						println(ACK_BAD);
						System.exit(-1); //changing dir is too flaky, will need to shutdown and restart this process.
					}
				} else if (line.equals(EXIT)) {
					loop = false;
					println(ACK_OK);
				} else {
					throw new ProtocolException("Expecting "+BEGIN_COMMAND+" or "+EXIT+" but got '"+line+"'");
				}
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	private GrailsConnector grails() {
		if (grails==null) {
			//For now, it is assumed that the 'baseDir' is the directory the process was launched in.
			grails = API.connect(new File(System.getProperty("user.dir")));
		}
		return grails;
	}

	private boolean changeDir(File newBaseDir) {
		if (newBaseDir.equals(getCurrentBaseDir())) {
			return true; //already in correct dir... nothing to do.
		}
		//Either we have no connector yet, or it is connected to the wrong base directory.
		//so try to create a new connector for the newBaseDir. 
		//Note: implementation doesn't currently allow 'reconnecting' to a different directory.
		try {
			grails = API.connect(newBaseDir);
			return true;
		} catch (Exception e) {
			//e.printStackTrace(System.err);
			return false;
		}
	}

	private File getCurrentBaseDir() {
		if (grails!=null) {
			return grails.getBaseDir();
		}
		return null;
	}

	private void writeDependencyFile(ExternalGrailsCommand cmd) {
		String file = cmd.getDependencyFile();
		if (file!=null) {
			GrailsBuildSettingsDependencyExtractor extractor = new GrailsBuildSettingsDependencyExtractor(grails.getBuildSettings());
			try {
				extractor.writeDependencyFile(file);
			} catch (Exception e) {
				log(e);
			}
		}
	}


//	private void welcomeMessage(ExternalGrailsCommand script) {
//		println(CONSOLE_OUT+"Welcome to Grails "+ getGrailsVersion());
//		println(CONSOLE_OUT+"Using STS Longrunning Grails Process!");
//		println(CONSOLE_OUT+"command: "+script);
//	}
//
	
	private void log(Exception e) {
		e.printStackTrace(System.err);
	}

	private ExternalGrailsCommand readCommand() throws IOException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException {
		debug(">>> Reading command");
		ExternalGrailsCommand command = new ExternalGrailsCommand();		
		String line = commands.readLine();
		debug("input> "+line);
		while (line!=null && !line.startsWith(END_COMMAND)) {
//			if (line.startsWith(COMMAND_SCRIPT_NAME)) {
//				command.setScriptName(line.substring(PROTOCOL_HEADER_LEN));
//			} else if (line.startsWith(COMMAND_ARGS)) {
//				command.setArgs(line.substring(PROTOCOL_HEADER_LEN));
//			} else if (line.startsWith(COMMAND_ENV)) {
//				command.setEnv(line.substring(PROTOCOL_HEADER_LEN));
			if (line.startsWith(COMMAND_UNPARSED)) {
				command.parse(line.substring(PROTOCOL_HEADER_LEN));
			} else if (line.startsWith(COMMAND_DEPENDENCY_FILE)) {
				command.setDependencyFile(line.substring(PROTOCOL_HEADER_LEN));
			} else {
				throw new ProtocolException("Unexpected input: "+line);
			}
			line = commands.readLine();
			debug("input> "+line);
		}
		debug("<<< Reading command");
		return command;
	}
	
	

	private void println(String msg) {
		system_out.println(msg);
		system_out.flush();
	}
	
}
