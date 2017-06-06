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

import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class provides a simulation of the MOS 6502 CPU's state machine.
 * A simple interface allows this 6502 to read and write to a simulated bus,
 * and exposes some of the internal state for inspection and debugging.
 */
public class Cpu implements InstructionTable {

    private final static Logger logger = LoggerFactory.getLogger(Cpu.class.getName());

    /* Process status register mnemonics */
    public static final int P_CARRY       = 0x01;
    public static final int P_ZERO        = 0x02;
    public static final int P_IRQ_DISABLE = 0x04;
    public static final int P_DECIMAL     = 0x08;
    public static final int P_BREAK       = 0x10;
    // Bit 5 is always '1'
    public static final int P_OVERFLOW    = 0x40;
    public static final int P_NEGATIVE    = 0x80;

    // NMI vector
    public static final int NMI_VECTOR_L = 0xfffa;
    public static final int NMI_VECTOR_H = 0xfffb;
    // Reset vector
    public static final int RST_VECTOR_L = 0xfffc;
    public static final int RST_VECTOR_H = 0xfffd;
    // IRQ vector
    public static final int IRQ_VECTOR_L = 0xfffe;
    public static final int IRQ_VECTOR_H = 0xffff;

    public static final long DEFAULT_CLOCK_PERIOD_IN_NS = 1000;

    /* Simulated clock speed (default is 1MHz) */
    private long clockPeriodInNs = DEFAULT_CLOCK_PERIOD_IN_NS;

    /* Simulated behavior */
    private CpuBehavior behavior;

    /* The Bus */
    private Bus bus;

    /* The CPU state */
    private final CpuState state = new CpuState();

    /* start time of op execution, needed for speed simulation */
    private long opBeginTime;

    /**
     * Construct a new CPU.
     */
    public Cpu() {
        this(CpuBehavior.NMOS_6502);
    }

    public Cpu(CpuBehavior behavior) {
        this.behavior = behavior;
    }

    /**
     * Set the bus reference for this CPU.
     */
    public void setBus(Bus bus) {
        this.bus = bus;
    }

    /**
     * Return the Bus that this CPU is associated with.
     */
    public Bus getBus() {
        return bus;
    }

    public void setBehavior(CpuBehavior behavior) {
        this.behavior = behavior;
    }

    public CpuBehavior getBehavior() {
        return behavior;
    }

    /**
     * Reset the CPU to known initial values.
     */
    public void reset() throws MemoryAccessException {
        /* TODO: In reality, the stack pointer could be anywhere
           on the stack after reset. This non-deterministic behavior might be
           worth while to simulate. */
        state.sp = 0xff;

        // Set the PC to the address stored in the reset vector
        state.pc = Utils.address(bus.read(RST_VECTOR_L, true), bus.read(RST_VECTOR_H, true));

        // Clear instruction register.
        state.ir = 0;

        // Clear status register bits.
        state.carryFlag = false;
        state.zeroFlag = false;
        state.irqDisableFlag = false;
        state.decimalModeFlag = false;
        state.breakFlag = false;
        state.overflowFlag = false;
        state.negativeFlag = false;

        state.irqAsserted = false;

        // Clear illegal opcode trap.
        state.opTrap = false;

        // Reset step counter
        state.stepCounter = 0L;

        // Reset registers.
        state.a = 0;
        state.x = 0;
        state.y = 0;

        peekAhead();
    }

    public void step(int num) throws MemoryAccessException {
        for (int i = 0; i < num; i++) {
            step();
        }
    }

