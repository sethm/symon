package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.InstructionTable;

import junit.framework.TestCase;

public class CpuIndirectModeTest extends TestCase {

    protected Cpu    cpu;
    protected Bus    bus;
    protected Memory mem;

    protected void setUp() throws Exception {
        this.cpu = new Cpu();
        this.bus = new Bus(0x0000, 0xffff);
        this.mem = new Memory(0x0000, 0x10000);
        bus.addCpu(cpu);
        bus.addDevice(mem);

        // Load the reset vector.
        bus.write(0xfffc, Bus.DEFAULT_LOAD_ADDRESS & 0x00ff);
        bus.write(0xfffd, (Bus.DEFAULT_LOAD_ADDRESS & 0xff00) >>> 8);

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
    * JMP - $6c
    *
    */

    /* JMP - Jump - $6c */

    public void test_JMP_notOnPageBoundary() throws MemoryAccessException {
        bus.write(0x3400, 0x00);
        bus.write(0x3401, 0x54);
        bus.loadProgram(0x6c, 0x00, 0x34);
        cpu.step();
        assertEquals(0x5400, cpu.getProgramCounter());
        // No change to status flags.
        assertEquals(0x20, cpu.getProcessorStatus());
    }

    public void test_JMP_with_ROR_Bug() throws MemoryAccessException {
        cpu.setBehavior(Cpu.CpuBehavior.NMOS_WITH_ROR_BUG);
        bus.write(0x3400, 0x22);
        bus.write(0x34ff, 0x00);
        bus.write(0x3500, 0x54);
        bus.loadProgram(0x6c, 0xff, 0x34);
        cpu.step();
        assertEquals(0x2200, cpu.getProgramCounter());
        // No change to status flags.
        assertEquals(0x20, cpu.getProcessorStatus());
    }

    public void test_JMP_withIndirectBug() throws MemoryAccessException {
        cpu.setBehavior(Cpu.CpuBehavior.NMOS_WITH_INDIRECT_JMP_BUG);
        bus.write(0x3400, 0x22);
        bus.write(0x34ff, 0x00);
        bus.write(0x3500, 0x54);
        bus.loadProgram(0x6c, 0xff, 0x34);
        cpu.step();
        assertEquals(0x2200, cpu.getProgramCounter());
        // No change to status flags.
        assertEquals(0x20, cpu.getProcessorStatus());
    }

    public void test_JMP_withOutIndirectBug() throws MemoryAccessException {
        cpu.setBehavior(Cpu.CpuBehavior.NMOS_WITHOUT_INDIRECT_JMP_BUG);
        bus.write(0x3400, 0x22);
        bus.write(0x34ff, 0x00);
        bus.write(0x3500, 0x54);
        bus.loadProgram(0x6c, 0xff, 0x34);
        cpu.step();
        assertEquals(0x5400, cpu.getProgramCounter());
        // No change to status flags.
        assertEquals(0x20, cpu.getProcessorStatus());
    }

    public void test_JMP_cmos() throws MemoryAccessException {
        cpu.setBehavior(Cpu.CpuBehavior.CMOS);
        bus.write(0x3400, 0x22);
        bus.write(0x34ff, 0x00);
        bus.write(0x3500, 0x54);
        bus.loadProgram(0x6c, 0xff, 0x34);
        cpu.step();
        assertEquals(0x5400, cpu.getProgramCounter());
        // No change to status flags.
        assertEquals(0x20, cpu.getProcessorStatus());
    }

}