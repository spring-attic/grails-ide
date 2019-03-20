/*******************************************************************************
 * Copyright (c) 2012 - 2013 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.runonserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.common.project.facet.core.internal.FacetedProjectNature;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.eclipse.wst.server.core.util.ProjectModuleFactoryDelegate;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;

import org.grails.ide.eclipse.runonserver.RunOnServerProperties.RunOnServerPropertiesListener;

/**
 * @author Kris De Volder
 * @author Andrew Eisenberg
 * @author Andy Clement
 * @author Christian Dupuis
 * @since 2.5.1 
 */
public class GrailsAppModuleFactoryDelegate extends ProjectModuleFactoryDelegate implements RunOnServerPropertiesListener {
	
	/**
	 * This is here mostly to make testing easier... 
	 */
	public static GrailsAppModuleFactoryDelegate instance = null;
	
	public static final String TYPE = "grails.app";
	public static final String VERSION = "1.0";
	/* 
	 * Notes:
	 *  1) the module type and version here should correspond to what is defined in our plugin.xml
	 *  contribution to the moduleFactories extension point.
	 *  
	 *  2) for the module to be useful, some server needs to declare that it can accept this module
	 *  type and version.
	 *  
	 *  3) Deployment to a server is also Governed by facets applied to the project
	 *  from whence a module came. We apply the "grails.app" facet to any Grails
	 *  App project for which we create a module.
	 *  
	 *  => Modules created by this factory will only be accepted by server runtimes
	 *  that declare they accept the BOTH grails.app facet and grails.app module type.
	 */
	
	private static final boolean DEBUG = false;
	
	private static void debug(String msg) {
		if (DEBUG) {
			System.out.println("GrailsAppModuleFactoryDelegate:" + msg);
		}
	}
	
	public GrailsAppModuleFactoryDelegate() {
		instance = this;
		RunOnServerProperties.addEnvChangeListener(this);
	}
	
//	/**
//	 * @param c
//	 * @throws CoreException
//	 */
//	private static void debug(ILaunchConfiguration c) {
//		try {
//			@SuppressWarnings("unchecked")
//			Map<String, Object> attribs = c.getAttributes();
//			for (Entry<String, Object> e : attribs.entrySet()) {
//				if (e.getKey().contains("source_locator")) {
//					debug(e.getKey() + " = " + e.getValue());
//				}
//			}
//		} catch (CoreException e1) {
//			e1.printStackTrace();
//		}
//	}
	
	
	/**
	 * Keeps track of associations between grails projects and their delegates.
	 */
	private Map<IProject, GrailsAppModuleDelegate> delegates = new WeakHashMap<IProject, GrailsAppModuleDelegate>();
	
	/**
	 * Get or create the corresponding GrailsAppModuleDelegate for a (Grails app) module. 
	 */
	@Override
	public ModuleDelegate getModuleDelegate(IModule module) {
		ensureChangeListener();
		synchronized (delegates) {
			GrailsAppModuleDelegate delegate = delegates.get(module.getProject());
			if (delegate==null) {
				delegate = new GrailsAppModuleDelegate(module);
				delegates.put(module.getProject(), delegate);
			}
			return delegate;
		}
	}

	/**
	 * Returns the corresponding GrailsAppModuleDelegate for a project. May return null if either
	 *   - the project is not a GrailsApp project
	 *   - the module delegate has not yet been created.
	 */
	public GrailsAppModuleDelegate getDelegate(IProject project) {
		synchronized (delegates) {
			return delegates.get(project);
		}
	}
	
	@Override
	protected IModule createModule(IProject project) {
		IModule created;
		if (GrailsNature.isGrailsAppProject(project)) {
			try {
				ensureFacetsAndNatures(project);
			} catch (CoreException e) {
				GrailsCoreActivator.log("Couldn't make '"+project+"' Faceted, deploying it may not work", e);
			}
			created = createModule(project.getName(), project.getName(), TYPE, VERSION, project);
		} else {
			created = null;
		}
		return created;
	}

	/**
	 * If the project associated with a deployable module isn't faceted, and has certain
	 * natures there's going to be some trouble. Here we check for and add the necessary natures
	 * and facets.
	 * 
	 * @param project
	 * @throws CoreException 
	 */
	private void ensureFacetsAndNatures(IProject project) throws CoreException {
		if (!project.hasNature(FacetedProjectNature.NATURE_ID)) {
			//Turn into a faceted project
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();
			String[] newNatures = RunOnServerPlugin.copyOf(natures, natures.length+1);
			newNatures[natures.length] = FacetedProjectNature.NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		} 
		
		//Ensure we have the GrailApp facet (This facet marks the project as "TcServer Deployable")
		GrailsAppFacet.addFacetIfNeeded(project);
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// Responding to changes in the workspace
	//
	
	private IResourceChangeListener changeListener;
	synchronized void ensureChangeListener() {
		if (changeListener==null) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			changeListener = new IResourceChangeListener() {
				public void resourceChanged(IResourceChangeEvent event) {
					if ((event.getType() & IResourceChangeEvent.POST_CHANGE) != 0) {
						try {
							resourcesChanged(event.getDelta());
						} catch (CoreException e) {
							GrailsCoreActivator.log("Error processing resource change event, event ignored:", e);
						}
					}
				}
			};
			workspace.addResourceChangeListener(changeListener);
		}
	}
	
