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
package org.grails.ide.eclipse.test.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.grails.ide.eclipse.test.util.GrailsTest;
import org.springsource.ide.eclipse.commons.core.ZipFileUtil;
import org.springsource.ide.eclipse.commons.core.process.StandardProcessRunner;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;



/**
 * Grails deployment code relies on the {@link ZipFileUtil} to explode war file created by Grails.
 * <p>
 * Here we test that some requirements to make that work well are being met.
 * 
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class ZipFileUtilTest extends GrailsTest {
	
	private static final int MINUTE = 60000; //milliseconds in a minute
	private static final double SECOND = 1000;

	/**
	 * Timestamps should be preserved.
	 */
	public void testTimeStamps() throws Exception {
		File workDir = StsTestUtil.createTempDirectory();
		File toZip = new File(workDir, "toZip");
		toZip.mkdir();
		File[] testFiles = new File[4];
		long now = System.currentTimeMillis();
		for (int i = 0; i < testFiles.length; i++) {
			File f =  new File(toZip, "file"+i);
			FileWriter writer = new FileWriter(f);
			try {
				writer.write("Test file # "+i);
			} finally {
				writer.close();
			}
			f.setLastModified(now-(1+i*5)*MINUTE); //Pretend it was modified some time ago to make this a reasonable test (otherwise test may pass because it runs so fast)
			testFiles[i] = f;
		}
		
		File zipFile = new File(workDir, "test.zip");
		zip(toZip, zipFile);
		
		File unzipped = new File(workDir, "unzipped");
		ZipFileUtil.unzip(zipFile.toURL(), unzipped, "toZip", null);
		
		// Verify time stamps on the test files
		for (int i = 0; i < testFiles.length; i++) {
			File orgFile = testFiles[i];
			File newFile = new File(unzipped, "file"+i);
			System.out.println(newFile);
			System.out.println("expect: "+orgFile.lastModified());
			System.out.println("found : "+newFile.lastModified());
			//1 second accuracy is guaranteed, nothing more. So we check for a margin of 2 seconds.
			assertEquals(orgFile.lastModified(), newFile.lastModified(), (double)2*SECOND); 
		}
		
	}

	private void zip(File dirToZip, File zipFile) throws IOException, InterruptedException {
		StandardProcessRunner runner = new StandardProcessRunner();
		int result = runner.run(dirToZip.getParentFile(), "zip", "-r", zipFile.getAbsolutePath(), dirToZip.getName());
		assertEquals(0, result);
	}

}
