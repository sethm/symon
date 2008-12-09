package com.loomcom.lm6502;

/**
 * Main 6502 CPU Simulation.
 */
public class Cpu {

	private int pc;
	private int sp;
	private Simulator sim;

	public Cpu(Simulator sim) {
		reset();
		this.sim = sim;
	}

	/**
	 * Reset the CPU to known initial values.
	 */
	public void reset() {
		sp = 0x01ff;
		/* locations fffc and fffd hold the reset vector address */
		pc = 0xfffc;
	}

	/**
	 * Trigger a maskable interrupt.
	 */
	public void interrupt() {
	}

	/**
	 * Trigger a nonmaskable interrupt.
	 */
	public void nmiInterrupt() {
	}

	/**
	 * @return An address specified by the two bytes immediately following the
	 *         Program Counter.
	 */
	private int readAddress() {
		return readAddress(pc);
	}

	/**
	 * Read the two bytes located at <tt>addr</tt> and <tt>addr + 1</tt>,
	 * and return the address held there.
	 *
	 * @param address
	 * @return The address specified in the two bytes at location <tt>addr</tt>
	 */
	private int readAddress(int address) {
		return (sim.read(address)<<8 & sim.read(address+1));
	}

	public Simulator getSimulator() {
		return sim;
	}

}

