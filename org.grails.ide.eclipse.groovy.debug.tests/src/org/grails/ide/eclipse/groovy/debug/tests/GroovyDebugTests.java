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
package org.grails.ide.eclipse.groovy.debug.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.jdt.debug.core.IJavaClassObject;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaPrimitiveValue;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.tests.eval.Tests;

/**
 * @author Andrew Eisenberg
 */
public class GroovyDebugTests extends Tests {

    public GroovyDebugTests(String arg) {
        super(arg);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SanityChecker.assertJDTWeaving();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        end();
    }

    protected void init() throws Exception {
        initializeFrame("EvalSimpleTests", 43, 1);
    }

    protected void initClosure() throws Exception {
        // stack built differently in 2.0 and later
        if (GroovyUtils.GROOVY_LEVEL < 20) {
            initializeFrame("ClosureTests", 5, 43);
        } else {
            initializeFrame("ClosureTests", 5, 31);
        }
    }
    protected void initPackage() throws Exception {
        initializeFrame("pack.other.OtherPackageTests", 19, 1);
    }
    
    
    protected void end() throws Exception {
        destroyFrame();
    }

    // boolean | boolean

    public void testBooleanOrBoolean() throws Throwable {
        init();
        IValue value = eval(xBoolean + orOp + xBoolean);
        String typeName = value.getReferenceTypeName();
        assertEquals("boolean or boolean : wrong type : ", "java.lang.Boolean",
                typeName);
        boolean booleanValue = ((IJavaPrimitiveValue) ((IJavaObject) value)
                .getField("value", false).getValue()).getBooleanValue();
        assertEquals("boolean or boolean : wrong result : ", xBooleanValue
                | xBooleanValue, booleanValue);

        value = eval(xBoolean + orOp + yBoolean);
        typeName = value.getReferenceTypeName();
        assertEquals("boolean or boolean : wrong type : ", "java.lang.Boolean",
                typeName);
        booleanValue = ((IJavaPrimitiveValue) ((IJavaObject) value)
                .getField("value", false).getValue()).getBooleanValue();
        assertEquals("boolean or boolean : wrong result : ", xBooleanValue
                | yBooleanValue, booleanValue);

        value = eval(yBoolean + orOp + xBoolean);
        typeName = value.getReferenceTypeName();
        assertEquals("boolean or boolean : wrong type : ", "java.lang.Boolean",
                typeName);
        booleanValue = ((IJavaPrimitiveValue) ((IJavaObject) value)
                .getField("value", false).getValue()).getBooleanValue();
        assertEquals("boolean or boolean : wrong result : ", yBooleanValue
                | xBooleanValue, booleanValue);

        value = eval(yBoolean + orOp + yBoolean);
        typeName = value.getReferenceTypeName();
        assertEquals("boolean or boolean : wrong type : ", "java.lang.Boolean",
                typeName);
        booleanValue = ((IJavaPrimitiveValue) ((IJavaObject) value)
                .getField("value", false).getValue()).getBooleanValue();
        assertEquals("boolean or boolean : wrong result : ", yBooleanValue
                | yBooleanValue, booleanValue);
    }

    // test a bunch of groovy expressions in one go
    public void testGroovyExpressions1() throws Exception {
        init();
        boolean expectingBoxed = GroovyUtils.GROOVY_LEVEL < 18;
        // ternary operator
        assertInteger("xVarBoolean ? xVarInt : xVarInt*2", xVarIntValue, expectingBoxed);

        // ternary operator reversed
        assertInteger("yVarBoolean ? xVarInt : xVarInt*2", xVarIntValue*2, true);

        // ternary operator with groovy truth
        assertInteger("xVarString ? xVarInt : xVarInt*2", xVarIntValue, expectingBoxed);

        // elvis operator
        assertInteger("xVarInt ?: xVarInt*2", xVarIntValue, expectingBoxed);

        // unary ++
        assertInteger("xVarInt++", xVarIntValue+1, true);

        // unary --
        // should have decremented variable back to original value
        assertInteger("xVarInt--", xVarIntValue, true);

        assertInteger("xVarInt+=2\nxVarInt", xVarIntValue+2, true);

        // assignment
        assertInteger("def x = 9\nxVarInt = x\nxVarInt", 9, true);
    }

