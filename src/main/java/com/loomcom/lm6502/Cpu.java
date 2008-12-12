package com.loomcom.lm6502;

/**
 * Main 6502 CPU Simulation.
 */
public class Cpu implements InstructionTable {

	/* The Bus */
	private Bus bus;

	/* User Registers */
	private int a;  // Accumulator
	private int x;  // X index register
	private int y;  // Y index register

	/* Internal Registers */
	private int pc;  // Program Counter register
	private int sp;  // Stack Pointer register
	private int ir;  // Instruction register

	/* Operands for the current instruction */
	private int[] operands = new int[2];
	private int addr; // The address the most recent instruction 
	                  // was fetched from

	/**
	 * Construct a new CPU.
	 */
	public Cpu() {
	}

	/**
	 * Set the bus reference for this CPU.
	 */
	public void setBus(Bus bus) {
		this.bus = bus;
	}

	/**
	 * Return the Bus that this CPU is associated with.
	 */
	public Bus getBus() {
		return bus;
	}

	/**
	 * Reset the CPU to known initial values.
	 */
	public void reset() {
		// Registers
		sp = 0x01ff;
		
		// Set the PC to the address stored in 0xfffc
		pc = CpuUtils.address(bus.read(0xfffc), bus.read(0xfffd));
		
		// Clear instruction register.
		ir = 0;
	}

