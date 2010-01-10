package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryAccessException;

import junit.framework.TestCase;

public class CpuAbsoluteModeTest extends TestCase {

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
   * ORA - $0d
   * ASL - $0e
   * JSR - $20
   * BIT - $2c
   * AND - $2d
   *
   * ROL - $2e
   * JMP - $4c
   * EOR - $4d
   * LSR - $4e
   * ADC - $6d
   *
   * ROR - $6e
   * STY - $8c
   * STA - $8d
   * STX - $8e
   * LDA - $ad
   *
   * LDX - $ae
   * LDY - $bc
   * CMP - $cd
   * CPY - $cc
   * DEC - $ce
   *
   * CPX - $ec
   * SBC - $ed
   * INC - $ee
   */

  /* ORA - Logical Inclusive OR - $0d */

  public void test_ORA() throws MemoryAccessException {
    // Set some initial values in memory
    bus.write(0x7f00, 0x00);
    bus.write(0x7f02, 0x11);
    bus.write(0x3504, 0x22);
    bus.write(0x3508, 0x44);
    bus.write(0x1210, 0x88);

    bus.loadProgram(0x0d, 0x00, 0x7f,  // ORA $7f00
                    0x0d, 0x02, 0x7f,  // ORA $7f02
                    0x0d, 0x04, 0x35,  // ORA $3504
                    0x0d, 0x08, 0x35,  // ORA $3508
                    0x0d, 0x10, 0x12); // ORA $1210
    extracted();
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x11, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x33, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x77, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    extracted();
    assertEquals(0xff, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* ASL - Arithmetic Shift Left - $0e */

  public void test_ASL() throws MemoryAccessException {
    bus.write(0x7f00, 0x00);
    bus.write(0x7f01, 0x01);
    bus.write(0x3502, 0x02);
    bus.write(0x3503, 0x44);
    bus.write(0x1204, 0x80);

    bus.loadProgram(0x0e, 0x00, 0x7f,  // ASL $7f00
                    0x0e, 0x01, 0x7f,  // ASL $7f01
                    0x0e, 0x02, 0x35,  // ASL $3502
                    0x0e, 0x03, 0x35,  // ASL $3503
                    0x0e, 0x04, 0x12); // ASL $1204

    extracted();
    assertEquals(0x00, bus.read(0x7f00));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x02, bus.read(0x7f01));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x04, bus.read(0x3502));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x88, bus.read(0x3503));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x00, bus.read(0x1204));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getCarryFlag());
  }

  /* JSR - Jump to Subroutine - $20 */

  public void test_JSR() throws MemoryAccessException {
    bus.loadProgram(0xea,              // NOP
                    0xea,              // NOP
                    0x20, 0x00, 0x34); // JSR $3400

    cpu.step(3);

    // New PC should be 0x3400
    assertEquals(0x3400, cpu.getProgramCounter());

    // Old PC-1 should be on stack (i.e.: address of third byte of the
    // JSR instruction, 0x0204)
    assertEquals(0x02, bus.read(0x1ff));
    assertEquals(0x04, bus.read(0x1fe));

    // No flags should have changed.
    assertEquals(0x20, cpu.getProcessorStatus());
  }

  /* BIT - Bit Test - $2c */

  public void test_BIT() throws MemoryAccessException {
    bus.write(0x1200, 0xc0);

    bus.loadProgram(0xa9, 0x01,        // LDA #$01
                    0x2c, 0x00, 0x12,  // BIT $1200

                    0xa9, 0x0f,        // LDA #$0f
                    0x2c, 0x00, 0x12,  // BIT $1200

                    0xa9, 0x40,        // LDA #$40
                    0x2c, 0x00, 0x12,  // BIT $1200

                    0xa9, 0x80,        // LDA #$80
                    0x2c, 0x00, 0x12,  // BIT $1200

                    0xa9, 0xc0,        // LDA #$c0
                    0x2c, 0x00, 0x12,  // BIT $1200

                    0xa9, 0xff,        // LDA #$ff
                    0x2c, 0x00, 0x12); // BIT $1200

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

  /* AND - Logical AND - $2d */

  public void test_AND() throws MemoryAccessException {
    bus.write(0x1200, 0x00);
    bus.write(0x1201, 0x11);
    bus.write(0x1202, 0xff);
    bus.write(0x1203, 0x99);
    bus.write(0x1204, 0x11);
    bus.write(0x1205, 0x0f);

    bus.loadProgram(0x2d, 0x00, 0x12,  // AND $1200
                    0x2d, 0x01, 0x12,  // AND $1201
                    0xa9, 0xaa,        // LDA #$aa
                    0x2d, 0x02, 0x12,  // AND $1202
                    0x2d, 0x03, 0x12,  // AND $1203
                    0x2d, 0x04, 0x12,  // AND $1204
                    0xa9, 0xff,        // LDA #$ff
                    0x2d, 0x05, 0x12); // AND $1205
    extracted();
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.step(2);
    assertEquals(0xaa, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x88, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.step(2);
    assertEquals(0x0f, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  /* ROL - Rotate Shift Left - $2e */

  public void test_ROL() throws MemoryAccessException {

    bus.write(0x1200, 0x00);
    bus.write(0x1201, 0x01);

    bus.loadProgram(0x2e, 0x00, 0x12,  // ROL $1200 (m=%00000000, c=0)
                    0x2e, 0x01, 0x12,  // ROL $1201 (m=%00000010, c=0)
                    0x38,              // SEC       (m=%00000010, c=1)
                    0x2e, 0x01, 0x12,  // ROL $1201 (m=%00000101, c=0)
                    0x2e, 0x01, 0x12,  // ROL $1201 (m=%00001010, c=0)
                    0x2e, 0x01, 0x12,  // ROL $1201 (m=%00010100, c=0)
                    0x2e, 0x01, 0x12,  // ROL $1201 (m=%00101000, c=0)
                    0x2e, 0x01, 0x12,  // ROL $1201 (m=%01010000, c=0)
                    0x2e, 0x01, 0x12,  // ROL $1201 (m=%10100000, c=0)
                    0x2e, 0x01, 0x12,  // ROL $1201 (m=%01000000, c=1)
                    0x2e, 0x01, 0x12); // ROL $1201 (m=%10000001, c=0)

    extracted();
    assertEquals(0x00, bus.read(0x1200));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x02, bus.read(0x1201));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step(2);
    assertEquals(0x05, bus.read(0x1201));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x0a, bus.read(0x1201));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x14, bus.read(0x1201));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x28, bus.read(0x1201));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x50, bus.read(0x1201));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0xa0, bus.read(0x1201));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x40, bus.read(0x1201));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getCarryFlag());

    extracted();
    assertEquals(0x81, bus.read(0x1201));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());
  }

  /* JMP - Jump - $4c */

  public void test_JMP() throws MemoryAccessException {
    bus.loadProgram(0x4c, 0x00, 0x34);
    extracted();
    assertEquals(0x3400, cpu.getProgramCounter());
    // No change to status flags.
    assertEquals(0x20, cpu.getProcessorStatus());
  }

  private void extracted() throws MemoryAccessException {
    cpu.step();
  }

  /* EOR - Exclusive OR - $4d */

  public void test_EOR() throws MemoryAccessException {
    bus.write(0x1210, 0x00);
    bus.write(0x1211, 0xff);
    bus.write(0x1212, 0x33);
    bus.write(0x1213, 0x44);

    bus.loadProgram(0xa9, 0x88,        // LDA #$88
                    0x4d, 0x10, 0x12,  // EOR $1210
                    0x4d, 0x11, 0x12,  // EOR $1211
                    0x4d, 0x12, 0x12,  // EOR $1212
                    0x4d, 0x13, 0x12); // EOR $1213
    cpu.step(2);
    assertEquals(0x88, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getZeroFlag());

    extracted();
    assertEquals(0x77, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getZeroFlag());

    extracted();
    assertEquals(0x44, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getZeroFlag());

    extracted();
    assertEquals(0x00, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getZeroFlag());
  }

  /* LSR - Logical Shift Right - $4e */

  public void test_LSR() throws MemoryAccessException {
    bus.write(0x1200, 0x00);
    bus.write(0x1201, 0x01);
    bus.write(0x1202, 0x02);
    bus.write(0x1203, 0x44);
    bus.write(0x1204, 0x80);
    bus.write(0x1205, 0x02);

    bus.loadProgram(0x4e, 0x00, 0x12,  // LSR $1200
                    0x4e, 0x01, 0x12,  // LSR $1201
                    0x4e, 0x02, 0x12,  // LSR $1202
                    0x4e, 0x03, 0x12,  // LSR $1203
                    0x4e, 0x04, 0x12,  // LSR $1204
                    0x38,              // SEC
                    0x4e, 0x05, 0x12); // LSR $1205

    extracted();
    assertEquals(0x00, bus.read(0x1200));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x00, bus.read(0x1201));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getCarryFlag());

    extracted();
    assertEquals(0x01, bus.read(0x1202));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x22, bus.read(0x1203));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x40, bus.read(0x1204));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    // Setting Carry should not affect the result.
    cpu.step(2);
    assertEquals(0x01, bus.read(0x1205));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());
  }

  /* ADC - Add with Carry - $6d */

  public void test_ADC() throws MemoryAccessException {
    bus.write(0x1210, 0x01);
    bus.write(0x1211, 0xff);

    bus.loadProgram(0xa9, 0x00,        // LDA #$00
                    0x6d, 0x10, 0x12); // ADC $1210
    cpu.step(2);
    assertEquals(0x01, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x7f,        // LDA #$7f
                    0x6d, 0x10, 0x12); // ADC $1210
    cpu.step(2);
    assertEquals(0x80, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertTrue(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x80,        // LDA #$80
                    0x6d, 0x10, 0x12); // ADC $1210
    cpu.step(2);
    assertEquals(0x81, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0xff,        // LDA #$ff
                    0x6d, 0x10, 0x12); // ADC $10
    cpu.step(2);
    assertEquals(0x00, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertTrue(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x00,        // LDA #$00
                    0x6d, 0x11, 0x12); // ADC $11
    cpu.step(2);
    assertEquals(0xff, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x7f,        // LDA #$7f
                    0x6d, 0x11, 0x12); // ADC $11
    cpu.step(2);
    assertEquals(0x7e, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x80,        // LDA #$80
                    0x6d, 0x11, 0x12); // ADC $11
    cpu.step(2);
    assertEquals(0x7f, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0xff,        // LDA #$ff
                    0x6d, 0x11, 0x12); // ADC $11
    cpu.step(2);
    assertEquals(0xfe, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());
  }

  public void test_ADC_IncludesCarry() throws MemoryAccessException {
    bus.write(0x1210, 0x01);

    bus.loadProgram(0xa9, 0x00,        // LDA #$00
                    0x38,              // SEC
                    0x6d, 0x10, 0x12); // ADC $10
    cpu.step(3);
    assertEquals(0x02, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());
  }

  public void test_ADC_DecimalMode() throws MemoryAccessException {
    bus.write(0x1210, 0x01);
    bus.write(0x1211, 0x99);

    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x01,        // LDA #$01
                    0x6d, 0x10, 0x12); // ADC $10
    cpu.step(3);
    assertEquals(0x02, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x49,        // LDA #$49
                    0x6d, 0x10, 0x12); // ADC $10
    cpu.step(3);
    assertEquals(0x50, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x50,        // LDA #$50
                    0x6d, 0x10, 0x12); // ADC $10
    cpu.step(3);
    assertEquals(0x51, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x99,        // LDA #$99
                    0x6d, 0x10, 0x12); // ADC $10
    cpu.step(3);
    assertEquals(0x00, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertTrue(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x00,        // LDA #$00
                    0x6d, 0x11, 0x12); // ADC $10
    cpu.step(3);
    assertEquals(0x99, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x49,        // LDA #$49
                    0x6d, 0x11, 0x12); // ADC $11
    cpu.step(3);
    assertEquals(0x48, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x50,        // LDA #$59
                    0x6d, 0x11, 0x12); // ADC $11
    cpu.step(3);
    assertEquals(0x49, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());
  }

  /* ROR - Rotate Right - $6e */

  public void test_ROR() throws MemoryAccessException {
    bus.write(0x1210, 0x00);
    bus.write(0x1211, 0x10);

    bus.loadProgram(0x6e, 0x10, 0x12,  // ROR $1200 (m=%00000000, c=0)
                    0x6e, 0x11, 0x12,  // ROR $1201 (m=%00001000, c=0)
                    0x6e, 0x11, 0x12,  // ROR $1201 (m=%00000100, c=0)
                    0x6e, 0x11, 0x12,  // ROR $1201 (m=%00000010, c=0)
                    0x6e, 0x11, 0x12,  // ROR $1201 (m=%00000001, c=0)
                    0x6e, 0x11, 0x12,  // ROR $1201 (m=%00000000, c=1)
                    0x6e, 0x11, 0x12,  // ROR $1201 (m=%10000000, c=0)
                    0x6e, 0x11, 0x12,  // ROR $1201 (m=%01000000, c=0)
                    0x6e, 0x11, 0x12,  // ROR $1201 (m=%00100000, c=0)
                    0x6e, 0x11, 0x12); // ROR $1201 (m=%00010000, c=0)

    extracted();
    assertEquals(0x00, bus.read(0x1210));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x08, bus.read(0x1211));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x04, bus.read(0x1211));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x02, bus.read(0x1211));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x01, bus.read(0x1211));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x00, bus.read(0x1211));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getCarryFlag());

    extracted();
    assertEquals(0x80, bus.read(0x1211));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x40, bus.read(0x1211));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x20, bus.read(0x1211));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    extracted();
    assertEquals(0x10, bus.read(0x1211));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());
  }

  /* STY - Store Y Register - $8c */

  public void test_STY() throws MemoryAccessException {
    cpu.setYRegister(0x00);
    bus.loadProgram(0x8c, 0x10, 0x12);
    extracted();
    assertEquals(0x00, bus.read(0x1210));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.reset();

    cpu.setYRegister(0x0f);
    bus.loadProgram(0x8c, 0x10, 0x12);
    extracted();
    assertEquals(0x0f, bus.read(0x1210));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.reset();

    cpu.setYRegister(0x80);
    bus.loadProgram(0x8c, 0x10, 0x12);
    extracted();
    assertEquals(0x80, bus.read(0x1210));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* STA - Store Accumulator - $8d */

  public void test_STA() throws MemoryAccessException {
    cpu.setAccumulator(0x00);
    bus.loadProgram(0x8d, 0x10, 0x12);
    extracted();
    assertEquals(0x00, bus.read(0x1210));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.reset();

    cpu.setAccumulator(0x0f);
    bus.loadProgram(0x8d, 0x10, 0x12);
    extracted();
    assertEquals(0x0f, bus.read(0x1210));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.reset();

    cpu.setAccumulator(0x80);
    bus.loadProgram(0x8d, 0x10, 0x12);
    extracted();
    assertEquals(0x80, bus.read(0x1210));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* STX - Store X Register - $8e */

  public void test_STX() throws MemoryAccessException {
    cpu.setXRegister(0x00);
    bus.loadProgram(0x8e, 0x10, 0x12);
    extracted();
    assertEquals(0x00, bus.read(0x1210));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.reset();

    cpu.setXRegister(0x0f);
    bus.loadProgram(0x8e, 0x10, 0x12);
    extracted();
    assertEquals(0x0f, bus.read(0x1210));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.reset();

    cpu.setXRegister(0x80);
    bus.loadProgram(0x8e, 0x10, 0x12);
    extracted();
    assertEquals(0x80, bus.read(0x1210));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* LDA - Load Accumulator - $ad */

  public void test_LDA() throws MemoryAccessException {
    bus.write(0x1210, 0x00);
    bus.write(0x1211, 0x0f);
    bus.write(0x1212, 0x80);

    bus.loadProgram(0xad, 0x10, 0x12,  // LDA $1210
                    0xad, 0x11, 0x12,  // LDA $1211
                    0xad, 0x12, 0x12); // LDA $1212

    extracted();
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x0f, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x80, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* LDX - Load X Register - $ae */

  public void test_LDX() throws MemoryAccessException {
    bus.write(0x1210, 0x00);
    bus.write(0x1211, 0x0f);
    bus.write(0x1212, 0x80);

    bus.loadProgram(0xae, 0x10, 0x12,  // LDX $1210
                    0xae, 0x11, 0x12,  // LDX $1211
                    0xae, 0x12, 0x12); // LDX $1212

    extracted();
    assertEquals(0x00, cpu.getXRegister());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x0f, cpu.getXRegister());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x80, cpu.getXRegister());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* LDY - Load Y Register - $bc */

  public void test_LDY() throws MemoryAccessException {
    bus.write(0x1210, 0x00);
    bus.write(0x1211, 0x0f);
    bus.write(0x1212, 0x80);

    bus.loadProgram(0xbc, 0x10, 0x12,  // LDY $1210
                    0xbc, 0x11, 0x12,  // LDY $1211
                    0xbc, 0x12, 0x12); // LDY $1212

    extracted();
    assertEquals(0x00, cpu.getYRegister());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x0f, cpu.getYRegister());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x80, cpu.getYRegister());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* CMP - Compare Accumulator - $cd */

  public void test_CMP() throws MemoryAccessException {
    bus.write(0x1210, 0x00);
    bus.write(0x1211, 0x80);
    bus.write(0x1212, 0xff);

    cpu.setAccumulator(0x80);

    bus.loadProgram(0xcd, 0x10, 0x12,  // CMP $1210
                    0xcd, 0x11, 0x12,  // CMP $1211
                    0xcd, 0x12, 0x12); // CMP $1212

    extracted();
    assertTrue(cpu.getCarryFlag());    // m > y
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag()); // m - y < 0

    extracted();
    assertTrue(cpu.getCarryFlag());    // m = y
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag()); // m - y == 0

    extracted();
    assertFalse(cpu.getCarryFlag());    // m < y
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag()); // m - y > 0
  }

  /* CPY - Compare Y Register - $cc */

  public void test_CPY() throws MemoryAccessException {
    bus.write(0x1210, 0x00);
    bus.write(0x1211, 0x80);
    bus.write(0x1212, 0xff);

    cpu.setYRegister(0x80);

    bus.loadProgram(0xcc, 0x10, 0x12,  // CPY $1210
                    0xcc, 0x11, 0x12,  // CPY $1211
                    0xcc, 0x12, 0x12); // CPY $1212

    extracted();
    assertTrue(cpu.getCarryFlag());    // m > y
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag()); // m - y < 0

    extracted();
    assertTrue(cpu.getCarryFlag());    // m = y
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag()); // m - y == 0

    extracted();
    assertFalse(cpu.getCarryFlag());    // m < y
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag()); // m - y > 0
  }

  /* DEC - Decrement Memory Location - $ce */

  public void test_DEC() throws MemoryAccessException {
    bus.write(0x1210, 0x00);
    bus.write(0x1211, 0x01);
    bus.write(0x1212, 0x80);
    bus.write(0x1213, 0xff);

    bus.loadProgram(0xce, 0x10, 0x12,  // DEC $1210
                    0xce, 0x11, 0x12,  // DEC $1211
                    0xce, 0x12, 0x12,  // DEC $1212
                    0xce, 0x13, 0x12); // DEC $1213

    extracted();
    assertEquals(0xff, bus.read(0x1210));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x00, bus.read(0x1211));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x7f, bus.read(0x1212));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    extracted();
    assertEquals(0xfe, bus.read(0x1213));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* CPX - Compare X Register - $ec */

  public void test_CPX() throws MemoryAccessException {
    bus.write(0x1210, 0x00);
    bus.write(0x1211, 0x80);
    bus.write(0x1212, 0xff);

    cpu.setXRegister(0x80);

    bus.loadProgram(0xec, 0x10, 0x12,  // CPX $1210
                    0xec, 0x11, 0x12,  // CPX $1211
                    0xec, 0x12, 0x12); // CPX $1212

    extracted();
    assertTrue(cpu.getCarryFlag());    // m > y
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag()); // m - y < 0

    extracted();
    assertTrue(cpu.getCarryFlag());    // m = y
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag()); // m - y == 0

    extracted();
    assertFalse(cpu.getCarryFlag());    // m < y
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag()); // m - y > 0
  }

  /* SBC - Subtract with Carry (borrow) - $ed */

  public void test_SBC() throws MemoryAccessException {
    bus.write(0x1210, 0x01);

    bus.loadProgram(0xa9, 0x00,        // LDA #$00
                    0xed, 0x10, 0x12); // SBC $1210
    cpu.step(2);
    assertEquals(0xfe, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x7f,        // LDA #$7f
                    0xed, 0x10, 0x12); // SBC $1210
    cpu.step(2);
    assertEquals(0x7d, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x80,        // LDA #$80
                    0xed, 0x10, 0x12); // SBC $1210
    cpu.step(2);
    assertEquals(0x7e, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0xff,        // LDA #$ff
                    0xed, 0x10, 0x12); // SBC $1210
    cpu.step(2);
    assertEquals(0xfd, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x02,        // LDA #$02
                    0xed, 0x10, 0x12); // SBC $1210
    cpu.step(2);
    assertEquals(0x00, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertTrue(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());
  }

  public void test_SBC_IncludesNotOfCarry() throws MemoryAccessException {
    bus.write(0x1210, 0x01);

    // Subtrace with Carry Flag cleared
    bus.loadProgram(0x18,              // CLC
                    0xa9, 0x05,        // LDA #$00
                    0xed, 0x10, 0x12); // SBC $1210

    cpu.step(3);
    assertEquals(0x03, cpu.getAccumulator());

    cpu.reset();

    // Subtrace with Carry Flag cleared
    bus.loadProgram(0x18,              // CLC
                    0xa9, 0x00,        // LDA #$00
                    0xed, 0x10, 0x12); // SBC $1210

    cpu.step(3);
    assertEquals(0xfe, cpu.getAccumulator());

    cpu.reset();

    // Subtract with Carry Flag set
    bus.loadProgram(0x38,              // SEC
                    0xa9, 0x05,        // LDA #$00
                    0xed, 0x10, 0x12); // SBC $1210
    cpu.step(3);
    assertEquals(0x04, cpu.getAccumulator());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();

    // Subtract with Carry Flag set
    bus.loadProgram(0x38,              // SEC
                    0xa9, 0x00,        // LDA #$00
                    0xed, 0x10, 0x12); // SBC $1210
    cpu.step(3);
    assertEquals(0xff, cpu.getAccumulator());
    assertFalse(cpu.getCarryFlag());

  }

  public void test_SBC_DecimalMode() throws MemoryAccessException {
    bus.write(0x1210, 0x01);
    bus.write(0x1220, 0x11);

    bus.loadProgram(0xf8,
                    0xa9, 0x00,
                    0xed, 0x10, 0x12);
    cpu.step(3);
    assertEquals(0x98, cpu.getAccumulator());
    assertFalse(cpu.getCarryFlag()); // borrow = set flag
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getDecimalModeFlag());

    cpu.reset();

    bus.loadProgram(0xf8,
                    0xa9, 0x99,
                    0xed, 0x10, 0x12);
    cpu.step(3);
    assertEquals(0x97, cpu.getAccumulator());
    assertTrue(cpu.getCarryFlag()); // No borrow = clear flag
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getDecimalModeFlag());

    cpu.reset();

    bus.loadProgram(0xf8,
                    0xa9, 0x50,
                    0xed, 0x10, 0x12);
    cpu.step(3);
    assertEquals(0x48, cpu.getAccumulator());
    assertTrue(cpu.getCarryFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getDecimalModeFlag());


    cpu.reset();

    bus.loadProgram(0xf8,               // SED
                    0xa9, 0x02,         // LDA #$02
                    0xed, 0x10, 0x12);  // SBC $1210
    cpu.step(3);
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getCarryFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertTrue(cpu.getZeroFlag());
    assertTrue(cpu.getDecimalModeFlag());

    cpu.reset();

    bus.loadProgram(0xf8,               // SED
                    0xa9, 0x10,         // LDA #$10
                    0xed, 0x20, 0x12);  // SBC $20
    cpu.step(3);
    assertEquals(0x98, cpu.getAccumulator());
    assertFalse(cpu.getCarryFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getDecimalModeFlag());

    cpu.reset();

    bus.loadProgram(0x38,               // SEC
                    0xf8,               // SED
                    0xa9, 0x05,         // LDA #$05
                    0xed, 0x10, 0x12);  // SBC $1210
    cpu.step(4);
    assertEquals(0x04, cpu.getAccumulator());
    assertTrue(cpu.getCarryFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getDecimalModeFlag());

    cpu.reset();

    bus.loadProgram(0x38,               // SEC
                    0xf8,               // SED
                    0xa9, 0x00,         // LDA #$00
                    0xed, 0x10, 0x12);  // SBC $1210
    cpu.step(4);
    assertEquals(0x99, cpu.getAccumulator());
    assertFalse(cpu.getCarryFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getDecimalModeFlag());
  }

  /* INC - Increment Memory Location - $ee */

  public void test_INC() throws MemoryAccessException {
    bus.write(0x1210, 0x00);
    bus.write(0x1211, 0x7f);
    bus.write(0x1212, 0xff);

    bus.loadProgram(0xee, 0x10, 0x12,  // INC $1210
                    0xee, 0x11, 0x12,  // INC $1211
                    0xee, 0x12, 0x12); // INC $1212

    extracted();
    assertEquals(0x01, bus.read(0x1210));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x80, bus.read(0x1211));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());

    extracted();
    assertEquals(0x00, bus.read(0x1212));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

  }

}
