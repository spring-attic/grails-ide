/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.groovy.debug.core.breakpoints;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaBreakpointListener;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.eval.IEvaluationListener;
import org.eclipse.jdt.debug.eval.IEvaluationResult;
import org.eclipse.jdt.groovy.core.util.ReflectionUtils;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jdt.internal.debug.core.breakpoints.JDIDebugBreakpointMessages;
import org.eclipse.jdt.internal.debug.core.breakpoints.JavaLineBreakpoint;
import org.eclipse.jdt.internal.debug.core.model.JDIThread;
import org.grails.ide.eclipse.groovy.debug.core.GroovyDebugCoreActivator;
import org.grails.ide.eclipse.groovy.debug.core.evaluation.GroovyJDIEvaluator;
import org.grails.ide.eclipse.groovy.debug.core.evaluation.JDITargetDelegate;

import com.sun.jdi.VMDisconnectedException;

/**
 * 
 * @author Andrew Eisenberg
 * @since 2.6.1
 */
public class GroovyConditionalBreakpointHandler {
    
    /**
     * Listens for evaluation completion for condition evaluation.
     * If an evaluation evaluates <code>true</code> or has an error, this breakpoint
     * will suspend the thread in which the breakpoint was hit.
     * If the evaluation returns <code>false</code>, the thread is resumed.
     */
    class EvaluationListener implements IEvaluationListener {
        
        /**
         * Lock for synchronizing evaluation
         */
        private Object fLock = new Object();
        
        /**
         * The breakpoint that was hit
         */
        private JavaLineBreakpoint fBreakpoint;
        
        /**
         * Result of the vote
         */
        private int fVote;
        
        EvaluationListener(JavaLineBreakpoint breakpoint) {
            fBreakpoint = breakpoint;
        }
        
        public void evaluationComplete(IEvaluationResult result) {
            fVote = determineVote(result);
            synchronized (fLock) {
                fLock.notifyAll();
            }
        }
        
        /**
         * Processes the result to determine whether to suspend or resume.
         * 
         * @param result evaluation result
         * @return vote
         */
        private int determineVote(IEvaluationResult result) {
            if (result.isTerminated()) {
                // indicates the user terminated the evaluation
                return IJavaBreakpointListener.SUSPEND;
            }
            JDIThread thread= (JDIThread)result.getThread();
            if (result.hasErrors()) {
                DebugException exception= result.getException();
                Throwable wrappedException= exception.getStatus().getException();
                if (wrappedException instanceof VMDisconnectedException) {
                    // VM terminated/disconnected during evaluation
                    return IJavaBreakpointListener.DONT_SUSPEND;
                } else {
                    fireConditionHasRuntimeErrors(fBreakpoint, exception);
                    return IJavaBreakpointListener.SUSPEND;
                }
            } else {
                try {
                    IValue value= result.getValue();
                    if (fBreakpoint.isConditionSuspendOnTrue()) {
                        try {
                            Boolean b = JDITargetDelegate.convertToBoolean((IJavaValue) value);
                            return b ? IJavaBreakpointListener.SUSPEND : IJavaBreakpointListener.DONT_SUSPEND;
                        } catch (DebugException e) {
                            // result was not boolean
                            fireConditionHasRuntimeErrors(fBreakpoint, new DebugException(
                                    new Status(IStatus.ERROR, JDIDebugPlugin.getUniqueIdentifier(), 
                                            MessageFormat.format(JDIDebugBreakpointMessages.ConditionalBreakpointHandler_1, new String[]{value.getReferenceTypeName()}))));
                            return IJavaBreakpointListener.SUSPEND;
                        }
                    } else {
                        IDebugTarget debugTarget= thread.getDebugTarget();
                        IValue lastValue= setCurrentConditionValue(value, debugTarget);
                        if (!value.equals(lastValue)) {
                            return IJavaBreakpointListener.SUSPEND;
                        } else {
                            return IJavaBreakpointListener.DONT_SUSPEND;
                        }
                    }
                } catch (DebugException e) {
                    // Suspend when an error occurs
                    JDIDebugPlugin.log(e);
                    return IJavaBreakpointListener.SUSPEND;
                }
            }
        }

