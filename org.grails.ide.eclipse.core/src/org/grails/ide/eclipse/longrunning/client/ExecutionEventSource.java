package org.grails.ide.eclipse.longrunning.client;

import org.eclipse.core.runtime.ListenerList;

abstract public class ExecutionEventSource {

	public interface ExecutionListener {
		void executionStateChanged(ExecutionEventSource target);
	}
	
	private ListenerList executionListeners = new ListenerList();

	protected void notifyExecutionListeners() {
		if (executionListeners!=null) {
			for (Object _l : executionListeners.getListeners()) {
				ExecutionListener l = (ExecutionListener)_l;
				l.executionStateChanged(this);
			}
		}
	}
	
	public void addExecutionListener(ExecutionListener l) {
		if (executionListeners!=null) {
			executionListeners.add(l);
		}
	}

	/**
	 * Remove all listeners, also any listeners added in the future will be discarded
	 * immediately. 
	 * 
	 * This method should be called after the target has terminated. When a target has
	 * terminated no further execution events are possible so no need to hold on to
	 * listeners.
	 */
	protected void clearListeners() {
		executionListeners = null;
	}
	
}
