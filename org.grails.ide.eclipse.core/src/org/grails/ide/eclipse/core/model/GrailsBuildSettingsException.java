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
package org.grails.ide.eclipse.core.model;

/**
 * @author Christian Dupuis
 * @since 2.2.0
 */
@SuppressWarnings("serial")
public class GrailsBuildSettingsException extends RuntimeException {
	
    public GrailsBuildSettingsException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

}
