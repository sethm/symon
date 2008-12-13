package com.loomcom.symon;

import com.loomcom.symon.devices.*;
import com.loomcom.symon.exceptions.*;

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

	public Simulator() throws MemoryRangeException {
		cpu = new Cpu();
		bus = new Bus(0x0000, 0xffff);
		bus.addCpu(cpu);
		bus.addDevice(new Memory(0x0000, 0x10000));
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
	 * A test method.
	 */
	
	public void runTest() {
		
		// Start at 0x0300
		bus.write(0xfffc, 0x00);
		bus.write(0xfffd, 0x03);
		
		bus.write(0x0300, 0xa9); // LDA #$FF
		bus.write(0x0301, 0xff); 
		bus.write(0x0302, 0xea); // NOP
		bus.write(0x0303, 0xea); // NOP
		bus.write(0x0304, 0xea); // NOP
		bus.write(0x0305, 0xa0); // LDY #$1A
		bus.write(0x0306, 0x1a);
		bus.write(0x0307, 0xea); // NOP
		bus.write(0x0308, 0xea); // NOP
		bus.write(0x0309, 0xa2); // LDX #$90
		bus.write(0x030a, 0x90);
		
		bus.write(0x030b, 0xea); // NOP
		bus.write(0x030c, 0xea); // NOP
		bus.write(0x030d, 0xea); // NOP

		bus.write(0x030e, 0xa2); // LDX #$02
		bus.write(0x030f, 0x02);

		bus.write(0x0310, 0xa9); // LDA #$00
		bus.write(0x0311, 0x00);
		bus.write(0x0312, 0xa2); // LDX #$00
		bus.write(0x0313, 0x00);
		bus.write(0x0314, 0xa0); // LDY #$00
		bus.write(0x0315, 0x00);
		
		bus.write(0x0316, 0x4c); // JMP #$0300
		bus.write(0x0317, 0x00); 
		bus.write(0x0318, 0x03);

		cpu.reset();

		for (int i = 0; i <= 40; i++) {
			cpu.step();
			System.out.println(cpu.toString());
		}

	}

	/**
	 * Main simulator routine.
	 */
	public static void main(String[] args) {
		try {
			new Simulator().runTest();
		} catch (MemoryRangeException ex) {
			System.err.println("Error: " + ex.toString());
		}
	}

}
