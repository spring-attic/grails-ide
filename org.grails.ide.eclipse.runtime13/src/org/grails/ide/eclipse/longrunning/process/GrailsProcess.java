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
package org.grails.ide.eclipse.longrunning.process;

import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.ACK_BAD;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.ACK_OK;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.BEGIN_COMMAND;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.CHANGE_DIR;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.COMMAND_ARGS;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.COMMAND_DEPENDENCY_FILE;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.COMMAND_ENV;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.COMMAND_SCRIPT_NAME;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.COMMAND_UNPARSED;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.CONSOLE_ERR;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.CONSOLE_OUT;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.END_COMMAND;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.EXIT;
import static org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants.PROTOCOL_HEADER_LEN;
import grails.util.BuildSettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.codehaus.groovy.grails.cli.GrailsScriptRunner;
import org.grails.ide.eclipse.runtime.GrailsBuildSettingsDependencyExtractor;
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
	private static boolean IS_GRAILS_14 = false;
	
	private void debug(String msg) {
		if (DEBUG) {
			System.out.println("%debug "+msg);
		}
	}

	public static void main(String[] args) {
		try {
			IS_GRAILS_14 = true;
			int i = 0;
			while (i < args.length) {
				if (args[i].equals("--debug")) {
					DEBUG = true;
					i++;
//				} else if (args[i].equals("--is14")) {
					i++;
				}
				else {
					i++;
				}
			}
			GrailsProcess process = new GrailsProcess();
			process.run();
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private GrailsScriptRunner scriptRunner;
	private BuildSettings buildSettings;

	private BufferedReader commands;
	
	public GrailsProcess() throws IOException {
		saveSystemProperties();
		commands = new BufferedReader(new InputStreamReader(system_in));
		
        buildSettings = createBuildSettings();

        scriptRunner = new GrailsScriptRunner(buildSettings);
        System.setOut(new FlushingPrintStream(new PrefixedOutputStream(CONSOLE_OUT, system_out)));
        System.setErr(new FlushingPrintStream(new PrefixedOutputStream(CONSOLE_ERR, system_out)));
        
        File grailsHome = buildSettings.getGrailsHome();
        debug("Starting Remote Script runner for Grails " + buildSettings.getGrailsVersion());
        debug("Grails home is " + (grailsHome == null ? "not set" : "set to: " + grailsHome) + '\n');
        
        if (!DEBUG) {
        	//Killing the process after a timeout interferes with debugging and stepping
        	new HeartBeatMonitor().start();
        }
	}

	private BuildSettings createBuildSettings()  {
		BuildSettings buildSettings = null;
		// Get hold of the GRAILS_HOME environment variable if it is available.
        String grailsHome = System.getProperty("grails.home");

        // Now we can pick up the Grails version from the Ant project properties.
        try {
        	buildSettings = new BuildSettings(new File(grailsHome));
        	if (IS_GRAILS_14) {
        		//buildSettings.setRootLoader((URLClassLoader) GrailsScriptRunner.class.getClassLoader());
        		buildSettings.setRootLoader((URLClassLoader) GrailsProcess.class.getClassLoader());
        	}
        } catch (Exception e) {
            throw new Error("An error occurred loading the grails-app/conf/BuildConfig.groovy file: " + e.getMessage());
        }
        
        return buildSettings;
	}

	private void run() throws IOException {
		try {
			heartBeat();
	        System.setProperty("grails.disable.exit", "true");
			boolean loop = true;
			while (loop) {
				resetSystemProperties();
				String line = commands.readLine();
				if (line.startsWith(BEGIN_COMMAND)) {
					ExternalGrailsCommand cmd = readCommand();
					CommandInput cmdInput = new CommandInput(commands);
					System.setIn(cmdInput.getInputStream());
					int code = -999;
					try {
						code = executeCommand(cmd);
						heartBeat();
						if (code==0) {
							writeDependencyFile(cmd);
						}
					} finally {
						System.setIn(system_in);
						println(END_COMMAND+code);
						cmdInput.terminate();
					}
				} else if (line.startsWith(CHANGE_DIR)) {
					File current = buildSettings.getBaseDir();
					File newBaseDir = new File(line.substring(PROTOCOL_HEADER_LEN));
					//Note: we have to send back some "ack" code so the client can properly synchronize
					if (newBaseDir.equals(current)) {
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
	
	Map<String,String> savedSystemProps = null;
	
	private void saveSystemProperties() {
		try {
			savedSystemProps = new HashMap<String, String>();
			Properties currentProps = System.getProperties();
			Enumeration<?> enumeration = currentProps.propertyNames();
			while (enumeration.hasMoreElements()) {
				String prop = (String)enumeration.nextElement();
				savedSystemProps.put(prop, System.getProperty(prop));
			}
		} catch (Exception e) {
			savedSystemProps = null;
		}
	}
	
	private void resetSystemProperties() {
		if (savedSystemProps!=null) {
			Properties currentProps = System.getProperties();
			//1: clear any properties that got added since we saved...
			Enumeration<?> enumeration = currentProps.propertyNames();
			while (enumeration.hasMoreElements()) {
				String prop = (String)enumeration.nextElement();
				if (!savedSystemProps.containsKey(prop)) {
					System.clearProperty(prop);
				}
			}
			//2: reset values of any properties that were set when we saved
			for (Entry<String, String> entry : savedSystemProps.entrySet()) {
				if (entry.getValue()!=null) {
					System.setProperty(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	private int executeCommand(ExternalGrailsCommand script) {
		welcomeMessage(script);
		int code;
		if (script.env==null) {
			code = scriptRunner.executeCommand(script.scriptName, script.args);
		} else {
			code = scriptRunner.executeCommand(script.scriptName, script.args, script.env);
		}
		return code;
	}

	private void welcomeMessage(ExternalGrailsCommand script) {
		println(CONSOLE_OUT+"Welcome to Grails "+ buildSettings.getGrailsVersion());
		println(CONSOLE_OUT+"Using STS Longrunning Grails Process!");
		println(CONSOLE_OUT+"script and args: "+script);
	}

	private void writeDependencyFile(ExternalGrailsCommand cmd) {
		String file = cmd.getDependencyFile();
		if (file!=null) {
			GrailsBuildSettingsDependencyExtractor extractor = new GrailsBuildSettingsDependencyExtractor(buildSettings);
			try {
				extractor.writeDependencyFile(file);
			} catch (Exception e) {
				log(e);
			}
		}
	}

	private void log(Exception e) {
		e.printStackTrace(System.err);
	}

	private ExternalGrailsCommand readCommand() throws IOException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException {
		debug(">>> Reading command");
		ExternalGrailsCommand command = new ExternalGrailsCommand();		
		String line = commands.readLine();
		debug("input> "+line);
		while (line!=null && !line.startsWith(END_COMMAND)) {
			if (line.startsWith(COMMAND_SCRIPT_NAME)) {
				command.setScriptName(line.substring(PROTOCOL_HEADER_LEN));
			} else if (line.startsWith(COMMAND_ARGS)) {
				command.setArgs(line.substring(PROTOCOL_HEADER_LEN));
			} else if (line.startsWith(COMMAND_ENV)) {
				command.setEnv(line.substring(PROTOCOL_HEADER_LEN));
			} else if (line.startsWith(COMMAND_UNPARSED)) {
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
