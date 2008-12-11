package com.loomcom.lm6502.devices;

import com.loomcom.lm6502.*;

/**
 * A memory-mapped IO Device.
 */

public abstract class Device {

	/** The memory range for this device. */
	private MemoryRange memoryRange;

	private String name;

	/** Reference to the CPU, for interrupts. */
	private Cpu cpu;

	public Device(int address, int size, String name, Cpu cpu)
		    throws MemoryRangeException {
		this.memoryRange = new MemoryRange(address, address + size - 1);
		this.name = name;
		this.cpu = cpu;
	}

	public abstract void write(int address, int data);

	public abstract int read(int address);

	public MemoryRange getMemoryRange() {
		return memoryRange;
	}

	public int getEndAddress() {
		return memoryRange.getEndAddress();
	}

	public int getStartAddress() {
		return memoryRange.getStartAddress();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
