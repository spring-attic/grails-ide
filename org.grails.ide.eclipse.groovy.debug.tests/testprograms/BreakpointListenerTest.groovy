/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

public class BreakpointListenerTest {

        public static void main(String[] args) {
                foo();  // conditional breakpoint here:  foo(); return false;
                System.out.println("out of foo");
        }

        private static void foo() {
                System.out.println("hello");  // breakpoint here
        }

}
