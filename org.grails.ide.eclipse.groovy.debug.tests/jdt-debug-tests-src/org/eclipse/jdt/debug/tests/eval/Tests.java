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
package org.eclipse.jdt.debug.tests.eval;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.eval.EvaluationManager;
import org.eclipse.jdt.debug.eval.IEvaluationEngine;
import org.eclipse.jdt.debug.eval.IEvaluationListener;
import org.eclipse.jdt.debug.eval.IEvaluationResult;
import org.eclipse.jdt.debug.tests.AbstractDebugTest;

/**
 * @author Andrew Eisenberg
 */
public abstract class Tests extends AbstractDebugTest {

	protected static final String xByte = "((byte)-3)";
	protected static final String xChar = "((char)-3)";
	protected static final String xShort = "((short)-3)";
	protected static final String xInt = "(-3)";
	protected static final String xLong = "(-3l)";
	protected static final String xFloat = "(-3.2f)";
	protected static final String xDouble = "(-3.2)";
	protected static final String xString = "\"minus three\"";
	protected static final String xBoolean = "true";
	protected static final String xNull = "null";
	
	static byte xByteValue = (byte)-3;
	static char xCharValue = (char)-3;
	static short xShortValue = (short)-3;
	static int xIntValue = -3;
	static long xLongValue = -3l;
	protected static final float xFloatValue = -3.2f;
	protected static final double xDoubleValue = -3.2;
	protected static final String xStringValue = "minus three";
	protected static final boolean xBooleanValue = true;
	protected static final Object xNullValue = null;

	protected static final String xVarByte = "xVarByte";
	protected static final String xVarChar = "xVarChar";
	protected static final String xVarShort = "xVarShort";
	protected static final String xVarInt = "xVarInt";
	protected static final String xVarLong = "xVarLong";
	protected static final String xVarFloat = "xVarFloat";
	protected static final String xVarDouble = "xVarDouble";
	protected static final String xVarString = "xVarString";
	protected static final String xVarBoolean = "xVarBoolean";
//	protected static final String xVarNull = "xVarNull";
	
	protected static final byte xVarByteValue = (byte)-5;
	protected static final char xVarCharValue = (char)-5;
	protected static final short xVarShortValue = (short)-5;
	protected static final int xVarIntValue = -5;
	protected static final long xVarLongValue = -5;
	protected static final float xVarFloatValue = (float)-5.3;
	protected static final double xVarDoubleValue = -5.3;
	protected static final String xVarStringValue = "minus five";
	protected static final boolean xVarBooleanValue = true;
//	protected static final Object xVarNullValue = null;

	protected static final String yByte = "((byte)8)";
	protected static final String yChar = "((char)8)";
	protected static final String yShort = "((short)8)";
	protected static final String yInt = "8";
	protected static final String yLong = "8l";
	protected static final String yFloat = "7.8f";
	protected static final String yDouble = "7.8";
	protected static final String yString = "\"eight\"";
	protected static final String yBoolean = "false";
	protected static final String yNull = "null";

	protected static final byte yByteValue = (byte)8;
	protected static final char yCharValue = (char)8;
	protected static final short yShortValue = (short)8;
	protected static final int yIntValue = 8;
	protected static final long yLongValue = 8;
	protected static final float yFloatValue = (float)7.8;
	protected static final double yDoubleValue = 7.8;
	protected static final String yStringValue = "eight";
	protected static final boolean yBooleanValue = false;
	protected static final Object yNullValue = null;
	
	protected static final String yVarByte = "yVarByte";
	protected static final String yVarChar = "yVarChar";
	protected static final String yVarShort = "yVarShort";
	protected static final String yVarInt = "yVarInt";
	protected static final String yVarLong = "yVarLong";
	protected static final String yVarFloat = "yVarFloat";
	protected static final String yVarDouble = "yVarDouble";
	protected static final String yVarString = "yVarString";
	protected static final String yVarBoolean = "yVarBoolean";
//	protected static final String yVarNull = "yVarNull";
	
	protected static final byte yVarByteValue = (byte)7;
	protected static final char yVarCharValue = (char)7;
	protected static final short yVarShortValue = (short)7;
	protected static final int yVarIntValue = 7;
	protected static final long yVarLongValue = 7;
	protected static final float yVarFloatValue = (float)6.9;
	protected static final double yVarDoubleValue = 6.9;
	protected static final String yVarStringValue = "seven";
	protected static final boolean yVarBooleanValue = false;
//	protected static final Object yVarNullValue = null;
	
