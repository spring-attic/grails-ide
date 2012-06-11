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
package org.grails.ide.eclipse.runonserver.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.springsource.ide.eclipse.commons.configurator.ServerHandler;
import org.springsource.ide.eclipse.commons.configurator.ServerHandlerCallback;
import org.springsource.ide.eclipse.commons.core.FileUtil;
import org.springsource.ide.eclipse.commons.core.ZipFileUtil;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

import com.springsource.sts.internal.server.tc.core.TcServer;

/**
 * This TcServerFixture was based on a copy of Steffen Pingel's com.springsource.sts.server.tc.tests.support.TcServerFixture
 * in the "com.springsource.sts.server.tc.tests" plugin. 
 * <p>
 * Copied here to allow more freely modifying the test fixture without fear of impacting Steffen's tests.
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class TcServerFixture {

	public static String INST_INSIGHT = "spring-insight-instance";

	public static String INST_SEPARATE = "separate-instance";

	public static String INST_COMBINED = "combined-instance";

//	public static TcServerFixture V_6_0 = new TcServerFixture("com.springsource.tcserver.60", "tcServer-6.0");
//
//	public static TcServerFixture V_2_5 = new TcServerFixture("com.springsource.tcserver.70",
//			"tc-server-developer-2.5.0.RELEASE");
//
//	public static TcServerFixture V_2_0 = new TcServerFixture("com.springsource.tcserver.60",
//			"tc-server-developer-2.0.0.SR01");

	private final String serverType;
	private File tcServerStub; // Points to unzipped TcServer distro.

	public TcServerFixture(URL tcServerZip, String serverType) throws IOException {
		this.serverType = serverType;
		File unzip =StsTestUtil.createTempDirectory();
		//Unzipping into a temp directory to ensure we have write access to the server distro directory
		// (needed for creating TcServer instance).
		ZipFileUtil.unzip(tcServerZip, unzip, new NullProgressMonitor());
		this.tcServerStub = unzip.listFiles()[0];
	}

	public TcServerFixture(File tcServerStub, String serverType) throws IOException {
		this.serverType = serverType;
		this.tcServerStub = tcServerStub;
	}
	
	public File getStubLocation() throws IOException {
		return tcServerStub;
	}

	public ServerHandler provisionServer() throws Exception {
		File baseDir = StsTestUtil.createTempDirectory("tcServer", null);
		// copy server skeleton
		FileUtil.copyDirectory(getStubLocation(), baseDir, new NullProgressMonitor());
		return getHandler(baseDir.getAbsolutePath());
	}

	public ServerHandler getHandler(String path) throws Exception {
		ServerHandler handler = new ServerHandler(serverType);
		handler.setRuntimeName("runtime");
		handler.setServerName("server");
		handler.setServerPath(path);
		return handler;
	}

	public IServer createServer(final String instance) throws Exception {
		ServerHandler handler = provisionServer();
		return handler.createServer(new NullProgressMonitor(), ServerHandler.ALWAYS_OVERWRITE,
				new ServerHandlerCallback() {
					@Override
					public void configureServer(IServerWorkingCopy wc) throws CoreException {
						// TODO e3.6 remove casts for setAttribute()
						if (instance != null) {
							((ServerWorkingCopy) wc).setAttribute(TcServer.KEY_ASF_LAYOUT, false);
						}
						else {
							((ServerWorkingCopy) wc).setAttribute(TcServer.KEY_ASF_LAYOUT, true);
						}
						((ServerWorkingCopy) wc).setAttribute(TcServer.KEY_SERVER_NAME, instance);
						((ServerWorkingCopy) wc).setAttribute(TcServer.PROPERTY_TEST_ENVIRONMENT, false);
						((ServerWorkingCopy) wc).importRuntimeConfiguration(wc.getRuntime(), null);
					}
				});
	}

	public static void deleteServerAndRuntime(IServer server) throws CoreException {
		IFolder serverConfiguration = server.getServerConfiguration();
		server.delete();
		serverConfiguration.delete(true, true, new NullProgressMonitor());

		IRuntime runtime = server.getRuntime();
		if (runtime != null) {
			runtime.delete();
		}
	}

}
