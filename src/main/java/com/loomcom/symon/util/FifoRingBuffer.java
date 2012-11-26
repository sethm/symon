package com.loomcom.symon.util;

import com.loomcom.symon.exceptions.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A FIFO buffer with a bounded maximum size.
 */
public class FifoRingBuffer<E> implements Iterable<E> {

    private Queue<E> fifoBuffer;
    private int maxLength;

    public FifoRingBuffer(int maxLength) {
        this.fifoBuffer = new LinkedList<E>();
        this.maxLength = maxLength;
    }

    public E pop() throws FifoUnderrunException {
        return fifoBuffer.remove();
    }

    public boolean isEmpty() {
        return fifoBuffer.isEmpty();
    }

    public void push(E val) {
        if (fifoBuffer.size() == maxLength) {
            // Delete the oldest element.
            fifoBuffer.remove();
        }
        fifoBuffer.offer(val);
    }

    public E peek() {
        return fifoBuffer.peek();
    }

    public void reset() {
        fifoBuffer.clear();
    }

    public int length() {
        return fifoBuffer.size();
    }

    public String toString() {
        return "[FifoRingBuffer: size=" + fifoBuffer.size() + "]";
    }

    public Iterator<E> iterator() {
        return fifoBuffer.iterator();
    }
}

