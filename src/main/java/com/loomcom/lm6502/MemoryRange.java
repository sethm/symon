package com.loomcom.lm6502;

import com.loomcom.lm6502.exceptions.*;

/**
 * MemoryRange is a simple container class representing a literal
 * range of memory, with a staraddress, and an end address.  It has
 * guards against creating impossible memory ranges, and implements
 * some useful methods for checking address inclusion and range
 * overlaps.
 */
public class MemoryRange implements Comparable<MemoryRange> {

	/** The starting address of the memory range. */
	public int startAddress;
	/** The ending address of the memory range. */
	public int endAddress;

	public MemoryRange(int startAddress, int endAddress)
		    throws MemoryRangeException {
		if (startAddress < 0 || endAddress < 0) {
			throw new MemoryRangeException("Addresses cannot be less than 0.");
		}
		if (startAddress >= endAddress) {
			throw new MemoryRangeException("End address must be greater " +
																		 "than start address.");
		}
		this.startAddress = startAddress;
		this.endAddress = endAddress;
	}

	/**
	 * @returns the starting address.
	 */
	public int startAddress() {
		return startAddress;
	}

	/**
	 * @returns the ending address.
	 */
	public int endAddress() {
		return endAddress;
	}

	/**
	 * Checks for address inclusion in the range.
	 *
	 * @returns true if the address is included within this range,
	 *					false otherwise.
	 */
	public boolean includes(int address) {
		return (address <= endAddress &&
		        address >= startAddress);
	}

	/**
	 * Checks for overlapping memory ranges.
	 *
	 * @returns true if this range overlaps in any way with the other.
	 */
	public boolean overlaps(MemoryRange other) {
		return (this.includes(other.startAddress()) ||
						other.includes(this.startAddress()));
	}

	// Implementation of Comparable interface
	public int compareTo(MemoryRange other) {
		if (other == null) {
			throw new NullPointerException("Cannot compare to null.");
		}
		if (this == other) {
			return 0;
		}
		Integer thisStartAddr = new Integer(this.startAddress());
		Integer thatStartAddr = new Integer(other.startAddress());
		return thisStartAddr.compareTo(thatStartAddr);
	}

	public String toString() {
		StringBuffer desc = new StringBuffer("@");
		desc.append(String.format("0x%04x", startAddress));
		desc.append("-");
		desc.append(String.format("0x%04x", endAddress));
		return desc.toString();
	}
}
