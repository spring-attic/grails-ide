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
package org.grails.ide.eclipse.core.internal.plugins;


public enum GrailsElementKind {
    
    BOOT_STRAP("conf", "BootStrap.groovy", true),
    BUILD_CONFIG("conf", "BuildConfig.groovy", true),
    CONFIG("conf", "Config.groovy", true),
    DATA_SOURCE("conf", "DataSource.groovy", true),
    URL_MAPPINGS("conf", "UrlMappings.groovy", true),
    DOMAIN_CLASS("domain", ".groovy", false),
    CONTROLLER_CLASS("controllers", "Controller.groovy", false),
    UNIT_TEST("unit", "Tests.groovy", false),
    INTEGRATION_TEST("integration", "Tests.groovy", false),
    SERVICE_CLASS("services", "Service.groovy", false),
    TAGLIB_CLASS("taglib", "TagLib.groovy", false),
    GSP("views", "", false),
    OTHER("", "", false), // in src/groovy or src/java or elsewhere in the grails project
    INVALID("", "", false),  // when the element is not managed by the current project
    CLASSPATH("", "", false), // the .classpath file (does not correspond to an ICompilationUnit)
    PROJECT("", "", false); // the .project file (does not correspond to an ICompilationUnit) This also includes changes to the set of installed plugins

    public static String CONF_FOLDER = "conf";

    private String sourceFolder;
    private String nameSuffix;
    private boolean isConfigElement;

    private GrailsElementKind(String sourceFolder, String nameSuffix, boolean isConfigElement) {
        this.sourceFolder = sourceFolder;
        this.nameSuffix = nameSuffix;
        this.isConfigElement = isConfigElement;
    }
    
    /**
     * The name of the last segment of the source folder that contains artifacts of this kind 
     * (or empty string if unknown or not in a source folder)
     * 
     * @return last segment of source folder or empty string  
     */
    public String getSourceFolder() {
        return sourceFolder;
    }
    
    /**
     * Returns the suffix for grails artifacts of this kind, or an empty string if there is none.
     * Eg- "Controller", "Service", "Taglib"...
     * @return the suffix or empty string
     */
    public String getNameSuffix() {
        return nameSuffix;
    }
    
    /**
     * @return true iff the element is something in the config folder
     */
    public boolean isConfigElement() {
        return isConfigElement;
    }
    
    public boolean hasRelatedDomainClass() {
        switch (this) {
            case CONTROLLER_CLASS:
            case SERVICE_CLASS:
            case TAGLIB_CLASS:
                return true;
        }
        return false;
    }
    
    public boolean hasRelatedControllerClass() {
        switch (this) {
            case DOMAIN_CLASS:
            case SERVICE_CLASS:
            case TAGLIB_CLASS:
            case GSP:
                return true;
        }
        return false;
    }
    
    public boolean hasRelatedGSP() {
        switch (this) {
            case DOMAIN_CLASS:
            case SERVICE_CLASS:
            case CONTROLLER_CLASS:
            case TAGLIB_CLASS:
                return true;
        }
        return false;
    }



    public boolean hasRelatedTagLibClass() {
        switch (this) {
            case DOMAIN_CLASS:
            case CONTROLLER_CLASS:
            case SERVICE_CLASS:
                return true;
        }
        return false;
    }

    public boolean hasRelatedServiceClass() {
        switch (this) {
            case DOMAIN_CLASS:
            case TAGLIB_CLASS:
            case CONTROLLER_CLASS:
                return true;
        }
        return false;
    }
    
}