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
package org.grails.ide.eclipse.core;

import org.eclipse.core.runtime.IStatus;

/**
 * Custom logger interface for {@link GrailsCoreActivator}.
 * Useful for testing.
 * @author Andrew Eisenberg
 * @created Jan 19, 2010
 */
public interface ILogger {
    
    public void logEntry(IStatus status);
}
