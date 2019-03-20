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

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.grails.ide.eclipse.core.GrailsCoreActivator;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;
import org.grails.ide.eclipse.editor.groovy.elements.IGrailsElement;

/**
 * Abstraction to encapsulate the method to compute extra renamings for a given renaming.
 * 
 * @author Kris De Volder
 * @since 2.7
 */
public abstract class ExtraRenamingsComputer {
	
	final public static boolean DEBUG = false;
	void debug(String string) {
		if (DEBUG) {
			System.out.println(this.getClass().getSimpleName()+": " +string);
		}
	}
	
	private final ITypeRenaming org;
	private final IJavaProject javaProject;
	
	private Collection<ITypeRenaming> extra = null;
	private ArrayList<ITypeRenaming> result;

	public ExtraRenamingsComputer(ITypeRenaming org) {
		Assert.isNotNull(org);
		this.org = org;
		this.javaProject = org.getTarget().getJavaProject();
	}

	protected IJavaProject getJavaProject() {
		return javaProject;
	}

	/**
	 * This method should be called before using the extra renamings computer. It may
	 * return an error / warning etc. status if, for some reason the original renaming
	 * violates a naming convention.
	 */
	public RefactoringStatus checkPreconditions() {
		return new RefactoringStatus();
	}
	
	
	/**
	 * Create appropriate renamings computer based on the original renaming. 
	 * @return An applicable {@link ExtraRenamingsComputer} or null (if not applicable).
	 */
	public static ExtraRenamingsComputer create(ITypeRenaming org) {
		IType target = org.getTarget();
		IGrailsElement element = GrailsWorkspaceCore.get().create(target);
		if (element!=null) {
			return new PostfixedClassExtraRenamingsComputer(org, 
					"ControllerTests", 
					"ControllerTest", 
					"Controller", 
					"ServiceTests",
					"ServiceTest",
					"Service",
					"TagLibTests",
					"TagLibTest",
					"TagLib",
					"Tests",
					"Test",
					""
			);
		}
		return null;
	}
	
	public ITypeRenaming getOrginalRenaming() {
		return org;
	}
	
	public Collection<ITypeRenaming> getExtraRenamings(IProgressMonitor pm) {
		if (this.extra==null) {
			this.extra = computeExtraRenamings(pm);
			Assert.isNotNull(this.extra, "ExtraRenamingsComputer.computeExtraRenamings must not return null");
		}
		return this.extra;
	}

	protected String simpleName(String fqBaseName) {
		int split = fqBaseName.lastIndexOf('.');
		if (split>=0) {
			return fqBaseName.substring(split+1);
		}
		return fqBaseName;
	}

	protected String packageName(String fqBaseName) {
		int split = fqBaseName.lastIndexOf('.');
		if (split>=0) {
			return fqBaseName.substring(0,split);
		}
		return "";
	}

	protected Collection<ITypeRenaming> computeExtraRenamings(IProgressMonitor pm) {
		if (result==null) {
			result = new ArrayList<ITypeRenaming>();
			computeDefaultRenamings(getPackageName(), getBaseName(), getNewBaseName());
		}
	
		return result;
	}

	protected abstract String getBaseName();

	protected abstract String getNewBaseName();

	protected void computeDefaultRenamings(String pkgName, String domainClassName,
			String newDomainClassName) {
		if (domainClassName==null || newDomainClassName==null) {
			return; // without base names to generate the naming patterns we can't do anything, so no extra renamings are generated
		}
		debug("Computing additional renamings for '"+pkgName+"."+domainClassName+"' => '"+newDomainClassName+"'");
		renamingCandidate(pkgName, domainClassName, newDomainClassName);
		renamingCandidate(pkgName, domainClassName + "Controller", 	newDomainClassName+"Controller");
		renamingCandidate(pkgName, domainClassName + "TagLib", 		newDomainClassName+"TagLib"	);
		renamingCandidate(pkgName, domainClassName + "Service", 	newDomainClassName+"Service"	);
	}

	protected String getOrgFullyQualifiedName() {
		return getOrginalRenaming().getTarget().getFullyQualifiedName('.');
	}

	private String getPackageName() {
		return packageName(getOrgFullyQualifiedName());
	}

	/**
	 * Add an extra renaming candidate to the list of results if it is a reasonable
	 * candidate. Some checks are performed to determine whether a candidate is reasonable.
	 * <p>
	 * This will also automatically add derived "XXXTest" and "XXXTests" candidates. 
	 */
	protected void renamingCandidate(String pkgName, String targetName, String newName) {
		boolean isTest = targetName.endsWith("Test") || targetName.endsWith("Tests");
		if (!isTest) {
			renamingCandidate(pkgName, targetName+"Test", newName+"Test");
			renamingCandidate(pkgName, targetName+"Tests", newName+"Tests");
		}
		
		String fqTargetName = pkgName+"."+targetName;
		String orgFqName = getOrgFullyQualifiedName();
		if (orgFqName.equals(fqTargetName)) {
			return; //Skip if the name is the same as the original renaming that triggered the additional renamings
		}
		try {
			IJavaProject javaProject = getJavaProject();
			IType targetType = javaProject.findType(fqTargetName);
			if (targetType!=null) {
				result.add(new TypeRenaming(targetType, newName));
			}
		} catch (JavaModelException e) {
			GrailsCoreActivator.log(e);
		}
	}
	
}
