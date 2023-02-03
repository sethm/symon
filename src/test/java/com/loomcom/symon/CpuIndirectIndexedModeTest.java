package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;

import org.junit.*;

import static org.junit.Assert.*;

public class CpuIndirectIndexedModeTest {

    protected Cpu    cpu;
    protected Bus    bus;
    protected Memory mem;

    @Before
    public void runBeforeEveryTest() throws Exception {
        this.cpu = new Cpu();
        this.bus = new Bus(0x0000, 0xffff);
        this.mem = new Memory(0x0000, 0xffff);
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
        assertEquals(0x24, cpu.getProcessorStatus());
    }

    @Test
    public void test_LDA() throws Exception {
        assertEquals(cpu.toString(), 0x00, cpu.getAccumulator());
        bus.write(0x0014, 0x00);
        bus.write(0x0015, 0xd8);
        bus.write(0xd828, 0x03);

        cpu.setYRegister(0x28);

        bus.loadProgram(0xb1, 0x14); // LDA ($14),Y
        cpu.step(1);

        assertEquals(0x03, cpu.getAccumulator());
    }

    @Test
    public void test_ORA() throws Exception {
        bus.write(0x0014, 0x00);
        bus.write(0x0015, 0xd8);
        bus.write(0xd828, 0xe3);

        cpu.setYRegister(0x28);
        cpu.setAccumulator(0x32);

        bus.loadProgram(0x11, 0x14); // ORA ($14),Y
        cpu.step(1);

        assertEquals(0xf3, cpu.getAccumulator());
        assertEquals(0xe3, bus.read(0xd828, true));
    }

    @Test
    public void test_AND() throws Exception {
        bus.write(0x0014, 0x00);
        bus.write(0x0015, 0xd8);
        bus.write(0xd828, 0xe3);

        cpu.setYRegister(0x28);
        cpu.setAccumulator(0x32);

        bus.loadProgram(0x31, 0x14); // AND ($14),Y
        cpu.step(1);

        assertEquals(0x22, cpu.getAccumulator());
        assertEquals(0xe3, bus.read(0xd828, true));
    }

}
