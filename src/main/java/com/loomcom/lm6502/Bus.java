package com.loomcom.lm6502;

import java.util.*;
import com.loomcom.lm6502.devices.*;

public class Bus {

	private int bottom = 0x0000;
	private int top    = 0xffff;

	/**
	 * Ordered list of IO devices.
	 */
	private List<Device> devices;

	public Bus(int size) {
		this(0, size - 1);
	}

	public Bus(int bottom, int top) {
		this.devices = new ArrayList(8);
		this.bottom = bottom;
		this.top = top;
	}

	public int bottom() {
		return bottom;
	}

	public int top() {
		return top;
	}

	public void addDevice(Device device)
		throws MemoryRangeException {
		// Make sure there's no memory overlap.
		MemoryRange memRange = device.getMemoryRange();
		for (Device d : devices) {
			if (d.getMemoryRange().overlaps(memRange)) {
				throw new MemoryRangeException("The device being added overlaps " +
																			 "with an existing device.");
			}
		}

		// Add the device
		devices.add(device);
	}

	/**
	 * Returns true if the memory map is full, i.e., there are no
	 * gaps between any IO devices.	 All memory locations map to some
	 * device.
	 */
	public boolean isComplete() {
		// Emtpy maps cannot be complete.
		if (devices.isEmpty()) { return false; }

		// Loop over devices and ensure they are contiguous.
		MemoryRange prev = null;
		int i = 0;
		int length = devices.size();
		for (Device d : devices) {
			MemoryRange cur = d.getMemoryRange();
			if (i == 0) {
				// If the first entry doesn't start at 'bottom', return false.
				if (cur.getStartAddress() != bottom) { return false; }
			}

			if (prev != null && i < length - 1) {
				// Otherwise, compare previous map's end against this map's
				// top.  They must be adjacent!
				if (cur.getStartAddress() - 1 != prev.getEndAddress()) {
					return false;
				}
			}

			if (i == length - 1) {
				// If the last entry doesn't end at top, return false;
				if (cur.getEndAddress() != top) { return false; }
			}

			i++;
			prev = cur;
		}

		// Must be complete.
		return true;
	}

	public List getDevices() {
		// Expose a copy of the device list, not the original
		return new ArrayList(devices);
	}
}