	/**
	 * Performs an individual machine cycle.
	 */
	public void step() {
		// Store the address from which the IR was read, for debugging
		addr = pc;

		// Fetch memory location for this instruction.
		ir = bus.read(pc);

		// TODO: The way we increment the PC may need
		// to change when interrupts are implemented
		
		// Increment PC
		incProgramCounter();

		// Decode the instruction and operands
		int size = Cpu.instructionSizes[ir];
		for (int i = 0; i < size-1; i++) {
			operands[i] = bus.read(pc);
			// Increment PC after reading
			incProgramCounter();
		}

		// Execute
		switch(ir) {
		
		case 0x00: // HLT
			// TODO: Halt!
			break;
		case 0x01: // n/a
			break;
		case 0x02: // n/a
			break;
		case 0x03: // n/a
			break;
		case 0x04: // n/a
			break;
		case 0x05: // n/a
			break;
		case 0x06: // n/a
			break;
		case 0x07: // n/a
			break;
		case 0x08: // n/a
			break;
		case 0x09: // n/a
			break;
		case 0x0a: // n/a
			break;
		case 0x0b: // n/a
			break;
		case 0x0c: // n/a
			break;
		case 0x0d: // n/a
			break;
		case 0x0e: // n/a
			break;
		case 0x0f: // n/a
			break;

		case 0x10: // n/a
			break;
		case 0x11: // n/a
			break;
		case 0x12: // n/a
			break;
		case 0x13: // n/a
			break;
		case 0x14: // n/a
			break;
		case 0x15: // n/a
			break;
		case 0x16: // n/a
			break;
		case 0x17: // n/a
			break;
		case 0x18: // n/a
			break;
		case 0x19: // n/a
			break;
		case 0x1a: // n/a
			break;
		case 0x1b: // n/a
			break;
		case 0x1c: // n/a
			break;
		case 0x1d: // n/a
			break;
		case 0x1e: // n/a
			break;
		case 0x1f: // n/a
			break;

		case 0x20: // n/a
			break;
		case 0x21: // n/a
			break;
		case 0x22: // n/a
			break;
		case 0x23: // n/a
			break;
		case 0x24: // n/a
			break;
		case 0x25: // n/a
			break;
		case 0x26: // n/a
			break;
		case 0x27: // n/a
			break;
		case 0x28: // n/a
			break;
		case 0x29: // n/a
			break;
		case 0x2a: // n/a
			break;
		case 0x2b: // n/a
			break;
		case 0x2c: // n/a
			break;
		case 0x2d: // n/a
			break;
		case 0x2e: // n/a
			break;
		case 0x2f: // n/a
			break;

		case 0x30: // n/a
			break;
		case 0x31: // n/a
			break;
		case 0x32: // n/a
			break;
		case 0x33: // n/a
			break;
		case 0x34: // n/a
			break;
		case 0x35: // n/a
			break;
		case 0x36: // n/a
			break;
		case 0x37: // n/a
			break;
		case 0x38: // n/a
			break;
		case 0x39: // n/a
			break;
		case 0x3a: // n/a
			break;
		case 0x3b: // n/a
			break;
		case 0x3c: // n/a
			break;
		case 0x3d: // n/a
			break;
		case 0x3e: // n/a
			break;
		case 0x3f: // n/a
			break;

		case 0x40: // n/a
			break;
		case 0x41: // n/a
			break;
		case 0x42: // n/a
			break;
		case 0x43: // n/a
			break;
		case 0x44: // n/a
			break;
		case 0x45: // n/a
			break;
		case 0x46: // n/a
			break;
		case 0x47: // n/a
			break;
		case 0x48: // n/a
			break;
		case 0x49: // n/a
			break;
		case 0x4a: // n/a
			break;
		case 0x4b: // n/a
			break;
		case 0x4c: // JMP - Absolute
			pc = CpuUtils.address(operands[0], operands[1]);
			break;
		case 0x4d: // n/a
			break;
		case 0x4e: // n/a
			break;
		case 0x4f: // n/a
			break;

		case 0x50: // n/a
			break;
		case 0x51: // n/a
			break;
		case 0x52: // n/a
			break;
		case 0x53: // n/a
			break;
		case 0x54: // n/a
			break;
		case 0x55: // n/a
			break;
		case 0x56: // n/a
			break;
		case 0x57: // n/a
			break;
		case 0x58: // n/a
			break;
		case 0x59: // n/a
			break;
		case 0x5a: // n/a
			break;
		case 0x5b: // n/a
			break;
		case 0x5c: // n/a
			break;
		case 0x5d: // n/a
			break;
		case 0x5e: // n/a
			break;
		case 0x5f: // n/a
			break;

		case 0x60: // n/a
			break;
		case 0x61: // n/a
			break;
		case 0x62: // n/a
			break;
		case 0x63: // n/a
			break;
		case 0x64: // n/a
			break;
		case 0x65: // n/a
			break;
		case 0x66: // n/a
			break;
		case 0x67: // n/a
			break;
		case 0x68: // n/a
			break;
		case 0x69: // n/a
			break;
		case 0x6a: // n/a
			break;
		case 0x6b: // n/a
			break;
		case 0x6c: // n/a
			break;
		case 0x6d: // n/a
			break;
		case 0x6e: // n/a
			break;
		case 0x6f: // n/a
			break;

		case 0x70: // n/a
			break;
		case 0x71: // n/a
			break;
		case 0x72: // n/a
			break;
		case 0x73: // n/a
			break;
		case 0x74: // n/a
			break;
		case 0x75: // n/a
			break;
		case 0x76: // n/a
			break;
		case 0x77: // n/a
			break;
		case 0x78: // n/a
			break;
		case 0x79: // n/a
			break;
		case 0x7a: // n/a
			break;
		case 0x7b: // n/a
			break;
		case 0x7c: // n/a
			break;
		case 0x7d: // n/a
			break;
		case 0x7e: // n/a
			break;
		case 0x7f: // n/a
			break;

		case 0x80: // n/a
			break;
		case 0x81: // n/a
			break;
		case 0x82: // n/a
			break;
		case 0x83: // n/a
			break;
		case 0x84: // n/a
			break;
		case 0x85: // n/a
			break;
		case 0x86: // n/a
			break;
		case 0x87: // n/a
			break;
		case 0x88: // n/a
			break;
		case 0x89: // n/a
			break;
		case 0x8a: // n/a
			break;
		case 0x8b: // n/a
			break;
		case 0x8c: // n/a
			break;
		case 0x8d: // n/a
			break;
		case 0x8e: // n/a
			break;
		case 0x8f: // n/a
			break;

		case 0x90: // n/a
			break;
		case 0x91: // n/a
			break;
		case 0x92: // n/a
			break;
		case 0x93: // n/a
			break;
		case 0x94: // n/a
			break;
		case 0x95: // n/a
			break;
		case 0x96: // n/a
			break;
		case 0x97: // n/a
			break;
		case 0x98: // n/a
			break;
		case 0x99: // n/a
			break;
		case 0x9a: // n/a
			break;
		case 0x9b: // n/a
			break;
		case 0x9c: // n/a
			break;
		case 0x9d: // n/a
			break;
		case 0x9e: // n/a
			break;
		case 0x9f: // n/a
			break;

		case 0xa0: // LDY - Immediate
			y = operands[0];
			// TODO: Set Zero Flag, Negative Flag			
			break;
		case 0xa1: // n/a
			break;
		case 0xa2: // LDX - Immediate
			x = operands[0];
			// TODO: Set Zero Flag, Negative Flag
			break;
		case 0xa3: // n/a
			break;
		case 0xa4: // n/a
			break;
		case 0xa5: // n/a
			break;
		case 0xa6: // n/a
			break;
		case 0xa7: // n/a
			break;
		case 0xa8: // n/a
			break;
		case 0xa9: // LDA - Immediate
			a = operands[0];
			// TODO: Set Zero Flag, Negative Flag
			break;
		case 0xaa: // n/a
			break;
		case 0xab: // n/a
			break;
		case 0xac: // n/a
			break;
		case 0xad: // n/a
			break;
		case 0xae: // n/a
			break;
		case 0xaf: // n/a
			break;

		case 0xb0: // n/a
			break;
		case 0xb1: // n/a
			break;
		case 0xb2: // n/a
			break;
		case 0xb3: // n/a
			break;
		case 0xb4: // n/a
			break;
		case 0xb5: // n/a
			break;
		case 0xb6: // n/a
			break;
		case 0xb7: // n/a
			break;
		case 0xb8: // n/a
			break;
		case 0xb9: // n/a
			break;
		case 0xba: // n/a
			break;
		case 0xbb: // n/a
			break;
		case 0xbc: // n/a
			break;
		case 0xbd: // n/a
			break;
		case 0xbe: // n/a
			break;
		case 0xbf: // n/a
			break;

		case 0xc0: // n/a
			break;
		case 0xc1: // n/a
			break;
		case 0xc2: // n/a
			break;
		case 0xc3: // n/a
			break;
		case 0xc4: // n/a
			break;
		case 0xc5: // n/a
			break;
		case 0xc6: // n/a
			break;
		case 0xc7: // n/a
			break;
		case 0xc8: // n/a
			break;
		case 0xc9: // n/a
			break;
		case 0xca: // n/a
			break;
		case 0xcb: // n/a
			break;
		case 0xcc: // n/a
			break;
		case 0xcd: // n/a
			break;
		case 0xce: // n/a
			break;
		case 0xcf: // n/a
			break;

		case 0xd0: // n/a
			break;
		case 0xd1: // n/a
			break;
		case 0xd2: // n/a
			break;
		case 0xd3: // n/a
			break;
		case 0xd4: // n/a
			break;
		case 0xd5: // n/a
			break;
		case 0xd6: // n/a
			break;
		case 0xd7: // n/a
			break;
		case 0xd8: // n/a
			break;
		case 0xd9: // n/a
			break;
		case 0xda: // n/a
			break;
		case 0xdb: // n/a
			break;
		case 0xdc: // n/a
			break;
		case 0xdd: // n/a
			break;
		case 0xde: // n/a
			break;
		case 0xdf: // n/a
			break;

		case 0xe0: // n/a
			break;
		case 0xe1: // n/a
			break;
		case 0xe2: // n/a
			break;
		case 0xe3: // n/a
			break;
		case 0xe4: // n/a
			break;
		case 0xe5: // n/a
			break;
		case 0xe6: // n/a
			break;
		case 0xe7: // n/a
			break;
		case 0xe8: // n/a
			break;
		case 0xe9: // n/a
			break;
		case 0xea: // NOP
			break;
		case 0xeb: // n/a
			break;
		case 0xec: // n/a
			break;
		case 0xed: // n/a
			break;
		case 0xee: // n/a
			break;
		case 0xef: // n/a
			break;

		case 0xf0: // n/a
			break;
		case 0xf1: // n/a
			break;
		case 0xf2: // n/a
			break;
		case 0xf3: // n/a
			break;
		case 0xf4: // n/a
			break;
		case 0xf5: // n/a
			break;
		case 0xf6: // n/a
			break;
		case 0xf7: // n/a
			break;
		case 0xf8: // n/a
			break;
		case 0xf9: // n/a
			break;
		case 0xfa: // n/a
			break;
		case 0xfb: // n/a
			break;
		case 0xfc: // n/a
			break;
		case 0xfd: // n/a
			break;
		case 0xfe: // n/a
			break;
		case 0xff: // n/a
			break;
		}
	}
	
