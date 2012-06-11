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
package org.grails.ide.eclipse.search.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.jdt.ui.search.QuerySpecification;
import org.eclipse.search.ui.text.Match;

import org.grails.ide.eclipse.search.AbstractGrailsSearch;
import org.grails.ide.eclipse.search.AbstractQueryParticipant;
import org.grails.ide.eclipse.test.util.GrailsTest;

public class AbstractGrailsSearchParticipantTest extends GrailsTest {
	
	public final String TEST_PROJECT_NAME = AbstractGrailsSearchParticipantTest.class.getSimpleName();
	public final String PACKAGE_NAME = TEST_PROJECT_NAME.toLowerCase();
	

	/**
	 * This class is just like {@link Match} but it implements hashCode and equals so we can
	 * put it into a hash set and compare expected matches easily to actual matches.
	 * 
	 * @author Kris De Volder
	 *
	 * @since 2.9
	 */
	public static class MatchInfo {
		public final Object element;
		public int start;
		public int len;
		public MatchInfo(Object element, int start, int len) {
			this.element = element;
			this.start = start;
			this.len = len;
		}
		@Override
		public String toString() {
			return "MatchInfo [element=" + toStr(element) + ", start=" + start
					+ ", len=" + len + "]";
		}
		private String toStr(Object e) {
			if (e instanceof IJavaElement) {
				return ((IJavaElement) e).getHandleIdentifier();
			} else {
				return e.toString();
			}
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((element == null) ? 0 : element.hashCode());
			result = prime * result + len;
			result = prime * result + start;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MatchInfo other = (MatchInfo) obj;
			if (element == null) {
				if (other.element != null)
					return false;
			} else if (!element.equals(other.element))
				return false;
			if (len != other.len)
				return false;
			if (start != other.start)
				return false;
			return true;
		}
		/**
		 * Change start and len as needed to avoid including surrounding quotes.
		 */
		public MatchInfo adjustForQuotes(String findString) {
			if (findString.startsWith("\"")) {
				start++;
				len--;
			}
			if (findString.endsWith("\"")) {
				len--;
			}
			return this;
		}
	}

	public static class MockSearchRequestor implements ISearchRequestor {
	
		Set<MatchInfo> expectedMatches = new HashSet<MatchInfo>();
		Set<MatchInfo> actualMatches = new HashSet<MatchInfo>();
		
		public MockSearchRequestor(MatchInfo[] expectedMatches) {
			for (MatchInfo matchInfo : expectedMatches) {
				this.expectedMatches.add(matchInfo);
			}
		}
	
		public void reportMatch(Match match) {
			actualMatches.add(new MatchInfo(match.getElement(), match.getOffset(), match.getLength()));
		}
		
		public void assertMatches() {
			StringBuilder errorMessage = new StringBuilder();
			for (MatchInfo actualMatch : actualMatches) {
				if (!expectedMatches.contains(actualMatch)) {
					errorMessage.append("Unexpected: "+actualMatch+"\n");
				}
				expectedMatches.remove(actualMatch);
			}
	
			//any matches that were seen have now been removed from expectedMatches... this means that any left over
			//where missing from the actual result.
			for (MatchInfo leftOverMatch : expectedMatches) {
				errorMessage.append("Expected but not found: "+leftOverMatch+"\n");
			}
			
			String msg = errorMessage.toString();
			if (msg.length()>0) {
				fail(errorMessage.toString());
			}
		}
	
	}

