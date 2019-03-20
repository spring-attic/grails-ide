/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.editor.groovy.elements;

import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;


/**
 * This object is a singleton and manages the grails projects currently in the workspace
 * @author Andrew Eisenberg
 * @author Christian Dupuis
 * @author Nieraj Singh
 * @created Nov 23, 2009
 */
public final class GrailsWorkspaceCore  {
    // The singleton instance
    private final static GrailsWorkspaceCore INSTANCE = new GrailsWorkspaceCore();
    
    
    private GrailsWorkspaceCore() {
    }
    
    public static GrailsWorkspaceCore get() {
        return INSTANCE;
    }

    /**
     * Creates the {@link GrailsProject} associated with a particular
     * {@link IJavaProject}.  Will return null if the project 
     * is not a Grails project.
     * @param elt
     * @return
     */
    public GrailsProject create(IJavaProject project) {
        try {
            if (project != null && project.getProject().hasNature(GrailsNature.NATURE_ID)) {
                return new GrailsProject(project);
            }
        } catch (CoreException e) {
        }
        // not in a java project, or java project is closed
        return null;
    }
    
    /**
     * Creates the {@link GrailsProject} associated with a particular
     * {@link IJavaElement}.  Will return null if the element 
     * is not in a Grails project.
     * @param elt
     * @return
     */
    public GrailsProject getGrailsProjectFor(IJavaElement elt) {
        return create(elt.getJavaProject());
    }
    
    public static boolean hasRelatedDomainClass(GroovyCompilationUnit unit) {
        return hasRelatedInternal(unit, GrailsElementKind.DOMAIN_CLASS);
    }
    
    public static boolean hasRelatedControllerClass(GroovyCompilationUnit unit) {
        return hasRelatedInternal(unit, GrailsElementKind.CONTROLLER_CLASS);
    }
    
    public static boolean hasRelatedServiceClass(GroovyCompilationUnit unit) {
        return hasRelatedInternal(unit, GrailsElementKind.SERVICE_CLASS);
    }
    
    public static boolean hasRelatedTagLibClass(GroovyCompilationUnit unit) {
        return hasRelatedInternal(unit, GrailsElementKind.TAGLIB_CLASS);
    }
    
    public static boolean hasRelatedTestClass(GroovyCompilationUnit unit) {
        return hasRelatedInternal(unit, GrailsElementKind.UNIT_TEST);
    }
    
    public static boolean hasRelatedGSP(GroovyCompilationUnit unit) {
        return hasRelatedInternal(unit, GrailsElementKind.GSP);
    }
    
    private static boolean hasRelatedInternal(GroovyCompilationUnit unit, GrailsElementKind kind) {
        try {
            if (! GrailsNature.isGrailsProject(unit.getJavaProject().getProject())) {
                return false;
            }
        } catch (Exception e) {
            GrailsCoreActivator.log(e);
            return false;
        }
        switch (kind) {
            case DOMAIN_CLASS:
                return GrailsWorkspaceCore.get().getGrailsProjectFor(unit).getElementKind(unit).hasRelatedDomainClass();
            case CONTROLLER_CLASS:
                return GrailsWorkspaceCore.get().getGrailsProjectFor(unit).getElementKind(unit).hasRelatedControllerClass();
            case SERVICE_CLASS:
                return GrailsWorkspaceCore.get().getGrailsProjectFor(unit).getElementKind(unit).hasRelatedServiceClass();
            case TAGLIB_CLASS:
                return GrailsWorkspaceCore.get().getGrailsProjectFor(unit).getElementKind(unit).hasRelatedTagLibClass();
            case UNIT_TEST:
                return GrailsWorkspaceCore.get().getGrailsProjectFor(unit).getElementKind(unit).hasRelatedTestClass();
            case INTEGRATION_TEST:
                return GrailsWorkspaceCore.get().getGrailsProjectFor(unit).getElementKind(unit).hasRelatedTestClass();
            case GSP:
                return GrailsWorkspaceCore.get().getGrailsProjectFor(unit).getElementKind(unit).hasRelatedGSP();
            default:
                return false;
        }
    }
    
    
    public static boolean isDomainClass(GroovyCompilationUnit unit) {
        try {
            if (! unit.getJavaProject().getProject().hasNature(GrailsNature.NATURE_ID)) {
                return false;
            }
        } catch (Exception e) {
            GrailsCoreActivator.log(e);
            return false;
        }
        return GrailsWorkspaceCore.get().getGrailsProjectFor(unit).getDomainClass(unit) != null;
    }
    
    public static boolean isControllerClass(GroovyCompilationUnit unit) {
        try {
            if (! unit.getJavaProject().getProject().hasNature(GrailsNature.NATURE_ID)) {
                return false;
            }
        } catch (Exception e) {
            GrailsCoreActivator.log(e);
            return false;
        }
        return GrailsWorkspaceCore.get().getGrailsProjectFor(unit).getControllerClass(unit) != null;
    }
    
    public static boolean isTagLibClass(GroovyCompilationUnit unit) {
        try {
            if (! unit.getJavaProject().getProject().hasNature(GrailsNature.NATURE_ID)) {
                return false;
            }
        } catch (Exception e) {
            GrailsCoreActivator.log(e);
            return false;
        }
        return GrailsWorkspaceCore.get().getGrailsProjectFor(unit).getTagLibClass(unit) != null;
    }
    
    public GrailsProject create(IProject project) {
        IJavaProject jProj = JavaCore.create(project);
        return jProj != null && jProj.exists() ? create(jProj) : null;
    }

	public IGrailsElement create(IType type) {
		GrailsProject project = getGrailsProjectFor(type);
		if (project!=null) {
			return project.getGrailsElement(type);
		}
		return null;
	}

	public static boolean isServiceClass(IType target) {
		return isKind(target, GrailsElementKind.SERVICE_CLASS);
	}

	public static boolean isKind(IType target, final GrailsElementKind kind) {
		IGrailsElement element = get().create(target);
		if (element!=null) {
			return element.getKind()==kind;
		}
		return false;
	}

	public static boolean isDomainClass(IType target) {
		return isKind(target,GrailsElementKind.DOMAIN_CLASS);
	}
}
