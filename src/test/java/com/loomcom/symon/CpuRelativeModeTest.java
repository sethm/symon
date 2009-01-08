package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryRangeException;
import junit.framework.TestCase;

public class CpuRelativeModeTest extends TestCase {

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
   * BPL - Branch if Positive          - 0x10
   * BMI - Branch if Minus             - 0x30
   * BVC - Branch if Overflow Clear    - 0x50
   * BVS - Branch if Overflow Set      - 0x70
   * BCC - Branch if Carry Clear       - 0x90
   * BCS - Branch if Carry Set         - 0xb0
   * BNE - Branch if Not Equal to Zero - 0xd0
   * BEQ - Branch if Equal to Zero     - 0xf0
   *
   */

  /* BPL - Branch if Positive          - 0x10 */

  public void test_BPL() {
    // Positive Offset
    bus.loadProgram(0x10, 0x05);  // BPL $05 ; *=$0202+$05 ($0207)
    cpu.setNegativeFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0x10, 0x05);  // BPL $05 ; *=$0202+$05 ($0207)
    cpu.clearNegativeFlag();
    cpu.step();
    assertEquals(0x207, cpu.getProgramCounter());

    // Negative Offset
    cpu.reset();
    bus.loadProgram(0x10, 0xfb);  // BPL $fb ; *=$0202-$05 ($01fd)
    cpu.setNegativeFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0x10, 0xfb);  // BPL $fb ; *=$0202-$05 ($01fd)
    cpu.clearNegativeFlag();
    cpu.step();
    assertEquals(0x1fd, cpu.getProgramCounter());
  }

  /* BMI - Branch if Minus             - 0x30 */

  public void test_BMI() {
    // Positive Offset
    bus.loadProgram(0x30, 0x05);  // BMI $05 ; *=$0202+$05 ($0207)
    cpu.setNegativeFlag();
    cpu.step();
    assertEquals(0x207, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0x30, 0x05);  // BMI $05 ; *=$0202+$05 ($0207)
    cpu.clearNegativeFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());

    // Negative Offset
    cpu.reset();
    bus.loadProgram(0x30, 0xfb);  // BMI $fb ; *=$0202-$05 ($01fd)
    cpu.setNegativeFlag();
    cpu.step();
    assertEquals(0x1fd, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0x30, 0xfb);  // BMI $fb ; *=$0202-$05 ($01fd)
    cpu.clearNegativeFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());
  }

  /* BVC - Branch if Overflow Clear    - 0x50 */

  public void test_BVC() {
    // Positive Offset
    bus.loadProgram(0x50, 0x05);  // BVC $05 ; *=$0202+$05 ($0207)
    cpu.setOverflowFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0x50, 0x05);  // BVC $05 ; *=$0202+$05 ($0207)
    cpu.clearOverflowFlag();
    cpu.step();
    assertEquals(0x207, cpu.getProgramCounter());

    // Negative Offset
    cpu.reset();
    bus.loadProgram(0x50, 0xfb);  // BVC $fb ; *=$0202-$05 ($01fd)
    cpu.setOverflowFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0x50, 0xfb);  // BVC $fb ; *=$0202-$05 ($01fd)
    cpu.clearOverflowFlag();
    cpu.step();
    assertEquals(0x1fd, cpu.getProgramCounter());
  }

  /* BVS - Branch if Overflow Set      - 0x70 */

  public void test_BVS() {
    // Positive Offset
    bus.loadProgram(0x70, 0x05);  // BVS $05 ; *=$0202+$05 ($0207)
    cpu.setOverflowFlag();
    cpu.step();
    assertEquals(0x207, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0x70, 0x05);  // BVS $05 ; *=$0202+$05 ($0207)
    cpu.clearOverflowFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());

    // Negative Offset
    cpu.reset();
    bus.loadProgram(0x70, 0xfb);  // BVS $fb ; *=$0202-$05 ($01fd)
    cpu.setOverflowFlag();
    cpu.step();
    assertEquals(0x1fd, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0x70, 0xfb);  // BVS $fb ; *=$0202-$05 ($01fd)
    cpu.clearOverflowFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());
  }

  /* BCC - Branch if Carry Clear       - 0x90 */

  public void test_BCC() {
    // Positive Offset
    bus.loadProgram(0x90, 0x05);  // BCC $05 ; *=$0202+$05 ($0207)
    cpu.setCarryFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0x90, 0x05);  // BCC $05 ; *=$0202+$05 ($0207)
    cpu.clearCarryFlag();
    cpu.step();
    assertEquals(0x207, cpu.getProgramCounter());

    // Negative Offset
    cpu.reset();
    bus.loadProgram(0x90, 0xfb);  // BCC $fb ; *=$0202-$05 ($01fd)
    cpu.setCarryFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0x90, 0xfb);  // BCC $fb ; *=$0202-$05 ($01fd)
    cpu.clearCarryFlag();
    cpu.step();
    assertEquals(0x1fd, cpu.getProgramCounter());
  }

  /* BCS - Branch if Carry Set         - 0xb0 */

  public void test_BCS() {
    // Positive Offset
    bus.loadProgram(0xb0, 0x05);  // BCS $05 ; *=$0202+$05 ($0207)
    cpu.setCarryFlag();
    cpu.step();
    assertEquals(0x207, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0xb0, 0x05);  // BCS $05 ; *=$0202+$05 ($0207)
    cpu.clearCarryFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());

    // Negative Offset
    cpu.reset();
    bus.loadProgram(0xb0, 0xfb);  // BCS $fb ; *=$0202-$05 ($01fd)
    cpu.setCarryFlag();
    cpu.step();
    assertEquals(0x1fd, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0xb0, 0xfb);  // BCS $fb ; *=$0202-$05 ($01fd)
    cpu.clearCarryFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());
  }

  /* BNE - Branch if Not Equal to Zero - 0xd0 */

  public void test_BNE() {
     // Positive Offset
    bus.loadProgram(0xd0, 0x05);  // BNE $05 ; *=$0202+$05 ($0207)
    cpu.setZeroFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0xd0, 0x05);  // BNE $05 ; *=$0202+$05 ($0207)
    cpu.clearZeroFlag();
    cpu.step();
    assertEquals(0x207, cpu.getProgramCounter());

    // Negative Offset
    cpu.reset();
    bus.loadProgram(0xd0, 0xfb);  // BNE $fb ; *=$0202-$05 ($01fd)
    cpu.setZeroFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0xd0, 0xfb);  // BNE $fb ; *=$0202-$05 ($01fd)
    cpu.clearZeroFlag();
    cpu.step();
    assertEquals(0x1fd, cpu.getProgramCounter());
  }

  /* BEQ - Branch if Equal to Zero     - 0xf0 */

  public void test_BEQ() {
     // Positive Offset
    bus.loadProgram(0xf0, 0x05);  // BEQ $05 ; *=$0202+$05 ($0207)
    cpu.setZeroFlag();
    cpu.step();
    assertEquals(0x207, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0xf0, 0x05);  // BEQ $05 ; *=$0202+$05 ($0207)
    cpu.clearZeroFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());

    // Negative Offset
    cpu.reset();
    bus.loadProgram(0xf0, 0xfb);  // BEQ $fb ; *=$0202-$05 ($01fd)
    cpu.setZeroFlag();
    cpu.step();
    assertEquals(0x1fd, cpu.getProgramCounter());

    cpu.reset();
    bus.loadProgram(0xf0, 0xfb);  // BEQ $fb ; *=$0202-$05 ($01fd)
    cpu.clearZeroFlag();
    cpu.step();
    assertEquals(0x202, cpu.getProgramCounter());
  }

}