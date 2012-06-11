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
package org.grails.ide.eclipse.editor.gsp.tags;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.grails.ide.eclipse.core.GrailsCoreActivator;


/**
 * Provides support for parsing GSP tags JavaDoc to
 * get better content assist and hovers.
 * 
 * @author Andrew Eisenberg
 * @since 2.5.2
 * @see http://jira.codehaus.org/browse/GRAILS-6593
 */
public class GSPTagJavaDocParser {
    public static class GSPTagDescription {
        public final String description;
        public final Map<String, String> attributes;
        public final Set<String> requiredAttributes;
        public final boolean isEmpty;

        public GSPTagDescription(String description,
                Map<String, String> attributes,
                Set<String> requiredAttributes, boolean isEmpty) {
            this.description = description == null ? "" : description;
            this.attributes = Collections.unmodifiableMap(attributes);
            this.requiredAttributes = Collections.unmodifiableSet(requiredAttributes);
            this.isEmpty = isEmpty;
        }

        public GSPTagDescription(boolean isEmpty) {
            this(null, Collections.EMPTY_MAP, Collections.EMPTY_SET, isEmpty);
        }
    }

    private static final String REQUIRED = "REQUIRED";
    private static final String KNOWN_ATTRIBUTES = "Known Attributes";
    private static final String ATTR = "@attr";
    private static final String ENDL = "\n";
    private static final String BR = "<br/>";
    private static final String STRONG = "<b>";
    private static final String STRONG_END = "</b>";
    private static final String REQUIRED_STRONG = STRONG + REQUIRED + " " + STRONG_END;
    private static final String EMPTY_TAG = "@emptyTag";
    public static final Pattern OPEN_HEAD_PATTERN = Pattern.compile("<head>", Pattern.CASE_INSENSITIVE);
    public static final Pattern CLOSE_HEAD_PATTERN = Pattern.compile("</head>", Pattern.CASE_INSENSITIVE);
    public static final String OPEN_HEAD_REPLACE = "&lt;head&gt;";
    public static final String CLOSE_HEAD_REPLACE = "&lt;/head&gt;";

    public GSPTagDescription parseJavaDoc(IField jdtTagField, FieldNode tagField) {
        if (jdtTagField == null) {
            return null;
        }
        boolean isEmpty = isProbablyEmpty(tagField);
        try {
            ISourceRange javaDocRange = jdtTagField.getJavadocRange();
            if (javaDocRange != null) {
                IBuffer buffer = jdtTagField.getTypeRoot().getBuffer();
                String javaDocString = buffer.getText(javaDocRange.getOffset(), javaDocRange.getLength()) + "\nint x;";
                char[] javaDocChars = javaDocString.toCharArray();
                ASTParser parser = ASTParser.newParser(AST.JLS3);
                parser.setSource(javaDocChars);
                parser.setKind(ASTParser.K_CLASS_BODY_DECLARATIONS);
                TypeDeclaration result = (TypeDeclaration) parser.createAST(null);
                BodyDeclaration decl = (BodyDeclaration) result.bodyDeclarations().get(0);
                Javadoc doc = decl.getJavadoc();
                
                
                @SuppressWarnings("cast")
                List<ASTNode> tags = (List<ASTNode>) doc.tags();
                String description = null;
                Map<String, String> attrs = new LinkedHashMap<String, String>();
                Map<String, String> attrsInDescription = new LinkedHashMap<String, String>();
                Set<String> requiredAttrs = new HashSet<String>();
                for (ASTNode elt : tags) {
                    switch (elt.getNodeType()) {
                        case ASTNode.TAG_ELEMENT:
                            TagElement tagElt = (TagElement) elt;
                            if (tagElt.getTagName() == null) {
                                if (description == null)  {
                                    description = fragmentsToText(tagElt);
                                }
                            } else if (tagElt.getTagName().equals(ATTR)) {
                                fragmentsToAttrText(tagElt, attrs, attrsInDescription, requiredAttrs);
                            } else if (tagElt.getTagName().equals(EMPTY_TAG)) {
                                isEmpty = true;
                            }
                            break;
                    }
                }
                
                // now add the attrs and requiredAttrs to the description
                if (!attrsInDescription.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    if (description != null) {
                        sb.append(description + BR + BR + ENDL);
                    }
                    sb.append(STRONG + KNOWN_ATTRIBUTES + STRONG_END + BR + ENDL);
                    for (String attr : attrsInDescription.values()) {
                         sb.append(attr);
                    }
                    description = sb.toString();
                }
                return new GSPTagDescription(description, attrs, requiredAttrs, isEmpty);
            }
        } catch (JavaModelException e) {
            GrailsCoreActivator.log(e);
        } catch (IndexOutOfBoundsException e) {
            GrailsCoreActivator.log(e);
        }
        
        // javadoc is not available
        // but still keep track of whether or not it should be an empty tag
        return new GSPTagDescription(isEmpty);
    }

