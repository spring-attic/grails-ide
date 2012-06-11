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
package org.grails.ide.eclipse.core.internal;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.springsource.ide.eclipse.commons.core.SpringCoreUtils;


/**
 * @author Christian Dupuis
 * @author Nieraj Singh
 * @author Kris De Volder
 * @since 2.2.0
 */
public class GrailsPropertyTester extends PropertyTester {

	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (receiver instanceof IResource && "isClasspathEnabled".equals(property)) {
			return GrailsResourceUtil.hasClasspathContainer((IResource) receiver);
		}
		else if ((receiver instanceof IResource || receiver instanceof IAdaptable) && "canlaunch".equals(property)) {
			IProject project = null;
			if (receiver instanceof IResource) {
				project = ((IResource) receiver).getProject();
			}
			else {
				IResource res = (IResource) ((IAdaptable) receiver).getAdapter(IResource.class);
				if (res != null) {
					project = res.getProject();
				}
			}
			return project != null && SpringCoreUtils.hasNature(project, GrailsNature.NATURE_ID)
					&& GrailsCoreActivator.getDefault().getInstallManager().getGrailsInstall(project) != null;
		}
		else if ((receiver instanceof IResource || receiver instanceof IAdaptable) && isTestableResource(receiver)) {
			return true;
		}
//FIXKDV: We want to be able to select IJavaElements for test running as well
//		else if ((receiver instanceof IJavaElement) && "cantest".equals(property)) {
//			return isTestableJavaElement((IJavaElement)receiver);
//		}

		return false;
	}

	private boolean isTestableJavaElement(IJavaElement receiver) {
		System.out.println("isTestableJavaElement? "+receiver);
		if (receiver.getElementType() == IJavaElement.TYPE) {
			System.out.println("isTestableJavaElement? >> It is a TYPE");
			IType type = (IType) receiver;
			IPackageFragmentRoot root = (IPackageFragmentRoot) type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			return GrailsResourceUtil.isSourceFolder(root) && isToplevelType(type);
		}
		System.out.println("isTestableJavaElement? >> NO: It is NOT a TYPE");
		return false;
	}

	private boolean isToplevelType(IType type) {
		return type.getDeclaringType() == null;
	}

	private boolean isTestableResource(Object receiver) {
		IProject project = null;
		IResource resource = null;
		if (receiver instanceof IResource) {
			resource = (IResource) receiver;
		}
		else if (receiver instanceof IAdaptable) {
			IResource res = (IResource) ((IAdaptable) receiver).getAdapter(IResource.class);
			if (res != null) {
				resource = res;
			}
		}
		if (resource!=null) {
			project = resource.getProject();
			return GrailsNature.isGrailsProject(project)
				&& (  resource instanceof IProject
				   || GrailsResourceUtil.isTestFolder(resource)
				   || GrailsResourceUtil.isSourceFile(resource) );
			
		}
		return false;
	}

}
