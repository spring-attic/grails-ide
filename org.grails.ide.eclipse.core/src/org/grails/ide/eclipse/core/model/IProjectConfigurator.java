package org.grails.ide.eclipse.core.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * Methods for configuring grails project to work correctly inside of Eclipse. These might be
 * dependent on the Grails version, so to obtain configurator... ask the IGrailsInstall.
 * 
 * @author Kris De Volder
 * @since 3.6.4
 */
public interface IProjectConfigurator {

	IProject configureNewlyCreatedProject(IGrailsInstall grailsInstall, IProject project) throws CoreException;

}
