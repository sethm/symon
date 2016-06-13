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

package com.loomcom.symon;

public interface InstructionTable {

    /**
     * Enumeration of valid CPU behaviors. These determine what behavior and instruction
     * set will be simulated, depending on desired version of 6502.
     *
     * TODO: As of version 0.6, this is still not used! All CPUs are "idealized" NMOS 6502 only.
     */
    enum CpuBehavior {
        /**
         * The earliest NMOS 6502 includes a bug that causes the ROR instruction
         * to behave like an ASL that does not affect the carry bit. This version
         * is very rare in the wild.
         *
         * NB: Does NOT implement "unimplemented" NMOS instructions.
         */
        NMOS_WITH_ROR_BUG,

        /**
         * All NMOS 6502's have a bug with the indirect JMP instruction. If the
         *
         * NB: Does NOT implement "unimplemented" NMOS instructions.
         */
        NMOS_6502,

        /**
         * Emulate a CMOS 65C02, with all CMOS instructions and addressing modes.
         */
        CMOS_6502,

        /**
         * Emulate a CMOS 65C816.
         */
        CMOS_65816
    }

    /**
     * Enumeration of Addressing Modes.
     */
    enum Mode {
        ACC {
            public String toString() {
                return "Accumulator";
            }
        },

        ABS {
            public String toString() {
                return "Absolute";
            }
        },

        ABX {
            public String toString() {
                return "Absolute, X-indexed";
            }
        },

        ABY {
            public String toString() {
                return "Absolute, Y-indexed";
            }
        },

        IMM {
            public String toString() {
                return "Immediate";
            }
        },

        IMP {
            public String toString() {
                return "Implied";
            }
        },

        IND {
            public String toString() {
                return "Indirect";
            }
        },

        XIN {
            public String toString() {
                return "X-indexed Indirect";
            }
        },

        INY {
            public String toString() {
                return "Indirect, Y-indexed";
            }
        },

        REL {
            public String toString() {
                return "Relative";
            }
        },

        ZPG {
            public String toString() {
                return "Zero Page";
            }
        },

        ZPX {
            public String toString() {
                return "Zero Page, X-indexed";
            }
        },

        ZPY {
            public String toString() {
                return "Zero Page, Y-indexed";
            }
        },

        ZPI {
            public String toString() {
                return "Zero Page Indirect";
            }
        },

        NUL {
            public String toString() {
                return "NULL";
            }
        }
    }

    // 6502 opcodes.  No 65C02 opcodes implemented.

