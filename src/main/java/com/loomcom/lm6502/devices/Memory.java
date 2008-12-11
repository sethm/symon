package com.loomcom.lm6502.devices;

import com.loomcom.lm6502.*;

public class Memory extends Device {

	private boolean readOnly;

	public Memory(int address, int size, Cpu cpu, boolean readOnly)
		    throws MemoryRangeException {
		super(address, size, "RW Memory", cpu);
		this.readOnly = readOnly;
	}

	public Memory(int address, int size, Cpu cpu)
		    throws MemoryRangeException {
		this(address, size, cpu, false);
	}

	public void write(int address, int data) {
	}

	public int read(int address) {
		return 0;
	}

}