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
package org.grails.ide.eclipse.test.gsp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.internal.corext.SourceRange;

import org.grails.ide.eclipse.editor.gsp.search.IGSPSearchRequestor;
import org.grails.ide.eclipse.editor.gsp.search.SearchInGSPs;

/**
 * @author Andrew Eisenberg
 * @since 2.7.0
 */
public class GSPSearchTests extends AbstractGSPTagsTest {
    
    public void testSearchForListType() throws Exception {
        String gspContent = "${List\nList f\nClass<List> t}";
        IType type = testProject.getJavaProject().findType("java.util.List");
        assertMatchesInGSP(gspContent, type, findInContents(gspContent, type.getElementName()));
    }
    
    public void testSearchForListIteratorMethod() throws Exception {
        String gspContent = "${List f\nf.iterator\n[].iterator}";
        IType type = testProject.getJavaProject().findType("java.util.List");
        IMember member = type.getMethod("iterator", new String[0]);
        assertMatchesInGSP(gspContent, member, findInContents(gspContent, member.getElementName()));
    }
    
    // Standard tag not being found because there is no source attachment
    public void _testSearchForStandardTag() throws Exception {
        String gspContent = "<g:form><g:form a=\"\"/></g:form>\n" +
        		"<g:form a=\"\" ><g:form /></g:form>";
        IType type = testProject.getJavaProject().findType("org.codehaus.groovy.grails.plugins.web.taglib.FormTagLib");
        IMember member = type.getField("form");
        assertMatchesInGSP(gspContent, member, findInContents(gspContent, member.getElementName()));
    }
    
    // Standard tag not being found because there is no source attachment
    public void _testSearchForStandardTag2() throws Exception {
        String gspContent = "<kkk:form /><g:form><g:form a=\"\"/></g:form>\n" +
        "<g:form a=\"\" ><g:form /></g:form>";
        IType type = testProject.getJavaProject().findType("org.codehaus.groovy.grails.plugins.web.taglib.FormTagLib");
        IMember member = type.getField("form");
        List<ISourceRange> findInContents = findInContents(gspContent, member.getElementName());
        findInContents.remove(0);
        assertMatchesInGSP(gspContent, member, findInContents);
    }

    public void testSearchForCustomTag() throws Exception {
        GroovyCompilationUnit unit = createTagLib("def myTag = { }");
        String gspContent = "<g:myTag><g:myTag a=\"\"/></g:myTag>\n" +
                "<g:myTag a=\"\" ><g:myTag /></g:myTag>";
        IMember member = unit.getTypes()[0].getField("myTag");
        assertMatchesInGSP(gspContent, member, findInContents(gspContent, member.getElementName()));
    }

    public void testSearchForCustomTag2() throws Exception {
        GroovyCompilationUnit unit = createTagLib("def myTag = { }");
        String gspContent = "<kkk:myTag /><g:myTag><g:myTag a=\"\"/></g:myTag>\n" +
        "<g:myTag a=\"\" ><g:myTag /></g:myTag>";
        IMember member = unit.getTypes()[0].getField("myTag");
        List<ISourceRange> findInContents = findInContents(gspContent, member.getElementName());
        findInContents.remove(0);  // first location isn't a match
        assertMatchesInGSP(gspContent, member, findInContents);
    }

    private List<ISourceRange> findInContents(String gspContent, String toFind) {
        List<ISourceRange> matches = new ArrayList<ISourceRange>();
        int from = 0;
        while (from < gspContent.length()) {
            int nextMatch = gspContent.indexOf(toFind, from);
            if (nextMatch > 0) {
                matches.add(new SourceRange(nextMatch, toFind.length()));
                from = nextMatch +1;
            } else {
                break;
            }
        }
        return matches;
    }

    class TestGSPRequestor implements IGSPSearchRequestor {
        
        final IFile fileToSearch;
        final IJavaElement elementToSearchFor;
        Set<ISourceRange> matches = new TreeSet<ISourceRange>(new Comparator<ISourceRange>() {
            public int compare(ISourceRange o1, ISourceRange o2) {
                return o1.getOffset() - o2.getOffset();
            }
        });
        

        public TestGSPRequestor(IFile fileToSearch, IJavaElement elementToSearchFor) {
            this.fileToSearch = fileToSearch;
            this.elementToSearchFor = elementToSearchFor;
        }

        public boolean searchForTags() {
            return true;
        }

        public void acceptMatch(IFile file, int start, int length) {
            matches.add(new SourceRange(start, length));
        }

        public int limitTo() {
            return IJavaSearchConstants.REFERENCES;
        }

        public IJavaElement elementToSearchFor() {
            return elementToSearchFor;
        }

        public List<IFile> getGSPsToSearch() {
            return Collections.singletonList(fileToSearch);
        }
        
    }
    

    private void assertMatchesInGSP(String gspContent, IJavaElement elementToSearchFor, List<ISourceRange> expectedMatches) throws Exception {
        assertTrue("Element to search for does not exits: " + elementToSearchFor, elementToSearchFor.exists());
        IFile file = testProject.createFile("grails-app/views/some.gsp", gspContent);
        TestGSPRequestor requestor = new TestGSPRequestor(file, elementToSearchFor);
        SearchInGSPs search = new SearchInGSPs();
        search.performSearch(requestor, null);
        
        assertEquals(expectedMatches, new ArrayList<ISourceRange>(requestor.matches));
    }
}