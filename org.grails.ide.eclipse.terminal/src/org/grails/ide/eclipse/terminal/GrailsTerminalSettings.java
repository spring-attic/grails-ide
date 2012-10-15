package org.grails.ide.eclipse.terminal;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.grails.ide.eclipse.commands.GrailsCommand;
import org.grails.ide.eclipse.commands.GrailsCommandFactory;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.model.IGrailsInstall;

public class GrailsTerminalSettings {

	private String projectName = "";
	private String command = "";
	
	public String getProjectName() {
		return projectName;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	@Override
	public String toString() {
		return "{ projectName: "+projectName+", cmd: "+command+" }";
	}

	public IGrailsInstall getGrailsInstall() {
		IProject project = getProject();
		if (project!=null && GrailsNature.isGrailsProject(project)) {
			return GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(project);
		}
		return null;
	}

	public IProject getProject() {
		if (projectName!=null && !"".equals(projectName)) {
			IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			if (p.isAccessible()) {
				return p;
			}
		}
		return null;
	}

	public File getWorkingDir() {
		IProject p = getProject();
		if (p!=null) {
			return p.getLocation().toFile();
		}
		return ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
	}

	public GrailsCommand getGrailsCommand() {
		GrailsCommand cmd = GrailsCommandFactory.fromString(getProject(), getCommand());
		cmd.setSystemProperty("grails.console.eclipse.ansi", ""+true);
		return cmd;
	}

	public void copyFrom(GrailsCommand cmd) {
		setCommand(cmd.getCommand());
		IProject p = cmd.getProject();
		String name = p==null?"":p.getName();
		setProjectName(name);
	}
	
}
