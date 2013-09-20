// COPIED_FROM org.eclipse.jst.jsp.core.internal.java.StackMap
/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     GoPivotal, Inc.    - augmented for use with Grails
 *******************************************************************************/
package org.grails.ide.eclipse.editor.gsp.translation.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

/**
 * @author Andrew Eisenberg
 */
class StackMap {

	private Map fInternalMap = null;

	public StackMap() {
		fInternalMap = new HashMap();
	}

	/**
	 * Removes all mappings from this StackMap
	 */
	public void clear() {
		fInternalMap.clear();
	}

	/**
	 * Returns the most recently pushed value to which this map maps the
	 * specified key. Returns <tt>null</tt> if the map contains no mapping
	 * for this key.
	 * 
	 * @param key
	 *            key whose associated value is to be returned.
	 * @return the most recently put value to which this map maps the
	 *         specified key, or <tt>null</tt> if the map contains no
	 *         mapping for this key.
	 */
	public Object peek(Object key) {
		Stack stack = (Stack) fInternalMap.get(key);
		if (stack != null) {
			Object o = stack.peek();
			if (stack.isEmpty()) {
				fInternalMap.remove(key);
			}
			return o;
		}
		return null;
	}

	/**
	 * Associates the specified value with the specified key in this map. If
	 * the map previously contained a mapping for this key, the old value is
	 * pushed onto the top of this key's private stack.
	 * 
	 * @param key
	 *            key with which the specified value is to be associated.
	 * @param value
	 *            value to be associated with the specified key.
	 * @return newest value associated with specified key
	 * 
	 * @throws UnsupportedOperationException
	 *             if the <tt>put</tt> operation is not supported by this
	 *             StackMap.
	 * @throws ClassCastException
	 *             if the class of the specified key or value prevents it from
	 *             being stored in this StackMap.
	 * @throws IllegalArgumentException
	 *             if some aspect of this key or value prevents it from being
	 *             stored in this StackMap.
	 * @throws NullPointerException,
	 *             as this map does not permit <tt>null</tt> keys or values
	 */
	public Object push(Object key, Object value) {
		Stack stack = (Stack) fInternalMap.get(key);
		if (stack == null) {
			stack = new Stack();
			fInternalMap.put(key, stack);
		}
		Object o = stack.push(value);
		return o;
	}

	/**
	 * Removes the most-recent mapping for this key from this StackMap if it
	 * is present.
	 * 
	 * <p>
	 * Returns the value to which the map previously associated the key, or
	 * <tt>null</tt> if the map contained no mapping for this key. The map
	 * will not contain a mapping for the specified key once the call returns.
	 * 
	 * @param key
	 *            key whose stack is to be popped
	 * @return most-recently pushed value associated with specified key, or
	 *         <tt>null</tt> if there was no mapping for key.
	 * 
	 * @throws ClassCastException
	 *             if the key is of an inappropriate type for this map.
	 * @throws NullPointerException
	 *             if the key is <tt>null</tt> as this class does not permit
	 *             <tt>null</tt> keys
	 */
	public Object pop(Object key) {
		Stack stack = (Stack) fInternalMap.get(key);
		if (stack != null) {
			Object o = stack.pop();
			if (stack.isEmpty()) {
				fInternalMap.remove(key);
			}
			return o;
		}
		return null;
	}

    /**
	 * Returns the number of entries in this StackMap, the sum of the sizes of
	 * every remembered stack.
	 * 
	 * @return the number of entries in this map.
	 */
	int size() {
		int size = 0;
		Iterator i = fInternalMap.values().iterator();
		while (i.hasNext()) {
			Collection c = (Collection) i.next();
			size += c.size();
		}
		return size;
	}

	/**
	 * Returns all of the values of this StackMap.
	 * 
	 * @return the values of every Stack within this StackMap.
	 */
	Collection values() {
		Collection values = new LinkedList();
		Iterator i = fInternalMap.values().iterator();
		while (i.hasNext()) {
			Collection c = (Collection) i.next();
			values.addAll(c);
		}
		return values;
	}
}