	@Override
	protected IPath[] getListenerPaths() {
		return new IPath[] {
				new Path(".project") // When natures change should clear module cache in superclass.
		};
	}
	

	/**
	 * Called whenever some resources are changed in the workspace. Needs to delegate to resourcesChanged method of
	 * any corresponding GrailsAppModuleDelegates.
	 * @throws CoreException 
	 */
	private void resourcesChanged(IResourceDelta delta) throws CoreException {
		delta.accept(new IResourceDeltaVisitor() {
			
			public boolean visit(IResourceDelta delta) throws CoreException {
//				debug("visitDelta: " + delta.getResource());
				switch (delta.getResource().getType()) {
				case IResource.ROOT:
//					debug("visitDelta => TRUE ");
					return true;
				case IResource.PROJECT: 
//					debug("visitDelta => delegate to GrailsApp?");
					IProject project = (IProject) delta.getResource();
					if (GrailsNature.isGrailsAppProject(project)) {
//						debug("visitDelta => delegate to GrailsApp? YES");
						GrailsAppModuleDelegate grailsApp = getDelegate(project);
						if (grailsApp!=null) {
							//If the delegate hasn't been created yet, we don't need to watch for changes.
							//TODO: But... possible race condition? What if Delegate is in the process of being created? 
							grailsApp.projectChanged(delta);
						}
						return false; // Don't go deeper, we have delegate the handling to the grailsApp
					} else {
						// Project may have been removed or is not a grails app, or no longer a grails app.
						// Whatever the case may be, it is certain we should not keep any module delegates for this project anymore
						// and if any modules existed for it, they are no longer valid.
						removeDelegate(project);
					}
					return false;
				default:
//					debug("visitDelta => FALSE");
					return false;
				}
			}
		});
	}
	
	private void removeModulesFromServer(IProject project) {
		IProgressMonitor mon = new NullProgressMonitor();
		IServer[] allServers = ServerCore.getServers();
		for (IServer server : allServers) {
			IServerWorkingCopy wc = server.createWorkingCopy();
			IModule[] ms = wc.getModules();
			if (ms!=null && ms.length>0) {
				List<IModule> toRemove = new ArrayList<IModule>();
				for (IModule m : ms) {
					if (project.equals(m.getProject())) {
						toRemove.add(m);
					}
				}
				if (toRemove.size()>0) {
					try {
						wc.modifyModules(new IModule[0], toRemove.toArray(new IModule[toRemove.size()]), mon);
						saveLater("Removing '"+project.getName()+"' from server", wc);
					} catch (CoreException e) {
						GrailsCoreActivator.log("Problem removing deleted project's modules from server", e);
					}
				}
			}
		}
	}

	private void saveLater(String description, final IServerWorkingCopy wc) {
		WorkspaceJob job = new WorkspaceJob(description) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				try {
					wc.save(true, monitor);
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return e.getStatus();
				}
			}
		};
		job.setPriority(Job.INTERACTIVE);
		job.schedule();
	}

	/**
	 * This is called when a module becomes invalid (most likely this is because the project was deleted).
	 */
	private void removeDelegate(IProject project) {
		synchronized (delegates) {
			GrailsAppModuleDelegate delegate = delegates.get(project);
			if (delegate!=null) {
				// STS-3162: Commented out because it causes apps in Cloud Foundry to be automatically removed.
				// Ensure that any module associated with deleted project are removed from server.
                //	removeModulesFromServer(project);
				
				// Remove any delegates from our map
				delegates.remove(project);
				
				//Clear module caches in super class or references to the delegate remain in there!
				super.clearCache(project);
				
				// Following is probably not necessary anymore (if we got all references to delegate)
				// but is safer to do this anyway.
				delegate.clearCaches();
			}
		}
	}

	/**
	 * Called when the 'env' property in {@link RunOnServerProperties} changes for any project.
	 */
	public synchronized void envChanged(IProject project, String oldEnv, String newEnv) {
		GrailsAppModuleDelegate delegate = getDelegate(project);
		if (delegate!=null) {
			delegate.envChanged(oldEnv, newEnv);
		}
	}

	public void incrementalChanged(IProject project, boolean old, boolean isIncremental) {
		GrailsAppModuleDelegate delegate = getDelegate(project);
		if (delegate!=null) {
			delegate.incrementalChanged(old, isIncremental);
		}
	}
}
