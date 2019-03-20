/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.debug.tests;


import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.core.LaunchDelegate;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationPresentationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsDialog;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutExtension;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.eclipse.debug.ui.actions.ToggleBreakpointAction;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.debug.core.IJavaClassPrepareBreakpoint;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.IJavaPatternBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaStratumLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaTargetPatternBreakpoint;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.debug.core.IJavaWatchpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.debug.eval.EvaluationManager;
import org.eclipse.jdt.debug.eval.IAstEvaluationEngine;
import org.eclipse.jdt.debug.eval.IEvaluationListener;
import org.eclipse.jdt.debug.eval.IEvaluationResult;
import org.eclipse.jdt.debug.testplugin.DebugElementEventWaiter;
import org.eclipse.jdt.debug.testplugin.DebugElementKindEventDetailWaiter;
import org.eclipse.jdt.debug.testplugin.DebugElementKindEventWaiter;
import org.eclipse.jdt.debug.testplugin.DebugEventWaiter;
import org.eclipse.jdt.debug.tests.refactoring.MemberParser;
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils;
import org.eclipse.jdt.internal.debug.ui.IJDIPreferencesConstants;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.console.ConsoleHyperlinkPosition;
import org.eclipse.ui.progress.WorkbenchJob;

import com.sun.jdi.InternalException;

/**
 * Tests for launch configurations
 * @author Andrew Eisenberg
 */
public abstract class AbstractDebugTest extends TestCase implements  IEvaluationListener {
	
	/**
	 * the default timeout
	 */
	public static final int DEFAULT_TIMEOUT = 30000;
	
	//constants
	protected static final String JAVA = "java"; //$NON-NLS-1$
	protected static final String JAVA_EXTENSION = ".java"; //$NON-NLS-1$
	protected static final String LAUNCHCONFIGURATIONS = "launchConfigurations"; //$NON-NLS-1$
	protected static final String LAUNCH_EXTENSION = ".launch"; //$NON-NLS-1$
	protected static final String LOCAL_JAVA_APPLICATION_TYPE_ID = "org.eclipse.jdt.launching.localJavaApplication"; //$NON-NLS-1$
	protected static final String JAVA_LAUNCH_SHORTCUT_ID = "org.eclipse.jdt.debug.ui.localJavaShortcut"; //$NON-NLS-1$
	protected static final String TEST_LAUNCH_SHORTCUT = "org.eclipse.jdt.debug.tests.testShortCut";
	
	/**
	 * an evaluation result
	 */
	public IEvaluationResult fEvaluationResult;
	
	/**
	 * the java project
	 */
	public static IJavaProject fJavaProject;
	
	/**
	 * The last relevant event set - for example, that caused
	 * a thread to suspend
	 */
	protected DebugEvent[] fEventSet;
	
	/**
	 * Constructor
	 * @param name
	 */
	public AbstractDebugTest(String name) {
		super(name);
		// set error dialog to non-blocking to avoid hanging the UI during test
		ErrorDialog.AUTOMATED_MODE = true;
		SafeRunnable.setIgnoreErrors(true);
	}
	
	
	protected void setUp() throws Exception {
		super.setUp();
		if (!ProjectCreationDecorator.isReady()) {
			new TestSuite(ProjectCreationDecorator.class).run(new TestResult());
		}
	}

	/**
	 * Sets the last relevant event set
	 *
	 * @param set event set
	 */
	protected void setEventSet(DebugEvent[] set) {
		fEventSet = set;
	}
	
	/**
	 * Returns the last relevant event set
	 * 
	 * @return event set
	 */
	protected DebugEvent[] getEventSet() {
		return fEventSet;
	}
	
