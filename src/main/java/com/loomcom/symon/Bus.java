/*
 * Copyright (c) 2008-2013 Seth J. Morabito <sethm@loomcom.com>
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

package com.loomcom.symon;

import com.loomcom.symon.devices.Device;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The Bus ties the whole thing together, man.
 */
public class Bus {

    // The default address at which to load programs
    public static int DEFAULT_LOAD_ADDRESS = 0x0200;
	
    // By default, our bus starts at 0, and goes up to 64K
    private int startAddress = 0x0000;
    private int endAddress = 0xffff;

    // The CPU
    private Cpu cpu;

    // Ordered list of IO devices.
    private SortedSet<Device> devices;

    public Bus(int size) {
        this(0, size - 1);
    }

    public Bus(int startAddress, int endAddress) {
        this.devices = new TreeSet<Device>();
        this.startAddress = startAddress;
        this.endAddress = endAddress;
    }

    public int startAddress() {
        return startAddress;
    }

    public int endAddress() {
        return endAddress;
    }

    /**
     * Add a device to the bus. Throws a MemoryRangeException if the device overlaps with any others.
     *
     * @param device
     * @throws MemoryRangeException
     */
    public void addDevice(Device device) throws MemoryRangeException {
        // Make sure there's no memory overlap.
        MemoryRange memRange = device.getMemoryRange();
        for (Device d : devices) {
            if (d.getMemoryRange().overlaps(memRange)) {
                throw new MemoryRangeException("The device being added at " +
                                               String.format("$%04X", memRange.startAddress()) +
                                               " overlaps with an existing " +
                                               "device, '" + d + "'");
            }
        }

        // Add the device
        device.setBus(this);
        devices.add(device);
    }

    /**
     * Remove a device from the bus.
     *
     * @param device
     */
    public void removeDevice(Device device) {
        if (devices.contains(device)) {
            devices.remove(device);
        }
    }

    public void addCpu(Cpu cpu) {
        this.cpu = cpu;
        cpu.setBus(this);
    }

    /**
     * Returns true if the memory map is full, i.e., there are no
     * gaps between any IO devices.  All memory locations map to some
     * device.
     */
    public boolean isComplete() {
        // Empty maps cannot be complete.
        if (devices.isEmpty()) {
            return false;
        }

        // Loop over devices and add their size
        int filledMemory = 0;
        for (Device d : devices) {
            filledMemory += d.getSize();
        }

        // Returns if the total size of the devices fill the bus' memory space
        return filledMemory == endAddress - startAddress + 1;
    }

    public int read(int address) throws MemoryAccessException {
        for (Device d : devices) {
            MemoryRange range = d.getMemoryRange();
            if (range.includes(address)) {
                // Compute offset into this device's address space.
                int devAddr = address - range.startAddress();
                return d.read(devAddr);
            }
        }
        throw new MemoryAccessException("Bus read failed. No device at address " + String.format("$%04X", address));
    }

    public void write(int address, int value) throws MemoryAccessException {
        for (Device d : devices) {
            MemoryRange range = d.getMemoryRange();
            if (range.includes(address)) {
                // Compute offset into this device's address space.
                int devAddr = address - range.startAddress();
                d.write(devAddr, value);
                return;
            }
        }
        throw new MemoryAccessException("Bus write failed. No device at address " + String.format("$%04X", address));
    }

    public SortedSet<Device> getDevices() {
        // Expose a copy of the device list, not the original
        return new TreeSet<Device>(devices);
    }

    public Cpu getCpu() {
        return cpu;
    }

    public void loadProgram(int... program) throws MemoryAccessException {
        int address = getCpu().getProgramCounter();
        int i = 0;
        for (int d : program) {
            write(address + i++, d);
        }
    }
}
