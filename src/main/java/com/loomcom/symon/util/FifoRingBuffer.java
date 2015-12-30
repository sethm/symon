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
        this.fifoBuffer = new LinkedList<>();
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