	/**
	 * Returns the launch manager
	 * 
	 * @return launch manager
	 */
	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}
	
	/**
	 * Returns the singleton instance of the <code>LaunchConfigurationManager</code>
	 * 
	 * @return the singleton instance of the <code>LaunchConfigurationManager</code>
	 * @since 3.3
	 */
	protected LaunchConfigurationManager getLaunchConfigurationManager() {
		return DebugUIPlugin.getDefault().getLaunchConfigurationManager();
	}
	
	/**
	 * Returns the breakpoint manager
	 * 
	 * @return breakpoint manager
	 */
	protected IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}	
	
	/**
	 * Returns the 'DebugTests' project.
	 * 
	 * @return the test project
	 */
	protected IJavaProject getJavaProject() {
		return getJavaProject("DebugTests");
	}
	
	protected IJavaProject get15Project() {
		return getJavaProject("OneFive");
	}
	
	/**
	 * Returns the Java project with the given name.
	 * 
	 * @param name project name
	 * @return the Java project with the given name
	 */
	protected IJavaProject getJavaProject(String name) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		return JavaCore.create(project);
	}
	
	/**
	 * Returns the launch shortcut with the given id
	 * @param id
	 * @return the <code>LaunchShortcutExtension</code> with the given id, 
	 * or <code>null</code> if none
	 * 
	 * @since 3.3
	 */
	protected LaunchShortcutExtension getLaunchShortcutExtension(String id) {
		List exts = getLaunchConfigurationManager().getLaunchShortcuts();
		LaunchShortcutExtension ext = null;
		for (int i = 0; i < exts.size(); i++) {
			ext = (LaunchShortcutExtension) exts.get(i);
			if(ext.getId().equals(id)) {
				return ext;
			}
		}
		return null;
	}
	
	/**
	 * New to 3.3 is the ability to have multiple delegates for a variety of overlapping mode combinations.
	 * As such, for tests that launch specific configurations, must be check to ensure that there is a preferred 
	 * launch delegate available for the launch in the event there are duplicates. Otherwise the tests
	 * will hang waiting for a user to select a resolution action.
	 * @param configuration
	 * @param modes
	 * @throws CoreException
	 * 
	 * @since 3.3
	 */
	protected void ensurePreferredDelegate(ILaunchConfiguration configuration, Set modes) throws CoreException {
		ILaunchConfigurationType type = configuration.getType();
		ILaunchDelegate[] delegates = type.getDelegates(modes);
		if(delegates.length > 1) {
			type.setPreferredDelegate(modes, getDelegateById(type.getIdentifier(), LOCAL_JAVA_APPLICATION_TYPE_ID));
		}
	}
	
	/**
	 * Returns the LaunchDelegate for the specified ID
	 * @param delegateId the id of the delegate to search for
	 * @return the <code>LaunchDelegate</code> associated with the specified id or <code>null</code> if not found
	 * @since 3.3
	 */
	protected ILaunchDelegate getDelegateById(String typeId, String delegateId) {
		LaunchManager lm = (LaunchManager) getLaunchManager();
		LaunchDelegate[] delegates = lm.getLaunchDelegates(typeId);
		for(int i = 0; i < delegates.length; i++) {
			if(delegates[i].getId().equals(delegateId)) {
				return delegates[i];
			}
		}
		return null;
	}
	
	/**
	 * Returns the source folder with the given name in the given project.
	 * 
	 * @param project
	 * @param name source folder name
	 * @return package fragment root
	 */
	protected IPackageFragmentRoot getPackageFragmentRoot(IJavaProject project, String name) {
		IProject p = project.getProject();
		return project.getPackageFragmentRoot(p.getFolder(name));
	}
	
	/**
	 * Returns the <code>IHyperLink</code> at the given offset in the specified document
	 * or <code>null</code> if the offset does not point to an <code>IHyperLink</code>
	 * @param offset
	 * @param doc
	 * @return the <code>IHyperLink</code> at the given offset or <code>null</code>
	 */
	protected IHyperlink getHyperlink(int offset, IDocument doc) {
		if (offset >= 0 && doc != null) {
			Position[] positions = null;
			try {
				positions = doc.getPositions(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY);
			} catch (BadPositionCategoryException ex) {
				// no links have been added
				return null;
			}
			for (int i = 0; i < positions.length; i++) {
				Position position = positions[i];
				if (offset >= position.getOffset() && offset <= (position.getOffset() + position.getLength())) {
					return ((ConsoleHyperlinkPosition)position).getHyperLink();
				}
			}
		}
		return null;
	}
	
	/**
	 * Launches the given configuration and waits for an event. Returns the
	 * source of the event. If the event is not received, the launch is
	 * terminated and an exception is thrown.
	 * 
	 * @param configuration the configuration to launch
	 * @param waiter the event waiter to use
	 * @return Object the source of the event
	 * @exception Exception if the event is never received.
	 */
	protected Object launchAndWait(ILaunchConfiguration configuration, DebugEventWaiter waiter) throws CoreException {
	    return launchAndWait(configuration, waiter, true);
	}
	
	/**
	 * Launches the given configuration in debug mode and waits for an event. 
	 * Returns the source of the event. If the event is not received, the 
	 * launch is terminated and an exception is thrown.
	 * 
	 * @param configuration the configuration to launch
	 * @param waiter the event waiter to use
	 * @param register whether to register the launch
	 * @return Object the source of the event
	 * @exception Exception if the event is never received.
	 */
	protected Object launchAndWait(ILaunchConfiguration configuration, DebugEventWaiter waiter, boolean register) throws CoreException {
		return launchAndWait(configuration, ILaunchManager.DEBUG_MODE, waiter, register);
	}
	
	/**
	 * Launches the given configuration and waits for an event. Returns the
	 * source of the event. If the event is not received, the launch is
	 * terminated and an exception is thrown.
	 * 
	 * @param configuration the configuration to launch
	 * @param mode the mode to launch the configuration in
	 * @param waiter the event waiter to use
	 * @param register whether to register the launch
	 * @return Object the source of the event
	 * @exception Exception if the event is never received.
	 */
	protected Object launchAndWait(ILaunchConfiguration configuration, String mode, DebugEventWaiter waiter, boolean register) throws CoreException {
		ILaunch launch = configuration.launch(mode, null, false, register);
		Object suspendee= waiter.waitForEvent();
		if (suspendee == null) {
			StringBuffer buf = new StringBuffer();
            buf.append("Test case: "); //$NON-NLS-1$
            buf.append(getName());
            buf.append("\n"); //$NON-NLS-1$
            buf.append("Never received event: "); //$NON-NLS-1$
            buf.append(waiter.getEventKindName());
            buf.append("\n"); //$NON-NLS-1$
            if (launch.isTerminated()) {
            	buf.append("Process exit value: "); //$NON-NLS-1$
            	buf.append(launch.getProcesses()[0].getExitValue());
                buf.append("\n"); //$NON-NLS-1$
            }
            IConsole console = DebugUITools.getConsole(launch.getProcesses()[0]);
            if (console instanceof TextConsole) {
                TextConsole textConsole = (TextConsole)console;
                String string = textConsole.getDocument().get();
                buf.append("Console output follows:\n"); //$NON-NLS-1$
                buf.append(string);
            }
            buf.append("\n"); //$NON-NLS-1$
            DebugPlugin.log(new Status(IStatus.ERROR, "org.eclipse.jdt.debug.ui.tests", buf.toString())); //$NON-NLS-1$
			try {
				launch.terminate();
			} catch (CoreException e) {
				e.printStackTrace();
				fail("Program did not suspend, and unable to terminate launch."); //$NON-NLS-1$
			}
			throw new TestAgainException();
		}
		setEventSet(waiter.getEventSet());
		assertNotNull("Program did not suspend, launch terminated.", suspendee); //$NON-NLS-1$
		return suspendee;		
	}
	
	
	
	/**
	 * Launches the type with the given name, and waits for a
	 * suspend event in that program. Returns the thread in which the suspend
	 * event occurred.
	 * 
	 * @param mainTypeName the program to launch
	 * @return thread in which the first suspend event occurred
	 */
	protected IJavaThread launchAndSuspend(String mainTypeName) throws Exception {
		ILaunchConfiguration config = getLaunchConfiguration(mainTypeName);
		assertNotNull("Could not locate launch configuration for " + mainTypeName, config); //$NON-NLS-1$
		return launchAndSuspend(config);
	}

	/**
	 * Launches the given configuration in debug mode, and waits for a 
	 * suspend event in that program. Returns the thread in which the suspend
	 * event occurred.
	 * 
	 * @param config the configuration to launch
	 * @return thread in which the first suspend event occurred
	 */	
	protected IJavaThread launchAndSuspend(ILaunchConfiguration config) throws Exception {
		DebugEventWaiter waiter= new DebugElementKindEventWaiter(DebugEvent.SUSPEND, IJavaThread.class);
		waiter.setTimeout(DEFAULT_TIMEOUT);
		Object suspendee = launchAndWait(config, waiter);
		return (IJavaThread)suspendee;		
	}
	
	/**
	 * Launches the type with the given name, and waits for a breakpoint-caused 
	 * suspend event in that program. Returns the thread in which the suspend
	 * event occurred.
	 * 
	 * @param mainTypeName the program to launch
	 * @return thread in which the first suspend event occurred
	 */
	protected IJavaThread launchToBreakpoint(String mainTypeName) throws Exception {
		return launchToBreakpoint(getJavaProject(), mainTypeName);
	}
	
	/**
	 * Launches the type with the given name, and waits for a breakpoint-caused 
	 * suspend event in that program. Returns the thread in which the suspend
	 * event occurred.
	 * 
	 * @param project the project the type is in
	 * @param mainTypeName the program to launch
	 * @return thread in which the first suspend event occurred
	 */
	protected IJavaThread launchToBreakpoint(IJavaProject project, String mainTypeName) throws Exception {
		return launchToBreakpoint(project, mainTypeName, true);
	}	
	
	/**
	 * Launches the type with the given name, and waits for a breakpoint-caused 
	 * suspend event in that program. Returns the thread in which the suspend
	 * event occurred.
	 * 
	 * @param mainTypeName the program to launch
	 * @param register whether to register the launch
	 * @return thread in which the first suspend event occurred
	 */
	protected IJavaThread launchToBreakpoint(String mainTypeName, boolean register) throws Exception {
		return launchToBreakpoint(getJavaProject(), mainTypeName, register);
	}
	
	/**
	 * Launches the type with the given name, and waits for a breakpoint-caused 
	 * suspend event in that program. Returns the thread in which the suspend
	 * event occurred.
	 * 
	 * @param mainTypeName the program to launch
	 * @param register whether to register the launch
	 * @return thread in which the first suspend event occurred
	 */
	protected IJavaThread launchToBreakpoint(IJavaProject project, String mainTypeName, boolean register) throws Exception {
		ILaunchConfiguration config = getLaunchConfiguration(project, mainTypeName);
		assertNotNull("Could not locate launch configuration for " + mainTypeName, config); //$NON-NLS-1$
		return launchToBreakpoint(config, register);
	}	

	/**
	 * Launches the given configuration in debug mode, and waits for a breakpoint-caused 
	 * suspend event in that program. Returns the thread in which the suspend
	 * event occurred.
	 * 
	 * @param config the configuration to launch
	 * @return thread in which the first suspend event occurred
	 */	
	protected IJavaThread launchToBreakpoint(ILaunchConfiguration config) throws CoreException {
	    return launchToBreakpoint(config, true);
	}
	
	/**
	 * Launches the given configuration in debug mode, and waits for a breakpoint-caused 
	 * suspend event in that program. Returns the thread in which the suspend
	 * event occurred.
	 * 
	 * @param config the configuration to launch
	 * @param whether to register the launch
	 * @return thread in which the first suspend event occurred
	 */	
	protected IJavaThread launchToBreakpoint(ILaunchConfiguration config, boolean register) throws CoreException {
		DebugEventWaiter waiter= new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, IJavaThread.class, DebugEvent.BREAKPOINT);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		Object suspendee= launchAndWait(config, waiter, register);
		assertTrue("suspendee was not an IJavaThread", suspendee instanceof IJavaThread); //$NON-NLS-1$
		return (IJavaThread)suspendee;		
	}	
	
	/**
	 * Launches the type with the given name, and waits for a terminate
	 * event in that program. Returns the debug target in which the suspend
	 * event occurred.
	 * 
	 * @param mainTypeName the program to launch
	 * @param timeout the number of milliseconds to wait for a terminate event
	 * @return debug target in which the terminate event occurred
	 */
	protected IJavaDebugTarget launchAndTerminate(String mainTypeName) throws Exception {
		ILaunchConfiguration config = getLaunchConfiguration(mainTypeName);
		assertNotNull("Could not locate launch configuration for " + mainTypeName, config); //$NON-NLS-1$
		return launchAndTerminate(config, DEFAULT_TIMEOUT);
	}

	/**
	 * Launches the given configuration in debug mode, and waits for a terminate
	 * event in that program. Returns the debug target in which the terminate
	 * event occurred.
	 * 
	 * @param config the configuration to launch
	 * @param timeout the number of milliseconds to wait for a terminate event
	 * @return thread in which the first suspend event occurred
	 */	
	protected IJavaDebugTarget launchAndTerminate(ILaunchConfiguration config, int timeout) throws Exception {
		return launchAndTerminate(config, timeout, true);	
	}
	
	/**
	 * Launches the given configuration in debug mode, and waits for a terminate
	 * event in that program. Returns the debug target in which the terminate
	 * event occurred.
	 * 
	 * @param config the configuration to launch
	 * @param timeout the number of milliseconds to wait for a terminate event
	 * @param register whether to register the launch
	 * @return thread in which the first suspend event occurred
	 */	
	protected IJavaDebugTarget launchAndTerminate(ILaunchConfiguration config, int timeout, boolean register) throws Exception {
		DebugEventWaiter waiter= new DebugElementKindEventWaiter(DebugEvent.TERMINATE, IJavaDebugTarget.class);
		waiter.setTimeout(timeout);

		Object terminatee = launchAndWait(config, waiter, register);		
		assertNotNull("Program did not terminate.", terminatee); //$NON-NLS-1$
		assertTrue("terminatee is not an IJavaDebugTarget", terminatee instanceof IJavaDebugTarget); //$NON-NLS-1$
		IJavaDebugTarget debugTarget = (IJavaDebugTarget) terminatee;
		assertTrue("debug target is not terminated", debugTarget.isTerminated() || debugTarget.isDisconnected()); //$NON-NLS-1$
		return debugTarget;		
	}	
	
	/**
	 * Launches the type with the given name, and waits for a line breakpoint suspend
	 * event in that program. Returns the thread in which the suspend
	 * event occurred.
	 * 
	 * @param mainTypeName the program to launch
	 * @param bp the breakpoint that should cause a suspend event
	 * @return thread in which the first suspend event occurred
	 */
	protected IJavaThread launchToLineBreakpoint(String mainTypeName, ILineBreakpoint bp) throws Exception {
		return launchToLineBreakpoint(mainTypeName, bp, true);
	}

	/**
	 * Launches the type with the given name, and waits for a line breakpoint suspend
	 * event in that program. Returns the thread in which the suspend
	 * event occurred.
	 * 
	 * @param mainTypeName the program to launch
	 * @param bp the breakpoint that should cause a suspend event
	 * @param register whether to register the launch
	 * @return thread in which the first suspend event occurred
	 */
	protected IJavaThread launchToLineBreakpoint(String mainTypeName, ILineBreakpoint bp, boolean register) throws Exception {
		ILaunchConfiguration config = getLaunchConfiguration(mainTypeName);
		assertNotNull("Could not locate launch configuration for " + mainTypeName, config); //$NON-NLS-1$
		return launchToLineBreakpoint(config, bp, register);
	}
	
	/**
	 * Launches the given configuration in debug mode, and waits for a line breakpoint 
	 * suspend event in that program. Returns the thread in which the suspend
	 * event occurred.
	 * 
	 * @param config the configuration to launch
	 * @param bp the breakpoint that should cause a suspend event
	 * @return thread in which the first suspend event occurred
	 */	
	protected IJavaThread launchToLineBreakpoint(ILaunchConfiguration config, ILineBreakpoint bp, boolean register) throws Exception {
		DebugEventWaiter waiter= new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, IJavaThread.class, DebugEvent.BREAKPOINT);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		Object suspendee= launchAndWait(config, waiter, register);
		assertTrue("suspendee was not an IJavaThread", suspendee instanceof IJavaThread); //$NON-NLS-1$
		IJavaThread thread = (IJavaThread) suspendee;
		IBreakpoint hit = getBreakpoint(thread);
		assertNotNull("suspended, but not by breakpoint", hit); //$NON-NLS-1$
		assertTrue("hit un-registered breakpoint", bp.equals(hit)); //$NON-NLS-1$
		assertTrue("suspended, but not by line breakpoint", hit instanceof ILineBreakpoint); //$NON-NLS-1$
		ILineBreakpoint breakpoint= (ILineBreakpoint) hit;
		int lineNumber = breakpoint.getLineNumber();
		int stackLine = thread.getTopStackFrame().getLineNumber();
		assertTrue("line numbers of breakpoint and stack frame do not match", lineNumber == stackLine); //$NON-NLS-1$
		
		return thread;		
	}
	
	/**
	 * Returns the standard java launch tab group
	 * @return the standard java launch tab group
	 * @throws CoreException
	 * 
	 * @since 3.3
	 */
	protected ILaunchConfigurationTabGroup getJavaLaunchGroup() throws CoreException {
		ILaunchConfigurationType javaType = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION); 
		ILaunchConfigurationTabGroup standardGroup = LaunchConfigurationPresentationManager.getDefault().getTabGroup(javaType, ILaunchManager.DEBUG_MODE);
		return standardGroup;
	}
	
	/**
	 * Returns an instance of the launch configuration dialog on the the specified launch mode
	 * @param modeid the id of the mode to open the launch dialog on
	 * @return an new instance of <code>IlaunchConfigurationDialog</code>
	 * 
	 * @since 3.3
	 */
	protected ILaunchConfigurationDialog getLaunchConfigurationDialog(String modeid) {
		return new LaunchConfigurationsDialog(null, DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(modeid));
	}
	
	/**
	 * Resumes the given thread, and waits for another breakpoint-caused suspend event.
	 * Returns the thread in which the suspend event occurs.
	 * 
	 * @param thread thread to resume
	 * @return thread in which the first suspend event occurs
	 */
	protected IJavaThread resume(IJavaThread thread) throws Exception {
	    return resume(thread, DEFAULT_TIMEOUT);
	}	
	
	/**
	 * Resumes the given thread, and waits for another breakpoint-caused suspend event.
	 * Returns the thread in which the suspend event occurs.
	 * 
	 * @param thread thread to resume
	 * @param timeout timeout in milliseconds
	 * @return thread in which the first suspend event occurs
	 */
	protected IJavaThread resume(IJavaThread thread, int timeout) throws Exception {
		DebugEventWaiter waiter= new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, IJavaThread.class, DebugEvent.BREAKPOINT);
		waiter.setTimeout(timeout);
		
		thread.resume();

		Object suspendee= waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		assertNotNull("Program did not suspend.", suspendee); //$NON-NLS-1$
		return (IJavaThread)suspendee;
	}	
	
	/**
	 * Resumes the given thread, and waits for a suspend event caused by the specified
	 * line breakpoint.  Returns the thread in which the suspend event occurs.
	 * 
	 * @param thread thread to resume
	 * @return thread in which the first suspend event occurs
	 */
	protected IJavaThread resumeToLineBreakpoint(IJavaThread resumeThread, ILineBreakpoint bp) throws Exception {
		DebugEventWaiter waiter= new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, IJavaThread.class, DebugEvent.BREAKPOINT);
		waiter.setTimeout(DEFAULT_TIMEOUT);
		
		resumeThread.resume();

		Object suspendee= waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		assertNotNull("Program did not suspend.", suspendee); //$NON-NLS-1$
		assertTrue("suspendee was not an IJavaThread", suspendee instanceof IJavaThread); //$NON-NLS-1$
		IJavaThread thread = (IJavaThread) suspendee;
		IBreakpoint hit = getBreakpoint(thread);
		assertNotNull("suspended, but not by breakpoint", hit); //$NON-NLS-1$
		assertTrue("hit un-registered breakpoint", bp.equals(hit)); //$NON-NLS-1$
		assertTrue("suspended, but not by line breakpoint", hit instanceof ILineBreakpoint); //$NON-NLS-1$
		ILineBreakpoint breakpoint= (ILineBreakpoint) hit;
		int lineNumber = breakpoint.getLineNumber();
		int stackLine = thread.getTopStackFrame().getLineNumber();
		assertTrue("line numbers of breakpoint and stack frame do not match", lineNumber == stackLine); //$NON-NLS-1$
		
		return (IJavaThread)suspendee;
	}	
	
	/**
	 * Resumes the given thread, and waits for the debug target
	 * to terminate (i.e. finish/exit the program).
	 * 
	 * @param thread thread to resume
	 */
	protected void exit(IJavaThread thread) throws Exception {
		DebugEventWaiter waiter= new DebugElementKindEventWaiter(DebugEvent.TERMINATE, IProcess.class);
		waiter.setTimeout(DEFAULT_TIMEOUT);
		
		thread.resume();

		Object suspendee= waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		assertNotNull("Program did not terminate.", suspendee); //$NON-NLS-1$
	}	
		
	/**
	 * Resumes the given thread, and waits the associated debug
	 * target to terminate.
	 * 
	 * @param thread thread to resume
	 * @return the terminated debug target
	 */
	protected IJavaDebugTarget resumeAndExit(IJavaThread thread) throws Exception {
		DebugEventWaiter waiter= new DebugElementEventWaiter(DebugEvent.TERMINATE, thread.getDebugTarget());
		waiter.setTimeout(DEFAULT_TIMEOUT);
		
		thread.resume();

		Object suspendee= waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		assertNotNull("Program did not terminate.", suspendee); //$NON-NLS-1$
		IJavaDebugTarget target = (IJavaDebugTarget)suspendee;
		assertTrue("program should have exited", target.isTerminated() || target.isDisconnected()); //$NON-NLS-1$
		return target;
	}	
		
	/**
	 * Returns the launch configuration for the given main type
	 * 
	 * @param mainTypeName program to launch
	 * @see ProjectCreationDecorator
	 */
	protected ILaunchConfiguration getLaunchConfiguration(String mainTypeName) {
		return getLaunchConfiguration(getJavaProject(), mainTypeName);
	}
	
	/**
	 * Returns the launch configuration for the given main type
	 * 
	 * @param mainTypeName program to launch
	 * @see ProjectCreationDecorator
	 */
	protected ILaunchConfiguration getLaunchConfiguration(IJavaProject project, String mainTypeName) {
		IFile file = project.getProject().getFolder(LAUNCHCONFIGURATIONS).getFile(mainTypeName + LAUNCH_EXTENSION);
		ILaunchConfiguration config = getLaunchManager().getLaunchConfiguration(file);
		assertNotNull("the configuration cannot be null", config);
		assertTrue("Could not find launch configuration for " + mainTypeName, config.exists()); //$NON-NLS-1$
		return config;
	}	
	
	/**
	 * Returns the launch configuration in the specified folder in the given project, for the given main type
	 * 
	 * @param project the project to look in
	 * @param containername the name of the container in the specified project to look for the config
	 * @param mainTypeName program to launch
	 * @see ProjectCreationDecorator
	 */
	protected ILaunchConfiguration getLaunchConfiguration(IJavaProject project, String containername, String mainTypeName) {
		IFile file = project.getProject().getFolder(containername).getFile(mainTypeName + LAUNCH_EXTENSION);
		ILaunchConfiguration config = getLaunchManager().getLaunchConfiguration(file);
		assertNotNull("the configuration cannot be null", config);
		assertTrue("Could not find launch configuration for " + mainTypeName, config.exists()); //$NON-NLS-1$
		return config;
	}
	
	/**
	 * Returns the corresponding <code>IResource</code> from the <code>IJavaElement</code> with the
	 * specified name
	 * @param typeName the name of the <code>IJavaElement</code> to get the resource for
	 * @return the corresponding <code>IResource</code> from the <code>IJavaElement</code> with the
	 * specified name
	 * @throws Exception
	 */
	protected IResource getBreakpointResource(String typeName) throws Exception {
		IJavaElement element = getJavaProject().findElement(new Path(typeName + JAVA_EXTENSION));
		IResource resource = element.getCorrespondingResource();
		if (resource == null) {
			resource = getJavaProject().getProject();
		}		
		return resource;
	}
	
	/**
	 * Returns the resource from the specified type or the project from the testing java project in the 
	 * event there is no resource from the specified type
	 * @param type
	 * @return
	 * @throws Exception
	 */
	protected IResource getBreakpointResource(IType type) throws Exception {
		if (type == null) {
			return getJavaProject().getProject();
		}
		IResource resource = type.getResource();
		if (resource == null) {
			resource = type.getJavaProject().getProject();
		}		
		return resource;
	}	
	
	/**
	 * Creates and returns a line breakpoint at the given line number in the type with the
	 * given name.
	 * 
	 * @param lineNumber line number
	 * @param typeName type name
	 */
	protected IJavaLineBreakpoint createLineBreakpoint(int lineNumber, String typeName) throws Exception {
		IType type = getType(typeName);
		return createLineBreakpoint(type, lineNumber);
	}
	
	/**
	 * Creates am  new java line breakpoint
	 * @param lineNumber
	 * @param root
	 * @param packageName
	 * @param cuName
	 * @param fullTargetName
	 * @return a new line breakpoint
	 */
	protected IJavaLineBreakpoint createLineBreakpoint(int lineNumber, String root, String packageName, String cuName, 
			String fullTargetName) throws Exception{
		IJavaProject javaProject = getJavaProject();
		ICompilationUnit cunit = getCompilationUnit(javaProject, root, packageName, cuName);
		assertNotNull("did not find requested Compilation Unit", cunit); //$NON-NLS-1$
		IType targetType = (IType)(new MemberParser()).getDeepest(cunit,fullTargetName);
		assertNotNull("did not find requested type", targetType); //$NON-NLS-1$
		assertTrue("did not find type to install breakpoint in", targetType.exists()); //$NON-NLS-1$
		
		return createLineBreakpoint(targetType, lineNumber);
	}

	
	/**
	 * Creates a line breakpoint in the given type (may be a top level non public type)
	 * 
	 * @param lineNumber line number to create the breakpoint at
	 * @param packageName fully qualified package name containing the type, example "a.b.c"
	 * @param cuName simple name of compilation unit containing the type, example "Something.java"
	 * @param typeName $ qualified type name, example "Something" or "NonPublic" or "Something$Inner"
	 * @return line breakpoint
	 * @throws Exception
	 */
	protected IJavaLineBreakpoint createLineBreakpoint(int lineNumber, String packageName, String cuName, String typeName) throws Exception {
		IType type = getType(packageName, cuName, typeName);
		return createLineBreakpoint(type, lineNumber);
	}
	
	/**
	 * Creates a line breakpoint in the given type at the given line number.
	 * 
	 * @param type type in which to install the breakpoint
	 * @param lineNumber line number to install the breakpoint at
	 * @return line breakpoint
	 * @throws Exception
	 */
	protected IJavaLineBreakpoint createLineBreakpoint(IType type, int lineNumber) throws Exception {
		IMember member = null;
		if (type != null) {
			IJavaElement sourceElement = null;
			String source = null;
			if (type.isBinary()) {
				IClassFile classFile = type.getClassFile();
				source = classFile.getSource();
				sourceElement = classFile;
			} else {
				ICompilationUnit unit = type.getCompilationUnit();
				source = unit.getSource();
				sourceElement = unit;
			}
			// translate line number to offset
			if (source != null) {
				Document document = new Document(source);
				IRegion region = document.getLineInformation(lineNumber);
				if (sourceElement instanceof ICompilationUnit) {
					member = (IMember) ((ICompilationUnit)sourceElement).getElementAt(region.getOffset());
				} else {
					member = (IMember) ((IClassFile)sourceElement).getElementAt(region.getOffset());
				}
			}
		}
		Map map = getExtraBreakpointAttributes(member);
		IJavaLineBreakpoint bp = JDIDebugModel.createLineBreakpoint(getBreakpointResource(type), type.getFullyQualifiedName(), lineNumber, -1, -1, 0, true, map);
		forceDeltas(bp);
		return bp;
	}
	
	/**
	 * Forces marker deltas to be sent based on breakpoint creation.
	 * 
	 * @param breakpoint
	 */
	private void forceDeltas(IBreakpoint breakpoint) throws CoreException {
		IProject project = breakpoint.getMarker().getResource().getProject();
		if (project != null) {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
		}
	}
	
	/**
	 * Returns the type in the test project based on the given name. The type name may refer to a 
	 * top level non public type.
	 * 
	 * @param packageName package name, example "a.b.c"
	 * @param cuName simple compilation unit name within the package, example "Something.java"
	 * @param typeName simple dot qualified type name, example "Something" or "NonPublic" or "Something.Inner"
	 * @return associated type or <code>null</code> if none
	 * @throws Exception
	 */
	protected IType getType(String packageName, String cuName, String typeName) throws Exception {
		IPackageFragment[] packageFragments = getJavaProject().getPackageFragments();
		for (int i = 0; i < packageFragments.length; i++) {
			IPackageFragment fragment = packageFragments[i];
			if (fragment.getElementName().equals(packageName)) {
				ICompilationUnit compilationUnit = fragment.getCompilationUnit(cuName);
				String[] names = typeName.split("\\$"); //$NON-NLS-1$
				IType type = compilationUnit.getType(names[0]);
				for (int j = 1; j < names.length; j++) {
					type = type.getType(names[j]);
				}
				if (type.exists()) {
					return type;
				}
			}
		}
		return null;
	}
	
	/**
	 * Creates and returns a map of java element breakpoint attributes for a breakpoint on the
	 * given java element, or <code>null</code> if none
	 * 
	 * @param element java element the breakpoint is associated with
	 * @return map of breakpoint attributes or <code>null</code>
	 * @throws Exception
	 */
	protected Map getExtraBreakpointAttributes(IMember element) throws Exception {
		if (element != null && element.exists()) {
			Map map = new HashMap();
			ISourceRange sourceRange = element.getSourceRange();
			int start = sourceRange.getOffset();
			int end = start + sourceRange.getLength();
			IType type = null;
			if (element instanceof IType) {
				type = (IType) element;
			} else {
				type = element.getDeclaringType();
			}
			BreakpointUtils.addJavaBreakpointAttributesWithMemberDetails(map, type, start, end);
			return map;
		}
		return null;
	}	
	
	/**
	 * Creates and returns a line breakpoint at the given line number in the type with the
	 * given name and sets the specified condition on the breakpoint.
	 * 
	 * @param lineNumber line number
	 * @param typeName type name
	 * @param condition condition
	 */
	protected IJavaLineBreakpoint createConditionalLineBreakpoint(int lineNumber, String typeName, String condition, boolean suspendOnTrue) throws Exception {
		IJavaLineBreakpoint bp = createLineBreakpoint(lineNumber, typeName);
		bp.setCondition(condition);
		bp.setConditionEnabled(true);
		bp.setConditionSuspendOnTrue(suspendOnTrue);
		return bp;
	}
	
	/**
	 * Creates and returns a pattern breakpoint at the given line number in the
	 * source file with the given name.
	 * 
	 * @param lineNumber line number
	 * @param sourceName name of source file
	 * @param pattern the pattern of the class file name
	 */
	protected IJavaPatternBreakpoint createPatternBreakpoint(int lineNumber, String sourceName, String pattern) throws Exception {
		return JDIDebugModel.createPatternBreakpoint(getJavaProject().getProject(), sourceName, pattern, lineNumber, -1, -1, 0, true, null);
	}
	
	/**
	 * Creates and returns a target pattern breakpoint at the given line number in the
	 * source file with the given name.
	 * 
	 * @param lineNumber line number
	 * @param sourceName name of source file
	 */
	protected IJavaTargetPatternBreakpoint createTargetPatternBreakpoint(int lineNumber, String sourceName) throws Exception {
		return JDIDebugModel.createTargetPatternBreakpoint(getJavaProject().getProject(), sourceName, lineNumber, -1, -1, 0, true, null);
	}	
	
	/**
	 * Creates and returns a stratum breakpoint at the given line number in the
	 * source file with the given name.
	 * 
	 * @param lineNumber line number
	 * @param sourceName name of source file
	 * @param stratum the stratum of the source file
	 */
	protected IJavaStratumLineBreakpoint createStratumBreakpoint(int lineNumber, String sourceName, String stratum) throws Exception {
		return JDIDebugModel.createStratumBreakpoint(getJavaProject().getProject(), stratum, sourceName, null, null, lineNumber, -1, -1, 0, true, null);
	}
	
	/**
	 * Creates and returns a method breakpoint
	 * 
	 * @param typeNamePattern type name pattern
	 * @param methodName method name
	 * @param methodSignature method signature or <code>null</code>
	 * @param entry whether to break on entry
	 * @param exit whether to break on exit
	 */
	protected IJavaMethodBreakpoint createMethodBreakpoint(String typeNamePattern, String methodName, String methodSignature, boolean entry, boolean exit) throws Exception {
		return createMethodBreakpoint(getJavaProject(), typeNamePattern, methodName, methodSignature, entry, exit);
	}	
	
	/**
	 * Creates and returns a method breakpoint
	 * 
	 * @param project java project
	 * @param typeNamePattern type name pattern
	 * @param methodName method name
	 * @param methodSignature method signature or <code>null</code>
	 * @param entry whether to break on entry
	 * @param exit whether to break on exit
	 */
	protected IJavaMethodBreakpoint createMethodBreakpoint(IJavaProject project, String typeNamePattern, String methodName, String methodSignature, boolean entry, boolean exit) throws Exception {
		IMethod method= null;
		IResource resource = project.getProject();
		if (methodSignature != null && methodName != null) {
			IType type = project.findType(typeNamePattern);
			if (type != null ) {
				resource = getBreakpointResource(type);
				method = type.getMethod(methodName, Signature.getParameterTypes(methodSignature));
			}
		}
		Map map = getExtraBreakpointAttributes(method);
		IJavaMethodBreakpoint bp = JDIDebugModel.createMethodBreakpoint(resource, typeNamePattern, methodName, methodSignature, entry, exit,false, -1, -1, -1, 0, true, map);
		forceDeltas(bp);
		return bp;
	}	
	
	/**
	 * Creates a method breakpoint in a fully specified type (potentially non public).
	 * 
	 * @param packageName package name containing type to install breakpoint in, example "a.b.c"
	 * @param cuName simple compilation unit name within package, example "Something.java"
	 * @param typeName $ qualified type name within compilation unit, example "Something" or
	 *  "NonPublic" or "Something$Inner"
	 * @param methodName method or <code>null</code> for all methods
	 * @param methodSignature JLS method signature or <code>null</code> for all methods with the given name
	 * @param entry whether to break on entry
	 * @param exit whether to break on exit
	 * @return method breakpoint
	 * @throws Exception
	 */
	protected IJavaMethodBreakpoint createMethodBreakpoint(String packageName, String cuName, String typeName, String methodName, String methodSignature, boolean entry, boolean exit) throws Exception {
		IType type = getType(packageName, cuName, typeName);
		assertNotNull("did not find type to install breakpoint in", type); //$NON-NLS-1$
		IMethod method= null;
		if (methodSignature != null && methodName != null) {
			if (type != null ) {
				method = type.getMethod(methodName, Signature.getParameterTypes(methodSignature));
			}
		}
		Map map = getExtraBreakpointAttributes(method);
		IJavaMethodBreakpoint bp = JDIDebugModel.createMethodBreakpoint(getBreakpointResource(type), type.getFullyQualifiedName(), methodName, methodSignature, entry, exit,false, -1, -1, -1, 0, true, map);
		forceDeltas(bp);
		return bp;
	}
		

	/**
	 * Creates a MethodBreakPoint on the method specified at the given path. 
	 * Syntax:
	 * Type$InnerType$MethodNameAndSignature$AnonymousTypeDeclarationNumber$FieldName
	 * eg:<code>
	 * public class Foo{
	 * 		class Inner
	 * 		{
	 * 			public void aMethod()
	 * 			{
	 * 				Object anon = new Object(){
	 * 					int anIntField;
	 * 					String anonTypeMethod() {return "an Example";}				
	 * 				}
	 * 			}
	 * 		}
	 * }</code>
	 * Syntax to get the anonymous toString would be: Foo$Inner$aMethod()V$1$anonTypeMethod()QString
	 * so, createMethodBreakpoint(packageName, cuName, "Foo$Inner$aMethod()V$1$anonTypeMethod()QString",true,false);
	 */
	protected IJavaMethodBreakpoint createMethodBreakpoint(String root, String packageName, String cuName, 
									String fullTargetName, boolean entry, boolean exit) throws Exception {
		
		IJavaProject javaProject = getJavaProject();
		ICompilationUnit cunit = getCompilationUnit(javaProject, root, packageName, cuName);
		assertNotNull("did not find requested Compilation Unit", cunit); //$NON-NLS-1$
		IMethod targetMethod = (IMethod)(new MemberParser()).getDeepest(cunit,fullTargetName);
		assertNotNull("did not find requested method", targetMethod); //$NON-NLS-1$
		assertTrue("Given method does not exist", targetMethod.exists()); //$NON-NLS-1$
		IType methodParent = (IType)targetMethod.getParent();//safe - method's only parent = Type
		assertNotNull("did not find type to install breakpoint in", methodParent); //$NON-NLS-1$
				
		Map map = getExtraBreakpointAttributes(targetMethod);
		IJavaMethodBreakpoint bp = JDIDebugModel.createMethodBreakpoint(getBreakpointResource(methodParent), methodParent.getFullyQualifiedName(),targetMethod.getElementName(), targetMethod.getSignature(), entry, exit,false, -1, -1, -1, 0, true, map);
		forceDeltas(bp);
		return bp;
	}		
	
	/**
	 * @param cu the Compilation where the target resides
	 * @param target the full name of the target, as per MemberParser syntax
	 * @return the requested Member
	 */
	protected IMember getMember(ICompilationUnit cu, String target) {
		IMember toReturn = (new MemberParser()).getDeepest(cu,target);
		return toReturn;
	}
	
	/**
	 * Delegate method to get a resource with a specific name from the testing workspace 'src' folder
	 * @param name the name of the <code>IResource</code> to get
	 * @return the specified <code>IResource</code> or <code>null</code> if it does not exist
	 * 
	 * @since 3.4
	 */
	protected IResource getResource(String name) {
		return ResourcesPlugin.getWorkspace().getRoot().findMember(new Path("/DebugTests/src/"+name));
	}
	
	/**
	 * Creates and returns a class prepare breakpoint on the type with the given fully qualified name.
	 * 
	 * @param typeName type on which to create the breakpoint
	 * @return breakpoint
	 * @throws Exception
	 */
	protected IJavaClassPrepareBreakpoint createClassPrepareBreakpoint(String typeName) throws Exception {
		return createClassPrepareBreakpoint(getType(typeName));
	}
	
	/**
	 * Creates and returns a class prepare breakpoint on the type with the given fully qualified name.
	 * 
	 * @param typeName type on which to create the breakpoint
	 * @return breakpoint
	 * @throws Exception
	 */
	protected IJavaClassPrepareBreakpoint createClassPrepareBreakpoint(String root,
			String packageName, String cuName, String fullTargetName) throws Exception {
		ICompilationUnit cunit = getCompilationUnit(getJavaProject(), root, packageName, cuName);
		IType type = (IType)getMember(cunit,fullTargetName);
		assertTrue("Target type not found", type.exists()); //$NON-NLS-1$
		return createClassPrepareBreakpoint(type);
	}	
	
	/**
	 * Creates a class prepare breakpoint in a fully specified type (potentially non public).
	 * 
	 * @param packageName package name containing type to install breakpoint in, example "a.b.c"
	 * @param cuName simple compilation unit name within package, example "Something.java"
	 * @param typeName $ qualified type name within compilation unit, example "Something" or
	 *  "NonPublic" or "Something$Inner"
	 */	
	protected IJavaClassPrepareBreakpoint createClassPrepareBreakpoint(String packageName, String cuName, String typeName) throws Exception {
		return createClassPrepareBreakpoint(getType(packageName, cuName, typeName));
	}
	
	/**
	 * Creates a class prepare breakpoint for the given type
	 * 
	 * @param type type
	 * @return class prepare breakpoint
	 * @throws Exception
	 */
	protected IJavaClassPrepareBreakpoint createClassPrepareBreakpoint(IType type) throws Exception {
		assertNotNull("type not specified for class prepare breakpoint", type); //$NON-NLS-1$
		int kind = IJavaClassPrepareBreakpoint.TYPE_CLASS;
		if (type.isInterface()) {
			kind = IJavaClassPrepareBreakpoint.TYPE_INTERFACE;
		}
		Map map = getExtraBreakpointAttributes(type);
		IJavaClassPrepareBreakpoint bp = JDIDebugModel.createClassPrepareBreakpoint(getBreakpointResource(type), type.getFullyQualifiedName(), kind, -1, -1, true, map);
		forceDeltas(bp);
		return bp;
	}
	
	/**
	 * Returns the Java model type from the test project with the given name or <code>null</code>
	 * if none.
	 * 
	 * @param typeName
	 * @return type or <code>null</code>
	 * @throws Exception
	 */
	protected IType getType(String typeName) throws Exception {
		return getJavaProject().findType(typeName);
	}
	
	/**
	 * Creates and returns a watchpoint
	 * 
	 * @param typeNmae type name
	 * @param fieldName field name
	 * @param access whether to suspend on field access
	 * @param modification whether to suspend on field modification
	 */	
	protected IJavaWatchpoint createWatchpoint(String typeName, String fieldName, boolean access, boolean modification) throws Exception {
		IType type = getType(typeName);
		return createWatchpoint(type, fieldName, access, modification);
	}
	
	/**
	 * Creates and returns an exception breakpoint
	 * 
	 * @param exName exception name
	 * @param caught whether to suspend in caught locations
	 * @param uncaught whether to suspend in uncaught locations
	 */	
	protected IJavaExceptionBreakpoint createExceptionBreakpoint(String exName, boolean caught, boolean uncaught) throws Exception {
		IType type = getType(exName);
		Map map = getExtraBreakpointAttributes(type);
		IJavaExceptionBreakpoint bp = JDIDebugModel.createExceptionBreakpoint(getBreakpointResource(type),exName, caught, uncaught, false, true, map);
		forceDeltas(bp);
		return bp;
	}
	
	/**
	 * Creates and returns a watchpoint
	 * 
	 * @param typeNmae type name
	 * @param fieldName field name
	 * @param access whether to suspend on field access
	 * @param modification whether to suspend on field modification
	 */	
