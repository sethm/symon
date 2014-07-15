/*
 * Copyright (c) 2008-2013 Seth J. Morabito <sethm@loomcom.com>
 *                         Maik Merten <maikmerten@googlemail.com>
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
public class Acia6850 extends Device {

    public static final int ACIA_SIZE = 2;


	static final int STAT_REG = 0;	// read-only
	static final int CTRL_REG = 0;	// write-only
	
    static final int RX_REG = 1;	// read-only
    static final int TX_REG = 1;	// write-only


    /**
     * Register addresses
     */
    private int baseAddress;

    /**
     * Registers. These are ignored in the current implementation.
     */
    private int commandRegister;

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

    public Acia6850(int address) throws MemoryRangeException {
        super(address, address + ACIA_SIZE - 1, "ACIA");
        this.baseAddress = address;
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
        receiveIrqEnabled = (commandRegister & 0x80) == 0;
        // Bits 5 & 6 controls transmit IRQ behavior
        transmitIrqEnabled = (commandRegister & 0x20) == 0 && (commandRegister & 0x40) != 0;
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
		this.baudRateDelay = calculateBaudRateDelay();
    }

    /**
     * @return The contents of the status register.
     */
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

    @Override
    public String toString() {
        return "ACIA6850@" + String.format("%04X", baseAddress);
    }

    public synchronized int rxRead() {
        lastRxRead = System.nanoTime();
        rxFull = false;
		overrun = false;
        return rxChar;
    }

    public synchronized void rxWrite(int data) {
		// when receiving while full: overrun
		if(rxFull) {
			overrun = true;
		}
		
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
		overrun = false;
    }

}
