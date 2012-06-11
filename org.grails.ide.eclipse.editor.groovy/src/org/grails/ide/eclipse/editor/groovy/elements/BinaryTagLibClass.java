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
package org.grails.ide.eclipse.editor.groovy.elements;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ClassFile;
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner;
import org.grails.ide.eclipse.core.GrailsCoreActivator;


/**
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @created Dec 11, 2009
 */
public class BinaryTagLibClass extends TagLibClass {

    public BinaryTagLibClass(ClassFile classFile) {
        super(new GroovyClassFileWorkingCopy(classFile, DefaultWorkingCopyOwner.PRIMARY));
    }
    
    @Override
    protected void initializeCachedFields() {
        try {
            cachedFields = ((GroovyClassFileWorkingCopy) unit).classFile.getType().getFields();
        } catch (JavaModelException e) {
            GrailsCoreActivator.log(e);
        }
    }
}
