/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.longrunning.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * Utility to get a stacktrace from an external grails process by calling jps and jstack.
 * 
 * @author Kris De Volder
 */
public class GrailsProcessStackTracer {

	/**
	 * Reads input from process and keeps it all until someone asks for it.
	 */
	public class OutputFetcher extends Thread {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		private boolean killed = false;
		private byte[] buffer = new byte[1024 * 4];
		private InputStream in;
		
		public OutputFetcher(InputStream in) {
			this.start();
			this.in = in;
		}
		
		@Override
		public void run() {
			try {
				while (!killed) {
					//Don't block if no input is available or the thread may hang indefinitely
					int count = in.read(buffer);
					if (count == -1) {
						//end reached
						killed = true;
					} else if (count > 0) {
						//Got some data
						output.write(buffer, 0, count);
					}
				}
			} catch (IOException e) {
				// exeptions will implicitly kill the thread.
			}
		}
		
		@Override
		public String toString() {
			return output.toString();
		}

		public void kill() {
			this.killed = true;
		}
	}

	/**
	 * @return pid of GrailsStarter process or -1 if no such process is found.
	 */
	int getGrailsProcessId() throws IOException {
		String jpsOut = exec("jps");
		System.out.println(jpsOut);
		String[] lines = jpsOut.split("[\\r\\n]+");
		int found = -1;
		for (int i = 0; i < lines.length && found < 0; i++) {
			try {
				String[] pieces = lines[i].split("\\s+");
				int pid = Integer.valueOf(pieces[0]);
				String name = pieces[1];
				if (name.contains("GrailsStarter")) {
					found = pid;
				}
			} catch (Exception e) {
				//Some unexpected doesn't parse? Ignore
			}
		}
		return found;
	}
	
	public String getStackTraces() throws IOException {
		int pid = getGrailsProcessId();
		if (pid>=0) {
			return "Dumping a stacktrace before killing hanging process ...\n" + exec("jstack", ""+pid);
		} else {
			return "Couldn't collect a stacktrace because Grails process was not found";
		}
	}
	
	private String exec(String... command) throws IOException {
		boolean done = false;
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectErrorStream(true);
		Process p = pb.start();
		OutputFetcher output = new OutputFetcher(p.getInputStream());
		try {
			done = false;
			while (!done) {
				try {
					p.waitFor();
					done = true;
				} catch (InterruptedException e) {
				}
			}
		} finally {
			//Ensure the thread stops spinning after process is dead.
			output.kill();
		}
		done = false;
		while (!done) {
			try {
				output.join();
				done = true;
			} catch (InterruptedException e) {
			}
		}
		return output.toString();
	}
	
	public static void main(String[] args) throws IOException {
		int pid = new GrailsProcessStackTracer().getGrailsProcessId();
		System.out.println("Grails process id = "+pid);
	}

}
