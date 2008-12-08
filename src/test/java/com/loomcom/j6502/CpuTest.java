/**
 *
 */
package com.loomcom.j6502;

import junit.framework.*;

import java.util.HashMap;
import java.util.Map;

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
		Cpu cpu = new Cpu(new Simulator());
		assertNotNull(cpu);
	}

	public void testGetSimulator() {
		Simulator sim = new Simulator();
		Cpu cpu = new Cpu(sim);
		assertEquals(sim, cpu.getSimulator());
	}

}