	protected static final String xFieldByte = "xFieldByte";
	protected static final String xFieldChar = "xFieldChar";
	protected static final String xFieldShort = "xFieldShort";
	protected static final String xFieldInt = "xFieldInt";
	protected static final String xFieldLong = "xFieldLong";
	protected static final String xFieldFloat = "xFieldFloat";
	protected static final String xFieldDouble = "xFieldDouble";
	protected static final String xFieldString = "xFieldString";
	protected static final String xFieldBoolean = "xFieldBoolean";

	protected static final String yFieldByte = "yFieldByte";
	protected static final String yFieldChar = "yFieldChar";
	protected static final String yFieldShort = "yFieldShort";
	protected static final String yFieldInt = "yFieldInt";
	protected static final String yFieldLong = "yFieldLong";
	protected static final String yFieldFloat = "yFieldFloat";
	protected static final String yFieldDouble = "yFieldDouble";
	protected static final String yFieldString = "yFieldString";
	protected static final String yFieldBoolean = "yFieldBoolean";

	protected static final String xStaticFieldByte = "xStaticFieldByte";
	protected static final String xStaticFieldChar = "xStaticFieldChar";
	protected static final String xStaticFieldShort = "xStaticFieldShort";
	protected static final String xStaticFieldInt = "xStaticFieldInt";
	protected static final String xStaticFieldLong = "xStaticFieldLong";
	protected static final String xStaticFieldFloat = "xStaticFieldFloat";
	protected static final String xStaticFieldDouble = "xStaticFieldDouble";
	protected static final String xStaticFieldString = "xStaticFieldString";
	protected static final String xStaticFieldBoolean = "xStaticFieldBoolean";

	protected static final String yStaticFieldByte = "yStaticFieldByte";
	protected static final String yStaticFieldChar = "yStaticFieldChar";
	protected static final String yStaticFieldShort = "yStaticFieldShort";
	protected static final String yStaticFieldInt = "yStaticFieldInt";
	protected static final String yStaticFieldLong = "yStaticFieldLong";
	protected static final String yStaticFieldFloat = "yStaticFieldFloat";
	protected static final String yStaticFieldDouble = "yStaticFieldDouble";
	protected static final String yStaticFieldString = "yStaticFieldString";
	protected static final String yStaticFieldBoolean = "yStaticFieldBoolean";
	
	protected static final byte xFieldByteValue = -2;
	protected static final char xFieldCharValue = (char)-2;
	protected static final short xFieldShortValue = -2;
	protected static final int xFieldIntValue = -2;
	protected static final long xFieldLongValue = -2;
	protected static final float xFieldFloatValue = (float)-2.1;
	protected static final double xFieldDoubleValue = -2.1;
	protected static final String xFieldStringValue = "minus two";
	protected static final boolean xFieldBooleanValue = true;

	protected static final byte yFieldByteValue = 9;
	protected static final char yFieldCharValue = 9;
	protected static final short yFieldShortValue = 9;
	protected static final int yFieldIntValue = 9;
	protected static final long yFieldLongValue = 9;
	protected static final float yFieldFloatValue = (float)8.6;
	protected static final double yFieldDoubleValue = 8.6;
	protected static final String yFieldStringValue = "nine";
	protected static final boolean yFieldBooleanValue = false;

	protected static final byte xStaticFieldByteValue = -1;
	protected static final char xStaticFieldCharValue = (char)-1;
	protected static final short xStaticFieldShortValue = -1;
	protected static final int xStaticFieldIntValue = -1;
	protected static final long xStaticFieldLongValue = -1;
	protected static final float xStaticFieldFloatValue = (float)-1.5;
	protected static final double xStaticFieldDoubleValue = -1.5;
	protected static final String xStaticFieldStringValue = "minus one";
	protected static final boolean xStaticFieldBooleanValue = true;

	protected static final byte yStaticFieldByteValue = 6;
	protected static final char yStaticFieldCharValue = 6;
	protected static final short yStaticFieldShortValue = 6;
	protected static final int yStaticFieldIntValue = 6;
	protected static final long yStaticFieldLongValue = 6;
	protected static final float yStaticFieldFloatValue = (float)6.5;
	protected static final double yStaticFieldDoubleValue = 6.5;
	protected static final String yStaticFieldStringValue = "six";
	protected static final boolean yStaticFieldBooleanValue = false;
	
