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

import static org.springsource.ide.eclipse.commons.tests.util.StsTestUtil.assertNoErrors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;

import junit.framework.AssertionFailedError;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.ServerPlugin;
import org.eclipse.wst.server.core.util.WebResource;
import org.springsource.ide.eclipse.commons.core.ZipFileUtil;
import org.springsource.ide.eclipse.commons.frameworks.test.util.ACondition;
import org.springsource.ide.eclipse.commons.frameworks.test.util.SWTBotUtils;
import org.springsource.ide.eclipse.commons.tests.util.StsTestUtil;

import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.commands.GrailsCommandUtils;
import org.grails.ide.eclipse.commands.GrailsExecutor;
import org.grails.ide.eclipse.commands.GrailsExecutorListener;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.launch.SynchLaunch.ILaunchResult;
import org.grails.ide.eclipse.core.model.GrailsBuildSettingsHelper;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.runonserver.GrailsAppModuleDelegate;
import org.grails.ide.eclipse.runonserver.GrailsAppModuleFactoryDelegate;
import org.grails.ide.eclipse.runonserver.RunOnServerProperties;
import org.grails.ide.eclipse.test.util.GrailsTest;
import com.springsource.sts.server.insight.internal.ui.InsightTcServerCallback;
import com.springsource.sts.server.tc.tests.support.TcServerFixture;
import com.springsource.sts.server.tc.tests.support.TcServerHarness;

/**
 * Note this test must *not* be run in the UI thread. This will cause deadlock
 * and test failure.
 * 
 * @author Kris De Volder
 * @author Andrew Eisenberg
 */
public abstract class RunOnServerTestTemplate extends GrailsTest {

	/**
	 * An executor listener that checks whether an expected command is executed.
	 * 
	 * @author Kris De Volder
	 * @created 2011-01-19
	 */
	public class ExpectedCommand extends GrailsExecutorListener {

		boolean executed = false;

		Throwable caught = null;

		private final IProject expectedProject;

		private final String expectedCmd;

		private final String expectedOutputSnippet;

		public ExpectedCommand(IProject project, String cmd,
				String outputSnippet) {
			this.expectedProject = project;
			this.expectedCmd = cmd;
			this.expectedOutputSnippet = outputSnippet;
		}

		@Override
		public void commandExecuted(GrailsCommand cmd, ILaunchResult result) {
			try {
				debug("Expected: " + expectedCmd);
				debug("Command: " + cmd);
				assertFalse("Expected command executed more than once: " + cmd,
						executed);
				assertEquals(expectedCmd, cmd.getCommand());
				assertEquals(expectedProject, cmd.getProject());
				assertContains(expectedOutputSnippet, result.getOutput());
				assertTrue(result.isOK());
				executed = true;
			} catch (Throwable e) {
				caught = e;
			}
		}

		@Override
		public void commandExecuted(GrailsCommand cmd, Throwable thrown) {
			caught = thrown;
		}

		public void assertOk() throws Throwable {
			if (caught != null) {
				throw caught;
			} else if (!executed) {
				throw new AssertionFailedError("Exected command '"
						+ expectedCmd + "' was not executed");
			}
		}

	}

	protected static TcServerFixture serverFixture = null;
	private static URL serverZip = null;

	public static void debug(String string) {
		System.err.println(string);
	}

	/**
	 * If this is set to true, test won't create the test project if it already
	 * exists. (for faster test running).
	 * <p>
	 * Tests that make changes that may cause other tests to break should set
	 * this to false explicitly.
	 */
	private static boolean reusableTestProject = false;

	private static final int SECOND = 1000;

