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
package org.grails.ide.eclipse.longrunning.client;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.launch.ClasspathLocalizer;
import org.grails.ide.eclipse.core.launch.EclipsePluginClasspathEntry;
import org.grails.ide.eclipse.core.launch.GrailsLaunchArgumentUtils;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;
import org.grails.ide.eclipse.longrunning.Console;
import org.grails.ide.eclipse.longrunning.GrailsProcessManager;
import org.grails.ide.eclipse.runtime.shared.longrunning.GrailsProcessConstants;
import org.grails.ide.eclipse.runtime.shared.longrunning.ProtocolException;


/**
 * A client that is able to send request to execute Grails commands to
 * an external grails process. (Note that the class GrailsProcessConstants itself which 
 * implements the process, shouldn't be instantiated since it runs on an external JVM).
 * <p>
 * Normally you shouldn't instantiate GrailsClient instances directly. Instead use {@link GrailsProcessManager} to obtain client instances.
 * 
 * @author Kris De Volder
 * @author Andy Clement
 * @since 2.6
 */
public class GrailsClient {
	
	/**
	 * Set this to a value other than null to add debugging stuff to the command line to 
	 * start the GrailsProcessConstants that this client connects to in debugging mode. To be able to 
	 * actually debug it, you will need to create and start a remote debugging launch configuration 
	 * in Eclipse.
	 * <p>
	 * Uncomment one of the tree choices below.
	 * <p>
	 * Option 1: debuggin disabled.
	 * <p> 
	 * Option 2: Enable debug process in 'server' mode. This means that the debugged process itself
	 * is a 'server' and the debugger (Remote Eclipse debugging launch conf) connects to it.
	 * <p>
	 * Option 3: Enable debug process in 'client' mode. This means that the debugger is a server, and
	 * the debugged process connects to it as a client.
	 * <p>
	 * To use this you must create an Eclipse remote debugging launch configuration and change it
	 * to a 'Standard socket listening' setting. The launched process will attempt to connect 
	 * to this remote debugging session when it starts.
	 * <p>
	 * Note that if the process is running in 'client' mode but a corresponding debug process isn't
	 * started in Eclipse, then the GrailsProcess will fail to start.
	 * <p>
	 * See also http://www.grails.org/GrailsDevEnvironment for a bit more info on debugging
	 * Grails.
	 */
	public static String DEBUG_PROCESS = null; //Disabled
	//public static final String DEBUG_PROCESS = "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005";
	//public static final String DEBUG_PROCESS = "-Xrunjdwp:transport=dt_socket,server=n,address=8000";
	
	/**
	 * When this flag is set we will echo anything sent from the external process to the client 
	 * or vice versa onto System.out.
	 */
	private static final boolean DEBUG_PROTOCOL = false;
	
	/**
	 * When this flag is set to true, the client will produce some debugging output onto system
	 * out.
	 */
	private static final boolean DEBUG_CLIENT = false;

	/**
	 * Polling interval used to check for data coming from the process.
	 * 
	 * If this time is longer the process will become less responsive	
	 * but if it is shorter it will be taking more CPU polling for input.
	 */
	public static final long POLLING_INTERVAL = 300;
	
	/**
	 * Time stamps of files that, when changed, mean we need to restart the external process.
	 */
	private long[] timeStamps;

	/**
	 * Call this to print messages about
	 * @param string
	 */
	private void debug_client(String string) {
		if (DEBUG_CLIENT) {
			System.out.println(string);
		}
	}

	static void debug_protocol(String string) {
		if (DEBUG_PROTOCOL) {
			System.out.println(string);
		}
	}
	
	private Process process; 

	// Design note: to interact with the Reader's and Writer's below, do not reference them directly!
	// use the println and readLine methods in this class instead.
	
	private LineReader  fromProcess; 	// Read this to get the output from the process
	private PrintWriter toProcess;		// Write to this to send something to the process
	private PrintWriter toConsoleOut;	// Write to this to show regular output to the user.
	private PrintWriter toConsoleErr;	// Write to this to show errors output to the user.
	
