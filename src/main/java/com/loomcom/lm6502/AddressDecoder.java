package com.loomcom.lm6502;

import java.util.*;

public class AddressDecoder {

	public static final int MEMORY_BOTTOM = 0x0000;
	public static final int MEMORY_TOP = 0xFFFF;

	/**
	 * Ordered map of memory ranges to IO devices.
	 */
	private SortedMap<MemoryRange, Device> ioMap;

	public AddressDecoder() {
		ioMap = new TreeMap();
	}

	public void addDevice(Device d)
		throws MemoryRangeException {
		// Make sure there's no memory overlap.
		for (MemoryRange memRange : ioMap.keySet()) {
			if (d.getMemoryRange().overlapsWith(memRange)) {
				throw new MemoryRangeException("The device being added overlaps with an existing device.");
			}
		}

		// Add the device to the map.
		ioMap.put(d.getMemoryRange(), d);
	}

	/**
	 * Returns true if the memory map is full, i.e., there are no
	 * gaps between any IO devices.	 All memory locations map to some
	 * device.
	 */
	public boolean isComplete() {
		// Emtpy maps cannot be complete.
		if (ioMap.isEmpty()) { return false; }

		// Loop over devices and ensure they are contiguous.
		MemoryRange prev = null;
		int i = 0;
		int size = ioMap.size();
		for (Map.Entry e : ioMap.entrySet()) {
			MemoryRange cur = (MemoryRange)e.getKey();
			if (i == 0) {
				// If the first entry doesn't start at MEMORY_BOTTOM, return false.
				if (cur.getStartAddress() != MEMORY_BOTTOM) { return false; }
			} else if (i < size - 1) {
				// Otherwise, compare previous map's end against this map's
				// top.  They must be adjacent!
				if (cur.getStartAddress() - 1 != prev.getEndAddress()) {
					return false;
				}
			} else {
				// If the last entry doesn't end at MEMORY_TOP, return false;
				if (cur.getEndAddress() != MEMORY_TOP) { return false; }
			}
			i++;
			prev = cur;
		}

		// Must be complete.
		return true;
	}

	/**
	 * Returns true if there are any gap in the memory map.
	 */
	public boolean hasGaps() {
		return !isComplete();
	}
}