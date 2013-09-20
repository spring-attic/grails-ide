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
package org.grails.ide.eclipse.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * This is a ByteArrayOutputStream that puts an upper bound on the amount of
 * data that is kept in its buffer. If the allowed buffer size is exceeded, 
 * then the oldest data in the buffer is discarded to remain within the
 * allowed limit.
 * 
 * TODO: This is a poor implementation in terms of performance. A better implementation
 * would use a cyclic byte buffer which would never need to copy the contents of the buffer 
 * to shrink it down to size.
 * 
 * @author Kris De Volder
 */
public class LimitedByteArrayOutputStream extends ByteArrayOutputStream {

	private int MAX_BYTES;

	public LimitedByteArrayOutputStream(int maxBytes) {
		this.MAX_BYTES = maxBytes;
	}

	/**
	 * Shrink buffer to allowable size. The agressive flag controls whether we 
	 * must shrink the buffer aggressively or if it is ok to allow some excess data above the limit.
	 * Allowing for this 'margin' over the limit is to avoid excessive copying of the buffer on every 
	 * byte read.
	 * <p>
	 * Generally, the aggressive flag should be set to true when the buffer size or
	 * contents could be observed by the client calling the api method that is requesting
	 * buffer shrinkage. 
	 */
    private void shrink(boolean aggressive) {
    	boolean mustShrink = aggressive ? count>MAX_BYTES : count > MAX_BYTES + MAX_BYTES / 10;
    	if (mustShrink) {
    		System.arraycopy(buf, count-MAX_BYTES, buf, 0, MAX_BYTES);
    		count = MAX_BYTES;
    	}
	}

	
    public synchronized void write(int b) {
    	super.write(b);
    	shrink(false);
    }

	public synchronized void write(byte b[], int off, int len) {
    	super.write(b, off, len);
    	shrink(false);
    }
    
    @Override
    public synchronized int size() {
    	int actualSz = super.size();
    	if (actualSz>MAX_BYTES) {
    		return MAX_BYTES;
    	} else { 
    		return actualSz;
    	}
    }
    
    @Override
    public synchronized String toString() {
    	shrink(true);
    	return super.toString();
    }
    
    @Override
    public synchronized String toString(String charsetName)
    		throws UnsupportedEncodingException {
    	shrink(true);
    	return super.toString(charsetName);
    }

    @Override
    public synchronized void writeTo(OutputStream out) throws IOException {
    	shrink(true);
    	super.writeTo(out);
    }
    
}
