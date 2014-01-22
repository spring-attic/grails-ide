/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.internal.plugins;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;


/**
 * Resource change listening for Grails projects and resources.
 * @author Nieraj Singh
 * @author Andrew Eisenberg
 */
public class GrailsCore implements IResourceChangeListener {

    private class PerProjectCacheManager {
        private final Map<IProject, Map<Class<? extends IGrailsProjectInfo>, IGrailsProjectInfo>> projectInfoMap = 
            new HashMap<IProject, Map<Class<? extends IGrailsProjectInfo>,IGrailsProjectInfo>>();

        synchronized <T extends IGrailsProjectInfo> T get(
                IProject project, Class<T> infoClass, boolean create) throws InstantiationException, IllegalAccessException {
            Map<Class<? extends IGrailsProjectInfo>, IGrailsProjectInfo> infoList = projectInfoMap.get(project);
            if (infoList == null) {
                infoList = new HashMap<Class<? extends IGrailsProjectInfo>, IGrailsProjectInfo>();
                projectInfoMap.put(project, infoList);
            }
            T info = (T) infoList.get(infoClass);
            if (info == null && create) {
                info = infoClass.newInstance();
                info.setProject(project);
                infoList.put(infoClass, info);
            }
            return info;

        }
        
        synchronized Map<Class<? extends IGrailsProjectInfo>, IGrailsProjectInfo> removeProject(IProject project) {
            return projectInfoMap.remove(project);
        }
        
        synchronized Map<Class<? extends IGrailsProjectInfo>, IGrailsProjectInfo> getAll(IProject project) {
            return projectInfoMap.get(project);
        }
        
        synchronized boolean isEmpty() {
            return projectInfoMap.isEmpty();
        }
    }

	// The singleton instance
	private final static GrailsCore INSTANCE = new GrailsCore();

	private PerProjectCacheManager cacheManager = new PerProjectCacheManager();

	private GrailsCore() {
		// Singleton
	}
	
	private final Map<String, Object> projectLocks = new HashMap<String, Object>();
	
	// Access to the projectLocks map must be synchronized
	private final Object projectLocksLock = new Object();
	
	/**
	 * Retrieves a lock for synchronizing on Cache operations for this particular project.
	 * Note that this map only grows and old projects are not removed.  I think that this is
	 * fine since we are not expecting an overwhelming number of projects
	 * @param project the project to lock on
	 * @return a lock to synchronize on that is specific for the current project
	 */
	public Object getLockForProject(IProject project) {
	    if (project == null) {
	        return this;
	    }
	    
	    synchronized (projectLocksLock) {
    	    Object lock = projectLocks.get(project.getName());
    	    if (lock == null) {
    	        lock = new Object();
    	        projectLocks.put(project.getName(), lock);
    	    }
    	    return lock;
	    }
	}
	
	private IProject[] getAllProjects() {
	    synchronized (projectLocksLock) {
	        IProject[] allProjects = new IProject[projectLocks.size()];
	        int i = 0;
	        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
	        for (String name : projectLocks.keySet()) {
                allProjects[i++] = root.getProject(name);
            }
	        return allProjects;
	    }
	}

	public static GrailsCore get() {
		return INSTANCE;
	}

	/**
	 * Not API. Only for internal use within the plugin
	 */
	public void initialize() {
	    cacheManager = new PerProjectCacheManager();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	/**
	 * Not API. Only for internal use within the plugin
	 */
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		IProject[] allProjects = getAllProjects();
		for (IProject project : allProjects) {
			disconnectProject(project);
		}
		cacheManager = null;
	}

	/**
	 * Notifies all connected {@link IGrailsProjectInfo}s that a change to one
	 * of the grails elements has occurred. Currently, not all grails changes
	 * are interesting. And so, some changes are ignored.
	 * <p>
	 * Here is the list of grails changes that we keep track of:
	 * <ul>
	 * <li>Changes to custom taglibs</li>
	 * <li>Changes to plugins and the grails classpath</li>
	 * <li>Project close and deletions</li>
	 * <li>Removal of grails nature</li>
	 * </ul>
	 * This list may grow over time.
	 * <p>
	 * This method grabs a lock on the project.
	 * 
	 * @param project
	 *            The affected project
	 * @param changeKinds
	 *            the list of {@link GrailsElementKind}s affected by this change
	 * @param change
	 *            the delta
	 */
	private void notifyGrailsProjectInfos(IProject project,
			GrailsElementKind[] changeKinds, IResourceDelta change) {
		synchronized (getLockForProject(project)) {
            Map<Class<? extends IGrailsProjectInfo>, IGrailsProjectInfo> infos = cacheManager.getAll(project);
            if (infos != null) {
                for (IGrailsProjectInfo info : infos.values()) {
                    info.projectChanged(changeKinds, change);
                }
            }
        }
	}

