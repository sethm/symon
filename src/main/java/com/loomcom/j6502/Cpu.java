package com.loomcom.j6502;

/**
 * Main 6502 CPU Simulation.
 */
public class Cpu {

	/**
	 * The Program Counter.
	 */
	private int m_pc;

	/**
	 * The system stack pointer.
	 */
	private int m_sp;

	/**
	 * Reference to the simulator
	 */
	private Simulator m_sim;

	public Cpu(Simulator sim) {
		reset();
		this.m_sim = sim;
	}

	/**
	 * Reset the CPU to known initial values.
	 */
	public void reset() {
		m_sp = 0x01ff;
		/* locations fffc and fffd hold the reset vector address */
		m_pc = 0xfffc;
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
		return readAddress(m_pc);
	}

	/**
	 * Read the two bytes located at <tt>addr</tt> and <tt>addr + 1</tt>,
	 * and return the address held there.
	 *
	 * @param address
	 * @return The address specified in the two bytes at location <tt>addr</tt>
	 */
	private int readAddress(int address) {
		return (m_sim.read(address)<<8 & m_sim.read(address+1));
	}

	public Simulator getSimulator() {
		return m_sim;
	}

}