    /**
     * Performs an individual instruction cycle.
     */
    public void step() throws MemoryAccessException {
        opBeginTime = System.nanoTime();

        // Store the address from which the IR was read, for debugging
        state.lastPc = state.pc;

        // Check for Interrupts before doing anything else.
        // This will set the PC and jump to the interrupt vector.
        if (state.nmiAsserted) {
            handleNmi();
        } else if (state.irqAsserted && !getIrqDisableFlag()) {
            handleIrq(state.pc);
        }

        // Fetch memory location for this instruction.
        state.ir = bus.read(state.pc, true);
        int irAddressMode = (state.ir >> 2) & 0x07;  // Bits 3-5 of IR:  [ | | |X|X|X| | ]
        int irOpMode = state.ir & 0x03;              // Bits 6-7 of IR:  [ | | | | | |X|X]

        incrementPC();

        clearOpTrap();

        // Decode the instruction and operands
        state.instSize = Cpu.instructionSizes[state.ir];
        for (int i = 0; i < state.instSize - 1; i++) {
            state.args[i] = bus.read(state.pc, true);
            // Increment PC after reading
            incrementPC();
        }

        state.stepCounter++;

        // Get the data from the effective address (if any)
        int effectiveAddress = 0;
        int tmp; // Temporary storage

        switch (irOpMode) {
            case 0:
            case 2:
                switch (irAddressMode) {
                    case 0: // #Immediate
                        break;
                    case 1: // Zero Page
                        effectiveAddress = state.args[0];
                        break;
                    case 2: // Accumulator - ignored
                        break;
                    case 3: // Absolute
                        effectiveAddress = Utils.address(state.args[0], state.args[1]);
                        break;
                    case 4: // 65C02 (Zero Page)
                        if (behavior == CpuBehavior.CMOS_6502 ||
                            behavior == CpuBehavior.CMOS_65816) {
                            effectiveAddress = Utils.address(bus.read(state.args[0], true),
                                                             bus.read((state.args[0] + 1) & 0xff, true));
                        }
                        break;
                    case 5: // Zero Page,X / Zero Page,Y
                        if (state.ir == 0x14) { // 65C02 TRB Zero Page
                            effectiveAddress = state.args[0];
                        }
                        else if (state.ir == 0x96 || state.ir == 0xb6) {
                            effectiveAddress = zpyAddress(state.args[0]);
                        } else {
                            effectiveAddress = zpxAddress(state.args[0]);
                        }
                        break;
                    case 7:
                        if (state.ir == 0x9c || state.ir == 0x1c) { // 65C02 STZ & TRB Absolute
                            effectiveAddress = Utils.address(state.args[0], state.args[1]);
                        }
                        else if (state.ir == 0xbe) { // Absolute,X / Absolute,Y
                            effectiveAddress = yAddress(state.args[0], state.args[1]);
                        } else {
                            effectiveAddress = xAddress(state.args[0], state.args[1]);
                        }
                        break;
                }
                break;
            case 3: // Rockwell/WDC 65C02
                switch (irAddressMode) {
                    case 1: // Zero Page
                    case 3:
                    case 5:
                    case 7: // Zero Page, Relative
                        effectiveAddress = state.args[0];
                        break;
                }
                break;
            case 1:
                switch (irAddressMode) {
                    case 0: // (Zero Page,X)
                        tmp = (state.args[0] + state.x) & 0xff;
                        effectiveAddress = Utils.address(bus.read(tmp, true), bus.read(tmp + 1, true));
                        break;
                    case 1: // Zero Page
                        effectiveAddress = state.args[0];
                        break;
                    case 2: // #Immediate
                        effectiveAddress = -1;
                        break;
                    case 3: // Absolute
                        effectiveAddress = Utils.address(state.args[0], state.args[1]);
                        break;
                    case 4: // (Zero Page),Y
                        tmp = Utils.address(bus.read(state.args[0], true),
                                      bus.read((state.args[0] + 1) & 0xff, true));
                        effectiveAddress = (tmp + state.y) & 0xffff;
                        break;
                    case 5: // Zero Page,X
                        effectiveAddress = zpxAddress(state.args[0]);
                        break;
                    case 6: // Absolute, Y
                        effectiveAddress = yAddress(state.args[0], state.args[1]);
                        break;
                    case 7: // Absolute, X
                        effectiveAddress = xAddress(state.args[0], state.args[1]);
                        break;
                }
                break;
        }

        // Execute
        switch (state.ir) {

            /** Single Byte Instructions; Implied and Relative **/
            case 0x00: // BRK - Force Interrupt - Implied
                handleBrk(state.pc + 1);
                break;
            case 0x08: // PHP - Push Processor Status - Implied
                // Break flag is always set in the stack value.
                stackPush(state.getStatusFlag() | 0x10);
                break;
            case 0x10: // BPL - Branch if Positive - Relative
                if (!getNegativeFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0x18: // CLC - Clear Carry Flag - Implied
                clearCarryFlag();
                break;
            case 0x20: // JSR - Jump to Subroutine - Implied
                stackPush((state.pc - 1 >> 8) & 0xff); // PC high byte
                stackPush(state.pc - 1 & 0xff);        // PC low byte
                state.pc = Utils.address(state.args[0], state.args[1]);
                break;
            case 0x28: // PLP - Pull Processor Status - Implied
                setProcessorStatus(stackPop());
                break;
            case 0x30: // BMI - Branch if Minus - Relative
                if (getNegativeFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0x38: // SEC - Set Carry Flag - Implied
                setCarryFlag();
                break;
            case 0x40: // RTI - Return from Interrupt - Implied
                setProcessorStatus(stackPop());
                int lo = stackPop();
                int hi = stackPop();
                setProgramCounter(Utils.address(lo, hi));
                break;
            case 0x48: // PHA - Push Accumulator - Implied
                stackPush(state.a);
                break;
            case 0x50: // BVC - Branch if Overflow Clear - Relative
                if (!getOverflowFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0x58: // CLI - Clear Interrupt Disable - Implied
                clearIrqDisableFlag();
                break;
            case 0x5a: // 65C02 PHY - Push Y to stack
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                stackPush(state.y);
                break;
            case 0x60: // RTS - Return from Subroutine - Implied
                lo = stackPop();
                hi = stackPop();
                setProgramCounter((Utils.address(lo, hi) + 1) & 0xffff);
                break;
            case 0x68: // PLA - Pull Accumulator - Implied
                state.a = stackPop();
                setArithmeticFlags(state.a);
                break;
            case 0x70: // BVS - Branch if Overflow Set - Relative
                if (getOverflowFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0x78: // SEI - Set Interrupt Disable - Implied
                setIrqDisableFlag();
                break;
            case 0x7a: // 65C02 PLY - Pull Y from Stack
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                state.y = stackPop();
                setArithmeticFlags(state.y);
                break;
            case 0x80: // 65C02 BRA - Branch Always
                if (behavior == CpuBehavior.CMOS_6502 ||
                    behavior == CpuBehavior.CMOS_65816) {
                        state.pc = relAddress(state.args[0]);
                }
                break;
            case 0x88: // DEY - Decrement Y Register - Implied
                state.y = --state.y & 0xff;
                setArithmeticFlags(state.y);
                break;
            case 0x8a: // TXA - Transfer X to Accumulator - Implied
                state.a = state.x;
                setArithmeticFlags(state.a);
                break;
            case 0x90: // BCC - Branch if Carry Clear - Relative
                if (!getCarryFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0x98: // TYA - Transfer Y to Accumulator - Implied
                state.a = state.y;
                setArithmeticFlags(state.a);
                break;
            case 0x9a: // TXS - Transfer X to Stack Pointer - Implied
                setStackPointer(state.x);
                break;
            case 0xa8: // TAY - Transfer Accumulator to Y - Implied
                state.y = state.a;
                setArithmeticFlags(state.y);
                break;
            case 0xaa: // TAX - Transfer Accumulator to X - Implied
                state.x = state.a;
                setArithmeticFlags(state.x);
                break;
            case 0xb0: // BCS - Branch if Carry Set - Relative
                if (getCarryFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0xb8: // CLV - Clear Overflow Flag - Implied
                clearOverflowFlag();
                break;
            case 0xba: // TSX - Transfer Stack Pointer to X - Implied
                state.x = getStackPointer();
                setArithmeticFlags(state.x);
                break;
            case 0xc8: // INY - Increment Y Register - Implied
                state.y = ++state.y & 0xff;
                setArithmeticFlags(state.y);
                break;
            case 0xca: // DEX - Decrement X Register - Implied
                state.x = --state.x & 0xff;
                setArithmeticFlags(state.x);
                break;
            case 0xd0: // BNE - Branch if Not Equal to Zero - Relative
                if (!getZeroFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0xd8: // CLD - Clear Decimal Mode - Implied
                clearDecimalModeFlag();
                break;
            case 0xda: // 65C02 PHX - Push X to stack
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                stackPush(state.x);
                break;
            case 0xe8: // INX - Increment X Register - Implied
                state.x = ++state.x & 0xff;
                setArithmeticFlags(state.x);
                break;
            case 0xea: // NOP
                // Do nothing.
                break;
            case 0xf0: // BEQ - Branch if Equal to Zero - Relative
                if (getZeroFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0xf8: // SED - Set Decimal Flag - Implied
                setDecimalModeFlag();
                break;
            case 0xfa: // 65C02 PLX - Pull X from Stack
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                state.x = stackPop();
                setArithmeticFlags(state.x);
                break;

            /** JMP *****************************************************************/
            case 0x4c: // JMP - Absolute
                state.pc = Utils.address(state.args[0], state.args[1]);
                break;
            case 0x6c: // JMP - Indirect
                lo = Utils.address(state.args[0], state.args[1]); // Address of low byte

                if (state.args[0] == 0xff &&
                    (behavior == CpuBehavior.NMOS_6502 ||
                     behavior == CpuBehavior.NMOS_WITH_ROR_BUG)) {
                    hi = Utils.address(0x00, state.args[1]);
                } else {
                    hi = lo + 1;
                }

                state.pc = Utils.address(bus.read(lo, true), bus.read(hi, true));
                /* TODO: For accuracy, allow a flag to enable broken behavior of early 6502s:
                 *
                 * "An original 6502 has does not correctly fetch the target
                 * address if the indirect vector falls on a page boundary
                 * (e.g. $xxFF where xx is and value from $00 to $FF). In this
                 * case fetches the LSB from $xxFF as expected but takes the MSB
                 * from $xx00. This is fixed in some later chips like the 65SC02
                 * so for compatibility always ensure the indirect vector is not
                 * at the end of the page."
                 * (http://www.obelisk.demon.co.uk/6502/reference.html#JMP)
                 */
                break;
            case 0x7c: // 65C02 JMP - (Absolute Indexed Indirect,X)
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                lo = (((state.args[1] << 8) | state.args[0]) + state.x) & 0xffff;
                hi = lo + 1;
                state.pc = Utils.address(bus.read(lo, true), bus.read(hi, true));
                break;

            /** ORA - Logical Inclusive Or ******************************************/
            case 0x09: // #Immediate
                state.a |= state.args[0];
                setArithmeticFlags(state.a);
                break;
            case 0x12: // 65C02 ORA (ZP)
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
            case 0x01: // (Zero Page,X)
            case 0x05: // Zero Page
            case 0x0d: // Absolute
            case 0x11: // (Zero Page),Y
            case 0x15: // Zero Page,X
            case 0x19: // Absolute,Y
            case 0x1d: // Absolute,X
                state.a |= bus.read(effectiveAddress, true);
                setArithmeticFlags(state.a);
                break;


            /** ASL - Arithmetic Shift Left *****************************************/
            case 0x0a: // Accumulator
                state.a = asl(state.a);
                setArithmeticFlags(state.a);
                break;
            case 0x06: // Zero Page
            case 0x0e: // Absolute
            case 0x16: // Zero Page,X
            case 0x1e: // Absolute,X
                tmp = asl(bus.read(effectiveAddress, true));
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** BIT - Bit Test ******************************************************/
            case 0x89: // 65C02 #Immediate
                setZeroFlag((state.a & state.args[0]) == 0);
                break;
            case 0x34: // 65C02 Zero Page,X
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
            case 0x24: // Zero Page
            case 0x2c: // Absolute
            case 0x3c: // Absolute,X
                tmp = bus.read(effectiveAddress, true);
                setZeroFlag((state.a & tmp) == 0);
                setNegativeFlag((tmp & 0x80) != 0);
                setOverflowFlag((tmp & 0x40) != 0);
                break;


            /** AND - Logical AND ***************************************************/
            case 0x29: // #Immediate
                state.a &= state.args[0];
                setArithmeticFlags(state.a);
                break;
            case 0x32: // 65C02 AND (ZP)
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
            case 0x21: // (Zero Page,X)
            case 0x25: // Zero Page
            case 0x2d: // Absolute
            case 0x31: // (Zero Page),Y
            case 0x35: // Zero Page,X
            case 0x39: // Absolute,Y
            case 0x3d: // Absolute,X
                state.a &= bus.read(effectiveAddress, true);
                setArithmeticFlags(state.a);
                break;


            /** ROL - Rotate Left ***************************************************/
            case 0x2a: // Accumulator
                state.a = rol(state.a);
                setArithmeticFlags(state.a);
                break;
            case 0x26: // Zero Page
            case 0x2e: // Absolute
            case 0x36: // Zero Page,X
            case 0x3e: // Absolute,X
                tmp = rol(bus.read(effectiveAddress, true));
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** EOR - Exclusive OR **************************************************/
            case 0x49: // #Immediate
                state.a ^= state.args[0];
                setArithmeticFlags(state.a);
                break;
            case 0x52: // 65C02 EOR (ZP)
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
            case 0x41: // (Zero Page,X)
            case 0x45: // Zero Page
            case 0x4d: // Absolute
            case 0x51: // (Zero Page,Y)
            case 0x55: // Zero Page,X
            case 0x59: // Absolute,Y
            case 0x5d: // Absolute,X
                state.a ^= bus.read(effectiveAddress, true);
                setArithmeticFlags(state.a);
                break;


            /** LSR - Logical Shift Right *******************************************/
            case 0x4a: // Accumulator
                state.a = lsr(state.a);
                setArithmeticFlags(state.a);
                break;
            case 0x46: // Zero Page
            case 0x4e: // Absolute
            case 0x56: // Zero Page,X
            case 0x5e: // Absolute,X
                tmp = lsr(bus.read(effectiveAddress, true));
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** ADC - Add with Carry ************************************************/
            case 0x69: // #Immediate
                if (state.decimalModeFlag) {
                    state.a = adcDecimal(state.a, state.args[0]);
                } else {
                    state.a = adc(state.a, state.args[0]);
                }
                break;
            case 0x72: // 65C02 ADC (ZP)
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
            case 0x61: // (Zero Page,X)
            case 0x65: // Zero Page
            case 0x6d: // Absolute
            case 0x71: // (Zero Page),Y
            case 0x75: // Zero Page,X
            case 0x79: // Absolute,Y
            case 0x7d: // Absolute,X
                if (state.decimalModeFlag) {
                    state.a = adcDecimal(state.a, bus.read(effectiveAddress, true));
                } else {
                    state.a = adc(state.a, bus.read(effectiveAddress, true));
                }
                break;


            /** ROR - Rotate Right **************************************************/
            case 0x6a: // Accumulator
                state.a = ror(state.a);
                setArithmeticFlags(state.a);
                break;
            case 0x66: // Zero Page
            case 0x6e: // Absolute
            case 0x76: // Zero Page,X
            case 0x7e: // Absolute,X
                tmp = ror(bus.read(effectiveAddress, true));
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** STA - Store Accumulator *********************************************/
            case 0x92: // 65C02 STA (ZP)
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
            case 0x81: // (Zero Page,X)
            case 0x85: // Zero Page
            case 0x8d: // Absolute
            case 0x91: // (Zero Page),Y
            case 0x95: // Zero Page,X
            case 0x99: // Absolute,Y
            case 0x9d: // Absolute,X
                bus.write(effectiveAddress, state.a);
                break;


            /** STY - Store Y Register **********************************************/
            case 0x84: // Zero Page
            case 0x8c: // Absolute
            case 0x94: // Zero Page,X
                bus.write(effectiveAddress, state.y);
                break;


            /** STX - Store X Register **********************************************/
            case 0x86: // Zero Page
            case 0x8e: // Absolute
            case 0x96: // Zero Page,Y
                bus.write(effectiveAddress, state.x);
                break;

            /** STZ - 65C02 Store Zero ****************************************************/
            case 0x64: // Zero Page
            case 0x74: // Zero Page,X
            case 0x9c: // Absolute
            case 0x9e: // Absolute,X
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                bus.write(effectiveAddress, 0);
                break;

            /** LDY - Load Y Register ***********************************************/
            case 0xa0: // #Immediate
                state.y = state.args[0];
                setArithmeticFlags(state.y);
                break;
            case 0xa4: // Zero Page
            case 0xac: // Absolute
            case 0xb4: // Zero Page,X
            case 0xbc: // Absolute,X
                state.y = bus.read(effectiveAddress, true);
                setArithmeticFlags(state.y);
                break;


            /** LDX - Load X Register ***********************************************/
            case 0xa2: // #Immediate
                state.x = state.args[0];
                setArithmeticFlags(state.x);
                break;
            case 0xa6: // Zero Page
            case 0xae: // Absolute
            case 0xb6: // Zero Page,Y
            case 0xbe: // Absolute,Y
                state.x = bus.read(effectiveAddress, true);
                setArithmeticFlags(state.x);
                break;


            /** LDA - Load Accumulator **********************************************/
            case 0xa9: // #Immediate
                state.a = state.args[0];
                setArithmeticFlags(state.a);
                break;
            case 0xb2: // 65C02 LDA (ZP)
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
            case 0xa1: // (Zero Page,X)
            case 0xa5: // Zero Page
            case 0xad: // Absolute
            case 0xb1: // (Zero Page),Y
            case 0xb5: // Zero Page,X
            case 0xb9: // Absolute,Y
            case 0xbd: // Absolute,X
                state.a = bus.read(effectiveAddress, true);
                setArithmeticFlags(state.a);
                break;


            /** CPY - Compare Y Register ********************************************/
            case 0xc0: // #Immediate
                cmp(state.y, state.args[0]);
                break;
            case 0xc4: // Zero Page
            case 0xcc: // Absolute
                cmp(state.y, bus.read(effectiveAddress, true));
                break;


            /** CMP - Compare Accumulator *******************************************/
            case 0xc9: // #Immediate
                cmp(state.a, state.args[0]);
                break;
            case 0xd2: // 65C02 CMP (ZP)
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
            case 0xc1: // (Zero Page,X)
            case 0xc5: // Zero Page
            case 0xcd: // Absolute
            case 0xd1: // (Zero Page),Y
            case 0xd5: // Zero Page,X
            case 0xd9: // Absolute,Y
            case 0xdd: // Absolute,X
                cmp(state.a, bus.read(effectiveAddress, true));
                break;


            /** DEC - Decrement Memory **********************************************/
            case 0x3a: // 65C02 Immediate
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                state.a = --state.a & 0xFF;
                setArithmeticFlags(state.a);
                break;
            case 0xc6: // Zero Page
            case 0xce: // Absolute
            case 0xd6: // Zero Page,X
            case 0xde: // Absolute,X
                tmp = (bus.read(effectiveAddress, true) - 1) & 0xff;
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** CPX - Compare X Register ********************************************/
            case 0xe0: // #Immediate
                cmp(state.x, state.args[0]);
                break;
            case 0xe4: // Zero Page
            case 0xec: // Absolute
                cmp(state.x, bus.read(effectiveAddress, true));
                break;


            /** SBC - Subtract with Carry (Borrow) **********************************/
            case 0xe9: // #Immediate
                if (state.decimalModeFlag) {
                    state.a = sbcDecimal(state.a, state.args[0]);
                } else {
                    state.a = sbc(state.a, state.args[0]);
                }
                break;
            case 0xf2: // 65C02 SBC (ZP)
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
            case 0xe1: // (Zero Page,X)
            case 0xe5: // Zero Page
            case 0xed: // Absolute
            case 0xf1: // (Zero Page),Y
            case 0xf5: // Zero Page,X
            case 0xf9: // Absolute,Y
            case 0xfd: // Absolute,X
                if (state.decimalModeFlag) {
                    state.a = sbcDecimal(state.a, bus.read(effectiveAddress, true));
                } else {
                    state.a = sbc(state.a, bus.read(effectiveAddress, true));
                }
                break;


            /** INC - Increment Memory **********************************************/
            case 0x1a: // 65C02 Increment Immediate
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                state.a = ++state.a & 0xff;
                setArithmeticFlags(state.a);
                break;
            case 0xe6: // Zero Page
            case 0xee: // Absolute
            case 0xf6: // Zero Page,X
            case 0xfe: // Absolute,X
                tmp = (bus.read(effectiveAddress, true) + 1) & 0xff;
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** 65C02 RMB - Reset Memory Bit **************************************/
            case 0x07: // 65C02 RMB0 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp &= ~(1 << 0);
                bus.write(effectiveAddress, tmp);
                break;
            case 0x17: // 65C02 RMB1 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp &= ~(1 << 1);
                bus.write(effectiveAddress, tmp);
                break;
            case 0x27: // 65C02 RMB2 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp &= ~(1 << 2);
                bus.write(effectiveAddress, tmp);
                break;
            case 0x37: // 65C02 RMB3 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp &= ~(1 << 3);
                bus.write(effectiveAddress, tmp);
                break;
            case 0x47: // 65C02 RMB4 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp &= ~(1 << 4);
                bus.write(effectiveAddress, tmp);
                break;
            case 0x57: // 65C02 RMB5 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp &= ~(1 << 5);
                bus.write(effectiveAddress, tmp);
                break;
            case 0x67: // 65C02 RMB6 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp &= ~(1 << 6);
                bus.write(effectiveAddress, tmp);
                break;
            case 0x77: // 65C02 RMB7 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp &= ~(1 << 7);
                bus.write(effectiveAddress, tmp);
                break;


            /** 65C02 SMB - Set Memory Bit **************************************/
            case 0x87: // 65C02 SMB0 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp |= (1);
                bus.write(effectiveAddress, tmp);
                break;
            case 0x97: // 65C02 SMB1 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp |= (1 << 1);
                bus.write(effectiveAddress, tmp);
                break;
            case 0xa7: // 65C02 SMB2 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp |= (1 << 2);
                bus.write(effectiveAddress, tmp);
                break;
            case 0xb7: // 65C02 SMB3 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp |= (1 << 3);
                bus.write(effectiveAddress, tmp);
                break;
            case 0xc7: // 65C02 SMB4 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp |= (1 << 4);
                bus.write(effectiveAddress, tmp);
                break;
            case 0xd7: // 65C02 SMB5 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp |= (1 << 5);
                bus.write(effectiveAddress, tmp);
                break;
            case 0xe7: // 65C02 SMB6 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp |= (1 << 6);
                bus.write(effectiveAddress, tmp);
                break;
            case 0xf7: // 65C02 SMB7 - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true) & 0xff;
                tmp |= (1 << 7);
                bus.write(effectiveAddress, tmp);
                break;

            /** 65C02 TRB/TSB - Test and Reset Bit/Test and Set Bit ***************/
            case 0x14: // 65C02 TRB - Test and Reset bit - Zero Page
            case 0x1c: // 65C02 TRB - Test and Reset bit - Absolute
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                setZeroFlag((state.a & tmp) == 0);
                tmp = (tmp &= ~(state.a)) & 0xff;
                bus.write(effectiveAddress,tmp);
                break;

            case 0x04: // 65C02 TSB - Test and Set bit - Zero Page
            case 0x0c: // 65C02 TSB - Test and Set bit - Absolute
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                setZeroFlag((state.a & tmp) == 0);
                tmp = (tmp |= (state.a)) & 0xff;
                bus.write(effectiveAddress,tmp);
                break;

            /** 65C02 BBR - Branch if Bit Reset *************************************/
            case 0x0f: // 65C02 BBR - Branch if bit 0 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 0) == 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;

            case 0x1f: // 65C02 BBR - Branch if bit 1 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 1) == 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;

            case 0x2f: // 65C02 BBR - Branch if bit 2 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 2) == 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;

            case 0x3f: // 65C02 BBR - Branch if bit 3 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 3) == 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;

            case 0x4f: // 65C02 BBR - Branch if bit 4 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 4) == 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;


            case 0x5f: // 65C02 BBR - Branch if bit 5 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 5) == 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;

            case 0x6f: // 65C02 BBR - Branch if bit 6 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 6) == 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;

            case 0x7f: // 65C02 BBR - Branch if bit 5 reset - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 7) == 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;


            /** 65C02 BBS - Branch if Bit Set  ************************************/
            case 0x8f: // 65C02 BBS - Branch if bit 0 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 0) > 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;

            case 0x9f: // 65C02 BBS - Branch if bit 1 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 1) > 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;

            case 0xaf: // 65C02 BBS - Branch if bit 2 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 2) > 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;

            case 0xbf: // 65C02 BBS - Branch if bit 3 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 3) > 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;


            case 0xcf: // 65C02 BBS - Branch if bit 4 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 4) > 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;


            case 0xdf: // 65C02 BBS - Branch if bit 5 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 5) > 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;

            case 0xef: // 65C02 BBS - Branch if bit 6 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 6) > 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;

            case 0xff: // 65C02 BBS - Branch if bit 5 set - Zero Page
                if (behavior == CpuBehavior.NMOS_6502 ||
                    behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                    break;
                }
                tmp = bus.read(effectiveAddress, true);
                if ((tmp & 1 << 7) > 0) {
                    state.pc = relAddress(state.args[1]);
                }
                break;


            /** Unimplemented Instructions ****************************************/
            // TODO: Create a flag to enable highly-accurate emulation of unimplemented instructions.
            default:
                setOpTrap();
                break;
        }

        delayLoop(state.ir);

        // Peek ahead to the next insturction and arguments
        peekAhead();
    }

    private void peekAhead() throws MemoryAccessException {
        state.nextIr = bus.read(state.pc, true);
        int nextInstSize = Cpu.instructionSizes[state.nextIr];
        for (int i = 1; i < nextInstSize; i++) {
            int nextRead = (state.pc + i) % bus.endAddress();
            state.nextArgs[i-1] = bus.read(nextRead, true);
        }
    }

    private void handleBrk(int returnPc) throws MemoryAccessException {
        handleInterrupt(returnPc, IRQ_VECTOR_L, IRQ_VECTOR_H, true);
        clearIrq();
    }

    private void handleIrq(int returnPc) throws MemoryAccessException {
        handleInterrupt(returnPc, IRQ_VECTOR_L, IRQ_VECTOR_H, false);
        clearIrq();
    }

    private void handleNmi() throws MemoryAccessException {
        handleInterrupt(state.pc, NMI_VECTOR_L, NMI_VECTOR_H, false);
        clearNmi();
    }

    /**
     * Handle the common behavior of BRK, /IRQ, and /NMI
     *
     * @throws MemoryAccessException
     */
    private void handleInterrupt(int returnPc, int vectorLow, int vectorHigh, boolean isBreak) throws MemoryAccessException {

        if (isBreak)
        {
            // Set the break flag before pushing.
            setBreakFlag();
        }
        else
        {
            // IRQ & NMI clear break flag
            clearBreakFlag();
        }

        // Push program counter + 1 onto the stack
        stackPush((returnPc >> 8) & 0xff); // PC high byte
        stackPush(returnPc & 0xff);        // PC low byte
        stackPush(state.getStatusFlag());
        // Set the Interrupt Disabled flag.  RTI will clear it.
        setIrqDisableFlag();

        // 65C02 & 65816 clear Decimal flag after pushing Processor status to the stack
        if (behavior == CpuBehavior.CMOS_6502||
            behavior == CpuBehavior.CMOS_65816) {
            clearDecimalModeFlag();
        }

        // Load interrupt vector address into PC
        state.pc = Utils.address(bus.read(vectorLow, true), bus.read(vectorHigh, true));
    }

    /**
     * Add with Carry, used by all addressing mode implementations of ADC.
     * As a side effect, this will set the overflow and carry flags as
     * needed.
     *
     * @param acc     The current value of the accumulator
     * @param operand The operand
     * @return The sum of the accumulator and the operand
     */
    private int adc(int acc, int operand) {
        int result = (operand & 0xff) + (acc & 0xff) + getCarryBit();
        int carry6 = (operand & 0x7f) + (acc & 0x7f) + getCarryBit();
        setCarryFlag((result & 0x100) != 0);
        setOverflowFlag(state.carryFlag ^ ((carry6 & 0x80) != 0));
        result &= 0xff;
        setArithmeticFlags(result);
        return result;
    }

    /**
     * Add with Carry (BCD).
     */

    private int adcDecimal(int acc, int operand) {
        int l, h, result;
        l = (acc & 0x0f) + (operand & 0x0f) + getCarryBit();
        if ((l & 0xff) > 9) l += 6;
        h = (acc >> 4) + (operand >> 4) + (l > 15 ? 1 : 0);
        if ((h & 0xff) > 9) h += 6;
        result = (l & 0x0f) | (h << 4);
        result &= 0xff;
        setCarryFlag(h > 15);
        setZeroFlag(result == 0);
        setOverflowFlag(false); // BCD never sets overflow flag

        if (behavior == CpuBehavior.NMOS_6502||
            behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                setNegativeFlag(false); // BCD is never negative on NMOS 6502
        }
        else {
            state.negativeFlag = (result & 0x80) != 0; // N Flag is valid on CMOS 6502/65816
        }
        return result;
    }

    /**
     * Common code for Subtract with Carry.  Just calls ADC of the
     * one's complement of the operand.  This lets the N, V, C, and Z
     * flags work out nicely without any additional logic.
     */
    private int sbc(int acc, int operand) {
        int result;
        result = adc(acc, ~operand);
        setArithmeticFlags(result);
        return result;
    }

    /**
     * Subtract with Carry, BCD mode.
     */
    private int sbcDecimal(int acc, int operand) {
        int l, h, result;
        l = (acc & 0x0f) - (operand & 0x0f) - (state.carryFlag ? 0 : 1);
        if ((l & 0x10) != 0) l -= 6;
        h = (acc >> 4) - (operand >> 4) - ((l & 0x10) != 0 ? 1 : 0);
        if ((h & 0x10) != 0) h -= 6;
        result = (l & 0x0f) | (h << 4) & 0xff;
        setCarryFlag((h & 0xff) < 15);
        setZeroFlag(result == 0);
        setOverflowFlag(false); // BCD never sets overflow flag

        if (behavior == CpuBehavior.NMOS_6502||
            behavior == CpuBehavior.NMOS_WITH_ROR_BUG) {
                setNegativeFlag(false); // BCD is never negative on NMOS 6502
        }
        else {
            state.negativeFlag = (result & 0x80) != 0; // N Flag is valid on CMOS 6502/65816
        }
        return (result & 0xff);
    }

    /**
     * Compare two values, and set carry, zero, and negative flags
     * appropriately.
     */
    private void cmp(int reg, int operand) {
        int tmp = (reg - operand) & 0xff;
        setCarryFlag(reg >= operand);
        setZeroFlag(tmp == 0);
        setNegativeFlag((tmp & 0x80) != 0); // Negative bit set
    }

    /**
     * Set the Negative and Zero flags based on the current value of the
     * register operand.
     */
    private void setArithmeticFlags(int reg) {
        state.zeroFlag = (reg == 0);
        state.negativeFlag = (reg & 0x80) != 0;
    }

    /**
     * Shifts the given value left by one bit, and sets the carry
     * flag to the high bit of the initial value.
     *
     * @param m The value to shift left.
     * @return the left shifted value (m * 2).
     */
    private int asl(int m) {
        setCarryFlag((m & 0x80) != 0);
        return (m << 1) & 0xff;
    }

    /**
     * Shifts the given value right by one bit, filling with zeros,
     * and sets the carry flag to the low bit of the initial value.
     */
    private int lsr(int m) {
        setCarryFlag((m & 0x01) != 0);
        return (m & 0xff) >>> 1;
    }

    /**
     * Rotates the given value left by one bit, setting bit 0 to the value
     * of the carry flag, and setting the carry flag to the original value
     * of bit 7.
     */
    private int rol(int m) {
        int result = ((m << 1) | getCarryBit()) & 0xff;
        setCarryFlag((m & 0x80) != 0);
        return result;
    }

    /**
     * Rotates the given value right by one bit, setting bit 7 to the value
     * of the carry flag, and setting the carry flag to the original value
     * of bit 1.
     */
    private int ror(int m) {
        int result = ((m >>> 1) | (getCarryBit() << 7)) & 0xff;
        setCarryFlag((m & 0x01) != 0);
        return result;
    }

    /**
     * @param clockPeriodInNs The simulated clock period, in nanoseconds
     */
    public void setClockPeriodInNs(long clockPeriodInNs) {
        logger.debug("Setting simulated clock period to {} ns.", clockPeriodInNs);
        this.clockPeriodInNs = clockPeriodInNs;
    }

    /**
     * Return the current Cpu State.
     *
     * @return the current Cpu State.
     */
    public CpuState getCpuState() {
        return state;
    }

    /**
     * @return the negative flag
     */
    public boolean getNegativeFlag() {
        return state.negativeFlag;
    }

    /**
     * @param negativeFlag the negative flag to set
     */
    public void setNegativeFlag(boolean negativeFlag) {
        state.negativeFlag = negativeFlag;
    }

    public void setNegativeFlag() {
        state.negativeFlag = true;
    }

    public void clearNegativeFlag() {
        state.negativeFlag = false;
    }

    /**
     * @return the carry flag
     */
    public boolean getCarryFlag() {
        return state.carryFlag;
    }

    /**
     * @return 1 if the carry flag is set, 0 if it is clear.
     */
    public int getCarryBit() {
        return (state.carryFlag ? 1 : 0);
    }

    /**
     * @param carryFlag the carry flag to set
     */
    public void setCarryFlag(boolean carryFlag) {
        state.carryFlag = carryFlag;
    }

    /**
     * Sets the Carry Flag
     */
    public void setCarryFlag() {
        state.carryFlag = true;
    }

    /**
     * Clears the Carry Flag
     */
    public void clearCarryFlag() {
        state.carryFlag = false;
    }

    /**
     * @return the zero flag
     */
    public boolean getZeroFlag() {
        return state.zeroFlag;
    }

    /**
     * @param zeroFlag the zero flag to set
     */
    public void setZeroFlag(boolean zeroFlag) {
        state.zeroFlag = zeroFlag;
    }

    /**
     * Sets the Zero Flag
     */
    public void setZeroFlag() {
        state.zeroFlag = true;
    }

    /**
     * Clears the Zero Flag
     */
    public void clearZeroFlag() {
        state.zeroFlag = false;
    }

    /**
     * @return the irq disable flag
     */
    public boolean getIrqDisableFlag() {
        return state.irqDisableFlag;
    }

    public void setIrqDisableFlag() {
        state.irqDisableFlag = true;
    }

    public void clearIrqDisableFlag() {
        state.irqDisableFlag = false;
    }


    /**
     * @return the decimal mode flag
     */
    public boolean getDecimalModeFlag() {
        return state.decimalModeFlag;
    }

    /**
     * Sets the Decimal Mode Flag to true.
     */
    public void setDecimalModeFlag() {
        state.decimalModeFlag = true;
    }

    /**
     * Clears the Decimal Mode Flag.
     */
    public void clearDecimalModeFlag() {
        state.decimalModeFlag = false;
    }

    /**
     * @return the break flag
     */
    public boolean getBreakFlag() {
        return state.breakFlag;
    }

    /**
     * Sets the Break Flag
     */
    public void setBreakFlag() {
        state.breakFlag = true;
    }

    /**
     * Clears the Break Flag
     */
    public void clearBreakFlag() {
        state.breakFlag = false;
    }

    /**
     * @return the overflow flag
     */
    public boolean getOverflowFlag() {
        return state.overflowFlag;
    }

    /**
     * @param overflowFlag the overflow flag to set
     */
    public void setOverflowFlag(boolean overflowFlag) {
        state.overflowFlag = overflowFlag;
    }

    /**
     * Sets the Overflow Flag
     */
    public void setOverflowFlag() {
        state.overflowFlag = true;
    }

    /**
     * Clears the Overflow Flag
     */
    public void clearOverflowFlag() {
        state.overflowFlag = false;
    }

    /**
     * Set the illegal instruction trap.
     */
    public void setOpTrap() {
        state.opTrap = true;
    }

    /**
     * Clear the illegal instruction trap.
     */
    public void clearOpTrap() {
        state.opTrap = false;
    }

    public int getAccumulator() {
        return state.a;
    }

    public void setAccumulator(int val) {
        state.a = val;
    }

    public int getXRegister() {
        return state.x;
    }

    public void setXRegister(int val) {
        state.x = val;
    }

    public int getYRegister() {
        return state.y;
    }

    public void setYRegister(int val) {
        state.y = val;
    }

    public int getProgramCounter() {
        return state.pc;
    }

    public void setProgramCounter(int addr) {
        state.pc = addr;

        // As a side-effect of setting the program counter,
        // we want to peek ahead at the next state.
        try {
            peekAhead();
        } catch (MemoryAccessException ex) {
            logger.error("Could not peek ahead at next instruction state.");
        }
    }

    public int getStackPointer() {
        return state.sp;
    }

    public void setStackPointer(int offset) {
        state.sp = offset;
    }

    public int getInstruction() {
        return state.ir;
    }

    /**
     * @value The value of the Process Status Register bits to be set.
     */
    public void setProcessorStatus(int value) {
        if ((value & P_CARRY) != 0)
            setCarryFlag();
        else
            clearCarryFlag();

        if ((value & P_ZERO) != 0)
            setZeroFlag();
        else
            clearZeroFlag();

        if ((value & P_IRQ_DISABLE) != 0)
            setIrqDisableFlag();
        else
            clearIrqDisableFlag();

        if ((value & P_DECIMAL) != 0)
            setDecimalModeFlag();
        else
            clearDecimalModeFlag();

        if ((value & P_BREAK) != 0)
            setBreakFlag();
        else
            clearBreakFlag();

        if ((value & P_OVERFLOW) != 0)
            setOverflowFlag();
        else
            clearOverflowFlag();

        if ((value & P_NEGATIVE) != 0)
            setNegativeFlag();
        else
            clearNegativeFlag();
    }

    public String getAccumulatorStatus() {
        return "$" + Utils.byteToHex(state.a);
    }

    public String getXRegisterStatus() {
        return "$" + Utils.byteToHex(state.x);
    }

    public String getYRegisterStatus() {
        return "$" + Utils.byteToHex(state.y);
    }

    public String getProgramCounterStatus() {
        return "$" + Utils.wordToHex(state.pc);
    }

    public String getStackPointerStatus() {
        return "$" + Utils.byteToHex(state.sp);
    }

    public int getProcessorStatus() {
        return state.getStatusFlag();
    }

    /**
     * Simulate transition from logic-high to logic-low on the INT line.
     */
    public void assertIrq() {
       state.irqAsserted = true;
    }

    /**
     * Simulate transition from logic-low to logic-high of the INT line.
     */
    public void clearIrq() {
        state.irqAsserted = false;
    }

    /**
     * Simulate transition from logic-high to logic-low on the NMI line.
     */
    public void assertNmi() {
        state.nmiAsserted = true;
    }

    /**
     * Simulate transition from logic-low to logic-high of the NMI line.
     */
    public void clearNmi() {
        state.nmiAsserted = false;
    }

    /**
     * Push an item onto the stack, and decrement the stack counter.
     * Will wrap-around if already at the bottom of the stack (This
     * is the same behavior as the real 6502)
     */
    void stackPush(int data) throws MemoryAccessException {
        bus.write(0x100 + state.sp, data);

        if (state.sp == 0) {
            state.sp = 0xff;
        } else {
            --state.sp;
        }
    }


    /**
     * Pre-increment the stack pointer, and return the top of the stack.
     * Will wrap-around if already at the top of the stack (This
     * is the same behavior as the real 6502)
     */
    int stackPop() throws MemoryAccessException {
        if (state.sp == 0xff) {
            state.sp = 0x00;
        } else {
            ++state.sp;
        }

        return bus.read(0x100 + state.sp, true);
    }

    /**
     * Peek at the value currently at the top of the stack
     */
    int stackPeek() throws MemoryAccessException {
        return bus.read(0x100 + state.sp + 1, true);
    }

    /*
    * Increment the PC, rolling over if necessary.
    */
    void incrementPC() {
        if (state.pc == 0xffff) {
            state.pc = 0;
        } else {
            ++state.pc;
        }
    }

    /**
     * Given a hi byte and a low byte, return the Absolute,X
     * offset address.
     */
    int xAddress(int lowByte, int hiByte) {
        return (Utils.address(lowByte, hiByte) + state.x) & 0xffff;
    }

    /**
     * Given a hi byte and a low byte, return the Absolute,Y
     * offset address.
     */
    int yAddress(int lowByte, int hiByte) {
        return (Utils.address(lowByte, hiByte) + state.y) & 0xffff;
    }

    /**
     * Given a single byte, compute the Zero Page,X offset address.
     */
    int zpxAddress(int zp) {
        return (zp + state.x) & 0xff;
    }

    /**
     * Given a single byte, compute the Zero Page,Y offset address.
     */
    int zpyAddress(int zp) {
        return (zp + state.y) & 0xff;
    }

    /**
     * Given a single byte, compute the offset address.
     */
    int relAddress(int offset) {
        // Cast the offset to a signed byte to handle negative offsets
        return (state.pc + (byte) offset) & 0xffff;
    }

    /*
     * Perform a busy-loop until the instruction should complete on the wall clock
     */
    private void delayLoop(int opcode) {
        final int clockSteps;

        if (behavior == CpuBehavior.NMOS_WITH_ROR_BUG ||
            behavior == CpuBehavior.NMOS_6502) {
            clockSteps = Cpu.instructionClocksNmos[0xff & opcode];
        } else {
            clockSteps = Cpu.instructionClocksCmos[0xff & opcode];
        }

        if (clockSteps == 0) {
            logger.warn("Opcode {} has clock step of 0!", String.format("0x%02x", opcode));
            return;
        }

        long interval = clockSteps * clockPeriodInNs;
        long end;

        do {
            end = System.nanoTime();
        } while (opBeginTime + interval >= end);
    }

    /**
     * Return a formatted string representing the last instruction and
     * operands that were executed.
     *
     * @return A string representing the mnemonic and operands of the instruction
     */
    public static String disassembleOp(int opCode, int[] args) {
        String mnemonic = opcodeNames[opCode];

        if (mnemonic == null) {
            return "???";
        }

        StringBuilder sb = new StringBuilder(mnemonic);

        switch (instructionModes[opCode]) {
            case ABS:
                sb.append(" $").append(Utils.wordToHex(Utils.address(args[0], args[1])));
                break;
            case AIX:
                sb.append(" ($").append(Utils.wordToHex(Utils.address(args[0], args[1]))).append(",X)");
            case ABX:
                sb.append(" $").append(Utils.wordToHex(Utils.address(args[0], args[1]))).append(",X");
                break;
            case ABY:
                sb.append(" $").append(Utils.wordToHex(Utils.address(args[0], args[1]))).append(",Y");
                break;
            case IMM:
                sb.append(" #$").append(Utils.byteToHex(args[0]));
                break;
            case IND:
                sb.append(" ($").append(Utils.wordToHex(Utils.address(args[0], args[1]))).append(")");
                break;
            case XIN:
                sb.append(" ($").append(Utils.byteToHex(args[0])).append(",X)");
                break;
            case INY:
                sb.append(" ($").append(Utils.byteToHex(args[0])).append("),Y");
                break;
            case REL:
            case ZPR:
            case ZPG:
                sb.append(" $").append(Utils.byteToHex(args[0]));
                break;
            case ZPX:
                sb.append(" $").append(Utils.byteToHex(args[0])).append(",X");
                break;
            case ZPY:
                sb.append(" $").append(Utils.byteToHex(args[0])).append(",Y");
                break;
        }

        return sb.toString();
    }

    /**
     * Return a formatted string representing the next instruction and
     * operands to be executed.
     *
     * @return A string representing the mnemonic and operands of the instruction
     */
    public String disassembleNextOp() {
        return Cpu.disassembleOp(state.nextIr, state.nextArgs);
    }

    /**
     * @param address Address to disassemble
     * @return String containing the disassembled instruction and operands.
     */
    public String disassembleOpAtAddress(int address) throws MemoryAccessException {
        int opCode = bus.read(address, true);
        int args[] = new int[2];
        int size = Cpu.instructionSizes[opCode];
        for (int i = 1; i < size; i++) {
            int nextRead = (address + i) % bus.endAddress();
            args[i-1] = bus.read(nextRead, true);
        }

        return disassembleOp(opCode, args);
    }
}
