/**
 * 
 */
package com.loomcom.j6502;

import static org.junit.Assert.*;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

/**
 * @author sethm
 *
 */
public class CpuTest {

	@Test
	public void testCpu() {
		Cpu cpu = new Cpu(new Simulator());
		assertNotNull(cpu);
	}

	@Test
	public void testReset() {
		fail("Not yet implemented");
	}

	@Test
	public void testInterrupt() {
		fail("Not yet implemented");
	}

	@Test
	public void testNmiInterrupt() {
		fail("Not yet implemented");
	}
	
	@Test
	public void testGetSimulator() {
		Simulator sim = new Simulator();
		Cpu cpu = new Cpu(sim);
		assertEquals(sim, cpu.getSimulator());		
	}

}

class MockSimulator extends Simulator {
	public int step;
	public boolean hasRun = false;
	public Map<Integer,Integer> memory;

	@Override
	public int read(int address) {
		Integer val = memory.get(new Integer(address));
		if (val == null) {
			throw new NullPointerException("Read from a non-existent memory location");
		} else {
			return val.intValue();
		}
	}

	@Override
	public void run() {
		hasRun = true;
	}

	@Override
	public void step() {
		step++;
	}

	@Override
	public void write(int address, int value) {
		// TODO Auto-generated method stub
		super.write(address, value);
	}
}