    public void testListAndMap() throws Exception {
        init();
        // literal list
        assertArrayList("[1, 2]", Arrays.asList(1, 2));

        // literal map
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("a", 1);
        map.put("b", 2);
        assertHashMap("[a:1, b:2]", map);

        // existing list
        assertArrayList("xList", Arrays.asList(1, 2, 3));

        // existing map
        map = new HashMap<String, Integer>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertHashMap("xMap", map);

        // left shift
        assertArrayList("[1,2] << 3", Arrays.asList(1, 2, 3));
        assertArrayList("xList<<4", Arrays.asList(1, 2, 3, 4));
    }

    public void testForLoop() throws Exception {
        init();
        assertArrayList("def newList = []\n" +
                "for (val in xList) {\n" +
                "newList << (val+1)\n" +
                "}\n" +
                "newList", Arrays.asList(2, 3, 4));
        assertArrayList("def newList = []\n" +
                "for (val in [1,2,3]) {\n" +
                "newList << (val+1)\n" +
                "}\n" +
                "newList", Arrays.asList(2, 3, 4));
    }

    public void testComparisons() throws Exception {
        init();
        assertBoolean("xVarInt < 5", true, true);
        assertBoolean("xVarInt <= 5", true, true);
        assertBoolean("xVarInt > 5", false, true);
        assertBoolean("xVarInt >= 5", false, true);
        assertBoolean("xVarInt == 5", false, true);
        assertBoolean("xVarInt != 5", true, true);
        assertBoolean("xVarInt === 5", false, true);
        assertBoolean("xVarInt !== 5", true, true);

        // not sure what should be here...
        //        assertBoolean("xVarInt <=> 5", true);
    }

    public void testIf() throws Exception {
        init();
        assertInteger("if (xList) {\n4 } else {\n5 }", 4, true);
        assertInteger("if (null) {\n5 } else {\n4 }", 4, true);
    }

    // failing because proxy should implement Comparable
    public void testWhile() throws Exception {
        init();
        assertInteger("def x = 0\n while(x < 5) {\nx++}\n x", 5, true);
        assertInteger("while(xVarInt < 5) {\nxVarInt++ }\n xVarInt", 5, true);
    }

    public void testFor() throws Exception {
        init();
        assertInteger("def y = 0\nfor (x in xList) {\ny += x}\n y", 6, true);
        assertInteger("def y = 0\nfor (x in 2..4) {\ny += x}\n y", 9, true);
        assertInteger("def y = 0\nfor (x in xMap.entrySet()) {\ny += x.value }\n y", 6, true);
    }

    public void testStatic() throws Exception {
        init();
        assertInteger("staticInt", 9, true);
        assertInteger("staticInt+=1", 10, true);
        assertInteger("staticInt", 10, true);  // should have been incremented previously
        assertInteger("staticMethodInt()", 9, true);
        assertInteger("1+staticMethodInt()", 10, true);
        assertInteger("staticMethodWithArg(staticInt)", 10, true);
    }
    
    public void testImportedClassReference() throws Exception {
        init();
        assertInteger("new Other().fieldX", 9, false);
        assertInteger("new Other().methodX()", 9, true);
        assertInteger("def var = new Other()\nvar.fieldX", 9, false);
        assertInteger("def var = new Other()\nvar.fieldX++\nvar.fieldX", 10, false);
        assertBoolean("new Other().fieldX < 100", true, true);
    }
    
    public void testNonImportedClassReference() throws Exception {
        init();
        assertInteger("new pack.OtherNoImport().fieldX", 9, false);
        assertInteger("new pack.OtherNoImport().methodX()", 9, true);
        assertInteger("def var = new pack.OtherNoImport()\nvar.fieldX", 9, false);
        assertInteger("def var = new pack.OtherNoImport()\nvar.fieldX++\nvar.fieldX", 10, false);
        assertBoolean("new pack.OtherNoImport().fieldX < 100", true, true);
    }
    
    public void testGString() throws Exception {
        init();
         assertInteger("def var = 9\n\"$var\".values[0]", 9, true);
    }
    
    public void testInnerClass() throws Exception {
        init();
        assertClass("new EvalSimpleTests.InnerClass().val", ArrayList.class);
    }
    
    public void testOuterClass() throws Exception {
        init();
        assertClass("new OuterClass().val", ArrayList.class);
    }
    
    public void testOtherClasses1() throws Exception {
        init();
        assertBoolean("new OuterClass().val == new EvalSimpleTests.InnerClass().val", true, true);
    }
    
    public void testOtherClasses2() throws Exception {
        init();
        assertInteger("def t = new OuterClass()\nt.val=[]\nt.val << 5\nt.val[0]", 5, true);
    }
    
