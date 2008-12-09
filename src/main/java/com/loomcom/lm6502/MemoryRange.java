package com.loomcom.lm6502;

public class MemoryRange implements Comparable<MemoryRange> {
	public int startAddress;
	public int endAddress;

	public MemoryRange(int startAddress, int endAddress)
		    throws MemoryRangeException {
		if (startAddress < 0 || endAddress < 0) {
			throw new MemoryRangeException("Addresses cannot be less than 0.");
		}
		if (startAddress >= endAddress) {
			throw new MemoryRangeException("End address must be greater than start address.");
		}
		this.startAddress = startAddress;
		this.endAddress = endAddress;
	}

	public int getStartAddress() {
		return startAddress;
	}

	public int getEndAddress() {
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
	public boolean overlapsWith(MemoryRange other) {
		return ((this.getEndAddress() >= other.getStartAddress() &&
						 this.getEndAddress() <= other.getEndAddress()) ||
						(other.getEndAddress() >= this.getStartAddress() &&
						 other.getEndAddress() <= this.getEndAddress()) ||
						(this.getStartAddress() <= other.getStartAddress() &&
						 this.getEndAddress() >= other.getEndAddress()) ||
						(other.getStartAddress() <= this.getStartAddress() &&
						 other.getEndAddress() >= this.getEndAddress()));
	}

	// Implementation of Comparable interface
	public int compareTo(MemoryRange other) {
		if (other == null) {
			throw new NullPointerException("Cannot compare to null.");
		}
		if (this == other) {
			return 0;
		}
		Integer thisStartAddr = new Integer(this.getStartAddress());
		Integer thatStartAddr = new Integer(other.getStartAddress());
		return thisStartAddr.compareTo(thatStartAddr);
	}
}