	protected static final String xArrayByte = "xArrayByte";
	protected static final String xArrayChar = "xArrayChar";
	protected static final String xArrayShort = "xArrayShort";
	protected static final String xArrayInt = "xArrayInt";
	protected static final String xArrayLong = "xArrayLong";
	protected static final String xArrayFloat = "xArrayFloat";
	protected static final String xArrayDouble = "xArrayDouble";
	protected static final String xArrayString = "xArrayString";
	protected static final String xArrayBoolean = "xArrayBoolean";

	protected static final String yArrayByte = "yArrayByte";
	protected static final String yArrayChar = "yArrayChar";
	protected static final String yArrayShort = "yArrayShort";
	protected static final String yArrayInt = "yArrayInt";
	protected static final String yArrayLong = "yArrayLong";
	protected static final String yArrayFloat = "yArrayFloat";
	protected static final String yArrayDouble = "yArrayDouble";
	protected static final String yArrayString = "yArrayString";
	protected static final String yArrayBoolean = "yArrayBoolean";

	protected static final byte[] xArrayByteValue = new byte[]{1, 2, 3};
	protected static final char[] xArrayCharValue = new char[]{1, 2, 3};
	protected static final short[] xArrayShortValue = new short[]{1, 2, 3};
	protected static final int[] xArrayIntValue = new int[]{1, 2, 3};
	protected static final long[] xArrayLongValue = new long[]{1, 2, 3};
	protected static final float[] xArrayFloatValue = new float[]{(float)1.2, (float)2.3, (float)3.4};
	protected static final double[] xArrayDoubleValue = new double[]{1.2, 2.3, 3.4};
	protected static final String[] xArrayStringValue = new String[]{"one", "two", "three"};
	protected static final boolean[] xArrayBooleanValue = new boolean[]{true, false, true};

	protected static final byte[] yArrayByteValue = new byte[]{7, 8, 9};
	protected static final char[] yArrayCharValue = new char[]{7, 8, 9};
	protected static final short[] yArrayShortValue = new short[]{7, 8, 9};
	protected static final int[] yArrayIntValue = new int[]{7, 8, 9};
	protected static final long[] yArrayLongValue = new long[]{7, 8, 9};
	protected static final float[] yArrayFloatValue = new float[]{(float)7.6, (float)8.7, (float)9.8};
	protected static final double[] yArrayDoubleValue = new double[]{7.6, 8.7, 9.8};
	protected static final String[] yArrayStringValue = new String[]{"seven", "eight", "nine"};
	protected static final boolean[] yArrayBooleanValue = new boolean[]{false, true, false};
		
	
	protected static final String plusOp = "+";
	protected static final String minusOp = "-";
	protected static final String multiplyOp = "*";
	protected static final String divideOp = "/";
	protected static final String remainderOp = "%";
	protected static final String greaterOp = ">";
	protected static final String greaterEqualOp = ">=";
	protected static final String lessOp = "<";
	protected static final String lessEqualOp = "<=";
	protected static final String equalEqualOp = "==";
	protected static final String notEqualOp = "!=";
	protected static final String leftShiftOp = "<<";
	protected static final String rightShiftOp = ">>";
	protected static final String unsignedRightShiftOp = ">>>";
	protected static final String orOp = "|";
	protected static final String andOp = "&";
	protected static final String xorOp = "^";
	protected static final String notOp = "!";
	protected static final String twiddleOp = "~";
	protected static final String equalOp = "=";
	protected static final String plusAssignmentOp = "+=";
	protected static final String minusAssignmentOp = "-=";
	protected static final String multiplyAssignmentOp = "*=";
	protected static final String divideAssignmentOp = "/=";
	protected static final String remainderAssignmentOp = "%=";
	protected static final String leftShiftAssignmentOp = "<<=";
	protected static final String rightShiftAssignmentOp = ">>=";
	protected static final String unsignedRightShiftAssignmentOp = ">>>=";
	protected static final String orAssignmentOp = "|=";
	protected static final String andAssignmentOp = "&=";
	protected static final String xorAssignmentOp = "^=";
	protected static final String prefixPlusPlusOp = "++";
	protected static final String postfixPlusPlusOp = "++";
	protected static final String prefixMinusMinusOp = "--";
	protected static final String postfixMinusMinusOp = "--";
	
	
	protected static final String aInt = "a";
	protected static final String bInt = "b";
	protected static final String cInt = "c";
	protected static final String dInt = "d";
	protected static final String eInt = "e";
	protected static final String fInt = "f";
	protected static final String gInt = "g";
	protected static final String hInt = "h";
	protected static final String iInt = "i";
	protected static final String jInt = "j";

