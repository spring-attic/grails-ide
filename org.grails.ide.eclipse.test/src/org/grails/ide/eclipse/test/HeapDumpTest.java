package org.grails.ide.eclipse.test;

import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.springsource.ide.eclipse.commons.tests.util.HeapDumper;

/**
 * This isn't really a 'test'. Just something that creates a heapdump.
 * It is meant to run at the end of a very large test suite so that the dumped 
 * heap image can be analyzed for memory leaks.
 */
public class HeapDumpTest extends TestCase {

	public HeapDumpTest(String name) {
		super(name);
	}

	public void testDumpHeapImage() {
		HeapDumper.dumpHeap("grails-dump.hprof", true);
	}
	
}