    /**
     * Instruction opcode names. This lists all opcodes for
     * NMOS 6502, CMOS 65C02, and CMOS 65C816
     */
    String[] opcodeNames = {
        "BRK", "ORA",  null,  null,  null, "ORA", "ASL",  null,  // 0x00-0x07
        "PHP", "ORA", "ASL",  null,  null, "ORA", "ASL",  null,  // 0x08-0x0f
        "BPL", "ORA", "ORA",  null,  null, "ORA", "ASL",  null,  // 0x10-0x17
        "CLC", "ORA",  null,  null,  null, "ORA", "ASL",  null,  // 0x18-0x1f
        "JSR", "AND",  null,  null, "BIT", "AND", "ROL",  null,  // 0x20-0x27
        "PLP", "AND", "ROL",  null, "BIT", "AND", "ROL",  null,  // 0x28-0x2f
        "BMI", "AND", "AND",  null,  null, "AND", "ROL",  null,  // 0x30-0x37
        "SEC", "AND",  null,  null,  null, "AND", "ROL",  null,  // 0x38-0x3f
        "RTI", "EOR",  null,  null,  null, "EOR", "LSR",  null,  // 0x40-0x47
        "PHA", "EOR", "LSR",  null, "JMP", "EOR", "LSR",  null,  // 0x48-0x4f
        "BVC", "EOR", "EOR",  null,  null, "EOR", "LSR",  null,  // 0x50-0x57
        "CLI", "EOR",  null,  null,  null, "EOR", "LSR",  null,  // 0x58-0x5f
        "RTS", "ADC",  null,  null,  null, "ADC", "ROR",  null,  // 0x60-0x67
        "PLA", "ADC", "ROR",  null, "JMP", "ADC", "ROR",  null,  // 0x68-0x6f
        "BVS", "ADC", "ADC",  null,  null, "ADC", "ROR",  null,  // 0x70-0x77
        "SEI", "ADC",  null,  null, "JMP", "ADC", "ROR",  null,  // 0x78-0x7f
        "BCS", "STA",  null,  null, "STY", "STA", "STX",  null,  // 0x80-0x87
        "DEY",  null, "TXA",  null, "STY", "STA", "STX",  null,  // 0x88-0x8f
        "BCC", "STA", "STA",  null, "STY", "STA", "STX",  null,  // 0x90-0x97
        "TYA", "STA", "TXS",  null,  null, "STA",  null,  null,  // 0x98-0x9f
        "LDY", "LDA", "LDX",  null, "LDY", "LDA", "LDX",  null,  // 0xa0-0xa7
        "TAY", "LDA", "TAX",  null, "LDY", "LDA", "LDX",  null,  // 0xa8-0xaf
        "BCS", "LDA", "LDA",  null, "LDY", "LDA", "LDX",  null,  // 0xb0-0xb7
        "CLV", "LDA", "TSX",  null, "LDY", "LDA", "LDX",  null,  // 0xb8-0xbf
        "CPY", "CMP",  null,  null, "CPY", "CMP", "DEC",  null,  // 0xc0-0xc7
        "INY", "CMP", "DEX",  null, "CPY", "CMP", "DEC",  null,  // 0xc8-0xcf
        "BNE", "CMP", "CMP",  null,  null, "CMP", "DEC",  null,  // 0xd0-0xd7
        "CLD", "CMP",  null,  null,  null, "CMP", "DEC",  null,  // 0xd8-0xdf
        "CPX", "SBC",  null,  null, "CPX", "SBC", "INC",  null,  // 0xe0-0xe7
        "INX", "SBC", "NOP",  null, "CPX", "SBC", "INC",  null,  // 0xe8-0xef
        "BEQ", "SBC", "SBC",  null,  null, "SBC", "INC",  null,  // 0xf0-0xf7
        "SED", "SBC",  null,  null,  null, "SBC", "INC",  null   // 0xf8-0xff
    };

