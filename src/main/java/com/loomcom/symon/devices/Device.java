/*
 * Copyright (c) 2008-2012 Seth J. Morabito <sethm@loomcom.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.loomcom.symon.devices;

import com.loomcom.symon.*;
import com.loomcom.symon.exceptions.*;

/**
 * A memory-mapped IO Device.
 */

public abstract class Device implements Comparable<Device> {

  /** The memory range for this device. */
  private MemoryRange memoryRange;

  /** The name of the device. */
  private String name;

  /** Reference to the bus where this Device is attached. */
  private Bus bus;

  public Device(int address, int size, String name)
    throws MemoryRangeException {
    this.memoryRange = new MemoryRange(address, address + size - 1);
    this.name = name;
  }

  public Device(int address, int size) throws MemoryRangeException {
    this(address, size, null);
  }

  /* Methods required to be implemented by inheriting classes. */
  public abstract void write(int address, int data) throws MemoryAccessException;
  public abstract int read(int address) throws MemoryAccessException;
  public abstract String toString();

  public void setBus(Bus bus) {
    this.bus = bus;
  }
  
  public Bus getBus() {
    return this.bus;
  }

  public MemoryRange getMemoryRange() {
    return memoryRange;
  }

  public int endAddress() {
    return memoryRange.endAddress();
  }

  public int startAddress() {
    return memoryRange.startAddress();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * Compares two devices.  The sort order is defined by the sort
   * order of the device's memory ranges.
   */
  public int compareTo(Device other) {
    if (other == null) {
      throw new NullPointerException("Cannot compare to null.");
    }
    if (this == other) {
      return 0;
    }
    return getMemoryRange().compareTo(other.getMemoryRange());
  }
}
