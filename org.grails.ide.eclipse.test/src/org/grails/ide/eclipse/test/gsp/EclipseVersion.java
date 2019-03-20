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
package org.grails.ide.eclipse.test.gsp;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

enum EclipseVersion { UNKNOWN(-1), E34(34), E35(35), E36(36), E37(37), E38(38); 
	private final int versionNumber;
	
	private EclipseVersion(int versionNumber) {
		this.versionNumber = versionNumber;
    }

	
	public int getVersionNumber() {
        return versionNumber;
    }
	
	
	
    public static EclipseVersion getVersion() {
        Bundle b = Platform.getBundle("org.eclipse.core.runtime");
        Version v = b.getVersion();
        switch (v.getMinor()) {
            case 4:
                return EclipseVersion.E34;
            case 5:
                return EclipseVersion.E35;
            case 6:
                return EclipseVersion.E36;
            case 7:
                return EclipseVersion.E37;
            case 8:
                return EclipseVersion.E38;
            default:
                return EclipseVersion.UNKNOWN;
        }
    }
}