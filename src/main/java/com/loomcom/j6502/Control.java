package com.loomcom.j6502;

import java.io.IOException;

/**
 * Main control class for the J6502 Simulator.
 */
public class Control {

    public static void main(String[] args) {

	CommandParser parser = new CommandParser(System.in, System.out);
	parser.run();

    }

}