	//	private BufferedReader fromConsole; // Read from this to get input from the user.
	// Why the above is removed? Because Grails implicitly uses System.in, not much we can do about that.

	private IGrailsInstall install;

	private File workingDir;

	private String javaVM;

	

	public GrailsClient(IGrailsInstall install, File workingDir) {
		this.install = install;
		this.workingDir = workingDir;
	}
	
	private void init() throws IOException {
		String grailsHome = install.getHome();
		
		List<String> args = new ArrayList<String>();

		args.add(javaVM());

		if (DEBUG_PROCESS!=null) {
			args.add(DEBUG_PROCESS);
		}

		//VM ARGS
		args.add("-Xmx512M");
		args.add("-XX:MaxPermSize=192m");
		args.add("-classpath");
		args.add(bootstrapClassPath(install));
		
		// System properties
		Map<String, String> systemProps = GrailsCoreActivator.getDefault().getLaunchSystemProperties();
		GrailsLaunchArgumentUtils.setMaybe(systemProps, "file.encoding", "UTF-8");
		GrailsLaunchArgumentUtils.setMaybe(systemProps, "grails.home", grailsHome);
		GrailsLaunchArgumentUtils.setMaybe(systemProps, "tools.jar", toolsJar());

		for (Entry<String, String> entry : systemProps.entrySet()) {
			String key = entry.getKey();
			Assert.isTrue(!key.contains("="), "Property "+key+" contains '='");
			args.add("-D"+key+"="+entry.getValue());
		}

		// Main class
		args.add("org.codehaus.groovy.grails.cli.support.GrailsStarter");

		// Program args
		args.add("--main");
		args.add(GrailsProcessConstants.PROCESS_CLASS_NAME);
		args.add("--conf");
		args.add(grailsHome+"/conf/groovy-starter.conf");
		args.add("--classpath");
		args.add(neededPlugins());
		if (DEBUG_PROCESS!=null) {
			args.add("--debug");
		}
		if (install.getVersion().compareTo(GrailsVersion.V_2_0_0)>=0) {
			args.add("--is14");
		}

		debug_client(">>> Grails exec args");
		for (String string : args) {
			debug_client(string);
		}
		debug_client("<<< Grails exec args");

		ProcessBuilder processBuilder = new ProcessBuilder(args);
		processBuilder.directory(workingDir);
		processBuilder.redirectErrorStream(true);

		process = processBuilder.start();
		fromProcess = new LineReader(process.getInputStream(), DEBUG_PROTOCOL);
		toProcess = new PrintWriter(process.getOutputStream());
		timeStamps = getTimeStamps();
		Assert.isTrue(isRunning());
	}

	private long[] getTimeStamps() {
		return new long[] {
			getTimeStamp("application.properties"),
			getTimeStamp("grails-app/conf/BuildConfig.groovy")
		};
	}

	/**
	 * Gets the timestamp of a file, with path relative to the processes workingdir
	 */
	private long getTimeStamp(String fileName) {
		File file = new File(workingDir, fileName);
		if (file.exists()) {
			return file.lastModified();
		}
		return 0;
	}

	public void changeDir(File newDir) throws IOException, TimeoutException {
		workingDir = newDir;
		if (isRunning()) {
			//This will ensure that if process is running it assumes the correct dir... or shuts down
			println(toProcess, GrailsProcessConstants.CHANGE_DIR+newDir.getCanonicalPath());
			String ack = fromProcess.readLine(POLLING_INTERVAL+20000);
			if (GrailsProcessConstants.ACK_BAD.equals(ack)) {
				waitFor(process); // We expect the process to shutdown soon.
			} else if (GrailsProcessConstants.ACK_OK.equals(ack)) {
				// fine
			} else {
				throw new ProtocolException("Expected an 'ack' but got:\n"+ack);
			}
		}
	}

	/**
	 * Is the assocated process running?
	 */
	public boolean isRunning() {
		if (process!=null) {
			try {
				process.exitValue();
				return false;
			}
			catch (IllegalThreadStateException e) {
				return true; // this exception indicates it isn't terminated yet.
			}
		}
		return false;
 	}
	
