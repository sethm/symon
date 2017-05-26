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
    public int read(int address, boolean cpuAccess) throws MemoryAccessException {
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