	public static void startServer(IServer server) throws CoreException,
			OperationCanceledException, InterruptedException {
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
		// TODO: The above 'join' makes the test pass, but shouldn't be needed
		// See https://issuetracker.springsource.com/browse/STS-1581

		debug("Starting server '" + server + "'...");
		final boolean[] started = new boolean[] { false };
		IServerListener listener = new IServerListener() {

			boolean starting = false;

			public void serverChanged(ServerEvent event) {
				IServer server = event.getServer();
				debug("ServerEvent: " + event);
				if (server.getServerState() == IServer.STATE_STARTING) {
					starting = true;
					debug("Server " + server + " is starting...");
				}
				if (server.getServerState() == IServer.STATE_STARTED
						&& !started[0]) {
					debug("Server " + server + " started");
					// debug(SWTBotUtils.getConsoleText(new SWTWorkbenchBot()));
					started[0] = true;
				}
				if (starting
						&& server.getServerState() == IServer.STATE_STOPPED) {
					debug("Server " + server + " stopped!");
					try {
						debug(SWTBotUtils.getConsoleText(new SWTWorkbenchBot()));
					} catch (Throwable e) {
					}
				}
			}

		};
		server.addServerListener(listener);
		server.start(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		long endTime = System.currentTimeMillis() + 180 * SECOND;
		while (System.currentTimeMillis() < endTime && !started[0]) {
			System.out.println("Waiting for server to start ...");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
		}
		assertTrue("Server '" + server
				+ "' didn't start within a reasonable time", started[0]);
		server.removeServerListener(listener);
	}

	// protected static TcServerFixture serverFixture210 = new
	// TcServerFixture(Activator.PLUGIN_ID, "com.springsource.tcserver.60",
	// "tc-server-developer-2.1.0.RELEASE");

	private IWorkspace ws = null;

	public IProject project = null;

	private IServer server;

	private ExpectedCommand expectedCommand;
	
	private TcServerHarness harness;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		setJava15Compliance();
		ensureDefaultGrailsVersion(GrailsVersion.MOST_RECENT);

		// On the build server all output to System.out is eaten, which is
		// annoying if you need to
		// determine what went wrong. Send all output to system.err.
		// savedOut = System.out;
		// System.setOut(System.err);

		if (!reusableTestProject) {
			setupclass();
		}
		InsightTcServerCallback.disableDialog(false); // Dialog would freeze
														// some tests, waiting
														// for user to click.
		ws = ResourcesPlugin.getWorkspace();
		StsTestUtil.setAutoBuilding(false);
		ensureProject("gTunes");

		RunOnServerProperties.setEnv(project, "dev");
	}

	private void setupclass() throws CoreException, IOException {
		reusableTestProject = true;
		IProject gTunes = ResourcesPlugin.getWorkspace().getRoot()
				.getProject("gTunes");
		if (gTunes.exists()) {
			gTunes.delete(true, true, null);
		}
	}

	protected void stopServer() {
		if (server!=null) {
			if (server.getServerState() != IServer.STATE_STOPPED) {
				server.stop(true);
			}
			long endTime = System.currentTimeMillis() + 120 * SECOND;
			while (server.getServerState() != IServer.STATE_STOPPED) {
				System.out.println("Waiting for server to stop");
				try {
					Thread.sleep(2 * SECOND);
				} catch (Exception e) {
				}
			}
		}
	}

	protected IProject ensureProject(String projectName) throws Exception {
		GrailsVersion version = GrailsVersion.getDefault(); // Set in setUp
															// method.
		IJobManager jm = Job.getJobManager();
		ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRuleFactory()
				.buildRule();
		try {
			jm.beginRule(rule, new NullProgressMonitor());
			project = ws.getRoot().getProject(projectName);
			if (!project.exists()) {
				IPath workspaceLoc = ws.getRoot().getLocation();
				URL projectZip = getProjectZip(projectName, version);
				ZipFileUtil.unzip(projectZip, workspaceLoc.append(projectName)
						.toFile(), projectName, null);
				project.create(null);
				GrailsCommandUtils.eclipsifyProject(null, true, project);
			}
			return project;
		} finally {
			jm.endRule(rule);
		}
	}

	private IModule getModule() {
		IModule[] modules = ServerUtil.getModules(project);
		assertEquals(1, modules.length);
		IModule module = modules[0];
		return module;
	}

