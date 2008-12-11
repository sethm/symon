package com.loomcom.lm6502;

import java.io.IOException;

/**
 * Main control class for the J6502 Simulator.
 */
public class Simulator {

	/**
	 * Command-line parser used by this simulator.
	 */
	CommandParser m_parser;

	/**
	 * The CPU itself.
	 */
	Cpu m_cpu;

	/**
	 * The Bus responsible for routing memory read/write requests to the
	 * correct IO devices.
	 */
	Bus m_bus;

	public Simulator() {
		m_cpu = new Cpu(this);
		m_parser = new CommandParser(System.in, System.out, this);
	}

	public void run() {
		m_parser.run();
	}

	public static void main(String[] args) {
		new Simulator().run();
	}

	public void step() {
	}

	public int read(int address) {
		return 0;
	}

	public void write(int address, int value) {
	}
}
