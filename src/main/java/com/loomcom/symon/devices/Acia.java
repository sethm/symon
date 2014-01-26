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


/**
 * This is a simulation of the MOS 6551 ACIA, with limited
 * functionality.  Interrupts are not supported.
 * <p/>
 * Unlike a 16550 UART, the 6551 ACIA has only one-byte transmit and
 * receive buffers. It is the programmer's responsibility to check the
 * status (full or empty) for transmit and receive buffers before
 * writing / reading.
 */
public class Acia extends Device {

    public static final int ACIA_SIZE = 4;

    static final int DATA_REG = 0;
    static final int STAT_REG = 1;
    static final int CMND_REG = 2;
    static final int CTRL_REG = 3;

    /**
     * Register addresses
     */
    private int baseAddress;

    /**
     * Registers. These are ignored in the current implementation.
     */
    private int commandRegister;
    private int controlRegister;

    private boolean receiveIrqEnabled = false;
    private boolean transmitIrqEnabled = false;
    private boolean overrun = false;

    private long lastTxWrite   = 0;
    private long lastRxRead    = 0;
    private int  baudRate      = 0;
    private long baudRateDelay = 0;

    /**
     * Read/Write buffers
     */
    private int rxChar = 0;
    private int txChar = 0;

    private boolean rxFull  = false;
    private boolean txEmpty = true;

    public Acia(int address) throws MemoryRangeException {
        super(address, address + ACIA_SIZE - 1, "ACIA");
        this.baseAddress = address;
    }

    @Override
    public int read(int address) throws MemoryAccessException {
        switch (address) {
            case DATA_REG:
                return rxRead();
            case STAT_REG:
                return statusReg();
            case CMND_REG:
                return commandRegister;
            case CTRL_REG:
                return controlRegister;
            default:
                throw new MemoryAccessException("No register.");
        }
    }

    @Override
    public void write(int address, int data) throws MemoryAccessException {
        switch (address) {
            case 0:
                txWrite(data);
                break;
            case 1:
                reset();
                break;
            case 2:
                setCommandRegister(data);
                break;
            case 3:
                setControlRegister(data);
                break;
            default:
                throw new MemoryAccessException("No register.");
        }
    }


    private void setCommandRegister(int data) {
        commandRegister = data;

        // Bit 1 controls receiver IRQ behavior
        receiveIrqEnabled = (commandRegister & 0x02) == 0;
        // Bits 2 & 3 controls transmit IRQ behavior
        transmitIrqEnabled = (commandRegister & 0x08) == 0 && (commandRegister & 0x04) != 0;
    }

    /**
     * Set the control register and associated state.
     *
     * @param data
     */
    private void setControlRegister(int data) {
        controlRegister = data;

        // If the value of the data is 0, this is a request to reset,
        // otherwise it's a control update.

        if (data == 0) {
            reset();
        } else {
            // Mask the lower three bits to get the baud rate.
            int baudSelector = data & 0x0f;
            switch (baudSelector) {
                case 0:
                    baudRate = 0;
                    break;
                case 1:
                    baudRate = 50;
                    break;
                case 2:
                    baudRate = 75;
                    break;
                case 3:
                    baudRate = 110; // Real rate is actually 109.92
                    break;
                case 4:
                    baudRate = 135; // Real rate is actually 134.58
                    break;
                case 5:
                    baudRate = 150;
                    break;
                case 6:
                    baudRate = 300;
                    break;
                case 7:
                    baudRate = 600;
                    break;
                case 8:
                    baudRate = 1200;
                    break;
                case 9:
                    baudRate = 1800;
                    break;
                case 10:
                    baudRate = 2400;
                    break;
                case 11:
                    baudRate = 3600;
                    break;
                case 12:
                    baudRate = 4800;
                    break;
                case 13:
                    baudRate = 7200;
                    break;
                case 14:
                    baudRate = 9600;
                    break;
                case 15:
                    baudRate = 19200;
                    break;
            }

            // Recalculate the baud rate delay.
            baudRateDelay = calculateBaudRateDelay();
        }
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
    }

    /**
     * @return The contents of the status register.
     */
    public int statusReg() {
        // TODO: Overrun, Parity Error, Framing Error, DTR, DSR, and Interrupt flags.
        int stat = 0;
        if (rxFull && System.nanoTime() >= (lastRxRead + baudRateDelay)) {
            stat |= 0x08;
        }
        if (txEmpty && System.nanoTime() >= (lastTxWrite + baudRateDelay)) {
            stat |= 0x10;
        }
        return stat;
    }

    @Override
    public String toString() {
        return "ACIA@" + String.format("%04X", baseAddress);
    }

    public synchronized int rxRead() {
        lastRxRead = System.nanoTime();
        rxFull = false;
        return rxChar;
    }

    public synchronized void rxWrite(int data) {
        rxFull = true;

        if (receiveIrqEnabled) {
            getBus().assertIrq();
        }

        rxChar = data;
    }

    public synchronized int txRead() {
        txEmpty = true;

        if (transmitIrqEnabled) {
            getBus().assertIrq();
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
    public boolean hasRxChar() {
        return rxFull;
    }

    private synchronized void reset() {
        txChar = 0;
        txEmpty = true;
        rxChar = 0;
        rxFull = false;
        receiveIrqEnabled = false;
        transmitIrqEnabled = false;
    }

}
