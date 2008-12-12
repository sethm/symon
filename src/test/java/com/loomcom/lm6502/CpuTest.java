package com.loomcom.lm6502;

import junit.framework.*;

/**
 *
 */
public class CpuTest extends TestCase {

	public CpuTest(String testName) {
		super(testName);
	}

	public static Test suite() {
		return new TestSuite(CpuTest.class);
	}

	public void testCpu() {
		Cpu cpu = new Cpu();
		assertNotNull(cpu);
	}

}