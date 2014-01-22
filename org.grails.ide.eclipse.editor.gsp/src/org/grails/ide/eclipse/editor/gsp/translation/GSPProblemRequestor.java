// COPIED_FROM org.eclipse.jst.jsp.core.internal.java.JSPProblemRequestor
/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Pivotal Software, Inc.    - augmented for use with Grails
 *******************************************************************************/
package org.grails.ide.eclipse.editor.gsp.translation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.compiler.IProblem;


/**
 * 
 * Make accessible to classes in this package
 * Make {@link GSPTranslation} create .groovy compilation units instead of .java.
 * @author Andrew Eisenberg
 * @created Nov 8, 2009
 */
class GSPProblemRequestor implements IProblemRequestor {

    private boolean fIsActive = false;

    private boolean fIsRunning = false;

    private List fCollectedProblems;

    public void beginReporting() {

        fIsRunning = true;
        fCollectedProblems = new ArrayList();
    }

    public void acceptProblem(IProblem problem) {

        if (isActive())
            fCollectedProblems.add(problem);
    }

    public void endReporting() {

        fIsRunning = false;
    }

    public boolean isActive() {

        return fIsActive && fCollectedProblems != null;
    }

    /**
     * Sets the active state of this problem requestor.
     * 
     * @param isActive
     *            the state of this problem requestor
     */
    public void setIsActive(boolean isActive) {

        if (fIsActive != isActive) {
            fIsActive = isActive;
            if (fIsActive)
                startCollectingProblems();
            else
                stopCollectingProblems();
        }
    }

    /**
     * Tells this annotation model to collect temporary problems from now on.
     */
    private void startCollectingProblems() {

        fCollectedProblems = new ArrayList();
    }

    /**
     * Tells this annotation model to no longer collect temporary problems.
     */
    private void stopCollectingProblems() {

        // do nothing
    }

    /**
     * @return the list of collected problems
     */
    public List getCollectedProblems() {

        return fCollectedProblems;
    }

    public boolean isRunning() {

        return fIsRunning;
    }
}
