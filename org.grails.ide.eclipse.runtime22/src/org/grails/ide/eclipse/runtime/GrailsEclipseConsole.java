/*
 * Copyright 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.ide.eclipse.runtime;

import java.io.IOException;
import java.io.OutputStream;
import org.fusesource.jansi.AnsiOutputStream;

/**
 * This class patches up the GrailsConsole to avoid it doing bad stuff that
 * just doesn't work and creates trouble when process is hooked up to
 * an Eclipse console instead of some OS-specific terminal implementation. 
 *
 * @author Kris De Volder
 *
 * @since 3.3.M1
 */
public class GrailsEclipseConsole extends grails.build.logging.GrailsEclipseConsole {

    private static final boolean DEBUG = false;
    		//boolProp("grails.console.eclipse.debug");
    private static final String ECLIPSE_SUPPORTS_ANSI_PROP = "grails.console.eclipse.ansi";

    private Boolean eclipseSupportsAnsi = null; //lazy initialized because implicitly used from super constructor.

    public GrailsEclipseConsole() throws IOException {
        super();
    }

    @Override
    protected OutputStream ansiWrap(OutputStream out) {
        if (DEBUG) {
            try {
                out.write(("<<<"+ECLIPSE_SUPPORTS_ANSI_PROP+":"+eclipseSupportsAnsi()+">>>\n").getBytes());
            } catch (IOException e) {
            }
        }
        //This method is called from the super constructor so eclipseSupportsAnsi field
        //must be lazy initialised (it can't be initialised before we get called!)
        if (eclipseSupportsAnsi()) {
            return out; // expose unfiltered output to eclipse
        }
        //Important: Don't call the super.answWrap because it goes to some OS-specific code paths that
        // do bad things, such as, for example hanging the process in some cases.
        //Just keep it simple and directly wrap the out here to remove Ansi codes. No OS-specific processing.
        //Any such processing would really not do any good for Eclipse consoles which are not OS terminals anyway.
        return new AnsiOutputStream(out); //Remove ansi codes.
    }

    private boolean eclipseSupportsAnsi() {
        if (eclipseSupportsAnsi == null) {
            eclipseSupportsAnsi = boolProp(ECLIPSE_SUPPORTS_ANSI_PROP);
        }
        return eclipseSupportsAnsi;
    }

    private static Boolean boolProp(String propName) {
        try {
            String prop =  System.getProperty(propName);
            return prop != null && Boolean.valueOf(prop);
        } catch (Exception e) {
            return false;
        }
    }

}
