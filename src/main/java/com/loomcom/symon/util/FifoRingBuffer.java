package com.loomcom.symon.util;

import com.loomcom.symon.exceptions.*;

/**
 * A very simple and efficient FIFO ring buffer implementation backed
 * by an array. It can only hold only integers.
 */
public class FifoRingBuffer {

  private int[] fifoBuffer;
  private int readPtr = 0;
  private int writePtr = 0;
  private int size = 0;

  public FifoRingBuffer(int size) {
    if (size <= 0) {
      throw new RuntimeException("Cannot create a FifoRingBuffer with size <= 0.");
    }
    this.size = size;
    fifoBuffer = new int[size];
  }

  public int peek() throws FifoUnderrunException {
    if (isEmpty()) {
      throw new FifoUnderrunException("Buffer Underrun");
    }
    return fifoBuffer[readPtr];
  }

  public int pop() throws FifoUnderrunException {
    if (isEmpty()) {
      throw new FifoUnderrunException("Buffer Underrun");
    }
    int val = fifoBuffer[readPtr];
    incrementReadPointer();
    return val;
  }

  public boolean isEmpty() {
    return(readPtr == writePtr);
  }

  public boolean isFull() {
    return((readPtr == 0 && writePtr == (size - 1)) ||
           writePtr == (readPtr - 1));
  }

  public void push(int val) {
    fifoBuffer[writePtr] = val;
    incrementWritePointer();
  }

  public void reset() {
    readPtr = 0;
    writePtr = 0;
  }

  private void incrementWritePointer() {
    if (++writePtr == size) {
      writePtr = 0;
    }
    if (writePtr == readPtr) {
      incrementReadPointer();
    }
  }

  private void incrementReadPointer() {
    if (++readPtr == size) {
      readPtr = 0;
    }
  }

  public String toString() {
    return "[FifoRingBuffer: size=" + size + "]";
  }
}

