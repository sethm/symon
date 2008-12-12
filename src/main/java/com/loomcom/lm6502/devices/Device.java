package com.loomcom.lm6502.devices;

import com.loomcom.lm6502.*;
import com.loomcom.lm6502.exceptions.*;

/**
 * A memory-mapped IO Device.
 */

public abstract class Device implements Comparable<Device> {

	/** The memory range for this device. */
	private MemoryRange memoryRange;

	/** The name of the device. */
	private String name;

	/** Reference to the bus where this Device is attached. */
	private Bus bus;

	public Device(int address, int size, String name)
		    throws MemoryRangeException {
		this.memoryRange = new MemoryRange(address, address + size - 1);
		this.name = name;
	}

	public Device(int address, int size) throws MemoryRangeException {
		this(address, size, null);
	}

	/* Methods required to be implemented by inheriting classes. */
	public abstract void write(int address, int data);
	public abstract int read(int address);
	public abstract String toString();

	public void setBus(Bus bus) {
		this.bus = bus;
	}

	public MemoryRange getMemoryRange() {
		return memoryRange;
	}

	public int endAddress() {
		return memoryRange.endAddress();
	}

	public int startAddress() {
		return memoryRange.startAddress();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Compares two devices.  The sort order is defined by the sort
	 * order of the device's memory ranges.
	 */
	public int compareTo(Device other) {
		if (other == null) {
			throw new NullPointerException("Cannot compare to null.");
		}
		if (this == other) {
			return 0;
		}
		return getMemoryRange().compareTo(other.getMemoryRange());
	}
}
