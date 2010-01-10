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

  public void loadProgram(int address, int[] program) throws MemoryAccessException {
    // Reset interrupt vector
    int hi = (address&0xff00)>>>8;
    int lo = address&0x00ff;
    bus.write(0xfffc, lo);
    bus.write(0xfffd, hi);

    int i = 0;
    for (int d : program) {
      bus.write(address + i++, d);
    }
  }

  /**
   * A test method.
   */

  public void runTest() throws MemoryAccessException {
    int[] program = {
      0xa9, // LDA #$FF
      0xff,
      0xa0, // LDY #$1A
      0x1a,
      0xa2, // LDX #$90
      0x90,
      0xa2, // LDX #$02
      0x02,
      0x49, // EOR #$FF
      0xff,
      0xa9, // LDA #$00
      0x00,
      0xa2, // LDX #$00
      0x00,
      0x29, // AND #$FF
      0xff,
      0xa0, // LDY #$00
      0x00,
      0x4c, // JMP #$0300
      0x00,
      0x03
    };

    loadProgram(0x0300, program);
    cpu.reset();

    int steps = program.length;

    for (int i = 0; i <= steps; i++) {
      cpu.step();
      System.out.println(cpu.toString());
    }

  }

  /**
   * Main simulator routine.
   */
  public static void main(String[] args) throws MemoryAccessException {
    try {
      new Simulator().runTest();
    } catch (MemoryRangeException ex) {
      System.err.println("Error: " + ex.toString());
    }
  }

}
