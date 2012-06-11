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
package org.grails.ide.eclipse.groovy.debug.core;

import org.eclipse.contribution.jdt.debug.IDebugProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaBreakpointListener;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.eval.IEvaluationListener;
import org.eclipse.jdt.groovy.core.util.ContentTypeUtils;
import org.eclipse.jdt.groovy.core.util.ReflectionUtils;
import org.eclipse.jdt.internal.debug.core.breakpoints.ConditionalBreakpointHandler;
import org.grails.ide.eclipse.groovy.debug.core.breakpoints.GroovyConditionalBreakpointHandler;
import org.grails.ide.eclipse.groovy.debug.core.evaluation.GroovyJDIEvaluator;

import com.sun.jdi.Location;

/**
 * Provides Groovy-aware debug support in the display view.
 * @author Andrew Eisenberg
 * @author Andy Clement
 * @since 2.5.1
 */
public class GroovyDebugProvider implements IDebugProvider {
    
    private static final String[] EXTRA_STEP_FILTERS = new String[] {
            // can't put either of these on the app side 
            // or else step into won't work
//            "org.codehaus.groovy.runtime.*",
//            "org.codehaus.groovy.runtime.MetaClassHelper",
            
            // yay! this works
            "groovy.*",
            
            // must do these explicitly since we can't do org.codehaus.groovy.runtime.* 
            "org.codehaus.groovy.runtime.typehandling.*",
            "org.codehaus.groovy.runtime.wrappers.*",
            "org.codehaus.groovy.runtime.metaclass.*",
            "org.codehaus.groovy.runtime.GroovyCategorySupport*",
            "org.codehaus.groovy.runtime.ScriptBytecodeAdapter",
            "org.codehaus.groovy.runtime.InvokerHelper",
            "org.codehaus.groovy.runtime.ArrayUtil",
            "org.codehaus.groovy.runtime.DefaultGroovyMethods",
            "org.codehaus.groovy.runtime.callsite.*",
            "org.codehaus.groovy.runtime.dgmimpl.*",
            "org.codehaus.groovy.runtime.dgm*",
            "org.codehaus.groovy.util.*", 
            "org.codehaus.groovy.reflection.*",
            "org.codehaus.groovy.tools.*",
            "org.codehaus.groovy.classgen.*",
            "org.codehaus.groovy.control.*",
            
            // spring stuff
            "org.springframework.*",
            
            // some other important ones
            "java.*", 
            "groovyjarjar.*", 
            "com.sun.*", 
            "sun.net.*",
            "sun.nio.*",
            // the following 3 cannot be filtered in the debugged side
            // otherwise some step-intos will not work
//            "sun.misc.*",
//            "sun.reflect.*",
//            "sun.*",
            };
    
    



    public void performEvaluation(String snippet, IJavaObject object,
            IJavaStackFrame frame, IEvaluationListener listener, IJavaProject javaProject, int evaluationDetail, boolean hitBreakpoints)
            throws DebugException {
        GroovyJDIEvaluator evaluator = new GroovyJDIEvaluator(javaProject, (IJavaDebugTarget) frame.getDebugTarget());
        try {
            evaluator.evaluate(snippet, object, frame, listener, evaluationDetail, hitBreakpoints);
        } catch (CoreException e) {
            if (e instanceof DebugException) {
                throw (DebugException) e;
            } else {
                throw new DebugException(e.getStatus());
            }
        }
    }

    public boolean shouldPerformEvaluation(IJavaStackFrame frame) throws DebugException {
        String sourcePath = frame.getSourcePath();
        
        if (!GroovyDebugCoreActivator.isDisplayViewEnabled()) {
            return false;
        }
        
        return (ContentTypeUtils.isGroovyLikeFileName(sourcePath)) || (sourcePath != null && sourcePath.endsWith("gsp"));
     }
    
    
    public String[] augmentStepFilters(String[] origStepFilters) {
        if (!GroovyDebugCoreActivator.isStepFilteringEnabled()) {
            return origStepFilters;
        }
        
        if (origStepFilters == null || origStepFilters.length == 0) {
            return EXTRA_STEP_FILTERS;
        } else {
            // look for 'org.codehaus.groovy' to remove
            int ocgIndex = 0;
            while (ocgIndex < origStepFilters.length) {
                if (origStepFilters[ocgIndex].equals("org.codehaus.groovy.*")) {
                    break;
                }
                ocgIndex++;
            }
            String[] removedStepFilters;
            if (ocgIndex < origStepFilters.length) {
                removedStepFilters = new String[origStepFilters.length-1];
                System.arraycopy(origStepFilters, 0, removedStepFilters, 0, ocgIndex);
                System.arraycopy(origStepFilters, ocgIndex+1, removedStepFilters, ocgIndex, removedStepFilters.length - ocgIndex);
            } else {
                removedStepFilters = origStepFilters;
            }
            
            String[] newStepFilters = new String[removedStepFilters.length + EXTRA_STEP_FILTERS.length];
            System.arraycopy(removedStepFilters, 0, newStepFilters, 0, removedStepFilters.length);
            System.arraycopy(EXTRA_STEP_FILTERS, 0, newStepFilters, removedStepFilters.length, EXTRA_STEP_FILTERS.length);
            return newStepFilters;
        }
    }
    

    /**
     * @return true iff this is a native frame with a sun. package prefix
     */
    public boolean shouldPerformExtraStep(Location location)
            throws DebugException {
        if (!GroovyDebugCoreActivator.isStepFilteringEnabled()) {
            return false;
        }
        if (location.lineNumber() <= 0) {
            // synthetic frame
//            System.out.println("Filtered: " + location.declaringType().name()  + "." + location.method() + " :: " + location);
            return true;
        }
        //  hmmmmm.....may not need this any more
        String name = location.declaringType().name();
        if (name.startsWith("sun.") || name.startsWith("groovy.lang.") || name.startsWith("org.codehaus.groovy")) {
//            System.out.println("Filtered: " + location.declaringType().name()  + "." + location.method() + "()");
            return true;
        }
        return false;
    }

    /*
     * Closely follow the logic of ConditionalBreakpointListener.breakpointHit()
     */
    public int conditionalBreakpointHit(IJavaThread thread,
            IJavaBreakpoint breakpoint, ConditionalBreakpointHandler handler) {
        try {
            GroovyConditionalBreakpointHandler groovyHandler = new GroovyConditionalBreakpointHandler();
            int result = groovyHandler.conditionalBreakpointHit(thread, breakpoint);
            updateErrors(handler, groovyHandler);
            
            // FIXADE I'm not exactly sure why this is necessary, but when this is here,
            // we avoid an occasional InvalidStackFrameException 
            // I think this has something to do with not sending too many requests to the debugged app 
            // at once.
            // It seems to be that the more complicated the expression being evaluated, the
            // higher the wait number must be.
            // If you're feeling adventurous, then I suggest you remove the synchonized block 
            // and try to make things work without the wait.
            synchronized (this) {
                try {
                    wait(50);
                } catch (InterruptedException e) {
                }
            }
            return result;
        } catch (Exception e) {
            GroovyDebugCoreActivator.log(e);
            return IJavaBreakpointListener.SUSPEND;
        }
    }
    

    private void updateErrors(ConditionalBreakpointHandler handler, GroovyConditionalBreakpointHandler groovyHandler) {
        ReflectionUtils.setPrivateField(ConditionalBreakpointHandler.class, "fHasErrors", handler, groovyHandler.hasErrors());
    }

    public boolean isAlwaysInteretingLaunch() {
        return GroovyDebugCoreActivator.isStepFilteringEnabledOnAll();
        
    }
}
