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
package org.eclipse.jdt.debug.testplugin;

import java.util.ArrayList;
import java.util.List;

import junit.framework.AssertionFailedError;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaBreakpointListener;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.eval.EvaluationManager;
import org.eclipse.jdt.debug.eval.IAstEvaluationEngine;
import org.eclipse.jdt.debug.eval.IEvaluationListener;
import org.eclipse.jdt.debug.eval.IEvaluationResult;
import org.eclipse.jdt.debug.tests.AbstractDebugTest;

/**
 * Test for breakpoint listener extension. Performs an evaluation while voting
 * whether to resume a thread.
 * @author Andrew Eisenberg
 */
public class EvalualtionBreakpointListener implements IJavaBreakpointListener {
	
	/**
	 * How to vote when hit
	 */
	public static int VOTE = IJavaBreakpointListener.DONT_CARE;
	
	/**
	 * Whether hit
	 */
	public static boolean HIT = false;
	
	/**
	 * Whether removed
	 */
	public static boolean REMOVED = false;
	
	/**
	 * Expression to evaluate when hit
	 */
	public static String EXPRESSION;
	
	/**
	 * Project to compile expression in
	 */
	public static IJavaProject PROJECT;
	
	/**
	 * Evaluation result
	 */
	public static IEvaluationResult RESULT;
	
	/**
	 * List of breakpoints with compilation errors
	 */
	public static List COMPILATION_ERRORS = new ArrayList();
	
	/**
	 * Lock used to notify when a notification is received.
	 */
	public static Object REMOVE_LOCK = new Object();
	
	/**
	 * List of breakpoints with runtime errors
	 */
	public static List RUNTIME_ERRORS = new ArrayList();
	
	public static void reset() {
		VOTE = IJavaBreakpointListener.DONT_CARE;
		EXPRESSION = null;
		PROJECT = null;
		RESULT = null;
		HIT = false;
		REMOVED = false;
		COMPILATION_ERRORS.clear();
		RUNTIME_ERRORS.clear();
	}

	/**
	 * Constructs a breakpoint listener to evaluate an expression when a breakpoint is hit.
	 */
	public EvalualtionBreakpointListener() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.core.IJavaBreakpointListener#addingBreakpoint(org.eclipse.jdt.debug.core.IJavaDebugTarget, org.eclipse.jdt.debug.core.IJavaBreakpoint)
	 */
	public void addingBreakpoint(IJavaDebugTarget target, IJavaBreakpoint breakpoint) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.core.IJavaBreakpointListener#breakpointHasCompilationErrors(org.eclipse.jdt.debug.core.IJavaLineBreakpoint, org.eclipse.jdt.core.dom.Message[])
	 */
	public void breakpointHasCompilationErrors(IJavaLineBreakpoint breakpoint, Message[] errors) {
		COMPILATION_ERRORS.add(breakpoint);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.core.IJavaBreakpointListener#breakpointHasRuntimeException(org.eclipse.jdt.debug.core.IJavaLineBreakpoint, org.eclipse.debug.core.DebugException)
	 */
	public void breakpointHasRuntimeException(IJavaLineBreakpoint breakpoint, DebugException exception) {
		RUNTIME_ERRORS.add(breakpoint);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.core.IJavaBreakpointListener#breakpointHit(org.eclipse.jdt.debug.core.IJavaThread, org.eclipse.jdt.debug.core.IJavaBreakpoint)
	 */
	public int breakpointHit(IJavaThread thread, IJavaBreakpoint breakpoint) {
		HIT = true;
		final Object lock = new Object();
		RESULT = null;
		if (PROJECT != null && EXPRESSION != null) {
			IAstEvaluationEngine engine = EvaluationManager.newAstEvaluationEngine(PROJECT, (IJavaDebugTarget) thread.getDebugTarget());
			IEvaluationListener listener = new IEvaluationListener(){
				public void evaluationComplete(IEvaluationResult result) {
					RESULT = result;
					synchronized (lock) {
						lock.notifyAll();
					}
				}
			};
			try {
				synchronized (lock) {
					engine.evaluate(EXPRESSION, (IJavaStackFrame) thread.getTopStackFrame(), listener, DebugEvent.EVALUATION_IMPLICIT, false);
					lock.wait(AbstractDebugTest.DEFAULT_TIMEOUT);
				}
			} catch (DebugException e) {
				throw new AssertionFailedError(e.getStatus().getMessage());
			} catch (InterruptedException e) {
				throw new AssertionFailedError(e.getMessage());
			}
		}
		return VOTE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.core.IJavaBreakpointListener#breakpointInstalled(org.eclipse.jdt.debug.core.IJavaDebugTarget, org.eclipse.jdt.debug.core.IJavaBreakpoint)
	 */
	public void breakpointInstalled(IJavaDebugTarget target, IJavaBreakpoint breakpoint) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.core.IJavaBreakpointListener#breakpointRemoved(org.eclipse.jdt.debug.core.IJavaDebugTarget, org.eclipse.jdt.debug.core.IJavaBreakpoint)
	 */
	public void breakpointRemoved(IJavaDebugTarget target, IJavaBreakpoint breakpoint) {
		REMOVED = true;
		synchronized (REMOVE_LOCK) {
			REMOVE_LOCK.notifyAll();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.core.IJavaBreakpointListener#installingBreakpoint(org.eclipse.jdt.debug.core.IJavaDebugTarget, org.eclipse.jdt.debug.core.IJavaBreakpoint, org.eclipse.jdt.debug.core.IJavaType)
	 */
	public int installingBreakpoint(IJavaDebugTarget target, IJavaBreakpoint breakpoint, IJavaType type) {
		return IJavaBreakpointListener.DONT_CARE;
	}
	
}
