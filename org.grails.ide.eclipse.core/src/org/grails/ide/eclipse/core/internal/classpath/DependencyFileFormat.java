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
package org.grails.ide.eclipse.core.internal.classpath;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * The purpose of this class is to segregate all the code relating to the "DependencyFileFormat".
 * The "DependencyFile" is a file that is written out by some class injected into an external 
 * grails process and later read by STS to initialise the classpath container.
 * <p>
 * This class provides methods for reading and writing the file. This class is classloaded from
 * both the external Grails process and STS, as such, to be safe, it should be depending only
 * on some low-level Java libraries that are readily available in any Java environment and
 * pose little risk of being compromised by modifications to the Grails execution environment
 * that are beyond our control (e.g. see STS-1530 where installing a Grails plugin can cause
 * our use of the JAXP library to write the dependency file to fail).
 */
public class DependencyFileFormat {

	//////////////// Writing //////////////////////////////////////////////////////////////////////////////

	private static class DepWriter {

		private OutputStream out;

		public DepWriter(File file) throws FileNotFoundException {
			out = new BufferedOutputStream(new FileOutputStream(file));
		}

		public void write(String header, Set<String> entries) throws IOException {
			println(header);
			println(""+entries.size());
			for (String entry : entries) {
				println(entry);
			}
		}

		private void write(String header, String entry) throws IOException {
			println(header);
			println(entry);
		}

		public void close() {
			try {
				out.close();
			} catch (IOException e) {
				//Ignore...
			}
		}

		/**
		 * Writes a String to a line of the file escaping newline characters.
		 * <p>
		 * We only use '\n' as line terminator, regardless of platform.
		 */
		private void println(String line) throws IOException {
			byte[] bytes = line.getBytes();
			for (byte b : bytes) {
				if (b=='\n' || b=='\\') {
					out.write('\\');
				}
				out.write(b);
			}
			out.write('\n');
			out.flush();
		}

	}

	public static void write(File file, DependencyData data) throws IOException {
		DepWriter w = null;
		try {
			w =new DepWriter(file);
			w.write("#dependencies", data.getDependencies());
			w.write("#sources", data.getSources());
			w.write("#workDir", data.getWorkDir());
			w.write("#plugin descriptors", data.getPluginDescriptors());
			w.write("#plugins directory", data.getPluginsDirectory());
			w.write("#plugin classes dir", data.getPluginClassesDirectory());
		} finally {
			if (w!=null) {
				w.close();
			}
		}
	}
	
	///////////////////////// Reading //////////////////////////////////////////////////////////////

	public static class DepReader {

		InputStream in;
		InfiniteByteBuffer buf = new InfiniteByteBuffer();

		public DepReader(File file) throws FileNotFoundException {
			in = new BufferedInputStream(new FileInputStream(file));
		}

		public Set<String> readSet(String expectHeader) throws IOException {
			readHeader(expectHeader);
			Set<String> set = new LinkedHashSet<String>();
			int size = readInt();
			for (int i = 0; i < size; i++) {
				set.add(readln());
			}
			return set;
		}

		private void readHeader(String expectHeader) throws IOException {
			String header = readln();
			if (!expectHeader.equals(header)) {
				throw new IOException("DependencyFileFormat expected "+expectHeader+" but found "+header);
			}
		}

		private String readln() throws IOException {
			buf.clear();
			while (true) {
				byte b = readByte();
				if (b=='\\') {
					b = readByte();
				} else if (b=='\n') {
					break;
				}
				buf.add(b);
			}
			return buf.getString();
		}

		private byte readByte() throws IOException {
			int b = in.read();
			if (b<0) {
				throw new EOFException();
			}
			return (byte)b;
		}

		private int readInt() throws IOException {
			try {
				return Integer.parseInt(readln());
			} catch (NumberFormatException e) {
				throw new IOException("DependencyDataFileFormat");
			}
		}

		public String readString(String expectHeader) throws IOException {
			readHeader(expectHeader);
			return readln();
		}

		public void close() {
			try {
				in.close();
			} catch (IOException e) {
				//Ignore ...
			}
		}
	}

	public static DependencyData read(File file) throws IOException {
		DepReader r = null;
		try {
			r = new DepReader(file);
			Set<String> dependencies = r.readSet("#dependencies");
			Set<String> pluginSourceFolders = r.readSet("#sources");
			String workDirFile = r.readString("#workDir");
			Set<String> pluginXmlFiles = r.readSet("#plugin descriptors");
			String pluginsDirectoryFile = r.readString("#plugins directory");
			String pluginClassesDir = r.readString("#plugin classes dir");
			return new DependencyData(pluginSourceFolders, dependencies, workDirFile, pluginsDirectoryFile, pluginXmlFiles, pluginClassesDir);
		} finally {
			if (r!=null) {
				r.close();
			}
		}
	}
	
	/////////// Util ///////////////////////////////////////////////////////////
	
	/**
	 * Byte buffer that grows in size to accomodate what is put in it.
	 */
	public static class InfiniteByteBuffer {
		
		private static final int SIZE_INCREMENT = 128;
		private int i = 0;
		
		private byte[] bytes = new byte[SIZE_INCREMENT];
		
		/**
		 * Make buffer empty, so it can be reused.
		 */
		public void clear() {
			i = 0;
		}
		
		public void add(byte b) {
			if (i>=bytes.length) {
				grow();
			}
			bytes[i++] = b;
		}

		private void grow() {
			byte[] newBytes = new byte[bytes.length+SIZE_INCREMENT];
			System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
			bytes = newBytes;
		}

		/**
		 * Convert current contents of byte buffer to String using default character encoding.
		 */
		public String getString() {
			return new String(bytes, 0, i);
		}
		

		/**
		 * For easier debugging
		 */
		@Override
		public String toString() {
			return getString();
		}
	}
	

}
