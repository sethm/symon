package com.loomcom.lm6502;

import java.io.IOException;

/**
 * Main control class for the J6502 Simulator.
 */
public class Simulator {

	/**
	 * Command-line parser used by this simulator.
	 */
	private CommandParser parser;

	/**
	 * The CPU itself.
	 */
	private Cpu cpu;

	/**
	 * The Bus responsible for routing memory read/write requests to the
	 * correct IO devices.
	 */
	private Bus bus;

	public Simulator() {
		cpu = new Cpu();
		parser = new CommandParser(System.in, System.out, this);
	}

	public void run() {
		parser.run();
	}

	public void step() {
	}

	public int read(int address) {
		return 0;
	}

	public void write(int address, int value) {
	}

	/**
	 * Main simulator routine.
	 */
	public static void main(String[] args) {
		new Simulator().run();
	}

}
