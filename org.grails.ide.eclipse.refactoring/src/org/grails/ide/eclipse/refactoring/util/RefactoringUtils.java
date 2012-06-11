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
package org.grails.ide.eclipse.refactoring.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.FileStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsResourceUtil;


/**
 * Utility class containing methods to create Search scopes.
 * 
 * @author Kris De Volder
 * @since 2.7
 */
public class RefactoringUtils {

		/**
		 * Creates a default 'search scope' to search for references that need updating in a Grails project. The default scope includes all
		 * source folders in the Grails project that actually 'belong' to the project (i.e. all source folders, but excluding 
		 * the linked plugin source folders.
		 * 
		 * @throws JavaModelException 
		 */
		public static IJavaSearchScope getSearchScope(IJavaProject javaProject) throws JavaModelException {
			IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
			List<IPackageFragmentRoot> srcFolders = new ArrayList<IPackageFragmentRoot>();
			for (IPackageFragmentRoot root : roots) {
				if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
					if (!GrailsResourceUtil.isGrailsDependencyPackageFragmentRoot(root)) {
						srcFolders.add(root);
	//					System.out.println(root.getPath());
					}
				}
			}
			return SearchEngine.createJavaSearchScope(srcFolders.toArray(new IJavaElement[srcFolders.size()]));
		}

		public static RefactoringStatusContext statusContext(IMember member) {
			try {
				ICompilationUnit cu = member.getCompilationUnit();
				if (cu!=null) {
					IFile file = (IFile) cu.getCorrespondingResource();
					if (file!=null) {
						return new FileStatusContext(file, RefactoringUtils.textRegion(member.getNameRange()));
					}
				}
			} catch (Exception e) {
				GrailsCoreActivator.log(e);
			}
			return null;
		}

		public static IRegion textRegion(ISourceRange nameRange) {
			if (nameRange!=null) {
				return new Region(nameRange.getOffset(), nameRange.getLength());
			}
			return null;
		}

}
