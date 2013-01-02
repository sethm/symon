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
