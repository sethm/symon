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
public class Cpu65C02AbsoluteModeTest extends TestCase {
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
        bus.write(0x0010,0xff);

        bus.loadProgram(0x9c, 0x10, 0x00);  // STZ Absolute

        // Test STZ Absolute ($0010)
        assertEquals(0xff, bus.read(0x0010, true));
        cpu.step();
        assertEquals(0x00, bus.read(0x0010, true));

    }

    public void test_STZRequiresCmosCpu() throws Exception {
        makeNmosCpu();
        bus.write(0x0010,0xff);

        bus.loadProgram(0x9c, 0x10, 0x00);   // STZ Absolute

        // Test STZ Absolute ($0010)
        assertEquals(0xff, bus.read(0x0010, true));
        cpu.step();
        assertEquals(0xff, bus.read(0x0010, true));

    }

    public void test_TSB() throws Exception {
        makeCmosCpu();
        bus.loadProgram(0x0c, 0x10, 0x00);   // 65C02 TSB Absolute $0010

        bus.write(0x10, 0x01);
        cpu.setAccumulator(0x01);
        cpu.step();
        assertEquals(0x01,bus.read(0x10,true)); // 0x01 & 0x01 = 0x01
        assertFalse(cpu.getZeroFlag());

        cpu.reset();
        cpu.setAccumulator(0x02);
        cpu.step();
        assertEquals(0x03,bus.read(0x0010,true));
        assertTrue(cpu.getZeroFlag());

    }

    public void test_TSBRequiresCmosCpu() throws Exception {
        makeNmosCpu();
        bus.loadProgram(0x0c, 0x10, 0x00);   // 65C02 TSB Absolute $0010

        bus.write(0x10, 0x00);
        cpu.setAccumulator(0x01);
        cpu.step();
        assertEquals(0x00,bus.read(0x0010,true));

    }

    public void test_TRB() throws Exception {
        makeCmosCpu();
        bus.loadProgram(0x1c, 0x00, 0x01);   // 65C02 TRB Absolute $0010

        bus.write(0x0100, 0x03);
        cpu.setAccumulator(0x01);
        cpu.step();
        assertEquals(0x02,bus.read(0x0100,true));     // $03 &= ~($01) = $02
        assertFalse(cpu.getZeroFlag());             // Z = !(A & M)

        cpu.reset();
        cpu.setAccumulator(0x01);
        cpu.step();
        assertEquals(0x02,bus.read(0x0100,true));    // $02 &= ~($01) = $02
        assertTrue(cpu.getZeroFlag());               // Z = !(A & M)

    }

    public void test_TRBRequiresCmosCpu() throws Exception {
        makeNmosCpu();
        bus.loadProgram(0x1c, 0x00, 0x01);   // 65C02 TRB Absolute $0010

        bus.write(0x0100, 0xff);
        cpu.setAccumulator(0x01);
        cpu.step();
        assertEquals(0xff,bus.read(0x0100,true));

    }
}
