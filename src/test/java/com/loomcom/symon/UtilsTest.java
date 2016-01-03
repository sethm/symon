package com.loomcom.symon;

import com.loomcom.symon.util.Utils;
import junit.framework.TestCase;

public class UtilsTest extends TestCase {
    public void testByteToHex() {
        assertEquals("FE", Utils.byteToHex(0xfe));
        assertEquals("00", Utils.byteToHex(0));
        assertEquals("0A", Utils.byteToHex(10));
    }

    public void testByteToHexIgnoresSign() {
        assertEquals("FF", Utils.byteToHex(-1));
    }

    public void testByteToHexMasksLowByte() {
        assertEquals("FE", Utils.byteToHex(0xfffe));
        assertEquals("00", Utils.byteToHex(0xff00));
    }

    public void testWordToHex() {
        assertEquals("0000", Utils.wordToHex(0));
        assertEquals("FFFF", Utils.wordToHex(65535));
        assertEquals("FFFE", Utils.wordToHex(65534));
    }

    public void testWordToHexIgnoresSign() {
        assertEquals("FFFF", Utils.wordToHex(-1));
    }

    public void testWordToHexMasksTwoLowBytes() {
        assertEquals("FFFE", Utils.wordToHex(0xfffffe));
        assertEquals("FF00", Utils.wordToHex(0xffff00));
    }

    public void testAllBytesAreCorrect() {
        for (int i = 0; i <= 0xff; i++) {
            assertEquals(String.format("%02X", i), Utils.byteToHex(i));
        }
    }
}