    /**
     * Examine AST structure of the field to see if it has less than 2 parameters to its closure
     * if so, then it is probably empty
     * @param tagField
     * @return
     */
    private boolean isProbablyEmpty(FieldNode tagField) {
        if (tagField == null) {
            return false;
        }
        Expression initialExpression = tagField.getInitialExpression();
        if (initialExpression instanceof ClosureExpression) {
            Parameter[] parameters = ((ClosureExpression) initialExpression).getParameters();
            return parameters == null || parameters.length < 2;
        }
        return false;
    }

    private void fragmentsToAttrText(TagElement tagElt,
            Map<String, String> attrs, Map<String, String> attrsInDescription, Set<String> requiredAttrs) {
        String simpleString = tagElt.toString();
        StringTokenizer tokenizer = new StringTokenizer(simpleString, " \t\n\r\f*");
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        
        if (tokenizer.hasMoreElements()) {
            // first element is @attr
            tokenizer.nextElement();
        }
        
        String attrName = null;
        if (tokenizer.hasMoreElements()) {
            attrName = tokenizer.nextToken();
            sb.append(STRONG + attrName + STRONG_END);
            sb2.append(STRONG + attrName + STRONG_END);
        }
        boolean isRequired;
        if (tokenizer.hasMoreElements()) {
            String maybeRequired = tokenizer.nextToken();
            isRequired = maybeRequired.compareToIgnoreCase(REQUIRED) == 0;
            sb2.append(" : ");
            if (isRequired) {
                sb.append(BR + ENDL + REQUIRED_STRONG);
                sb2.append(REQUIRED_STRONG);
            }
            sb.append(BR + BR + ENDL);
            if (!isRequired) {
                sb.append(maybeRequired);
                sb2.append(maybeRequired);
            }
        } else {
            isRequired = false;
        }
        while (tokenizer.hasMoreElements()) {
            if (sb.charAt(sb.length()-1) != ' ' && sb.charAt(sb.length()-1) != '\n') {
                sb.append(" ");
                sb2.append(" ");
            }
            String token = tokenizer.nextToken();
            sb.append(token);
            sb2.append(token);
        }
        sb2.append(BR + ENDL);
        if (attrName != null) {
            attrs.put(attrName, sb.toString());
            attrsInDescription.put(attrName, sb2.toString());
            if (isRequired) {
                requiredAttrs.add(attrName);
            }
        }
    }

    protected String fragmentsToText(TagElement tagElt) {
        String simpleString = tagElt.toString();
        StringTokenizer tokenizer = new StringTokenizer(simpleString, " \t\n\r\f*");
        StringBuilder sb = new StringBuilder();
        while (tokenizer.hasMoreElements()) {
            String token = tokenizer.nextToken();
            if (token.length() > 0) {
                // bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=367378
                // can't have a head tag in the javadoc
                token = OPEN_HEAD_PATTERN.matcher(token).replaceFirst(OPEN_HEAD_REPLACE);
                token = CLOSE_HEAD_PATTERN.matcher(token).replaceFirst(CLOSE_HEAD_REPLACE);
                sb.append(token);
                if (tokenizer.hasMoreElements()) {
                    sb.append(" ");
                }
            }
        }
        return sb.toString();
    }
}
