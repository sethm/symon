/*
 * Copyright (c) 2016 Seth J. Morabito <web@loomcom.com>
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

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

    // Ordered sets of IO devices, associated with their priority
    private Map<Integer, SortedSet<Device>> deviceMap;

    // an array for quick lookup of adresses, brute-force style
    private Device[] deviceAddressArray;


    public Bus(int size) {
        this(0, size - 1);
    }

    public Bus(int startAddress, int endAddress) {
        this.deviceMap = new HashMap<>();
        this.startAddress = startAddress;
        this.endAddress = endAddress;
    }

    public int startAddress() {
        return startAddress;
    }

    public int endAddress() {
        return endAddress;
    }

    private void buildDeviceAddressArray() {
        int size = (this.endAddress - this.startAddress) + 1;
        deviceAddressArray = new Device[size];

        // getDevices() provides an OrderedSet with devices ordered by priorities
        for (Device device : getDevices()) {
            MemoryRange range = device.getMemoryRange();
            for (int address = range.startAddress; address <= range.endAddress; ++address) {
                deviceAddressArray[address - this.startAddress] = device;
            }
        }

    }

    /**
     * Add a device to the bus.
     *
     * @param device   Device to add
     * @param priority Bus prioirity.
     * @throws MemoryRangeException
     */
    public void addDevice(Device device, int priority) throws MemoryRangeException {

        MemoryRange range = device.getMemoryRange();

        if (range.startAddress() < this.startAddress || range.startAddress() > this.endAddress) {
            throw new MemoryRangeException("start address of device " + device.getName() + " does not fall within the address range of the bus");
        }

        if (range.endAddress() < this.startAddress || range.endAddress() > this.endAddress) {
            throw new MemoryRangeException("end address of device " + device.getName() + " does not fall within the address range of the bus");
        }

        SortedSet<Device> deviceSet = deviceMap.get(priority);

        if (deviceSet == null) {
            deviceSet = new TreeSet<>();
            deviceMap.put(priority, deviceSet);
        }

        device.setBus(this);
        deviceSet.add(device);
        buildDeviceAddressArray();
    }

    /**
     * Add a device to the bus. Throws a MemoryRangeException if the device overlaps with any others.
     *
     * @param device Device to add
     * @throws MemoryRangeException
     */
    public void addDevice(Device device) throws MemoryRangeException {
        addDevice(device, 0);
    }


    /**
     * Remove a device from the bus.
     *
     * @param device Device to remove
     */
    public void removeDevice(Device device) {
        for (SortedSet<Device> deviceSet : deviceMap.values()) {
            deviceSet.remove(device);
        }
        buildDeviceAddressArray();
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
        if (deviceAddressArray == null) {
            buildDeviceAddressArray();
        }

        for (int address = startAddress; address <= endAddress; ++address) {
            if (deviceAddressArray[address - startAddress] == null) {
                return false;
            }
        }

        return true;
    }

    public int read(int address, boolean cpuAccess) throws MemoryAccessException {
        Device d = deviceAddressArray[address - this.startAddress];
        if (d != null) {
            MemoryRange range = d.getMemoryRange();
            int devAddr = address - range.startAddress();
            return d.read(devAddr, cpuAccess) & 0xff;
        }

        throw new MemoryAccessException("Bus read failed. No device at address " + String.format("$%04X", address));
    }

    public void write(int address, int value) throws MemoryAccessException {
        Device d = deviceAddressArray[address - this.startAddress];
        if (d != null) {
            MemoryRange range = d.getMemoryRange();
            int devAddr = address - range.startAddress();
            d.write(devAddr, value);
            return;
        }

        throw new MemoryAccessException("Bus write failed. No device at address " + String.format("$%04X", address));
    }

    public void assertIrq() {
        if (cpu != null) {
            cpu.assertIrq();
        }
    }

    public void clearIrq() {
        if (cpu != null) {
            cpu.clearIrq();
        }
    }

    public void assertNmi() {
        if (cpu != null) {
            cpu.assertNmi();
        }
    }

    public void clearNmi() {
        if (cpu != null) {
            cpu.clearNmi();
        }
    }

    public SortedSet<Device> getDevices() {
        // create an ordered set of devices, ordered by device priorities
        SortedSet<Device> devices = new TreeSet<>();

        List<Integer> priorities = new ArrayList<>(deviceMap.keySet());
        Collections.sort(priorities);

        for (int priority : priorities) {
            SortedSet<Device> deviceSet = deviceMap.get(priority);
            for (Device device : deviceSet) {
                devices.add(device);
            }
        }

        return devices;
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
