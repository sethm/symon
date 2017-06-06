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
public class Cpu65C02ZeroPageModeTest extends TestCase {
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
        bus.write(0x0000,0xff);

        bus.loadProgram(0x64,0x00);          // STZ Zero Page $00

        // Test STZ Zero Page
        assertEquals(0xff,bus.read(0x00, true));
        cpu.step();
        assertEquals(0x00,bus.read(0x00, true));
    }

    public void test_STZRequiresCmosCpu() throws Exception {
        makeNmosCpu();
        bus.write(0x0000,0xff);

        bus.loadProgram(0x64,0x00);          // STZ Zero Page $00

        // Test STZ Zero Page
        assertEquals(0xff,bus.read(0x00, true));
        cpu.step();
        assertEquals(0xff,bus.read(0x00, true));

    }

    public void test_SMB() throws Exception {
        makeCmosCpu();
        bus.loadProgram(0x87,0x01,  // SMB0 $01
                        0x97,0x01,  // SMB1 $01
                        0xa7,0x01,  // SMB2 $01
                        0xb7,0x01,  // SMB3 $01
                        0xc7,0x01,  // SMB4 $01
                        0xd7,0x01,  // SMB5 $01
                        0xe7,0x01,  // SMB6 $01
                        0xf7,0x01); // SMB7 $01

        // SMB0
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(1 << 0,bus.read(0x0001, true));

        // SMB1
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(1 << 1,bus.read(0x0001, true));

        // SMB2
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(1 << 2,bus.read(0x0001, true));

        // SMB3
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(1 << 3,bus.read(0x0001, true));

        // SMB4
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(1 << 4,bus.read(0x0001, true));

        // SMB5
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(1 << 5,bus.read(0x0001, true));

        // SMB6
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(1 << 6,bus.read(0x0001, true));

        // SMB7
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(1 << 7,bus.read(0x0001, true));

    }

    public void test_SMBRequiresCmosCpu() throws Exception {
        makeNmosCpu();
        bus.loadProgram(0x87,0x01,  // SMB0 $01
                        0x97,0x01,  // SMB1 $01
                        0xa7,0x01,  // SMB2 $01
                        0xb7,0x01,  // SMB3 $01
                        0xc7,0x01,  // SMB4 $01
                        0xd7,0x01,  // SMB5 $01
                        0xe7,0x01,  // SMB6 $01
                        0xf7,0x01); // SMB7 $01

        // SMB0
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0x00,bus.read(0x0001, true));

        // SMB1
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0x00,bus.read(0x0001, true));

        // SMB2
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0x00,bus.read(0x0001, true));

        // SMB3
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0x00,bus.read(0x0001, true));

        // SMB4
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0x00,bus.read(0x0001, true));

        // SMB5
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0x00,bus.read(0x0001, true));

        // SMB6
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0x00,bus.read(0x0001, true));

        // SMB7
        bus.write(0x01,0x00);
        assertEquals(0x00,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0x00,bus.read(0x0001, true));

    }

    public void test_RMB() throws Exception {
        makeCmosCpu();
        bus.loadProgram(0x07,0x01,  // SMB0 $01
                        0x17,0x01,  // SMB1 $01
                        0x27,0x01,  // SMB2 $01
                        0x37,0x01,  // SMB3 $01
                        0x47,0x01,  // SMB4 $01
                        0x57,0x01,  // SMB5 $01
                        0x67,0x01,  // SMB6 $01
                        0x77,0x01); // SMB7 $01

        // RMB0
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0xfe,bus.read(0x0001, true));

        // RMB1
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0xfd,bus.read(0x0001, true));

        // RMB2
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0xfb,bus.read(0x0001, true));

        // RMB3
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0xf7,bus.read(0x0001, true));

        // RMB4
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0xef,bus.read(0x0001, true));

        // RMB5
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0xdf,bus.read(0x0001, true));

        // RMB6
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0xbf,bus.read(0x0001, true));

        // RMB7
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0x7f,bus.read(0x0001, true));

    }

    public void test_RMBRequiresCmosCpu() throws Exception {
        makeNmosCpu();
        bus.loadProgram(0x07,0x01,  // SMB0 $01
                        0x17,0x01,  // SMB1 $01
                        0x27,0x01,  // SMB2 $01
                        0x37,0x01,  // SMB3 $01
                        0x47,0x01,  // SMB4 $01
                        0x57,0x01,  // SMB5 $01
                        0x67,0x01,  // SMB6 $01
                        0x77,0x01); // SMB7 $01

        // RMB0
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0xff,bus.read(0x0001, true));

        // RMB1
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0xff,bus.read(0x0001, true));

        // RMB2
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0xff,bus.read(0x0001, true));

        // RMB3
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0xff,bus.read(0x0001, true));

        // RMB4
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0xff,bus.read(0x0001, true));

        // RMB5
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0xff,bus.read(0x0001, true));

        // RMB6
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0xff,bus.read(0x0001, true));

        // RMB7
        bus.write(0x01,0xff);
        assertEquals(0xff,bus.read(0x0001, true));
        cpu.step();
        assertEquals(0xff,bus.read(0x0001, true));

    }

    public void test_TSB() throws Exception {
        makeCmosCpu();
        bus.loadProgram(0x04, 0x10);        // 65C02 TSB Zero Page $10

        bus.write(0x10, 0x01);
        cpu.setAccumulator(0x03);
        cpu.step();
        assertEquals(0x03,bus.read(0x10,true));
        assertFalse(cpu.getZeroFlag());

        cpu.reset();
        bus.write(0x10, 0x01);
        cpu.setAccumulator(0x02);
        cpu.step();
        assertEquals(0x03,bus.read(0x10,true));
        assertTrue(cpu.getZeroFlag());

    }

    public void test_TSBRequiresCmosCpu() throws Exception {
        makeNmosCpu();
        bus.loadProgram(0x04, 0x10);        // 65C02 TSB Zero Page $10

        bus.write(0x10, 0x01);
        cpu.setAccumulator(0x03);
        cpu.step();
        assertEquals(0x01,bus.read(0x10,true));

    }

    public void test_TRB() throws Exception {
        makeCmosCpu();
        cpu.reset();
        bus.loadProgram(0x14, 0x40);        // 65C02 TRB Zero Page $40

        bus.write(0x0040, 0xff);
        cpu.setAccumulator(0x01);
        cpu.step();
        assertEquals(0xfe,bus.read(0x0040,true));     // $03 &= ~($01) = $02
        assertFalse(cpu.getZeroFlag());             // Z = !(A & M)

        cpu.reset();
        cpu.setAccumulator(0x01);
        cpu.step();
        assertEquals(0xfe,bus.read(0x0040,true));    // $02 &= ~($01) = $02
        assertTrue(cpu.getZeroFlag());               // Z = !(A & M)

    }

    public void test_TRBRequiresCmosCpu() throws Exception {
        makeNmosCpu();
        cpu.reset();
        bus.loadProgram(0x14, 0x40);        // 65C02 TRB Zero Page $40

        bus.write(0x0040, 0xff);
        cpu.setAccumulator(0x01);
        cpu.step();
        assertEquals(0xff,bus.read(0x0040,true));     // $03 &= ~($01) = $02

    }

}