	protected static final String aString = "aa";
	protected static final String bString = "bb";
	protected static final String cString = "cc";
	protected static final String dString = "dd";
	protected static final String eString = "ee";
	protected static final String fString = "ff";
	protected static final String gString = "gg";
	protected static final String hString = "hh";
	protected static final String iString = "ii";
	protected static final String jString = "jj";

	protected static final int aIntValue_0 = 1;
	protected static final int bIntValue_0 = 2;
	protected static final int cIntValue_0 = 3;
	protected static final int dIntValue_0 = 4;
	protected static final int eIntValue_0 = 5;
	protected static final int fIntValue_0 = 6;
	protected static final int aIntValue_1 = 1;
	protected static final int bIntValue_1 = 2;
	protected static final int cIntValue_1 = 37;
	protected static final int dIntValue_1 = 48;
	protected static final int eIntValue_1 = 5;
	protected static final int fIntValue_1 = 6;
	protected static final int gIntValue_1 = 7;
	protected static final int hIntValue_1 = 8;
	protected static final int aIntValue_2 = 1;
	protected static final int bIntValue_2 = 2;
	protected static final int cIntValue_2 = 379;
	protected static final int dIntValue_2 = 480;
	protected static final int eIntValue_2 = 59;
	protected static final int fIntValue_2 = 60;
	protected static final int gIntValue_2 = 7;
	protected static final int hIntValue_2 = 8;
	protected static final int iIntValue_2 = 9;
	protected static final int jIntValue_2 = 0;


	protected static final String aStringValue_0 = "one";
	protected static final String bStringValue_0 = "two";
	protected static final String cStringValue_0 = "three";
	protected static final String dStringValue_0 = "four";
	protected static final String eStringValue_0 = "five";
	protected static final String fStringValue_0 = "six";
	protected static final String aStringValue_1 = "one";
	protected static final String bStringValue_1 = "two";
	protected static final String cStringValue_1 = "three seven";
	protected static final String dStringValue_1 = "four eight";
	protected static final String eStringValue_1 = "five";
	protected static final String fStringValue_1 = "six";
	protected static final String gStringValue_1 = "seven";
	protected static final String hStringValue_1 = "eight";
	protected static final String aStringValue_2 = "one";
	protected static final String bStringValue_2 = "two";
	protected static final String cStringValue_2 = "three seven nine";
	protected static final String dStringValue_2 = "four eight zero";
	protected static final String eStringValue_2 = "five nine";
	protected static final String fStringValue_2 = "six zero";
	protected static final String gStringValue_2 = "seven";
	protected static final String hStringValue_2 = "eight";
	protected static final String iStringValue_2 = "nine";
	protected static final String jStringValue_2 = "zero";
	
	protected static final String EMPTY= "";
	protected static final String THIS= "this.";
	protected static final String T_T= "EvalNestedTypeTests.";
	protected static final String T_T_A= T_T + "A.";
	protected static final String T_A= "A.";
	protected static final String T_T_A_AA= T_T_A + "AA.";
	protected static final String T_A_AA= T_A + "AA.";
	protected static final String T_AA= "AA.";
	protected static final String T_T_A_AB= T_T_A + "AB.";
	protected static final String T_A_AB= T_A + "AB.";
	protected static final String T_AB= "AB.";
	protected static final String T_T_B= T_T + "B.";
	protected static final String T_B= "B.";
	protected static final String T_T_B_BB= T_T_B + "BB.";
	protected static final String T_B_BB= T_B + "BB.";
	protected static final String T_BB= "BB.";
	protected static final String T_C= "C.";
	protected static final String T_E= "E.";

	protected static final String T_T_this= T_T + "this.";
	protected static final String T_T_A_this= T_T_A + "this.";
	protected static final String T_A_this= T_A + "this.";
	protected static final String T_B_this= T_B + "this.";
	protected static final String T_C_this= T_C + "this.";
	protected static final String T_E_this= T_E + "this.";

