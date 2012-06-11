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
package org.grails.ide.eclipse.editor.groovy.types;

import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.model.GrailsVersion;
import org.grails.ide.eclipse.core.model.IGrailsInstall;

import org.grails.ide.eclipse.editor.groovy.elements.DomainClass;

/**
 * 
 * @author Andrew Eisenberg
 * @since 2.8.0
 */
public class FinderValidatorFactory {
    public DynamicFinderValidator createValidator(DomainClass domainClass) {
        IGrailsInstall grailsInstall = GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(domainClass.getCompilationUnit().getJavaProject().getProject());
        boolean use200 = grailsInstall.getVersion().compareTo(GrailsVersion.V_1_3_7) > 0;
        return new DynamicFinderValidator(use200, domainClass);
    }
}
