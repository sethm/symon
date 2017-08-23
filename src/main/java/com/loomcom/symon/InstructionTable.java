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
        AIX {
            public String toString() {
                return "Absolute, X-Indexed Indirect";
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

        ZPR {
            public String toString() {
                return "Zero Page, Relative";
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
        "BRK", "ORA", "NOP", "NOP", "TSB", "ORA", "ASL",  "RMB0",  // 0x00-0x07
        "PHP", "ORA", "ASL", "NOP", "TSB", "ORA", "ASL",  "BBR0",  // 0x08-0x0f
        "BPL", "ORA", "ORA", "NOP", "TRB", "ORA", "ASL",  "RMB1",  // 0x10-0x17
        "CLC", "ORA", "INC", "NOP", "TRB", "ORA", "ASL",  "BBR1",  // 0x18-0x1f
        "JSR", "AND", "NOP", "NOP", "BIT", "AND", "ROL",  "RMB2",  // 0x20-0x27
        "PLP", "AND", "ROL", "NOP", "BIT", "AND", "ROL",  "BBR2",  // 0x28-0x2f
        "BMI", "AND", "AND", "NOP", "BIT", "AND", "ROL",  "RMB3",  // 0x30-0x37
        "SEC", "AND", "DEC", "NOP", "BIT", "AND", "ROL",  "BBR3",  // 0x38-0x3f
        "RTI", "EOR", "NOP", "NOP", "NOP", "EOR", "LSR",  "RMB4",  // 0x40-0x47
        "PHA", "EOR", "LSR", "NOP", "JMP", "EOR", "LSR",  "BBR4",  // 0x48-0x4f
        "BVC", "EOR", "EOR", "NOP", "NOP", "EOR", "LSR",  "RMB5",  // 0x50-0x57
        "CLI", "EOR", "PHY", "NOP", "NOP", "EOR", "LSR",  "BBR5",  // 0x58-0x5f
        "RTS", "ADC", "NOP", "NOP", "STZ", "ADC", "ROR",  "RMB6",  // 0x60-0x67
        "PLA", "ADC", "ROR", "NOP", "JMP", "ADC", "ROR",  "BBR6",  // 0x68-0x6f
        "BVS", "ADC", "ADC", "NOP", "STZ", "ADC", "ROR",  "RMB7",  // 0x70-0x77
        "SEI", "ADC", "PLY", "NOP", "JMP", "ADC", "ROR",  "BBR7",  // 0x78-0x7f
        "BRA", "STA", "NOP", "NOP", "STY", "STA", "STX",  "SMB0",  // 0x80-0x87
        "DEY", "BIT", "TXA", "NOP", "STY", "STA", "STX",  "BBS0",  // 0x88-0x8f
        "BCC", "STA", "STA", "NOP", "STY", "STA", "STX",  "SMB1",  // 0x90-0x97
        "TYA", "STA", "TXS", "NOP", "STZ", "STA", "STZ",  "BBS1",  // 0x98-0x9f
        "LDY", "LDA", "LDX", "NOP", "LDY", "LDA", "LDX",  "SMB2",  // 0xa0-0xa7
        "TAY", "LDA", "TAX", "NOP", "LDY", "LDA", "LDX",  "BBS2",  // 0xa8-0xaf
        "BCS", "LDA", "LDA", "NOP", "LDY", "LDA", "LDX",  "SMB3",  // 0xb0-0xb7
        "CLV", "LDA", "TSX", "NOP", "LDY", "LDA", "LDX",  "BBS3",  // 0xb8-0xbf
        "CPY", "CMP", "NOP", "NOP", "CPY", "CMP", "DEC",  "SMB4",  // 0xc0-0xc7
        "INY", "CMP", "DEX", "NOP", "CPY", "CMP", "DEC",  "BBS4",  // 0xc8-0xcf
        "BNE", "CMP", "CMP", "NOP", "NOP", "CMP", "DEC",  "SMB5",  // 0xd0-0xd7
        "CLD", "CMP", "PHX", "NOP", "NOP", "CMP", "DEC",  "BBS5",  // 0xd8-0xdf
        "CPX", "SBC", "NOP", "NOP", "CPX", "SBC", "INC",  "SMB6",  // 0xe0-0xe7
        "INX", "SBC", "NOP", "NOP", "CPX", "SBC", "INC",  "BBS6",  // 0xe8-0xef
        "BEQ", "SBC", "SBC", "NOP", "NOP", "SBC", "INC",  "SMB7",  // 0xf0-0xf7
        "SED", "SBC", "PLX", "NOP", "NOP", "SBC", "INC",  "BBS7"   // 0xf8-0xff
    };

    /**
     * Instruction addressing modes. This table includes sizes
     * for all instructions for NMOS 6502, CMOS 65C02,
     * and CMOS 65C816
     */
    Mode[] instructionModes = {
        Mode.IMP, Mode.XIN, Mode.NUL, Mode.NUL,   // 0x00-0x03
        Mode.ZPG, Mode.ZPG, Mode.ZPG, Mode.ZPG,   // 0x04-0x07
        Mode.IMP, Mode.IMM, Mode.ACC, Mode.NUL,   // 0x08-0x0b
        Mode.ABS, Mode.ABS, Mode.ABS, Mode.ZPR,   // 0x0c-0x0f
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0x10-0x13
        Mode.ZPG, Mode.ZPX, Mode.ZPX, Mode.ZPG,   // 0x14-0x17
        Mode.IMP, Mode.ABY, Mode.IMP, Mode.NUL,   // 0x18-0x1b
        Mode.ABS, Mode.ABX, Mode.ABX, Mode.ZPR,   // 0x1c-0x1f
        Mode.ABS, Mode.XIN, Mode.NUL, Mode.NUL,   // 0x20-0x23
        Mode.ZPG, Mode.ZPG, Mode.ZPG, Mode.ZPG,   // 0x24-0x27
        Mode.IMP, Mode.IMM, Mode.ACC, Mode.NUL,   // 0x28-0x2b
        Mode.ABS, Mode.ABS, Mode.ABS, Mode.ZPR,   // 0x2c-0x2f
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0x30-0x33
        Mode.ZPX, Mode.ZPX, Mode.ZPX, Mode.ZPG,   // 0x34-0x37
        Mode.IMP, Mode.ABY, Mode.IMP, Mode.NUL,   // 0x38-0x3b
        Mode.NUL, Mode.ABX, Mode.ABX, Mode.ZPR,   // 0x3c-0x3f
        Mode.IMP, Mode.XIN, Mode.NUL, Mode.NUL,   // 0x40-0x43
        Mode.NUL, Mode.ZPG, Mode.ZPG, Mode.ZPG,   // 0x44-0x47
        Mode.IMP, Mode.IMM, Mode.ACC, Mode.NUL,   // 0x48-0x4b
        Mode.ABS, Mode.ABS, Mode.ABS, Mode.ZPR,   // 0x4c-0x4f
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0x50-0x53
        Mode.NUL, Mode.ZPX, Mode.ZPX, Mode.ZPG,   // 0x54-0x57
        Mode.IMP, Mode.ABY, Mode.IMP, Mode.NUL,   // 0x58-0x5b
        Mode.NUL, Mode.ABX, Mode.ABX, Mode.ZPR,   // 0x5c-0x5f
        Mode.IMP, Mode.XIN, Mode.NUL, Mode.NUL,   // 0x60-0x63
        Mode.ZPG, Mode.ZPG, Mode.ZPG, Mode.ZPG,   // 0x64-0x67
        Mode.IMP, Mode.IMM, Mode.ACC, Mode.NUL,   // 0x68-0x6b
        Mode.IND, Mode.ABS, Mode.ABS, Mode.ZPR,   // 0x6c-0x6f
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0x70-0x73
        Mode.ZPX, Mode.ZPX, Mode.ZPX, Mode.ZPG,   // 0x74-0x77
        Mode.IMP, Mode.ABY, Mode.IMP, Mode.NUL,   // 0x78-0x7b
        Mode.AIX, Mode.ABX, Mode.ABX, Mode.ZPR,   // 0x7c-0x7f
        Mode.REL, Mode.XIN, Mode.NUL, Mode.NUL,   // 0x80-0x83
        Mode.ZPG, Mode.ZPG, Mode.ZPG, Mode.ZPG,   // 0x84-0x87
        Mode.IMP, Mode.IMM, Mode.IMP, Mode.NUL,   // 0x88-0x8b
        Mode.ABS, Mode.ABS, Mode.ABS, Mode.ZPR,   // 0x8c-0x8f
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0x90-0x93
        Mode.ZPX, Mode.ZPX, Mode.ZPY, Mode.ZPG,   // 0x94-0x97
        Mode.IMP, Mode.ABY, Mode.IMP, Mode.NUL,   // 0x98-0x9b
        Mode.ABS, Mode.ABX, Mode.ABX, Mode.ZPR,   // 0x9c-0x9f
        Mode.IMM, Mode.XIN, Mode.IMM, Mode.NUL,   // 0xa0-0xa3
        Mode.ZPG, Mode.ZPG, Mode.ZPG, Mode.ZPG,   // 0xa4-0xa7
        Mode.IMP, Mode.IMM, Mode.IMP, Mode.NUL,   // 0xa8-0xab
        Mode.ABS, Mode.ABS, Mode.ABS, Mode.ZPR,   // 0xac-0xaf
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0xb0-0xb3
        Mode.ZPX, Mode.ZPX, Mode.ZPY, Mode.ZPG,   // 0xb4-0xb7
        Mode.IMP, Mode.ABY, Mode.IMP, Mode.NUL,   // 0xb8-0xbb
        Mode.ABX, Mode.ABX, Mode.ABY, Mode.ZPR,   // 0xbc-0xbf
        Mode.IMM, Mode.XIN, Mode.NUL, Mode.NUL,   // 0xc0-0xc3
        Mode.ZPG, Mode.ZPG, Mode.ZPG, Mode.ZPG,   // 0xc4-0xc7
        Mode.IMP, Mode.IMM, Mode.IMP, Mode.NUL,   // 0xc8-0xcb
        Mode.ABS, Mode.ABS, Mode.ABS, Mode.ZPR,   // 0xcc-0xcf
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0xd0-0xd3
        Mode.NUL, Mode.ZPX, Mode.ZPX, Mode.ZPG,   // 0xd4-0xd7
        Mode.IMP, Mode.ABY, Mode.IMP, Mode.NUL,   // 0xd8-0xdb
        Mode.NUL, Mode.ABX, Mode.ABX, Mode.ZPR,   // 0xdc-0xdf
        Mode.IMM, Mode.XIN, Mode.NUL, Mode.NUL,   // 0xe0-0xe3
        Mode.ZPG, Mode.ZPG, Mode.ZPG, Mode.ZPG,   // 0xe4-0xe7
        Mode.IMP, Mode.IMM, Mode.IMP, Mode.NUL,   // 0xe8-0xeb
        Mode.ABS, Mode.ABS, Mode.ABS, Mode.ZPR,   // 0xec-0xef
        Mode.REL, Mode.INY, Mode.ZPI, Mode.NUL,   // 0xf0-0xf3
        Mode.NUL, Mode.ZPX, Mode.ZPX, Mode.ZPG,   // 0xf4-0xf7
        Mode.IMP, Mode.ABY, Mode.IMP, Mode.NUL,   // 0xf8-0xfb
        Mode.NUL, Mode.ABX, Mode.ABX, Mode.ZPR    // 0xfc-0xff
    };


    /**
     * Size, in bytes, required for each instruction. This table
     * includes sizes for all instructions for NMOS 6502, CMOS 65C02,
     * and CMOS 65C816
     */
    int[] instructionSizes = {
        1, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0x00-0x0f
        2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3,   // 0x10-0x1f
        3, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0x20-0x2f
        2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3,   // 0x30-0x3f
        1, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0x40-0x4f
        2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3,   // 0x50-0x5f
        1, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0x60-0x6f
        2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3,   // 0x70-0x7f
        2, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0x80-0x8f
        2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3,   // 0x90-0x9f
        2, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0xa0-0xaf
        2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3,   // 0xb0-0xbf
        2, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0xc0-0xcf
        2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3,   // 0xd0-0xdf
        2, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,   // 0xe0-0xef
        2, 2, 2, 1, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3    // 0xf0-0xff
    };

    /**
     * Number of clock cycles required for each instruction when
     * in NMOS mode.
     */
    int[] instructionClocksNmos = {
        7, 6, 1, 8, 3, 3, 5, 5, 3, 2, 2, 2, 4, 4, 6, 6,   // 0x00-0x0f
        2, 5, 1, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,   // 0x10-0x1f
        6, 6, 1, 8, 3, 3, 5, 5, 4, 2, 2, 2, 4, 4, 6, 6,   // 0x20-0x2f
        2, 5, 1, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,   // 0x30-0x3f
        6, 6, 1, 8, 3, 3, 5, 5, 3, 2, 2, 2, 3, 4, 6, 6,   // 0x40-0x4f
        2, 5, 1, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,   // 0x50-0x5f
        6, 6, 1, 8, 3, 3, 5, 5, 4, 2, 2, 2, 5, 4, 6, 6,   // 0x60-0x6f
        2, 5, 1, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,   // 0x70-0x7f
        2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,   // 0x80-0x8f
        2, 6, 1, 6, 4, 4, 4, 4, 2, 5, 2, 5, 5, 5, 5, 5,   // 0x90-0x9f
        2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,   // 0xa0-0xaf
        2, 5, 1, 5, 4, 4, 4, 4, 2, 4, 2, 4, 4, 4, 4, 4,   // 0xb0-0xbf
        2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,   // 0xc0-0xcf
        2, 5, 1, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,   // 0xd0-0xdf
        2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,   // 0xe0-0xef
        2, 5, 1, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7    // 0xf0-0xff
    };

    /**
     * Number of clock cycles required for each instruction when
     * in CMOS mode
     */
    int[] instructionClocksCmos = {
        7, 6, 2, 1, 5, 3, 5, 5, 3, 2, 2, 1, 6, 4, 6, 5,   // 0x00-0x0f
        2, 5, 5, 1, 5, 4, 6, 5, 2, 4, 2, 1, 6, 4, 6, 5,   // 0x10-0x1f
        6, 6, 2, 1, 3, 3, 5, 5, 4, 2, 2, 1, 4, 4, 6, 5,   // 0x20-0x2f
        2, 5, 5, 1, 4, 4, 6, 5, 2, 4, 2, 1, 4, 4, 6, 5,   // 0x30-0x3f
        6, 6, 2, 1, 2, 3, 5, 3, 3, 2, 2, 1, 3, 4, 6, 5,   // 0x40-0x4f
        2, 5, 5, 1, 4, 4, 6, 5, 2, 4, 3, 1, 8, 4, 6, 5,   // 0x50-0x5f
        6, 6, 2, 1, 3, 3, 5, 5, 4, 2, 2, 1, 6, 4, 6, 5,   // 0x60-0x6f
        2, 5, 5, 1, 4, 4, 6, 5, 2, 4, 4, 3, 6, 4, 6, 5,   // 0x70-0x7f
        3, 6, 2, 1, 3, 3, 3, 5, 2, 2, 2, 1, 4, 4, 4, 5,   // 0x80-0x8f
        2, 6, 5, 1, 4, 4, 4, 5, 2, 5, 2, 1, 4, 5, 5, 5,   // 0x90-0x9f
        2, 6, 2, 1, 3, 3, 3, 5, 2, 2, 2, 1, 4, 4, 4, 5,   // 0xa0-0xaf
        2, 5, 5, 1, 4, 4, 4, 5, 2, 4, 2, 1, 4, 4, 4, 5,   // 0xb0-0xbf
        2, 6, 2, 1, 3, 3, 5, 5, 2, 2, 2, 3, 4, 4, 6, 5,   // 0xc0-0xcf
        2, 5, 5, 1, 4, 4, 6, 5, 2, 4, 3, 3, 4, 4, 7, 5,   // 0xd0-0xdf
        2, 6, 2, 1, 3, 3, 5, 5, 2, 2, 2, 1, 4, 4, 6, 5,   // 0xe0-0xef
        2, 5, 5, 1, 4, 4, 6, 5, 2, 4, 4, 1, 4, 4, 7, 5    // 0xf0-0xff
    };

}