	protected static final String I_A= "i_a.";
	protected static final String I_AA= "i_aa.";
	protected static final String I_AB= "i_ab.";
	protected static final String I_AC= "i_ac.";
	protected static final String I_AD= "i_ad.";
	protected static final String I_AE= "i_ae.";
	protected static final String I_AF= "i_af.";
	protected static final String I_B= "i_b.";
	protected static final String I_BB= "i_bb.";
	protected static final String I_BC= "i_bc.";
	protected static final String I_BD= "i_bd.";
	protected static final String I_C= "i_c.";
	protected static final String I_CB= "i_cb.";
	protected static final String I_CC= "i_cc.";
	protected static final String I_CD= "i_cd.";
	protected static final String I_D= "i_d.";
	protected static final String I_DB= "i_db.";
	protected static final String I_DC= "i_dc.";
	protected static final String I_DD= "i_dd.";
	protected static final String I_E= "i_e.";
	protected static final String I_EB= "i_eb.";
	protected static final String I_EC= "i_ec.";
	protected static final String I_ED= "i_ed.";
	protected static final String I_F= "i_f.";
	protected static final String I_FB= "i_fb.";
	protected static final String I_FC= "i_fc.";
	protected static final String I_FD= "i_fd.";
	

	/**
	 * Constructor for Tests.
	 * @param name
	 */
	public Tests(String name) {
		super(name);
	}

	static protected IJavaThread fSuspendeeThread;
	
	static protected IJavaStackFrame fFrame;
	
	static protected ICompilationUnit fCu;
	
	static protected IEvaluationEngine fEngine;
	

	static protected IValue eval(String command) {
		
		class Listener implements IEvaluationListener {
			IEvaluationResult fResult;
			
			public void evaluationComplete(IEvaluationResult result) {
				fResult= result;
			}
			
			public IEvaluationResult getResult() {
				return fResult;
			}
		}
		Listener listener= new Listener();
		try {
			fEngine.evaluate(command, fFrame, listener, DebugEvent.EVALUATION_IMPLICIT, false);
		} catch (DebugException e) {
			e.printStackTrace();
		}
		while (listener.fResult == null) {
			try {
				Thread.sleep(100);
			} catch(InterruptedException e) {
			}
		}
		IEvaluationResult result= listener.getResult();
		if (result.hasErrors()) {
			String message;
			DebugException exception= result.getException();
			if (exception == null) {
				message= IInternalDebugCoreConstants.EMPTY_STRING;
				String[] messages= result.getErrorMessages();
				for (int i= 0, limit= messages.length; i < limit; i++) {
					message += messages[i] + ", ";
				}
			} else {
				message= exception.getStatus().getMessage();
			}
			assertTrue(message, false);
		}
		return result.getValue();
	}
	
	protected void initializeFrame(String testClass, int breakPointLine, int numberFrames) throws Exception {
		fFrame = getStackFrame(breakPointLine, numberFrames, 0, 0, testClass);

		fEngine = getEvaluationEngine((IJavaDebugTarget)fFrame.getDebugTarget(), getJavaProject());
	}
	
	protected void initializeFrame(String testClass, int breakPointLine, int numberFrames, int hitCount) throws Exception {
		fFrame = getStackFrame(breakPointLine, numberFrames, 0, hitCount, testClass);

		fEngine = getEvaluationEngine((IJavaDebugTarget)fFrame.getDebugTarget(), getJavaProject());
	}
	
	protected void destroyFrame() throws Exception {
		try {
			terminateAndRemove(fSuspendeeThread);
		} finally {
			removeAllBreakpoints();
			if (fEngine != null) {
				fEngine.dispose();
			}
		}
		fFrame = null;
	}
	
	
	protected IEvaluationEngine getEvaluationEngine(IJavaDebugTarget vm, IJavaProject project) {
		IEvaluationEngine engine = EvaluationManager.newAstEvaluationEngine(project, vm);
		return engine;
	}		
	
	protected IJavaStackFrame getStackFrame(int breakpointLine, int numberFrames, int frameNumber, int hitCount, String testClass) throws Exception {
		IJavaLineBreakpoint breakpoint= createLineBreakpoint(breakpointLine, testClass);
		breakpoint.setHitCount(hitCount);
		fSuspendeeThread= launchToLineBreakpoint(testClass, breakpoint);
		IStackFrame[] stackFrames= fSuspendeeThread.getStackFrames();
		assertEquals("Should be " + numberFrames + " stack frame children, was: " + stackFrames.length, numberFrames, stackFrames.length);
		IStackFrame stackFrame= stackFrames[frameNumber];
		return (IJavaStackFrame)stackFrame;
	}
	
}
