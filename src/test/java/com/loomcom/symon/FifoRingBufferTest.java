package com.loomcom.symon;

import com.loomcom.symon.util.FifoRingBuffer;
import junit.framework.TestCase;
import java.lang.Character;

/**
 * Created with IntelliJ IDEA.
 * User: seth
 * Date: 11/22/12
 * Time: 11:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class FifoRingBufferTest extends TestCase {
    public void testRingBufferShouldDeleteOldestItemIfAtMaximum() {
        FifoRingBuffer<Character> buffer = new FifoRingBuffer<Character>(3);

        assertEquals(0, buffer.length());
        buffer.push('a');
        assertEquals(1, buffer.length());
        assertTrue('a' == buffer.peek());

        buffer.push('b');
        assertEquals(2, buffer.length());
        assertTrue('a' == buffer.peek());

        buffer.push('c');
        assertEquals(3, buffer.length());
        assertTrue('a' == buffer.peek());

        buffer.push('d');
        assertEquals(3, buffer.length());
        assertTrue('b' == buffer.peek());

        buffer.push('e');
        assertEquals(3, buffer.length());
        assertTrue('c' == buffer.peek());
    }
}
