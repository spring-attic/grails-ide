package org.grails.ide.eclipse.longrunning.client;

import org.eclipse.core.runtime.ListenerList;

abstract public class ExecutionEventSource {

	public interface ExecutionListener {
		void executionStateChanged(ExecutionEventSource target);
	}
	
	private ListenerList executionListeners = new ListenerList();

	protected void notifyExecutionListeners() {
		for (Object _l : executionListeners.getListeners()) {
			ExecutionListener l = (ExecutionListener)_l;
			l.executionStateChanged(this);
		}
	}
	
	public void addExecutionListener(ExecutionListener l) {
		executionListeners.add(l);
	}

	
}
