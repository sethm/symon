package com.loomcom.symon;

import junit.framework.*;

import com.loomcom.symon.devices.*;
import com.loomcom.symon.exceptions.*;

import java.util.*;

/**
 *
 */
public class BusTest extends TestCase {

  public BusTest(String testName) {
    super(testName);
  }

  public static Test suite() {
    return new TestSuite(BusTest.class);
  }

  public void testCreatingWithStartAndEndAddresses() {
    Bus b = null;

    b = new Bus(0x00, 0xff);
    assertEquals(0x00, b.startAddress());
    assertEquals(0xff, b.endAddress());

    b = new Bus(0x20, 0xea);
    assertEquals(0x20, b.startAddress());
    assertEquals(0xea, b.endAddress());
  }

  public void testCreatingWithSize() {
    Bus b = null;

    b = new Bus(256);
    assertEquals(0x00, b.startAddress());
    assertEquals(0xff, b.endAddress());

    b = new Bus(4096);
    assertEquals(0x000, b.startAddress());
    assertEquals(0xfff, b.endAddress());

    b = new Bus(65536);
    assertEquals(0x0000, b.startAddress());
    assertEquals(0xffff, b.endAddress());
  }

  public void testAddDevice() throws MemoryRangeException {
    Device memory = new Memory(0x0000, 0x0100, true);
    Device rom    = new Memory(0x0100, 0x0200, false);

    Bus b = new Bus(0x0000, 0xffff);

    assertEquals(0, b.getDevices().size());
    b.addDevice(memory);
    assertEquals(1, b.getDevices().size());
    b.addDevice(rom);
    assertEquals(2, b.getDevices().size());
  }

  public void testOverlappingDevicesShouldFail() throws MemoryRangeException {
    Device memory = new Memory(0x0000, 0x0100, true);
    Device rom    = new Memory(0x00ff, 0x0200, false);

    Bus b = new Bus(0x0000, 0xffff);

    b.addDevice(memory);

    try {
      b.addDevice(rom);
      fail("Should have thrown a MemoryRangeException.");
    } catch (MemoryRangeException ex) {
      // expected
    }
  }

  public void testIsCompleteWithFirstDeviceNotStartingAtStartAddress() throws MemoryRangeException {
    Device memory = new Memory(0x00ff, 0xff00, true);

    Bus b = new Bus(0x0000, 0xffff);
    assertFalse("Address space was unexpectedly complete!", b.isComplete());
    b.addDevice(memory);
    assertFalse("Address space was unexpectedly complete!", b.isComplete());
  }

  public void testIsCompleteWithOneDevice() throws MemoryRangeException {
    Device memory = new Memory(0x0000, 0x10000, true);

    Bus b = new Bus(0x0000, 0xffff);
    assertFalse("Address space was unexpectedly complete!", b.isComplete());
    b.addDevice(memory);
    assertTrue("Address space should have been complete!", b.isComplete());
  }

  public void testIsCompleteWithTwoDevices() throws MemoryRangeException {
    Device memory = new Memory(0x0000, 0x8000, true);
    Device rom    = new Memory(0x8000, 0x8000, false);

    Bus b = new Bus(0x0000, 0xffff);
    assertFalse("Address space was unexpectedly complete!", b.isComplete());
    b.addDevice(memory);
    assertFalse("Address space was unexpectedly complete!", b.isComplete());
    b.addDevice(rom);
    assertTrue("Address space should have been complete!", b.isComplete());
  }

  public void testIsCompleteWithThreeDevices() throws MemoryRangeException {
    Device memory = new Memory(0x0000, 0x8000, true);
    Device rom1   = new Memory(0x8000, 0x4000, false);
    Device rom2   = new Memory(0xC000, 0x4000, false);

    Bus b = new Bus(0x0000, 0xffff);
    assertFalse("Address space was unexpectedly complete!", b.isComplete());
    b.addDevice(memory);
    assertFalse("Address space was unexpectedly complete!", b.isComplete());
    b.addDevice(rom1);
    assertFalse("Address space was unexpectedly complete!", b.isComplete());
    b.addDevice(rom2);
    assertTrue("Address space should have been complete!", b.isComplete());
  }

}