    /**
     * Instruction addressing modes. This table includes sizes
     * for all instructions for NMOS 6502, CMOS 65C02,
     * and CMOS 65C816
     */
    Mode[] instructionModes = {
        Mode.IMP, Mode.XIN, Mode.NUL, Mode.NUL,   // 0x00-0x03
        Mode.NUL, Mode.ZPG, Mode.ZPG, Mode.NUL,   // 0x04-0x07
        Mode.IMP, Mode.IMM, Mode.ACC, Mode.NUL,   // 0x08-0x0b
        Mode.NUL, Mode.ABS, Mode.ABS, Mode.NUL,   // 0x0c-0x0f
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0x10-0x13
        Mode.NUL, Mode.ZPX, Mode.ZPX, Mode.NUL,   // 0x14-0x17
        Mode.IMP, Mode.ABY, Mode.NUL, Mode.NUL,   // 0x18-0x1b
        Mode.NUL, Mode.ABX, Mode.ABX, Mode.NUL,   // 0x1c-0x1f
        Mode.ABS, Mode.XIN, Mode.NUL, Mode.NUL,   // 0x20-0x23
        Mode.ZPG, Mode.ZPG, Mode.ZPG, Mode.NUL,   // 0x24-0x27
        Mode.IMP, Mode.IMM, Mode.ACC, Mode.NUL,   // 0x28-0x2b
        Mode.ABS, Mode.ABS, Mode.ABS, Mode.NUL,   // 0x2c-0x2f
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0x30-0x33
        Mode.NUL, Mode.ZPX, Mode.ZPX, Mode.NUL,   // 0x34-0x37
        Mode.IMP, Mode.ABY, Mode.NUL, Mode.NUL,   // 0x38-0x3b
        Mode.NUL, Mode.ABX, Mode.ABX, Mode.NUL,   // 0x3c-0x3f
        Mode.IMP, Mode.XIN, Mode.NUL, Mode.NUL,   // 0x40-0x43
        Mode.NUL, Mode.ZPG, Mode.ZPG, Mode.NUL,   // 0x44-0x47
        Mode.IMP, Mode.IMM, Mode.ACC, Mode.NUL,   // 0x48-0x4b
        Mode.ABS, Mode.ABS, Mode.ABS, Mode.NUL,   // 0x4c-0x4f
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0x50-0x53
        Mode.NUL, Mode.ZPX, Mode.ZPX, Mode.NUL,   // 0x54-0x57
        Mode.IMP, Mode.ABY, Mode.NUL, Mode.NUL,   // 0x58-0x5b
        Mode.NUL, Mode.ABX, Mode.ABX, Mode.NUL,   // 0x5c-0x5f
        Mode.IMP, Mode.XIN, Mode.NUL, Mode.NUL,   // 0x60-0x63
        Mode.NUL, Mode.ZPG, Mode.ZPG, Mode.NUL,   // 0x64-0x67
        Mode.IMP, Mode.IMM, Mode.ACC, Mode.NUL,   // 0x68-0x6b
        Mode.IND, Mode.ABS, Mode.ABS, Mode.NUL,   // 0x6c-0x6f
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0x70-0x73
        Mode.NUL, Mode.ZPX, Mode.ZPX, Mode.NUL,   // 0x74-0x77
        Mode.IMP, Mode.ABY, Mode.NUL, Mode.NUL,   // 0x78-0x7b
        Mode.ABX, Mode.ABX, Mode.ABX, Mode.NUL,   // 0x7c-0x7f
        Mode.REL, Mode.XIN, Mode.NUL, Mode.NUL,   // 0x80-0x83
        Mode.ZPG, Mode.ZPG, Mode.ZPG, Mode.NUL,   // 0x84-0x87
        Mode.IMP, Mode.NUL, Mode.IMP, Mode.NUL,   // 0x88-0x8b
        Mode.ABS, Mode.ABS, Mode.ABS, Mode.NUL,   // 0x8c-0x8f
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0x90-0x93
        Mode.ZPX, Mode.ZPX, Mode.ZPY, Mode.NUL,   // 0x94-0x97
        Mode.IMP, Mode.ABY, Mode.IMP, Mode.NUL,   // 0x98-0x9b
        Mode.NUL, Mode.ABX, Mode.NUL, Mode.NUL,   // 0x9c-0x9f
        Mode.IMM, Mode.XIN, Mode.IMM, Mode.NUL,   // 0xa0-0xa3
        Mode.ZPG, Mode.ZPG, Mode.ZPG, Mode.NUL,   // 0xa4-0xa7
        Mode.IMP, Mode.IMM, Mode.IMP, Mode.NUL,   // 0xa8-0xab
        Mode.ABS, Mode.ABS, Mode.ABS, Mode.NUL,   // 0xac-0xaf
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0xb0-0xb3
        Mode.ZPX, Mode.ZPX, Mode.ZPY, Mode.NUL,   // 0xb4-0xb7
        Mode.IMP, Mode.ABY, Mode.IMP, Mode.NUL,   // 0xb8-0xbb
        Mode.ABX, Mode.ABX, Mode.ABY, Mode.NUL,   // 0xbc-0xbf
        Mode.IMM, Mode.XIN, Mode.NUL, Mode.NUL,   // 0xc0-0xc3
        Mode.ZPG, Mode.ZPG, Mode.ZPG, Mode.NUL,   // 0xc4-0xc7
        Mode.IMP, Mode.IMM, Mode.IMP, Mode.NUL,   // 0xc8-0xcb
        Mode.ABS, Mode.ABS, Mode.ABS, Mode.NUL,   // 0xcc-0xcf
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0xd0-0xd3
        Mode.NUL, Mode.ZPX, Mode.ZPX, Mode.NUL,   // 0xd4-0xd7
        Mode.IMP, Mode.ABY, Mode.NUL, Mode.NUL,   // 0xd8-0xdb
        Mode.NUL, Mode.ABX, Mode.ABX, Mode.NUL,   // 0xdc-0xdf
        Mode.IMM, Mode.XIN, Mode.NUL, Mode.NUL,   // 0xe0-0xe3
        Mode.ZPG, Mode.ZPG, Mode.ZPG, Mode.NUL,   // 0xe4-0xe7
        Mode.IMP, Mode.IMM, Mode.IMP, Mode.NUL,   // 0xe8-0xeb
        Mode.ABS, Mode.ABS, Mode.ABS, Mode.NUL,   // 0xec-0xef
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0xf0-0xf3
        Mode.NUL, Mode.ZPX, Mode.ZPX, Mode.NUL,   // 0xf4-0xf7
        Mode.IMP, Mode.ABY, Mode.NUL, Mode.NUL,   // 0xf8-0xfb
        Mode.NUL, Mode.ABX, Mode.ABX, Mode.NUL    // 0xfc-0xff
    };