	/**
	 * Returns a string representing the CPU state. 
	 */
	public String toString() {
		String opcode = CpuUtils.opcode(ir, operands[0], operands[1]);
		StringBuffer sb = new StringBuffer(String.format("$%04X", addr) + "  ");
		sb.append(String.format("%-12s", opcode));
		sb.append("A="  + String.format("$%02X", a)  + "; ");
		sb.append("X="  + String.format("$%02X", x)  + "; ");
		sb.append("Y="  + String.format("$%02X", y)  + "; ");
		sb.append("PC="  + String.format("$%04X", pc));
		return sb.toString();
	}
	
	/**
	 * Push an item onto the stack, and decrement the stack counter.
	 * Silently fails to push onto the stack if SP is
	 * TODO: Unit tests.  
	 */
	protected void push(int data) {
		bus.write(sp, data);
		if (sp > 0x100) { sp--; }
	}
	

	/**
	 * Pop a byte off the user stack, and increment the stack counter. 
	 * TODO: Unit tests.
	 */
	protected int pop() {
		int data = bus.read(sp);
		if (sp < 0x1ff) { sp++; }
		return data;
	}

	/*
	 * Increment the PC, rolling over if necessary.
	 */
	protected void incProgramCounter() {
		if (pc == 0xffff) {
			pc = 0;
		} else {
			++pc;
		}
	}
	
}