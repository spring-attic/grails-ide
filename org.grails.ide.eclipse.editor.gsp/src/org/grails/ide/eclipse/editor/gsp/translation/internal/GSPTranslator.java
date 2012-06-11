// COPIED_FROM org.eclipse.jst.jsp.core.internal.java.JSPTranslator
/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     VMWare, Inc.    - augmented for use with Grails
 *******************************************************************************/
package org.grails.ide.eclipse.editor.gsp.translation.internal;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

import javax.servlet.jsp.tagext.VariableInfo;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jst.jsp.core.internal.JSPCoreMessages;
import org.eclipse.jst.jsp.core.internal.Logger;
import org.eclipse.jst.jsp.core.internal.contentmodel.TaglibController;
import org.eclipse.jst.jsp.core.internal.contentmodel.tld.TLDCMDocumentManager;
import org.eclipse.jst.jsp.core.internal.contentmodel.tld.provisional.JSP12TLDNames;
import org.eclipse.jst.jsp.core.internal.contentmodel.tld.provisional.TLDElementDeclaration;
import org.eclipse.jst.jsp.core.internal.contenttype.DeploymentDescriptorPropertyCache;
import org.eclipse.jst.jsp.core.internal.contenttype.DeploymentDescriptorPropertyCache.PropertyGroup;
import org.eclipse.jst.jsp.core.internal.domdocument.DOMDocumentForJSP;
import org.eclipse.jst.jsp.core.internal.java.EscapedTextUtil;
import org.eclipse.jst.jsp.core.internal.java.IJSPProblem;
import org.eclipse.jst.jsp.core.internal.java.JSP2ServletNameUtil;
import org.eclipse.jst.jsp.core.internal.java.JSPTranslator;
import org.eclipse.jst.jsp.core.internal.provisional.JSP11Namespace;
import org.eclipse.jst.jsp.core.internal.provisional.JSP12Namespace;
import org.eclipse.jst.jsp.core.internal.regions.DOMJSPRegionContexts;
import org.eclipse.jst.jsp.core.internal.taglib.CustomTag;
import org.eclipse.jst.jsp.core.internal.taglib.TaglibHelper;
import org.eclipse.jst.jsp.core.internal.taglib.TaglibHelperManager;
import org.eclipse.jst.jsp.core.internal.taglib.TaglibVariable;
import org.eclipse.jst.jsp.core.internal.util.FacetModuleCoreSupport;
import org.eclipse.jst.jsp.core.internal.util.ZeroStructuredDocumentRegion;
import org.eclipse.jst.jsp.core.jspel.IJSPELTranslator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.html.core.internal.contentmodel.JSP20Namespace;
import org.eclipse.wst.sse.core.internal.FileBufferModelManager;
import org.eclipse.wst.sse.core.internal.ltk.parser.BlockMarker;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionCollection;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionContainer;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.parser.ContextRegionContainer;
import org.eclipse.wst.xml.core.internal.provisional.contentmodel.CMDocumentTracker;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.grails.ide.eclipse.core.internal.plugins.GrailsCore;
import org.grails.ide.eclipse.editor.groovy.controllers.PerProjectControllerCache;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.StringTokenizer;

/**
 * Translates a JSP document into a HttpServlet subclass. Keeps two way mapping from
 * Java translation to the original JSP source, which can be obtained through
 * getJava2JspRanges() and getJsp2JavaRanges().
 * @author Andrew Eisenberg
 */
public class GSPTranslator implements Externalizable {
    /**
     * <p>This value should be incremented if any of the following methods change:
     * <ul>
     * <li>{@link #writeExternal(ObjectOutput)}</li>
     * <li>{@link #readExternal(ObjectInput)}</li>
     * <li>{@link #writeString(ObjectOutput, String)}</li>
     * <li>{@link #readString(ObjectInput)}</li>
     * <li>{@link #writeRanges(ObjectOutput, HashMap)}</li>
     * <li>{@link #readRanges(ObjectInput)}</li>
     * </ul>
     * 
     * This is because if any of these change then previously externalized {@link JSPTranslator}s
     * will no longer be able to be read by the new implementation.  This value is used by
     * the {@link Externalizable} API automatically to determine if the file being read is of the
     * correct version to be read by the current implementation of the {@link JSPTranslator}</p>
     * 
     * @see #writeExternal(ObjectOutput)
     * @see #readExternal(ObjectInput)
     * @see #writeString(ObjectOutput, String)
     * @see #readString(ObjectInput)
     * @see #writeRanges(ObjectOutput, HashMap)
     * @see #readRanges(ObjectInput)
     */
    private static final long serialVersionUID = 1L;
    
    /** for debugging */
    private static final boolean DEBUG = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.jst.jsp.core/debug/jspjavamapping")); //$NON-NLS-1$  //$NON-NLS-2$
    
    /** handy plugin ID constant */
    private static final String JSP_CORE_PLUGIN_ID = "org.eclipse.jst.jsp.core"; //$NON-NLS-1$
    
    // constants for reading extension point
    /** Default EL Translator extension ID */
    private static final String DEFAULT_JSP_EL_TRANSLATOR_ID = "org.eclipse.jst.jsp.defaultJSP20"; //$NON-NLS-1$
    
    /** the name of the element in the extension point */
    private static final String EL_TRANSLATOR_EXTENSION_NAME = "elTranslator"; //$NON-NLS-1$
    
    /** the name of the property in the extension point */
    private static final String ELTRANSLATOR_PROP_NAME = "ELTranslator"; //$NON-NLS-1$

    
    // these constants are commonly used strings during translation
    /** end line characters */
    public static final String ENDL = "\n"; //$NON-NLS-1$
    
    /** session variable declaration */
    private static final String SESSION_VARIABLE_DECLARATION = "javax.servlet.http.HttpSession session = pageContext.getSession();" + ENDL; //$NON-NLS-1$
    
    /** footer text */
    private static final String FOOTER = "}}"; //$NON-NLS-1$
    
    /** exception declaration */
    private static final String EXCEPTION = "Throwable exception = null;"; //$NON-NLS-1$
    
    /** expression prefix */
    public static final String EXPRESSION_PREFIX = "out.print("; //$NON-NLS-1$
    
    /** expression suffix */
    public static final String EXPRESSION_SUFFIX = ");"; //$NON-NLS-1$
    
    /** try/catch start */
    private static final String TRY_CATCH_START = ENDL + "try {" + ENDL; //$NON-NLS-1$
    
    /** try/catch end */
    private static final String TRY_CATCH_END = " } catch (java.lang.Exception e) {} " + ENDL; //$NON-NLS-1$
    
    /** JSP tag name prefix */
    static final String JSP_PREFIX = "jsp:"; //$NON-NLS-1$
    
    
    // these constants are to keep track of what type of code is currently being translated
    /** code in question is standard JSP */
    protected final static int STANDARD_JSP = 0;
    
    /** code in question is embedded (JSP as an attribute or within comment tags) */
    protected final static int EMBEDDED_JSP = 1;
    
    /** code in question is a JSP declaration */
    protected final static int DECLARATION = 2;
    
    /** code in question is a JSP expression */
    protected final static int EXPRESSION = 4;
    
    /** code in question is a JSP scriptlet */
    protected final static int SCRIPTLET = 8;
    
    
    // strings specific to this translation
    /** translated class header */
    String fClassHeader = null;
    
    /** translated class name */
    String fClassname = null;
    
    /** translated class super class */
    String fSuperclass = null;

    /** translated class imports */
    String fImplicitImports = null;

    /** translated class service header */
    String fServiceHeader = null;
    
    String fGrailsActionParameters = null;
    
    /** translated user defined imports */
    private StringBuffer fUserImports = new StringBuffer();
    
    //translation specific state
    /** {@link IDOMModel} for the JSP file being translated */
    IDOMModel fStructuredModel = null;
    
    /** {@link IStructuredDocument} for the JSP file being translated */
    IStructuredDocument fStructuredDocument = null;
    
    /** the EL translator */
    private IJSPELTranslator fELTranslator = null;
    
    /** reported translation problems */
    private List fTranslationProblems = new ArrayList();
    
    /** fSourcePosition = position in JSP source */
    private int fSourcePosition = -1;
    
    /** fRelativeOffest = offset in the buffer there the cursor is */
    private int fRelativeOffset = -1;
    
    /** fCursorPosition = offset in the translated java document */
    private int fCursorPosition = -1;

    /** some page directive attributes */
    private boolean fIsErrorPage = false;
    private boolean fCursorInExpression = false;
    private boolean fIsInASession = true;

    /** user java code in body of the service method */
    private StringBuffer fUserCode = new StringBuffer();
    /** user EL Expression */
    private StringBuffer fUserELExpressions = new StringBuffer();
    /** user defined vars declared in the beginning of the class */
    private StringBuffer fUserDeclarations = new StringBuffer();

    /**
     * A map of tag names to tag library variable information; used to store
     * the ones needed for AT_END variable support.
     */
    private StackMap fTagToVariableMap = null;
    private Stack fUseBeansStack = new Stack();

    /** the final translated java document */
    private StringBuffer fResult;
    
    /** the buffer where the cursor is */
    private StringBuffer fCursorOwner = null;

    private IStructuredDocumentRegion fCurrentNode;
    
    /** flag for if the cursor is in the current regionb eing translated */
    private boolean fInCodeRegion = false;

    /** used to avoid infinite looping include files */
    private Stack fIncludes = null;
    private Set fIncludedPaths = new HashSet(2);
    private boolean fProcessIncludes = true;
    /** mostly for helper classes, so they parse correctly */
    private ArrayList fBlockMarkers = null;
    /**
     * for keeping track of offset in user buffers while document is being
     * built
     */
    private int fOffsetInUserImports = 0;
    private int fOffsetInUserDeclarations = 0;
    private int fOffsetInUserCode = 0;

    /** correlates ranges (positions) in java to ranges in jsp */
    private HashMap fJava2JspRanges = new HashMap();

    /**
     * map of ranges in fUserImports (relative to the start of the buffer) to
     * ranges in source JSP buffer.
     */
    private HashMap fImportRanges = new HashMap();
    /**
     * map of ranges in fUserCode (relative to the start of the buffer) to
     * ranges in source JSP buffer.
     */
    private HashMap fCodeRanges = new HashMap();
    /**
     * map of ranges in fUserDeclarations (relative to the start of the
     * buffer) to ranges in source JSP buffer.
     */
    private HashMap fDeclarationRanges = new HashMap();

    private HashMap fUseBeanRanges = new HashMap();

    private HashMap fUserELRanges = new HashMap();

    /**
     * ranges that don't directly map from java code to JSP code (eg.
     * <%@include file="included.jsp"%>
     */
    private HashMap fIndirectRanges = new HashMap();

    private IProgressMonitor fProgressMonitor = null;

    /**
     * save JSP document text for later use may just want to read this from
     * the file or strucdtured document depending what is available
     */
    private StringBuffer fJspTextBuffer = new StringBuffer();

    /** EL Translator ID (pluggable) */
    private String fELTranslatorID;

    /**
     * <code>true</code> if code has been found, such as HTML tags, that is not translated
     * <code>false</code> otherwise.  Useful for deciding if a place holder needs to be
     * written to translation
     */
    private boolean fFoundNonTranslatedCode;

    /**
     * <code>true</code> if code has been translated for the current region,
     * <code>false</code> otherwise
     */
    private boolean fCodeTranslated;

    /**
     * GRAILS change
     * the workspace resource where this gsp comes from
     * will be null if it cannot be determined
     */
    private final IFile gspFile;

    /**
     * A structure for holding a region collection marker and list of variable
     * information. The region can be used later for positioning validation
     * messages.
     */
    static class RegionTags {
        ITextRegionCollection region;
        CustomTag tag;

        RegionTags(ITextRegionCollection region, CustomTag tag) {
            this.region = region;
            this.tag = tag;
        }
    }

    public GSPTranslator(IFile gspFile) {
        this.gspFile = gspFile;
        init();
    }

    /**
     * configure using an XMLNode
     * 
     * @param node
     * @param monitor
     */
    private void configure(IDOMNode node, IProgressMonitor monitor) {

        fProgressMonitor = monitor;
        fStructuredModel = node.getModel();
        String baseLocation = fStructuredModel.getBaseLocation();

        fELTranslatorID = getELTranslatorProperty(baseLocation);

        fStructuredDocument = fStructuredModel.getStructuredDocument();

        String className = createClassname(node);
        if (className.length() > 0) {
            setClassname(className);
            fClassHeader = "public class " + className + " extends "; //$NON-NLS-1$ //$NON-NLS-2$
        }

    }

