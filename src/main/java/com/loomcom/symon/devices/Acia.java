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

import com.loomcom.symon.exceptions.MemoryRangeException;


/**
 * Abstract base class for ACIAS such as the 6551 and 6580
 */

public abstract class Acia extends Device {

    private String name;

    /**
     * Register addresses
     */
    int baseAddress;

    boolean receiveIrqEnabled = false;
    boolean transmitIrqEnabled = false;
    boolean overrun = false;
    boolean interrupt = false;

    long lastTxWrite   = 0;
    long lastRxRead    = 0;
    int  baudRate      = 0;
    long baudRateDelay = 0;

    /**
     * Read/Write buffers
     */
    int rxChar = 0;
    int txChar = 0;

    boolean rxFull  = false;
    boolean txEmpty = true;


    public Acia(int address, int size, String name) throws MemoryRangeException {
        super(address, address + size - 1, name);
        this.name = name;
        this.baseAddress = address;
    }


    /*
     * Calculate the delay in nanoseconds between successive read/write operations, based on the
     * configured baud rate.
     */
    private long calculateBaudRateDelay() {
        if (baudRate > 0) {
            // TODO: This is a pretty rough approximation based on 8 bits per character,
            // and 1/baudRate per bit. It could certainly be improved
            return (long)((1.0 / baudRate) * 1000000000 * 8);
        } else {
            return 0;
        }
    }

    /**
     * @return The simulated baud rate in bps.
     */
    @SuppressWarnings("unused")
    public int getBaudRate() {
        return baudRate;
    }

    /**
     * Set the baud rate of the simulated ACIA.
     *
     * @param rate The baud rate in bps. 0 means no simulated baud rate delay.
     */
    public void setBaudRate(int rate) {
        this.baudRate = rate;
		this.baudRateDelay = calculateBaudRateDelay();
    }

    /**
     * @return The contents of the status register.
     */
    public abstract int statusReg(boolean cpuAccess);

    @Override
    public String toString() {
        return name + "@" + String.format("%04X", baseAddress);
    }

    public synchronized int rxRead(boolean cpuAccess) {
        if (cpuAccess) {
            lastRxRead = System.nanoTime();
            overrun = false;
            rxFull = false;
        }
        return rxChar;
    }

    public synchronized void rxWrite(int data) {
        if (rxFull) {
            overrun = true;
        }

        rxFull = true;

        if (receiveIrqEnabled) {
            interrupt = true;
            getBus().assertIrq();
        }

        rxChar = data;
    }

    public synchronized int txRead(boolean cpuAccess) {
        if (cpuAccess) {
            txEmpty = true;

            if (transmitIrqEnabled) {
                interrupt = true;
                getBus().assertIrq();
            }
        }
        return txChar;
    }

    public synchronized void txWrite(int data) {
        lastTxWrite = System.nanoTime();
        txChar = data;
        txEmpty = false;
    }

    /**
     * @return true if there is character data in the TX register.
     */
    public boolean hasTxChar() {
        return !txEmpty;
    }

    /**
     * @return true if there is character data in the RX register.
     */
    @SuppressWarnings("unused")
    public boolean hasRxChar() {
        return rxFull;
    }

}