        /**
         * @param value
         * @param debugTarget
         * @return
         */
        protected IValue setCurrentConditionValue(IValue value,
                IDebugTarget debugTarget) {
            return (IValue) ReflectionUtils.executePrivateMethod(
                    JavaLineBreakpoint.class, "setCurrentConditionValue",
                    new Class[] { IDebugTarget.class, IValue.class },
                    fBreakpoint, new Object[] { debugTarget, value });
        }
    
        /**
         * Result of the conditional expression evaluation - to resume or not resume, that 
         * is the question.
         * 
         * @return vote result
         */
        int getVote() {
            return fVote;
        }
        
        /**
         * Returns the lock object to synchronize this evaluation.
         * 
         * @return lock object
         */
        Object getLock() {
            return fLock;
        }
    }   

    
    /**
     * Whether the condition had compile or runtime errors
     */
    private boolean fHasErrors = false;

    
    /*
     * Closely follow the logic of ConditionalBreakpointListener.breakpointHit()
     */
    public int conditionalBreakpointHit(IJavaThread thread,
            IJavaBreakpoint breakpoint) {
        if (breakpoint instanceof IJavaLineBreakpoint) {
            JavaLineBreakpoint lineBreakpoint = (JavaLineBreakpoint) breakpoint;
            try {
                final String condition= lineBreakpoint.getCondition();
                if (condition == null) {
                    return IJavaBreakpointListener.SUSPEND;
                }
                EvaluationListener listener= new EvaluationListener(lineBreakpoint);
                IJavaStackFrame frame = (IJavaStackFrame) thread.getTopStackFrame(); 
                IJavaProject project= getJavaProject(lineBreakpoint, frame);
                if (project == null) {
                    fireConditionHasErrors(lineBreakpoint, new Message[]{new Message(JDIDebugBreakpointMessages.JavaLineBreakpoint_Unable_to_compile_conditional_breakpoint___missing_Java_project_context__1, -1)});
                    return IJavaBreakpointListener.SUSPEND;
                }
                IJavaDebugTarget target = (IJavaDebugTarget) thread.getDebugTarget();
                GroovyJDIEvaluator evaluator = new GroovyJDIEvaluator(project, target);
                
                Object lock = listener.getLock();
                synchronized (lock) {
                    evaluator.evaluate(lineBreakpoint.getCondition(), null, frame, listener, DebugEvent.EVALUATION_IMPLICIT, false);
                    // TODO: timeout?
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        fireConditionHasRuntimeErrors(lineBreakpoint, new DebugException(
                            new Status(IStatus.ERROR, JDIDebugPlugin.getUniqueIdentifier(), JDIDebugBreakpointMessages.ConditionalBreakpointHandler_0, e)));
                        return IJavaBreakpointListener.SUSPEND;
                    }
                }
                return listener.getVote();
            } catch (CoreException e) {
                DebugException de = null;
                if (e instanceof DebugException) {
                    de = (DebugException) e;
                } else {
                    de = new DebugException(e.getStatus());
                }
                fireConditionHasRuntimeErrors(lineBreakpoint, de);
            }
        }
        return IJavaBreakpointListener.SUSPEND;
    }

    /**
     * @param lineBreakpoint
     * @param frame
     * @return
     */
    protected IJavaProject getJavaProject(JavaLineBreakpoint lineBreakpoint,
            IJavaStackFrame frame) {
        return (IJavaProject) ReflectionUtils.executePrivateMethod(
                JavaLineBreakpoint.class, "getJavaProject",
                new Class[] { IJavaStackFrame.class }, lineBreakpoint,
                new Object[] { frame });
    }
    
    private void fireConditionHasRuntimeErrors(IJavaLineBreakpoint breakpoint, DebugException exception) {
        fHasErrors = true;
        GroovyDebugCoreActivator.log(exception);
        JDIDebugPlugin.getDefault().fireBreakpointHasRuntimeException(breakpoint, exception);
    }


    /**
     * Notifies listeners that a conditional breakpoint expression has been
     * compiled that contains errors
     */
    private void fireConditionHasErrors(IJavaLineBreakpoint breakpoint, Message[] messages) {
        fHasErrors = true;
        JDIDebugPlugin.getDefault().fireBreakpointHasCompilationErrors(breakpoint, messages);
    }

    /**
     * Returns whether errors were encountered when evaluating the condition (compilation or runtime).
     * 
     * @return whether errors were encountered when evaluating the condition
     */
    public boolean hasErrors() {
        return fHasErrors;
    }
}