/*	protected IJavaWatchpoint createWatchpoint(String typeName, String fieldName, boolean access, boolean modification) throws Exception {
		IType type = getType(typeName);
		return createWatchpoint(type, fieldName, access, modification);
	}*/


	/**
	 * Creates a WatchPoint on the field specified at the given path.
	 * Will create watchpoints on fields within anonymous types, inner types,
	 * local (non-public) types, and public types.  
	 * @param root
	 * @param packageName package name containing type to install breakpoint in, example "a.b.c"
	 * @param cuName simple compilation unit name within package, example "Something.java"
	 * @param fullTargetName - see below
	 * @param access whether to suspend on access 
	 * @param modification whether to suspend on modification
	 * @return a watchpoint
	 * @throws Exception
	 * @throws CoreException
	 * 
	 * <p>
	 * <pre>
	 * Syntax example:
	 * Type$InnerType$MethodNameAndSignature$AnonymousTypeDeclarationNumber$FieldName
	 * eg:
	 * public class Foo{
	 * 		class Inner
	 * 		{
	 * 			public void aMethod()
	 * 			{
	 * 				Object anon = new Object(){
	 * 					int anIntField;
	 * 					String anonTypeMethod() {return "an Example";}				
	 * 				}
	 * 			}
	 * 		}
	 * }</pre>
	 * </p>
	 * To get the anonymous toString, syntax of fullTargetName would be: <code>Foo$Inner$aMethod()V$1$anIntField</code> 
	 */
	protected IJavaWatchpoint createNestedTypeWatchPoint(String root, String packageName, String cuName, 
			String fullTargetName, boolean access, boolean modification) throws Exception, CoreException {
		
		ICompilationUnit cunit = getCompilationUnit(getJavaProject(), root, packageName, cuName);
		IField field = (IField)getMember(cunit,fullTargetName);
		assertNotNull("Path to field is not valid", field); //$NON-NLS-1$
		assertTrue("Field is not valid", field.exists()); //$NON-NLS-1$
		IType type = (IType)field.getParent();
		return createWatchpoint(type, field.getElementName(), access, modification);
	}
	

	/**
	 * Creates a watchpoint in a fully specified type (potentially non public).
	 * 
	 * @param packageName package name containing type to install breakpoint in, example "a.b.c"
	 * @param cuName simple compilation unit name within package, example "Something.java"
	 * @param typeName $ qualified type name within compilation unit, example "Something" or
	 *  "NonPublic" or "Something$Inner"
	 * @param fieldName name of the field
	 * @param access whether to suspend on access 
	 * @param modification whether to suspend on modification
	 */		
	protected IJavaWatchpoint createWatchpoint(String packageName, String cuName, String typeName, String fieldName, boolean access, boolean modification) throws Exception {
		IType type = getType(packageName, cuName, typeName);
		return createWatchpoint(type, fieldName, access, modification);
	}
	
	/**
	 * Creates a watchpoint on the specified field.
	 * 
	 * @param type type containing the field
	 * @param fieldName name of the field
	 * @param access whether to suspend on access
	 * @param modification whether to suspend on modification
	 * @return watchpoint
	 * @throws Exception
	 */
	protected IJavaWatchpoint createWatchpoint(IType type, String fieldName, boolean access, boolean modification) throws Exception, CoreException {
		assertNotNull("type not specified for watchpoint", type); //$NON-NLS-1$
		IField field = type.getField(fieldName);
		Map map = getExtraBreakpointAttributes(field);
		IJavaWatchpoint wp = JDIDebugModel.createWatchpoint(getBreakpointResource(type), type.getFullyQualifiedName(), fieldName, -1, -1, -1, 0, true, map);
		wp.setAccess(access);
		wp.setModification(modification);
		forceDeltas(wp);
		return wp;
	}	
		
	/**
	 * Terminates the given thread and removes its launch
	 */
	protected void terminateAndRemove(IJavaThread thread) {
		if (thread != null) {
			terminateAndRemove((IJavaDebugTarget)thread.getDebugTarget());
		}
	}
	
	/**
	 * Terminates the given debug target and removes its launch.
	 * 
	 * NOTE: all breakpoints are removed, all threads are resumed, and then
	 * the target is terminated. This avoids defunct processes on Linux.
	 */
	protected void terminateAndRemove(IJavaDebugTarget debugTarget) {
	    ILaunch launch = debugTarget.getLaunch();
		if (!(debugTarget.isTerminated() || debugTarget.isDisconnected())) {
			IPreferenceStore jdiUIPreferences = JDIDebugUIPlugin.getDefault().getPreferenceStore();
			jdiUIPreferences.setValue(IJDIPreferencesConstants.PREF_SUSPEND_ON_UNCAUGHT_EXCEPTIONS, false);
			
			DebugEventWaiter waiter = new DebugElementEventWaiter(DebugEvent.TERMINATE, debugTarget);
			try {
				removeAllBreakpoints();
				IThread[] threads = debugTarget.getThreads();
				for (int i = 0; i < threads.length; i++) {
					IThread thread = threads[i];
					try {
						if (thread.isSuspended()) {
							thread.resume();
						}
					} catch (CoreException e) {
					}
				}
				debugTarget.getDebugTarget().terminate();
				waiter.waitForEvent();
			} catch (CoreException e) {
			}
		}
		getLaunchManager().removeLaunch(launch);
        // ensure event queue is flushed
        DebugEventWaiter waiter = new DebugElementEventWaiter(DebugEvent.MODEL_SPECIFIC, this);
        DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[]{new DebugEvent(this, DebugEvent.MODEL_SPECIFIC)});
        waiter.waitForEvent();
	}
	
	/**
	 * Deletes all existing breakpoints
	 */
	protected void removeAllBreakpoints() {
		IBreakpoint[] bps = getBreakpointManager().getBreakpoints();
		try {
			getBreakpointManager().removeBreakpoints(bps, true);
		} catch (CoreException e) {
		}
	}
	
	/**
	 * Returns the first breakpoint the given thread is suspended
	 * at, or <code>null</code> if none.
	 * 
	 * @return the first breakpoint the given thread is suspended
	 * at, or <code>null</code> if none
	 */
	protected IBreakpoint getBreakpoint(IThread thread) {
		IBreakpoint[] bps = thread.getBreakpoints();
		if (bps.length > 0) {
			return bps[0];
		}
		return null;
	}
	
	/**
	 * Evaluates the given snippet in the context of the given stack frame and returns
	 * the result.
	 * 
	 * @param snippet code snippet
	 * @param frame stack frame context
	 * @return evaluation result
	 */
	protected IEvaluationResult evaluate(String snippet, IJavaStackFrame frame) throws Exception {
		DebugEventWaiter waiter= new DebugElementKindEventWaiter(DebugEvent.SUSPEND, IJavaThread.class);
		waiter.setTimeout(DEFAULT_TIMEOUT);
		
		IAstEvaluationEngine engine = EvaluationManager.newAstEvaluationEngine(getJavaProject(), (IJavaDebugTarget)frame.getDebugTarget());
		engine.evaluate(snippet, frame, this, DebugEvent.EVALUATION, true);

		Object suspendee= waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		assertNotNull("Program did not suspend.", suspendee); //$NON-NLS-1$
		engine.dispose();
		return fEvaluationResult;
	}		
	
	/**
	 * @see IEvaluationListener#evaluationComplete(IEvaluationResult)
	 */
	public void evaluationComplete(IEvaluationResult result) {
		fEvaluationResult = result;
	}
	
	/**
	 * Performs a step over in the given stack frame and returns when complete.
	 * 
	 * @param frame stack frame to step in
	 */
	protected IJavaThread stepOver(IJavaStackFrame frame) throws Exception {
		DebugEventWaiter waiter= new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, IJavaThread.class, DebugEvent.STEP_END);
		waiter.setTimeout(DEFAULT_TIMEOUT);
		
		frame.stepOver();
		
		Object suspendee= waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		assertNotNull("Program did not suspend.", suspendee); //$NON-NLS-1$
		return (IJavaThread) suspendee;
	}
	
	/**
	 * Performs a step over in the given stack frame and returns when a breakpoint is hit.
	 * 
	 * @param frame stack frame to step in
	 */
	protected IJavaThread stepOverToBreakpoint(IJavaStackFrame frame) throws Exception {
		DebugEventWaiter waiter= new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, IJavaThread.class, DebugEvent.BREAKPOINT);
		waiter.setTimeout(DEFAULT_TIMEOUT);
		
		frame.stepOver();
		
		Object suspendee= waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		assertNotNull("Program did not suspend.", suspendee); //$NON-NLS-1$
		return (IJavaThread) suspendee;
	}	

	/**
	 * Performs a step into in the given stack frame and returns when complete.
	 * 
	 * @param frame stack frame to step in
	 */
	protected IJavaThread stepInto(IJavaStackFrame frame) throws Exception {
		DebugEventWaiter waiter= new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, IJavaThread.class, DebugEvent.STEP_END);
		waiter.setTimeout(DEFAULT_TIMEOUT);
		
		frame.stepInto();
		
		Object suspendee= waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		assertNotNull("Program did not suspend.", suspendee); //$NON-NLS-1$
		return (IJavaThread) suspendee;		
	}
	
	/**
	 * Performs a step return in the given stack frame and returns when complete.
	 * 
	 * @param frame stack frame to step return from
	 */
	protected IJavaThread stepReturn(IJavaStackFrame frame) throws Exception {
		DebugEventWaiter waiter= new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, IJavaThread.class, DebugEvent.STEP_END);
		waiter.setTimeout(DEFAULT_TIMEOUT);
		
		frame.stepReturn();
		
		Object suspendee= waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		assertNotNull("Program did not suspend.", suspendee); //$NON-NLS-1$
		return (IJavaThread) suspendee;
	}	
	
	/**
	 * Performs a step into with filters in the given stack frame and returns when
	 * complete.
	 * 
	 * @param frame stack frame to step in
	 */
	protected IJavaThread stepIntoWithFilters(IJavaStackFrame frame) throws Exception {
		return stepIntoWithFilters(frame, true);
	}
	
	/**
	 * Performs a step into with filters in the given stack frame and returns when
	 * complete.
	 * 
	 * @param whether to step thru or step return from a filtered location
	 * @param frame stack frame to step in
	 */
	protected IJavaThread stepIntoWithFilters(IJavaStackFrame frame, boolean stepThru) throws Exception {
		DebugEventWaiter waiter= new DebugElementKindEventWaiter(DebugEvent.SUSPEND, IJavaThread.class);
		waiter.setTimeout(DEFAULT_TIMEOUT);
		
		// turn filters on
		IJavaDebugTarget target = (IJavaDebugTarget) frame.getDebugTarget();
		try {
			target.setStepFiltersEnabled(true);
			target.setStepThruFilters(stepThru);
			frame.stepInto();
			Object suspendee= waiter.waitForEvent();
			setEventSet(waiter.getEventSet());
			assertNotNull("Program did not suspend.", suspendee); //$NON-NLS-1$
			return (IJavaThread) suspendee;
		} catch (DebugException e) {
			tryTestAgain(e);
		} finally {
			// turn filters off
			target.setStepFiltersEnabled(false);
			target.setStepThruFilters(true);
		}
		return null;
	}	

	/**
	 * Performs a step return with filters in the given stack frame and returns when
	 * complete.
	 * 
	 * @param frame stack frame to step in
	 */
	protected IJavaThread stepReturnWithFilters(IJavaStackFrame frame) throws Exception {
		DebugEventWaiter waiter= new DebugElementKindEventWaiter(DebugEvent.SUSPEND, IJavaThread.class);
		waiter.setTimeout(DEFAULT_TIMEOUT);
		
		// turn filters on
		IJavaDebugTarget target = (IJavaDebugTarget) frame.getDebugTarget();
		try {
			target.setStepFiltersEnabled(true);
			frame.stepReturn();
		} catch (DebugException e) {
			tryTestAgain(e);
		} finally {
			// turn filters off
			target.setStepFiltersEnabled(false);
		}
		
		
		Object suspendee= waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		assertNotNull("Program did not suspend.", suspendee); //$NON-NLS-1$
		return (IJavaThread) suspendee;		
	}	
	
	/**
	 * Performs a step over with filters in the given stack frame and returns when
	 * complete.
	 * 
	 * @param frame stack frame to step in
	 */
	protected IJavaThread stepOverWithFilters(IJavaStackFrame frame) throws Exception {
		DebugEventWaiter waiter= new DebugElementKindEventWaiter(DebugEvent.SUSPEND, IJavaThread.class);
		waiter.setTimeout(DEFAULT_TIMEOUT);
		
		// turn filters on
		IJavaDebugTarget target = (IJavaDebugTarget) frame.getDebugTarget();
		try {
			target.setStepFiltersEnabled(true);
			frame.stepOver();
		} catch (DebugException e) {
			tryTestAgain(e);
		} finally {
			// turn filters off
			target.setStepFiltersEnabled(false);
		}
		
		
		Object suspendee= waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		assertNotNull("Program did not suspend.", suspendee); //$NON-NLS-1$
		return (IJavaThread) suspendee;		
	}

	/**
	 * Returns the compilation unit with the given name.
	 * 
	 * @param project the project containing the CU
	 * @param root the name of the source folder in the project
	 * @param pkg the name of the package (empty string for default package)
	 * @param name the name of the CU (ex. Something.java)
	 * @return compilation unit
	 */
	protected ICompilationUnit getCompilationUnit(IJavaProject project, String root, String pkg, String name) {
		IProject p = project.getProject();
		IResource r = p.getFolder(root);
		return project.getPackageFragmentRoot(r).getPackageFragment(pkg).getCompilationUnit(name);
	}
	
    /**
     * Wait for builds to complete
     */
    public static void waitForBuild() {
        boolean wasInterrupted = false;
        do {
            try {
                Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
                Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
                wasInterrupted = false;
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                wasInterrupted = true;
            }
        } while (wasInterrupted);
    }	
    
    
    /**
     * Finds the specified variable within the context of the specified stackframe. Returns null if a variable with
     * the given name does not exist
     * @param frame
     * @param name
     * @return the <code>IJavaVariable</code> with the given name or <code>null</code> if it
     * does not exist
     * @throws DebugException
     */
    protected IJavaVariable findVariable(IJavaStackFrame frame, String name) throws DebugException {
        IJavaVariable variable = frame.findVariable(name);
        if (variable == null) {
            // dump visible variables
            IDebugModelPresentation presentation = DebugUIPlugin.getModelPresentation();
            System.out.println("Could not find variable '" + name + "' in frame: " + presentation.getText(frame)); //$NON-NLS-1$ //$NON-NLS-2$
            System.out.println("Visible variables are:"); //$NON-NLS-1$
            IVariable[] variables = frame.getVariables();
            for (int i = 0; i < variables.length; i++) {
                IVariable variable2 = variables[i];
                System.out.println("\t" + presentation.getText(variable2)); //$NON-NLS-1$
            }
            if (!frame.isStatic()) {
                variables = frame.getThis().getVariables();
                for (int i = 0; i < variables.length; i++) {
                    IVariable variable2 = variables[i];
                    System.out.println("\t" + presentation.getText(variable2)); //$NON-NLS-1$
                }
            }
        }
        return variable;
    }
	
	/**
	 * Returns if the local filesystem is case-sensitive or not
	 * @return true if the local filesystem is case-sensitive, false otherwise
	 */
	protected boolean isFileSystemCaseSensitive() {
		return Platform.OS_MACOSX.equals(Platform.getOS()) ? false : new File("a").compareTo(new File("A")) != 0; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
    /**
     * Creates a shared launch configuration for the type with the given name.
     */
    protected void createLaunchConfiguration(String mainTypeName) throws Exception {
        createLaunchConfiguration(getJavaProject(), mainTypeName);
    }
    
    /**
     * Creates a shared launch configuration for the type with the given name.
     */
    protected void createLaunchConfiguration(IJavaProject project, String mainTypeName) throws Exception {
        ILaunchConfigurationType type = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
        ILaunchConfigurationWorkingCopy config = type.newInstance(project.getProject().getFolder(LAUNCHCONFIGURATIONS), mainTypeName);
        config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainTypeName);
        config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getElementName());
        // use 'java' instead of 'javaw' to launch tests (javaw is problematic
        // on JDK1.4.2)
        Map map = new HashMap(1);
        map.put(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, JAVA);
        config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE_SPECIFIC_ATTRS_MAP, map);
        config.doSave();
    }    

    /**
     * Creates a shared launch configuration for the type with the given name.
     */
    protected void createLaunchConfiguration(IJavaProject project, String containername, String mainTypeName) throws Exception {
        ILaunchConfigurationType type = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
        ILaunchConfigurationWorkingCopy config = type.newInstance(project.getProject().getFolder(containername), mainTypeName);
        config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainTypeName);
        config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getElementName());
        // use 'java' instead of 'javaw' to launch tests (javaw is problematic
        // on JDK1.4.2)
        Map map = new HashMap(1);
        map.put(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, JAVA);
        config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE_SPECIFIC_ATTRS_MAP, map);
        config.doSave();
    }
    
	/**
	 * When a test throws the 'try again' exception, try it again.
	 * @see junit.framework.TestCase#runBare()
	 */
	public void runBare() throws Throwable {
		boolean tryAgain = true;
		int attempts = 0;
		while (tryAgain) {
			try {
				attempts++;
				super.runBare();
				tryAgain = false;
			} catch (TestAgainException e) {
				Status status = new Status(IStatus.ERROR, "org.eclipse.jdt.debug.tests", "Test failed attempt " + attempts + ". Re-testing: " + this.getName(), e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				DebugPlugin.log(status);
				if (attempts > 9) {
					tryAgain = false;
				}
			}
		}
	}
    
	/**
	 * Opens and returns an editor on the given file or <code>null</code>
	 * if none. The editor will be activated.
	 * 
	 * @param file
	 * @return editor or <code>null</code>
	 */
	protected IEditorPart openEditor(final IFile file) throws PartInitException, InterruptedException {
		Display display = DebugUIPlugin.getStandardDisplay();
		if (Thread.currentThread().equals(display.getThread())) {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			return IDE.openEditor(page, file, true);
		} else {
			final IEditorPart[] parts = new IEditorPart[1];
			WorkbenchJob job = new WorkbenchJob(display, "open editor") {
				public IStatus runInUIThread(IProgressMonitor monitor) {
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					try {
						parts[0] = IDE.openEditor(page, file, true);
					} catch (PartInitException e) {
						return e.getStatus();
					}
					return Status.OK_STATUS;
				}
			};
			job.schedule();
			job.join();
			return parts[0];
		}
	}
	
	/**
	 * Toggles a breakpoint in the editor at the given line number returning the breakpoint
	 * or <code>null</code> if none.
	 * 
	 * @param editor
	 * @param lineNumber
	 * @return returns the created breakpoint or <code>null</code> if none.
	 * @throws InterruptedException
	 */
	protected IBreakpoint toggleBreakpoint(final IEditorPart editor, int lineNumber) throws InterruptedException {
		final IVerticalRulerInfo info = new VerticalRulerInfoStub(lineNumber-1); // sub 1, as the doc lines start at 0
		WorkbenchJob job = new WorkbenchJob(DebugUIPlugin.getStandardDisplay(), "toggle breakpoint") {
			public IStatus runInUIThread(IProgressMonitor monitor) {
				ToggleBreakpointAction action = new ToggleBreakpointAction(editor, null, info);
				action.run();
				return Status.OK_STATUS;
			}
		};
		final Object lock = new Object();
		final IBreakpoint[] breakpoints = new IBreakpoint[1];
		IBreakpointListener listener = new IBreakpointListener() {
			public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
			}
			public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
			}
			public void breakpointAdded(IBreakpoint breakpoint) {
				synchronized (lock) {
					breakpoints[0] = breakpoint;
					lock.notifyAll();
				}
			}
		};
		IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
		manager.addBreakpointListener(listener);
		synchronized (lock) {
			job.schedule();
			lock.wait(DEFAULT_TIMEOUT);
		}
		manager.removeBreakpointListener(listener);
		return breakpoints[0];
	}
	
	/**
	 * Closes all editors in the active workbench page.
	 */
	protected void closeAllEditors() {
	    Runnable closeAll = new Runnable() {
            public void run() {
                IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                activeWorkbenchWindow.getActivePage().closeAllEditors(false);
            }
        };
        Display display = DebugUIPlugin.getStandardDisplay();
        display.syncExec(closeAll);
	}
    
	/**
	 * Returns the version level of the class files being run, based on the system property <code>java.class.version</code>
	 * @return the version level of the class files being run in the current VM
	 *  
	 *  @since 3.6
	 */
	protected String getClassFileVersion() {
		String version = System.getProperty("java.class.version");
		if(version.compareTo("48.0") <= 0) {
			return JavaCore.VERSION_1_4;
		}
		if(version.compareTo("49.0") <= 0) {
			return JavaCore.VERSION_1_5;
		}
		return JavaCore.VERSION_1_6;
	}
	
	/**
	 * Determines if the test should be attempted again based on the error code.
	 * See bug 297071.
	 * 
	 * @param e Debug Exception
	 * @throws TestAgainException
	 * @throws DebugException
	 */
	protected void tryTestAgain(DebugException e) throws Exception {
		Throwable cause = e.getCause();
		if (cause instanceof InternalException) {
			int code = ((InternalException)cause).errorCode();
			if (code == 13) {
				throw new TestAgainException();
			}
		}
		throw e;
	}
	
}

