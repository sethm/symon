package com.loomcom.lm6502.devices;

import java.util.*;

import com.loomcom.lm6502.*;
import com.loomcom.lm6502.exceptions.*;

public class Memory extends Device {

	private boolean readOnly;

	private int[] mem;

	public Memory(int address, int size, Cpu cpu, boolean readOnly)
		    throws MemoryRangeException {
		super(address, size, "RW Memory", cpu);

		this.readOnly = readOnly;
		this.mem = new int[size];

		// Init the mem to all 0xff
		Arrays.fill(this.mem, 0xff);
	}

	public Memory(int address, int size, Cpu cpu)
		    throws MemoryRangeException {
		this(address, size, cpu, false);
	}

	public void write(int address, int data) {
		System.out.println(String.format("[write] Before write: $%04x=$%04x", address, this.mem[address]));
		this.mem[address] = data;
		System.out.println(String.format("[write] After write:  $%04x=$%04x", address, this.mem[address]));
	}

	public int read(int address) {
		return this.mem[address];
	}

	public String toString() {
		return "Memory: " + getMemoryRange().toString();
	}

}