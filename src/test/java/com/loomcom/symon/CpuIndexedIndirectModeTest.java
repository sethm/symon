package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryAccessException;

import org.junit.*;
import static org.junit.Assert.*;

public class CpuIndexedIndirectModeTest {

  protected Cpu cpu;
  protected Bus bus;
  protected Memory mem;

  @Before
  public void runBeforeEveryTest() throws Exception {
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

  @Test
  public void test_LDA() throws Exception {
    bus.write(0x008c, 0x3f);
    bus.write(0x008d, 0xc4);
    bus.write(0xc43f, 0x45);

    cpu.setXRegister(0x0c);

    bus.loadProgram(0xa1, 0x80); // LDA ($80,X)
    cpu.step(1);

    assertEquals(0x45, cpu.getAccumulator());
  }

  @Test
  public void test_ORA() throws Exception {
    bus.write(0x0012, 0x1f);
    bus.write(0x0013, 0xc5);
    bus.write(0xc51f, 0x31);

    cpu.setXRegister(0x02);
    cpu.setAccumulator(0x15);

    bus.loadProgram(0x01, 0x10); // ORA ($10,X)
    cpu.step(1);

    assertEquals(0x35, cpu.getAccumulator());
    assertEquals(0x31, bus.read(0xc51f));
  }

  @Test
  public void test_AND() throws Exception {
    bus.write(0x0012, 0x1f);
    bus.write(0x0013, 0xc5);
    bus.write(0xc51f, 0x31);

    cpu.setXRegister(0x02);
    cpu.setAccumulator(0x15);

    bus.loadProgram(0x21, 0x10); // AND ($10,X)
    cpu.step(1);

    assertEquals(0x11, cpu.getAccumulator());
    assertEquals(0x31, bus.read(0xc51f));
  }
}