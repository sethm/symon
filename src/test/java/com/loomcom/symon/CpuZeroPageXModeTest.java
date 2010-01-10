package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryAccessException;

import junit.framework.TestCase;

public class CpuZeroPageXModeTest extends TestCase {

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
   * ORA - $15
   * ASL - $16
   * AND - $35
   * ROL - $36
   * EOR - $55
   *
   * LSR - $56
   * ADC - $75
   * ROR - $76
   * STY - $94
   * STA - $95
   *
   * LDY - $b4
   * LDA - $b5
   * CMP - $d5
   * DEC - $d6
   * SBC - $f5
   *
   * INC - $f6
   */

  /* ORA - Logical Inclusive OR - $15 */

  public void test_ORA() throws MemoryAccessException {
    // Set some initial values in zero page.
    bus.write(0x30, 0x00);
    bus.write(0x32, 0x11);
    bus.write(0x34, 0x22);
    bus.write(0x38, 0x44);
    bus.write(0x40, 0x88);
    bus.write(0x02, 0x88);

    // Set offset in X register.
    cpu.setXRegister(0x30);

    bus.loadProgram(0x15, 0x00,  // ORA $00,X
                    0x15, 0x02,  // ORA $02,X
                    0x15, 0x04,  // ORA $04,X
                    0x15, 0x08,  // ORA $08,X
                    0x15, 0x10,  // ORA $10,X
                    0xa9, 0x00,  // LDA #$00
                    0x15, 0xd2); // ORA $d2,X - should wrap around

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

    // Should wrap around and ORA with value in 0x02
    cpu.step(2);
    assertEquals(0x88, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* ASL - Arithmetic Shift Left - $16 */

  public void test_ASL() throws MemoryAccessException {
    bus.write(0x30, 0x00);
    bus.write(0x31, 0x01);
    bus.write(0x32, 0x02);
    bus.write(0x33, 0x44);
    bus.write(0x34, 0x80);
    bus.write(0x02, 0x01);

    // Set offset in X register.
    cpu.setXRegister(0x30);

    bus.loadProgram(0x16, 0x00,  // ASL $00,X
                    0x16, 0x01,  // ASL $01,X
                    0x16, 0x02,  // ASL $02,X
                    0x16, 0x03,  // ASL $03,X
                    0x16, 0x04,  // ASL $04,X
                    0x16, 0xd2); // ASL $d2,X

    cpu.step();
    assertEquals(0x00, bus.read(0x30));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x02, bus.read(0x31));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x04, bus.read(0x32));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x88, bus.read(0x33));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x00, bus.read(0x34));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getCarryFlag());

    // Should wrap around, d2 + 30 = 02
    cpu.step();
    assertEquals(0x02, bus.read(0x02));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());
  }

  /* AND - Logical AND - $35 */

  public void test_AND() throws MemoryAccessException {
    bus.write(0x30, 0x00);
    bus.write(0x31, 0x11);
    bus.write(0x32, 0xff);
    bus.write(0x33, 0x99);
    bus.write(0x34, 0x11);
    bus.write(0x35, 0x0f);
    bus.write(0x02, 0x11);

    // Set offset in X register.
    cpu.setXRegister(0x30);

    bus.loadProgram(0x35, 0x00,  // AND $00
                    0x35, 0x01,  // AND $01,X
                    0xa9, 0xaa,  // LDA #$aa
                    0x35, 0x02,  // AND $02,X
                    0x35, 0x03,  // AND $03
                    0x35, 0x04,  // AND $04,X
                    0xa9, 0xff,  // LDA #$ff
                    0x35, 0x05,  // AND $05,X
                    0xa9, 0x01,  // LDA #$01
                    0x35, 0xd2); // AND $d2,X
    cpu.step();
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.step();
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.step(2);
    assertEquals(0xaa, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());

    cpu.step();
    assertEquals(0x88, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());

    cpu.step();
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.step(2);
    assertEquals(0x0f, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.step(2);
    assertEquals(0x01, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  /* ROL - Rotate Shift Left - $36 */

  public void test_ROL() throws MemoryAccessException {

    bus.write(0x70, 0x00);
    bus.write(0x71, 0x01);

    // Set offset in X register
    cpu.setXRegister(0x70);

    bus.loadProgram(0x36, 0x00,  // ROL $00,X (m=%00000000, c=0)
                    0x36, 0x01,  // ROL $01,X (m=%00000010, c=0)
                    0x38,        // SEC       (m=%00000010, c=1)
                    0x36, 0x01,  // ROL $01,X (m=%00000101, c=0)
                    0x36, 0x01,  // ROL $01,X (m=%00001010, c=0)
                    0x36, 0x01,  // ROL $01,X (m=%00010100, c=0)
                    0x36, 0x01,  // ROL $01,X (m=%00101000, c=0)
                    0x36, 0x01,  // ROL $01,X (m=%01010000, c=0)
                    0x36, 0x01,  // ROL $01,X (m=%10100000, c=0)
                    0x36, 0x01,  // ROL $01,X (m=%01000000, c=1)
                    0x36, 0x01); // ROL $01,X (m=%10000001, c=0)

    cpu.step();
    assertEquals(0x00, bus.read(0x70));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x02, bus.read(0x71));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step(2);
    assertEquals(0x05, bus.read(0x71));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x0a, bus.read(0x71));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x14, bus.read(0x71));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x28, bus.read(0x71));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x50, bus.read(0x71));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0xa0, bus.read(0x71));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x40, bus.read(0x71));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x81, bus.read(0x71));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());
  }

  /* EOR - Exclusive OR - $55 */

  public void test_EOR() throws MemoryAccessException {
    bus.write(0x40, 0x00);
    bus.write(0x41, 0xff);
    bus.write(0x42, 0x33);
    bus.write(0x43, 0x44);

    cpu.setXRegister(0x30);

    bus.loadProgram(0xa9, 0x88,  // LDA #$88
                    0x55, 0x10,  // EOR $10,X
                    0x55, 0x11,  // EOR $11,X
                    0x55, 0x12,  // EOR $12,X
                    0x55, 0x13); // EOR $13,X
    cpu.step(2);
    assertEquals(0x88, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getZeroFlag());

    cpu.step();
    assertEquals(0x77, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getZeroFlag());

    cpu.step();
    assertEquals(0x44, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getZeroFlag());

    cpu.step();
    assertEquals(0x00, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getZeroFlag());
  }

  /* LSR - Logical Shift Right - $56 */

  public void test_LSR() throws MemoryAccessException {
    bus.write(0x30, 0x00);
    bus.write(0x31, 0x01);
    bus.write(0x32, 0x02);
    bus.write(0x33, 0x44);
    bus.write(0x34, 0x80);
    bus.write(0x35, 0x02);

    cpu.setXRegister(0x30);

    bus.loadProgram(0x56, 0x00,  // LSR $00,X
                    0x56, 0x01,  // LSR $01,X
                    0x56, 0x02,  // LSR $02,X
                    0x56, 0x03,  // LSR $03,X
                    0x56, 0x04,  // LSR $04,X
                    0x38,        // SEC
                    0x56, 0x05); // LSR $05,X

    cpu.step();
    assertEquals(0x00, bus.read(0x30));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x00, bus.read(0x31));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x01, bus.read(0x32));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x22, bus.read(0x33));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x40, bus.read(0x34));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    // Setting Carry should not affect the result.
    cpu.step(2);
    assertEquals(0x01, bus.read(0x35));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());
  }

  /* ADC - Add with Carry - $75 */

  public void test_ADC() throws MemoryAccessException {
    bus.write(0x40, 0x01);
    bus.write(0x41, 0xff);

    cpu.setXRegister(0x30);

    bus.loadProgram(0xa9, 0x00,  // LDA #$00
                    0x75, 0x10); // ADC $10,X
    cpu.step(2);
    assertEquals(0x01, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x7f,  // LDA #$7f
                    0x75, 0x10); // ADC $10,X
    cpu.step(2);
    assertEquals(0x80, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertTrue(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x80,  // LDA #$80
                    0x75, 0x10); // ADC $10,X
    cpu.step(2);
    assertEquals(0x81, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0xff,  // LDA #$ff
                    0x75, 0x10); // ADC $10,X
    cpu.step(2);
    assertEquals(0x00, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertTrue(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x00,  // LDA #$00
                    0x75, 0x11); // ADC $11,X
    cpu.step(2);
    assertEquals(0xff, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x7f,  // LDA #$7f
                    0x75, 0x11); // ADC $11,X
    cpu.step(2);
    assertEquals(0x7e, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x80,  // LDA #$80
                    0x75, 0x11); // ADC $11,X
    cpu.step(2);
    assertEquals(0x7f, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0xff,  // LDA #$ff
                    0x75, 0x11); // ADC $11,X
    cpu.step(2);
    assertEquals(0xfe, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());
  }

  public void test_ADC_IncludesCarry() throws MemoryAccessException {
    bus.write(0x40, 0x01);

    bus.loadProgram(0xa9, 0x00,  // LDA #$00
                    0x38,        // SEC
                    0x75, 0x10); // ADC $10,X

    cpu.setXRegister(0x30);

    cpu.step(3);
    assertEquals(0x02, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());
  }

  public void test_ADC_DecimalMode() throws MemoryAccessException {
    bus.write(0x40, 0x01);
    bus.write(0x41, 0x99);

    bus.loadProgram(0xf8,        // SED
                    0xa9, 0x01,  // LDA #$01
                    0x75, 0x10); // ADC $10,X

    cpu.setXRegister(0x30);

    cpu.step(3);
    assertEquals(0x02, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,        // SED
                    0xa9, 0x49,  // LDA #$49
                    0x75, 0x10); // ADC $10,X
    cpu.step(3);
    assertEquals(0x50, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,        // SED
                    0xa9, 0x50,  // LDA #$50
                    0x75, 0x10); // ADC $10,X
    cpu.step(3);
    assertEquals(0x51, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,        // SED
                    0xa9, 0x99,  // LDA #$99
                    0x75, 0x10); // ADC $10,X
    cpu.step(3);
    assertEquals(0x00, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertTrue(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,        // SED
                    0xa9, 0x00,  // LDA #$00
                    0x75, 0x11); // ADC $10,X
    cpu.step(3);
    assertEquals(0x99, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,        // SED
                    0xa9, 0x49,  // LDA #$49
                    0x75, 0x11); // ADC $11,X
    cpu.step(3);
    assertEquals(0x48, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,        // SED
                    0xa9, 0x50,  // LDA #$59
                    0x75, 0x11); // ADC $11,X
    cpu.step(3);
    assertEquals(0x49, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());
  }

  /* ROR - Rotate Right - $76 */

  public void test_ROR() throws MemoryAccessException {

    bus.write(0x40, 0x00);
    bus.write(0x41, 0x10);

    bus.loadProgram(0x76, 0x10,  // ROR $00 (m=%00000000, c=0)
                    0x76, 0x11,  // ROR $01 (m=%00001000, c=0)
                    0x76, 0x11,  // ROR $01 (m=%00000100, c=0)
                    0x76, 0x11,  // ROR $01 (m=%00000010, c=0)
                    0x76, 0x11,  // ROR $01 (m=%00000001, c=0)
                    0x76, 0x11,  // ROR $01 (m=%00000000, c=1)
                    0x76, 0x11,  // ROR $01 (m=%10000000, c=0)
                    0x76, 0x11,  // ROR $01 (m=%01000000, c=0)
                    0x76, 0x11,  // ROR $01 (m=%00100000, c=0)
                    0x76, 0x11); // ROR $01 (m=%00010000, c=0)

    cpu.setXRegister(0x30);

    cpu.step();
    assertEquals(0x00, bus.read(0x40));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x08, bus.read(0x41));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x04, bus.read(0x41));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x02, bus.read(0x41));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x01, bus.read(0x41));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x00, bus.read(0x41));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x80, bus.read(0x41));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x40, bus.read(0x41));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x20, bus.read(0x41));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.step();
    assertEquals(0x10, bus.read(0x41));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getCarryFlag());
  }

  /* STY - Store Y Register - $94 */

  public void test_STY() throws MemoryAccessException {
    cpu.setXRegister(0x30);

    cpu.setYRegister(0x00);
    bus.loadProgram(0x94, 0x10); // STY $10,X
    cpu.step();
    assertEquals(0x00, bus.read(0x40));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.reset();

    cpu.setYRegister(0x0f);
    bus.loadProgram(0x94, 0x10); // STY $10,X
    cpu.step();
    assertEquals(0x0f, bus.read(0x40));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.reset();

    cpu.setYRegister(0x80);
    bus.loadProgram(0x94, 0x10); // STY $10,X
    cpu.step();
    assertEquals(0x80, bus.read(0x40));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* STA - Store Accumulator - $95 */

  public void test_STA() throws MemoryAccessException {
    cpu.setXRegister(0x30);

    cpu.setAccumulator(0x00);
    bus.loadProgram(0x95, 0x10); // STA $10,X
    cpu.step();
    assertEquals(0x00, bus.read(0x40));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.reset();

    cpu.setAccumulator(0x0f);
    bus.loadProgram(0x95, 0x10); // STA $10,X
    cpu.step();
    assertEquals(0x0f, bus.read(0x40));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.reset();

    cpu.setAccumulator(0x80);
    bus.loadProgram(0x95, 0x10); // STA $10,X
    cpu.step();
    assertEquals(0x80, bus.read(0x40));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* LDY - Load Y Register - $b4 */

  public void test_LDY() throws MemoryAccessException {
    bus.write(0x45, 0x00);
    bus.write(0x46, 0x0f);
    bus.write(0x47, 0x80);

    bus.loadProgram(0xb4, 0x10,  // LDY $10,X
                    0xb4, 0x11,  // LDY $11,X
                    0xb4, 0x12); // LDY $12,X

    cpu.setXRegister(0x35);

    cpu.step();
    assertEquals(0x00, cpu.getYRegister());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.step();
    assertEquals(0x0f, cpu.getYRegister());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.step();
    assertEquals(0x80, cpu.getYRegister());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* LDA - Load Accumulator - $b5 */

  public void test_LDA() throws MemoryAccessException {
    bus.write(0x42, 0x00);
    bus.write(0x43, 0x0f);
    bus.write(0x44, 0x80);

    bus.loadProgram(0xb5, 0x10,  // LDA $10,X
                    0xb5, 0x11,  // LDA $11,X
                    0xb5, 0x12); // LDA $12,X

    cpu.setXRegister(0x32);

    cpu.step();
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.step();
    assertEquals(0x0f, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.step();
    assertEquals(0x80, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* CMP - Compare Accumulator - $d5 */

  public void test_CMP() throws MemoryAccessException {
    bus.write(0x40, 0x00);
    bus.write(0x41, 0x80);
    bus.write(0x42, 0xff);

    cpu.setAccumulator(0x80);

    bus.loadProgram(0xd5, 0x10,
                    0xd5, 0x11,
                    0xd5, 0x12);

    cpu.setXRegister(0x30);

    cpu.step();
    assertTrue(cpu.getCarryFlag());    // m > y
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag()); // m - y < 0

    cpu.step();
    assertTrue(cpu.getCarryFlag());    // m = y
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag()); // m - y == 0

    cpu.step();
    assertFalse(cpu.getCarryFlag());    // m < y
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag()); // m - y > 0
  }

  /* DEC - Decrement Memory Location - $d6 */

  public void test_DEC() throws MemoryAccessException {
    bus.write(0x40, 0x00);
    bus.write(0x41, 0x01);
    bus.write(0x42, 0x80);
    bus.write(0x43, 0xff);

    bus.loadProgram(0xd6, 0x10,  // DEC $10,X
                    0xd6, 0x11,  // DEC $11,X
                    0xd6, 0x12,  // DEC $12,X
                    0xd6, 0x13); // DEC $13,X

    cpu.setXRegister(0x30);

    cpu.step();
    assertEquals(0xff, bus.read(0x40));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());

    cpu.step();
    assertEquals(0x00, bus.read(0x41));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.step();
    assertEquals(0x7f, bus.read(0x42));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.step();
    assertEquals(0xfe, bus.read(0x43));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* SBC - Subtract with Carry - $f5 */

  public void test_SBC() throws MemoryAccessException {
    bus.write(0x40, 0x01);

    bus.loadProgram(0xa9, 0x00,  // LDA #$00
                    0xf5, 0x10); // SBC $10,X
    cpu.setXRegister(0x30);
    cpu.step(2);
    assertEquals(0xfe, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x7f,  // LDA #$7f
                    0xf5, 0x10); // SBC $10,X
    cpu.setXRegister(0x30);
    cpu.step(2);
    assertEquals(0x7d, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x80,  // LDA #$80
                    0xf5, 0x10); // SBC $10,X
    cpu.setXRegister(0x30);
    cpu.step(2);
    assertEquals(0x7e, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0xff,  // LDA #$ff
                    0xf5, 0x10); // SBC $10,X
    cpu.setXRegister(0x30);
    cpu.step(2);
    assertEquals(0xfd, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x02,  // LDA #$02
                    0xf5, 0x10); // SBC $10,X
    cpu.setXRegister(0x30);
    cpu.step(2);
    assertEquals(0x00, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertTrue(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());
  }

  public void test_SBC_IncludesNotOfCarry() throws MemoryAccessException {
    bus.write(0x40, 0x01);

    // Subtrace with Carry Flag cleared
    bus.loadProgram(0x18,        // CLC
                    0xa9, 0x05,  // LDA #$00
                    0xf5, 0x10); // SBC $10,X
    cpu.setXRegister(0x30);
    cpu.step(3);
    assertEquals(0x03, cpu.getAccumulator());

    cpu.reset();

    // Subtrace with Carry Flag cleared
    bus.loadProgram(0x18,        // CLC
                    0xa9, 0x00,  // LDA #$00
                    0xf5, 0x10); // SBC $10,X
    cpu.setXRegister(0x30);
    cpu.step(3);
    assertEquals(0xfe, cpu.getAccumulator());

    cpu.reset();

    // Subtract with Carry Flag set
    bus.loadProgram(0x38,        // SEC
                    0xa9, 0x05,  // LDA #$00
                    0xf5, 0x10); // SBC $10,X
    cpu.setXRegister(0x30);
    cpu.step(3);
    assertEquals(0x04, cpu.getAccumulator());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();

    // Subtract with Carry Flag set
    bus.loadProgram(0x38,        // SEC
                    0xa9, 0x00,  // LDA #$00
                    0xf5, 0x10); // SBC $10,X
    cpu.setXRegister(0x30);
    cpu.step(3);
    assertEquals(0xff, cpu.getAccumulator());
    assertFalse(cpu.getCarryFlag());

  }

  public void test_SBC_DecimalMode() throws MemoryAccessException {
    bus.write(0x40, 0x01);
    bus.write(0x50, 0x11);

    bus.loadProgram(0xf8,
                    0xa9, 0x00,
                    0xf5, 0x10);
    cpu.setXRegister(0x30);
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
                    0xf5, 0x10);
    cpu.setXRegister(0x30);
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
                    0xf5, 0x10);
    cpu.setXRegister(0x30);
    cpu.step(3);
    assertEquals(0x48, cpu.getAccumulator());
    assertTrue(cpu.getCarryFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getDecimalModeFlag());


    cpu.reset();

    bus.loadProgram(0xf8,         // SED
                    0xa9, 0x02,   // LDA #$02
                    0xf5, 0x10);  // SBC $10
    cpu.setXRegister(0x30);
    cpu.step(3);
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getCarryFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertTrue(cpu.getZeroFlag());
    assertTrue(cpu.getDecimalModeFlag());

    cpu.reset();

    bus.loadProgram(0xf8,         // SED
                    0xa9, 0x10,   // LDA #$10
                    0xf5, 0x20);  // SBC $20
    cpu.setXRegister(0x30);
    cpu.step(3);
    assertEquals(0x98, cpu.getAccumulator());
    assertFalse(cpu.getCarryFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getDecimalModeFlag());

    cpu.reset();

    bus.loadProgram(0x38,         // SEC
                    0xf8,         // SED
                    0xa9, 0x05,   // LDA #$05
                    0xf5, 0x10);  // SBC $10
    cpu.setXRegister(0x30);
    cpu.step(4);
    assertEquals(0x04, cpu.getAccumulator());
    assertTrue(cpu.getCarryFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getDecimalModeFlag());

    cpu.reset();

    bus.loadProgram(0x38,         // SEC
                    0xf8,         // SED
                    0xa9, 0x00,   // LDA #$00
                    0xf5, 0x10);  // SBC $10
    cpu.setXRegister(0x30);
    cpu.step(4);
    assertEquals(0x99, cpu.getAccumulator());
    assertFalse(cpu.getCarryFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getDecimalModeFlag());
  }

  /* INC - Increment Memory Location - $f6 */

  public void test_INC() throws MemoryAccessException {
    bus.write(0x30, 0x00);
    bus.write(0x31, 0x7f);
    bus.write(0x32, 0xff);

    cpu.setXRegister(0x20);

    bus.loadProgram(0xf6, 0x10,  // INC $10,X
                    0xf6, 0x11,  // INC $11,X
                    0xf6, 0x12); // INC $12,X

    cpu.step();
    assertEquals(0x01, bus.read(0x30));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.step();
    assertEquals(0x80, bus.read(0x31));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());

    cpu.step();
    assertEquals(0x00, bus.read(0x32));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

}