    /**
     * memory saving configure (no StructuredDocument in memory) currently
     * doesn't handle included files
     * 
     * @param jspFile
     * @param monitor
     */
    private void configure(IFile jspFile, IProgressMonitor monitor) {
        // when configured on a file
        // fStructuredModel, fPositionNode, fModelQuery, fStructuredDocument
        // are all null
        fProgressMonitor = monitor;

        fELTranslatorID = getELTranslatorProperty(jspFile);

        String className = createClassname(jspFile);
        if (className.length() > 0) {
            setClassname(className);
            fClassHeader = "public class " + className + " extends "; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Set the jsp text from an IFile
     * 
     * @param jspFile
     */
    private void setJspText(IFile jspFile) {
        try {
            BufferedInputStream in = new BufferedInputStream(jspFile.getContents());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while ((line = reader.readLine()) != null) {
                fJspTextBuffer.append(line);
                fJspTextBuffer.append(ENDL);
            }
            reader.close();
        }
        catch (CoreException e) {
            Logger.logException(e);
        }
        catch (IOException e) {
            Logger.logException(e);
        }
    }

    /**
     * Get the value of the ELTranslator property from a workspace relative
     * path string
     * 
     * @param baseLocation
     *            Workspace-relative string path
     * @return Value of the ELTranslator property associated with the project.
     */
    private String getELTranslatorProperty(String baseLocation) {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        String elTranslatorValue = null;
        IFile file = workspaceRoot.getFile(new Path(baseLocation));
        if (file != null) {
            elTranslatorValue = getELTranslatorProperty(file);
        }
        return elTranslatorValue;
    }

    /**
     * Get the value of the ELTranslator property from an IFile
     * 
     * @param file
     *            IFile
     * @return Value of the ELTranslator property associated with the project.
     */
    private String getELTranslatorProperty(IFile file) {
        String elTranslatorValue = null;
        if (file != null) {
            if (file.exists()) {
                try {
                    elTranslatorValue = file.getPersistentProperty(new QualifiedName(JSP_CORE_PLUGIN_ID, ELTRANSLATOR_PROP_NAME));
                    if (null == elTranslatorValue) {

                        elTranslatorValue = file.getProject().getPersistentProperty(new QualifiedName(JSP_CORE_PLUGIN_ID, ELTRANSLATOR_PROP_NAME));
                    }
                }
                catch (CoreException e) {
                    // ISSUE: why do we log this here? Instead of allowing to
                    // throwup?
                    Logger.logException(e);
                }

            }
        }
        return elTranslatorValue;
    }

    /**
     * @param node
     * @return the simple class name, not fully qualified
     */
    private String createClassname(IDOMNode node) {
        String classname = ""; //$NON-NLS-1$
        if (node != null) {
            String base = node.getModel().getBaseLocation();
            classname = JSP2ServletNameUtil.mangle(base);
        }
        return classname;
    }

    /**
     * @param jspFile
     * @return
     */
    private String createClassname(IFile jspFile) {

        String classname = ""; //$NON-NLS-1$
        if (jspFile != null) {
            classname = JSP2ServletNameUtil.mangle(jspFile.getFullPath().toString());
        }
        return classname;
    }

    private IJSPProblem createJSPProblem(final int problemEID, final int problemID, final String message, final int start, final int end) {
        final int line = fStructuredDocument.getLineOfOffset(start);
        final char[] classname = fClassname.toCharArray();

        /*
         * Note: these problems would result in translation errors on the
         * server, so the severity is not meant to be controllable
         */
        return new IJSPProblem() {
            public void setSourceStart(int sourceStart) {
            }

            public void setSourceLineNumber(int lineNumber) {
            }

            public void setSourceEnd(int sourceEnd) {
            }

            public boolean isWarning() {
                return false;
            }

            public boolean isError() {
                return true;
            }

            public int getSourceStart() {
                return start;
            }

            public int getSourceLineNumber() {
                return line;
            }

            public int getSourceEnd() {
                return end;
            }

            public char[] getOriginatingFileName() {
                return classname;
            }

            public String getMessage() {
                return message;
            }

            public int getID() {
                return problemID;
            }

            public String[] getArguments() {
                return new String[0];
            }

            public int getEID() {
                return problemEID;
            }
        };
    }

    public void setClassname(String classname) {
        this.fClassname = classname;
    }

    public String getClassname() {
        return this.fClassname != null ? this.fClassname : "GenericJspServlet"; //$NON-NLS-1$
    }

    /**
     * So that the JSPTranslator can be reused.
     */
    public void reset(IDOMNode node, IProgressMonitor progress) {

        // initialize some things on node
        configure(node, progress);
        reset();
        // set the jsp text buffer
        fJspTextBuffer.append(fStructuredDocument.get());
    }

    /**
     * conservative version (no StructuredDocument/Model)
     * 
     * @param jspFile
     * @param progress
     */
    public void reset(IFile jspFile, IProgressMonitor progress) {

        // initialize some things on node
        configure(jspFile, progress);
        reset();
        // set the jsp text buffer
        setJspText(jspFile);
    }

    /**
     * Reinitialize some fields
     */
    private void reset() {

        // reset progress monitor
        if (fProgressMonitor != null)
            fProgressMonitor.setCanceled(false);

        // reinit fields
        fSourcePosition = -1;
        fRelativeOffset = -1;
        fCursorPosition = -1;

        fIsErrorPage = fCursorInExpression = false;
        fIsInASession = true;

        fUserCode = new StringBuffer();
        fUserDeclarations = new StringBuffer();
        fUserImports = new StringBuffer();
        fUserELExpressions = new StringBuffer();

        fResult = null;
        fCursorOwner = null; // the buffer where the cursor is

        fCurrentNode = null;
        fInCodeRegion = false; // flag for if cursor is in the current region
        // being translated

        if (fIncludes != null)
            fIncludes.clear();

        fBlockMarkers = null;

        fOffsetInUserImports = 0;
        fOffsetInUserDeclarations = 0;
        fOffsetInUserCode = 0;

        fJava2JspRanges.clear();
        fImportRanges.clear();
        fCodeRanges.clear();
        fUseBeanRanges.clear();
        fDeclarationRanges.clear();
        fUserELRanges.clear();
        fIndirectRanges.clear();
        fIncludedPaths.clear();

        fJspTextBuffer = new StringBuffer();
        
        fFoundNonTranslatedCode = false;
        fCodeTranslated = false;

    }

    /**
     * @return just the "shell" of a servlet, nothing contributed from the JSP
     *         doc
     */
    public final StringBuffer getEmptyTranslation() {
        reset();
        buildResult(true);
        return getTranslation();
    }

    /**
     * <p>put the final java document together</p>
     * 
     * @param updateRanges <code>true</code> if the ranges need to be updated as the result
     * is built, <code>false</code> if the ranges have already been updated.  This is useful
     * if building a result from a persisted {@link JSPTranslator}.
     */
    private final void buildResult(boolean updateRanges) {
        // to build the java document this is the order:
        // 
        // + default imports
        // + user imports
        // + class header
        // [+ error page]
        // + user declarations
        // + service method header
        // + try/catch start
        // + user code
        // + try/catch end
        // + service method footer
        fResult = new StringBuffer(fImplicitImports.length() + fUserImports.length() + fClassHeader.length() +
                fUserDeclarations.length() + fServiceHeader.length() + TRY_CATCH_START.length()
                + fUserCode.length() + TRY_CATCH_END.length() + FOOTER.length());

        int javaOffset = 0;

        fResult.append(fImplicitImports);
        javaOffset += fImplicitImports.length();

        // updateRanges(fIndirectImports, javaOffset);
        if(updateRanges) {
            updateRanges(fImportRanges, javaOffset);
        }
        // user imports
        append(fUserImports);
        javaOffset += fUserImports.length();

        // class header
        fResult.append(fClassHeader); //$NON-NLS-1$
        javaOffset += fClassHeader.length();
        fResult.append(fSuperclass + "{" + ENDL); //$NON-NLS-1$
        javaOffset += fSuperclass.length() + 2;

        if(updateRanges) {
            updateRanges(fDeclarationRanges, javaOffset);
        }
        // user declarations
        append(fUserDeclarations);
        javaOffset += fUserDeclarations.length();

        if(updateRanges) {
            updateRanges(fUserELRanges, javaOffset);
        }
        append(fUserELExpressions);
        javaOffset += fUserELExpressions.length();

        fResult.append(fServiceHeader);
        javaOffset += fServiceHeader.length();
        
        // GRAILS change
        if (fGrailsActionParameters != null) {
            fResult.append(fGrailsActionParameters);
            javaOffset += fGrailsActionParameters.length();
        }
        
        // session participant
        if (fIsInASession) {
            fResult.append(SESSION_VARIABLE_DECLARATION);
            javaOffset += SESSION_VARIABLE_DECLARATION.length();
        }
        // error page
        if (fIsErrorPage) {
            fResult.append(EXCEPTION);
            javaOffset += EXCEPTION.length();
        }


        fResult.append(TRY_CATCH_START);
        javaOffset += TRY_CATCH_START.length();

        if(updateRanges) {
            updateRanges(fCodeRanges, javaOffset);
        }

        // user code
        append(fUserCode);
        javaOffset += fUserCode.length();


        fResult.append(TRY_CATCH_END);
        javaOffset += TRY_CATCH_END.length();

        // footer
        fResult.append(FOOTER);
        javaOffset += FOOTER.length();

        fJava2JspRanges.putAll(fImportRanges);
        fJava2JspRanges.putAll(fDeclarationRanges);
        fJava2JspRanges.putAll(fCodeRanges);
        fJava2JspRanges.putAll(fUserELRanges);

    }

    /**
     * @param javaRanges
     * @param offsetInJava
     */
    private void updateRanges(HashMap rangeMap, int offsetInJava) {
        // just need to update java ranges w/ the offset we now know
        Iterator it = rangeMap.keySet().iterator();
        while (it.hasNext())
            ((Position) it.next()).offset += offsetInJava;
    }

    /**
     * map of ranges (positions) in java document to ranges in jsp document
     * 
     * @return a map of java positions to jsp positions.
     */
    public HashMap getJava2JspRanges() {
        return fJava2JspRanges;
    }

    /**
     * map of ranges in jsp document to ranges in java document.
     * 
     * @return a map of jsp positions to java positions, or null if no
     *         translation has occured yet (the map hasn't been built).
     */
    public HashMap getJsp2JavaRanges() {
        if (fJava2JspRanges == null)
            return null;
        HashMap flipFlopped = new HashMap();
        Iterator keys = fJava2JspRanges.keySet().iterator();
        Object range = null;
        while (keys.hasNext()) {
            range = keys.next();
            flipFlopped.put(fJava2JspRanges.get(range), range);
        }
        return flipFlopped;
    }

    public HashMap getJava2JspImportRanges() {
        return fImportRanges;
    }

    public HashMap getJava2JspUseBeanRanges() {
        return fUseBeanRanges;
    }

    public HashMap getJava2JspIndirectRanges() {
        return fIndirectRanges;
    }

    /**
     * Adds to the jsp<->java map by default
     * 
     * @param value
     *            a comma delimited list of imports
     */
    protected void addImports(String value) {
        addImports(value, true);
    }

    /**
     * Pass in a comma delimited list of import values, appends each to the
     * final result buffer
     * 
     * @param value
     *            a comma delimited list of imports
     */
    protected void addImports(String value, boolean addToMap) {
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=81687
        // added the "addToMap" parameter to exclude imports originating
        // from included JSP files to be added to the jsp<->java mapping
        StringTokenizer st = new StringTokenizer(value, ",", false); //$NON-NLS-1$
        String tok = ""; //$NON-NLS-1$
        // String appendage = ""; //$NON-NLS-1$
        while (st.hasMoreTokens()) {
            tok = st.nextToken();
            appendImportToBuffer(tok, fCurrentNode, addToMap);
        }
    }

    /**
     * /* keep track of cursor position inside the buffer /* appends buffer to
     * the final result buffer
     */
    protected void append(StringBuffer buf) {
        if (getCursorOwner() == buf) {
            fCursorPosition = fResult.length() + getRelativeOffset();
        }
        fResult.append(buf.toString());
    }

    /**
     * Only valid after a configure(...), translate(...) or
     * translateFromFile(...) call
     * 
     * @return the current result (java translation) buffer
     */
    public final StringBuffer getTranslation() {

        if (DEBUG) {
            StringBuffer debugString = new StringBuffer();
            try {
                Iterator it = fJava2JspRanges.keySet().iterator();
                while (it.hasNext()) {
                    debugString.append("--------------------------------------------------------------\n"); //$NON-NLS-1$
                    Position java = (Position) it.next();
                    debugString.append("Java range:[" + java.offset + ":" + java.length + "]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    debugString.append("[" + fResult.toString().substring(java.offset, java.offset + java.length) + "]\n"); //$NON-NLS-1$ //$NON-NLS-2$
                    debugString.append("--------------------------------------------------------------\n"); //$NON-NLS-1$
                    debugString.append("|maps to...|\n"); //$NON-NLS-1$
                    debugString.append("==============================================================\n"); //$NON-NLS-1$
                    Position jsp = (Position) fJava2JspRanges.get(java);
                    debugString.append("JSP range:[" + jsp.offset + ":" + jsp.length + "]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    debugString.append("[" + fJspTextBuffer.toString().substring(jsp.offset, jsp.offset + jsp.length) + "]\n"); //$NON-NLS-1$ //$NON-NLS-2$
                    debugString.append("==============================================================\n"); //$NON-NLS-1$
                    debugString.append("\n"); //$NON-NLS-1$
                    debugString.append("\n"); //$NON-NLS-1$
                }
            }
            catch (Exception e) {
                Logger.logException("JSPTranslation error", e); //$NON-NLS-1$
            }
            debugString.append("==============================================================\n"); //$NON-NLS-1$
            debugString.append("\n"); //$NON-NLS-1$
            debugString.append("GSP Text"); //$NON-NLS-1$
            debugString.append("\n"); //$NON-NLS-1$
            debugString.append(fResult); //$NON-NLS-1$
            Logger.log(Logger.INFO_DEBUG, debugString.toString());
            System.out.println(debugString);
        }

        return fResult;
    }

    public List getTranslationProblems() {
        return fTranslationProblems;
    }

    /**
     * Only valid after a configure(...), translate(...) or
     * translateFromFile(...) call
     * 
     * @return the text in the JSP file
     */
    public final String getJspText() {
        return fJspTextBuffer.toString();
    }

    /**
     * Add the server-side scripting variables used by this tag, along with
     * any scoping.
     * 
     * @param tagToAdd
     * @param customTag
     * @param regions 
     */
    protected void addTaglibVariables(String tagToAdd, ITextRegionCollection customTag) {
        if (customTag.getFirstRegion().getType().equals(DOMRegionContext.XML_TAG_OPEN)) {
            /*
             * Start tag
             */
            if (!"g:def".equals(tagToAdd)) {
                addStartTagVariable(tagToAdd, customTag);
            }
            // GRAILS handle declaration
            if ("g:def".equals(tagToAdd) || "g:set".equals(tagToAdd) || "g:each".equals(tagToAdd)) {
                addVariableAssignment(tagToAdd, customTag);
            }
        }
        else if (customTag.getFirstRegion().getType().equals(DOMRegionContext.XML_END_TAG_OPEN)) {
            /*
             * End tag
             */
            addEndTagVariable(tagToAdd, customTag);
        }
    }
    
    private void addEndTagVariable(String tagToAdd, ITextRegionCollection customTag){
        IFile f = getFile();
        if (f == null || !f.exists())
            return;
        String decl = ""; //$NON-NLS-1$
        RegionTags regionTag = (RegionTags) fTagToVariableMap.pop(tagToAdd);
        if (regionTag != null) {
            // even an empty array will indicate a need for a closing brace
            TaglibVariable[] taglibVars = regionTag.tag.getTagVariables();
            StringBuffer text = new StringBuffer();
            if (regionTag.tag.isIterationTag())
                doAfterBody(text, regionTag);
            text.append("} // </"); //$NON-NLS-1$
            text.append(tagToAdd);
            text.append(">\n"); //$NON-NLS-1$
            appendToBuffer(text.toString(), fUserCode, false, customTag); //$NON-NLS-1$
            for (int i = 0; i < taglibVars.length; i++) {
                if (taglibVars[i].getScope() == VariableInfo.AT_END) {
                    decl = taglibVars[i].getDeclarationString();
                    appendToBuffer(decl, fUserCode, false, customTag);
                }
            }
        }
        else {
            /*
             * Since something should have been in the map because of a
             * start tag, its absence now means an unbalanced end tag.
             * Extras will be checked later to flag unbalanced start tags.
             */
            IJSPProblem missingStartTag = createJSPProblem(IJSPProblem.StartCustomTagMissing, IJSPProblem.F_PROBLEM_ID_LITERAL, "No start tag for " + tagToAdd, customTag.getStartOffset(), customTag.getEndOffset());
            fTranslationProblems.add(missingStartTag);
        }
    }
    private void addStartTagVariable(String tagToAdd,ITextRegionCollection customTag){
        IFile f = getFile();

        if (f == null || !f.exists())
            return;
        TaglibHelper helper = TaglibHelperManager.getInstance().getTaglibHelper(f);
        String decl = ""; //$NON-NLS-1$
        List problems = new ArrayList();
        CustomTag tag = helper.getCustomTag(tagToAdd, getStructuredDocument(), customTag, problems);
        TaglibVariable[] taglibVars = tag.getTagVariables();
        fTranslationProblems.addAll(problems);
        /*
         * Add AT_BEGIN variables
         */
        for (int i = 0; i < taglibVars.length; i++) {
            if (taglibVars[i].getScope() == VariableInfo.AT_BEGIN) {
                decl = taglibVars[i].getDeclarationString();
                appendToBuffer(decl, fUserCode, false, customTag);
            }
        }
        boolean isEmptyTag = isEmptyTag(customTag);
        
        /*
         * Add a single  { to limit the scope of NESTED variables
         */
        StringBuffer text = new StringBuffer();
        if (!isEmptyTag && tag.isIterationTag() && tag.getTagClassName() != null) {
            text.append("\nwhile(true) "); //$NON-NLS-1$
        }
        // GRAILS Change: always need something around the anonymous block
        else {
            text.append("\nif(true) "); //$NON-NLS-1$
        }
        // GRAILS End
        text.append("{ // <"); //$NON-NLS-1$
        text.append(tagToAdd);
        if (isEmptyTag)
            text.append("/>\n"); //$NON-NLS-1$
        else
            text.append(">\n"); //$NON-NLS-1$

        appendToBuffer(text.toString(), fUserCode, false, customTag); //$NON-NLS-1$

        for (int i = 0; i < taglibVars.length; i++) {
            if (taglibVars[i].getScope() == VariableInfo.NESTED) {
                decl = taglibVars[i].getDeclarationString();
                appendToBuffer(decl, fUserCode, false, customTag);
            }
        }
        /*
         * For empty tags, add the corresponding } and AT_END variables immediately.  
         */
        if (isEmptyTag) {
            text = new StringBuffer();
            text.append("} // <"); //$NON-NLS-1$
            text.append(tagToAdd);
            text.append("/>\n"); //$NON-NLS-1$
            appendToBuffer(text.toString(), fUserCode, false, customTag); //$NON-NLS-1$
            /* Treat this as the end for empty tags */
            for (int i = 0; i < taglibVars.length; i++) {
                if (taglibVars[i].getScope() == VariableInfo.AT_END) {
                    decl = taglibVars[i].getDeclarationString();
                    appendToBuffer(decl, fUserCode, false, customTag);
                }
            }
        }
        else {
            /*
             * For non-empty tags, remember the variable information
             */
            fTagToVariableMap.push(tagToAdd, new RegionTags(customTag, tag));
        }
        
    }

    private boolean isEmptyTag(ITextRegionCollection customTag) {
        ITextRegion lastRegion = customTag.getLastRegion();
        // custom tag is embedded
        if (customTag instanceof ITextRegionContainer) {
            ITextRegionList regions = customTag.getRegions();
            int size = customTag.getNumberOfRegions() - 1;
            while (size > 0 && !(DOMRegionContext.XML_EMPTY_TAG_CLOSE.equals(lastRegion.getType()) || DOMRegionContext.XML_TAG_NAME.equals(lastRegion.getType()) || DOMRegionContext.XML_TAG_CLOSE.equals(lastRegion.getType()) )) {
                lastRegion = regions.get(--size);
            }
        }
        
        return DOMRegionContext.XML_EMPTY_TAG_CLOSE.equals(lastRegion.getType());
    }

    private void addCustomTaglibVariables(String tagToAdd, ITextRegionCollection customTag, ITextRegion prevRegion) {
        //Can't judge by first region as start and end tag are part of same ContextRegionContainer      
        if (prevRegion != null && prevRegion.getType().equals(DOMRegionContext.XML_END_TAG_OPEN)) {
            /*
             * End tag
             */
            addEndTagVariable(tagToAdd, customTag);
        }
        else if (prevRegion != null && prevRegion.getType().equals(DOMRegionContext.XML_TAG_OPEN)) {
            /*
             * Start tag
             */
            addStartTagVariable(tagToAdd,customTag);
        }
    }

    private void doAfterBody(StringBuffer buffer, RegionTags regionTag) {
        // GRAILS change unnecessary
//        buffer.append("\tif ( (new "); //$NON-NLS-1$
//        buffer.append(regionTag.tag.getTagClassName());
//        buffer.append("()).doAfterBody() != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN)\n\t\tbreak;\n"); //$NON-NLS-1$
    }

    /**
     * @return the workspace file for this model, null otherwise
     */
    private IFile getFile() {
        IFile f = null;
        ITextFileBuffer buffer = FileBufferModelManager.getInstance().getBuffer(getStructuredDocument());
        if (buffer != null) {
            IPath path = buffer.getLocation();
            if (path.segmentCount() > 1) {
                f = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
            }
            if (f != null && f.isAccessible()) {
                return f;
            }
        }
        return null;
    }

    /*
     * used by inner helper class (XMLJSPRegionHelper, JSPIncludeRegionHelper)
     */
    public List getBlockMarkers() {
        if (fBlockMarkers == null)
            fBlockMarkers = new ArrayList();
        return fBlockMarkers;
    }

    /**
     * the main control loop for translating the document, driven by the
     * structuredDocument nodes
     */
    public void translate() {
        if (fTagToVariableMap == null) {
            fTagToVariableMap = new StackMap();
        }
        fTranslationProblems.clear();

        setCurrentNode(new ZeroStructuredDocumentRegion(fStructuredDocument, 0));
        translatePreludes();

        setCurrentNode(fStructuredDocument.getFirstStructuredDocumentRegion());
        // GRAILS Start
        // keep track of the last comment in case we need to mark an error against it.
        int lastComment = 0;
        // GRAILS End

        while (getCurrentNode() != null && !isCanceled()) {
            //no code has been translated for this region yet
            fCodeTranslated = false;
            // intercept HTML comment flat node
            // also handles UNDEFINED (which is what CDATA comes in as)
            // basically this part will handle any "embedded" JSP containers
            if (getCurrentNode().getType() == DOMRegionContext.XML_COMMENT_TEXT || getCurrentNode().getType() == DOMRegionContext.XML_CDATA_TEXT || getCurrentNode().getType() == DOMRegionContext.UNDEFINED) {
            	// GRAILS Start
            	// keep track of the last comment in case we need to mark an error against it.
            	lastComment = getCurrentNode().getStartOffset();
            	// GRAILS End
                translateXMLCommentNode(getCurrentNode());
            }
            else {
                // iterate through each region in the flat node
                translateRegionContainer(getCurrentNode(), STANDARD_JSP);
            }
            
            //if no code was translated for this region then found "non translated code"
            if(!fCodeTranslated) {
                fFoundNonTranslatedCode = true;
            }
            
            if (getCurrentNode() != null)
                advanceNextNode();
        }
        
        writePlaceHolderForNonTranslatedCode();

        setCurrentNode(new ZeroStructuredDocumentRegion(fStructuredDocument, fStructuredDocument.getLength()));
        translateCodas();

        /*
         * Any contents left in the map indicate start tags that never had end
         * tags. While the '{' that is present without the matching '}' should
         * cause a Java translation fault, that's not particularly helpful to
         * a user who may only know how to use custom tags as tags. Ultimately
         * unbalanced custom tags should just be reported as unbalanced tags,
         * and unbalanced '{'/'}' only reported when the user actually
         * unbalanced them with scriptlets.
         */
        Iterator regionAndTaglibVariables = fTagToVariableMap.values().iterator();
        while (regionAndTaglibVariables.hasNext()) {
            RegionTags regionTag = (RegionTags) regionAndTaglibVariables.next();
            ITextRegionCollection extraStartRegion = regionTag.region;

            // GRAILS start
            // orig
//            IJSPProblem missingEndTag = createJSPProblem(IJSPProblem.EndCustomTagMissing, IJSPProblem.F_PROBLEM_ID_LITERAL, "", extraStartRegion.getStart(), extraStartRegion.getEnd());
            // make error message more comprehensible
            IJSPProblem missingEndTag = createJSPProblem(IJSPProblem.EndCustomTagMissing, IJSPProblem.F_PROBLEM_ID_LITERAL, "Comment not closed", lastComment, lastComment + extraStartRegion.getLength());
            // GRAILS End
            fTranslationProblems.add(missingEndTag);

            StringBuffer text = new StringBuffer();
            // Account for iteration tags that have a missing end tag
            if (regionTag.tag.isIterationTag())
                doAfterBody(text, regionTag);
            text.append("} // [</"); //$NON-NLS-1$
            text.append(regionTag.tag.getTagName());
            text.append(">]"); //$NON-NLS-1$
            appendToBuffer(text.toString(), fUserCode, false, fStructuredDocument.getLastStructuredDocumentRegion());
        }
        fTagToVariableMap.clear();

        /*
         * Now do the same for jsp:useBean tags, whose contents get their own
         * { & }
         */
        while (!fUseBeansStack.isEmpty()) {
            appendToBuffer("}", fUserCode, false, fStructuredDocument.getLastStructuredDocumentRegion());
            ITextRegionCollection extraStartRegion = (ITextRegionCollection) fUseBeansStack.pop();
            IJSPProblem missingEndTag = createJSPProblem(IJSPProblem.UseBeanEndTagMissing, IJSPProblem.F_PROBLEM_ID_LITERAL, "", extraStartRegion.getStartOffset(), extraStartRegion.getEndOffset());
            fTranslationProblems.add(missingEndTag);
        }
        
        // GRAILS change
        // now add the parameters from the associated action
        if (gspFile != null && gspFile.isAccessible()) {
            PerProjectControllerCache cache = GrailsCore.get().connect(gspFile.getProject(), PerProjectControllerCache.class);
            fGrailsActionParameters = cache.findReturnValuesAsDeclarations(gspFile.getParent().getName(), gspFile.getName());
        }
        // GRAILS end

        buildResult(true);
    }

    /**
     * Translates a region container (and XML JSP container, or <% JSP
     * container). This method should only be called in this class and for
     * containers in the primary structured document as all buffer appends
     * will be direct.
     */
    protected void setDocumentContent(IDocument document, InputStream contentStream, String charset) {
        Reader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(contentStream, charset), 2048);
            StringBuffer buffer = new StringBuffer(2048);
            char[] readBuffer = new char[2048];
            int n = in.read(readBuffer);
            while (n > 0) {
                buffer.append(readBuffer, 0, n);
                n = in.read(readBuffer);
            }
            document.set(buffer.toString());
        }
        catch (IOException x) {
            // ignore
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (IOException x) {
                    // ignore
                }
            }
        }
    }

    protected void init() {
        fClassname = "_JSPServlet"; //$NON-NLS-1$
        fClassHeader = "public class " + fClassname + " extends "; //$NON-NLS-1$ //$NON-NLS-2$
        
        fImplicitImports = "import javax.servlet.*;" + ENDL + //$NON-NLS-1$
                    "import javax.servlet.http.*;" + ENDL + //$NON-NLS-1$
                    "import javax.servlet.jsp.*;" + ENDL + ENDL; //$NON-NLS-1$

        fServiceHeader = "public void _jspService(javax.servlet.http.HttpServletRequest request," + //$NON-NLS-1$
                    " javax.servlet.http.HttpServletResponse response)" + ENDL + //$NON-NLS-1$
                    "\t\tthrows java.io.IOException, javax.servlet.ServletException {" + ENDL + //$NON-NLS-1$
                    "javax.servlet.jsp.PageContext pageContext = JspFactory.getDefaultFactory().getPageContext(this, request, response, null, true, JspWriter.DEFAULT_BUFFER, true);" + ENDL + //$NON-NLS-1$
                    "javax.servlet.ServletContext application = pageContext.getServletContext();" + ENDL + //$NON-NLS-1$
                    "javax.servlet.jsp.JspWriter out = pageContext.getOut();" + ENDL + //$NON-NLS-1$
                    "Object page = this;" + ENDL; //$NON-NLS-1$
        
        // GRAILS change
        // add grails-specific variables
        fServiceHeader += "java.util.Map flash = null;" + ENDL + //$NON-NLS-1$
                "java.util.Map params = null;" + ENDL; //$NON-NLS-1$
        
        fServiceHeader = 
                "/** Access a property from a Spring Bean. */ public String fieldValue(String bean, String field) { return null; } " + ENDL + //$NON-NLS-1$
                "/** Write a message to the output stream. */ public String message(String code) { return null; } " + ENDL + //$NON-NLS-1$
                fServiceHeader;
        
        
        fGrailsActionParameters = "";
        // GRAILS change end

        fSuperclass = "javax.servlet.http.HttpServlet"; //$NON-NLS-1$
    }

    /**
     * 
     * @return the status of the translator's progrss monitor, false if the
     *         monitor is null
     */
    private boolean isCanceled() {
        return (fProgressMonitor == null) ? false : fProgressMonitor.isCanceled();
    }

    private void advanceNextNode() {
        setCurrentNode(getCurrentNode().getNext());
        if (getCurrentNode() != null)
            setSourceReferencePoint();
    }

    private void setSourceReferencePoint() {
        if (isJSP(getCurrentNode().getFirstRegion().getType())) {
            Iterator it = getCurrentNode().getRegions().iterator();
            ITextRegion r = null;
            while (it.hasNext()) {
                r = (ITextRegion) it.next();
                if (r.getType() == DOMJSPRegionContexts.JSP_CONTENT || r.getType() == DOMRegionContext.XML_CONTENT)
                    break;
                else if (r.getType() == DOMJSPRegionContexts.JSP_DIRECTIVE_NAME)
                    break;
                else if (r.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE && getCurrentNode().getFullText(r).trim().equals("import")) //$NON-NLS-1$
                    break;
            }
        }
    }

    /**
     * translates a region container (and XML JSP container, or <% JSP
     * container)
     * 
     * This method should only be called in this class and for containers in
     * the primary structured document as all buffer appends will be direct
     */
    protected void translateRegionContainer(ITextRegionCollection container, int JSPType) {

        ITextRegionCollection containerRegion = container;

        Iterator regions = containerRegion.getRegions().iterator();
        ITextRegion region = null;
        while (regions.hasNext()) {
            region = (ITextRegion) regions.next();
            String type = region.getType();

            // content assist was not showing up in JSP inside a javascript region
            if (DOMRegionContext.BLOCK_TEXT == type) {
                // check if it's nested jsp in a script tag...
                if (region instanceof ITextRegionContainer) {
                    // pass in block text's container & iterator
                    Iterator regionIterator = ((ITextRegionCollection) region).getRegions().iterator();
                    translateJSPNode(region, regionIterator, type, EMBEDDED_JSP);
                }
                else {
                    //be sure to combine all of the text from the block region
                    StringBuffer fullText = new StringBuffer(containerRegion.getFullText(region));
                    while(regions.hasNext()) {
                        region = (ITextRegion)regions.next();
                        if (region instanceof ITextRegionContainer) {
                            // pass in block text's container & iterator
                            Iterator regionIterator = ((ITextRegionCollection) region).getRegions().iterator();
                            translateJSPNode(region, regionIterator, type, EMBEDDED_JSP);
                        }
                        
                        if(region.getType() == DOMRegionContext.BLOCK_TEXT) {
                            fullText.append(containerRegion.getFullText(region));
                        } else {
                            //update type for when we exit if statement for BLOCK_TEXT
                            type = region.getType();
                            break;
                        }
                    }
                    
                    /**
                     * LIMITATION - Normally the script content within a
                     * script tag is a single document region with a single
                     * BLOCK_TEXT text region within it. Any JSP scripting
                     * will be within its own region container (for the sake
                     * of keeping the scripting open/content/end as a group)
                     * also of BLOCK_TEXT. That ignores custom tags that might
                     * be in there, though, as they require proper scoping and
                     * variable declaration to be performed even though
                     * they're not proper nodes in the DOM. The only way to
                     * really do this is to treat the entire script content as
                     * JSP content on its own, akin to an included segment.
                     * Further complicating this solution is that tagdependent
                     * custom tags have their comment marked as BLOCK_TEXT as
                     * well, so there's no clear way to tell the two cases
                     * apart.
                     */

                    // ////////////////////////////////////////////////////////////////////////////////
                    // THIS EMBEDDED JSP TEXT WILL COME OUT LATER WHEN
                    // PARTITIONING HAS
                    // SUPPORT FOR NESTED XML-JSP
                    // CMVC 241882
                    decodeScriptBlock(fullText.toString(), containerRegion.getStartOffset());
                    // ////////////////////////////////////////////////////////////////////////////////
                }
            }
            // if (region instanceof ITextRegionCollection &&
            // ((ITextRegionCollection) region).getNumberOfRegions() > 0) {
            // translateRegionContainer((ITextRegionCollection) region,
            // EMBEDDED_JSP);
            // }
            if (type != null && isJSP(type)) // <%, <%=, <%!, <%@
            {
                // translateJSPNode(region, regions, type, JSPType);
                translateJSPNode(containerRegion, regions, type, JSPType);
            }
            else if (type != null && (type == DOMRegionContext.XML_TAG_OPEN || type == DOMRegionContext.XML_END_TAG_OPEN)) {
                translateXMLNode(containerRegion, regions);
            }
            else if(type != null && type == DOMRegionContext.XML_CONTENT && region instanceof ITextRegionContainer) {
                //this case was put in to parse EL that is not in an attribute
                translateXMLContent((ITextRegionContainer)region);
            }
            //the end tags of these regions are "translated" in a sense
            else if(type == DOMJSPRegionContexts.JSP_DIRECTIVE_CLOSE ||
                    type == DOMJSPRegionContexts.JSP_CLOSE) {
                this.fCodeTranslated = true;
            }
        }
    }

    /*
     * ////////////////////////////////////////////////////////////////////////////////// **
     * TEMP WORKAROUND FOR CMVC 241882 Takes a String and blocks out
     * jsp:scriptlet, jsp:expression, and jsp:declaration @param blockText
     * @return
     */
    void decodeScriptBlock(String blockText, int startOfBlock) {
        XMLJSPRegionHelper helper = new XMLJSPRegionHelper(this, false);
        helper.addBlockMarker(new BlockMarker("jsp:scriptlet", null, DOMJSPRegionContexts.JSP_CONTENT, false)); //$NON-NLS-1$
        helper.addBlockMarker(new BlockMarker("jsp:expression", null, DOMJSPRegionContexts.JSP_CONTENT, false)); //$NON-NLS-1$
        helper.addBlockMarker(new BlockMarker("jsp:declaration", null, DOMJSPRegionContexts.JSP_CONTENT, false)); //$NON-NLS-1$
        helper.addBlockMarker(new BlockMarker("jsp:directive.include", null, DOMJSPRegionContexts.JSP_CONTENT, false)); //$NON-NLS-1$
        helper.addBlockMarker(new BlockMarker("jsp:directive.taglib", null, DOMJSPRegionContexts.JSP_CONTENT, false)); //$NON-NLS-1$
        helper.reset(blockText, startOfBlock);
        // force parse
        helper.forceParse();
    }

    /*
     * returns string minus CDATA open and close text
     */
    final public String stripCDATA(String text) {
        String resultText = ""; //$NON-NLS-1$
        String CDATA_OPEN = "<![CDATA["; //$NON-NLS-1$
        String CDATA_CLOSE = "]]>"; //$NON-NLS-1$
        int start = 0;
        int end = text.length();
        while (start < text.length()) {
            if (text.indexOf(CDATA_OPEN, start) > -1) {
                end = text.indexOf(CDATA_OPEN, start);
                resultText += text.substring(start, end);
                start = end + CDATA_OPEN.length();
            }
            else if (text.indexOf(CDATA_CLOSE, start) > -1) {
                end = text.indexOf(CDATA_CLOSE, start);
                resultText += text.substring(start, end);
                start = end + CDATA_CLOSE.length();
            }
            else {
                end = text.length();
                resultText += text.substring(start, end);
                break;
            }
        }
        return resultText;
    }

    // END OF WORKAROUND CODE...
    // /////////////////////////////////////////////////////////////////////////////////////
    /**
     * determines if the type is a pure JSP type (not XML)
     */
    protected boolean isJSP(String type) {
        return ((type == DOMJSPRegionContexts.JSP_DIRECTIVE_OPEN || type == DOMJSPRegionContexts.JSP_EXPRESSION_OPEN || type == DOMJSPRegionContexts.JSP_DECLARATION_OPEN || type == DOMJSPRegionContexts.JSP_SCRIPTLET_OPEN || type == DOMJSPRegionContexts.JSP_CONTENT || type == DOMJSPRegionContexts.JSP_EL_OPEN) && type != DOMRegionContext.XML_TAG_OPEN);
        // checking XML_TAG_OPEN so <jsp:directive.xxx/> gets treated like
        // other XML jsp tags
    }

    /**
     * This currently only detects EL content and translates it,
     * but if other cases arise later then they could be added in here
     * 
     * @param embeddedContainer the container that may contain EL
     */
    protected void translateXMLContent(ITextRegionContainer embeddedContainer) {
        ITextRegionList embeddedRegions = embeddedContainer.getRegions();
        int length = embeddedRegions.size();
        for (int i = 0; i < length; i++) {
            ITextRegion delim = embeddedRegions.get(i);
            String type = delim.getType();

            // check next region to see if it's EL content
            if (i + 1 < length) {
                if((type == DOMJSPRegionContexts.JSP_EL_OPEN || type == DOMJSPRegionContexts.JSP_VBL_OPEN)) {
                    ITextRegion region = null;
                    
                    int start = delim.getEnd();
                    while (++i < length) {
                        region = embeddedRegions.get(i);
                        if (region == null || !isELType(region.getType()))
                            break;
                    }
                    fLastJSPType = EXPRESSION;
                    String elText = embeddedContainer.getFullText().substring(start, (region != null ? region.getStart() : embeddedContainer.getLength() - 1));
                    translateEL(elText, embeddedContainer.getText(delim), fCurrentNode,
                            embeddedContainer.getEndOffset(delim), elText.length());
                }
            }
        }
    }

    private boolean isELType(String type) {
        return DOMJSPRegionContexts.JSP_EL_CONTENT.equals(type) || DOMJSPRegionContexts.JSP_EL_DQUOTE.equals(type) || DOMJSPRegionContexts.JSP_EL_QUOTED_CONTENT.equals(type) || DOMJSPRegionContexts.JSP_EL_SQUOTE.equals(type);
    }

    /**
     * translates the various XMLJSP type nodes
     * 
     * @param regions
     *            the regions of the XMLNode
     */
    protected void translateXMLNode(ITextRegionCollection container, Iterator regions) {
        // contents must be valid XHTML, translate escaped CDATA into what it
        // really is...
        ITextRegion r = null;
        if (regions.hasNext()) {
            r = (ITextRegion) regions.next();
            // <jsp:directive.xxx > comes in as this
            if (r.getType() == DOMRegionContext.XML_TAG_NAME || r.getType() == DOMJSPRegionContexts.JSP_DIRECTIVE_NAME)

            {
                String fullTagName = container.getText(r);
                if (fullTagName.indexOf(':') > -1 && !fullTagName.startsWith(JSP_PREFIX)) {
                    addTaglibVariables(fullTagName, container); // it
                }
                StringTokenizer st = new StringTokenizer(fullTagName, ":.", false); //$NON-NLS-1$
                if (st.hasMoreTokens() && st.nextToken().equals("jsp")) //$NON-NLS-1$
                {
                    if (st.hasMoreTokens()) {
                        String jspTagName = st.nextToken();

                        if (jspTagName.equals("scriptlet")) //$NON-NLS-1$
                        {
                            translateXMLJSPContent(SCRIPTLET);
                        }
                        else if (jspTagName.equals("expression")) //$NON-NLS-1$
                        {
                            translateXMLJSPContent(EXPRESSION);
                        }
                        else if (jspTagName.equals("declaration")) //$NON-NLS-1$
                        {
                            translateXMLJSPContent(DECLARATION);
                        }
                        else if (jspTagName.equals("directive")) //$NON-NLS-1$
                        {
                            if (st.hasMoreTokens()) {
                                String directiveName = st.nextToken();
                                if (directiveName.equals("taglib")) { //$NON-NLS-1$
                                    while (r != null && regions.hasNext() && !r.getType().equals(DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)) {
                                        r = (ITextRegion) regions.next();
                                        if (container.getText(r).equals(JSP11Namespace.ATTR_NAME_PREFIX)) {
                                            String prefix = getAttributeValue(r, regions);
                                            if (prefix != null) {
                                                handleTaglib(prefix);
                                            }
                                        }
                                    }
                                    return;
                                }
                                else if (directiveName.equals("include")) { //$NON-NLS-1$

                                    String fileLocation = ""; //$NON-NLS-1$

                                    // skip to required "file" attribute,
                                    // should be safe because
                                    // "file" is the only attribute for the
                                    // include directive
                                    while (r != null && regions.hasNext() && !r.getType().equals(DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)) {
                                        r = (ITextRegion) regions.next();
                                    }
                                    fileLocation = getAttributeValue(r, regions);
                                    if (fileLocation != null)
                                        handleIncludeFile(fileLocation);
                                }
                                else if (directiveName.equals("page")) { //$NON-NLS-1$

                                    // bad if currentNode is referenced after
                                    // here w/ the current list
                                    // see:
                                    // https://w3.opensource.ibm.com/bugzilla/show_bug.cgi?id=3035
                                    // setCurrentNode(getCurrentNode().getNext());
                                    if (getCurrentNode() != null) {
                                        // 'regions' contain the attrs
                                        translatePageDirectiveAttributes(regions, getCurrentNode());
                                    }
                                }
                                else if (directiveName.equals("tag")) { //$NON-NLS-1$
                                    translatePageDirectiveAttributes(regions, getCurrentNode());
                                }
                                else if (directiveName.equals("variable")) { //$NON-NLS-1$
                                    translateVariableDirectiveAttributes(regions);
                                }
                            }
                        }
                        else if (jspTagName.equals("include")) { //$NON-NLS-1$
                            // <jsp:include page="filename") />
                            checkAttributeValueContainer(regions, "page"); //$NON-NLS-1$
                        }
                        else if (jspTagName.equals("forward")) { //$NON-NLS-1$
                            checkAttributeValueContainer(regions, "page"); //$NON-NLS-1$
                        }
                        else if (jspTagName.equals("param")) { //$NON-NLS-1$
                            checkAttributeValueContainer(regions, "value"); //$NON-NLS-1$
                        }
                        else if (jspTagName.equals("setProperty")) { //$NON-NLS-1$
                            checkAttributeValueContainer(regions, "value"); //$NON-NLS-1$
                        }
                        else if (jspTagName.equals("useBean")) //$NON-NLS-1$
                        {
                            checkAttributeValueContainer(regions, "name"); //$NON-NLS-1$
                            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=103004
                            // advanceNextNode(); // get the content
                            if (getCurrentNode() != null) {
                                translateUseBean(container); // 'regions'
                            }
                        }

                    }
                }
                else {
                    checkAllAttributeValueContainers(regions);
                }
            }
        }
    }

    /**
     * translates embedded containers for ALL attribute values
     * 
     * @param regions
     */
    private void checkAllAttributeValueContainers(Iterator regions) {
        // tag name is not jsp
        // handle embedded jsp attributes...
        ITextRegion embedded = null;
        // Iterator attrRegions = null;
        // ITextRegion attrChunk = null;
        while (regions.hasNext()) {
            embedded = (ITextRegion) regions.next();
            if (embedded instanceof ITextRegionContainer) {
                // parse out container

                // https://bugs.eclipse.org/bugs/show_bug.cgi?id=130606
                // fix exponential iteration problem w/ embedded expressions
                translateEmbeddedJSPInAttribute((ITextRegionContainer) embedded);
                // attrRegions = ((ITextRegionContainer)
                // embedded).getRegions().iterator();
                // while (attrRegions.hasNext()) {
                // attrChunk = (ITextRegion) attrRegions.next();
                // String type = attrChunk.getType();
                // // embedded JSP in attribute support only want to
                // // translate one time per
                // // embedded region so we only translate on the JSP open
                // // tags (not content)
                // if (type == DOMJSPRegionContexts.JSP_EXPRESSION_OPEN ||
                // type ==
                // DOMJSPRegionContexts.JSP_SCRIPTLET_OPEN || type ==
                // DOMJSPRegionContexts.JSP_DECLARATION_OPEN || type ==
                // DOMJSPRegionContexts.JSP_DIRECTIVE_OPEN || type ==
                // DOMJSPRegionContexts.JSP_EL_OPEN) {
                // // now call jsptranslate
                // translateEmbeddedJSPInAttribute((ITextRegionContainer)
                // embedded);
                // break;
                // }
                // }
            }
        }
    }

    /**
     * translates embedded container for specified attribute
     * 
     * @param regions
     * @param attrName
     */
    private void checkAttributeValueContainer(Iterator regions, String attrName) {
        ITextRegion r = null;
        while (regions.hasNext()) {
            r = (ITextRegion) regions.next();
            if (r.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME && getCurrentNode().getText(r).equals(attrName)) { //$NON-NLS-1$
                // skip to attribute value
                while (regions.hasNext() && (r = (ITextRegion) regions.next()) != null) {
                    if (r.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE)
                        break;
                }
                // forces embedded region to be translated
                if (r instanceof ContextRegionContainer) {
                    translateEmbeddedJSPInAttribute((ContextRegionContainer) r);
                }
                break;
            }
        }
    }

    /*
     * example:
     * 
     * <jsp:scriptlet>scriptlet jsp-java content <![CDATA[ more jsp java ]]>
     * jsp-java content... <![CDATA[ more jsp java ]]> </jsp:scriptlet>
     * 
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=93366
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=88590 translate
     * everything inbetween <scriptlet> tags, which may be more than one
     * region (esp. CDATA)
     * 
     */
    private void translateXMLJSPContent(int type) {

        IStructuredDocumentRegion sdr = getCurrentNode().getNext();
        int start = sdr.getStartOffset();
        int end = sdr.getEndOffset();
        String sdrText = ""; //$NON-NLS-1$

        StringBuffer regionText = new StringBuffer();
        // read structured document regions until </jsp:scriptlet> or EOF
        while (sdr != null && sdr.getType() != DOMRegionContext.XML_TAG_NAME) {

            // setup for next region
            if (regionText.length() == 0)
                start = sdr.getStartOffset();
            sdrText = sdr.getText();

            if (sdr.getType() == DOMRegionContext.XML_CDATA_TEXT) {
                // Clear out the buffer
                if (regionText.length() > 0) {
                    writeToBuffer(type, regionText.toString(), start, end);
                    regionText = new StringBuffer();
                }
                // just to be safe, make sure CDATA start & end are there
                if (sdrText.startsWith("<![CDATA[") && sdrText.endsWith("]]>")) { //$NON-NLS-1$ //$NON-NLS-2$

                    start = sdr.getStartOffset() + 9; // <![CDATA[
                    end = sdr.getEndOffset() - 3; // ]]>
                    sdrText = sdrText.substring(9, sdrText.length() - 3);
                    writeToBuffer(type, sdrText, start, end);
                }
            }
            else {
                // handle entity references
                regionText.append(EscapedTextUtil.getUnescapedText(sdrText));
                end = sdr.getEndOffset();
            }
            sdr = sdr.getNext();
        }

        if (regionText.length() > 0)
            writeToBuffer(type, regionText.toString(), start, end);
        setCurrentNode(sdr);
        setSourceReferencePoint();
    }

    private void writeToBuffer(int type, String content, int jspStart, int jspEnd) {
        switch (type) {
            case SCRIPTLET :
                translateScriptletString(content, getCurrentNode(), jspStart, jspEnd - jspStart, false);
                break;
            case EXPRESSION :
                translateExpressionString(content, getCurrentNode(), jspStart, jspEnd - jspStart, false);
                break;
            case DECLARATION :
                translateDeclarationString(content, getCurrentNode(), jspStart, jspEnd - jspStart, false);
                break;
        }
    }

    /**
     * goes through comment regions, checks if any are an embedded JSP
     * container if it finds one, it's sends the container into the
     * translation routine
     */
    protected void translateXMLCommentNode(IStructuredDocumentRegion node) {
        Iterator it = node.getRegions().iterator();
        ITextRegion commentRegion = null;
        while (it != null && it.hasNext()) {
            commentRegion = (ITextRegion) it.next();
            if (commentRegion instanceof ITextRegionContainer) {
                translateRegionContainer((ITextRegionContainer) commentRegion, EMBEDDED_JSP); // it's
                // embedded
                // jsp...iterate
                // regions...
            }
            else if (DOMRegionContext.XML_COMMENT_TEXT.equals(commentRegion.getType())) {
                // https://bugs.eclipse.org/bugs/show_bug.cgi?id=222215
                // support custom tags hidden in a comment region
                decodeScriptBlock(node.getFullText(commentRegion), node.getStartOffset(commentRegion));
            }
        }
    }

    /**
     * determines which type of JSP node to translate
     */
    protected void translateJSPNode(ITextRegion region, Iterator regions, String type, int JSPType) {
        if (type == DOMJSPRegionContexts.JSP_DIRECTIVE_OPEN && regions != null) {
            translateDirective(regions);
        }
        else {
            ITextRegionCollection contentRegion = null;
            if (JSPType == STANDARD_JSP && (setCurrentNode(getCurrentNode().getNext())) != null) {
                contentRegion = getCurrentNode();
            }
            else if (JSPType == EMBEDDED_JSP && region instanceof ITextRegionCollection) {
                translateEmbeddedJSPInBlock((ITextRegionCollection) region, regions);
                // ensure the rest of this method won't be called
            }
            /* NOTE: the type here is of the node preceding the current node
             * thus must check to see if the current node is JSP close, if it is
             * then the JSP is something akin to <%%> and should not be translated
             * (Bug 189318)
             */
            if (contentRegion != null && contentRegion.getType() != DOMJSPRegionContexts.JSP_CLOSE) {
                if (type == DOMJSPRegionContexts.JSP_EXPRESSION_OPEN) {
                    translateExpression(contentRegion);
                }
                else if (type == DOMJSPRegionContexts.JSP_DECLARATION_OPEN) {
                    translateDeclaration(contentRegion);
                }
                else if (type == DOMJSPRegionContexts.JSP_CONTENT || type == DOMJSPRegionContexts.JSP_SCRIPTLET_OPEN) {
                    translateScriptlet(contentRegion);
                }
            }
            else {
                // this is the case of an attribute w/ no region <p
                // align="<%%>">
                setCursorOwner(getJSPTypeForRegion(region));
            }
        }
    }


    private void translateEL(String elText, String delim, IStructuredDocumentRegion currentNode, int contentStart, int contentLength) {
        IJSPELTranslator translator = getELTranslator();
        if (null != translator) {
            List elProblems = translator.translateEL(elText, delim, currentNode, contentStart, contentLength, fUserELExpressions, fUserELRanges, fStructuredDocument);
            fTranslationProblems.addAll(elProblems);
        }
    }

    /**
     * Discover and instantiate an EL translator.
     */
    public IJSPELTranslator getELTranslator() {
        if (fELTranslator == null) {

            /*
             * name of plugin that exposes this extension point
             */
            IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(JSP_CORE_PLUGIN_ID, EL_TRANSLATOR_EXTENSION_NAME); // -
            // extension
            // id

            // Iterate over all declared extensions of this extension point.
            // A single plugin may extend the extension point more than once,
            // although it's not recommended.
            IConfigurationElement bestTranslator = null;
            IExtension[] extensions = extensionPoint.getExtensions();
            for (int curExtension = 0; curExtension < extensions.length; curExtension++) {
                IExtension extension = extensions[curExtension];

                IConfigurationElement[] translators = extension.getConfigurationElements();
                for (int curTranslator = 0; curTranslator < translators.length; curTranslator++) {

                    IConfigurationElement elTranslator = translators[curTranslator];

                    if (!EL_TRANSLATOR_EXTENSION_NAME.equals(elTranslator.getName())) { // -
                        // name
                        // of
                        // configElement
                        continue;
                    }

                    String idString = elTranslator.getAttribute("id"); //$NON-NLS-1$
                    if (null != idString && idString.equals(fELTranslatorID) || (null == bestTranslator && DEFAULT_JSP_EL_TRANSLATOR_ID.equals(idString))) {
                        bestTranslator = elTranslator;
                    }
                }
            }

            if (null != bestTranslator) {
                try {
                    Object execExt = bestTranslator.createExecutableExtension("class"); //$NON-NLS-1$
                    if (execExt instanceof IJSPELTranslator) {
                        return fELTranslator = (IJSPELTranslator) execExt;
                    }
                }
                catch (CoreException e) {
                    Logger.logException(e);
                }
            }
        }
        return fELTranslator;
    }

    /**
     * Pass the ITextRegionCollection which is the embedded region
     * 
     * @param regions
     *            iterator for collection
     */
    private void translateEmbeddedJSPInBlock(ITextRegionCollection collection, Iterator regions) {
        ITextRegion region = null;
        while (regions.hasNext()) {
            region = (ITextRegion) regions.next();
            if (isJSP(region.getType()))
                break;
            region = null;
        }
        if (region != null) {
            translateEmbeddedJSPInAttribute(collection);
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=126377
            // all of collection was translated so just finish off iterator
            while (regions.hasNext())
                regions.next();
        }
    }

    /*
     * Translates all embedded jsp regions in embeddedContainer for example:
     * <a href="index.jsp?p=<%=abc%>b=<%=xyz%>">abc</a>
     */
    private void translateEmbeddedJSPInAttribute(ITextRegionCollection embeddedContainer) {
        // THIS METHOD IS A FIX FOR
        // jsp embedded in attribute regions
        // loop all regions
        ITextRegionList embeddedRegions = embeddedContainer.getRegions();
        ITextRegion delim = null;
        ITextRegion content = null;
        String type = null;
        String quotetype = null;
        for (int i = 0; i < embeddedRegions.size(); i++) {

            // possible delimiter, check later
            delim = embeddedRegions.get(i);
            type = delim.getType();
            if (type == DOMRegionContext.XML_TAG_NAME ) {
                String fullTagName = embeddedContainer.getText(delim);
                if (fullTagName.indexOf(':') > -1 && !fullTagName.startsWith(JSP_PREFIX)) {
                    ITextRegion prevRegion =null;
                    if (i>0)
                        prevRegion = embeddedRegions.get(i-1);
                    addCustomTaglibVariables(fullTagName, embeddedContainer,prevRegion); // it may be a custom tag
                }
            }
            if(type == DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE || type == DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_SQUOTE
                || type == DOMJSPRegionContexts.JSP_TAG_ATTRIBUTE_VALUE_DQUOTE || type == DOMJSPRegionContexts.JSP_TAG_ATTRIBUTE_VALUE_SQUOTE)
                quotetype = type;

            // check next region to see if it's content
            if (i + 1 < embeddedRegions.size()) {
                String regionType = embeddedRegions.get(i + 1).getType();
                if (regionType == DOMJSPRegionContexts.JSP_CONTENT || regionType == DOMJSPRegionContexts.JSP_EL_CONTENT)
                    content = embeddedRegions.get(i + 1);
            }

            if (content != null) {
                int contentStart = embeddedContainer.getStartOffset(content);
                int rStart = fCurrentNode.getStartOffset() + contentStart;
                int rEnd = fCurrentNode.getStartOffset() + embeddedContainer.getEndOffset(content);

                boolean inThisRegion = rStart <= fSourcePosition && rEnd >= fSourcePosition;
                // int jspPositionStart = fCurrentNode.getStartOffset() +
                // contentStart;

                if (type == DOMJSPRegionContexts.JSP_EXPRESSION_OPEN) {
                    fLastJSPType = EXPRESSION;
                    // translateExpressionString(embeddedContainer.getText(content),
                    // fCurrentNode, contentStart, content.getLength());
                    translateExpressionString(embeddedContainer.getText(content), embeddedContainer, contentStart, content.getLength(), quotetype);
                }
                else if (type == DOMJSPRegionContexts.JSP_SCRIPTLET_OPEN) {
                    fLastJSPType = SCRIPTLET;
                    // translateScriptletString(embeddedContainer.getText(content),
                    // fCurrentNode, contentStart, content.getLength());
                    translateScriptletString(embeddedContainer.getText(content), embeddedContainer, contentStart, content.getLength(), false);
                }
                else if (type == DOMJSPRegionContexts.JSP_DECLARATION_OPEN) {
                    fLastJSPType = DECLARATION;
                    // translateDeclarationString(embeddedContainer.getText(content),
                    // fCurrentNode, contentStart, content.getLength());
                    translateDeclarationString(embeddedContainer.getText(content), embeddedContainer, contentStart, content.getLength(), false);
                }
                else if (type == DOMJSPRegionContexts.JSP_EL_OPEN) {
                    fLastJSPType = EXPRESSION;
                    translateEL(embeddedContainer.getText(content), embeddedContainer.getText(delim), fCurrentNode, contentStart, content.getLength());
                }

                // calculate relative offset in buffer
                if (inThisRegion) {
                    setCursorOwner(fLastJSPType);
                    int currentBufferLength = getCursorOwner().length();
                    setRelativeOffset((fSourcePosition - contentStart) + currentBufferLength);
                    if (fLastJSPType == EXPRESSION) {
                        // if an expression, add then length of the enclosing
                        // paren..
                        setCursorInExpression(true);
                        setRelativeOffset(getRelativeOffset() + EXPRESSION_PREFIX.length());
                    }
                }
            }
            else {
                type = null;
            }
        }
    }

    private int fLastJSPType = SCRIPTLET;

    /**
     * JSPType is only used internally in this class to describe tye type of
     * region to be translated
     * 
     * @param region
     * @return int
     */
    private int getJSPTypeForRegion(ITextRegion region) {
        String regionType = region.getType();
        int type = SCRIPTLET;
        if (regionType == DOMJSPRegionContexts.JSP_SCRIPTLET_OPEN)
            type = SCRIPTLET;
        else if (regionType == DOMJSPRegionContexts.JSP_EXPRESSION_OPEN)
            type = EXPRESSION;
        else if (regionType == DOMJSPRegionContexts.JSP_DECLARATION_OPEN)
            type = DECLARATION;
        else if (regionType == DOMJSPRegionContexts.JSP_CONTENT)
            type = fLastJSPType;
        // remember the last type, in case the next type that comes in is
        // JSP_CONTENT
        fLastJSPType = type;
        return type;
    }

    /**
     * /* <%@ %> /* need to pass in the directive tag region
     */
    protected void translateDirective(Iterator regions) {
        ITextRegion r = null;
        String regionText, attrValue = ""; //$NON-NLS-1$
        while (regions.hasNext() && (r = (ITextRegion) regions.next()) != null && r.getType() == DOMJSPRegionContexts.JSP_DIRECTIVE_NAME) { // could
            // be
            // XML_CONTENT
            // =
            // "",
            // skips
            // attrs?
            regionText = getCurrentNode().getText(r);
            if (regionText.equals("taglib")) { //$NON-NLS-1$
                // add custom tag block markers here
                handleTaglib();
                return;
            }
            else if (regionText.equals("include")) { //$NON-NLS-1$
                String fileLocation = ""; //$NON-NLS-1$
                // CMVC 258311
                // PMR 18368, B663
                // skip to required "file" attribute, should be safe because
                // "file" is the only attribute for the include directive
                while (r != null && regions.hasNext() && !r.getType().equals(DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)) {
                    r = (ITextRegion) regions.next();
                }
                fileLocation = getAttributeValue(r, regions);
                if (attrValue != null)
                    handleIncludeFile(fileLocation);
            }
            else if (regionText.equals("page")) { //$NON-NLS-1$
                translatePageDirectiveAttributes(regions, getCurrentNode());
            }
            else if (regionText.equals("tag")) { //$NON-NLS-1$
                // some attributes overlap, so both are handled in this method
                translatePageDirectiveAttributes(regions, getCurrentNode());
            }
            else if (regionText.equals("variable")) { //$NON-NLS-1$
                translateVariableDirectiveAttributes(regions);
            }
            else if (regionText.equals("attribute")) { //$NON-NLS-1$
                translateAttributeDirectiveAttributes(regions);
            }
        }
    }

    private void translateAttributeDirectiveAttributes(Iterator regions) {
        ITextRegion r = null;
        String attrName, attrValue;

        String varType = "java.lang.String"; //$NON-NLS-1$ // the default class...
        String varName = null;
        String description = "";//$NON-NLS-1$ 
        boolean isFragment = false;

        // iterate all attributes
        while (regions.hasNext() && (r = (ITextRegion) regions.next()) != null && r.getType() != DOMJSPRegionContexts.JSP_CLOSE) {
            attrName = attrValue = null;
            if (r.getType().equals(DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)) {
                attrName = getCurrentNode().getText(r).trim();
                if (attrName.length() > 0) {
                    if (regions.hasNext() && (r = (ITextRegion) regions.next()) != null && r.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS) {
                        if (regions.hasNext() && (r = (ITextRegion) regions.next()) != null && r.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {
                            attrValue = StringUtils.strip(getCurrentNode().getText(r));
                        }
                        // has equals, but no value?
                    }
                    if (attrName.equals(JSP11Namespace.ATTR_NAME_TYPE)) {
                        varType = attrValue;
                    }
                    else if (attrName.equals(JSP20Namespace.ATTR_NAME_FRAGMENT)) {
                        isFragment = Boolean.valueOf(attrValue).booleanValue();
                    }
                    else if (attrName.equals(JSP11Namespace.ATTR_NAME_NAME)) {
                        varName = attrValue;
                    }
                    else if (attrName.equals(JSP20Namespace.ATTR_NAME_DESCRIPTION)) {
                        description = attrValue;
                    }
                }
            }
        }
        if (varName != null) {
            if (isFragment) {
                // 2.0:JSP.8.5.2
                varType = "javax.servlet.jsp.tagext.JspFragment";
            }
            String declaration = new TaglibVariable(varType, varName, "", description).getDeclarationString(true, TaglibVariable.M_PRIVATE);
            appendToBuffer(declaration, fUserDeclarations, false, fCurrentNode);
        }
    }

    private void translateVariableDirectiveAttributes(Iterator regions) {
        /*
         * Shouldn't create a scripting variable in *this* tag file's
         * translation, only in JSP files that use it -
         * https://bugs.eclipse.org/bugs/show_bug.cgi?id=188780
         */
    }

    /*
     * This method should ideally only be called once per run through
     * JSPTranslator This is intended for use by inner helper classes that
     * need to add block markers to their own parsers. This method only adds
     * markers that came from <@taglib> directives, (not <@include>), since
     * include file taglibs are handled on the fly when they are encountered. *
     * @param regions
     * 
     * @deprecated - does not properly handle prefixes
     */
    protected void handleTaglib() {
        // get/create TLDCMDocument
        TLDCMDocumentManager mgr = TaglibController.getTLDCMDocumentManager(fStructuredDocument);
        if (mgr != null) {
            List trackers = mgr.getCMDocumentTrackers(getCurrentNode().getEnd());
            Iterator it = trackers.iterator();
            CMDocumentTracker tracker = null;
            Iterator taglibRegions = null;
            IStructuredDocumentRegion sdRegion = null;
            ITextRegion r = null;
            while (it.hasNext()) {
                tracker = (CMDocumentTracker) it.next();
                sdRegion = tracker.getStructuredDocumentRegion();
                // since may be call from another thread (like a background
                // job)
                // this check is to be safer
                if (sdRegion != null && !sdRegion.isDeleted()) {
                    taglibRegions = sdRegion.getRegions().iterator();
                    while (!sdRegion.isDeleted() && taglibRegions.hasNext()) {
                        r = (ITextRegion) taglibRegions.next();
                        if (r.getType().equals(DOMJSPRegionContexts.JSP_DIRECTIVE_NAME)) {
                            String text = sdRegion.getText(r);
                            if (JSP12TLDNames.TAGLIB.equals(text) || JSP12Namespace.ElementName.DIRECTIVE_TAGLIB.equals(text)) {
                                addBlockMarkers(tracker.getDocument());
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * This method should ideally only be called once per run through
     * JSPTranslator This is intended for use by inner helper classes that
     * need to add block markers to their own parsers. This method only adds
     * markers that came from <@taglib> directives, (not <@include>), since
     * include file taglibs are handled on the fly when they are encountered. *
     * @param regions
     */
    private void handleTaglib(String prefix) {
        // get/create TLDCMDocument
        TLDCMDocumentManager mgr = TaglibController.getTLDCMDocumentManager(fStructuredDocument);
        if (mgr != null) {
            // get trackers for the CMDocuments enabled at this offset
            List trackers = mgr.getCMDocumentTrackers(getCurrentNode().getEnd());
            Iterator it = trackers.iterator();
            CMDocumentTracker tracker = null;
            while (it.hasNext()) {
                tracker = (CMDocumentTracker) it.next();
                addBlockMarkers(prefix + ":", tracker.getDocument());
            }
        }
    }

    /*
     * adds block markers to JSPTranslator's block marker list for all
     * elements in doc @param doc
     */
    protected void addBlockMarkers(CMDocument doc) {
        if (doc.getElements().getLength() > 0) {
            Iterator elements = doc.getElements().iterator();
            CMNode node = null;
            while (elements.hasNext()) {
                node = (CMNode) elements.next();
                getBlockMarkers().add(new BlockMarker(node.getNodeName(), null, DOMJSPRegionContexts.JSP_CONTENT, true));
            }
        }
    }

    /*
     * adds block markers to JSPTranslator's block marker list for all
     * elements in doc @param doc
     */
    protected void addBlockMarkers(String prefix, CMDocument doc) {
        if (doc.getElements().getLength() > 0) {
            Iterator elements = doc.getElements().iterator();
            CMNode node = null;
            while (elements.hasNext()) {
                node = (CMNode) elements.next();
                if (node instanceof TLDElementDeclaration && ((TLDElementDeclaration) node).getBodycontent().equals(JSP12TLDNames.CONTENT_TAGDEPENDENT))
                    getBlockMarkers().add(new BlockMarker(prefix + node.getNodeName(), null, DOMRegionContext.BLOCK_TEXT, true));
                else
                    getBlockMarkers().add(new BlockMarker(prefix + node.getNodeName(), null, DOMJSPRegionContexts.JSP_CONTENT, true));
            }
        }
    }

    /**
     * If r is an attribute name region, this method will safely return the
     * value for that attribute.
     * 
     * @param r
     * @param remainingRegions
     * @return the value for the attribute name (r), or null if isn't one
     */
    protected String getAttributeValue(ITextRegion r, Iterator remainingRegions) {
        if (r.getType().equals(DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)) {
            if (remainingRegions.hasNext() && (r = (ITextRegion) remainingRegions.next()) != null && r.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS) {
                if (remainingRegions.hasNext() && (r = (ITextRegion) remainingRegions.next()) != null && r.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {
                    return StringUtils.stripQuotes(getCurrentNode().getText(r));
                }
            }
        }
        return null;
    }

    /**
     * takes an iterator of the attributes of a page directive and the
     * directive itself. The iterator is used in case it can be optimized, but
     * the documentRegion is still required to ensure that the values are
     * extracted from the correct text.
     */
    protected void translatePageDirectiveAttributes(Iterator regions, IStructuredDocumentRegion documentRegion) {
        ITextRegion r = null;
        String attrName, attrValue;
        // iterate all attributes
        while (regions.hasNext() && (r = (ITextRegion) regions.next()) != null && r.getType() != DOMJSPRegionContexts.JSP_CLOSE) {
            attrName = attrValue = null;
            if (r.getType().equals(DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)) {

                attrName = documentRegion.getText(r).trim();
                if (attrName.length() > 0) {
                    if (regions.hasNext() && (r = (ITextRegion) regions.next()) != null && r.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS) {
                        if (regions.hasNext() && (r = (ITextRegion) regions.next()) != null && r.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {

                            attrValue = StringUtils.strip(documentRegion.getText(r));
                        }
                        // has equals, but no value?
                    }
                    setDirectiveAttribute(attrName, attrValue);
                }
            }
        }
    }

    /**
     * sets the appropriate page/tag directive attribute
     */
    protected void setDirectiveAttribute(String attrName, String attrValue) {
        if (attrValue == null)
            return; // uses default (if there was one)
        if (attrName.equals("extends")) //$NON-NLS-1$
        {
            fSuperclass = attrValue;
        }
        else if (attrName.equals("import")) //$NON-NLS-1$
        {
            addImports(attrValue);
        }
        else if (attrName.equals("session")) //$NON-NLS-1$
        {
            fIsInASession = "true".equalsIgnoreCase(attrValue);
        }
        else if (attrName.equals("buffer")) //$NON-NLS-1$
        {
            // ignore for now
        }
        else if (attrName.equals("autoFlush")) //$NON-NLS-1$
        {
            // ignore for now
        }
        else if (attrName.equals("isThreadSafe")) //$NON-NLS-1$
        {
            // fThreadSafe = "true".equalsIgnoreCase(attrValue); //$NON-NLS-1$
        }
        else if (attrName.equals("isErrorPage")) //$NON-NLS-1$
        {
            fIsErrorPage = "true".equalsIgnoreCase(attrValue);
        }
    }

    protected void handleIncludeFile(String filename) {
        if (filename != null && fProcessIncludes) {
            IPath modelPath = getModelPath();
            IPath basePath = modelPath;
            if (basePath != null) {
                /*
                 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=227576
                 * 
                 * The resolution of the included fragment should use the file
                 * containing the directive as the base reference, not always
                 * the main JSP being invoked. Verified behavior with Apache
                 * Tomcat 5.5.20.
                 */
                if (!getIncludes().isEmpty()) {
                    basePath = new Path((String) getIncludes().peek());
                }
                String filePathString = FacetModuleCoreSupport.resolve(basePath, filename).toString();
                fIncludedPaths.add(filePathString);

                if (!getIncludes().contains(filePathString) && !filePathString.equals(modelPath.toString())) {
                    getIncludes().push(filePathString);
                    JSPIncludeRegionHelper helper = new JSPIncludeRegionHelper(this, true);
                    // Should we consider preludes on this segment?
                    helper.parse(filePathString);
                    getIncludes().pop();
                }
            }
        }
    }

    private Stack getIncludes() {
        if (fIncludes == null)
            fIncludes = new Stack();
        return fIncludes;
    }

    public Collection getIncludedPaths() {
        return fIncludedPaths;
    }

    protected void translateExpressionString(String newText, ITextRegionCollection embeddedContainer, int jspPositionStart, int jspPositionLength, boolean isIndirect) {
        appendToBuffer(EXPRESSION_PREFIX, fUserCode, false, embeddedContainer, true);
        appendToBuffer(newText, fUserCode, true, embeddedContainer, jspPositionStart, jspPositionLength, isIndirect, true);
        appendToBuffer(EXPRESSION_SUFFIX, fUserCode, false, embeddedContainer);
    }
    
    protected void translateExpressionString(String newText, ITextRegionCollection embeddedContainer, int jspPositionStart, int jspPositionLength, String quotetype) {
        if(quotetype == null || quotetype == DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE ||quotetype == DOMJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_SQUOTE ) {
            translateExpressionString(newText, embeddedContainer, jspPositionStart, jspPositionLength, false);
            return;
        }

        //-- This is a quoted attribute. We need to unquote as per the JSP spec: JSP 2.0 page 1-36
        appendToBuffer(EXPRESSION_PREFIX, fUserCode, false, embeddedContainer, true);

        int length = newText.length();
        int runStart = 0;
        int i = 0;
        for ( ; i < length; i++) {
            //-- collect a new run
            char c = newText.charAt(i);
            if (c == '\\') {
                //-- Escaped value. Add the run, then unescape
                int runLength = i-runStart;
                if (runLength > 0) {
                    appendToBuffer(newText.substring(runStart, i), fUserCode, true, embeddedContainer, jspPositionStart, runLength, true, true);
                    jspPositionStart += runLength + 1;
                    jspPositionLength -= runLength + 1;
                }
                runStart = ++i;
                if (i >= length) { // Escape but no data follows?!
                    //- error.
                    break;
                }
                c = newText.charAt(i);              // The escaped character, copied verbatim
            }
        }
        //-- Copy last-run
        int runLength = i - runStart;
        if (runLength > 0)
            appendToBuffer(newText.substring(runStart, i), fUserCode, true, embeddedContainer, jspPositionStart, runLength, true, false);
        appendToBuffer(EXPRESSION_SUFFIX, fUserCode, false, embeddedContainer);
    }

    protected void translateDeclarationString(String newText, ITextRegionCollection embeddedContainer, int jspPositionStart, int jspPositionLength, boolean isIndirect) {
        appendToBuffer(newText, fUserDeclarations, true, embeddedContainer, jspPositionStart, jspPositionLength, isIndirect);
        appendToBuffer(ENDL, fUserDeclarations, false, embeddedContainer);
    }

    /**
     * used by XMLJSPRegionHelper for included JSP files
     * 
     * @param newText
     * @param embeddedContainer
     * @param jspPositionStart
     * @param jspPositionLength
     */
    protected void translateScriptletString(String newText, ITextRegionCollection embeddedContainer, int jspPositionStart, int jspPositionLength, boolean isIndirect) {
        appendToBuffer(newText, fUserCode, true, embeddedContainer, jspPositionStart, jspPositionLength, isIndirect);
    }

    // the following 3 methods determine the cursor position
    // <%= %>
    protected void translateExpression(ITextRegionCollection region) {
        String newText = getUnescapedRegionText(region, EXPRESSION);
        appendToBuffer(EXPRESSION_PREFIX, fUserCode, false, region, true);
        appendToBuffer(newText, fUserCode, true, region, true);
        appendToBuffer(EXPRESSION_SUFFIX, fUserCode, false, region);
    }

    //
    // <%! %>
    protected void translateDeclaration(ITextRegionCollection region) {
        String newText = getUnescapedRegionText(region, DECLARATION);
        appendToBuffer(newText, fUserDeclarations, true, region);
        appendToBuffer(ENDL, fUserDeclarations, false, region);
    }

    //
    // <% %>
    protected void translateScriptlet(ITextRegionCollection region) {
        String newText = getUnescapedRegionText(region, SCRIPTLET);
        appendToBuffer(newText, fUserCode, true, region);
    }

    /**
     * Append using a region, probably indirect mapping (eg. <%@page
     * include=""%>)
     * 
     * @param newText
     * @param buffer
     * @param addToMap
     * @param jspReferenceRegion
     */
    private void appendToBuffer(String newText, StringBuffer buffer, boolean addToMap, ITextRegionCollection jspReferenceRegion) {
        int start = 0, length = 0;
        if (jspReferenceRegion != null) {
            start = jspReferenceRegion.getStartOffset();
            length = jspReferenceRegion.getLength();
        }
        appendToBuffer(newText, buffer, addToMap, jspReferenceRegion, start, length, false);
    }
    
    /**
     * Append using a region, probably indirect mapping (eg. <%@page
     * include=""%>)
     * 
     * @param newText
     * @param buffer
     * @param addToMap
     * @param jspReferenceRegion
     * @param nonl
     */
    private void appendToBuffer(String newText, StringBuffer buffer, boolean addToMap, ITextRegionCollection jspReferenceRegion, boolean nonl) {
        int start = 0, length = 0;
        if (jspReferenceRegion != null) {
            start = jspReferenceRegion.getStartOffset();
            length = jspReferenceRegion.getLength();
        }
        appendToBuffer(newText, buffer, addToMap, jspReferenceRegion, start, length, false, nonl);
    }
    
    private void appendToBuffer(String newText, StringBuffer buffer, boolean addToMap, ITextRegionCollection jspReferenceRegion, int jspPositionStart, int jspPositionLength, boolean isIndirect) {
        appendToBuffer(newText, buffer, addToMap, jspReferenceRegion, jspPositionStart, jspPositionLength, isIndirect, false);
    }


    /**
     * Adds newText to the buffer passed in, and adds to translation mapping
     * as specified by the addToMap flag. some special cases to consider (that
     * may be affected by changes to this method): included files scriplets in
     * an attribute value refactoring
     * 
     * @param newText
     * @param buffer
     * @param addToMap
     */
    private void appendToBuffer(String newText, StringBuffer buffer, boolean addToMap, ITextRegionCollection jspReferenceRegion, int jspPositionStart, int jspPositionLength, boolean isIndirect, boolean nonl) {

        int origNewTextLength = newText.length();

        // nothing to append
        if (jspReferenceRegion == null)
            return;

        // add a newline so translation looks cleaner
        if (! nonl && !newText.endsWith(ENDL))
            newText += ENDL;

        //dump any non translated code before writing translated code
        writePlaceHolderForNonTranslatedCode();

        //if appending to the buffer can assume something got translated
        fCodeTranslated = true;

        if (buffer == fUserCode) {
            buffer.append(newText);
            if (addToMap) {
                if (isUsebeanTag(jspReferenceRegion)) {
                    try {
                        // requires special mapping
                        appendUseBeanToBuffer(newText, jspReferenceRegion, isIndirect);
                    }
                    catch (Exception e) {
                        // still working out kinks
                        Logger.logException(e);
                    }
                }
                else {
                    // all other cases
                    Position javaRange = new Position(fOffsetInUserCode, origNewTextLength);
                    Position jspRange = new Position(jspPositionStart, jspPositionLength);

                    fCodeRanges.put(javaRange, jspRange);
                    if (isIndirect)
                        fIndirectRanges.put(javaRange, jspRange);
                }
            }
            fOffsetInUserCode += newText.length();
        }
        else if (buffer == fUserDeclarations) {
            buffer.append(newText);
            if (addToMap) {
                Position javaRange = new Position(fOffsetInUserDeclarations, newText.length());
                Position jspRange = new Position(jspPositionStart, jspPositionLength);

                fDeclarationRanges.put(javaRange, jspRange);
                if (isIndirect)
                    fIndirectRanges.put(javaRange, jspRange);
            }
            fOffsetInUserDeclarations += newText.length();
        }
    }

    /**
     * 
     * @param jspReferenceRegion
     * @return
     */
    private boolean isUsebeanTag(ITextRegionCollection jspReferenceRegion) {
        ITextRegionList regions = jspReferenceRegion.getRegions();
        ITextRegion r = null;
        boolean isUseBean = false;
        for (int i = 0; i < regions.size(); i++) {
            r = regions.get(i);
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=128490
            // length of 11 is the length of jsp:useBean
            // and saves the expensive getText.equals call
            if (r.getType() == DOMRegionContext.XML_TAG_NAME) {
                if (r.getTextLength() == 11 && jspReferenceRegion.getText(r).equals("jsp:useBean")) { //$NON-NLS-1$
                    isUseBean = true;
                }
                // break no matter what if you hit tagname
                break;
            }
        }
        return isUseBean;
    }

    /**
     * @param importName
     *            should be just the package plus the type eg. java.util.List
     *            or java.util.*
     * @param jspReferenceRegion
     *            should be the <%@ page import = "java.util.List"%> region
     * @param addToMap
     */
    private void appendImportToBuffer(String importName, ITextRegionCollection jspReferenceRegion, boolean addToMap) {
        String javaImportString = "import " + importName + ";" + ENDL; //$NON-NLS-1$ //$NON-NLS-2$
        fUserImports.append(javaImportString);
        if (addToMap) {
            addImportToMap(importName, jspReferenceRegion);
        }
        fOffsetInUserImports += javaImportString.length();
    }

    /**
     * new text can be something like: "import java.lang.Object;\n"
     * 
     * but the reference region could have been something like: <%@page
     * import="java.lang.Object, java.io.*, java.util.List"%>
     * 
     * so the exact mapping has to be calculated carefully.
     * 
     * isIndirect means that the import came from an included file (if true)
     * 
     * @param importName
     * @param jspReferenceRegion
     */
    private void addImportToMap(String importName, ITextRegionCollection jspReferenceRegion) {

        // massage text
        // String jspText = importName.substring(importName.indexOf("import ")
        // + 7, importName.indexOf(';'));
        // String jspText = importName.trim();

        // these positions will be updated below
        Position javaRange = new Position(fOffsetInUserImports + 7, 1);
        Position jspRange = new Position(jspReferenceRegion.getStart(), jspReferenceRegion.getLength());

        // calculate JSP range by finding "import" attribute
        ITextRegionList regions = jspReferenceRegion.getRegions();
        int size = regions.size();

        int start = -1;
        int length = -1;

        ITextRegion r = null;
        for (int i = 0; i < size; i++) {
            r = regions.get(i);
            if (r.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)
                if (jspReferenceRegion.getText(r).trim().equals("import")) { //$NON-NLS-1$
                    // get the attr value region
                    if (size > i + 2) {
                        r = regions.get(i + 2);
                        if (r.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {

                            String jspImportText = jspReferenceRegion.getText(r);

                            // the position in question (in the JSP) is what
                            // is bracketed below
                            // includes whitespace
                            // <%@page import="java.lang.Object,[ java.io.* ],
                            // java.util.List"%>

                            // in the java file
                            // import [ java.io.* ];

                            start = jspImportText.indexOf(importName);
                            length = importName.length();

                            // safety, don't add to map if bad positioning
                            if (start == -1 || length < 1)
                                break;

                            // update jsp range
                            jspRange.setOffset(jspReferenceRegion.getStartOffset(r) + start);
                            jspRange.setLength(length);

                            // update java range
                            javaRange.setLength(length);

                            break;
                        }
                    }
                }
        }

        // safety for bad ranges
        if (start != -1 && length > 1) {
            // put ranges in java -> jsp range map
            fImportRanges.put(javaRange, jspRange);
        }
    }

    /**
     * temp fix for 282295 until better mapping is in place
     * 
     * @param newText
     * @param jspReferenceRegion
     */
    private void appendUseBeanToBuffer(String newText, ITextRegionCollection jspReferenceRegion, boolean isIndirect) throws Exception {
        // java string looks like this (tokenized)
        // Type id = new Classname();\n
        // 0 1 2 3 4
        // or
        // Type id = null;\n // if there is no classname
        // 0 1 2 3

        // ----------------------
        // calculate java ranges
        // ----------------------
        StringTokenizer st = new StringTokenizer(newText, " ", false); //$NON-NLS-1$
        int i = 0;
        String[] parsedJava = new String[st.countTokens()];
        while (st.hasMoreTokens())
            parsedJava[i++] = st.nextToken();

        String type = parsedJava[0] != null ? parsedJava[0] : ""; //$NON-NLS-1$
        String id = parsedJava[1] != null ? parsedJava[1] : ""; //$NON-NLS-1$
        String className = parsedJava.length > 4 ? parsedJava[4] : ""; //$NON-NLS-1$

        Position javaTypeRange = new Position(fOffsetInUserCode, type.length());
        Position javaIdRange = new Position(fOffsetInUserCode + type.length() + 1, id.length());
        Position javaClassRange = new Position(fOffsetInUserCode + type.length() + 1 + id.length() + 7, 0);
        /*
         * https://bugs.eclipse.org/bugs/show_bug.cgi?id=212242 - Check for
         * the existence of '(' first.
         */
        int parenPos = -1;
        if (className.length() >= 4 && (parenPos = className.indexOf('(')) >= 0) {
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=86132
            int classNameLength = className.substring(0, parenPos).length();
            javaClassRange = new Position(fOffsetInUserCode + type.length() + 1 + id.length() + 7, classNameLength);
        }

        // ---------------------
        // calculate jsp ranges
        // ---------------------
        ITextRegionList regions = jspReferenceRegion.getRegions();
        ITextRegion r = null;
        String attrName = "", attrValue = ""; //$NON-NLS-1$ //$NON-NLS-2$
        int quoteOffset = 0;
        Position jspTypeRange = null;
        Position jspIdRange = null;
        Position jspClassRange = null;

        for (int j = 0; j < regions.size(); j++) {
            r = regions.get(j);
            if (r.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME) {
                attrName = jspReferenceRegion.getText(r);
                if (regions.size() > j + 2 && regions.get(j + 2).getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {
                    // get attr value
                    r = regions.get(j + 2);
                    attrValue = jspReferenceRegion.getText(r);

                    // may have quotes
                    quoteOffset = (attrValue.startsWith("\"") || attrValue.startsWith("'")) ? 1 : 0; //$NON-NLS-1$ //$NON-NLS-2$

                    if (attrName.equals("type")) //$NON-NLS-1$
                        jspTypeRange = new Position(jspReferenceRegion.getStartOffset(r) + quoteOffset, StringUtils.stripQuotesLeaveInsideSpace(attrValue).length());
                    else if (attrName.equals("id")) //$NON-NLS-1$
                        jspIdRange = new Position(jspReferenceRegion.getStartOffset(r) + quoteOffset, StringUtils.stripQuotesLeaveInsideSpace(attrValue).length());
                    else if (attrName.equals("class")) //$NON-NLS-1$
                        jspClassRange = new Position(jspReferenceRegion.getStartOffset(r) + quoteOffset, StringUtils.stripQuotesLeaveInsideSpace(attrValue).length());
                }
            }
        }

        // put ranges in java -> jsp range map
        if (!type.equals("") && jspTypeRange != null) { //$NON-NLS-1$
            fCodeRanges.put(javaTypeRange, jspTypeRange);
            // note: don't update offsets for this map when result is built
            // they'll be updated when code ranges offsets are updated
            fUseBeanRanges.put(javaTypeRange, jspTypeRange);
            if (isIndirect)
                fIndirectRanges.put(javaTypeRange, jspTypeRange);
        }
        if (!id.equals("") && jspIdRange != null) { //$NON-NLS-1$
            fCodeRanges.put(javaIdRange, jspIdRange);
            // note: don't update offsets for this map when result is built
            // they'll be updated when code ranges offsets are updated
            fUseBeanRanges.put(javaIdRange, jspTypeRange);
            if (isIndirect)
                fIndirectRanges.put(javaIdRange, jspTypeRange);
        }
        if (!className.equals("") && jspClassRange != null) { //$NON-NLS-1$
            fCodeRanges.put(javaClassRange, jspClassRange);
            // note: don't update offsets for this map when result is built
            // they'll be updated when code ranges offsets are updated
            fUseBeanRanges.put(javaClassRange, jspTypeRange);
            if (isIndirect)
                fIndirectRanges.put(javaClassRange, jspTypeRange);
        }
    }

    /**
     * Set the buffer to the current JSPType: STANDARD_JSP, EMBEDDED_JSP,
     * DECLARATION, EXPRESSION, SCRIPTLET (for keepting track of cursor
     * position when the final document is built)
     * 
     * @param JSPType
     *            the JSP type that the cursor is in
     */
    protected void setCursorOwner(int JSPType) {
        switch (JSPType) {
            case DECLARATION :
                setCursorOwner(fUserDeclarations);
                break;
            case EXPRESSION :
            case SCRIPTLET :
                setCursorOwner(fUserCode);
                break;
            default :
                setCursorOwner(fUserCode);
        }
    }

    /**
     * this piece of code iterates through fCurrentNodes and clumps them
     * together in a big text string - unescaping characters if they are not
     * CDATA - simply appending if they are CDATA it stops iteration when it
     * hits a node that is an XML_TAG_NAME (which should be the region closing
     * tag)
     */
    protected String getUnescapedRegionText(ITextRegionCollection stRegion, int JSPType) {
        StringBuffer buffer = new StringBuffer();
        int start = stRegion.getStartOffset();
        int end = stRegion.getEndOffset();
        // adjustment necessary for embedded region containers
        if (stRegion instanceof ITextRegionContainer && stRegion.getType() == DOMRegionContext.BLOCK_TEXT) {
            if (stRegion.getRegions() != null && stRegion.getRegions().size() > 1) {
                ITextRegion jspContent = stRegion.getRegions().get(1); // should
                // be
                // jspContent
                // region
                start = stRegion.getStartOffset(jspContent);
                end = stRegion.getEndOffset(jspContent);
            }
        }
        int CDATAOffset = 0; // number of characters lost in conversion
        int bufferSize = 0;
        if (stRegion.getType() == DOMJSPRegionContexts.JSP_CONTENT || stRegion.getType() == DOMRegionContext.BLOCK_TEXT // need
                    // this
                    // for
                    // embedded
                    // JSP
                    // regions
                    || stRegion.getType() == DOMRegionContext.XML_TAG_NAME) // need
        // this
        // in
        // case
        // there's
        // no
        // region...
        {
            fInCodeRegion = (start <= fSourcePosition && fSourcePosition <= end);
            if (fInCodeRegion) {
                setCursorOwner(JSPType);
                setRelativeOffset((fSourcePosition - start) + getCursorOwner().length());
                if (JSPType == EXPRESSION) {
                    // if an expression, add then length of the enclosing
                    // paren..
                    setCursorInExpression(true);
                    setRelativeOffset(getRelativeOffset() + EXPRESSION_PREFIX.length());
                }
            }
            ITextRegion jspContent = null;
            if (stRegion.getRegions() != null && stRegion.getRegions().size() > 1)
                jspContent = stRegion.getRegions().get(1);
            return (jspContent != null) ? stRegion.getFullText(jspContent) : stRegion.getFullText(); // don't
            // unescape
            // if
            // it's
            // not
            // an
            // XMLJSP
            // tag
        }
        else if (stRegion.getType() == DOMJSPRegionContexts.JSP_CLOSE) {
            // need to determine cursor owner so that the fCurosorPosition
            // will be
            // correct even if there is no region after the cursor in the JSP
            // file
            setCursorOwner(JSPType);
        }
        // iterate XMLCONTENT and CDATA regions
        // loop fCurrentNode until you hit </jsp:scriptlet> (or other closing
        // tag name)
        while (getCurrentNode() != null && getCurrentNode().getType() != DOMRegionContext.XML_TAG_NAME && getCurrentNode().getType() != DOMJSPRegionContexts.JSP_CLOSE) // need to stop on the ending tag name...
        {
            start = getCurrentNode().getStartOffset();
            end = getCurrentNode().getEndOffset();
            bufferSize = buffer.length();
            CDATAOffset = unescapeRegion(getCurrentNode(), buffer);
            fInCodeRegion = (start <= fSourcePosition && fSourcePosition <= end);
            if (fInCodeRegion) {
                setCursorOwner(JSPType);
                // this offset is sort of complicated...
                // it's composed of:
                // 1. the length of the start of the current region up till
                // where the cursor is
                // 2. minus the number of characters lost in CDATA translation
                // 3. plus the length of the escaped buffer before the current
                // region, but
                // is still within the jsp tag
                setRelativeOffset((fSourcePosition - getCurrentNode().getStartOffset()) + getCursorOwner().length() - CDATAOffset + bufferSize);
                if (JSPType == EXPRESSION) {
                    setCursorInExpression(true);
                    // if an expression, add then length of the enclosing
                    // paren..
                    setRelativeOffset(getRelativeOffset() + EXPRESSION_PREFIX.length());
                }
            }
            if (getCurrentNode() != null)
                advanceNextNode();
        }
        return buffer.toString();
    }

    /**
     * @param r
     *            the region to be unescaped (XMLContent, XML ENTITY
     *            REFERENCE, or CDATA)
     * @param sb
     *            the stringbuffer to append the text to
     * @return the number of characters removed in unescaping this text
     */
    protected int unescapeRegion(ITextRegion r, StringBuffer sb) {
        String s = ""; //$NON-NLS-1$
        int lengthBefore = 0, lengthAfter = 0, cdata_tags_length = 0;
        if (r != null && (r.getType() == DOMRegionContext.XML_CONTENT || r.getType() == DOMRegionContext.XML_ENTITY_REFERENCE)) {
            lengthBefore = (getCurrentNode() != r) ? getCurrentNode().getFullText(r).length() : getCurrentNode().getFullText().length();
            s = EscapedTextUtil.getUnescapedText(getCurrentNode(), r);
            lengthAfter = s.length();
            sb.append(s);
        }
        else if (r != null && r.getType() == DOMRegionContext.XML_CDATA_TEXT) {
            if (r instanceof ITextRegionContainer) // only interested in
            // contents
            {
                // navigate to next region container (which should be a JSP
                // region)
                Iterator it = ((ITextRegionContainer) r).getRegions().iterator();
                ITextRegion temp = null;
                while (it.hasNext()) {
                    temp = (ITextRegion) it.next();
                    if (temp instanceof ITextRegionContainer || temp.getType() == DOMRegionContext.XML_CDATA_TEXT) {
                        sb.append(getCurrentNode().getFullText(temp));
                    }
                    else if (temp.getType() == DOMRegionContext.XML_CDATA_OPEN || temp.getType() == DOMRegionContext.XML_CDATA_CLOSE) {
                        cdata_tags_length += temp.getLength();
                    }
                }
            }
        }
        return (lengthBefore - lengthAfter + cdata_tags_length);
    }

    //
    // <jsp:useBean>
    protected void translateUseBean(ITextRegionCollection container) {
        ITextRegion r = null;
        String attrName = null;
        String attrValue = null;
        String id = null;
        ITextRegion idRegion = null;
        String type = null;
        ITextRegion typeRegion = null;
        String className = null;
        ITextRegion classnameRegion = null;
        String beanName = null;
        ITextRegion beanNameRegion = null;

        if (DOMRegionContext.XML_END_TAG_OPEN.equals(container.getFirstRegion().getType())) {
            if (!fUseBeansStack.isEmpty()) {
                fUseBeansStack.pop();
                appendToBuffer("}", fUserCode, false, fCurrentNode); //$NON-NLS-1$ 
            }
            else {
                // no useBean start tag being remembered
                ITextRegionCollection extraEndRegion = container;
                IJSPProblem missingStartTag = createJSPProblem(IJSPProblem.UseBeanStartTagMissing, IJSPProblem.F_PROBLEM_ID_LITERAL, "", extraEndRegion.getStartOffset(), extraEndRegion.getEndOffset());
                fTranslationProblems.add(missingStartTag);
            }
            return;
        }

        Iterator regions = container.getRegions().iterator();
        while (regions.hasNext() && (r = (ITextRegion) regions.next()) != null && (r.getType() != DOMRegionContext.XML_TAG_CLOSE || r.getType() != DOMRegionContext.XML_EMPTY_TAG_CLOSE)) {
            attrName = attrValue = null;
            if (r.getType().equals(DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)) {
                attrName = container.getText(r).trim();
                if (regions.hasNext() && (r = (ITextRegion) regions.next()) != null && r.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS) {
                    if (regions.hasNext() && (r = (ITextRegion) regions.next()) != null && r.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {
                        attrValue = StringUtils.stripQuotes(container.getText(r));
                    }
                    // has equals, but no value?
                }
                // an attribute with no equals?
            }
            // (pa) might need different logic here if we wanna support more
            if (attrName != null && attrValue != null) {
                if (attrName.equals("id")) {//$NON-NLS-1$
                    id = attrValue;
                    idRegion = r;
                }
                else if (attrName.equals("class")) {//$NON-NLS-1$
                    className = attrValue;
                    classnameRegion = r;
                }
                else if (attrName.equals("type")) {//$NON-NLS-1$
                    type = attrValue;
                    typeRegion = r;
                }
                else if (attrName.equals("beanName")) { //$NON-NLS-1$
                    beanName = attrValue;
                    beanNameRegion = r;
                }
            }
        }

        if (id != null) {
            // The id is not a valid Java identifier
            if (!isValidJavaIdentifier(id) && idRegion != null) {
                Object problem = createJSPProblem(IJSPProblem.UseBeanInvalidID, IProblem.ParsingErrorInvalidToken, MessageFormat.format(JSPCoreMessages.JSPTranslator_0, new String[]{id}), container.getStartOffset(idRegion), container.getTextEndOffset(idRegion) - 1);
                fTranslationProblems.add(problem);
            }
            // No Type information is provided
            if (((type == null && className == null) || (type == null && beanName != null)) && idRegion != null) {
                Object problem = createJSPProblem(IJSPProblem.UseBeanMissingTypeInfo, IProblem.UndefinedType, NLS.bind(JSPCoreMessages.JSPTranslator_3, new String[]{id}), container.getStartOffset(idRegion), container.getTextEndOffset(idRegion) - 1);
                fTranslationProblems.add(problem);
            }
            // Cannot specify both a class and a beanName
            if (className != null && beanName != null && beanNameRegion != null) {
                ITextRegion nameRegion = container.getRegions().get(1);
                Object problem = createJSPProblem(IJSPProblem.UseBeanAmbiguousType, IProblem.AmbiguousType, JSPCoreMessages.JSPTranslator_2, container.getStartOffset(nameRegion), container.getTextEndOffset(nameRegion) - 1);
                fTranslationProblems.add(problem);
            }
            /*
             * Only have a class or a beanName at this point, and potentially
             * a type has id w/ type and/or classname/beanName
             */
            // Type id = new Classname/Beanname();
            // or
            // Type id = null; // if there is no classname or beanname
            if ((type != null || className != null)) {
                if (type == null) {
                    type = className;
                    typeRegion = classnameRegion;
                }

                /* Now check the types (multiple of generics may be involved) */
                List errorTypeNames = new ArrayList(2);
                if (!isTypeFound(type, errorTypeNames)) {
                    for (int i = 0; i < errorTypeNames.size(); i++) {
                        Object problem = createJSPProblem(IJSPProblem.F_PROBLEM_ID_LITERAL, IProblem.UndefinedType, MessageFormat.format(JSPCoreMessages.JSPTranslator_1, new String[]{errorTypeNames.get(i).toString()}), container.getStartOffset(typeRegion), container.getTextEndOffset(typeRegion) - 1);
                        fTranslationProblems.add(problem);
                    }
                }
                else {
                    String prefix = type + " " + id + " = "; //$NON-NLS-1$ //$NON-NLS-2$
                    String suffix = "null;" + ENDL; //$NON-NLS-1$
                    if (className != null)
                        suffix = "new " + className + "();" + ENDL; //$NON-NLS-1$ //$NON-NLS-2$
                    else if (beanName != null)
                        suffix = "(" + type + ") java.beans.Beans.instantiate(getClass().getClassLoader(), \"" + beanName + "\");" + ENDL; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    appendToBuffer(prefix + suffix, fUserCode, true, fCurrentNode);
                }
            }
        }
        /*
         * Add a brace and remember the start tag regardless of whether a
         * variable was correctly created
         */
        if (!DOMRegionContext.XML_EMPTY_TAG_CLOSE.equals(container.getLastRegion().getType())) {
            fUseBeansStack.push(container);
            // GRAILS Change: always need something around the anonymous block
            appendToBuffer("if(true){", fUserCode, false, fCurrentNode); //$NON-NLS-1$ 
            // GRAILS End
        }
    }

    /**
     * @param type
     * @return
     */
    private boolean isTypeFound(String rawTypeValue, List errorTypeNames) {
        IFile file = getFile();
        if(file == null)
            return true;
        
        IProject project = file.getProject();
        IJavaProject p = JavaCore.create(project);
        if (p.exists()) {
            String types[] = new String[3];
            if (rawTypeValue.indexOf('<') > 0) {
                // JSR 14 : Generics are being used, parse them out
                try {
                    StringTokenizer toker = new StringTokenizer(rawTypeValue);
                    // generic
                    types[0] = toker.nextToken("<"); //$NON-NLS-1$
                    // type 1 or key
                    types[1] = toker.nextToken(",>"); //$NON-NLS-1$
                    // type 2 or value
                    types[2] = toker.nextToken(",>"); //$NON-NLS-1$
                }
                catch (NoSuchElementException e) {
                    // StringTokenizer failure with unsupported syntax
                }
            }
            else {
                types[0] = rawTypeValue;
            }

            for (int i = 0; i < types.length; i++) {
                if (types[i] != null) {
                    // remove any array suffixes 
                    if (types[i].indexOf('[') > 0) {
                        types[i] = types[i].substring(0, types[i].indexOf('[')); //$NON-NLS-1$
                    }
                    // remove any "extends" prefixes (JSR 14)
                    if (types[i].indexOf("extends") > 0) {
                        types[i] = StringUtils.strip(types[i].substring(types[i].indexOf("extends"))); //$NON-NLS-1$
                    }

                    addNameToListIfTypeNotFound(p, types[i], errorTypeNames);
                }
            }
        }
        return errorTypeNames.isEmpty();
    }
    
    private void addNameToListIfTypeNotFound(IJavaProject p, String typeName, List collectedNamesNotFound) {
        try {
            if (typeName != null) {
                IType type = p.findType(typeName);
                if (type == null || !type.exists()) {
                    collectedNamesNotFound.add(typeName);
                }
                else {
                    IResource typeResource = type.getResource();
                    if(typeResource != null) {
                        
                    }
                }
            }
        }
        catch (JavaModelException e) {
            // Not a Java Project
        }
    }

    private boolean isValidJavaIdentifier(String id) {
        char[] idChars = id.toCharArray();
        if (idChars.length < 1)
            return false;
        boolean isValid = Character.isJavaIdentifierStart(idChars[0]);
        for (int i = 1; i < idChars.length; i++) {
            isValid = isValid && Character.isJavaIdentifierPart(idChars[i]);
        }
        return isValid;
    }

    final public int getCursorPosition() {
        return fCursorPosition;
    }

    protected boolean isCursorInExpression() {
        return fCursorInExpression;
    }

    protected void setCursorInExpression(boolean in) {
        fCursorInExpression = in;
    }

    final public void setSourceCursor(int i) {
        fSourcePosition = i;
    }

    final public int getSourcePosition() {
        return fSourcePosition;
    }

    final public TLDCMDocumentManager getTLDCMDocumentManager() {
        return TaglibController.getTLDCMDocumentManager(fStructuredDocument);
    }

    final public void setRelativeOffset(int relativeOffset) {
        this.fRelativeOffset = relativeOffset;
    }

    final public int getRelativeOffset() {
        return fRelativeOffset;
    }

    private void setCursorOwner(StringBuffer cursorOwner) {
        this.fCursorOwner = cursorOwner;
    }

    final public StringBuffer getCursorOwner() {
        return fCursorOwner;
    }

    private IStructuredDocumentRegion setCurrentNode(IStructuredDocumentRegion currentNode) {
        return this.fCurrentNode = currentNode;
    }

    final public IStructuredDocumentRegion getCurrentNode() {
        return fCurrentNode;
    }

    public IStructuredDocument getStructuredDocument() {
        return fStructuredDocument;
    }

    private IPath getModelPath() {
        IPath path = null;
        ITextFileBuffer buffer = FileBufferModelManager.getInstance().getBuffer(getStructuredDocument());
        if (buffer != null) {
            path = buffer.getLocation();
        }
        return path;
    }

    private void translateCodas() {
        fProcessIncludes = false;
        IPath modelpath = getModelPath();
        if (modelpath != null) {
            PropertyGroup[] propertyGroups = DeploymentDescriptorPropertyCache.getInstance().getPropertyGroups(modelpath);
            for (int j = 0; j < propertyGroups.length; j++) {
                IPath[] codas = propertyGroups[j].getIncludeCoda();
                for (int i = 0; i < codas.length; i++) {
                    if (!getIncludes().contains(codas[i].toString()) && !codas[i].equals(modelpath)) {
                        getIncludes().push(codas[i]);
                        JSPIncludeRegionHelper helper = new JSPIncludeRegionHelper(this, true);
                        helper.parse(codas[i].toString());
                        getIncludes().pop();
                    }
                }
            }
        }
        fProcessIncludes = true;
    }

    private void translatePreludes() {
        fProcessIncludes = false;
        IPath modelpath = getModelPath();
        if (modelpath != null) {
            PropertyGroup[] propertyGroups = DeploymentDescriptorPropertyCache.getInstance().getPropertyGroups(modelpath);
            for (int j = 0; j < propertyGroups.length; j++) {
                IPath[] preludes = propertyGroups[j].getIncludePrelude();
                for (int i = 0; i < preludes.length; i++) {
                    if (!getIncludes().contains(preludes[i].toString()) && !preludes[i].equals(modelpath)) {
                        getIncludes().push(preludes[i]);
                        JSPIncludeRegionHelper helper = new JSPIncludeRegionHelper(this, true);
                        helper.parse(preludes[i].toString());
                        getIncludes().pop();
                    }
                }
            }
        }
        fProcessIncludes = true;
    }

    /**
     * <p>Writes an empty expression to {@link #fUserCode} if previously
     * found non translated code</p>
     * <p>This should be done before appending any newly translated code.</p>
     */
    private void writePlaceHolderForNonTranslatedCode() {
        if(fFoundNonTranslatedCode) {
            // GRAILS Change: don't need this
            // original
//            String text = ("{" + EXPRESSION_PREFIX + "\"\"" + EXPRESSION_SUFFIX + "}" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//                    " //markup"+ ENDL); //$NON-NLS-1$
//            fUserCode.append(text);
//            fOffsetInUserCode += text.length();
            // GRAILS End
            fFoundNonTranslatedCode = false;
        }
    }

    /**
     * <p><b>NOTE: </b>If the implementation of this method is changed be sure to update
     *  {@link #readExternal(ObjectInput)} and {@link #serialVersionUID}</p>
     *
     * @see #readExternal(ObjectInput)
     * @see #serialVersionUID
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        writeString(out, this.fClassHeader);
        writeString(out, this.fClassname);
        writeString(out, this.fSuperclass);
        writeString(out, this.fImplicitImports);
        writeString(out, this.fServiceHeader);
        writeBuffer(out, this.fUserImports);
        out.writeInt(this.fSourcePosition);
        out.writeInt(this.fRelativeOffset);
        out.writeInt(this.fCursorPosition);
        out.writeBoolean(this.fIsErrorPage);
        out.writeBoolean(this.fCursorInExpression);
        out.writeBoolean(this.fIsInASession);
        writeBuffer(out, this.fUserCode);
        writeBuffer(out, this.fUserELExpressions);
        writeBuffer(out, this.fUserDeclarations);
        writeBuffer(out, this.fCursorOwner);
        out.writeBoolean(this.fInCodeRegion);
        out.writeBoolean(this.fProcessIncludes);
        out.writeInt(this.fOffsetInUserImports);
        out.writeInt(this.fOffsetInUserDeclarations);
        out.writeInt(this.fOffsetInUserCode);
        
        //write included paths
        out.writeInt(this.fIncludedPaths.size());
        Iterator iter = this.fIncludedPaths.iterator();
        while(iter.hasNext()) {
            writeString(out, (String)iter.next());
        }
        
        writeRanges(out, this.fImportRanges);
        writeRanges(out, this.fCodeRanges);
        writeRanges(out, this.fDeclarationRanges);
        writeRanges(out, this.fUseBeanRanges);
        writeRanges(out, this.fUserELRanges);
        writeRanges(out, this.fIndirectRanges);
        writeString(out, this.fELTranslatorID);
    }
    
    /**
     * <p><b>NOTE 1: </b>After reading in an externalized {@link JSPTranslator} the caller must
     * manually call {@link #postReadExternalSetup(IStructuredModel)} to finish setting up
     * the {@link JSPTranslator} for use.</p>
     * 
     * <p><b>NOTE 2: </b>If the implementation of this method is changed be sure to update
     * {@link #writeExternal(ObjectOutput)} and {@link #serialVersionUID}</p>
     * 
     * @see #writeExternal(ObjectOutput)
     * @see #serialVersionUID
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.fClassHeader = readString(in);
        this.fClassname = readString(in);
        this.fSuperclass = readString(in);
        this.fImplicitImports = readString(in);
        this.fServiceHeader = readString(in);
        this.fUserImports = new StringBuffer(readString(in));
        this.fSourcePosition = in.readInt();
        this.fRelativeOffset = in.readInt();
        this.fCursorPosition = in.readInt();
        this.fIsErrorPage = in.readBoolean();
        this.fCursorInExpression = in.readBoolean();
        this.fIsInASession = in.readBoolean();
        this.fUserCode = new StringBuffer(readString(in));
        this.fUserELExpressions = new StringBuffer(readString(in));
        this.fUserDeclarations = new StringBuffer(readString(in));
        this.fCursorOwner = new StringBuffer(readString(in));
        this.fInCodeRegion = in.readBoolean();
        this.fProcessIncludes = in.readBoolean();
        this.fOffsetInUserImports = in.readInt();
        this.fOffsetInUserDeclarations = in.readInt();
        this.fOffsetInUserCode = in.readInt();
        
        //read included paths
        int size = in.readInt();
        this.fIncludedPaths = new HashSet(size);
        for(int i = 0; i < size; ++i) {
            this.fIncludedPaths.add(readString(in));
        }
        
        this.fImportRanges = readRanges(in);
        this.fCodeRanges = readRanges(in);
        this.fDeclarationRanges = readRanges(in);
        this.fUseBeanRanges = readRanges(in);
        this.fUserELRanges = readRanges(in);
        this.fIndirectRanges = readRanges(in);
        this.fELTranslatorID = readString(in);
        
        //build result
        this.buildResult(false);
    }
    
    /**
     * <p>This does mandatory setup needed after a JSPTranslator is restored from
     * a persisted file</p>
     * 
     * @param jspModel {@link IStructuredModel} representing the JSP file that this
     * is a translator for
     */
    protected void postReadExternalSetup(IStructuredModel jspModel) {
        this.fStructuredDocument = jspModel.getStructuredDocument();
        this.fJspTextBuffer = new StringBuffer(this.fStructuredDocument.get());
        if(jspModel instanceof IDOMModel) {
            this.fStructuredModel = (IDOMModel)jspModel;
        }
    }
    
    /**
     * <p>Writes a string to an {@link ObjectOutput} stream</p>
     * 
     * <p><b>NOTE: </b>If the implementation of this method is changed be sure to update
     * {@link #readString(ObjectInput)} and {@link #serialVersionUID}</p>
     * 
     * @param out {@link ObjectOutput} stream to write <code>s</code> too
     * @param s {@link String} to write to <code>out</code>
     * 
     * @throws IOException IO can throw exceptions
     * 
     * @see #readString(ObjectInput)
     */
    private static void writeString(ObjectOutput out, String s) throws IOException {
        if(s != null) {
            out.writeInt(s.length());
            out.writeBytes(s);
        } else {
            out.writeInt(0);
        }
    }
    
    /**
     * <p>Reads a {@link String} written by {@link #writeString(ObjectOutput, String)} from
     * a {@link ObjectInput} stream.</p>
     * 
     * <p><b>NOTE: </b>If the implementation of this method is changed be sure to update
     * {@link #writeString(ObjectOutput, String)} and {@link #serialVersionUID}</p>
     * 
     * @param in {@link ObjectInput} stream to read the {@link String} from
     * @return {@link String} read from <code>in</code>
     * 
     * @throws IOException IO Can throw exceptions
     * 
     * @see #writeString(ObjectOutput, String)
     */
    private static String readString(ObjectInput in) throws IOException {
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes);
    }
    
    /**
     * <p>Writes a {@link StringBuffer} to an {@link ObjectOutput} stream</p>
     * 
     * @param out {@link ObjectOutput} stream to write <code>s</code> too
     * @param s {@link String} to write to <code>out</code>
     * 
     * @throws IOException IO can throw exceptions
     * 
     * @see #readString(ObjectInput)
     */
    private static void writeBuffer(ObjectOutput out, StringBuffer buff) throws IOException {
        if(buff != null && buff.length() > 0) {
            writeString(out, buff.toString());
        } else {
            writeString(out, null);
        }
    }
    
    /**
     * <p>Writes a {@link HashMap} of {@link Position}s to an {@link ObjectOutput} stream</p>
     * 
     * <p><b>NOTE: </b>If the implementation of this method is changed be sure to update
     * {@link #readRanges(ObjectInput)} and {@link #serialVersionUID}</p>
     * 
     * @param out {@link ObjectOutput} stream to write to
     * @param ranges {@link HashMap} of {@link Position}s to write to <code>out</code>
     * 
     * @throws IOException IO can throw exceptions
     * 
     * @see #readRanges(ObjectInput)
     */
    private static void writeRanges(ObjectOutput out, HashMap ranges) throws IOException {
        //this is a strange hack because Position is not designed to be used as keys in a Map, see Position doc
        HashMap temp = new HashMap();
        temp.putAll(ranges);
        
        Iterator iter = temp.keySet().iterator();
        out.writeInt(ranges.size());
        while(iter.hasNext()) {
            Position javaPos = (Position)iter.next();
            Position jspPos = (Position)temp.get(javaPos);
            out.writeInt(javaPos.offset);
            out.writeInt(javaPos.length);
            out.writeBoolean(javaPos.isDeleted);
            
            if(jspPos != null) {
                out.writeInt(jspPos.offset);
                out.writeInt(jspPos.length);
                out.writeBoolean(jspPos.isDeleted);
            } else {
                out.writeInt(-1);
                out.writeInt(-1);
            }
        }
    }
    
    /**
     * <p>Reads a {@link HashMap} of {@link Position}s from an {@link ObjectInput} stream that was written by
     * {@link #writeRanges(ObjectOutput, HashMap)}</p>
     * 
     * <p><b>NOTE: </b>If the implementation of this method is changed be sure to update
     * {@link #writeRanges(ObjectOutput, HashMap)} and {@link #serialVersionUID}</p>
     * 
     * @param in {@link ObjectInput} stream to read the {@link HashMap} of {@link Position}s from
     * @return {@link HashMap} of {@link Position}s read from <code>in</code>
     * 
     * @throws IOException IO can throw exceptions
     * 
     * @see #writeRanges(ObjectOutput, HashMap)
     */
    private static HashMap readRanges(ObjectInput in) throws IOException {
        int size = in.readInt();
        HashMap ranges = new HashMap(size);
        for(int i = 0; i < size; ++i) {
            Position javaPos = new Position(in.readInt(), in.readInt());
            if(in.readBoolean()) {
                javaPos.delete();
            }
            
            //if the jspPos was null for some reason then -1 was written for length and offset
            Position jspPos = null;
            int jspPosOffset = in.readInt();
            int jspPosLength = in.readInt();
            if(jspPosOffset != -1 && jspPosLength != -1) {
                jspPos = new Position(jspPosOffset, jspPosLength);
            }
            
            //only read a boolean if the jspPos was not null
            if(jspPos != null && in.readBoolean()) {
                jspPos.delete();
            }
            
            ranges.put(javaPos, jspPos);
        }
        
        return ranges;
    }
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        boolean equal = false;
        if(obj instanceof GSPTranslator) {
            GSPTranslator other = (GSPTranslator)obj;
            equal = this.fClassHeader.equals(other.fClassHeader) &&
                this.fClassname.equals(other.fClassname) &&
                this.fSuperclass.equals(other.fSuperclass) &&
                this.fImplicitImports.equals(other.fImplicitImports) &&
                this.fServiceHeader.equals(other.fServiceHeader) &&
                this.fGrailsActionParameters.equals(other.fGrailsActionParameters) &&
                buffersEqual(this.fUserImports, other.fUserImports) &&
                this.fSourcePosition == other.fSourcePosition &&
                this.fRelativeOffset == other.fRelativeOffset &&
                this.fCursorPosition == other.fCursorPosition &&
                this.fIsErrorPage == other.fIsErrorPage &&
                this.fCursorInExpression == other.fCursorInExpression &&
                this.fIsInASession == other.fIsInASession &&
                buffersEqual(this.fUserCode, other.fUserCode) &&
                buffersEqual(this.fUserELExpressions, other.fUserELExpressions) &&
                buffersEqual(this.fUserDeclarations, other.fUserDeclarations) &&
                buffersEqual(this.fCursorOwner, other.fCursorOwner) &&
                this.fInCodeRegion == other.fInCodeRegion &&
                this.fProcessIncludes == other.fProcessIncludes &&
                this.fOffsetInUserImports == other.fOffsetInUserImports &&
                this.fOffsetInUserDeclarations == other.fOffsetInUserDeclarations &&
                this.fOffsetInUserCode == other.fOffsetInUserCode &&
                rangesEqual(this.fImportRanges, other.fImportRanges) &&
                rangesEqual(this.fCodeRanges, other.fCodeRanges) &&
                rangesEqual(this.fDeclarationRanges, other.fDeclarationRanges) &&
                rangesEqual(this.fUseBeanRanges, other.fUseBeanRanges) &&
                rangesEqual(this.fUserELRanges, other.fUserELRanges) &&
                rangesEqual(this.fIndirectRanges, other.fIndirectRanges) &&
                (
                    (this.fELTranslatorID != null && other.fELTranslatorID != null) ||
                    (this.fELTranslatorID == null && other.fELTranslatorID != null && other.fELTranslatorID.length() == 0) ||
                    (this.fELTranslatorID != null && this.fELTranslatorID.length() == 0 && other.fELTranslatorID == null) ||
                    (this.fELTranslatorID.equals(other.fELTranslator))
                );
        }
        return equal;
    }
    
    /**
     * <p><code>null</code> is considered equivlent to an empty buffer</p>
     * 
     * @param buff1 can be <code>null</code>
     * @param buff2 can be <code>null</code>
     * @return <code>true</code> if the two given buffers are equal, <codee>false</code> otherwise
     */
    private static boolean buffersEqual(StringBuffer buff1, StringBuffer buff2) {
        return (buff1 == null && buff2 == null) ||
            (buff1 != null && buff2!= null && buff1.toString().equals(buff2.toString())) ||
            (buff1 == null && buff2 != null && buff2.length() == 0) ||
            (buff1 != null && buff1.length() == 0 && buff2 == null);
    }
    
    /**
     * @param ranges1
     * @param ranges2
     * @return <code>true</code> if the two maps of ranges contains the same key/value pares,
     * <code>false</code> otherwise
     */
    private static boolean rangesEqual(HashMap ranges1, HashMap ranges2) {
        //this is a strange hack because Position is not designed to be used as keys in a Map, see Position doc
        HashMap temp = new HashMap();
        temp.putAll(ranges1);
        ranges1 = temp;
        
        boolean equal = false;
        if(ranges1 != null && ranges2 != null) {
            equal = true;
            Iterator ranges1Keys = ranges1.keySet().iterator();
            while(ranges1Keys.hasNext() && equal) {
                Position key = (Position)ranges1Keys.next();
                Position ranges1Value = (Position)ranges1.get(key);
                Position ranges2Value = (Position)ranges2.get(key);
                equal = ranges1Value.equals(ranges2Value);
            }
        }
        
        return equal;
    }
    
    // GRAILS change
    // required so that a dummy JSPTranslator can be passed to parent classes
    // FIXADE Uggh...I don't like this, but having this method is the only way 
    // to avoid NPEs down the road.
    public JSPTranslator createMockJSPTranslator() {
        JSPTranslator mock = new JSPTranslator();
        IDOMDocument document = fStructuredModel == null ? null : fStructuredModel.getDocument();
        if (document == null) {
            document = new DOMDocumentForJSP();
        }
        mock.reset(document, new NullProgressMonitor());
        mock.translate();
        return mock;
    }
    
    /**
     * Add an assignment or declaration of a variable
     * @param fullTagName
     * @param container
     * @param regions 
     */
    private void addVariableAssignment(String fullTagName,
            ITextRegionCollection container) {
        
        // we only care about open tags
        if (! container.getFirstRegion().getType().equals(DOMRegionContext.XML_TAG_OPEN)) {
            return;
        }
        Iterator<ITextRegion> iter = container.getRegions().iterator();
        while (iter.hasNext()) {
            ITextRegion region = iter.next();
            if (region.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME) {
                String attrName = container.getText(region);
                if (iter.hasNext() && iter.next().getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS && iter.hasNext()) {
                    region = iter.next();
                    if (region.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {
                        if (attrName.equals("var")) {
                            String varName = container.getText(region);
                            // strip quotes
                            if (varName.length() > 2) {
                                varName = varName.substring(1, varName.length()-1);
                                String start = "\ndef ";
                                if (fullTagName.equals("g:def") || fullTagName.equals("g:set")) {
                                    appendToBuffer(start + varName + "\n", fUserCode, false, container);
                                } else if (fullTagName.equals("g:each")) {
                                    appendToBuffer(start + varName + "\n", fUserCode, false, container);
                                } else {
                                    // a g:set tag.  can ignore since we do not do the RHS.
                                }
                            }
                            // don't do value, always assume it is a string
//                        } else if (attrName.equals("value")) {
//                            varValue = container.getText(region);
                        }
                    } else {
                        translateXMLNode(container, iter);
                    }
                }
            }
        }
    }


    
    // GRAILS change end
}