	/**
	 * Determines where the output from this process will be sent by default (unless overriden
	 * by parameter to executeCommand.
	 * 
	 * @param out
	 */
	public void setDefaultConsole(OutputStream out) {
		toConsoleOut = out == null ? null : new PrintWriter(out);
		toConsoleErr = out == null ? null : toConsoleOut;
	}
	
	private String neededPlugins() throws IOException {
		List<String> entries = GrailsLaunchArgumentUtils.getBuildListenerClassPath(install.getVersion());
		return GrailsLaunchArgumentUtils.toPathsString(entries);
	}

	private String javaVM() {
		if (javaVM==null) {
			IVMInstall vm = JavaRuntime.getDefaultVMInstall();
			File jrePath = vm.getInstallLocation();
			//TODO: this probably doesn't work on all platforms.
			File javaExePath = new File(new File(jrePath, "bin"), "java");
			if (!javaExePath.exists()) {
				// Maybe on windows?
				javaExePath = new File(new File(jrePath, "bin"), "javaw.exe");
			}
			Assert.isTrue(javaExePath.exists());
			javaVM = javaExePath.getAbsolutePath();
		}
		return javaVM;
	}

	private String toolsJar() {
		IVMInstall vm = JavaRuntime.getDefaultVMInstall();
		File jrePath = vm.getInstallLocation();
		File toolsPath = new File(new File(jrePath, "lib"), "tools.jar");
		return toolsPath.getAbsolutePath();
	}

	private String bootstrapClassPath(IGrailsInstall install) {
		StringBuffer buf = new StringBuffer();
		File[] entries = install.getBootstrapClasspath();
		for (int i = 0; i < entries.length; i++) {
			if (i>0) {
				buf.append(File.pathSeparatorChar);
			}
			buf.append(entries[i]);
		}
		return buf.toString();
	}

//	/**
//	 * Executes a given command remotely redirecting output from this command to a given output Stream.
//	 * <p>
//	 * If null is passed as the outputStream, then executeCommand will send output to the default
//	 * console instead (if it is set). 
//	 * @throws TimeoutException 
//	 */
//	public synchronized int executeCommand(String scriptName, String args, OutputStream out, long timeOut) throws IOException, TimeoutException {
//		PrintWriter savedConsole = toConsole;
//		try {
//			if (out!=null) {
//				toConsole = new PrintWriter(out);
//			}
//			toProcess.println(GrailsProcessConstants.COMMAND_SCRIPT_NAME	+scriptName);
//			toProcess.println(GrailsProcessConstants.COMMAND_ARGS			+args);
//			toProcess.println(GrailsProcessConstants.END_COMMAND);
//			toProcess.flush(); //Must call 'flush or output may remain buffered-up and not processed by 
//			// the GrailsProcessConstants. This will cause both the client and the GrailsProcessConstants to hang waiting for 
//			// input that is not coming.
//			return getCommandResult(timeOut);
//		}
//		finally {
//			flush(toConsole); //Ensure all output is written before proceeding
//			toConsole = savedConsole;
//		}
//	}
//	
	/**
	 * Executes a given command remotely redirecting output from this command to a given output Stream.
	 * <p>
	 * If null is passed as the outputStream, then executeCommand will send output to the default
	 * console instead (if it is set). 
	 * @throws TimeoutException 
	 */
	public synchronized int executeCommand(GrailsCommand cmd, Console console) throws IOException, TimeoutException {
		restartProcessIfNeeded();
		Assert.isTrue(isRunning(), "The external Grails process is no longer running (crashed?)");
		PrintWriter savedToConsole = toConsoleOut;
		try {
//			checkSystemProps(cmd); // check disabled, all properties are now supported
			if (console!=null) {
				toConsoleOut = new PrintWriter(console.getOutputStream());
				toConsoleErr = new PrintWriter(console.getErrorStream());
			}
			println(toProcess, GrailsProcessConstants.BEGIN_COMMAND);
			println(toProcess, GrailsProcessConstants.COMMAND_UNPARSED + cmd.getCommand());
			File depFile = cmd.getDependencyFile();
			if (depFile !=null) {
				println(toProcess, GrailsProcessConstants.COMMAND_DEPENDENCY_FILE+depFile.getCanonicalPath());
			}
			println(toProcess, GrailsProcessConstants.END_COMMAND);
			toProcess.flush(); //Must call 'flush or output may remain buffered-up and not processed by 
			// the GrailsProcessConstants. This will cause both the client and the GrailsProcessConstants to hang waiting for 
			// input that is not coming.
			
			SendCommandInput sendInput = new SendCommandInput(this, console.getInputStream(), toProcess);
			try {
				return getCommandResult(cmd.getGrailsCommandTimeOut());
			} finally {
				sendInput.terminate();
			}
		} catch (TimeoutException e) {
			e.printStackTrace();
			//Process is probably stuck, should be killed
			shutDown();
			throw e;
		}
		finally {
			flush(toConsoleOut); //Ensure all output is written before proceeding
			toConsoleOut = savedToConsole;
		}
	}

