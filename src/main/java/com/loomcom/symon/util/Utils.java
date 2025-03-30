/*
 * Copyright (c) 2008-2025 Seth J. Morabito <web@loomcom.com>
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

/**
 * Various Utilities
 */
public class Utils {

    static final String NON_PRINTABLE = ".";

    static final String[] ASCII_CONSTANTS = {" ", "!", "\"", "#", "$", "%", "&", "'",
                                             "(", ")", "*", "+", ",", "-", ".", "/",
                                             "0", "1", "2", "3", "4", "5", "6", "7",
                                             "8", "9", ":", ";", "<", "=", ">", "?",
                                             "@", "A", "B", "C", "D", "E", "F", "G",
                                             "H", "I", "J", "K", "L", "M", "N", "O",
                                             "P", "Q", "R", "S", "T", "U", "V", "W",
                                             "X", "Y", "Z", "[", "\\", "]", "^", "_",
                                             "`", "a", "b", "c", "d", "e", "f", "g",
                                             "h", "i", "j", "k", "l", "m", "n", "o",
                                             "p", "q", "r", "s", "t", "u", "v", "w",
                                             "x", "y", "z", "{", "|", "}", "~"};

    static final String[] HEX_CONSTANTS = {"00", "01", "02", "03", "04", "05", "06", "07",
                                           "08", "09", "0A", "0B", "0C", "0D", "0E", "0F",
                                           "10", "11", "12", "13", "14", "15", "16", "17",
                                           "18", "19", "1A", "1B", "1C", "1D", "1E", "1F",
                                           "20", "21", "22", "23", "24", "25", "26", "27",
                                           "28", "29", "2A", "2B", "2C", "2D", "2E", "2F",
                                           "30", "31", "32", "33", "34", "35", "36", "37",
                                           "38", "39", "3A", "3B", "3C", "3D", "3E", "3F",
                                           "40", "41", "42", "43", "44", "45", "46", "47",
                                           "48", "49", "4A", "4B", "4C", "4D", "4E", "4F",
                                           "50", "51", "52", "53", "54", "55", "56", "57",
                                           "58", "59", "5A", "5B", "5C", "5D", "5E", "5F",
                                           "60", "61", "62", "63", "64", "65", "66", "67",
                                           "68", "69", "6A", "6B", "6C", "6D", "6E", "6F",
                                           "70", "71", "72", "73", "74", "75", "76", "77",
                                           "78", "79", "7A", "7B", "7C", "7D", "7E", "7F",
                                           "80", "81", "82", "83", "84", "85", "86", "87",
                                           "88", "89", "8A", "8B", "8C", "8D", "8E", "8F",
                                           "90", "91", "92", "93", "94", "95", "96", "97",
                                           "98", "99", "9A", "9B", "9C", "9D", "9E", "9F",
                                           "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7",
                                           "A8", "A9", "AA", "AB", "AC", "AD", "AE", "AF",
                                           "B0", "B1", "B2", "B3", "B4", "B5", "B6", "B7",
                                           "B8", "B9", "BA", "BB", "BC", "BD", "BE", "BF",
                                           "C0", "C1", "C2", "C3", "C4", "C5", "C6", "C7",
                                           "C8", "C9", "CA", "CB", "CC", "CD", "CE", "CF",
                                           "D0", "D1", "D2", "D3", "D4", "D5", "D6", "D7",
                                           "D8", "D9", "DA", "DB", "DC", "DD", "DE", "DF",
                                           "E0", "E1", "E2", "E3", "E4", "E5", "E6", "E7",
                                           "E8", "E9", "EA", "EB", "EC", "ED", "EE", "EF",
                                           "F0", "F1", "F2", "F3", "F4", "F5", "F6", "F7",
                                           "F8", "F9", "FA", "FB", "FC", "FD", "FE", "FF"};

    /**
     * Very fast 8-bit int to ASCII conversion.
     *
     * @param val The value of an ASCII character.
     * @return A string representing the ASCII character.
     */
    public static String byteToAscii(int val) {
         if (val >= 32 && val <= 126) {
             return ASCII_CONSTANTS[val - 32];
         } else {
             return NON_PRINTABLE;
         }
    }

    /**
     * Very fast 8-bit int to hex conversion, with zero-padded output.
     *
     * @param val The unsigned 8-bit value to convert to a zero padded hexadecimal string.
     * @return Two digit, zero padded hexadecimal string.
     */
    public static String byteToHex(int val) {
        return HEX_CONSTANTS[val & 0xff];
    }

    /**
     * Very fast 16-bit int to hex conversion, with zero-padded output.
     *
     * @param val The unsigned 16-bit value to convert to a zero padded hexadecimal string.
     * @return Four digit, zero padded hexadecimal string.
     */
    public static String wordToHex(int val) {
        return HEX_CONSTANTS[(val >> 8) & 0xff] + HEX_CONSTANTS[val & 0xff];
    }

    /**
     * Given two bytes, return an address.
     */
    public static int address(int lowByte, int hiByte) {
        return ((hiByte << 8) | lowByte) & 0xffff;
    }
}