	/**
	 * @param module
	 * @throws CoreException
	 */
	private void serverAddModule(IModule module) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.modifyModules(
		// Add:
				new IModule[] { module },
				// Remove:
				new IModule[0], null);
		wc.save(true, null);
	}

	private void serverRemoveModules() throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		IModule[] modules = wc.getModules();
		if (modules == null || modules.length == 0) {
			return;
		}
		wc.modifyModules(new IModule[0], modules, null);
		wc.save(true, null);
	}

	private void assertNotContains(String expect, String actual) {
		assertFalse(actual, actual.contains(expect));
	}

	private String getPageContent(URL url) throws Exception {
		Object content = null;
		long endTime = System.currentTimeMillis() + 30 * SECOND;
		boolean ok = false;
		Throwable e = null; // keeps last exception in the loop below.

		// Loop that keeps trying to get the contents...
		while (!ok && System.currentTimeMillis() < endTime) {
			try {
				Thread.sleep(2000);
				content = url.getContent();
				ok = content != null;
			} catch (Throwable _e) {
				e = _e;
				System.out.println(_e);
				ok = false;
			}
		}
		if (!ok && e != null) {
			// For more informative test failure, rethrow last exception from
			// the loop (if we have one).
			StsTestUtil.rethrow(e);
		}
		assertTrue("Couldn't get content for: " + url, ok && content != null);
		InputStream in = (InputStream) content;
		BufferedReader read = new BufferedReader(new InputStreamReader(in));
		String s = null;
		StringBuffer result = new StringBuffer();
		while ((s = read.readLine()) != null) {
			result.append(s);
			result.append("\n");
		}
		return result.toString();
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////////

	public void testScaffolding() throws Exception {
		assertTrue(GrailsNature.isGrailsAppProject(project));
		assertNoErrors(project);

		// The tests assume that created test server only has one port for http
		// contents and that
		// this port is set to the number passed into getTestServer. Do a simple
		// check to see if
		// this assumption holds.
		int port = 12345;
		server = getTestServer(port);
		ServerPort[] ports = server.getServerPorts(new NullProgressMonitor());
		assertEquals(
				"The test server has more than one port, it should only have a single http port",
				1, ports.length);
		assertEquals("The server should have an http port", "HTTP",
				ports[0].getProtocol());
		assertEquals("Setting the server http port didn't work", port,
				ports[0].getPort());
	}

	/**
	 * Test if the Module artifact adapter that makes run on server appear is
	 * active and returns the expected module artifact for a grails project.
	 * 
	 * @throws Exception
	 */
	public void testModuleArtifactAdapter() throws Exception {
		// As describe here:
		// http://www.eclipse.org/webtools/wst/components/server/runOnServer.html
		// To have Run On Server menu item appear on a grails project, a grails
		// project needs to

		// 1) Addapt to ILaunchable
		// 2) Provide enablement by contributing to
		// "org.eclipse.wst.server.ui.moduleArtifactAdapters" extension point
		// 3) Addapt to IModuleArtifact
		IModuleArtifact[] marts = ServerPlugin.getModuleArtifacts(project);
		assertEquals(1, marts.length);
		WebResource mart = (WebResource) marts[0];
		IModule module = mart.getModule();
		assertEquals(project.getName(), module.getName());
		assertEquals(Path.EMPTY, mart.getPath());
	}

	/**
	 * Test whether a module is associated with a GrailsApp project.
	 */
	public void testGrailsAppHasModule() throws Exception {
		IModule module = getModule(); //
		assertEquals(GrailsAppModuleFactoryDelegate.TYPE, module
				.getModuleType().getId());
		assertEquals(project, module.getProject());
		assertEquals(project.getName(), module.getName());
	}

	/**
	 * Test for https://issuetracker.springsource.com/browse/STS-1539 and
	 * https://issuetracker.springsource.com/browse/STS-1518
	 * <p>
	 * Both involve the audit logging plugin.
	 * <p>
	 * Note: this test will fail unless using a version of Greclipse that has
	 * the
	 */
	public void testAuditLoggingPlugin() throws Throwable {
		try {
			Class.forName("org.codehaus.jdt.groovy.internal.compiler.ast.GrailsGlobalPluginAwareEntityInjector");
		} catch (ClassNotFoundException e) {
			fail("This test requires a more recent version of Groovy Eclipse");
		}
		StsTestUtil.setAutoBuilding(true);

		int port = StsTestUtil.findFreeSocketPort();
		System.out.println("Creating server with port: " + port);
		server = getTestServer(port);
		IModule module = getModule();
		serverAddModule(module);

		System.out.println("Installing plugin audit logging...");
		ILaunchResult result = GrailsCommandFactory.installPlugin(project,
				"audit-logging").synchExec();
		reusableTestProject = false; // Other tests don't expect audit-logging
										// plugin
		System.out.println("Refreshing dependencies...");
		GrailsCommandUtils.refreshDependencies(JavaCore.create(project), false);
		System.out.println("Installed plugin audit logging... DONE");

		System.out.println("Starting server...");
		startServer(server);
		System.out.println("Starting server... DONE");

		String pageContent = getPageContent(new URL("http://localhost:" + port
				+ "/" + project.getName()));
		assertContains(">gtunes.SongController</a></li>", pageContent);
		assertContains(
				">org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEventController</a></li>",
				pageContent);

		pageContent = getPageContent(new URL("http://localhost:" + port + "/"
				+ project.getName() + "/auditLogEvent/list"));
		assertContains("<h1>AuditLogEvent List</h1>", pageContent);

		stopServer();

	}

	/**
	 * Test whether this module can be deployed to a TcServer instance.
	 * 
	 * @throws Throwable
	 */
	public void testGrailsAppCanDeploy() throws Throwable {
		int port = StsTestUtil.findFreeSocketPort();
		server = getTestServer(port);
		IModule module = getModule();

		// Publishing and starting server makes expected URL return expected
		// content?
		serverAddModule(module);

		// if (server.shouldPublish()) {
		// server.publish(IServer.PUBLISH_AUTO, null);
		// }

		startServer(server);

		String content = getPageContent(new URL("http://localhost:" + port
				+ "/" + project.getName()));
		System.out.println("Web content = " + content);
		assertContains("Welcome to Grails", content);
		assertContains(">gtunes.SongController</a></li>", content);

		String songController = getPageContent(new URL("http://localhost:"
				+ port + "/" + project.getName() + "/song"));
		System.out.println("Song controller = " + songController);
	}

	/**
	 * Test whether this module can be deployed to a TcServer instance.
	 * 
	 * @throws Throwable
	 */
	public void testGrailsAppContextRoot() throws Throwable {
		// Change the "app.context" property
		String contextRoot = "gTunes-blah";
		GrailsBuildSettingsHelper.setApplicationProperty(project,
				"app.context", contextRoot);
		reusableTestProject = false;

		int port = StsTestUtil.findFreeSocketPort();
		server = getTestServer(port);
		IModule module = getModule();

		// Publishing and starting server makes expected URL return expected
		// content?
		serverAddModule(module);

		// if (server.shouldPublish()) {
		// server.publish(IServer.PUBLISH_AUTO, null);
		// }

		startServer(server);

		String content = getPageContent(new URL("http://localhost:" + port
				+ "/" + contextRoot));
		System.out.println("Web content = " + content);
		assertContains("Welcome to Grails", content);
		assertRegexp("<li class=\"controller\"><a href=\"/" + contextRoot
				+ "/song.*\">gtunes.SongController</a></li>", content);

		if (GrailsVersion.getGrailsVersion(project).compareTo(
				GrailsVersion.V_2_0_0) >= 0) {
			String songController = getPageContent(new URL("http://localhost:"
					+ port + "/" + contextRoot + "/song/index"));
			System.out.println("Song controller = " + songController);
		} else {
			String songController = getPageContent(new URL("http://localhost:"
					+ port + "/" + contextRoot + "/song"));
			System.out.println("Song controller = " + songController);
		}
	}

	/**
	 * Test related to STS-1361
	 * 
	 * @throws Throwable
	 */
	public void testPublishDeleteRecreateRepublish() throws Throwable {
		int port = StsTestUtil.findFreeSocketPort();
		server = getTestServer(port);
		IModule module = getModule();

		// Publishing and starting server makes expected URL return expected
		// content?
		serverAddModule(module);

		startServer(server);

		String content = getPageContent(new URL("http://localhost:" + port
				+ "/" + project.getName()));
		System.out.println("Web content = " + content);
		assertContains("Welcome to Grails", content);
		assertContains(">gtunes.SongController</a></li>", content);

		stopServer();

		server.getModules();
		project.delete(true, false, null);
		reusableTestProject = false;

		// Check: gTunes module should disappear from server "soon".
		new ACondition() {
			@Override
			public boolean test() throws Exception {
				return server.getModules().length == 0;
			}
		}.waitFor(3000);

		GrailsCommandFactory.createApp("gTunes").synchExec();
		project = ws.getRoot().getProject("gTunes");
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		GrailsCommandUtils.eclipsifyProject(null, true, project);

		module = getModule();
		serverAddModule(module);

		startServer(server);

		content = getPageContent(new URL("http://localhost:" + port + "/"
				+ project.getName()));
		System.out.println("Web content = " + content);
		assertContains("Welcome to Grails", content);
		assertNotContains(">gtunes.SongController</a></li>", content);
	}

	/**
	 * Test related to STS-1339: adding a new controller with server running,
	 * should make this new controller appear in the browser (eventually).
	 */
	public void testAddNewController() throws Throwable {
		StsTestUtil.setAutoBuilding(true); // More likely the way user would be
											// working is with this on.
		final int port = StsTestUtil.findFreeSocketPort();
		server = getTestServer(port);
		IModule module = getModule();

		// Publishing and starting server makes expected URL return expected
		// content?
		serverAddModule(module);
		startServer(server);

		String content = getPageContent(new URL("http://localhost:" + port
				+ "/" + project.getName()));
		System.out.println("Web content = " + content);
		assertContains("Welcome to Grails", content);
		assertContains(">gtunes.SongController</a></li>", content);
		assertFalse(
				"Books should only be added later, they are already there!",
				content.contains(">gtunes.BookController</a></li>"));

		System.out.println("Adding 'books' to the gTunes app");
		reusableTestProject = false; // Other tests don't expect books
		addBooksToGTunes();

		// Check: book controller should appear on the main gTunes page
		// eventually.
		new ACondition("Loading new BookController") {
			String content;

			@Override
			public boolean test() throws Exception {
				content = getPageContent(new URL("http://localhost:" + port
						+ "/" + project.getName()));
				return content.contains(">gtunes.BookController</a></li>");
			}

			@Override
			public String getMessage() {
				if (content != null) {
					return content;
				} else {
					return super.getMessage();
				}
			}
		}.waitFor(180000);
	}

	/**
	 * Adds "Book" domain class and controller to the gTunes project.
	 * 
	 * @throws CoreException
	 */
	protected void addBooksToGTunes() throws CoreException {
		GrailsTest.createResource(project,
				"grails-app/domain/gtunes/Book.groovy", "package gtunes\n"
						+ "\n" + "class Book {\n" + "\n"
						+ "    static constraints = {\n" + "    }\n" + "	\n"
						+ "	String author\n" + "	String title\n" + "	\n"
						+ "}\n");
		GrailsTest.createResource(project,
				"grails-app/controllers/gtunes/BookController.groovy",
				"package gtunes\n" + "\n" + "class BookController {\n" + "\n"
						+ "    def scaffold = Book\n" + "}\n");
	}

	/**
	 * Test whether setting the 'env' property on a project results in a
	 * building the war file with the correct environment.
	 * 
	 * @throws Throwable
	 */
	public void testEnv() throws Throwable {
		File warFile = GrailsAppModuleDelegate.getWarFile(project);

		StsTestUtil.setAutoBuilding(true); // More likely the way user would be
											// working is with this on.
		int port = StsTestUtil.findFreeSocketPort();
		System.out.println("Using port: " + port);
		server = getTestServer(port);
		IModule module = getModule();
		serverAddModule(module);

		RunOnServerProperties.setEnv(project,
				"force war build next time by bashing something into env");

		// Check with 'dev' environment
		expectedCommand(project, "dev war " + warFile,
				"Environment set to development");
		RunOnServerProperties.setEnv(project, "dev");
		startServer(server);
		getPageContent(new URL("http://localhost:" + port + "/"
				+ project.getName()));
		stopServer();
		assertExpectedCommand();

		// Check with 'prod' environment
		port = StsTestUtil.findFreeSocketPort();
		setHttpPort(server, port);
		System.out.println("Using port: " + port);

		expectedCommand(project, "prod war " + warFile,
				"Environment set to production");
		RunOnServerProperties.setEnv(project, "prod");
		startServer(server);
		getPageContent(new URL("http://localhost:" + port + "/"
				+ project.getName()));
		stopServer();
		assertExpectedCommand();

		// Check with 'custom' environment
		port = StsTestUtil.findFreeSocketPort();
		setHttpPort(server, port);
		System.out.println("Using port: " + port);

		expectedCommand(project, "war " + warFile,
				"Environment set to blah blah");
		RunOnServerProperties.setEnv(project, "blah blah");
		startServer(server);
		getPageContent(new URL("http://localhost:" + port + "/"
				+ project.getName()));
		stopServer();
		assertExpectedCommand();

	}

	private void expectedCommand(IProject project, String cmd,
			String outputSnippet) {
		this.expectedCommand = new ExpectedCommand(project, cmd, outputSnippet);
		GrailsExecutor.setListener(expectedCommand);
	}

	private void assertExpectedCommand() throws Throwable {
		this.expectedCommand.assertOk();
	}

	/**
	 * What's the default grails version that we expect new projects to be
	 * created with.
	 */
	private String grailsVersion() {
		return GrailsCoreActivator.getDefault().getInstallManager()
				.getDefaultGrailsInstall().getVersionString();
	}

	/**
	 * Modifies properties file in a TcServer configuration folder to change the
	 * property that defines the httpPort number the server will be using.
	 * <p>
	 * Note that the implementation of this is hacky in that it makes various
	 * assumptions about the server and how it is being configured. It should
	 * work for the test fixtures in this suite (and testScaffolding checks
	 * whether it works).
	 */
	protected void setHttpPort(IServer server, int port) throws IOException,
			CoreException {
		IFile propFile = server.getServerConfiguration().getFile(
				"catalina.properties");
		IServerWorkingCopy wc = server.createWorkingCopy();
		Properties props = new Properties();
		InputStream in = null;
		try {
			in = propFile.getContents();
			props.load(in);
		} finally {
			if (in != null) {
				in.close();
			}
		}
		props.put("bio.http.port", "" + port);
		OutputStream out = null;
		try {
			out = new FileOutputStream(propFile.getLocation().toFile());
			props.store(out, "# modified by RunOnServer tests");
		} finally {
			if (out != null) {
				out.close();
			}
		}
		propFile.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
		// Need to touch and save server working copy for STS to clue in that
		// server port has changed
		// See https://issuetracker.springsource.com/browse/STS-1535
		wc.setServerConfiguration(server.getServerConfiguration());
		wc.save(true, new NullProgressMonitor());
	}

	protected IServer getTestServer(int httpPort) throws Exception {
		IServer created = getHarness().createServer(TcServerFixture.INST_INSIGHT);
		setHttpPort(created, httpPort);
		return created;
	}
	
	private TcServerHarness getHarness() {
		if (harness==null) {
			harness = new TcServerHarness(getTcServerFixture());
		}
		return harness;
	}

	@Override
	protected void tearDown() throws Exception {
		GrailsExecutor.setListener(null);
		stopServer();
		if (harness!=null) {
			harness.dispose();
		}
		super.tearDown();
	}

	protected abstract TcServerFixture getTcServerFixture();

//	protected URL getResourceURL(String resourcePath) {
//		URL result = this.getClass().getClassLoader().getResource(resourcePath);
//		Assert.isNotNull("Couldn't find resource: " + resourcePath, result);
//		return result;
//	}

	// TODO: Add some test that put stuff into a form to see if basic
	// functionality of the test app is working
	// See here on help for submitting form requests:
	// http://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests

}
