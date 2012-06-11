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
package org.eclipse.jdt.debug.testplugin.detailpane;

import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.VariablesViewModelPresentation;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * An example detail pane that creates a composite containing several controls, all of which
 * will be displayed in the detail pane.
 * @author Andrew Eisenberg
 * @since 3.3
 * @see TestDetailPaneFactory
 * @see IDetailPane
 */
public class TableDetailPane implements IDetailPane, IValueDetailListener {

	public static final String ID = "org.eclipse.jdt.debug.testplugin.detailpane.TableDetailPane";
	public static final String NAME = "Example Pane: Table Detail Pane";
	public static final String DESCRIPTION = "Example detail pane that displays details as a composite containing several controls";
	
	IWorkbenchPartSite fWorkbenchPartSite;
	private Table fTable;
	private TextViewer fText;
	private Composite fComposite;
	private Button fWordWrapButton;
	private IDebugModelPresentation fModelPresentation;
	
	/**
	 * Job to calculate detail string
	 */
	class DetailJob implements Runnable{
		
		private IValue fValue;
		private IValueDetailListener fListener;
		
		public DetailJob(IValue value, IValueDetailListener listener){
			fValue = value;
			fListener = listener;
		}

		public void run() {
			fModelPresentation.computeDetail(fValue, fListener);		
		}
			
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#init(org.eclipse.ui.IWorkbenchPartSite)
	 */
	public void init(IWorkbenchPartSite workbenchPartSite) {
		fWorkbenchPartSite = workbenchPartSite;
		
		fModelPresentation = new VariablesViewModelPresentation();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public Control createControl(Composite parent) {
		fComposite = new Composite(parent, SWT.NONE);
		fComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		fComposite.setLayout(new FillLayout(SWT.HORIZONTAL));
		fTable = new Table(fComposite,SWT.FULL_SELECTION);
		fTable.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				int index = fTable.getSelectionIndex();
				if (index >= 0){
					TableItem item = fTable.getItem(index);
					if (item != null){
						Thread detailJob = new Thread(new DetailJob((IValue)item.getData(),getValueDetailListener()));
						detailJob.start();						
					}
					else{
						fText.setDocument(null);
					}
				}
			}
			
			
		});
		
		Composite composite = new Composite(fComposite,SWT.NONE);
		composite.setLayout(new FillLayout(SWT.VERTICAL));
		fText = new TextViewer(composite,SWT.READ_ONLY);
		
		fWordWrapButton = new Button(composite,SWT.CHECK);
		fWordWrapButton.setText("Word Wrap");
		fWordWrapButton.setSelection(false);
		fWordWrapButton.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
			public void widgetSelected(SelectionEvent e) {
				fText.getTextWidget().setWordWrap(fWordWrapButton.getSelection());
			}
		});

		return fComposite;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#display(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void display(IStructuredSelection element) {
		
		if (element != null){
			
			fTable.removeAll();
			fText.setDocument(null);
			
			Iterator iterator = element.iterator();
			while (iterator.hasNext()){
				
				Object selection = iterator.next();
				if (selection != null && selection instanceof IVariable){
				
					IValue val = null;
					try {
						
						val = ((IVariable)selection).getValue();
						TableItem newItem = new TableItem(fTable,SWT.NONE);
						newItem.setText(val.getValueString());
						newItem.setData(val);
					
					} catch (DebugException e) {
						
					}
				}
			}
		}
		else{
			fTable.removeAll();
		}

	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#setFocus()
	 */
	public boolean setFocus(){
		if (fText != null){
			fText.getTextWidget().setFocus();
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#dispose()
	 */
	public void dispose() {
		if (fComposite != null) fComposite.dispose();
		if (fModelPresentation != null) fModelPresentation.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#getDescription()
	 */
	public String getDescription() {
		return DESCRIPTION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#getID()
	 */
	public String getID() {
		return ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IDetailPane#getName()
	 */
	public String getName() {
		return NAME;
	}

	protected IValueDetailListener getValueDetailListener(){
		return this;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.IValueDetailListener#detailComputed(org.eclipse.debug.core.model.IValue, java.lang.String)
	 */
	public void detailComputed(IValue value, final String result){
		WorkbenchJob append = new WorkbenchJob("append details") { //$NON-NLS-1$
			public IStatus runInUIThread(IProgressMonitor monitor) {
				fText.setDocument(new Document(result));
				return Status.OK_STATUS;
				
			}
		};
		append.setSystem(true);
		append.schedule();
	}

}
