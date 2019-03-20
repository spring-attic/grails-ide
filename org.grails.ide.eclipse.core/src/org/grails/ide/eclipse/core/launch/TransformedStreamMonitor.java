/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.launch;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IFlushableStreamMonitor;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.internal.core.OutputStreamMonitor;

/**
 * Wraps around an existing IStreamMonitor and provides a view to its listeners that is transformed
 * in some way or another.
 * 
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class TransformedStreamMonitor implements IStreamMonitor, IFlushableStreamMonitor, IStreamListener {

	/**
	 * An instance of this class implements some transformation on a stream. It gets called eachtime 
	 * the original Stream is appended and decides when to pass on transformed data to a
	 * downstream StreamListener.
	 * 
	 * @author Kris De Volder
	 *
	 * @since 2.8
	 */
	public static class StreamTransformer {
		
		private TransformedStreamMonitor downstream;
		
		/**
		 * The framework will call this to initialise the transformer and setup its downstream connection to
		 * received data. This method is of no importance to clients and should not be called or overridden by
		 * client code.
		 */
		final private void init(TransformedStreamMonitor downstream) {
			Assert.isLegal(downstream!=null, "downstream param should not be null"); 
			Assert.isLegal(this.downstream==null, "Init called more than once"); 
			this.downstream = downstream; 
		}
		
		/**
		 * Implementers should call this method to pass on transformed data to the downstream client.
		 */
		protected final void send(String transformedText) {
			downstream.sendDownstream(transformedText);
		}
		
		/**
		 * Implementers should override this method to receive data from the upstream provider. A default
		 * implementation is provided that implements the 'identity' transform (i.e. it just passes any 
		 * received data unchanged to the downstream receiver.
		 */
		protected void receive(String text) {
			send(text);
		}
		
	}
		
//	/**
//	 * We register ourselves as a listener to this monitor.
//	 */
//	private IStreamMonitor wrappedMon;
	
	/**
	 * We will present 'transformed' output to these listeners.
	 */
	private ListenerList listeners = new ListenerList();
	
	/**
	 * Buffers up contents, may be null, if 'isBuffered' is false.
	 */
	private StringBuffer contents = createBuffer();
	
	/**
	 * When true, received output is appended to the buffer and can be retrieved
	 * by getContents
	 */
	private boolean isBuffered = true;

	/**
	 * Where we plugin the object that does the actual transformation of the Stream.
	 */
	private StreamTransformer transformer;

	public TransformedStreamMonitor(OutputStreamMonitor mon, StreamTransformer transformer) {
		this.transformer = transformer;
		transformer.init(this);
//		this.wrappedMon = mon;
		synchronized (mon) {
			//1: To avoid loosing output we must fetch the output that
			//may already have been produced before we registered as a listener.
			//2: To avoid duplicated output, we must lock the wrappedMonitor while
			//we do this.
			mon.addListener(this);
			Assert.isTrue(listeners.isEmpty(), "It should not be possible to have listeneres at this point yet.");
			//Since there are no listeners yet (listening to us), we don't need to notify listeners of the
			//already produced text... we just need to make sure we don't loose that text.
			transformer.receive(mon.getContents());
			mon.flushContents();
			mon.setBuffered(false); // otherwise it keeps buffering and eats up memory
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	//Implementation of IStreamMonitor

	private StringBuffer createBuffer() {
		return new StringBuffer(2048);
	}

	public synchronized void addListener(IStreamListener l) {
		listeners.add(l);
	}

	public synchronized String getContents() {
		return contents.toString();
	}

	public synchronized void removeListener(IStreamListener l) {
		listeners.remove(l);
	}

	////////////////////////////////////////////////////////////////////////////
	//Implementation of IFlushableStreamMonitor
	
	public synchronized void flushContents() {
		contents.setLength(0);
	}
	
	public synchronized void setBuffered(boolean buffer) {
		this.isBuffered = buffer;
	}

	public synchronized boolean isBuffered() {
		return isBuffered;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Implementation of IStreamListener (this repesents a connection upstream, to receive the original data).
	
	public synchronized void streamAppended(String text, IStreamMonitor monitor) {
//		Assert.isTrue(monitor==wrappedMon,
//				"I only registered myself as a listener with one monitor!");
		transformer.receive(text);
	} 

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// The code below represents the downstream connection where data is being sent by the transformer.
	
	public void sendDownstream(String transformedText) {
		//Note: no synch locks should be held by this method while calling the listeners.
		//(too risky for deadlock as we do not know what those listeners might be doing/locking)
		//Therefore make a local copy of the listeners.
		Object[] copiedListeners = listeners.getListeners();
		for (Object _l : copiedListeners) {
			IStreamListener l = (IStreamListener) _l;
			l.streamAppended(transformedText, this);
		}
	}

}
