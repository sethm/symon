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
public class Cpu65C02ImmediateModeTest extends TestCase {
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

    public void test_BIT_Immediate() throws Exception {
        makeCmosCpu();
        bus.loadProgram(0x89, 0xF1); // 65C02 BIT #$01
        cpu.setAccumulator(0x02);

        cpu.step();
        assertTrue(cpu.getZeroFlag());              // #$02 & #$F1 = 0
        assertEquals(0x02,cpu.getAccumulator());    // Accumulator should not be modified
        assertFalse(cpu.getNegativeFlag());         // BIT #Immediate should not set N or V Flags
        assertFalse(cpu.getOverflowFlag());

        cpu.reset();
        cpu.setAccumulator(0x01);
        cpu.step();
        assertFalse(cpu.getZeroFlag());             // #$F1 & #$01 = 1
        assertEquals(0x01,cpu.getAccumulator());

    }

    public void test_BIT_ImmediateRequiresCmosCpu() throws Exception {
        makeNmosCpu();
        bus.loadProgram(0x89, 0xF1); // 65C02 BIT #$01

        cpu.step();
        cpu.setAccumulator(0x01);
        assertTrue(cpu.getZeroFlag());
        assertEquals(0x01,cpu.getAccumulator());

    }

}
