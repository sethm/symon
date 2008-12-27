package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryRangeException;
import junit.framework.TestCase;

public class CpuZeroPageModeTest extends TestCase {

	protected Cpu cpu;
	protected Bus bus;
	protected Memory mem;

	protected void setUp() throws Exception {
		this.cpu = new Cpu();
		this.bus = new Bus(0x0000, 0xffff);
		this.mem = new Memory(0x0000, 0x10000);
		bus.addCpu(cpu);
		bus.addDevice(mem);

		// Load the reset vector.
		bus.write(0xfffc, Cpu.DEFAULT_BASE_ADDRESS & 0x00ff);
		bus.write(0xfffd, (Cpu.DEFAULT_BASE_ADDRESS & 0xff00)>>>8);

		cpu.reset();
		// Assert initial state
		assertEquals(0, cpu.getAccumulator());
		assertEquals(0, cpu.getXRegister());
		assertEquals(0, cpu.getYRegister());
		assertEquals(0x200, cpu.getProgramCounter());
		assertEquals(0xff, cpu.getStackPointer());
		assertEquals(0x20, cpu.getProcessorStatus());
	}

	/*
	 * The following opcodes are tested for correctness in this file:
	 *
	 * ADC - $65
	 * AND - $25
	 * ASL - $06
	 * BIT - $24
	 * CMP - $c5
	 * 
	 * CPX - $e4
	 * CPY - $c4
	 * DEC - $c6
	 * EOR - $45
	 * INC - $e6
	 * 
	 * LDA - $a5
	 * LDX - $a6
	 * LDY - $a4
	 * LSR - $46
	 * ORA - $05
	 * 
	 * ROL - $26
	 * ROR - $66
	 * SBC - $e5
	 * STA - $85
	 * STX - $86
	 * 
	 * STY - $84
	 * 
	 */
	
	/* ADC - Add with Carry - $88 */
	public void test_ADC() {
		
	}
	
	/* AND - Logical AND - $25 */
	public void test_AND() {
		
	}
}