    /**
     * Size, in bytes, required for each instruction. This table
     * includes sizes for all instructions for NMOS 6502, CMOS 65C02,
     * and CMOS 65C816
     */
    int[] instructionSizes = {
        1, 2, 0, 0, 0, 2, 2, 0, 1, 2, 1, 0, 0, 3, 3, 0,   // 0x00-0x0f
        2, 2, 2, 0, 0, 2, 2, 0, 1, 3, 0, 0, 0, 3, 3, 0,   // 0x10-0x1f
        3, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0,   // 0x20-0x2f
        2, 2, 2, 0, 0, 2, 2, 0, 1, 3, 0, 0, 0, 3, 3, 0,   // 0x30-0x3f
        1, 2, 0, 0, 0, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0,   // 0x40-0x4f
        2, 2, 2, 0, 0, 2, 2, 0, 1, 3, 0, 0, 0, 3, 3, 0,   // 0x50-0x5f
        1, 2, 0, 0, 0, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0,   // 0x60-0x6f
        2, 2, 2, 0, 0, 2, 2, 0, 1, 3, 0, 0, 3, 3, 3, 0,   // 0x70-0x7f
        2, 2, 0, 0, 2, 2, 2, 0, 1, 0, 1, 0, 3, 3, 3, 0,   // 0x80-0x8f
        2, 2, 2, 0, 2, 2, 2, 0, 1, 3, 1, 0, 0, 3, 0, 0,   // 0x90-0x9f
        2, 2, 2, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0,   // 0xa0-0xaf
        2, 2, 2, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0,   // 0xb0-0xbf
        2, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0,   // 0xc0-0xcf
        2, 2, 2, 0, 0, 2, 2, 0, 1, 3, 0, 0, 0, 3, 3, 0,   // 0xd0-0xdf
        2, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0,   // 0xe0-0xef
        2, 2, 2, 0, 0, 2, 2, 0, 1, 3, 0, 0, 0, 3, 3, 0    // 0xf0-0xff
    };

