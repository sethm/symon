/**
 * A simple profiler, for debugging the simulator.
 * It is safe to ignore me.
 */

package com.loomcom.symon;

import com.loomcom.symon.devices.*;
import com.loomcom.symon.exceptions.*;

public class Profiler implements InstructionTable {

  public static void main(String[] args) {
    // new Profiler().profileMemoryReads();
    // new Profiler().dumpOpCodes();
    new Profiler().profileProgram();
  }

  public void dumpOpCodes() {
    for (int i = 0; i < 0x100; i++) {
      String name = opcodeNames[i];
      Mode mode = instructionModes[i];

      System.out.print(String.format("0x%02x: ", i));

      if (name == null) {
        System.out.println("n/a");
      } else {
        System.out.println(name + " (" + mode + ")");
      }
    }
  }

  public void profileProgram() {
    Bus bus = new Bus(0, 65535);
    Cpu cpu = new Cpu();

    bus.addCpu(cpu);

    try {
      bus.addDevice(new Memory(0x0000, 0x10000));
    } catch (MemoryRangeException ex) {
      System.err.println("Memory Range Exception! " + ex.getMessage());
      return;
    }

    // Start at 0x0300
    bus.write(0xfffc, 0x00);
    bus.write(0xfffd, 0x03);

    // The program to run, in an infinite loop.
    bus.write(0x0300, 0xa9); // LDA #$FF
    bus.write(0x0301, 0xff);
    bus.write(0x0302, 0xea); // NOP
    bus.write(0x0303, 0xea); // NOP
    bus.write(0x0304, 0xa0); // LDY #$1A
    bus.write(0x0305, 0x1a);
    bus.write(0x0306, 0xea); // NOP
    bus.write(0x0307, 0xea); // NOP
    bus.write(0x0308, 0xa2); // LDX #$03
    bus.write(0x0309, 0x03);

    bus.write(0x030a, 0xa9); // LDA #$00
    bus.write(0x030b, 0x00);
    bus.write(0x030c, 0xa2); // LDX #$00
    bus.write(0x030d, 0x00);
    bus.write(0x030e, 0xa0); // LDY #$00
    bus.write(0x030f, 0x00);

    bus.write(0x0310, 0x4c); // JMP #$0300
    bus.write(0x0311, 0x00);
    bus.write(0x0312, 0x03);


    long sum = 0;

    // The number of times to run the program
    long iters = 1000;

    // The number of steps to take when running the program
    long steps = 100000;

    for (int i = 0; i < iters; i++) {
      long startTime = System.nanoTime();

      // Reset the CPU (does not clear memory)
      cpu.reset();

      for (int j = 0; j < steps; j++) {
        cpu.step();
      }

      long endTime = System.nanoTime();
      long diff    = endTime - startTime;

      sum += diff;
    }

    long average    = sum / iters;
    long totalSteps = steps * iters;
    long avgStep    = sum / totalSteps;

    System.out.println("Total instructions executed:  " +
                       String.format("%,d", totalSteps));
    System.out.println("Total time taken:             " +
                       String.format("%,d us", sum / 1000));
    System.out.println("Average time per step:        " +
                       avgStep + " ns ");
  }

  public void profileMemoryReads() {
    // Create a bus.
    Bus b = new Bus(0, 65535);

    try {
      // Create eight devices, each 8KB, to fill the bus.
      b.addDevice(new Memory(0x0000, 0x2000)); // 8KB @ $0000-$1fff
      b.addDevice(new Memory(0x2000, 0x2000)); // 8KB @ $2000-$3fff
      b.addDevice(new Memory(0x4000, 0x2000)); // 8KB @ $4000-$5fff
      b.addDevice(new Memory(0x6000, 0x2000)); // 8KB @ $6000-$7fff
      b.addDevice(new Memory(0x8000, 0x2000)); // 8KB @ $8000-$9fff
      b.addDevice(new Memory(0xa000, 0x2000)); // 8KB @ $a000-$bfff
      b.addDevice(new Memory(0xc000, 0x2000)); // 8KB @ $c000-$dfff
      b.addDevice(new Memory(0xe000, 0x2000)); // 8KB @ $e000-$ffff
    } catch (MemoryRangeException ex) {
      System.err.println("Memory Range Exception! " + ex.getMessage());
      return;
    }

    // Read memory
    long sum = 0;
    long average = 0;

    long iters = 500;
    for (int i = 0; i < iters; i++) {
      long startTime = System.nanoTime();
      // Read and assign to a buffer
      int buf = 0;
      for (int j = 0; j < 0xffff; j++) {
        buf = b.read(j);
        if (buf != 0xff) {
          System.out.println("WARNING!  MEMORY SHOULD HAVE " +
                             "BEEN $FF, WAS: " + buf);
          System.exit(0);
        }
      }

      long endTime = System.nanoTime();
      long diff    = endTime - startTime;

      sum += diff;
      average = sum / (i + 1);
    }
    System.out.println("Average time to read 64KB: " + average +
                       " ns (" + (average / 1000) + " us)");
    System.out.println("Average time to read one byte: " +
                       sum / (64 * 1024 * iters) + " ns");
  }
}