	public static AbstractGrailsSearchParticipantTest.MatchInfo methodMatch(IJavaProject javaProject, String packageName, String className,
			String methodName, String findString) throws JavaModelException {
		IType type = javaProject.findType(packageName+"."+className);
		IMethod method = null;
		for (IMethod m : type.getMethods()) {
			if (m.getElementName().equals(methodName)) {
				assertNull("Ambiguous: there are multple methods with the name '"+methodName+"' in the class '"+type.getFullyQualifiedName(), method);
				method = m;
			}
		}
		CompilationUnit cu = (CompilationUnit)method.getCompilationUnit();
		int methodStart = method.getSourceRange().getOffset();
		int methodLen = method.getSourceRange().getLength();
		int start = new String(cu.getContents()).indexOf(findString, methodStart);
		int len = findString.length();
		assertTrue(methodStart>0);
		assertTrue(methodLen>0);
		assertTrue(start>=methodStart);
		assertTrue(start+len <= methodStart+methodLen);
		return new AbstractGrailsSearchParticipantTest.MatchInfo(method, start, len).adjustForQuotes(findString);
	}

	public static AbstractGrailsSearchParticipantTest.MatchInfo fieldMatch(IJavaProject javaProject, String packageName, String className,
			String fieldName, String findString) throws JavaModelException {
		IField field = field(javaProject, packageName, className, fieldName);
		CompilationUnit cu = (CompilationUnit)field.getCompilationUnit();
		int fieldStart = field.getSourceRange().getOffset();
		int fieldLen = field.getSourceRange().getLength();
		int start = new String(cu.getContents()).indexOf(findString, fieldStart);
		int len = findString.length();
		assertTrue(fieldStart>0);
		assertTrue(fieldLen>0);
		assertTrue(start>=fieldStart);
		assertTrue(start+len <= fieldStart+fieldLen);
		return new AbstractGrailsSearchParticipantTest.MatchInfo(field, start, len).adjustForQuotes(findString);
	}

	public static IField field(IJavaProject javaProject, String packageName,
			String className, String fieldName) throws JavaModelException {
		IType type = javaProject.findType(packageName==null? className : packageName+"."+className);
		IField field = type.getField(fieldName);
		assertNotNull(field);
		return field;
	}
	
	protected void assertMatches(AbstractQueryParticipant searchParticipant, QuerySpecification query, AbstractGrailsSearchParticipantTest.MatchInfo... expectedMatches)
			throws CoreException {
		MockSearchRequestor requestor = new MockSearchRequestor(expectedMatches);
		searchParticipant.search(requestor, query, new NullProgressMonitor());
		requestor.assertMatches();
	}
	
	protected void assertMatches(AbstractGrailsSearch search, MatchInfo[] expectedMatches) {
		MockSearchRequestor requestor = new MockSearchRequestor(expectedMatches);
		search.perform(requestor);
		requestor.assertMatches();
	}

	protected MatchInfo[] determineExpectedMatches(IField withinElement, String template,
			String expectSnippet) throws JavaModelException {
		int searchStart = withinElement.getSourceRange().getOffset();
		int searchEnd = searchStart + withinElement.getSourceRange().getLength();
		return expectedMatchesHelper(withinElement, searchStart, searchEnd, template, expectSnippet);
	}
	
	protected MatchInfo[] determineExpectedMatches(IFile gspFile, String template, String expectSnippet) {
		long fileSize = gspFile.getLocation().toFile().length();
		assertTrue("File '"+gspFile+"' doesn't exist or has lenght zero", fileSize>0);
		return expectedMatchesHelper(gspFile, 0, (int) fileSize, template, expectSnippet);
	}

	private MatchInfo[] expectedMatchesHelper(Object withinElement,
			int searchStart, int searchEnd, String template, String expectSnippet) {
		String expectTemplate = template.replace("***", expectSnippet);
		ArrayList<MatchInfo> expectedMatches = new ArrayList<AbstractGrailsSearchParticipantTest.MatchInfo>();
		int searchPos = searchStart;
		while (searchPos>=0 && searchPos<searchEnd) {
			searchPos = expectTemplate.indexOf('#', searchPos);
			if (searchPos>=0 && searchPos<searchEnd) {
				int start = searchPos;
				int end = start;
				while (expectTemplate.charAt(end)=='#') {
					end++;
				}
				expectedMatches.add(new MatchInfo(withinElement, start, end-start));
				searchPos = end;
			}
		}
		return expectedMatches.toArray(new MatchInfo[expectedMatches.size()]);
	}


}
