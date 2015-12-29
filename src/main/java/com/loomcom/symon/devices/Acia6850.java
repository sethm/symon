/*
 * Copyright (c) 2016 Seth J. Morabito <web@loomcom.com>
 *                    Maik Merten <maikmerten@googlemail.com>
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
 * This is a simulation of the Motorola 6850 ACIA, with limited
 * functionality.  Interrupts are not supported.
 * <p/>
 * Unlike a 16550 UART, the 6850 ACIA has only one-byte transmit and
 * receive buffers. It is the programmer's responsibility to check the
 * status (full or empty) for transmit and receive buffers before
 * writing / reading.
 */
public class Acia6850 extends Acia {

    public static final int ACIA_SIZE = 2;


    static final int STAT_REG = 0;	// read-only
    static final int CTRL_REG = 0;	// write-only
	
    static final int RX_REG = 1;	// read-only
    static final int TX_REG = 1;	// write-only

    
    /**
     * Registers. These are ignored in the current implementation.
     */
    private int commandRegister;


    public Acia6850(int address) throws MemoryRangeException {
        super(address, ACIA_SIZE, "ACIA6850");
        setBaudRate(2400);
    }

    @Override
    public int read(int address) throws MemoryAccessException {
        switch (address) {
            case RX_REG:
                return rxRead();
            case STAT_REG:
                return statusReg();

            default:
                throw new MemoryAccessException("No register.");
        }
    }

    @Override
    public void write(int address, int data) throws MemoryAccessException {
        switch (address) {
            case TX_REG:
                txWrite(data);
                break;
            case CTRL_REG:
                setCommandRegister(data);
                break;
            default:
                throw new MemoryAccessException("No register.");
        }
    }
    
    private void setCommandRegister(int data) {
        commandRegister = data;
		
        // Bits 0 & 1 control the master reset
        if((commandRegister & 0x01) != 0 && (commandRegister & 0x02) != 0) {
            reset();
        }

        // Bit 7 controls receiver IRQ behavior
        receiveIrqEnabled = (commandRegister & 0x80) != 0;
        // Bits 5 & 6 controls transmit IRQ behavior
        transmitIrqEnabled = (commandRegister & 0x20) != 0 && (commandRegister & 0x40) == 0;
    }



    /**
     * @return The contents of the status register.
     */
    @Override
    public int statusReg() {
        // TODO: Parity Error, Framing Error, DTR, DSR, and Interrupt flags.
        int stat = 0;
        if (rxFull && System.nanoTime() >= (lastRxRead + baudRateDelay)) {
            stat |= 0x01;
        }
        if (txEmpty && System.nanoTime() >= (lastTxWrite + baudRateDelay)) {
            stat |= 0x02;
        }
        if (overrun) {
            stat |= 0x20;
        }
		
        return stat;
    }


    private synchronized void reset() {
        overrun = false;
        rxFull = false;
        txEmpty = true;
    }

}
