package com.loomcom.symon.devices;

import java.io.*;
import java.util.*;

import com.loomcom.symon.exceptions.*;

import javax.swing.*;

public class Memory extends Device {

    private boolean readOnly;
    private int[] mem;

    /* Initialize all locations to 0x00 (BRK) */
    private static final int DEFAULT_FILL = 0x00;

    public Memory(int address, int size, boolean readOnly)
            throws MemoryRangeException {
        super(address, size, (readOnly ? "RO Memory" : "RW Memory"));
        this.readOnly = readOnly;
        this.mem = new int[size];
        this.fill(DEFAULT_FILL);
    }

    public Memory(int address, int size) throws MemoryRangeException {
        this(address, size, false);
    }

    public static Memory makeROM(int address, int size, File f) throws MemoryRangeException, IOException {
        Memory memory = new Memory(address, size, true);
        memory.loadFromFile(f);
        return memory;
    }

    public static Memory makeRAM(int address, int size) throws MemoryRangeException {
        Memory memory = new Memory(address, size, false);
        return memory;
    }

    public void write(int address, int data) throws MemoryAccessException {
        if (readOnly) {
            throw new MemoryAccessException("Cannot write to read-only memory at address " + address);
        } else {
            this.mem[address] = data;
        }
    }

    /**
     * Load the memory from a file.
     *
     * @param file The file to read an array of bytes from.
     * @throws MemoryRangeException if the file and memory size do not match.
     * @throws IOException if the file read fails.
     */
    public void loadFromFile(File file) throws MemoryRangeException, IOException {
        if (file.canRead()) {
            long fileSize = file.length();

            if (fileSize > mem.length) {
                throw new MemoryRangeException("File will not fit in available memory.");
            } else {
                int i = 0;
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                DataInputStream dis = new DataInputStream(bis);
                while (dis.available() != 0) {
                    mem[i++] = dis.readUnsignedByte();
                }
            }
        } else {
            throw new IOException("Cannot open file " + file);
        }

    }

    public int read(int address) throws MemoryAccessException {
        return this.mem[address];
    }

    public void fill(int val) {
        Arrays.fill(this.mem, val);
    }

    public String toString() {
        return "Memory: " + getMemoryRange().toString();
    }

}