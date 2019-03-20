/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.refactoring.rename.type;
///******************************************************************************************
// * Copyright (c) 2011 SpringSource, a division of VMware, Inc. All rights reserved.
// ******************************************************************************************/
//package org.grails.ide.eclipse.refactoring.rename;
//
//import static org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind.SERVICE_CLASS;
//
//import java.util.ArrayList;
//import java.util.Collection;
//
//import org.eclipse.core.runtime.Assert;
//import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.jdt.core.IJavaProject;
//import org.eclipse.jdt.core.IType;
//import org.eclipse.jdt.core.JavaModelException;
//import org.eclipse.ltk.core.refactoring.RefactoringStatus;
//
//import org.grails.ide.eclipse.core.GrailsCoreActivator;
//import org.grails.ide.eclipse.core.internal.plugins.GrailsElementKind;
//
///**
// * Rename participant that participates in the renaming of special Grails domain classes
// * to rename related controllers, service classes and taglib classes. 
// * 
// * @author Kris De Volder
// * @since 2.7
// */
//public class GrailsServiceClassRenameParticipant extends GrailsTypeRenameParticipant {
//	
//	private static final String SERVICE = "Service";
//
//	protected boolean isInteresting() {
//		GrailsElementKind kind = project.getElementKind(cu); 
//		debug("kind = " + kind);
//		return isInterestingKind(kind); 
//	}
//
//	protected boolean isInterestingKind(GrailsElementKind kind) {
//		return SERVICE_CLASS==kind;
//	}
//
//	@Override
//	public String getName() {
//		return "Grails Service Rename Participant";
//	}
//
//	@Override
//	protected Collection<TypeRenaming> computeExtraRenamings(RefactoringStatus status, IProgressMonitor pm) {
//		ArrayList<TypeRenaming> result = new ArrayList<TypeRenaming>();
//		
//		String fqBaseName = type.getFullyQualifiedName();
//		String pkgName = packageName(fqBaseName);
//		String baseName = simpleName(fqBaseName);
//		Assert.isTrue(baseName.endsWith(SERVICE));
//		String newName = getArguments().getNewName();
//		
//		if (!newName.endsWith(SERVICE)) {
//			status.addWarning("The new name '"+newName+"' doesn't end with 'Service'. " +
//					"If you proceed with this renaming, the class will no longer be a Grails service!");
//		} else {
//			debug("Computing additional renamings for '"+fqBaseName+"' => '"+newName+"'");
//			
//			baseName = baseName.substring(0,baseName.length()-SERVICE.length());
//			String newBaseName = newName.substring(0, newName.length()-SERVICE.length());
//
//			renamingCandidate(pkgName + "." + baseName, 					newBaseName, 				result);
//			renamingCandidate(pkgName + "." + baseName + "ServiceTests", 	newBaseName+"ServiceTests", result);
//			renamingCandidate(pkgName + "." + baseName + "ServiceTest", 	newBaseName+"ServiceTest", 	result);
//			renamingCandidate(pkgName + "." + baseName + "Controller", 		newBaseName+"Controller", 	result);
//			renamingCandidate(pkgName + "." + baseName + "TagLib", 			newBaseName+"TagLib", 		result);
//		}
//		return result;
//	}
//
//	/**
//	 * Create and add an extra renaming to the result, if the corresponding target type can be found.
//	 */
//	private void renamingCandidate(String fqTargetName, String newName, ArrayList<TypeRenaming> result) {
//		try {
//			IJavaProject javaProject = project.getGroovyProject().getProject();
//			IType targetType = javaProject.findType(fqTargetName);
//			if (targetType!=null) {
//				result.add(new TypeRenaming(targetType, newName));
//				if (!(newName.endsWith("Test") || newName.endsWith("Tests"))) {
//					renamingCandidate(fqTargetName+"Test", newName+"Test", result);
//					renamingCandidate(fqTargetName+"Tests", newName+"Tests", result);
//				}
//			}
//		} catch (JavaModelException e) {
//			GrailsCoreActivator.log(e);
//		}
//	}
//
//}
