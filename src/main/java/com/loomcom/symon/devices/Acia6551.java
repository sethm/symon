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
 * This is a simulation of the MOS 6551 ACIA, with limited
 * functionality.
 * <p/>
 * Unlike a 16550 UART, the 6551 ACIA has only one-byte transmit and
 * receive buffers. It is the programmer's responsibility to check the
 * status (full or empty) for transmit and receive buffers before
 * writing / reading.
 */
public class Acia6551 extends Acia {

    public static final int ACIA_SIZE = 4;

    static final int DATA_REG = 0;
    static final int STAT_REG = 1;
    static final int CMND_REG = 2;
    static final int CTRL_REG = 3;

    /**
     * Registers. These are ignored in the current implementation.
     */
    private int commandRegister;
    private int controlRegister;


    public Acia6551(int address) throws MemoryRangeException {
        super(address, ACIA_SIZE, "ACIA");

        // Figure 6 in the 6551 ACIA data sheet says the "hardware reset"
        // state of the Control Register is all zeros.
        setControlRegister(0b00000000);
        // Figure 7 of the 6551 ACIA data sheet says the "hardware reset"
        // state of the Command Register is zeros, but Transmitter Control
        // is set to "interrupt disabled, ready to send".
        setCommandRegister(0b00000010);
    }

    @Override
    public int read(int address, boolean cpuAccess) throws MemoryAccessException {
        switch (address) {
            case DATA_REG:
                return rxRead(cpuAccess);
            case STAT_REG:
                return statusReg(cpuAccess);
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
            case DATA_REG:
                txWrite(data);
                break;
            case STAT_REG:
                reset();
                break;
            case CMND_REG:
                setCommandRegister(data);
                break;
            case CTRL_REG:
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
     * @param data Data to write into the control register
     */
    private void setControlRegister(int data) {
        controlRegister = data;
        int rate = 0;

        // Mask the lower four bits to get the baud rate.
        int baudSelector = data & 0x0f;
        switch (baudSelector) {
            case 0:
                rate = 0;
                break;
            case 1:
                rate = 50;
                break;
            case 2:
                rate = 75;
                break;
            case 3:
                rate = 110; // Real rate is actually 109.92
                break;
            case 4:
                rate = 135; // Real rate is actually 134.58
                break;
            case 5:
                rate = 150;
                break;
            case 6:
                rate = 300;
                break;
            case 7:
                rate = 600;
                break;
            case 8:
                rate = 1200;
                break;
            case 9:
                rate = 1800;
                break;
            case 10:
                rate = 2400;
                break;
            case 11:
                rate = 3600;
                break;
            case 12:
                rate = 4800;
                break;
            case 13:
                rate = 7200;
                break;
            case 14:
                rate = 9600;
                break;
            case 15:
                rate = 19200;
                break;
        }

        setBaudRate(rate);
    }


    /**
     * @return The contents of the status register.
     */
    @Override
    public int statusReg(boolean cpuAccess) {
        // TODO: Parity Error, Framing Error, DTR, and DSR flags.
        int stat = 0;
        if (rxFull && System.nanoTime() >= (lastRxRead + baudRateDelay)) {
            stat |= 0x08;
        }
        if (txEmpty && System.nanoTime() >= (lastTxWrite + baudRateDelay)) {
            stat |= 0x10;
        }
        if (overrun) {
            stat |= 0x04;
        }
        if (interrupt) {
            stat |= 0x80;
        }

        if (cpuAccess) {
            interrupt = false;
        }

        return stat;
    }


    private synchronized void reset() {
        // Figure 6 in the 6551 ACIA data sheet says the "program reset"
        // event does not modify the control register.

        // Figure 7 in the 6551 ACIA data sheet says the "program reset"
        // event keeps the "parity check" configuration in the command
        // register, but resets the other bits to defaults.
        setCommandRegister((commandRegister & 0xe0) | 0x02);

        // Figure 8 in the 6551 ACIA data sheet says the "program reset"
        // event clears the "overrun" flag but otherwise has no effect.
        overrun = false;
    }
}