    public void testInClosure1() throws Exception {
        initClosure();
        assertInteger("val1", 1, true);
        assertInteger("val2", 2, true);
        assertInteger("val4", 4, true);
        assertInteger("val5()", 5, true);
        assertInteger("this.val5()", 5, true);
        assertInteger("delegate.val5()", 5, true);
        assertInteger("owner.val5()", 5, true);
    }
    
    public void testOtherPackage1() throws Exception {
        initPackage();
        assertClass("outer.val", java.util.ArrayList.class);
    }
    public void testOtherPackage2() throws Exception {
        initPackage();
        assertClass("inner.val", java.util.ArrayList.class);
    }
    
    private void assertArrayList(String expression, List<Integer> asList) throws DebugException {
        IValue value = eval(expression);
        String typeName = value.getReferenceTypeName();
        assertEquals("wrong type : ", "java.util.ArrayList<E>",
                typeName);

        IJavaObject list = (IJavaObject) value;
        for (int i = 0; i < asList.size(); i++) {
            IJavaValue result = list.sendMessage("get", "(I)Ljava/lang/Object;", new IJavaValue[] { ((IJavaDebugTarget) list.getDebugTarget()).newValue(i) }, fSuspendeeThread, false);
            assertValueInteger(asList.get(i), result, true);
        }
    }

    private void assertHashMap(String expression, Map<String, Integer> asMap) throws DebugException {
        IValue value = eval(expression);
        String typeName = value.getReferenceTypeName();
        assertTrue("wrong type : java.util.HashMap<K,V> || java.util.LinkedHashMap<K,V>", "java.util.HashMap<K,V>".equals(typeName) 
                || "java.util.LinkedHashMap<K,V>".equals(typeName));

        IJavaObject map = (IJavaObject) value;
        for (Map.Entry<String, Integer> entry: asMap.entrySet()) {
            IJavaValue result = map.sendMessage("get", "(Ljava/lang/Object;)Ljava/lang/Object;", 
                    new IJavaValue[] { ((IJavaDebugTarget) map.getDebugTarget()).newValue(entry.getKey()) }, fSuspendeeThread, false);
            assertValueInteger(entry.getValue(), result, true);
        }
    }

    protected void assertClass(String expression, Class<?> expected) throws DebugException {
        IValue value = eval(expression);
        assertValueClass(expected, value);
    }
    
    protected void assertValueClass(Class<?> expected, IValue value)
    throws DebugException {
        String typeName2 = value.getReferenceTypeName();
        assertEquals("Wrong type", "java.lang.Class<T>", typeName2);
        IJavaType classType = ((IJavaClassObject) value).getInstanceType();
        
        assertEquals("wrong result : ", expected.getCanonicalName(), classType.getName());
    }

    protected void assertInteger(String expression, int expected, boolean isBoxed) throws DebugException {
        IValue value = eval(expression);
        assertValueInteger(expected, value, isBoxed);
    }

    protected void assertValueInteger(int expected, IValue value, boolean isBoxed)
    throws DebugException {
        String typeName2 = value.getReferenceTypeName();
        assertEquals("unary operator : wrong type : ", 
                isBoxed ? "java.lang.Integer" : "int",
                typeName2);
        int intValue2;
        
        if (isBoxed) {
            intValue2 = ((IJavaPrimitiveValue) ((IJavaObject) value)
                    .getField("value", false).getValue()).getIntValue();
        } else {
            intValue2 = ((IJavaPrimitiveValue) value).getIntValue();
        }
        assertEquals("wrong result : ", expected, intValue2);
    }

    protected void assertBoolean(String expression, boolean expected, boolean isBoxed) throws DebugException {
        IValue value = eval(expression);
        assertValueBoolean(expected, value, isBoxed);
    }

    protected void assertValueBoolean(boolean expected, IValue value, boolean isBoxed)
    throws DebugException {
        String typeName2 = value.getReferenceTypeName();
        assertEquals("unary operator : wrong type : ", isBoxed ? "java.lang.Boolean" : "boolean",
                typeName2);
        boolean booleanValue2;
        if (isBoxed) {
            booleanValue2 = ((IJavaPrimitiveValue) ((IJavaObject) value)
                    .getField("value", false).getValue()).getBooleanValue();
        } else {
            booleanValue2 = ((IJavaPrimitiveValue) value).getBooleanValue();
        }
        assertEquals("wrong result : ", expected, booleanValue2);
    }

}
