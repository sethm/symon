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
	 * ORA - $05
	 * ASL - $06
	 * BIT - $24
	 * AND - $25
	 * ROL - $26
	 *
	 * EOR - $45
	 * LSR - $46
	 * ADC - $65
	 * ROR - $66
	 * STY - $84
	 *
	 * STA - $85
	 * STX - $86
	 * LDY - $a4
	 * LDA - $a5
	 * LDX - $a6
	 *
	 * CPY - $c4
	 * CMP - $c5
	 * DEC - $c6
	 * CPX - $e4
	 * SBC - $e5
	 *
	 * INC - $e6
	 */

	/* ORA - Logical Inclusive OR - $05 */
	public void test_ORA() {
		// Set some initial values in zero page.
		bus.write(0x0000, 0x00);
		bus.write(0x0002, 0x11);
		bus.write(0x0004, 0x22);
		bus.write(0x0008, 0x44);
		bus.write(0x0010, 0x88);

		bus.loadProgram(0x05, 0x00,  // ORA $00
		                0x05, 0x02,  // ORA $02
		                0x05, 0x04,  // ORA $04
		                0x05, 0x08,  // ORA $08
		                0x05, 0x10); // ORA $10
		cpu.step();
		assertEquals(0x00, cpu.getAccumulator());
		assertTrue(cpu.getZeroFlag());
		assertFalse(cpu.getNegativeFlag());

		cpu.step();
		assertEquals(0x11, cpu.getAccumulator());
		assertFalse(cpu.getZeroFlag());
		assertFalse(cpu.getNegativeFlag());

		cpu.step();
		assertEquals(0x33, cpu.getAccumulator());
		assertFalse(cpu.getZeroFlag());
		assertFalse(cpu.getNegativeFlag());

		cpu.step();
		assertEquals(0x77, cpu.getAccumulator());
		assertFalse(cpu.getZeroFlag());
		assertFalse(cpu.getNegativeFlag());

		cpu.step();
		assertEquals(0xff, cpu.getAccumulator());
		assertFalse(cpu.getZeroFlag());
		assertTrue(cpu.getNegativeFlag());
	}

	/* ASL - Arithmetic Shift Left - $06 */
	public void test_ASL() {
		bus.write(0x0000, 0x00);
		bus.write(0x0001, 0x01);
		bus.write(0x0002, 0x02);
		bus.write(0x0003, 0x44);
		bus.write(0x0004, 0x80);

		bus.loadProgram(0x06, 0x00,
										0x06, 0x01,
										0x06, 0x02,
										0x06, 0x03,
										0x06, 0x04);

		cpu.step();
		assertEquals(0x00, bus.read(0x0000));
		assertTrue(cpu.getZeroFlag());
		assertFalse(cpu.getNegativeFlag());
		assertFalse(cpu.getCarryFlag());

		cpu.step();
		assertEquals(0x02, bus.read(0x0001));
		assertFalse(cpu.getZeroFlag());
		assertFalse(cpu.getNegativeFlag());
		assertFalse(cpu.getCarryFlag());

		cpu.step();
		assertEquals(0x04, bus.read(0x0002));
		assertFalse(cpu.getZeroFlag());
		assertFalse(cpu.getNegativeFlag());
		assertFalse(cpu.getCarryFlag());

		cpu.step();
		assertEquals(0x88, bus.read(0x0003));
		assertFalse(cpu.getZeroFlag());
		assertTrue(cpu.getNegativeFlag());
		assertFalse(cpu.getCarryFlag());

		cpu.step();
		assertEquals(0x00, bus.read(0x0004));
		assertTrue(cpu.getZeroFlag());
		assertFalse(cpu.getNegativeFlag());
		assertTrue(cpu.getCarryFlag());
	}

	/* BIT - Bit Test - $24 */
	public void test_BIT() {
		bus.write(0x0000, 0xc0);

		bus.loadProgram(0xa9, 0x01,  // LDA #$01
										0x24, 0x00,  // BIT $00

										0xa9, 0x0f,  // LDA #$0f
										0x24, 0x00,  // BIT $00

										0xa9, 0x40,  // LDA #$40
										0x24, 0x00,  // BIT $00

										0xa9, 0x80,  // LDA #$80
										0x24, 0x00,  // BIT $00

										0xa9, 0xc0,  // LDA #$c0
										0x24, 0x00,  // BIT $00

										0xa9, 0xff,  // LDA #$ff
										0x24, 0x00); // BIT $00

		cpu.step(2);
		assertTrue(cpu.getZeroFlag());
		assertFalse(cpu.getNegativeFlag());
		assertFalse(cpu.getOverflowFlag());

		cpu.step(2);
		assertTrue(cpu.getZeroFlag());
		assertFalse(cpu.getNegativeFlag());
		assertFalse(cpu.getOverflowFlag());

		cpu.step(2);
		assertFalse(cpu.getZeroFlag());
		assertFalse(cpu.getNegativeFlag());
		assertTrue(cpu.getOverflowFlag());

		cpu.step(2);
		assertFalse(cpu.getZeroFlag());
		assertTrue(cpu.getNegativeFlag());
		assertFalse(cpu.getOverflowFlag());

		cpu.step(2);
		assertFalse(cpu.getZeroFlag());
		assertTrue(cpu.getNegativeFlag());
		assertTrue(cpu.getOverflowFlag());

		cpu.step(2);
		assertFalse(cpu.getZeroFlag());
		assertTrue(cpu.getNegativeFlag());
		assertTrue(cpu.getOverflowFlag());
	}

	/* AND - Logical AND - $25 */
	public void test_AND() {
	}

	/* ROL - Rotate Left - $26 */
	public void test_ROL() {
	}

	/* EOR - Exclusive OR - $45 */
	public void test_EOR() {
	}

	/* LSR - Logical Shift Right - $46 */
	public void test_LSR() {
	}

	/* ADC - Add with Carry - $65 */
	public void test_ADC() {
	}

	/* ROR - Rotate Right - $66 */
	public void test_ROR() {
	}

	/* STY - Store Y Register - $84 */
	public void test_STY() {
	}

	/* STA - Store Accumulator - $85 */
	public void test_STA() {
	}

	/* STX - Store X Register - $86 */
	public void test_STX() {
	}

	/* LDY - Load Y Register - $a4 */
	public void test_LDY() {
	}

	/* LDA - Load Accumulator - $a5 */
	public void test_LDA() {
	}

	/* LDX - Load X Register - $a6 */
	public void test_LDX() {
	}

	/* CPY - Compare Y Register - $c4 */
	public void test_CPY() {
	}

	/* CMP - Compare Accumulator - $c5 */
	public void test_CMP() {
	}

	/* DEC - Decrement Memory Location - $c6 */
	public void test_DEC() {
	}

	/* CPX - Compare X Register - $e4 */
	public void test_CPX() {
	}

	/* SBC - Subtract with Carry - $e5 */
	public void test_SBC() {
	}

	/* INC - Increment Memory Location - $e6 */
	public void test_INC() {
	}

}
