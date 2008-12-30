package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryRangeException;
import junit.framework.TestCase;

public class CpuZeroPageYModeTest extends TestCase {

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
   * STX - $96
   * LDX - $b6
   *
   */

  /* STX - Store X Register - $96 */

  public void test_STX() {
    cpu.setYRegister(0x30);
    cpu.setXRegister(0x00);
    bus.loadProgram(0x96, 0x10);  // STX $10,Y
    cpu.step();
    assertEquals(0x00, bus.read(0x40));
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.reset();
    cpu.setYRegister(0x30);
    cpu.setXRegister(0x0f);
    bus.loadProgram(0x96, 0x10);  // STX $10,Y
    cpu.step();
    assertEquals(0x0f, bus.read(0x40));
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.reset();
    cpu.setYRegister(0x30);
    cpu.setXRegister(0x80);
    bus.loadProgram(0x96, 0x10);  // STX $10,Y
    cpu.step();
    assertEquals(0x80, bus.read(0x40));
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

  /* LDX - Load X Register - $b6 */

  public void test_LDX() {
    bus.write(0x40, 0x00);
    bus.write(0x41, 0x0f);
    bus.write(0x42, 0x80);

    bus.loadProgram(0xb6, 0x10,
                    0xb6, 0x11,
                    0xb6, 0x12);

    cpu.setYRegister(0x30);

    cpu.step();
    assertEquals(0x00, cpu.getXRegister());
    assertTrue(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.step();
    assertEquals(0x0f, cpu.getXRegister());
    assertFalse(cpu.getZeroFlag());
    assertFalse(cpu.getNegativeFlag());

    cpu.step();
    assertEquals(0x80, cpu.getXRegister());
    assertFalse(cpu.getZeroFlag());
    assertTrue(cpu.getNegativeFlag());
  }

}
