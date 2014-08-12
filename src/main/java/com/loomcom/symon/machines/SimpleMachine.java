package com.loomcom.symon.machines;

import com.loomcom.symon.Bus;
import com.loomcom.symon.Cpu;
import com.loomcom.symon.devices.Acia;
import com.loomcom.symon.devices.Crtc;
import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.devices.Pia;
import com.loomcom.symon.exceptions.MemoryRangeException;

/**
 * A SimpleMachine is the simplest 6502 implementation possible - it
 * consists solely of RAM and a CPU. This machine is primarily useful
 * for running 6502 functional tests or debugging by hand.
 */
public class SimpleMachine implements Machine {

    private static final int BUS_BOTTOM = 0x0000;
    private static final int BUS_TOP    = 0xffff;

    private final Bus bus;
    private final Memory ram;
    private final Cpu cpu;

    public SimpleMachine() throws MemoryRangeException {
        this.bus = new Bus(BUS_BOTTOM, BUS_TOP);
        this.ram = new Memory(BUS_BOTTOM, BUS_TOP, false);
        this.cpu = new Cpu();

        bus.addCpu(cpu);
        bus.addDevice(ram);
    }

    @Override
    public Bus getBus() {
        return bus;
    }

    @Override
    public Cpu getCpu() {
        return cpu;
    }

    @Override
    public Memory getRam() {
        return ram;
    }

    @Override
    public Acia getAcia() {
        return null;
    }

    @Override
    public Pia getPia() {
        return null;
    }

    @Override
    public Crtc getCrtc() {
        return null;
    }

    @Override
    public Memory getRom() {
        return null;
    }

    @Override
    public void setRom(Memory rom) throws MemoryRangeException {
        // No-op
    }

    @Override
    public int getRomBase() {
        return 0;
    }

    @Override
    public int getRomSize() {
        return 0;
    }

    @Override
    public int getMemorySize() {
        return BUS_TOP + 1;
    }

    @Override
    public String getName() {
        return "Simple";
    }
}
