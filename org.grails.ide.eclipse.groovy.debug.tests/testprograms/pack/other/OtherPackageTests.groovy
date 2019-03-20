/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package pack.other
public class OtherPackageTests {
    
	public static void main(String[] args) {
        
        def inner = new InnerClass()
        def outer = new OuterClass()
        
        println 0
	}
    
    static class InnerClass {
        def val = ArrayList
    }
}

class OuterClass {
    def val = ArrayList
}
