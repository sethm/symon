 package com.loomcom.lm6502;

/**
 * A memory-mapped IO Device.
 */

public abstract class Device {

	/** The memory range for this device. */
	private MemoryRange memoryRange;

	/** Reference to the CPU, for interrupts. */
	private Cpu cpu;

	public Device(MemoryRange range, Cpu cpu) {
		this.memoryRange = range;
		this.cpu = cpu;
	}

	public MemoryRange getMemoryRange() {
		return memoryRange;
	}

	public int getEndAddress() {
		return memoryRange.getEndAddress();
	}

	public int getStartAddress() {
		return memoryRange.getStartAddress();
	}

	public void generateInterrupt() {
		cpu.interrupt();
	}

	public void generateNonMaskableInterrupt() {
		cpu.nmiInterrupt();
	}

}