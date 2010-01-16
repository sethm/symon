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

  public String getState() throws MemoryAccessException {
    return cpu.toString();
  }

  public void run() throws MemoryAccessException {
    parser.run();
  }

  public void load(int address, int[] program)
    throws MemoryAccessException {
    int i = 0;
    for (int d : program) {
      bus.write(address + i++, d);
    }
  }

  /**
   * A test method.
   */
  public void runTest() throws MemoryAccessException {
    int[] zpData = {
      0x39,             // $0000
      0x21,             // $0001
      0x12              // $0002
    };
    int[] data = {
      0xae,             // $c800
      0x13,             // $c801
      0x29              // $c802
    };
    int[] program = {
      0xa9, 0xff,       // LDA #$FF
      0xa0, 0x1a,       // LDY #$1A
      0xa2, 0x90,       // LDX #$90
      0xa2, 0x02,       // LDX #$02
      0x49, 0xff,       // EOR #$FF
      0xa9, 0x00,       // LDA #$00
      0xa2, 0x00,       // LDX #$00
      0x29, 0xff,       // AND #$FF
      0xa0, 0x00,       // LDY #$00
      0xa5, 0x00,       // LDA $00
      0xad, 0x00, 0xc8, // LDA $c800
      0x4c, 0x00, 0x03  // JMP #$0300
    };
    int programLength = 12;

    load(0x0000, zpData);
    load(0x0300, program);
    load(0xc800, data);
    cpu.setResetVector(0x0300);
    cpu.reset();

    for (int i = 0; i <= programLength; i++) {
      cpu.step();
      System.out.println(cpu.toString());
    }
  }

  /**
   * Main simulator routine.
   */
  public static void main(String[] args) throws MemoryAccessException {
    try {
      new Simulator().run();
    } catch (MemoryRangeException ex) {
      System.err.println("Error: " + ex.toString());
    }
  }

}
