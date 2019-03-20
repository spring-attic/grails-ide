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
import pack.Other;
public class EvalSimpleTests {
    
	public static void main(String[] args) {
		byte xVarByte = -5;
		char xVarChar = (char)-5;
		short xVarShort = -5;
		int xVarInt = -5;
		long xVarLong = -5;
		float xVarFloat = (float)-5.3;
		double xVarDouble = -5.3;
		String xVarString = "minus five";
		boolean xVarBoolean = true;
//		Object xVarNull = null;

		byte yVarByte = 7;
		char yVarChar = 7;
		short yVarShort = 7;
		int yVarInt = 7;
		long yVarLong = 7;
		float yVarFloat = (float)6.9;
		double yVarDouble = 6.9;
		String yVarString = "seven";
		boolean yVarBoolean = false;
//		Object yVarNull = null;
		
        def xList = [1, 2, 3]
        def xMap = [a:1, b:2, c:3]
        
        def yList = ['a', 'b', 'c']
        def yMap = [d:4, e:5, f:6]
        
		println("tests ...");
	}
    
    static staticInt = 9
    
    static staticMethodInt() {
        9
    }
    
    static staticMethodWithArg(foo) {
        foo
    }
    
    class InnerClass {
        def val = ArrayList
    }
}

class OuterClass {
    def val = ArrayList
}
