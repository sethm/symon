package com.loomcom.lm6502;

import java.util.Map;

public class AddressDecoder {
	/**
	 * Map of memory ranges to IO devices.
	 */
	private Map<MemoryRange, Device> m_ioMap;

	public AddressDecoder() {}

	public void addDevice(Device d)
		throws MemoryConflictException {
		// Make sure there's no memory overlap.
		// Add the device to the map.
		m_ioMap.put(d.getMemoryRange(), d);
	}

	/**
	 * Returns true if the memory map is full, i.e., there are no
	 * gaps between any IO devices.	 All memory locations map to some
	 * device.
	 */
	public boolean isComplete() {
		return true;
	}

	/**
	 * Returns true if the memory map is 'sparse', i.e., there
	 * are gaps between IO devices.
	 */
	public boolean isSparse() {
		return !isComplete();
	}
}

class MemoryRange {
	public int m_startAddress;
	public int m_endAddress;

	/**
	 * @returns true if the address is included within this range,
	 *					false otherwise.
	 */
	public boolean includes(int address) {
		return (address <= m_endAddress &&
						address >= m_startAddress);
	}

	public void setStartAddress(int startAddress) {
		m_startAddress = startAddress;
	}

	public void setEndAddress(int endAddress) {
		m_endAddress = endAddress;
	}

	public int getStartAddress() {
		return m_startAddress;
	}

	public int getEndAddress() {
		return m_endAddress;
	}
}

/**
 * Excption that will be thrown if devices conflict in the IO map.
 */
class MemoryConflictException extends Exception {}