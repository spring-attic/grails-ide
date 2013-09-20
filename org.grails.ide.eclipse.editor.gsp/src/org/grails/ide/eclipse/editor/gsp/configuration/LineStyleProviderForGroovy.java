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
package org.grails.ide.eclipse.editor.gsp.configuration;

import java.util.List;

import org.codehaus.groovy.eclipse.GroovyPlugin;
import org.codehaus.groovy.eclipse.editor.GroovyTagScanner;
import org.eclipse.jdt.groovy.core.util.ReflectionUtils;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jst.jsp.ui.internal.style.java.LineStyleProviderForJava;

/**
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @created Nov 6, 2009
 */
public class LineStyleProviderForGroovy extends LineStyleProviderForJava {
        
    class LineStyleGroovyTagScanner extends GroovyTagScanner {
        // make accessible
        public LineStyleGroovyTagScanner() {
            super(GroovyPlugin.getDefault().getTextTools().getColorManager(), null, null, null);
        }

        @Override
        protected List<IRule> createRules() {
            return super.createRules();
        }
    }
    
    public LineStyleProviderForGroovy() {
        RuleBasedScanner fScanner = (RuleBasedScanner) ReflectionUtils.getPrivateField(LineStyleProviderForJava.class, "fScanner", this);
        fScanner.setRules(new LineStyleGroovyTagScanner().createRules().toArray(new IRule[0]));
    }
    
}
