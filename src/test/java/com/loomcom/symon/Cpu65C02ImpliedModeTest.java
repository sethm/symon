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
public class Cpu65C02ImpliedModeTest extends TestCase {
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

    public void test_PHX() throws Exception {
        makeCmosCpu();
        cpu.stackPush(0x00);
        cpu.setXRegister(0xff);
        bus.loadProgram(0xda);

        assertEquals(cpu.stackPeek(), 0x00);
        cpu.step();
        assertEquals(cpu.stackPeek(), 0xff);

    }

    public void test_PHXRequiresCmosCpu() throws Exception {
        makeNmosCpu();
        cpu.stackPush(0x00);
        cpu.setXRegister(0xff);
        bus.loadProgram(0xda);

        assertEquals(cpu.stackPeek(), 0x00);
        cpu.step();
        assertEquals(cpu.stackPeek(), 0x00);

    }

    public void test_PLX() throws Exception {
        makeCmosCpu();
        cpu.stackPush(0xff);
        cpu.setXRegister(0x00);
        bus.loadProgram(0xfa);

        assertEquals(0x00, cpu.getXRegister());
        cpu.step();
        assertEquals(0xff, cpu.getXRegister());

    }

    public void test_PLXRequiresCmosCpu() throws Exception {
        makeNmosCpu();
        cpu.stackPush(0xff);
        cpu.setXRegister(0x00);
        bus.loadProgram(0xfa);

        assertEquals(0x00, cpu.getXRegister());
        cpu.step();
        assertEquals(0x00, cpu.getXRegister());

    }

    public void test_PHY() throws Exception {
        makeCmosCpu();
        cpu.stackPush(0x00);
        cpu.setYRegister(0xff);
        bus.loadProgram(0x5a);

        assertEquals(0x00, cpu.stackPeek());
        cpu.step();
        assertEquals(0xff, cpu.stackPeek());

    }

    public void test_PHYRequiresCmosCpu() throws Exception {
        makeNmosCpu();
        cpu.stackPush(0x00);
        cpu.setYRegister(0xff);
        bus.loadProgram(0x5a);

        assertEquals(0x00, cpu.stackPeek());
        cpu.step();
        assertEquals(0x00, cpu.stackPeek());

    }

    public void test_PLY() throws Exception {
        makeCmosCpu();
        cpu.stackPush(0xff);
        cpu.setYRegister(0x00);
        bus.loadProgram(0x7a);

        assertEquals(0x00, cpu.getYRegister());
        cpu.step();
        assertEquals(0xff, cpu.getYRegister());

    }

    public void test_PLYRequiresCmosCpu() throws Exception {
        makeNmosCpu();
        cpu.stackPush(0xff);
        cpu.setYRegister(0x00);
        bus.loadProgram(0x7a);

        assertEquals(0x00, cpu.getYRegister());
        cpu.step();
        assertEquals(0x00, cpu.getYRegister());

    }

    public void test_INC_A() throws Exception {
        makeCmosCpu();
        cpu.setAccumulator(0x10);
        bus.loadProgram(0x1a);

        cpu.step();
        assertEquals(0x11, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        // Incrementing to 0 should set Zero Flag
        cpu.reset();
        cpu.setAccumulator(0xff);
        cpu.step();
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        // Should set Negative Flag
        cpu.reset();
        cpu.setAccumulator(0x7F);
        cpu.step();
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getZeroFlag());

    }

    public void test_INC_ARequiresCmosCpu() throws Exception {
        makeNmosCpu();
        cpu.setAccumulator(0x10);
        bus.loadProgram(0x1a);

        cpu.step();
        assertEquals(0x10, cpu.getAccumulator());

    }

    public void test_DEC_A() throws Exception {
        makeCmosCpu();
        cpu.setAccumulator(0x10);
        bus.loadProgram(0x3a);

        cpu.step();
        assertEquals(0x0F, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        // Decrementing to 0 should set Zero Flag
        cpu.reset();
        cpu.setAccumulator(0x01);
        cpu.step();
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        // Should set Negative Flag
        cpu.reset();
        cpu.setAccumulator(0x00);
        cpu.step();
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getZeroFlag());

    }

    public void test_DEC_ARequiresCmosCpu() throws Exception {
        makeNmosCpu();
        cpu.setAccumulator(0x10);
        bus.loadProgram(0x3a);

        cpu.step();
        assertEquals(0x10, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

    }

    public void test_BRK_clearsDecimalModeFlag() throws Exception {
        makeCmosCpu();
        cpu.setDecimalModeFlag();
        assertEquals(0x00, cpu.stackPeek());
        assertFalse(cpu.getBreakFlag());
        assertTrue(cpu.getDecimalModeFlag());
        assertEquals(0x0200, cpu.getProgramCounter());
        assertEquals(0xff, cpu.getStackPointer());

        // Set the IRQ vector
        bus.write(0xffff, 0x12);
        bus.write(0xfffe, 0x34);

        bus.loadProgram(0xea,  // NOP
                        0xea,  // NOP
                        0xea,  // NOP
                        0x00,  // BRK
                        0xea,  // NOP
                        0xea); // NOP

        cpu.step(3); // Three NOP instructions

        assertEquals(0x203, cpu.getProgramCounter());
        assertTrue(cpu.getDecimalModeFlag());
        cpu.step(); // Triggers the BRK

        // Was at PC = 0x204.  PC+1 should now be on the stack
        assertEquals(0x02, bus.read(0x1ff, true)); // PC high byte
        assertEquals(0x05, bus.read(0x1fe, true)); // PC low byte


        // Interrupt vector held 0x1234, so we should be there.
        assertEquals(0x1234, cpu.getProgramCounter());
        assertEquals(0xfc, cpu.getStackPointer());

        // B and I flags should have been set on P
        assertTrue(cpu.getBreakFlag());
        assertFalse(cpu.getDecimalModeFlag());
    }

}
