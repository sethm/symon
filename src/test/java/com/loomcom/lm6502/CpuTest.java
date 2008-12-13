package com.loomcom.lm6502;

import junit.framework.*;

import com.loomcom.lm6502.devices.*;
import com.loomcom.lm6502.exceptions.*;

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
		bus.addDevice(new Memory(0x0000, 0x10000));

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
	
	/* LDA Immediate Mode Tests - 0xa9 */

	public void test_LDA_IMM_SetsAccumulator() {
		bus.write(0x0200, 0xa9);
		bus.write(0x0201, 0x12);
		cpu.step();
		assertEquals(0x12, cpu.getAccumulator());
	}
	
	public void test_LDA_IMM_SetsZeroFlagIfArgIsZero() {
		bus.write(0x0200, 0xa9);
		bus.write(0x0201, 0x00);
		cpu.step();
		assertTrue(cpu.getZeroFlag());
	}

	public void test_LDA_IMM_DoesNotSetZeroFlagIfArgNotZero() {
		bus.write(0x0200, 0xa9);
		bus.write(0x0201, 0x12);
		cpu.step();
		assertFalse(cpu.getZeroFlag());		
	}
	
	public void test_LDA_IMM_SetsNegativeFlagIfArgIsNegative() {
		bus.write(0x0200, 0xa9);
		bus.write(0x0201, 0x80);
		cpu.step();
		assertTrue(cpu.getNegativeFlag());
	}
	
	public void test_LDA_IMM_DoesNotSetNegativeFlagIfArgNotNegative() {
		bus.write(0x0200, 0xa9);
		bus.write(0x0201, 0x7f);
		cpu.step();
		assertFalse(cpu.getNegativeFlag());
	}

	/* LDX Immediate Mode Tests - 0xa2 */
	
	public void test_LDX_IMM_SetsXRegister() {
		bus.write(0x0200, 0xa2);
		bus.write(0x0201, 0x12);
		cpu.step();
		assertEquals(0x12, cpu.getXRegister());
	}
	
	public void test_LDX_IMM_SetsZeroFlagIfArgIsZero() {
		bus.write(0x0200, 0xa2);
		bus.write(0x0201, 0x00);
		cpu.step();
		assertTrue(cpu.getZeroFlag());
	}

	public void test_LDX_IMM_DoesNotSetZeroFlagIfArgNotZero() {
		bus.write(0x0200, 0xa2);
		bus.write(0x0201, 0x12);
		cpu.step();
		assertFalse(cpu.getZeroFlag());		
	}
	
	public void test_LDX_IMM_SetsNegativeFlagIfArgIsNegative() {
		bus.write(0x0200, 0xa2);
		bus.write(0x0201, 0x80);
		cpu.step();
		assertTrue(cpu.getNegativeFlag());
	}
	
	public void test_LDX_IMM_DoesNotSetNegativeFlagIfArgNotNegative() {
		bus.write(0x0200, 0xa2);
		bus.write(0x0201, 0x7f);
		cpu.step();
		assertFalse(cpu.getNegativeFlag());
	}

	/* LDY Immediate Mode Tests - 0xa0 */
	
	public void test_LDY_IMM_SetsYRegister() {
		bus.write(0x0200, 0xa0);
		bus.write(0x0201, 0x12);
		cpu.step();
		assertEquals(0x12, cpu.getYRegister());
	}
	
	public void test_LDY_IMM_SetsZeroFlagIfArgIsZero() {
		bus.write(0x0200, 0xa0);
		bus.write(0x0201, 0x00);
		cpu.step();
		assertTrue(cpu.getZeroFlag());
	}

	public void test_LDY_IMM_DoesNotSetZeroFlagIfArgNotZero() {
		bus.write(0x0200, 0xa0);
		bus.write(0x0201, 0x12);
		cpu.step();
		assertFalse(cpu.getZeroFlag());		
	}
	
	public void test_LDY_IMM_SetsNegativeFlagIfArgIsNegative() {
		bus.write(0x0200, 0xa0);
		bus.write(0x0201, 0x80);
		cpu.step();
		assertTrue(cpu.getNegativeFlag());
	}
	
	public void test_LDY_IMM_DoesNotSetNegativeFlagIfArgNotNegative() {
		bus.write(0x0200, 0xa0);
		bus.write(0x0201, 0x7f);
		cpu.step();
		assertFalse(cpu.getNegativeFlag());
	}
}