/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.editor.actions;

import java.util.List;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.ITextEditor;
import org.grails.ide.eclipse.core.GrailsCoreActivator;

import org.grails.ide.eclipse.editor.groovy.elements.GrailsWorkspaceCore;

/**
 * A hyperlink detector for the URL Mappings file.  See {@link #findLink(Statement, int, GroovyCompilationUnit)}
 * for a list of links that we look for.
 * 
 * @author Andrew Eisenberg
 * @since 2.8.0
 */
public class UrlMappingHyperlinkDetector extends AbstractHyperlinkDetector {
    private class NameRegion {
        final String name;
        final Region region;
        NameRegion(String name, Region region) {
            super();
            this.name = name;
            this.region = region;
        }
    }

    public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
            IRegion region, boolean canShowMultipleHyperlinks) {
        ITextEditor textEditor= (ITextEditor)getAdapter(ITextEditor.class);
        if (region == null || !(textEditor instanceof JavaEditor)) {
            return null;
        }
        
//        IAction openAction= textEditor.getAction("OpenEditor"); //$NON-NLS-1$
//        if (!(openAction instanceof SelectionDispatchAction)) {
//            return null;
//        }
//        
        ITypeRoot input= EditorUtility.getEditorInputJavaElement(textEditor, false);
        if (input == null) {
            return null;
        }
        
        if (! (input instanceof GroovyCompilationUnit)) {
            return null;
        }
        
        IResource resource = input.getResource();
        // we could get more specific and check to make sure that the file is 
        // in the proper package and source folder and in a grails project, but I think
        // it is useful here to have this functionality more widely available.
        if (resource == null || !resource.getName().equals("UrlMappings.groovy")) {
            return null;
        }
        
        GroovyCompilationUnit unit = (GroovyCompilationUnit) input;
        ModuleNode moduleNode = unit.getModuleNode();
        if (moduleNode == null) {
            return null;
        }
        
        ClassNode mappingClass = findMappingsClass(moduleNode);
        if (mappingClass == null) {
            return null;
        }
        
        FieldNode mappings = mappingClass.getField("mappings");
        if (mappings == null) {
            return null;
        }

        int offset= region.getOffset();
        if (mappings.getStart() > offset || mappings.getEnd() < offset) {
            return null;
        }
        Expression expression = mappings.getInitialExpression();
        if (! (expression instanceof ClosureExpression)) {
            return null;
        }
        Statement body = ((ClosureExpression) expression).getCode();
        if (! (body instanceof BlockStatement) || ((BlockStatement) body).getStatements() == null) {
            return null;
        }
        // now we know that we have a hyperlink request inside of a UrlMappings.mapping field.
        // we can do the real work now.
        return findMappingLinks(((BlockStatement) body).getStatements(), offset, unit);
    }

    private IHyperlink[] findMappingLinks(List<Statement> statements, int offset, GroovyCompilationUnit unit) {
        for (Statement statement : statements) {
            IHyperlink link = findLink(statement, offset, unit);
            if (link != null) {
                return new IHyperlink[] { link };
            }
        }
        return null;
    }

    /**
     * Handle these kinds of links:
     * <pre>
     * "/product"(controller:"product", action:"list") // link support to the controller and the action
     * "/product"(controller:"product")  // link support only for the controller
     * "/help"(controller:"site",view:"help") // link support to the controller and to the view (and maybe to the action as well)
     * "403"(view: "/errors/forbidden"  // link support to the view
     * name personList: "/showPeople" {
     *     controller = 'person'  // link support to the controller
     *     action = 'list'  // link support to the action
     * }
     * "/showPeople" {
     *     controller = 'person'  // link support to the controller
     *     action = 'list'  // link support to the action
     * }
     * "/product/$id"(controller:"product"){
     *    action = [GET:"show", PUT:"update", DELETE:"delete", POST:"save"]
     *  }    
     * </pre>
     * @param statements
     * @param offset
     * @return
     */
    private IHyperlink findLink(Statement statement, int offset, GroovyCompilationUnit unit) {
        if (! (statement instanceof ExpressionStatement)) {
            return null;
        }
        Expression expr  = ((ExpressionStatement) statement).getExpression();
        if (expr.getStart() > offset || expr.getEnd() < offset) {
            return null;
        }

        if (expr instanceof MethodCallExpression) {
            MethodCallExpression call = (MethodCallExpression) expr;
            Expression args = call .getArguments();
            if (! (args instanceof TupleExpression) || ((TupleExpression) args).getExpressions().size() == 0) {
                return null;
            }
            TupleExpression tuple = (TupleExpression) args;
            Expression firstArg = tuple.getExpression(0);
            Expression lastArg = tuple.getExpression(tuple.getExpressions().size()-1);
            
            NameRegion[] components;
            if (lastArg instanceof ClosureExpression && firstArg == lastArg) {
                /* we have something like this:
                 * "/showPeople" {
                 *     controller = 'person'  // link support to the controller
                 *     action = 'list'  // link support to the action
                 * }
                 */
                components = findLinkComponentsInClosure((ClosureExpression) lastArg, offset);
            } else if (firstArg instanceof MapExpression) {
                List<MapEntryExpression> mapEntryExpressions = ((MapExpression) firstArg).getMapEntryExpressions();
                if (mapEntryExpressions.size() > 0 && mapEntryExpressions.get(0).getValueExpression() instanceof MethodCallExpression) {
                    MethodCallExpression innerCall = (MethodCallExpression) mapEntryExpressions.get(0).getValueExpression();
                    if (innerCall.getArguments() instanceof ArgumentListExpression && ((ArgumentListExpression) innerCall.getArguments()).getExpressions().size() == 1 && ((ArgumentListExpression) innerCall.getArguments()).getExpression(0) instanceof ClosureExpression) {
                        /* we have something like this:
                         * name showPeople: "/showPeople" {
                         *     controller = 'person'  // link support to the controller
                         *     action = 'list'  // link support to the action
                         * }
                         */
                        
                        components = findLinkComponentsInClosure((ClosureExpression) ((ArgumentListExpression) innerCall.getArguments()).getExpression(0), offset);
                    } else {
                        components = null;
                    }
                } else {
                    /* we have something like this:
                     * "/product"(controller:"product", action:"list") // link support to the controller and the action
                     */
                    components = findLinkComponentsInCall((MapExpression) firstArg, offset);
                    if (components != null && lastArg instanceof ClosureExpression) {
                        /* we have something like this:
                         * "/product/$id"(controller:"product"){
                         *    action = [GET:"show", PUT:"update", DELETE:"delete", POST:"save"]
                         *  }
                         */
                        finishComponents((ClosureExpression) lastArg, components, offset);
                    }
                }
            } else {
                components = null;
            }

            if (components != null) {
                NameRegion controllerNameRegion = components[0];
                NameRegion actionNameRegion = components[1];
                NameRegion viewNameRegion = components[2];
                // may as well link to all possibilities here
                IHyperlink link = null;
                if (controllerNameRegion != null) {
                    IType type = findController(controllerNameRegion.name, unit.getJavaProject());
                    if (type != null && type.exists()) {
                        // action name should go first
                        if (actionNameRegion != null) {
                            IMember action = findAction(type, actionNameRegion.name);
                            if (actionNameRegion.region != null && action != null && action.exists()) {
                                link = new JavaElementHyperlink(actionNameRegion.region, action);
                            }
                        }
                        if (controllerNameRegion.region != null) {
                            link = new JavaElementHyperlink(controllerNameRegion.region, type);
                        }
                    }
                }
                if (viewNameRegion != null && viewNameRegion.region != null) {
                    String viewName = viewNameRegion.name;
                    // add a slash
                    if (viewName.charAt(0) != '/') {
                        viewName = "/" + viewName;
                    }
                    // add controller name
                    if (controllerNameRegion != null && ! viewName.startsWith("/" + controllerNameRegion.name + "/")) {
                        viewName = "/" + controllerNameRegion.name + viewName;
                    }
                    // add prefix
                    if (!viewName.endsWith(".gsp")) {
                        viewName = viewName + ".gsp";
                    }
                    IFile file = unit.getJavaProject().getProject().getFile("grails-app/views" + viewName);
                    if (file.exists()) {
                        link = new WorkspaceFileHyperlink(viewNameRegion.region, file);
                    }
                }
                return link;
            }
        }
        return null;
    }

    /**
     * find this kind of mapping:
     * action = [GET:"show", PUT:"update", DELETE:"delete", POST:"save"]
     * 
     * @param lastArg
     * @param components
     */
    private void finishComponents(ClosureExpression lastArg,
            NameRegion[] components, int offset) {
        if (! (lastArg.getCode() instanceof BlockStatement)) {
            return;
        }
        
        BlockStatement block = (BlockStatement) lastArg.getCode();
        for (Statement s : block.getStatements()) {
            if (s.getStart() < offset && s.getEnd() > offset) {
                if (s instanceof ExpressionStatement) {
                    Expression expr = ((ExpressionStatement) s).getExpression();
                    if (expr instanceof BinaryExpression && ((BinaryExpression) expr).getOperation().getText().equals("=")) {
                        BinaryExpression bexpr = (BinaryExpression) expr;
                        String mapping = null;
                        if (bexpr.getLeftExpression().getText().equals("action")) {
                            mapping = "action";
                        } else if (bexpr.getLeftExpression().getText().equals("view")) {
                            mapping = "view";
                        }
                        if (mapping != null && bexpr.getRightExpression() instanceof MapExpression) {
                            MapExpression mexpr = (MapExpression) bexpr.getRightExpression();
                            for (MapEntryExpression entry : mexpr.getMapEntryExpressions()) {
                                Expression value = entry.getValueExpression();
                                if (value.getStart() <= offset && value.getEnd() >= offset) {
                                    NameRegion nr = new NameRegion(value.getText(), new Region(value.getStart(), value.getLength()));
                                    if (mapping.equals("action")) {
                                        components[1] = nr;
                                    } else {
                                        components[2] = nr;
                                    }
                                }
                            }
                            
                        }
                    }
                }
            }
        }
    }

    private NameRegion[] findLinkComponentsInClosure(ClosureExpression firstArg,
            int offset) {
        if (! (firstArg.getCode() instanceof BlockStatement)) {
            return null;
        }
        
        BlockStatement code = (BlockStatement) firstArg.getCode();
        if (code.getStatements() == null) {
            return null;
        }
        NameRegion controllerName = null;
        NameRegion actionName = null;
        NameRegion viewName = null;

        for (Statement state : code.getStatements()) {
            if (state instanceof ExpressionStatement) {
                if (((ExpressionStatement) state).getExpression() instanceof BinaryExpression) {
                    BinaryExpression bexpr = (BinaryExpression) ((ExpressionStatement) state).getExpression();
                    Expression left = bexpr.getLeftExpression();
                    if (bexpr.getOperation().getText().equals("=") && left instanceof VariableExpression) {
                        Expression right = bexpr.getRightExpression();
                        Region region;
                        if (right.getStart() <= offset && right.getEnd() >= offset) {
                            region = new Region(right.getStart(), right.getLength());
                        } else {
                            region = null;
                        }

                        String name = left.getText();
                        if (name.equals("controller")) {
                            controllerName = new NameRegion(right.getText(), region);
                        } else if (name.equals("action")) {
                            actionName = new NameRegion(right.getText(), region);
                        } else if (name.equals("view")) {
                            viewName = new NameRegion(right.getText(), region);
                        }
                    }
                }
            }
        }
        return new NameRegion[] { controllerName, actionName, viewName };
    }

    private NameRegion[] findLinkComponentsInCall(MapExpression arguments, int offset) {
        NameRegion controllerName = null;
        NameRegion actionName = null;
        NameRegion viewName = null;
        
        List<MapEntryExpression> entries = arguments.getMapEntryExpressions();
        for (MapEntryExpression entry : entries) {
            Expression value = entry.getValueExpression();
            Region region;
            if (value.getStart() <= offset && value.getEnd() >= offset) {
                region = new Region(value.getStart(), value.getLength());
            } else {
                region = null;
            }
            
            Expression key = entry.getKeyExpression();
            String text = key.getText();
            if ("controller".equals(text)) {
                controllerName = new NameRegion(value.getText(), region);
            } else if ("action".equals(text)) {
                actionName =  new NameRegion(value.getText(), region);
            } else if ("view".equals(text)) {
                viewName =  new NameRegion(value.getText(), region);
            }
        }
        
        return new NameRegion[] { controllerName, actionName, viewName };
    }

    private IType findController(String controllerName, IJavaProject project) {
        try {
            return GrailsWorkspaceCore.get().create(project).findControllerFromSimpleName(controllerName);
        } catch (JavaModelException e) {
            GrailsCoreActivator.log(e);
        }
        return null;
    }

    private IMember findAction(IType type, String actionName) {
        try {
            for (IJavaElement child : type.getChildren()) {
                if (child.getElementName().equals(actionName)) {
                    // assume that the first time we find an element with the name, then we have found our match.
                    return (IMember) child;
                }
            }
        } catch (JavaModelException e) {
            GrailsCoreActivator.log(e);
        }
        return null;
    }

    private ClassNode findMappingsClass(ModuleNode moduleNode) {
        List<ClassNode> classes = moduleNode.getClasses();
        for (ClassNode clazz : classes) {
            if (clazz.getNameWithoutPackage().equals("UrlMappings")) {
                return clazz;
            }
        }
        return null;
    }
}
