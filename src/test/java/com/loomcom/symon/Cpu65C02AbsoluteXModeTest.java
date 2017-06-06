package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryAccessException;
import junit.framework.*;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for Zero Page Indirect addressing mode, found on some instructions
 * in the 65C02 and 65816
 */
public class Cpu65C02AbsoluteXModeTest extends TestCase {
    protected Cpu    cpu;
    protected Bus    bus;
    protected Memory mem;

    private void makeCmosCpu() throws Exception {
        makeCpu(InstructionTable.CpuBehavior.CMOS_6502);
    }

    private void makeNmosCpu() throws Exception {
        makeCpu(InstructionTable.CpuBehavior.NMOS_6502);
    }

    private void makeCpu(InstructionTable.CpuBehavior behavior) throws Exception {
        this.cpu = new Cpu(behavior);
        this.bus = new Bus(0x0000, 0xffff);
        this.mem = new Memory(0x0000, 0xffff);
        bus.addCpu(cpu);
        bus.addDevice(mem);

        // Load the reset vector.
        bus.write(0xfffc, Bus.DEFAULT_LOAD_ADDRESS & 0x00ff);
        bus.write(0xfffd, (Bus.DEFAULT_LOAD_ADDRESS & 0xff00) >>> 8);

        cpu.reset();
    }

    public void test_STZ() throws Exception {
        makeCmosCpu();
        bus.write(0x0011,0xff);

        bus.loadProgram(0x9e, 0x10, 0x00);  // STZ Absolute,X

        // Test STZ Absolute,X ($0011)
        cpu.setXRegister(0x01);
        assertEquals(0xff, bus.read(0x0011, true));
        cpu.step();
        assertEquals(0x00, bus.read(0x0011, true));
    }

    public void test_STZRequiresCmosCpu() throws Exception {
        makeNmosCpu();
        bus.write(0x0011,0xff);

        bus.loadProgram(0x9e, 0x10, 0x00);  // STZ Absolute,X

        // Test STZ Absolute,X ($0011)
        cpu.setXRegister(0x01);
        assertEquals(0xff, bus.read(0x0011, true));
        cpu.step();
        assertEquals(0xff, bus.read(0x0011, true));
    }

    public void test_JMP_Indirect_Absolute_X () throws Exception {
        makeCmosCpu();
        bus.write(0x304,00);
        bus.write(0x0305,04);
        bus.loadProgram(0x7c, 0x00, 0x03);
        cpu.setXRegister(0x04);
        cpu.step();
        assertEquals(0x0400,cpu.getProgramCounter());
    }

    public void test_JMP_Indirect_Absolute_XRequiresCmosCpu () throws Exception {
        makeNmosCpu();
        bus.write(0x304,00);
        bus.write(0x0305,04);
        bus.loadProgram(0x7c, 0x00, 0x03);
        cpu.setXRegister(0x04);
        cpu.step();
        assertEquals(0x0203,cpu.getProgramCounter());
    }
}
