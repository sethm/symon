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

    // Set the IRQ vector
    bus.write(Cpu.IRQ_VECTOR_H, 0x12);
    bus.write(Cpu.IRQ_VECTOR_L, 0x34);

    bus.loadProgram(0xea,
                    0xea,
                    0xea,
                    0x00,
                    0xea,
                    0xea);

    cpu.step(3); // Three NOP instructions

    assertEquals(0x203, cpu.getProgramCounter());

    cpu.step(); // Triggers the BRK

    // Was at PC = 0x204.  PC+2 should now be on the stack
    assertEquals(0x02, bus.read(0x1ff)); // PC high byte
    assertEquals(0x06, bus.read(0x1fe)); // PC low byte
    assertEquals(0x20|Cpu.P_CARRY|Cpu.P_OVERFLOW|Cpu.P_BREAK,
                 bus.read(0x1fd));       // Processor Status, with B set

    // Interrupt vector held 0x1234, so we should be there.
    assertEquals(0x1234, cpu.getProgramCounter());
    assertEquals(0xfc, cpu.getStackPointer());

    // B and I flags should have been set on P
    assertEquals(0x20|Cpu.P_CARRY|Cpu.P_OVERFLOW|Cpu.P_BREAK|
                 Cpu.P_IRQ_DISABLE,
                 cpu.getProcessorStatus());
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

  /* CLC - Clear Carry Flag - $18 */
  public void test_CLC() {
    cpu.setCarryFlag();
    assertTrue(cpu.getCarryFlag());

    bus.loadProgram(0x18);
    cpu.step();

    assertFalse(cpu.getCarryFlag());
  }

  /* CLD - Clear Decimal Mode Flag - $d8 */
  public void test_CLD() {
    cpu.setDecimalModeFlag();
    assertTrue(cpu.getDecimalModeFlag());

    bus.loadProgram(0xd8);
    cpu.step();

    assertFalse(cpu.getDecimalModeFlag());
  }

  /* CLI - Clear Interrupt Disabled Flag - $58 */
  public void test_CLI() {
    cpu.setIrqDisableFlag();
    assertTrue(cpu.getIrqDisableFlag());

    bus.loadProgram(0x58);
    cpu.step();

    assertFalse(cpu.getIrqDisableFlag());
  }

  /* CLV - Clear Overflow Flag - $b8 */
  public void test_CLV() {
    cpu.setOverflowFlag();
    assertTrue(cpu.getOverflowFlag());

    bus.loadProgram(0xb8);
    cpu.step();

    assertFalse(cpu.getOverflowFlag());
  }

  /* DEX - Decrement the X register - $ca */

  public void test_DEX() {
    bus.loadProgram(0xca);
    cpu.setXRegister(0x02);
    cpu.step();
    assertEquals(0x01, cpu.getXRegister());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_DEX_SetsZeroFlagWhenZero() {
    bus.loadProgram(0xca);
    cpu.setXRegister(0x01);
    cpu.step();
    assertEquals(0x00, cpu.getXRegister());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_DEX_SetsNegativeFlagWhen() {
    bus.loadProgram(0xca);
    cpu.step();
    assertEquals(0xff, cpu.getXRegister());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* DEY - Decrement the Y register - $88 */

  public void test_DEY() {
    bus.loadProgram(0x88);
    cpu.setYRegister(0x02);
    cpu.step();
    assertEquals(0x01, cpu.getYRegister());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_DEY_SetsZeroFlagWhenZero() {
    bus.loadProgram(0x88);
    cpu.setYRegister(0x01);
    cpu.step();
    assertEquals(0x00, cpu.getYRegister());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_DEY_SetsNegativeFlagWhen() {
    bus.loadProgram(0x88);
    cpu.step();
    assertEquals(0xff, cpu.getYRegister());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* INX - Increment the X register - $e8 */

  public void test_INX() {
    bus.loadProgram(0xe8);
    cpu.step();
    assertEquals(0x01, cpu.getXRegister());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_INX_SetsNegativeFlagWhenNegative() {
    bus.loadProgram(0xe8);
    cpu.setXRegister(0x7f);
    cpu.step();
    assertEquals(0x80, cpu.getXRegister());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  public void test_INX_SetsZeroFlagWhenZero() {
    bus.loadProgram(0xe8);
    cpu.setXRegister(0xff);
    cpu.step();
    assertEquals(0x00, cpu.getXRegister());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  /* INY - Increment the Y register - $c8 */

  public void test_INY() {
    bus.loadProgram(0xc8);
    cpu.step();
    assertEquals(0x01, cpu.getYRegister());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_INY_SetsNegativeFlagWhenNegative() {
    bus.loadProgram(0xc8);
    cpu.setYRegister(0x7f);
    cpu.step();
    assertEquals(0x80, cpu.getYRegister());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  public void test_INY_SetsZeroFlagWhenZero() {
    bus.loadProgram(0xc8);
    cpu.setYRegister(0xff);
    cpu.step();
    assertEquals(0x00, cpu.getYRegister());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  /* NOP - No Operation - $ea */

  public void test_NOP() {
    bus.loadProgram(0xea);
    cpu.step();
    // Should just not change anything except PC
    assertEquals(0, cpu.getAccumulator());
    assertEquals(0, cpu.getXRegister());
    assertEquals(0, cpu.getYRegister());
    assertEquals(0x201, cpu.getProgramCounter());
    assertEquals(0xff, cpu.getStackPointer());
    assertEquals(0x20, cpu.getProcessorStatus());
  }

  /* PHA - Push Accumulator - $48 */

  public void test_PHA() {
    bus.loadProgram(0x48);
    cpu.setAccumulator(0x3a);
    cpu.step();
    assertEquals(0xfe, cpu.getStackPointer());
    assertEquals(0x3a, cpu.stackPeek());
  }

  /* PHP - Push Processor Status - $08 */

  public void test_PHP() {
    bus.loadProgram(0x08);
    cpu.setProcessorStatus(0x27);
    cpu.step();
    assertEquals(0xfe, cpu.getStackPointer());
    assertEquals(0x27, cpu.stackPeek());
  }

  /* PLA - Pul Accumulator - $68 */

  public void test_PLA() {
    cpu.stackPush(0x32);
    bus.loadProgram(0x68);
    cpu.step();
    assertEquals(0x32, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertFalse(cpu.getZeroFlag());
  }

  public void test_PLA_SetsZeroIfAccumulatorIsZero() {
    cpu.stackPush(0x00);
    bus.loadProgram(0x68);
    cpu.step();
    assertEquals(0x00, cpu.getAccumulator());
    assertFalse(cpu.getNegativeFlag());
    assertTrue(cpu.getZeroFlag());
  }

  public void test_PLA_SetsNegativeIfAccumulatorIsNegative() {
    cpu.stackPush(0xff);
    bus.loadProgram(0x68);
    cpu.step();
    assertEquals(0xff, cpu.getAccumulator());
    assertTrue(cpu.getNegativeFlag());
    assertFalse(cpu.getZeroFlag());
  }

  /* PLP - Pull Processor Status - $28 */

  public void test_PLP() {
    cpu.stackPush(0x2f);
    bus.loadProgram(0x28);
    cpu.step();
    assertEquals(0x2f, cpu.getProcessorStatus());
  }

  /* RTI - Return from Interrupt - $40 */

  public void test_RTI() {
    cpu.stackPush(0x0f); // PC hi
    cpu.stackPush(0x11); // PC lo
    cpu.stackPush(0x29); // status

    bus.loadProgram(0x40);
    cpu.step();

    assertEquals(0x0f11, cpu.getProgramCounter());
    assertEquals(0x29, cpu.getProcessorStatus());
  }

  /* RTS - Return from Subroutine - $60 */

  public void test_RTS() {
    cpu.stackPush(0x0f); // PC hi
    cpu.stackPush(0x11); // PC lo

    bus.loadProgram(0x60);
    cpu.step();

    assertEquals(0x0f12, cpu.getProgramCounter());
    assertEquals(0x20, cpu.getProcessorStatus());
  }

  /* SEC - Set Carry Flag - $38 */

  public void test_SEC() {
    bus.loadProgram(0x38);
    cpu.step();
    assertTrue(cpu.getCarryFlag());
  }

  /* SED - Set Decimal Mode Flag - $f8 */

  public void test_SED() {
    bus.loadProgram(0xf8);
    cpu.step();
    assertTrue(cpu.getDecimalModeFlag());
  }

  /* SEI - Set Interrupt Disable Flag - $78 */

  public void test_SEI() {
    bus.loadProgram(0x78);
    cpu.step();
    assertTrue(cpu.getIrqDisableFlag());
  }

  /* TAX - Transfer Accumulator to X Register - $aa */

  public void test_TAX() {
    cpu.setAccumulator(0x32);
    bus.loadProgram(0xaa);
    cpu.step();
    assertEquals(0x32, cpu.getXRegister());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_TAX_SetsZeroFlagIfXIsZero() {
    cpu.setAccumulator(0x00);
    bus.loadProgram(0xaa);
    cpu.step();
    assertEquals(0x00, cpu.getXRegister());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_TAX_SetsNegativeFlagIfXIsNegative() {
    cpu.setAccumulator(0xff);
    bus.loadProgram(0xaa);
    cpu.step();
    assertEquals(0xff, cpu.getXRegister());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* TAY - Transfer Accumulator to Y Register - $a8 */

  public void test_TAY() {
    cpu.setAccumulator(0x32);
    bus.loadProgram(0xa8);
    cpu.step();
    assertEquals(0x32, cpu.getYRegister());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_TAY_SetsZeroFlagIfYIsZero() {
    cpu.setAccumulator(0x00);
    bus.loadProgram(0xa8);
    cpu.step();
    assertEquals(0x00, cpu.getYRegister());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_TAY_SetsNegativeFlagIfYIsNegative() {
    cpu.setAccumulator(0xff);
    bus.loadProgram(0xa8);
    cpu.step();
    assertEquals(0xff, cpu.getYRegister());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* TSX - Transfer Stack Pointer to X Register - $ba */

  public void test_TSX() {
    cpu.setStackPointer(0x32);
    bus.loadProgram(0xba);
    cpu.step();
    assertEquals(0x32, cpu.getXRegister());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_TSX_SetsZeroFlagIfXIsZero() {
    cpu.setStackPointer(0x00);
    bus.loadProgram(0xba);
    cpu.step();
    assertEquals(0x00, cpu.getXRegister());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_TSX_SetsNegativeFlagIfXIsNegative() {
    cpu.setStackPointer(0xff);
    bus.loadProgram(0xba);
    cpu.step();
    assertEquals(0xff, cpu.getXRegister());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* TXA - Transfer X Register to Accumulator - $8a */

  public void test_TXA() {
    cpu.setXRegister(0x32);
    bus.loadProgram(0x8a);
    cpu.step();
    assertEquals(0x32, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_TXA_SetsZeroFlagIfAccumulatorIsZero() {
    cpu.setXRegister(0x00);
    bus.loadProgram(0x8a);
    cpu.step();
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_TXA_SetsNegativeFlagIfAccumulatorIsNegative() {
    cpu.setXRegister(0xff);
    bus.loadProgram(0x8a);
    cpu.step();
    assertEquals(0xff, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* TXS - Transfer X Register to Stack Pointer - $9a */

  public void test_TXS() {
    cpu.setXRegister(0x32);
    bus.loadProgram(0x9a);
    cpu.step();
    assertEquals(0x32, cpu.getStackPointer());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_TXS_DoesNotAffectProcessorStatus() {
    cpu.setXRegister(0x00);
    bus.loadProgram(0x9a);
    cpu.step();
    assertEquals(0x00, cpu.getStackPointer());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.setXRegister(0x80);
    bus.loadProgram(0x9a);
    cpu.step();
    assertEquals(0x80, cpu.getStackPointer());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  /* TYA - Transfer Y Register to Accumulator - $98 */

  public void test_TYA() {
    cpu.setYRegister(0x32);
    bus.loadProgram(0x98);
    cpu.step();
    assertEquals(0x32, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_TYA_SetsZeroFlagIfAccumulatorIsZero() {
    cpu.setYRegister(0x00);
    bus.loadProgram(0x98);
    cpu.step();
    assertEquals(0x00, cpu.getAccumulator());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());
  }

  public void test_TYA_SetsNegativeFlagIfAccumulatorIsNegative() {
    cpu.setYRegister(0xff);
    bus.loadProgram(0x98);
    cpu.step();
    assertEquals(0xff, cpu.getAccumulator());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

}