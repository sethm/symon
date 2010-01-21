package com.loomcom.symon.devices;

import com.loomcom.symon.exceptions.*;
import com.loomcom.symon.util.*;

/**
 * This is a simulation of the MOS 6551 ACIA, with limited
 * functionality.  Interrupts are not supported.
 *
 * Unlike a 16550 UART, the 6551 ACIA has only one-byte transmit and
 * receive buffers. It is the programmer's responsibility to check the
 * status (full or empty) for transmit and receive buffers before
 * writing / reading.  However, in the simulation we maintain two
 * small buffers of 256 characters, since we only wake up to check for
 * keyboard input and do output every 500 instructions.
 */
public class Acia extends Device {

  public static final int ACIA_SIZE = 4;
  public static final int BUF_LEN = 256;

  static final int DATA_REG = 0;
  static final int STAT_REG = 1;
  static final int CMND_REG = 2;
  static final int CTRL_REG = 3;

  /** Register addresses */
  private int baseAddress;

  /** Registers. These are ignored in the current implementation. */
  private int commandRegister;
  private int controlRegister;

  /** Read/Write buffers */
  private FifoRingBuffer rxBuffer = new FifoRingBuffer(BUF_LEN);
  private FifoRingBuffer txBuffer = new FifoRingBuffer(BUF_LEN);

  public Acia(int address) throws MemoryRangeException {
    super(address, ACIA_SIZE, "ACIA");
    this.baseAddress = address;
  }

  @Override
  public int read(int address) throws MemoryAccessException {
    switch (address) {
    case DATA_REG:
      try {
        return rxRead();
      } catch (FifoUnderrunException ex) {
        throw new MemoryAccessException("Buffer underrun");
      }
    case STAT_REG:
      return ((rxBuffer.isEmpty() ? 0x00 : 0x08) |
              (txBuffer.isEmpty() ? 0x10 : 0x00));
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

  public int rxRead() throws FifoUnderrunException {
    return rxBuffer.pop();
  }

  public void rxWrite(int data) {
    rxBuffer.push(data);
  }

  public int txRead() throws FifoUnderrunException {
    return txBuffer.pop();
  }

  public void txWrite(int data) {
    txBuffer.push(data);
  }

  /**
   * @return true if there is character data in the TX register.
   */
  public boolean hasTxChar() {
    return !txBuffer.isEmpty();
  }

  /**
   * @return true if there is character data in the RX register.
   */
  public boolean hasRxChar() {
    return !rxBuffer.isEmpty();
  }

  private void reset() {
    txBuffer.reset();
    rxBuffer.reset();
  }

}
