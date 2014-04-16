/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.core.launch;

import java.io.IOException;
import java.util.Map;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.IStreamsProxy2;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.debug.internal.core.OutputStreamMonitor;


/**
 * Replaces RuntimeProcess, so that we can manipulate/transform the output streams from the process.
 * 
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class GrailsRuntimeProcess extends RuntimeProcess {
	
	public static class StreamsProxy implements IStreamsProxy, IStreamsProxy2 {

		public StreamsProxy(IStreamMonitor out, IStreamMonitor err, IStreamsProxy in) {
			super();
			this.out = out;
			this.err = err;
			this.in = in;
		}

		private IStreamMonitor out;
		private IStreamMonitor err;
		private IStreamsProxy in; // Only cares about the 'write' method.

		public IStreamMonitor getErrorStreamMonitor() {
			return err;
		}

		public IStreamMonitor getOutputStreamMonitor() {
			return out;
		}

		public void write(String input) throws IOException {
			in.write(input);
		}

		public void closeInputStream() throws IOException {
			if (in instanceof IStreamsProxy2) {
				((IStreamsProxy2) in).closeInputStream();
			}
		}
		
	}

	public static final String NEWLINE = System.getProperty("line.separator");
	private IStreamsProxy mustCloseStreamProxy;

	public GrailsRuntimeProcess(ILaunch launch, Process process, String label, Map attributes) {
		super(launch, process, label, attributes);
	}

	@Override
	protected IStreamsProxy createStreamsProxy() {
		mustCloseStreamProxy = super.createStreamsProxy();
		//We must make sure that the streams proxy created from super is closed when process terminates.
		//This won't happen automatically because our StreamsProxy, which wraps it doesn't extend the 
		//org.eclipse.debug.internal.core.StreamsProxy class.
		return removeDuplicates(mustCloseStreamProxy);
	}
	
	@Override
	public void terminate() throws DebugException {
		if (!isTerminated()) {
			//If we don't forcibly destroy the StreamsProxy then, on Windows the
			// 'terminated()' method may hang when it tries to close the streams
			// nicely. This will stop it from properly closing resources
			// and firing a termination event. Visibly to the user: 'Stop' button 
			// won't work.
			//See https://issuetracker.springsource.com/browse/STS-3800
			if (mustCloseStreamProxy instanceof org.eclipse.debug.internal.core.StreamsProxy) {
				((org.eclipse.debug.internal.core.StreamsProxy) mustCloseStreamProxy).kill();
			}
		}
		super.terminate();
	}
	
	@Override
	protected void terminated() {
		try {
			if (mustCloseStreamProxy instanceof org.eclipse.debug.internal.core.StreamsProxy) {
				((org.eclipse.debug.internal.core.StreamsProxy) mustCloseStreamProxy).close();
			}
		} finally {
			mustCloseStreamProxy = null;
			super.terminated();
		}
	}
	
	private IStreamsProxy removeDuplicates(IStreamsProxy streamsProxy) {
		return new StreamsProxy(removeDuplicates(streamsProxy.getOutputStreamMonitor()), 
				streamsProxy.getErrorStreamMonitor(), streamsProxy);
	}

	/**
	 * Removes 'duplicates' as follows: 
	 * <p>
	 * When the next line is just a copy of the previous line with some extra text added to the end...
	 * Then the next line is not printed, instead, the extra characters are appended to the previous line.
	 */
	private IStreamMonitor removeDuplicates(IStreamMonitor mon) {
		return new TransformedStreamMonitor((OutputStreamMonitor) mon, new Grails20OutputCleaner());
	}

	
}
