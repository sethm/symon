package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.*;

import junit.framework.*;

public class CpuAccumulatorModeTest extends TestCase {

  protected Cpu cpu;
  protected Bus bus;
  protected Memory mem;

  public void setUp() throws MemoryRangeException, MemoryAccessException {
    this.cpu = new Cpu();
    this.bus = new Bus(0x0000, 0xffff);
    this.mem = new Memory(0x0000, 0x10000);
    bus.addCpu(cpu);
    bus.addDevice(mem);

    // Load the reset vector.
    bus.write(0xfffc, Bus.DEFAULT_LOAD_ADDRESS & 0x00ff);
    bus.write(0xfffd, (Bus.DEFAULT_LOAD_ADDRESS & 0xff00)>>>8);

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
	 * ASL - $0a
	 * ROL - $2a
	 * LSR - $4a
	 * ROR - $6a
	 */

	/* ASL - Arithmetic Shift Left - $0a */

	public void test_ASL() throws MemoryAccessException {
		bus.loadProgram(0xa9, 0x00,  // LDA #$00
										0x0a,        // ASL A

										0xa9, 0x01,  // LDA #$01
                    0x0a,        // ASL A

										0xa9, 0x02,  // LDA #$02
                    0x0a,        // ASL A

										0xa9, 0x44,  // LDA #$44
                    0x0a,        // ASL A

										0xa9, 0x80,  // LDA #$80
                    0x0a);       // ASL A

    cpu.step(2);
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step(2);
    assertEquals(0x02, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step(2);
    assertEquals(0x04, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step(2);
    assertEquals(0x88, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step(2);
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
		assertTrue(cpu.getCarryFlag());
	}

	/* ROL - Rotate Left - $2a */

	public void test_ROL() throws MemoryAccessException {
    bus.loadProgram(0xa9, 0x00,  // LDA #$00
										0x2a,        // ROL A   (m=%00000000, c=0)
                    0xa9, 0x01,  // LDA #$01
										0x2a,        // ROL A   (m=%00000010, c=0)
                    0x38,        // SEC     (m=%00000010, c=1)
                    0x2a,        // ROL A   (m=%00000101, c=0)
                    0x2a,        // ROL A   (m=%00001010, c=0)
                    0x2a,        // ROL A   (m=%00010100, c=0)
                    0x2a,        // ROL A   (m=%00101000, c=0)
                    0x2a,        // ROL A   (m=%01010000, c=0)
                    0x2a,        // ROL A   (m=%10100000, c=0)
                    0x2a,        // ROL A   (m=%01000000, c=1)
                    0x2a);       // ROL A   (m=%10000001, c=0)

    cpu.step(2);
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step(2);
    assertEquals(0x02, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step(2);
    assertEquals(0x05, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x0a, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x14, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x28, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x50, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0xa0, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x40, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x81, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());
	}

	/* LSR - Logical Shift Right - $4a */

	public void test_LSR() throws MemoryAccessException {
    bus.loadProgram(0xa9, 0x00,  // LDA #$00
										0x4a,        // LSR A

										0xa9, 0x01,  // LDA #$01
                    0x4a,        // LSR A

										0xa9, 0x02,  // LDA #$02
                    0x4a,        // LSR A

										0xa9, 0x44,  // LDA #$44
                    0x4a,        // LSR A

										0xa9, 0x80,  // LDA #$80
                    0x4a,        // LSR A

                    0x38,        // SEC
										0xa9, 0x02,  // LDA #$02
                    0x4a);       // LSR $05

    cpu.step(2);
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step(2);
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.step(2);
    assertEquals(0x01, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step(2);
    assertEquals(0x22, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step(2);
    assertEquals(0x40, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    // Setting Carry should not affect the result.
    cpu.step(3);
    assertEquals(0x01, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());
	}

	/* ROR - Rotate Right - $6a */

	public void test_ROR() throws MemoryAccessException {
    bus.loadProgram(0xa9, 0x00,  // LDA #$00
										0x6a,        // ROR A   (m=%00000000, c=0)
										0xa9, 0x10,  // LDA #$10
                    0x6a,        // ROR A   (m=%00001000, c=0)
                    0x6a,        // ROR A   (m=%00000100, c=0)
                    0x6a,        // ROR A   (m=%00000010, c=0)
                    0x6a,        // ROR A   (m=%00000001, c=0)
                    0x6a,        // ROR A   (m=%00000000, c=1)
                    0x6a,        // ROR A   (m=%10000000, c=0)
                    0x6a,        // ROR A   (m=%01000000, c=0)
                    0x6a,        // ROR A   (m=%00100000, c=0)
                    0x6a);       // ROR A   (m=%00010000, c=0)

    cpu.step(2);
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step(2);
    assertEquals(0x08, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x04, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x02, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x01, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x80, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x40, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x20, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x10, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());
	}

}