	/**
	 * Checks variety of conditions that require the external process to be restarted.
	 */
	private void restartProcessIfNeeded() throws IOException {
		debug_client("Checking external process");
		if (!isRunning()) {
			debug_client("External process not running. Starting it");
			init();
		} else {
			// process is running, but maybe it is stale?
			long[] newStamps = getTimeStamps();
			if (!Arrays.equals(timeStamps, newStamps)) {
				debug_client("Timestamps changed, process needs to be restarted");
				shutDown();
				init();
			}
		}
	}

	private void flush(PrintWriter out) {
		if (out!=null) {
			out.flush();
		}
	}

	private int getCommandResult(long timeOut) throws IOException, TimeoutException {
		if (DEBUG_PROCESS!=null) {
			timeOut = timeOut * 30; // Otherwise with breakpoints in the process, it will timeout
		}
		String line = fromProcess.readLine(timeOut);
		while (line!=null && !line.startsWith(GrailsProcessConstants.END_COMMAND)) {
			if (line.startsWith(GrailsProcessConstants.CONSOLE_OUT)) {
				println(toConsoleOut, line.substring(GrailsProcessConstants.PROTOCOL_HEADER_LEN));
			} else if (line.startsWith(GrailsProcessConstants.CONSOLE_ERR)) {
				println(toConsoleErr, line.substring(GrailsProcessConstants.PROTOCOL_HEADER_LEN));
			}
			line = fromProcess.readLine(timeOut);
		}
		if (line==null) {
			//That really shouldn't be happening!
			// But it might if something goes wrong that makes the grails process crash.
			return -1;
		}
		return Integer.valueOf(line.substring(GrailsProcessConstants.PROTOCOL_HEADER_LEN));
	}
	
	void println(PrintWriter out, String line) {
		if (out!=null) {
			if (toProcess==out) {
				debug_protocol("send>>> "+line);
			}
			out.println(line);
			out.flush();
		}
	}

	/**
	 * Calling this method ensures that the external process associated with this client is terminated.
	 * <p>
	 * Calling this method when the process is already terminated has no effect.
	 */
	public synchronized void shutDown() {
		//Ask the process (nicely) to terminate
		println(toProcess, GrailsProcessConstants.EXIT);
		flush(toProcess);
		try {
			String ack = fromProcess.readLine(POLLING_INTERVAL+300);
			if (GrailsProcessConstants.ACK_OK.equals(ack)) {
				waitFor(process);
				process = null;
			}
		} catch (TimeoutException e) {
			//This somewhat expected, when some Grails command caused Grails to block, waiting for user input
			// don't log it.
		} catch (Exception e) {
			GrailsCoreActivator.log(e);
		} finally {
			if (process!=null) {
				// Whatever happened, timeout, nonsense response...
				// try your best to shut this process down.
				process.destroy();
				process = null; 
			}
		}
	}

	private void waitFor(Process process) {
		boolean done = false;
		while (!done) {
			try {
				process.waitFor();
				done = true;
			} catch (InterruptedException e) {
				//Ignore this and keep waiting until it really terminates
			}
		}
	}

	public IGrailsInstall getInstall() {
		return install;
	}
	
}
