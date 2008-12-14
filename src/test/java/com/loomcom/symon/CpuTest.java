package com.loomcom.symon;

import junit.framework.*;

import com.loomcom.symon.devices.*;
import com.loomcom.symon.exceptions.*;

/**
 *
 */
public class CpuTest extends TestCase {
	
	private Cpu cpu;
	private Bus bus;
	private Memory mem;
	
	public CpuTest(String testName) {
		super(testName);
	}

	public static Test suite() {
		return new TestSuite(CpuTest.class);
	}
	
	public void setUp() throws MemoryRangeException {
		this.cpu = new Cpu();
		this.bus = new Bus(0x0000, 0xffff);
		this.mem = new Memory(0x0000, 0x10000);
		bus.addCpu(cpu);
		bus.addDevice(mem);

		// All test programs start at 0x0200;
		bus.write(0xfffc, 0x00);
		bus.write(0xfffd, 0x02);

		cpu.reset();
	}
	
	public void testReset() {
		assertEquals(0, cpu.getAccumulator());
		assertEquals(0, cpu.getXRegister());
		assertEquals(0, cpu.getYRegister());
		assertEquals(0x0200, cpu.getProgramCounter());
		assertFalse(cpu.getCarryFlag());
		assertFalse(cpu.getZeroFlag());
		assertFalse(cpu.getIrqDisableFlag());
		assertFalse(cpu.getDecimalModeFlag());
		assertFalse(cpu.getBreakFlag());
		assertFalse(cpu.getOverflowFlag());
		assertFalse(cpu.getNegativeFlag());
	}
}