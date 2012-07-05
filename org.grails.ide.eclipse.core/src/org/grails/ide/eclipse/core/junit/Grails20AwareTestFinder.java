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
package org.grails.ide.eclipse.core.junit;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.junit.JUnitMessages;
import org.eclipse.jdt.internal.junit.launcher.ITestFinder;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.grails.ide.eclipse.core.internal.GrailsNature;
import org.grails.ide.eclipse.core.model.GrailsVersion;


/**
 * Tests related to JUnit integration (Run as >> Junit test) wotking with Grails 2.0.
 * 
 * @author Kris De Volder
 *
 * @since 2.9
 */
public class Grails20AwareTestFinder implements ITestFinder {
	
	public static final String TEST_FOR_ANNOT_NAME = "grails.test.mixin.TestFor";
	
	private static boolean DEBUG = false; //Platform.getLocation().toString().contains("kdvolder");
	
	private static void debug(Object msg) {
		if (DEBUG) {
			System.out.println(msg);
		}
	}

	private ITestFinder wrappee;
	
	public Grails20AwareTestFinder(ITestFinder finder) {
		this.wrappee = finder;
	}

	//ITestFinder implementation
	public void findTestsInContainer(IJavaElement element, Set result, IProgressMonitor pm) throws CoreException {
		if (enableFor(element)) {
			pm.beginTask("Searching for tests in "+element.getElementName(), 1);
			try {
				debug(">>>findTestsInContainer "+element.getElementName());
				if (element instanceof IType) {
					if (isGrail20Test((IType) element)) {
						result.add(element);
					}
				} else {
					searchForTests(element, result, new SubProgressMonitor(pm, 1));
				}
				debug("<<<findTestsInContainer "+element);
			} finally {
				pm.done();
			}
			wrappee.findTestsInContainer(element, result, pm);
			//We no longer filter the results. This means we may include tests that won't run correctly with
			//the 'naked' eclipse test runner. But users seem to prefer that over the filtering approach.
			//See https://issuetracker.springsource.com/browse/STS-2481
			//and https://issuetracker.springsource.com/browse/STS-2467
			//removeNonUnitTests(result);
		} else {
			wrappee.findTestsInContainer(element, result, pm);
		}
	}
	
	private void removeNonUnitTests(Set result) {
		Iterator<Object> iter = result.iterator();
		while (iter.hasNext()) {
			Object o = iter.next();
			if (o instanceof IType) { //Shouldn't be anything else than ITypes... but we check just to be sure.
				IType type = (IType) o;
				IPackageFragmentRoot pfr = (IPackageFragmentRoot) type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
				if (pfr!=null && isUnitTestPackageFragement(pfr)) {
					//Keep it
				} else {
					iter.remove();
				}
			}
		}
	}

	private void searchForTests(IJavaElement element, final Set result, IProgressMonitor pm) throws CoreException {
		//Loosely based on a copy of org.eclipse.jdt.internal.junit.launcher.JUnit4TestFinder.findTestsInContainer(IJavaElement, Set, IProgressMonitor)
		//Modifed to search just for Grails test classes marked by @TestFor annotations
		try {
			pm.beginTask(JUnitMessages.JUnit4TestFinder_searching_description, 4);

			IRegion region= CoreTestSearchEngine.getRegion(element);

			IJavaSearchScope scope= SearchEngine.createJavaSearchScope(region.getElements(), IJavaSearchScope.SOURCES);
			int matchRule= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
			SearchPattern testForPattern= SearchPattern.createPattern(TEST_FOR_ANNOT_NAME, IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE, matchRule);
			SearchPattern testPattern= SearchPattern.createPattern("*Tests", IJavaSearchConstants.TYPE, IJavaSearchConstants.DECLARATIONS, matchRule);

			SearchPattern theSearchPattern = SearchPattern.createOrPattern(testForPattern, testPattern);
			SearchParticipant[] searchParticipants = new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
			
			SearchRequestor requestor= new SearchRequestor() {
				@Override
				public void acceptSearchMatch(SearchMatch match) throws CoreException {
					if (match.getAccuracy() == SearchMatch.A_ACCURATE && !match.isInsideDocComment()) {
						Object element= match.getElement();
						if (element instanceof IType && isGrail20Test((IType) element)) {
							result.add(element);
						}
					}
				}
			};
			new SearchEngine().search(theSearchPattern, searchParticipants, scope, requestor, new SubProgressMonitor(pm, 2));
		} finally {
			pm.done();
		}
	}

//	private void findTestsIn(GroovyCompilationUnit element, Set result, IProgressMonitor pm) {
//		GroovyProjectFacade gproj = new GroovyProjectFacade(element);
//		List<ClassNode> types = element.getModuleNode().getClasses();
//		if (types!=null) {
//			for (ClassNode classNode : types) {
//				if (isGrailsTest(classNode)) {
//					result.add(o);
//				}
//			}
//		}
//	}

//	private boolean isGrailsTest(ClassNode classNode) {
//		List<AnnotationNode> annotations = classNode.getAnnotations();
//		for (AnnotationNode a : annotations) {
//			ClassNode cls = a.getClassNode();
//		}
//		return false;
//	}

