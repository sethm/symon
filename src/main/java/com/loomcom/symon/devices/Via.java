package com.loomcom.symon.devices;

import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;

public class Via extends Device {
    public static final int VIA_SIZE = 16;

    private static final int ORB = 0;
    private static final int ORA = 1;
    private static final int DDRB = 2;
    private static final int DDRA = 3;
    private static final int T1C_L = 4;
    private static final int T1C_H = 5;
    private static final int T1L_L = 6;
    private static final int T1L_H = 7;
    private static final int T2C_L = 8;
    private static final int T2C_H = 9;
    private static final int SR = 10;
    private static final int ACR = 11;
    private static final int PCR = 12;
    private static final int IFR = 13;
    private static final int IER = 14;
    private static final int ORA_H = 15;

    public Via(int address) throws MemoryRangeException {
      super(address, VIA_SIZE, "VIA");
    }

    @Override
    public void write(int address, int data) throws MemoryAccessException {

    }

    @Override
    public int read(int address) throws MemoryAccessException {
        return 0;
    }

    @Override
    public String toString() {
        return null;
    }
}
