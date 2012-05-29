package com.loomcom.symon.devices;

import com.loomcom.symon.exceptions.*;


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

    /**
     * Read/Write buffers
     */
    private int rxChar = 0;
    private int txChar = 0;

    private boolean rxFull = false;
    private boolean txFull = false;

    public Acia(int address) throws MemoryRangeException {
        super(address, ACIA_SIZE, "ACIA");
        this.baseAddress = address;
    }

    @Override
    public int read(int address) throws MemoryAccessException {
        switch (address) {
            case DATA_REG:
                return rxRead();
            case STAT_REG:
                return ((rxFull ? 0x08 : 0x00) |
                        (txFull ? 0x00 : 0x10));
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
                commandRegister = data;
                break;
            case 3:
                controlRegister = data;
                break;
            default:
                throw new MemoryAccessException("No register.");
        }
    }

    @Override
    public String toString() {
        return "ACIA@" + String.format("%04X", baseAddress);
    }

    public synchronized int rxRead() {
        rxFull = false;
        return rxChar;
    }

    public synchronized void rxWrite(int data) {
        rxFull = true;
        rxChar = data;
    }

    public synchronized int txRead() {
        txFull = false;
        return txChar;
    }

    public synchronized void txWrite(int data) {
        txFull = true;
        txChar = data;
    }

    /**
     * @return true if there is character data in the TX register.
     */
    public boolean hasTxChar() {
        return txFull;
    }

    /**
     * @return true if there is character data in the RX register.
     */
    public boolean hasRxChar() {
        return rxFull;
    }

    private synchronized void reset() {
        txChar = 0;
        txFull = false;
        rxChar = 0;
        rxFull = false;
    }

}
