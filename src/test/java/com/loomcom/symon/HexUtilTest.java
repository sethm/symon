package com.loomcom.symon;

import com.loomcom.symon.util.HexUtil;
import junit.framework.TestCase;

public class HexUtilTest extends TestCase {
    public void testByteToHex() {
        assertEquals("FE", HexUtil.byteToHex(0xfe));
        assertEquals("00", HexUtil.byteToHex(0));
        assertEquals("0A", HexUtil.byteToHex(10));
    }

    public void testByteToHexIgnoresSign() {
        assertEquals("FF", HexUtil.byteToHex(-1));
    }

    public void testByteToHexMasksLowByte() {
        assertEquals("FE", HexUtil.byteToHex(0xfffe));
        assertEquals("00", HexUtil.byteToHex(0xff00));
    }

    public void testWordToHex() {
        assertEquals("0000", HexUtil.wordToHex(0));
        assertEquals("FFFF", HexUtil.wordToHex(65535));
        assertEquals("FFFE", HexUtil.wordToHex(65534));
    }

    public void testWordToHexIgnoresSign() {
        assertEquals("FFFF", HexUtil.wordToHex(-1));
    }

    public void testWordToHexMasksTwoLowBytes() {
        assertEquals("FFFE", HexUtil.wordToHex(0xfffffe));
        assertEquals("FF00", HexUtil.wordToHex(0xffff00));
    }

    public void testAllBytesAreCorrect() {
        for (int i = 0; i <= 0xff; i++) {
            assertEquals(String.format("%02X", i), HexUtil.byteToHex(i));
        }
    }
}
