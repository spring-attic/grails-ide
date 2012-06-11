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
package org.grails.ide.eclipse.refactoring.rename.ui;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.eclipse.refactoring.actions.IRenameTarget;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.ITextViewerExtension6;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.IUndoManagerExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.grails.ide.eclipse.core.GrailsCoreActivator;

import org.grails.ide.eclipse.editor.groovy.elements.IGrailsElement;
import org.grails.ide.eclipse.refactoring.rename.type.GrailsTypeRenameRefactoring;

/**
 * @author Kris De Volder
 * @since 2.7
 */
public class GrailsTypeRenameTarget implements IRenameTarget {

	private static final boolean DEBUG = false;

	private static void debug(String string) {
		if (DEBUG) {
			System.out.println(string);
		}
	}
	
	/**
	 * This listener is active while we are performing a refactoring action triggered from the
	 * editor and its purpose is to add the editor as an undo context to the operations that
	 * were triggered from the editor. Also it 'deletes' bogus undos that were added because
	 * of https://bugs.eclipse.org/bugs/show_bug.cgi?id=345342
	 * <p>
	 * Normally, we shouldn't have to do any of this, and presumably this stuff will be obsolete
	 * if the bug gets fixed.
	 */
	public class OperationListener implements IOperationHistoryListener {
		
		/**
		 * This collects the bogus undo operations added because of bug:
		 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=345342
		 */
		private List<IUndoableOperation> bogusUndos = new ArrayList<IUndoableOperation>();
		
		public OperationListener() {
			OperationHistoryFactory.getOperationHistory().addOperationHistoryListener(this);
		}

		public void historyNotification(OperationHistoryEvent event) {
			if (event.getEventType() == OperationHistoryEvent.OPERATION_ADDED) { 
				IUndoableOperation oper = event.getOperation();
				debug("Undoable operation : " + oper);
				if (isBogus(oper)) {
					debug("bogus undo detected and remembered!");
					bogusUndos.add(oper);
				} else {
					IUndoContext context = getUndoContext();
					if (context!=null) {
						oper.addContext(getUndoContext());
					}
				}
			}
		}

		/**
		 * Recognizes the bogus undos added because of bug
		 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=345342
		 */
		private boolean isBogus(IUndoableOperation oper) {
			return oper.toString().contains("org.eclipse.text.undo.DocumentUndoManager$UndoableTextChange undo modification stamp: -1 ");
		}

		public void dispose() {
			OperationHistoryFactory.getOperationHistory().removeOperationHistoryListener(this);
			removeBogusUndos();
		}

		/**
		 * Work around for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=345342
		 */
		private void removeBogusUndos() {
			IOperationHistory hist = OperationHistoryFactory.getOperationHistory();
			final IUndoableOperation[] NO_UNDOS = new IUndoableOperation[0];
			for (IUndoableOperation bogusUndo : bogusUndos) {
				debug("deleting bogus undo: "+bogusUndo);
				IUndoContext[] contexts = bogusUndo.getContexts();
				for (IUndoContext c : contexts) {
					bogusUndo.removeContext(c); 
					// Deleting all contexts makes the undo as good as removed since it can't be triggered from any context now.
				}
			}
		}
	}

	private IGrailsElement targetElement;
	private ICompilationUnit cu;
	private IType type;
	private IUndoContext undoContext;
	
	private IUndoContext getUndoContext() {
		return this.undoContext;
	}

	public GrailsTypeRenameTarget(IGrailsElement target) throws JavaModelException {
		//TODO: stuff in here can go wrong and we aren't handling it
		this.targetElement = target;
		this.cu = target.getCompilationUnit();
		this.type = cu.getAllTypes()[0];
	}


	private void initUndoContext(AbstractTextEditor _editor) {
		if (_editor instanceof JavaEditor) {
			JavaEditor editor = (JavaEditor) _editor;
			ISourceViewer viewer = editor.getViewer();
			//		IDocument document= viewer.getDocument();
			//		fOriginalSelection= viewer.getSelectedRange();
			//		int offset= fOriginalSelection.x;

			try {
				//			CompilationUnit root= SharedASTProvider.getAST(getCompilationUnit(), SharedASTProvider.WAIT_YES, null);

				//			fLinkedPositionGroup= new LinkedPositionGroup();
				//			ASTNode selectedNode= NodeFinder.perform(root, fOriginalSelection.x, fOriginalSelection.y);
				//			if (! (selectedNode instanceof SimpleName)) {
				//				return; // TODO: show dialog
				//			}
				//			SimpleName nameNode= (SimpleName) selectedNode;

				if (viewer instanceof ITextViewerExtension6) {
					IUndoManager undoManager= ((ITextViewerExtension6)viewer).getUndoManager();
					if (undoManager instanceof IUndoManagerExtension) {
						IUndoManagerExtension undoManagerExtension= (IUndoManagerExtension)undoManager;
						this.undoContext = undoManagerExtension.getUndoContext();
						//					IOperationHistory operationHistory= OperationHistoryFactory.getOperationHistory();
						//					fStartingUndoOperation= operationHistory.getUndoOperation(undoContext);
					}
				}
			} catch (Exception e) {
				GrailsCoreActivator.log(e);
			}
		}
	}
	
	public boolean performRenameAction(Shell shell, AbstractTextEditor editor, boolean lightweight) {
		Assert.isLegal(undoContext==null, "Nested or concurrent 'performRenameAction' is not allowed");
		OperationListener operationListener = null;
		initUndoContext(editor);

		try {
			GrailsTypeRenameRefactoring refactoring = new GrailsTypeRenameRefactoring(type);
			GrailsTypeRenameWizard wizard = new GrailsTypeRenameWizard(refactoring);
			if (getUndoContext()!=null) {
				operationListener = new OperationListener();
			}
			try {
				RefactoringWizardOpenOperation operation= new RefactoringWizardOpenOperation(wizard);
				operation.run(shell, refactoring.getName());
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
			}
			return true;
		} catch (Exception e) {
			GrailsCoreActivator.log(e);
			return false;
		} finally {
			undoContext = null;
			if (operationListener!=null) {
				operationListener.dispose();
			}
		}
	}
}
