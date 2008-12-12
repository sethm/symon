package com.loomcom.lm6502;

/**
 * Main 6502 CPU Simulation.
 */
public class Cpu {

	/* The Bus */
	private Bus bus;

	/* User Registers */
	private int a;    // Accumulator
	private int x;    // X index register
	private int y;    // Y index register

	/* Internal Registers */
	private int pc;   // Program Counter register
	private int sp;   // Stack Pointer register
	private int ir;   // Instruction register

	/**
	 * Construct a new CPU.
	 */
	public Cpu() {
		reset();
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
		sp = 0x01ff;
		pc = 0xfffc;
		ir = 0;
	}

}

