package com.loomcom.symon.devices;

import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;

/**
 * Very basic implementation of a MOS 6522 VIA.
 *
 * TODO: Implement timers as threads.
 */
public class Via6522 extends Pia {
    public static final int VIA_SIZE = 16;

    enum Register {
        ORB, ORA, DDRB, DDRA, T1C_L, T1C_H, T1L_L, T1L_H,
        T2C_L, T2C_H, SR, ACR, PCR, IFR, IER, ORA_H
    }

    // Ports A and B
    private char[] portData = {0, 0};
    private char[] portDirections = {0, 0};

    public Via6522(int address) throws MemoryRangeException {
        super(address, address + VIA_SIZE - 1, "MOS 6522 VIA");
    }

    @Override
    public void write(int address, int data) throws MemoryAccessException {
        Register[] registers = Register.values();

        if (address >= registers.length) {
            throw new MemoryAccessException("Unknown register: " + address);
        }

        Register r = registers[address];

        switch (r) {
            case ORA:
            case ORB:
            case DDRA:
            case DDRB:
            case T1C_L:
            case T1C_H:
            case T1L_L:
            case T1L_H:
            case T2C_L:
            case T2C_H:
            case SR:
            case ACR:
            case PCR:
            case IFR:
            case IER:
            case ORA_H:
            default:
        }
    }

    @Override
    public int read(int address) throws MemoryAccessException {
        Register[] registers = Register.values();

        if (address >= registers.length) {
            throw new MemoryAccessException("Unknown register: " + address);
        }

        Register r = registers[address];

        switch (r) {
            case ORA:
            case ORB:
            case DDRA:
            case DDRB:
            case T1C_L:
            case T1C_H:
            case T1L_L:
            case T1L_H:
            case T2C_L:
            case T2C_H:
            case SR:
            case ACR:
            case PCR:
            case IFR:
            case IER:
            case ORA_H:
            default:
        }

        return 0;
    }
}
