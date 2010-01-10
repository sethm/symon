package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryAccessException;
import junit.framework.TestCase;

public class CpuIndirectXModeTest extends TestCase {

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
   * ORA - $01
   * AND - $21
   * EOR - $41
   * ADC - $61
   * STA - $81
   *
   * LDA - $a1
   * CMP - $c1
   * SBC - $e1
   *
   */

  /* ORA - Logical Inclusive OR - $1d */

  public void test_ORA() throws MemoryAccessException {
    // Set some initial values in memory
    bus.write(0x2c30, 0x00);
    bus.write(0x2c32, 0x11);
    bus.write(0x2c34, 0x22);
    bus.write(0x2c38, 0x44);
    bus.write(0x2c40, 0x88);

    // Set offset in X register.
    cpu.setXRegister(0x30);

    bus.loadProgram(0x1d, 0x00, 0x2c,  // ORA $2c00,X
                    0x1d, 0x02, 0x2c,  // ORA $2c02,X
                    0x1d, 0x04, 0x2c,  // ORA $2c04,X
                    0x1d, 0x08, 0x2c,  // ORA $2c08,X
                    0x1d, 0x10, 0x2c); // ORA $2c10,X

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

  /* AND - Logical AND - $3d */

  public void test_AND() throws MemoryAccessException {
    bus.write(0x1a30, 0x00);
    bus.write(0x1a31, 0x11);
    bus.write(0x1a32, 0xff);
    bus.write(0x1a33, 0x99);
    bus.write(0x1a34, 0x11);
    bus.write(0x1a35, 0x0f);
    bus.write(0x1a02, 0x11);

    // Set offset in X register.
    cpu.setXRegister(0x30);

    bus.loadProgram(0x3d, 0x00, 0x1a,  // AND $1a00,X
                    0x3d, 0x01, 0x1a,  // AND $1a01,X
                    0xa9, 0xaa,        // LDA #$aa
                    0x3d, 0x02, 0x1a,  // AND $1a02,X
                    0x3d, 0x03, 0x1a,  // AND $1a03,X
                    0x3d, 0x04, 0x1a,  // AND $1a04,X
                    0xa9, 0xff,        // LDA #$ff
                    0x3d, 0x05, 0x1a,  // AND $1a05,X
                    0xa9, 0x01,        // LDA #$01
                    0x3d, 0xd2, 0x1a); // AND $1ad2,X
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

  /* EOR - Exclusive OR - $5d */

  public void test_EOR() throws MemoryAccessException {
    bus.write(0xab40, 0x00);
    bus.write(0xab41, 0xff);
    bus.write(0xab42, 0x33);
    bus.write(0xab43, 0x44);

    cpu.setXRegister(0x30);

    bus.loadProgram(0xa9, 0x88,         // LDA #$88
                    0x5d, 0x10, 0xab,  // EOR $ab10,X
                    0x5d, 0x11, 0xab,  // EOR $ab11,X
                    0x5d, 0x12, 0xab,  // EOR $ab12,X
                    0x5d, 0x13, 0xab); // EOR $ab13,X
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

  /* ADC - Add with Carry - $7d */

  public void test_ADC() throws MemoryAccessException {
    bus.write(0xab40, 0x01);
    bus.write(0xab41, 0xff);

    cpu.setXRegister(0x30);

    bus.loadProgram(0xa9, 0x00,        // LDA #$00
                    0x7d, 0x10, 0xab); // ADC $ab10,X
    cpu.step(2);
    assertEquals(0x01, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x7f,        // LDA #$7f
                    0x7d, 0x10, 0xab); // ADC $ab10,X
    cpu.step(2);
    assertEquals(0x80, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertTrue(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x80,        // LDA #$80
                    0x7d, 0x10, 0xab); // ADC $ab10,X
    cpu.step(2);
    assertEquals(0x81, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0xff,        // LDA #$ff
                    0x7d, 0x10, 0xab); // ADC $ab10,X
    cpu.step(2);
    assertEquals(0x00, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertTrue(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x00,        // LDA #$00
                    0x7d, 0x11, 0xab); // ADC $ab11,X
    cpu.step(2);
    assertEquals(0xff, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x7f,        // LDA #$7f
                    0x7d, 0x11, 0xab); // ADC $ab11,X
    cpu.step(2);
    assertEquals(0x7e, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x80,        // LDA #$80
                    0x7d, 0x11, 0xab); // ADC $ab11,X
    cpu.step(2);
    assertEquals(0x7f, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0xff,        // LDA #$ff
                    0x7d, 0x11, 0xab); // ADC $ab11,X
    cpu.step(2);
    assertEquals(0xfe, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());
  }

  public void test_ADC_IncludesCarry() throws MemoryAccessException {
    bus.write(0xab40, 0x01);

    bus.loadProgram(0xa9, 0x00,        // LDA #$00
                    0x38,              // SEC
                    0x7d, 0x10, 0xab); // ADC $ab10,X

    cpu.setXRegister(0x30);

    cpu.step(3);
    assertEquals(0x02, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());
  }

  public void test_ADC_DecimalMode() throws MemoryAccessException {
    bus.write(0xab40, 0x01);
    bus.write(0xab41, 0x99);

    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x01,        // LDA #$01
                    0x7d, 0x10, 0xab); // ADC $ab10,X

    cpu.setXRegister(0x30);

    cpu.step(3);
    assertEquals(0x02, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x49,        // LDA #$49
                    0x7d, 0x10, 0xab); // ADC $ab10,X
    cpu.step(3);
    assertEquals(0x50, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x50,        // LDA #$50
                    0x7d, 0x10, 0xab); // ADC $ab10,X
    cpu.step(3);
    assertEquals(0x51, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x99,        // LDA #$99
                    0x7d, 0x10, 0xab); // ADC $ab10,X
    cpu.step(3);
    assertEquals(0x00, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertTrue(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x00,        // LDA #$00
                    0x7d, 0x11, 0xab); // ADC $ab10,X
    cpu.step(3);
    assertEquals(0x99, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x49,        // LDA #$49
                    0x7d, 0x11, 0xab); // ADC $ab11,X
    cpu.step(3);
    assertEquals(0x48, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x50,        // LDA #$59
                    0x7d, 0x11, 0xab); // ADC $ab11,X
    cpu.step(3);
    assertEquals(0x49, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());
  }

  /* STA - Store Accumulator - $9d */

  public void test_STA() throws MemoryAccessException {
    cpu.setXRegister(0x30);

    cpu.setAccumulator(0x00);
    bus.loadProgram(0x9d, 0x10, 0xab); // STA $ab10,X
    cpu.step();
    assertEquals(0x00, bus.read(0xab40));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.reset();

    cpu.setAccumulator(0x0f);
    bus.loadProgram(0x9d, 0x10, 0xab); // STA $ab10,X
    cpu.step();
    assertEquals(0x0f, bus.read(0xab40));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.reset();

    cpu.setAccumulator(0x80);
    bus.loadProgram(0x9d, 0x10, 0xab); // STA $ab10,X
    cpu.step();
    assertEquals(0x80, bus.read(0xab40));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* LDA - Load Accumulator - $bd */

  public void test_LDA() throws MemoryAccessException {
    bus.write(0xab42, 0x00);
    bus.write(0xab43, 0x0f);
    bus.write(0xab44, 0x80);

    bus.loadProgram(0xbd, 0x10, 0xab,  // LDA $ab10,X
                    0xbd, 0x11, 0xab,  // LDA $ab11,X
                    0xbd, 0x12, 0xab); // LDA $ab12,X

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

  /* CMP - Compare Accumulator - $dd */

  public void test_CMP() throws MemoryAccessException {
    bus.write(0xab40, 0x00);
    bus.write(0xab41, 0x80);
    bus.write(0xab42, 0xff);

    cpu.setAccumulator(0x80);

    bus.loadProgram(0xdd, 0x10, 0xab,  // CMP $ab10,X
                    0xdd, 0x11, 0xab,  // CMP $ab11,X
                    0xdd, 0x12, 0xab); // CMP $ab12,X

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

  /* SBC - Subtract with Carry - $fd */

  public void test_SBC() throws MemoryAccessException {
    bus.write(0xab40, 0x01);

    bus.loadProgram(0xa9, 0x00,        // LDA #$00
                    0xfd, 0x10, 0xab); // SBC $ab10,X
    cpu.setXRegister(0x30);
    cpu.step(2);
    assertEquals(0xfe, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x7f,        // LDA #$7f
                    0xfd, 0x10, 0xab); // SBC $ab10,X
    cpu.setXRegister(0x30);
    cpu.step(2);
    assertEquals(0x7d, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x80,        // LDA #$80
                    0xfd, 0x10, 0xab); // SBC $ab10,X
    cpu.setXRegister(0x30);
    cpu.step(2);
    assertEquals(0x7e, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0xff,        // LDA #$ff
                    0xfd, 0x10, 0xab); // SBC $ab10,X
    cpu.setXRegister(0x30);
    cpu.step(2);
    assertEquals(0xfd, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();
    bus.loadProgram(0xa9, 0x02,        // LDA #$02
                    0xfd, 0x10, 0xab); // SBC $ab10,X
    cpu.setXRegister(0x30);
    cpu.step(2);
    assertEquals(0x00, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertTrue(cpu.getZeroFlag());
    assertTrue(cpu.getCarryFlag());
  }

  public void test_SBC_IncludesNotOfCarry() throws MemoryAccessException {
    bus.write(0xab40, 0x01);

    // Subtrace with Carry Flag cleared
    bus.loadProgram(0x18,              // CLC
                    0xa9, 0x05,        // LDA #$00
                    0xfd, 0x10, 0xab); // SBC $ab10,X
    cpu.setXRegister(0x30);
    cpu.step(3);
    assertEquals(0x03, cpu.getAccumulator());

    cpu.reset();

    // Subtrace with Carry Flag cleared
    bus.loadProgram(0x18,              // CLC
                    0xa9, 0x00,        // LDA #$00
                    0xfd, 0x10, 0xab); // SBC $ab10,X
    cpu.setXRegister(0x30);
    cpu.step(3);
    assertEquals(0xfe, cpu.getAccumulator());

    cpu.reset();

    // Subtract with Carry Flag set
    bus.loadProgram(0x38,              // SEC
                    0xa9, 0x05,        // LDA #$00
                    0xfd, 0x10, 0xab); // SBC $ab10,X
    cpu.setXRegister(0x30);
    cpu.step(3);
    assertEquals(0x04, cpu.getAccumulator());
    assertTrue(cpu.getCarryFlag());

    cpu.reset();

    // Subtract with Carry Flag set
    bus.loadProgram(0x38,              // SEC
                    0xa9, 0x00,        // LDA #$00
                    0xfd, 0x10, 0xab); // SBC $ab10,X
    cpu.setXRegister(0x30);
    cpu.step(3);
    assertEquals(0xff, cpu.getAccumulator());
    assertFalse(cpu.getCarryFlag());

  }

  public void test_SBC_DecimalMode() throws MemoryAccessException {
    bus.write(0xab40, 0x01);
    bus.write(0xab50, 0x11);

    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x00,        // LDA #$00
                    0xfd, 0x10, 0xab); // SBC $ab10,X
    cpu.setXRegister(0x30);
    cpu.step(3);
    assertEquals(0x98, cpu.getAccumulator());
    assertFalse(cpu.getCarryFlag()); // borrow = set flag
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getDecimalModeFlag());

    cpu.reset();

    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x99,        // LDA #$99
                    0xfd, 0x10, 0xab); // SBC $ab10,X
    cpu.setXRegister(0x30);
    cpu.step(3);
    assertEquals(0x97, cpu.getAccumulator());
    assertTrue(cpu.getCarryFlag()); // No borrow = clear flag
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getDecimalModeFlag());

    cpu.reset();

    bus.loadProgram(0xf8,              // SED
                    0xa9, 0x50,        // LDA #$50
                    0xfd, 0x10, 0xab); // SBC $ab10,X
    cpu.setXRegister(0x30);
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
                    0xfd, 0x10, 0xab);  // SBC $ab10,X
    cpu.setXRegister(0x30);
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
                    0xfd, 0x20, 0xab);  // SBC $ab20,X
    cpu.setXRegister(0x30);
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
                    0xfd, 0x10, 0xab);  // SBC $ab10,X
    cpu.setXRegister(0x30);
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
                    0xfd, 0x10, 0xab);  // SBC $ab10,X
    cpu.setXRegister(0x30);
    cpu.step(4);
    assertEquals(0x99, cpu.getAccumulator());
    assertFalse(cpu.getCarryFlag());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getOverflowFlag());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getDecimalModeFlag());
  }

}
