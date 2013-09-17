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
package org.grails.ide.eclipse.commands.test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.grails.ide.eclipse.runtime.shared.DependencyData;
import org.grails.ide.eclipse.runtime.shared.DependencyFileFormat;


/**
 * Test reading and writing of dependency file format.
 * 
 * This test should be runnable as a simple JUnit test (it doesn't require STS/Eclipse infrastructure.
 * In fact, it it does there is a problem since the functionality being tested should be runable outside
 * of STS (part of external Grails process).
 * 
 * @author Kris De Volder
 * @since 2.6.M2
 */
public class DependencyFileFormatTest extends TestCase {
	
	/**
	 * Basic test, put some data into a data object, write it out,
	 * read it back, compare for equality.
	 * <p>
	 * No special characters or boundary cases being exercised.
	 */
	public void testWriteDataAndReadItBack() throws Exception {
		DependencyData data = makeTestData(
				//pluginSourceFolders
				new String[] {
						"/foo/bar/src",
						"/foo/zor/src"
				},
				//dependencies
				new String[] {
						"/home/kdvolder/.ivy2/bork.jar",
						"/home/kdvolder/.ivy2/zazazee.jar",
						"/home/kdvolder/.ivy2/nananan.jar",
				},
				//workDirFile
				"/home/.grails/1.3.6", 
				//pluginsDirectoryFile
				"/blah/blah/.plugins",
				//pluginXmlFiles
				new String[] {
						"/bo/.plugins/boingPlugin.xml",
						"/lalala/bralsl/.plugins/jajaja.xml"
				},
				"/home/.grails/1.3.7/foo/plugin-classes"
		);
		roundTrip(data);
	}

	private void roundTrip(DependencyData data) throws IOException {
		File dataFile = File.createTempFile("testData", ".txt");
		DependencyFileFormat.write(dataFile, data);
		DependencyData readData = DependencyFileFormat.read(dataFile);
		assertEquals(data, readData);
	}
	
	/**
	 * Throw some backslashes and spaces in the mix.
	 */
	public void testDataWithSpacesAndBackslashes() throws Exception {
		DependencyData data = makeTestData(
				//pluginSourceFolders
				new String[] {
						"C:\\Document and Settings\\foo\\bar\\src",
						"/foo/zor/src"
				},
				//dependencies
				new String[] {
						"C:\\Document and Settings\\jars\\bork.jar",
						"/home/kdvolder/.ivy2/zazazee.jar",
						"/home/kdvolder/.ivy2/nananan.jar",
				},
				//workDirFile
				"C:\\Document and Settings\\.grails\\1.3.6", 
				//pluginsDirectoryFile
				"C:\\Document and Settings\\.plugins",
				//pluginXmlFiles
				new String[] {
						"C:\\Document and Settings\\bo/.plugins/boingPlugin.xml",
						"C:\\Document and Settings\\lalala/bralsl/.plugins/jajaja.xml"
				},
				"C:\\Document and Settings\\plugin-classes"
		);
		roundTrip(data);
	}
	
	public void testDataWithEscapeSequence() throws Exception {
		DependencyData data = makeTestData(
				//pluginSourceFolders
				new String[] {
						"C:\\Document and Settings\\foo\\bar\\src",
						"/foo/zor/src\\n"
				},
				//dependencies
				new String[] {
						"C:\\Document\\n and Settings\\jars\\bork.jar",
						"/home/kdvolder/.ivy2/zazazee.jar",
						"/home/kd\\rvolder/.ivy2/nananan.jar",
				},
				//workDirFile
				"\\\\blah", 
				//pluginsDirectoryFile
				"\\nblah",
				//pluginXmlFiles
				new String[] {
						"C:\\Document and Setti\\\nngs\\bo/.plugins/boingPlugin.xml",
						"C:\\Document and Settings\\lalala/bralsl/.plugins/jajaja.xml"
				},
				"C:\\Document and\\r Settings\\plugin-classes"
		);
		roundTrip(data);
	}
	
	
	public void testDataWithEmptySets() throws Exception {
		DependencyData data = makeTestData(
				//pluginSourceFolders
				new String[] {},
				//dependencies
				new String[] {},
				//workDirFile
				"C:\\Document and Settings\\.grails\\1.3.6", 
				//pluginsDirectoryFile
				"C:\\Document and Settings\\.plugins",
				//pluginXmlFiles
				new String[] {},
				//pluginClassesDir
				"C:\\Document and Settings\\plugin-classes"
				
		);
		roundTrip(data);
	}

	public void testDataWithNullPointers() throws Exception {
		DependencyData data = makeTestData(
				//pluginSourceFolders
				null,
				//dependencies
				null,
				//workDirFile
				"null pointer not allowed here", 
				//pluginsDirectoryFile
				"null pointer not allowed here", 
				//pluginXmlFiles
				null,
				//pluginClassesDir
				"null pointer not allowed here"
		);
		roundTrip(data);
	}
	
	public void testDataWithControllCharacters() throws Exception {
		DependencyData data = makeTestData(
				//pluginSourceFolders
				new String[] {
						"/foo/bar/\n\t\rsrc",
						"/foo/\r\rzor/src"
				},
				//dependencies
				new String[] {
						"/home/kdv\t\tolder/.ivy2/bork.jar",
						"/home/kdvol\r\nder/.ivy2/zazazee.jar",
						"/home/kdvol\nder/.ivy2/nananan.jar",
				},
				//workDirFile
				"/home/.grails/1.3.6", 
				//pluginsDirectoryFile
				"/blah/blah/.plugins",
				//pluginXmlFiles
				new String[] {
						"/bo/.plug\r\r\nins/boingPlugin.xml",
						"/lalala/bra\nlsl/.plugins/jajaja.xml"
				},
				//pluginClassesDir
				"/home/.grails/1.3.7/f\r\roo/plugin-classes"
		);
		roundTrip(data);
	}

	private DependencyData makeTestData(String[] pluginSourceFolders, String[] dependencies,
			String workDirFile, String pluginsDirectoryFile, String[] pluginXmlFiles, String pluginClassesDir) {
		return new DependencyData(
				toSet(pluginSourceFolders), 
				toSet(dependencies), 
				workDirFile, 
				pluginsDirectoryFile, 
				toSet(pluginXmlFiles),
				pluginClassesDir,
				DependencyData.UNKNOWN_PORT
		);
	}

	private Set<String> toSet(String[] elements) {
		if (elements!=null) {
			return new LinkedHashSet<String>(Arrays.asList(elements));
		} 
		return null;
	}
	
}
