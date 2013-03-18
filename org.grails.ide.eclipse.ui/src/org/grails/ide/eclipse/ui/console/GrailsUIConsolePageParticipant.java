package org.grails.ide.eclipse.ui.console;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;
import org.grails.ide.eclipse.longrunning.client.ExecutionEventSource;
import org.grails.ide.eclipse.longrunning.client.GrailsCommandExecution;
import org.grails.ide.eclipse.longrunning.client.ExecutionEventSource.ExecutionListener;

/**
 * Creates and manages process console specific actions
 * 
 * @since 3.1
 */
public class GrailsUIConsolePageParticipant implements IConsolePageParticipant {
	
	// actions
	private StopCommandAction fTerminate;

    private IPageBookViewPage fPage;

    private IConsoleView fView;

	private GrailsIOConsole fConsole;
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.console.IConsolePageParticipant#init(IPageBookViewPage, IConsole)
     */
    public void init(IPageBookViewPage page, IConsole console) {
        fPage = page;
        fConsole = (GrailsIOConsole) console;
        
//        fRemoveTerminated = new ConsoleRemoveLaunchAction(fConsole.getProcess().getLaunch());
//        fRemoveAllTerminated = new ConsoleRemoveAllTerminatedAction();
        fTerminate = new StopCommandAction(page.getSite().getWorkbenchWindow(), fConsole);
        
        fView = (IConsoleView) fPage.getSite().getPage().findView(IConsoleConstants.ID_CONSOLE_VIEW);
        
        // contribute to toolbar
        IActionBars actionBars = fPage.getSite().getActionBars();
        configureToolBar(actionBars.getToolBarManager());
        GrailsCommandExecution p = getProcess();
        if (p!=null) {
        	p.addExecutionListener(new ExecutionListener() {
				public void executionStateChanged(ExecutionEventSource target) {
					fTerminate.update();
				}
			});
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    public Object getAdapter(Class required) {
//        if (IShowInSource.class.equals(required)) {
//            return this;
//        }
//        if (IShowInTargetList.class.equals(required)) {
//            return this; 
//        }
//        //CONTEXTLAUNCHING
//        if(ILaunchConfiguration.class.equals(required)) {
//        	ILaunch launch = getProcess().getLaunch();
//        	if(launch != null) {
//        		return launch.getLaunchConfiguration();
//        	}
//        	return null;
//        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.console.IConsolePageParticipant#dispose()
     */
    public void dispose() {
//        DebugUITools.getDebugContextManager().getContextService(fPage.getSite().getWorkbenchWindow()).removeDebugContextListener(this);
//		DebugPlugin.getDefault().removeDebugEventListener(this);
//        if (fRemoveTerminated != null) {
//            fRemoveTerminated.dispose();
//            fRemoveTerminated = null;
//        }
//		if (fRemoveAllTerminated != null) {
//			fRemoveAllTerminated.dispose();
//			fRemoveAllTerminated = null;
//		}
//		if (fTerminate != null) {
//		    fTerminate.dispose();
//		    fTerminate = null;
//		}
//		if (fStdOut != null) {
//			fStdOut.dispose();
//			fStdOut = null;
//		}
//		if (fStdErr != null) {
//			fStdErr.dispose();
//			fStdErr = null;
//		}
		fConsole = null;
    }

    /**
     * Contribute actions to the toolbar
     */
    protected void configureToolBar(IToolBarManager mgr) {
		mgr.appendToGroup(IConsoleConstants.LAUNCH_GROUP, fTerminate);
//      mgr.appendToGroup(IConsoleConstants.LAUNCH_GROUP, fRemoveTerminated);
//		mgr.appendToGroup(IConsoleConstants.LAUNCH_GROUP, fRemoveAllTerminated);
//		mgr.appendToGroup(IConsoleConstants.OUTPUT_GROUP, fStdOut);
//		mgr.appendToGroup(IConsoleConstants.OUTPUT_GROUP, fStdErr);
    }

    
    protected GrailsCommandExecution getProcess() {
        return fConsole != null ? fConsole.getExecution() : null;
    }

	/* (non-Javadoc)
     * @see org.eclipse.ui.console.IConsolePageParticipant#activated()
     */
    public void activated() {
        // add EOF submissions
        IPageSite site = fPage.getSite();
        IHandlerService handlerService = (IHandlerService)site.getService(IHandlerService.class);
        IContextService contextService = (IContextService)site.getService(IContextService.class);
//        fActivatedContext = contextService.activateContext(fContextId);
//        fActivatedHandler = handlerService.activateHandler("org.eclipse.debug.ui.commands.eof", fEOFHandler); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.console.IConsolePageParticipant#deactivated()
     */
    public void deactivated() {
        // remove EOF submissions
        IPageSite site = fPage.getSite();
        IHandlerService handlerService = (IHandlerService)site.getService(IHandlerService.class);
        IContextService contextService = (IContextService)site.getService(IContextService.class);
//      handlerService.deactivateHandler(fActivatedHandler);
//		contextService.deactivateContext(fActivatedContext);
    }

}
