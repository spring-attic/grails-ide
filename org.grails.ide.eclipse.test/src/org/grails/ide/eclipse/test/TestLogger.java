/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.test;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.grails.ide.eclipse.core.ILogger;


/**
 * @author Andrew Eisenberg
 * @created Jan 19, 2010
 */
public class TestLogger implements ILogger {
    
    List<IStatus> entries = new ArrayList<IStatus>();

    public void logEntry(IStatus status) {
        entries.add(status);
    }
    
    public List<IStatus> getErrorEntries() {
        List<IStatus> errors = new ArrayList<IStatus>();
        for (IStatus status : entries) {
            if (status.getSeverity() >= Status.ERROR) {
                errors.add(status);
            }
        }
        return errors;
    }
    
    public boolean hasErrors() {
        return getErrorEntries().size() > 0;
    }
    
    public String getAllEntriesAsText() {
        return getSomeEntriesAsText(entries);
    }

    /**
     * @return
     */
    public String getSomeEntriesAsText(List<IStatus> someEntries) {
        StringBuilder sb = new StringBuilder();
        for (IStatus status : someEntries) {
            sb.append(status.toString() + "\n");
        }
        return sb.toString();
    }
    
    public List<IStatus> getAllEntriesWithPrefix(String prefix) {
        List<IStatus> prefixedEntries = new ArrayList<IStatus>();
        for (IStatus status : entries) {
            if (status.getMessage().startsWith(prefix)) {
                prefixedEntries.add(status);
            }
        }
        return prefixedEntries;

    }
    
    public String getErrorEntriesAsText() {
        return getSomeEntriesAsText(getErrorEntries());
    }
}