	//ITestFinder implementation
	public boolean isTest(IType type) throws CoreException {
		debug("isTest? "+type.getElementName());
		boolean result = false;
		if (enableFor(type)) {
			debug("isTest? "+type.getElementName());
			//code specific for Grails 2.0 projects
			result = isGrail20Test(type);
			if (result) {
				return result; 
			} else {
				//We now always delegate the false case to wrappee. This means that we include integration and functional tests.
				//While those tests may not actually run correctly with the 'naked' eclipse test runner. Some users
				//are finding ways to make this work for them and get upset when it is no longer possible.
				//See https://issuetracker.springsource.com/browse/STS-2481
				//and https://issuetracker.springsource.com/browse/STS-2467
				return wrappee.isTest(type);
			}
		} else {
			return wrappee.isTest(type);
		}
	}

	private boolean isGrail20Test(IType type) {
		debug("isGrailsTest "+type);
		try {
			IPackageFragmentRoot pfr = (IPackageFragmentRoot) type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			if (pfr!=null) {
//				if (isUnitTestPackageFragement(pfr)) {
					ICompilationUnit cu = type.getCompilationUnit();
					if (cu instanceof GroovyCompilationUnit) {
						GroovyCompilationUnit gcu = (GroovyCompilationUnit) cu;
						ModuleNode module = gcu.getModuleNode();
						if (module != null) {
                            List<ClassNode> classes = module.getClasses();
                            String nameToFind = type.getFullyQualifiedName();
                            for (ClassNode classNode : classes) {
                                if (nameToFind.equals(classNode.getName())) {
                                    List<AnnotationNode> annots = classNode
                                            .getAnnotations();
                                    if (annots != null) {
                                        for (AnnotationNode annot : annots) {
                                            ClassNode annotClass = annot
                                                    .getClassNode();
                                            if (annotClass != null) {
                                                String annotName = annotClass
                                                        .getName();
                                                if (annotName
                                                        .equals(TEST_FOR_ANNOT_NAME)) {
                                                    return true;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            //If we get here the above test failed (i.e annotation wasn't found). Check Grails 2.0 naming conventions
                            //as well.
                            if (nameToFind.endsWith("Tests")) {
                                String domainName = nameToFind.substring(0, nameToFind.length()-"Tests".length());
                                IType domainType = gcu.getJavaProject().findType(domainName);
                                if (domainType!=null) {
                                    pfr = (IPackageFragmentRoot) domainType.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
                                    return isDomainPackageFragement(pfr);
                                }
                            }
                        }
					}
//				}
			}
		} catch (JavaModelException e) {
			GrailsCoreActivator.log(e);
		}
		return false;
	}

	private boolean isDomainPackageFragement(IPackageFragmentRoot pfr) {
		return isPackageFragementWithPath(pfr, "grails-app/domain");
	}

	private boolean isUnitTestPackageFragement(IPackageFragmentRoot pfr) {
		String pfrPath = "test/unit";
		return isPackageFragementWithPath(pfr, pfrPath);
	}

	public boolean isPackageFragementWithPath(IPackageFragmentRoot pfr,
			String pfrPath) {
		try {
			IResource rsrc = pfr.getCorrespondingResource();
			if (rsrc!=null) {
				IPath path = rsrc.getFullPath();
				debug("isGrailsTest pfr path = "+path);
				return path.removeFirstSegments(1).equals(new Path(pfrPath));
			}
		} catch (JavaModelException e) {
			GrailsCoreActivator.log(e);
		}
		return false;
	}

	/**
	 * This method determines whether grails aware functionality applies to a given element. If it
	 * returns false, then methods should just delegate to the original JUnit test finder.
	 */
	private boolean enableFor(IJavaElement el) {
//		debug("enableFor? "+el.getElementName());
		IJavaProject javaProject = el.getJavaProject();
		if (javaProject!=null) {
//			debug("enableFor? "+javaProject.getElementName());
			IProject project = javaProject.getProject();
			if (GrailsNature.isGrailsProject(project)) {
				GrailsVersion version = GrailsVersion.getEclipseGrailsVersion(project);
//				debug("enableFor? version = "+version);
				boolean result = GrailsVersion.V_2_0_0.compareTo(version) <=0;
//				debug("enableFor? => "+result);
				return result;
			} else {
//				debug("enableFor? not a Grails project");
			}
		}
//		debug("enableFor? => false");
		return false;
	}

	
}