	/**
	 * Connect an info of a particular class to the specified project. If the
	 * class is already connected, The object of that class is returned instead.
	 * 
     * This method grabs a lock on the project.
	 * 
	 * @param infoClass
	 * @param project
	 * @param infoClass
	 * @return the associated {@link IGrailsProjectInfo} of the specified class
	 *         connected to the passed in project, or null if there was an
	 *         exception or if not a Grails project
	 */
	public <T extends IGrailsProjectInfo> T connect(
			IProject project, Class<T> infoClass) {
		try {
			if (GrailsNature.isGrailsProject(project)) {
				synchronized (getLockForProject(project)) {
				    return cacheManager.get(project, infoClass, true);
                }
			}
		} catch (Exception e) {
			GrailsCoreActivator
					.log("Problem creating IGrailsProjectInfo of type " + infoClass.getCanonicalName() + //$NON-NLS-1$
							" for project " + project.getName(), e); //$NON-NLS-1$
		}
		return null;
	}

	/**
	 * Gets an info of a particular class that is already connected to the
	 * specified project.
	 * 
     * This method grabs a lock on the project.
	 * 
	 * @param project
	 * @param infoClass
	 * @return the associated {@link IGrailsProjectInfo} of the specified class
	 *         that is already connected to the project, or null if none are
	 *         connected.
	 */
	public <T extends IGrailsProjectInfo> T getInfo(
			IProject project, Class<T> infoClass) {
		try {
			if (GrailsNature.isGrailsProject(project)) {
				synchronized (getLockForProject(project)) {
                    return cacheManager.get(project, infoClass, false);
                }
			}
		} catch (Exception e) {
			GrailsCoreActivator
					.log("Problem getting IGrailsProjectInfo of type " + infoClass.getCanonicalName() + //$NON-NLS-1$
							" for project " + project.getName() + ".  Was the project deleted or closed?", e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	/**
	 * Disconnects all {@link IGrailsProjectInfo} for the given {@link IProject}.
	 * Grabs a lock on the given project.
	 * @param project
	 */
	public void disconnectProject(IProject project) {
		synchronized (getLockForProject(project)) {
		    Map<Class<? extends IGrailsProjectInfo>, IGrailsProjectInfo> infos = cacheManager.removeProject(project);
            if (infos != null) {
                for (IGrailsProjectInfo info : infos.values()) {
                    info.dispose();
                }
            }
        }
	}

	public void resourceChanged(IResourceChangeEvent event) {
		if (cacheManager.isEmpty()) {
			return;
		}
		
		switch (event.getType()) {
		case IResourceChangeEvent.PRE_CLOSE:
		case IResourceChangeEvent.PRE_DELETE:
			IResource resource = event.getResource();
			// don't need to check nature here
			if (resource != null && resource.getType() == IResource.PROJECT) {
				disconnectProject((IProject) resource);
			}
			break;

		case IResourceChangeEvent.POST_CHANGE:
			lookForChanges(event.getDelta());
			break;
		default:
			break;
		}
	}

	private void lookForChanges(IResourceDelta delta) {
		LookForChangeVisitor visitor = new LookForChangeVisitor();
		try {
			delta.accept(visitor);
		} catch (CoreException e) {
			GrailsCoreActivator.log(e);
		}

		Map<IProject, Set<GrailsElementKind>> projectKindMap = visitor.projectKindMap;
		for (Entry<IProject, Set<GrailsElementKind>> entry : projectKindMap
				.entrySet()) {
			notifyGrailsProjectInfos(entry.getKey(),
					entry.getValue().toArray(new GrailsElementKind[0]), delta);
		}
	}

	/**
	 * Walks the resource delta and calculates all the kinds of changes for each
	 * project
	 * 
	 * @author Andrew Eisenberg
	 * @created Jan 14, 2010
	 */
	@SuppressWarnings("nls")
	private class LookForChangeVisitor implements IResourceDeltaVisitor {
		private Map<IProject, Set<GrailsElementKind>> projectKindMap = new HashMap<IProject, Set<GrailsElementKind>>();

		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();

			switch (resource.getType()) {
			case IResource.PROJECT:
				// short circuit non-grails projects
				IProject project = (IProject) resource;
				return project.isAccessible()
						&& project.hasNature(GrailsNature.NATURE_ID);

			case IResource.FILE:
				if (resource.getParent().getType() == IResource.PROJECT) {
					if (resource.getName().equals(".classpath")) {
						addChange(resource, GrailsElementKind.CLASSPATH);
					} else if (resource.getName().equals(".project")) {
						addChange(resource, GrailsElementKind.PROJECT);
					}
				} else {
					// check for various kinds of groovy files
					String name = resource.getName();
					if (isGroovyLikeFile(resource)) {
    					if (isTagLibName(name) && isInTagLibFolder(resource)) {
    						addChange(resource, GrailsElementKind.TAGLIB_CLASS);
    					} else if (isServiceName(name) && isInServiceFolder(resource)) {
    					    addChange(resource, GrailsElementKind.SERVICE_CLASS);
    					} else if (isControllerName(name) && isInControllerFolder(resource)) {
    					    addChange(resource, GrailsElementKind.CONTROLLER_CLASS);
    					} else if (isInDomainFolder(resource)) {
    					    addChange(resource, GrailsElementKind.DOMAIN_CLASS);
    					}
					}
				}
			case IResource.ROOT:
			case IResource.FOLDER:
				return true;
			}
			return false;
		}
		
		protected boolean isGroovyLikeFile(IResource resource) {
			String fileExtension = resource.getFileExtension();
			if (fileExtension != null
					&& (fileExtension.equals("groovy") || fileExtension
							.equals("java"))) {
				return true;
			}
			return false;
		}

		/**
		 * returns true if this resource is in a tag lib folder
		 * 
		 * @param resource
		 * @return
		 */
		private boolean isInTagLibFolder(IResource resource) {
			// remove project path
			IPath fullPath = resource.getFullPath().removeFirstSegments(1);
			return fullPath.segmentCount() > 2
					&& fullPath.segment(0).equals("grails-app")
					&& fullPath.segment(1).equals("taglib");
		}

		/**
         * Does this resource's name (minus file extension) end in TagLib?
         * 
         * @param resource
         * @return
         */
        private boolean isTagLibName(String name) {
        	int lastDot = name.lastIndexOf('.');
        	if (lastDot > 0) {
        		int tagLibEnd = name.lastIndexOf("TagLib.") + "TagLib".length();
        		return tagLibEnd == lastDot;
        	}
        	return false;
        }

        /**
		 * returns true if this resource is in a services folder
		 * 
		 * @param resource
		 * @return
		 */
		private boolean isInServiceFolder(IResource resource) {
		    // remove project path
		    IPath fullPath = resource.getFullPath().removeFirstSegments(1);
		    return fullPath.segmentCount() > 2
		    && fullPath.segment(0).equals("grails-app")
		    && fullPath.segment(1).equals("services");
		}
		
		/**
         * Does this resource's name (minus file extension) end in Service?
         * 
         * @param resource
         * @return
         */
        private boolean isServiceName(String name) {
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0) {
                int tagLibEnd = name.lastIndexOf("Service.") + "Service".length();
                return tagLibEnd == lastDot;
            }
            return false;
        }

        /**
         * returns true if this resource is in a controllers folder
         * 
         * @param resource
         * @return
         */
        private boolean isInControllerFolder(IResource resource) {
            // remove project path
            IPath fullPath = resource.getFullPath().removeFirstSegments(1);
            return fullPath.segmentCount() > 2
            && fullPath.segment(0).equals("grails-app")
            && fullPath.segment(1).equals("controllers");
        }
        
        /**
         * Does this resource's name (minus file extension) end in Controller?
         * 
         * @param resource
         * @return
         */
        private boolean isControllerName(String name) {
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0) {
                int tagLibEnd = name.lastIndexOf("Controller.") + "Controller".length();
                return tagLibEnd == lastDot;
            }
            return false;
        }
		
        /**
         * returns true if this resource is in a domain class folder
         * 
         * @param resource
         * @return
         */
        private boolean isInDomainFolder(IResource resource) {
            // remove project path
            IPath fullPath = resource.getFullPath().removeFirstSegments(1);
            return fullPath.segmentCount() > 2
            && fullPath.segment(0).equals("grails-app")
            && fullPath.segment(1).equals("domain");
        }
        

        private void addChange(IResource resource, GrailsElementKind kind) {
			IProject affectedProject = resource.getProject();
			Set<GrailsElementKind> kindSet = projectKindMap
					.get(affectedProject);
			if (kindSet == null) {
				kindSet = new HashSet<GrailsElementKind>(3);
				projectKindMap.put(affectedProject, kindSet);
			}
			kindSet.add(kind);
		}
	}
}
