/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.ui.internal.utils;

/**
 * Helper class to represent an answer/result returned by some nested class code to its surrounding
 * scope. It is basically just a boxed value of some
 * other type. Typical use would be to be able to declare a final variable outside of a runnable
 * block so that the boxed value can be set from inside the runnable block. This is to
 * get around the restriction that a variables used from inside inner classes must be declared final.
 * 
 * @author Kris De Volder
 * @since 2.6
 */
public class Answer<T> {
	public T value = null;
	public Answer(T value) {
		this.value = value;
	}
	public Answer() {
		this(null);
	}
}