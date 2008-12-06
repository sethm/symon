package com.loomcom.j6502;

/**
 * A memory-mapped IO Device.
 */

public abstract class Device {

    /** The memory range for this device. */
    private MemoryRange m_memoryRange;

    /** Referece to the CPU, for interrupts. */
    private Cpu m_cpu;
    
    public Device(MemoryRange range, Cpu cpu) {
	m_memoryRange = range;
	m_cpu = cpu;
    }

    public void generateInterrupt() {
	m_cpu.interrupt();
    }

    public void generateNonMaskableInterrupt() {
	m_cpu.nmiInterrupt();
    }

}