    /**
     * Number of clock cycles required for each instruction when
     * in NMOS mode.
     */
    int[] instructionClocksNmos = {
        7, 6, 0, 0, 0, 3, 5, 0, 3, 2, 2, 0, 0, 4, 6, 0,   // 0x00-0x0f
        2, 5, 0, 0, 0, 4, 6, 0, 2, 4, 0, 0, 0, 4, 7, 0,   // 0x10-0x1f
        6, 6, 0, 0, 3, 3, 5, 0, 4, 2, 2, 0, 4, 4, 6, 0,   // 0x20-0x2f
        2, 5, 0, 0, 0, 4, 6, 0, 2, 4, 0, 0, 0, 4, 7, 0,   // 0x30-0x3f
        6, 6, 0, 0, 0, 3, 5, 0, 3, 2, 2, 0, 3, 4, 6, 0,   // 0x40-0x4f
        2, 5, 0, 0, 0, 4, 6, 0, 2, 4, 0, 0, 0, 4, 7, 0,   // 0x50-0x5f
        6, 6, 0, 0, 0, 3, 5, 0, 4, 2, 2, 0, 5, 4, 6, 0,   // 0x60-0x6f
        2, 5, 0, 0, 0, 4, 6, 0, 2, 4, 0, 0, 0, 4, 7, 0,   // 0x70-0x7f
        2, 6, 0, 0, 3, 3, 3, 0, 2, 0, 2, 0, 4, 4, 4, 0,   // 0x80-0x8f
        2, 6, 0, 0, 4, 4, 4, 0, 2, 5, 2, 0, 0, 5, 0, 0,   // 0x90-0x9f
        2, 6, 2, 0, 3, 3, 3, 0, 2, 2, 2, 0, 4, 4, 4, 0,   // 0xa0-0xaf
        2, 5, 0, 0, 4, 4, 4, 0, 2, 4, 2, 0, 4, 4, 4, 0,   // 0xb0-0xbf
        2, 6, 0, 0, 3, 3, 5, 0, 2, 2, 2, 0, 4, 4, 6, 0,   // 0xc0-0xcf
        2, 5, 0, 0, 0, 4, 6, 0, 2, 4, 0, 0, 0, 4, 7, 0,   // 0xd0-0xdf
        2, 6, 0, 0, 3, 3, 5, 0, 2, 2, 2, 0, 4, 4, 6, 0,   // 0xe0-0xef
        2, 5, 0, 0, 0, 4, 6, 0, 2, 4, 0, 0, 0, 4, 7, 0    // 0xf0-0xff
    };

    /**
     * Number of clock cycles required for each instruction when
     * in CMOS mode
     */
    int[] instructionClocksCmos = {
        7, 6, 0, 0, 0, 3, 5, 0, 3, 2, 2, 0, 0, 4, 6, 0,   // 0x00-0x0f
        2, 5, 5, 0, 0, 4, 6, 0, 2, 4, 0, 0, 0, 4, 7, 0,   // 0x10-0x1f
        6, 6, 0, 0, 3, 3, 5, 0, 4, 2, 2, 0, 4, 4, 6, 0,   // 0x20-0x2f
        2, 5, 5, 0, 0, 4, 6, 0, 2, 4, 0, 0, 0, 4, 7, 0,   // 0x30-0x3f
        6, 6, 0, 0, 0, 3, 5, 0, 3, 2, 2, 0, 3, 4, 6, 0,   // 0x40-0x4f
        2, 5, 5, 0, 0, 4, 6, 0, 2, 4, 0, 0, 0, 4, 7, 0,   // 0x50-0x5f
        6, 6, 0, 0, 0, 3, 5, 0, 4, 2, 2, 0, 5, 4, 6, 0,   // 0x60-0x6f
        2, 5, 5, 0, 0, 4, 6, 0, 2, 4, 0, 0, 0, 4, 7, 0,   // 0x70-0x7f
        2, 6, 0, 0, 3, 3, 3, 0, 2, 0, 2, 0, 4, 4, 4, 0,   // 0x80-0x8f
        2, 6, 5, 0, 4, 4, 4, 0, 2, 5, 2, 0, 0, 5, 0, 0,   // 0x90-0x9f
        2, 6, 2, 0, 3, 3, 3, 0, 2, 2, 2, 0, 4, 4, 4, 0,   // 0xa0-0xaf
        2, 5, 5, 0, 4, 4, 4, 0, 2, 4, 2, 0, 4, 4, 4, 0,   // 0xb0-0xbf
        2, 6, 0, 0, 3, 3, 5, 0, 2, 2, 2, 0, 4, 4, 6, 0,   // 0xc0-0xcf
        2, 5, 5, 0, 0, 4, 6, 0, 2, 4, 0, 0, 0, 4, 7, 0,   // 0xd0-0xdf
        2, 6, 0, 0, 3, 3, 5, 0, 2, 2, 2, 0, 4, 4, 6, 0,   // 0xe0-0xef
        2, 5, 5, 0, 0, 4, 6, 0, 2, 4, 0, 0, 0, 4, 7, 0    // 0xf0-0xff
    };

}
