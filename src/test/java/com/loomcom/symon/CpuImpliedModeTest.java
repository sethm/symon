package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryRangeException;
import junit.framework.*;

public class CpuImpliedModeTest extends TestCase {

	protected Cpu cpu;
	protected Bus bus;
	protected Memory mem;

	public void setUp() throws MemoryRangeException {
		this.cpu = new Cpu();
		this.bus = new Bus(0x0000, 0xffff);
		this.mem = new Memory(0x0000, 0x10000);
		bus.addCpu(cpu);
		bus.addDevice(mem);

		// Load the reset vector.
		bus.write(0xfffc, Cpu.DEFAULT_BASE_ADDRESS & 0x00ff);
		bus.write(0xfffd, (Cpu.DEFAULT_BASE_ADDRESS & 0xff00)>>>8);

		cpu.reset();
	}

	/*
	 * The following opcodes are tested for correctness in this file:
	 *
	 * BRK - $00
	 * CLC - $18
	 * CLD - $d8
	 * CLI - $58
	 * CLV - $B8
	 *
	 * DEX - $ca
	 * DEY - $88
	 * INX - $e8
	 * INY - $c8
	 * NOP - $ea
	 *
	 * PHA - $48
	 * PHP - $08
	 * PLA - $68
	 * PLP - $28
	 * RTI - $40
	 *
	 * RTS - $60
	 * SEC - $38
	 * SED - $f8
	 * SEI - $78
	 * TAX - $aa
	 *
	 * TAY - $a8
	 * TSX - $ba
	 * TXA - $8a
	 * TXS - $9a
	 * TYA - $98
	 */

	/* BRK Tests - 0x00 */

	public void test_BRK() {
		cpu.setCarryFlag();
		cpu.setOverflowFlag();
		assertEquals(0x20|Cpu.P_CARRY|Cpu.P_OVERFLOW,
								 cpu.getProcessorStatus());
		assertEquals(0xff, cpu.stackPeek());
		assertFalse(cpu.getBreakFlag());
		assertEquals(0x0200, cpu.getProgramCounter());
		assertEquals(0xff, cpu.getStackPointer());

		bus.loadProgram(0xea,
										0xea,
										0xea,
										0x00);

		cpu.step(3); // Three NOP instructions

		assertEquals(0x203, cpu.getProgramCounter());

		cpu.step(); // Triggers the BRK

		// Was at PC = 0x204, which should now be on the stack
		assertEquals(0x02, bus.read(0x1ff)); // PC high byte
		assertEquals(0x04, bus.read(0x1fe)); // PC low byte
		assertEquals(0x20|Cpu.P_CARRY|Cpu.P_OVERFLOW,
								 bus.read(0x1fd));       // Processor Status

		// Reset to original contents of PC
		assertEquals(0x0200, cpu.getProgramCounter());
		assertEquals(0xfc, cpu.getStackPointer());
		assertEquals(0x20|Cpu.P_CARRY|Cpu.P_OVERFLOW|Cpu.P_BREAK,
								 cpu.getProcessorStatus());
		assertEquals(0x20|Cpu.P_CARRY|Cpu.P_OVERFLOW,
								 cpu.stackPeek());
	}

	public void test_BRK_HonorsIrqDisableFlag() {
		cpu.setIrqDisableFlag();

		bus.loadProgram(0xea,
										0xea,
										0xea,
										0x00,
										0xea,
										0xea);

		cpu.step(3); // Three NOP instructions

		assertEquals(0x203, cpu.getProgramCounter());

		// Triggers the BRK, which should do nothing because
		// of the Interrupt Disable flag
		cpu.step();

		// Reset to original contents of PC
		assertEquals(0x0204, cpu.getProgramCounter());
		// Empty stack
		assertEquals(0xff, cpu.getStackPointer());

		cpu.step(2); // Two more NOPs

		// Reset to original contents of PC
		assertEquals(0x0206, cpu.getProgramCounter());
		// Empty stack
		assertEquals(0xff, cpu.getStackPointer());
	}

}