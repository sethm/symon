/**
 * A simple profiler, for debugging the simulator.
 * It is safe to ignore me.
 */

package com.loomcom.lm6502;

import java.util.*;
import com.loomcom.lm6502.devices.*;
import com.loomcom.lm6502.exceptions.*;

public class Profiler {
	public static void main(String[] args) {

		try {

			// Create a bus.
			Bus b = new Bus(0, 65535);

			// Create eight devices, each 8KB, to fill the bus.
			b.addDevice(new Memory(0x0000, 0x2000)); // 8KB @ $0000-$1fff
			b.addDevice(new Memory(0x2000, 0x2000)); // 8KB @ $2000-$3fff
			b.addDevice(new Memory(0x4000, 0x2000)); // 8KB @ $4000-$5fff
			b.addDevice(new Memory(0x6000, 0x2000)); // 8KB @ $6000-$7fff
			b.addDevice(new Memory(0x8000, 0x2000)); // 8KB @ $8000-$9fff
			b.addDevice(new Memory(0xa000, 0x2000)); // 8KB @ $a000-$bfff
			b.addDevice(new Memory(0xc000, 0x2000)); // 8KB @ $c000-$dfff
			b.addDevice(new Memory(0xe000, 0x2000)); // 8KB @ $e000-$ffff

			// Read memory
			long sum     = 0;
			long average = 0;

			long iters   = 500;

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
		} catch (MemoryRangeException ex) {
			System.out.println("Memory Access Exception! " + ex.getMessage());
		}
	}
}
