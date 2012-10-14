package com.loomcom.symon;

import com.loomcom.symon.exceptions.MemoryAccessException;

/**
 * Main 6502 CPU Simulation.
 */
public class Cpu implements InstructionTable {

    public static final int DEFAULT_BASE_ADDRESS = 0x200;

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
    public static final int IRQ_VECTOR_L = 0xfffa;
    public static final int IRQ_VECTOR_H = 0xfffb;
    // Reset vector
    public static final int RST_VECTOR_L = 0xfffc;
    public static final int RST_VECTOR_H = 0xfffd;
    // IRQ vector
    public static final int NMI_VECTOR_L = 0xfffe;
    public static final int NMI_VECTOR_H = 0xffff;

    /* The Bus */
    private Bus bus;

    /* User Registers */
    private int a;  // Accumulator
    private int x;  // X index register
    private int y;  // Y index register

    /* Internal Registers */
    private int pc;  // Program Counter register
    private int sp;  // Stack Pointer register, offset into page 1
    private int ir;  // Instruction register
    private int[] args = new int[3];  // Decoded instruction args
    private int instSize;   // # of operands for the instruction

    /* Scratch space for addressing mode and effective address
  * calculations */
    private int irAddressMode; // Bits 3-5 of IR:  [ | | |X|X|X| | ]
    private int irOpMode;      // Bits 6-7 of IR:  [ | | | | | |X|X]
    private int effectiveAddress;

    /* Internal scratch space */
    private int lo = 0, hi = 0;  // Used in address calculation
    private int tmp; // Temporary storage

    /* Unimplemented instruction flag */
    private boolean opTrap = false;

    private int addr; // The address the most recent instruction
    // was fetched from

    /* Status Flag Register bits */
    private boolean carryFlag;
    private boolean negativeFlag;
    private boolean zeroFlag;
    private boolean irqDisableFlag;
    private boolean decimalModeFlag;
    private boolean breakFlag;
    private boolean overflowFlag;

    /* The number of steps taken since the last reset. */
    private long stepCounter = 0L;

    /**
     * Construct a new CPU.
     */
    public Cpu() {
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

    /**
     * Reset the CPU to known initial values.
     */
    public void reset() throws MemoryAccessException {
        // Registers
        sp = 0xff;

        // Set the PC to the address stored in the reset vector
        pc = address(bus.read(RST_VECTOR_L), bus.read(RST_VECTOR_H));

        // Clear instruction register.
        ir = 0;

        // Clear status register bits.
        carryFlag = false;
        irqDisableFlag = false;
        decimalModeFlag = false;
        breakFlag = false;
        overflowFlag = false;
        zeroFlag = false;

        // Clear illegal opcode trap.
        opTrap = false;

        // Reset step counter
        stepCounter = 0L;

        // Reset registers.
        a = 0;
        x = 0;
        y = 0;
    }

    public void step(int num) throws MemoryAccessException {
        for (int i = 0; i < num; i++) {
            step();
        }
    }

    /**
     * Performs an individual machine cycle.
     */
    public void step() throws MemoryAccessException {
        // Store the address from which the IR was read, for debugging
        addr = pc;

        // Fetch memory location for this instruction.
        ir = bus.read(pc);
        irAddressMode = (ir >> 2) & 0x07;
        irOpMode = ir & 0x03;

        // Increment PC
        incrementPC();

        // Clear the illegal opcode trap.
        clearOpTrap();

        // Decode the instruction and operands
        instSize = Cpu.instructionSizes[ir];
        for (int i = 0; i < instSize - 1; i++) {
            args[i] = bus.read(pc);
            // Increment PC after reading
            incrementPC();
        }

        // Increment step counter
        stepCounter++;

        // Get the data from the effective address (if any)
        effectiveAddress = 0;

        switch (irOpMode) {
            case 0:
            case 2:
                switch (irAddressMode) {
                    case 0: // #Immediate
                        break;
                    case 1: // Zero Page
                        effectiveAddress = args[0];
                        break;
                    case 2: // Accumulator - ignored
                        break;
                    case 3: // Absolute
                        effectiveAddress = address(args[0], args[1]);
                        break;
                    case 5: // Zero Page,X / Zero Page,Y
                        if (ir == 0x96 || ir == 0xb6) {
                            effectiveAddress = zpyAddress(args[0]);
                        } else {
                            effectiveAddress = zpxAddress(args[0]);
                        }
                        break;
                    case 7: // Absolute,X / Absolute,Y
                        if (ir == 0xbe) {
                            effectiveAddress = yAddress(args[0], args[1]);
                        } else {
                            effectiveAddress = xAddress(args[0], args[1]);
                        }
                        break;
                }
                break;
            case 1:
                switch (irAddressMode) {
                    case 0: // (Zero Page,X)
                        tmp = args[0] + getXRegister();
                        effectiveAddress = address(bus.read(tmp), bus.read(tmp + 1));
                        break;
                    case 1: // Zero Page
                        effectiveAddress = args[0];
                        break;
                    case 2: // #Immediate
                        effectiveAddress = -1;
                        break;
                    case 3: // Absolute
                        effectiveAddress = address(args[0], args[1]);
                        break;
                    case 4: // (Zero Page),Y
                        tmp = address(bus.read(args[0]),
                                      bus.read((args[0] + 1) & 0xff));
                        effectiveAddress = (tmp + getYRegister()) & 0xffff;
                        break;
                    case 5: // Zero Page,X
                        effectiveAddress = zpxAddress(args[0]);
                        break;
                    case 6: // Absolute, Y
                        effectiveAddress = yAddress(args[0], args[1]);
                        break;
                    case 7: // Absolute, X
                        effectiveAddress = xAddress(args[0], args[1]);
                        break;
                }
                break;
        }

        // Execute
        switch (ir) {

            /** Single Byte Instructions; Implied and Relative **/
            case 0x00: // BRK - Force Interrupt - Implied
                if (!getIrqDisableFlag()) {
                    // Set the break flag before pushing.
                    setBreakFlag();
                    // Push program counter + 2 onto the stack
                    stackPush((pc + 2 >> 8) & 0xff); // PC high byte
                    stackPush(pc + 2 & 0xff);        // PC low byte
                    stackPush(getProcessorStatus());
                    // Set the Interrupt Disabled flag.  RTI will clear it.
                    setIrqDisableFlag();
                    // Load interrupt vector address into PC
                    pc = address(bus.read(IRQ_VECTOR_L), bus.read(IRQ_VECTOR_H));
                }
                break;
            case 0x08: // PHP - Push Processor Status - Implied
                stackPush(getProcessorStatus());
                break;
            case 0x10: // BPL - Branch if Positive - Relative
                if (!getNegativeFlag()) {
                    pc = relAddress(args[0]);
                }
                break;
            case 0x18: // CLC - Clear Carry Flag - Implied
                clearCarryFlag();
                break;
            case 0x20: // JSR - Jump to Subroutine - Implied
                stackPush((pc - 1 >> 8) & 0xff); // PC high byte
                stackPush(pc - 1 & 0xff);        // PC low byte
                pc = address(args[0], args[1]);
                break;
            case 0x28: // PLP - Pull Processor Status - Implied
                setProcessorStatus(stackPop());
                break;
            case 0x30: // BMI - Branch if Minus - Relative
                if (getNegativeFlag()) {
                    pc = relAddress(args[0]);
                }
                break;
            case 0x38: // SEC - Set Carry Flag - Implied
                setCarryFlag();
                break;
            case 0x40: // RTI - Return from Interrupt - Implied
                setProcessorStatus(stackPop());
                lo = stackPop();
                hi = stackPop();
                setProgramCounter(address(lo, hi));
                break;
            case 0x48: // PHA - Push Accumulator - Implied
                stackPush(a);
                break;
            case 0x50: // BVC - Branch if Overflow Clear - Relative
                if (!getOverflowFlag()) {
                    pc = relAddress(args[0]);
                }
                break;
            case 0x58: // CLI - Clear Interrupt Disable - Implied
                clearIrqDisableFlag();
                break;
            case 0x60: // RTS - Return from Subroutine - Implied
                lo = stackPop();
                hi = stackPop();
                setProgramCounter((address(lo, hi) + 1) & 0xffff);
                break;
            case 0x68: // PLA - Pull Accumulator - Implied
                a = stackPop();
                setArithmeticFlags(a);
                break;
            case 0x70: // BVS - Branch if Overflow Set - Relative
                if (getOverflowFlag()) {
                    pc = relAddress(args[0]);
                }
                break;
            case 0x78: // SEI - Set Interrupt Disable - Implied
                setIrqDisableFlag();
                break;
            case 0x88: // DEY - Decrement Y Register - Implied
                y = --y & 0xff;
                setArithmeticFlags(y);
                break;
            case 0x8a: // TXA - Transfer X to Accumulator - Implied
                a = x;
                setArithmeticFlags(a);
                break;
            case 0x90: // BCC - Branch if Carry Clear - Relative
                if (!getCarryFlag()) {
                    pc = relAddress(args[0]);
                }
                break;
            case 0x98: // TYA - Transfer Y to Accumulator - Implied
                a = y;
                setArithmeticFlags(a);
                break;
            case 0x9a: // TXS - Transfer X to Stack Pointer - Implied
                setStackPointer(x);
                break;
            case 0xa8: // TAY - Transfer Accumulator to Y - Implied
                y = a;
                setArithmeticFlags(y);
                break;
            case 0xaa: // TAX - Transfer Accumulator to X - Implied
                x = a;
                setArithmeticFlags(x);
                break;
            case 0xb0: // BCS - Branch if Carry Set - Relative
                if (getCarryFlag()) {
                    pc = relAddress(args[0]);
                }
                break;
            case 0xb8: // CLV - Clear Overflow Flag - Implied
                clearOverflowFlag();
                break;
            case 0xba: // TSX - Transfer Stack Pointer to X - Implied
                x = getStackPointer();
                setArithmeticFlags(x);
                break;
            case 0xc8: // INY - Increment Y Register - Implied
                y = ++y & 0xff;
                setArithmeticFlags(y);
                break;
            case 0xca: // DEX - Decrement X Register - Implied
                x = --x & 0xff;
                setArithmeticFlags(x);
                break;
            case 0xd0: // BNE - Branch if Not Equal to Zero - Relative
                if (!getZeroFlag()) {
                    pc = relAddress(args[0]);
                }
                break;
            case 0xd8: // CLD - Clear Decimal Mode - Implied
                clearDecimalModeFlag();
                break;
            case 0xe8: // INX - Increment X Register - Implied
                x = ++x & 0xff;
                setArithmeticFlags(x);
                break;
            case 0xea: // NOP
                // Do nothing.
                break;
            case 0xf0: // BEQ - Branch if Equal to Zero - Relative
                if (getZeroFlag()) {
                    pc = relAddress(args[0]);
                }
                break;
            case 0xf8: // SED - Set Decimal Flag - Implied
                setDecimalModeFlag();
                break;

            /** JMP *****************************************************************/
            case 0x4c: // JMP - Absolute
                pc = address(args[0], args[1]);
                break;
            case 0x6c: // JMP - Indirect
                lo = address(args[0], args[1]); // Address of low byte
                hi = lo + 1; // Address of high byte
                pc = address(bus.read(lo), bus.read(hi));
                /* TODO: For accuracy, allow a flag to enable broken behavior
                * of early 6502s:
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


            /** ORA - Logical Inclusive Or ******************************************/
            case 0x09: // #Immediate
                a |= args[0];
                setArithmeticFlags(a);
                break;
            case 0x01: // (Zero Page,X)
            case 0x05: // Zero Page
            case 0x0d: // Absolute
            case 0x11: // (Zero Page),Y
            case 0x15: // Zero Page,X
            case 0x19: // Absolute,Y
            case 0x1d: // Absolute,X
                a |= bus.read(effectiveAddress);
                setArithmeticFlags(a);
                break;


            /** ASL - Arithmetic Shift Left *****************************************/
            case 0x0a: // Accumulator
                a = asl(a);
                setArithmeticFlags(a);
                break;
            case 0x06: // Zero Page
            case 0x0e: // Absolute
            case 0x16: // Zero Page,X
            case 0x1e: // Absolute,X
                tmp = asl(bus.read(effectiveAddress));
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** BIT - Bit Test ******************************************************/
            case 0x24: // Zero Page
            case 0x2c: // Absolute
                tmp = a & bus.read(effectiveAddress);
                setZeroFlag(tmp == 0);
                setNegativeFlag((tmp & 0x80) != 0);
                setOverflowFlag((tmp & 0x40) != 0);
                break;


            /** AND - Logical AND ***************************************************/
            case 0x29: // #Immediate
                a &= args[0];
                setArithmeticFlags(a);
                break;
            case 0x21: // (Zero Page,X)
            case 0x25: // Zero Page
            case 0x2d: // Absolute
            case 0x31: // (Zero Page),Y
            case 0x35: // Zero Page,X
            case 0x39: // Absolute,Y
            case 0x3d: // Absolute,X
                a &= bus.read(effectiveAddress);
                setArithmeticFlags(a);
                break;


            /** ROL - Rotate Left ***************************************************/
            case 0x2a: // Accumulator
                a = rol(a);
                setArithmeticFlags(a);
                break;
            case 0x26: // Zero Page
            case 0x2e: // Absolute
            case 0x36: // Zero Page,X
            case 0x3e: // Absolute,X
                tmp = rol(bus.read(effectiveAddress));
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** EOR - Exclusive OR **************************************************/
            case 0x49: // #Immediate
                a ^= args[0];
                setArithmeticFlags(a);
                break;
            case 0x41: // (Zero Page,X)
            case 0x45: // Zero Page
            case 0x4d: // Absolute
            case 0x51: // (Zero Page,Y)
            case 0x55: // Zero Page,X
            case 0x59: // Absolute,Y
            case 0x5d: // Absolute,X
                a ^= bus.read(effectiveAddress);
                setArithmeticFlags(a);
                break;


            /** LSR - Logical Shift Right *******************************************/
            case 0x4a: // Accumulator
                a = lsr(a);
                setArithmeticFlags(a);
                break;
            case 0x46: // Zero Page
            case 0x4e: // Absolute
            case 0x56: // Zero Page,X
            case 0x5e: // Absolute,X
                tmp = lsr(bus.read(effectiveAddress));
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** ADC - Add with Carry ************************************************/
            case 0x69: // #Immediate
                if (decimalModeFlag) {
                    a = adcDecimal(a, args[0]);
                } else {
                    a = adc(a, args[0]);
                }
                break;
            case 0x61: // (Zero Page,X)
            case 0x65: // Zero Page
            case 0x6d: // Absolute
            case 0x71: // (Zero Page),Y
            case 0x75: // Zero Page,X
            case 0x79: // Absolute,Y
            case 0x7d: // Absolute,X
                if (decimalModeFlag) {
                    a = adcDecimal(a, bus.read(effectiveAddress));
                } else {
                    a = adc(a, bus.read(effectiveAddress));
                }
                break;


            /** ROR - Rotate Right **************************************************/
            case 0x6a: // Accumulator
                a = ror(a);
                setArithmeticFlags(a);
                break;
            case 0x66: // Zero Page
            case 0x6e: // Absolute
            case 0x76: // Zero Page,X
            case 0x7e: // Absolute,X
                tmp = ror(bus.read(effectiveAddress));
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** STA - Store Accumulator *********************************************/
            case 0x81: // (Zero Page,X)
            case 0x85: // Zero Page
            case 0x8d: // Absolute
            case 0x91: // (Zero Page),Y
            case 0x95: // Zero Page,X
            case 0x99: // Absolute,Y
            case 0x9d: // Absolute,X
                bus.write(effectiveAddress, a);
                setArithmeticFlags(a);
                break;


            /** STY - Store Y Register **********************************************/
            case 0x84: // Zero Page
            case 0x8c: // Absolute
            case 0x94: // Zero Page,X
                bus.write(effectiveAddress, y);
                setArithmeticFlags(y);
                break;


            /** STX - Store X Register **********************************************/
            case 0x86: // Zero Page
            case 0x8e: // Absolute
            case 0x96: // Zero Page,Y
                bus.write(effectiveAddress, x);
                setArithmeticFlags(x);
                break;


            /** LDY - Load Y Register ***********************************************/
            case 0xa0: // #Immediate
                y = args[0];
                setArithmeticFlags(y);
                break;
            case 0xa4: // Zero Page
            case 0xac: // Absolute
            case 0xb4: // Zero Page,X
            case 0xbc: // Absolute,X
                y = bus.read(effectiveAddress);
                setArithmeticFlags(y);
                break;


            /** LDX - Load X Register ***********************************************/
            case 0xa2: // #Immediate
                x = args[0];
                setArithmeticFlags(x);
                break;
            case 0xa6: // Zero Page
            case 0xae: // Absolute
            case 0xb6: // Zero Page,Y
            case 0xbe: // Absolute,Y
                x = bus.read(effectiveAddress);
                setArithmeticFlags(x);
                break;


            /** LDA - Load Accumulator **********************************************/
            case 0xa9: // #Immediate
                a = args[0];
                setArithmeticFlags(a);
                break;
            case 0xa1: // (Zero Page,X)
            case 0xa5: // Zero Page
            case 0xad: // Absolute
            case 0xb1: // (Zero Page),Y
            case 0xb5: // Zero Page,X
            case 0xb9: // Absolute,Y
            case 0xbd: // Absolute,X
                a = bus.read(effectiveAddress);
                setArithmeticFlags(a);
                break;


            /** CPY - Compare Y Register ********************************************/
            case 0xc0: // #Immediate
                cmp(y, args[0]);
                break;
            case 0xc4: // Zero Page
            case 0xcc: // Absolute
                cmp(y, bus.read(effectiveAddress));
                break;


            /** CMP - Compare Accumulator *******************************************/
            case 0xc9: // #Immediate
                cmp(a, args[0]);
                break;
            case 0xc1: // (Zero Page,X)
            case 0xc5: // Zero Page
            case 0xcd: // Absolute
            case 0xd1: // (Zero Page),Y
            case 0xd5: // Zero Page,X
            case 0xd9: // Absolute,Y
            case 0xdd: // Absolute,X
                cmp(a, bus.read(effectiveAddress));
                break;


            /** DEC - Decrement Memory **********************************************/
            case 0xc6: // Zero Page
            case 0xce: // Absolute
            case 0xd6: // Zero Page,X
            case 0xde: // Absolute,X
                tmp = (bus.read(effectiveAddress) - 1) & 0xff;
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** CPX - Compare X Register ********************************************/
            case 0xe0: // #Immediate
                cmp(x, args[0]);
                break;
            case 0xe4: // Zero Page
            case 0xec: // Absolute
                cmp(x, bus.read(effectiveAddress));
                break;


            /** SBC - Subtract with Carry (Borrow) **********************************/
            case 0xe9: // #Immediate
                if (decimalModeFlag) {
                    a = sbcDecimal(a, args[0]);
                } else {
                    a = sbc(a, args[0]);
                }
                break;
            case 0xe1: // (Zero Page,X)
            case 0xe5: // Zero Page
            case 0xed: // Absolute
            case 0xf1: // (Zero Page),Y
            case 0xf5: // Zero Page,X
            case 0xf9: // Absolute,Y
            case 0xfd: // Absolute,X
                if (decimalModeFlag) {
                    a = sbcDecimal(a, bus.read(effectiveAddress));
                } else {
                    a = sbc(a, bus.read(effectiveAddress));
                }
                break;


            /** INC - Increment Memory **********************************************/
            case 0xe6: // Zero Page
            case 0xee: // Absolute
            case 0xf6: // Zero Page,X
            case 0xfe: // Absolute,X
                tmp = (bus.read(effectiveAddress) + 1) & 0xff;
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;

            /** Unimplemented Instructions ****************************************/
            default:
                setOpTrap();
                break;
        }
    }

    /**
     * Add with Carry, used by all addressing mode implementations of ADC.
     * As a side effect, this will set the overflow and carry flags as
     * needed.
     *
     * @param acc     The current value of the accumulator
     * @param operand The operand
     * @return
     */
    public int adc(int acc, int operand) {
        int result = (operand & 0xff) + (acc & 0xff) + getCarryBit();
        int carry6 = (operand & 0x7f) + (acc & 0x7f) + getCarryBit();
        setCarryFlag((result & 0x100) != 0);
        setOverflowFlag(carryFlag ^ ((carry6 & 0x80) != 0));
        result &= 0xff;
        setArithmeticFlags(result);
        return result;
    }

    /**
     * Add with Carry (BCD).
     */

    public int adcDecimal(int acc, int operand) {
        int l, h, result;
        l = (acc & 0x0f) + (operand & 0x0f) + getCarryBit();
        if ((l & 0xff) > 9) l += 6;
        h = (acc >> 4) + (operand >> 4) + (l > 15 ? 1 : 0);
        if ((h & 0xff) > 9) h += 6;
        result = (l & 0x0f) | (h << 4);
        result &= 0xff;
        setCarryFlag(h > 15);
        setZeroFlag(result == 0);
        setNegativeFlag(false); // BCD is never negative
        setOverflowFlag(false); // BCD never sets overflow flag
        return result;
    }

    /**
     * Common code for Subtract with Carry.  Just calls ADC of the
     * one's complement of the operand.  This lets the N, V, C, and Z
     * flags work out nicely without any additional logic.
     *
     * @param acc
     * @param operand
     * @return
     */
    public int sbc(int acc, int operand) {
        int result;
        result = adc(acc, ~operand);
        setArithmeticFlags(result);
        return result;
    }

    /**
     * Subtract with Carry, BCD mode.
     *
     * @param acc
     * @param operand
     * @return
     */
    public int sbcDecimal(int acc, int operand) {
        int l, h, result;
        l = (acc & 0x0f) - (operand & 0x0f) - (carryFlag ? 0 : 1);
        if ((l & 0x10) != 0) l -= 6;
        h = (acc >> 4) - (operand >> 4) - ((l & 0x10) != 0 ? 1 : 0);
        if ((h & 0x10) != 0) h -= 6;
        result = (l & 0x0f) | (h << 4);
        setCarryFlag((h & 0xff) < 15);
        setZeroFlag(result == 0);
        setNegativeFlag(false); // BCD is never negative
        setOverflowFlag(false); // BCD never sets overflow flag
        return (result & 0xff);
    }

    /**
     * Compare two values, and set carry, zero, and negative flags
     * appropriately.
     *
     * @param reg
     * @param operand
     */
    public void cmp(int reg, int operand) {
        setCarryFlag(reg >= operand);
        setZeroFlag(reg == operand);
        setNegativeFlag((reg - operand) > 0);
    }

    /**
     * Set the Negative and Zero flags based on the current value of the
     * register operand.
     *
     * @param reg The register.
     */
    public void setArithmeticFlags(int reg) {
        zeroFlag = (reg == 0);
        negativeFlag = (reg & 0x80) != 0;
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
        return (m >>> 1) & 0xff;
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
     * @return the negative flag
     */
    public boolean getNegativeFlag() {
        return negativeFlag;
    }

    /**
     * @return 1 if the negative flag is set, 0 if it is clear.
     */
    public int getNegativeBit() {
        return (negativeFlag ? 1 : 0);
    }

    /**
     * @param register the register value to test for negativity
     */
    public void setNegativeFlag(int register) {
        negativeFlag = (register < 0);
    }

    /**
     * @param negativeFlag the negative flag to set
     */
    public void setNegativeFlag(boolean negativeFlag) {
        this.negativeFlag = negativeFlag;
    }

    public void setNegativeFlag() {
        this.negativeFlag = true;
    }

    public void clearNegativeFlag() {
        this.negativeFlag = false;
    }

    /**
     * @return the carry flag
     */
    public boolean getCarryFlag() {
        return carryFlag;
    }

    /**
     * @return 1 if the carry flag is set, 0 if it is clear.
     */
    public int getCarryBit() {
        return (carryFlag ? 1 : 0);
    }

    /**
     * @param carryFlag the carry flag to set
     */
    public void setCarryFlag(boolean carryFlag) {
        this.carryFlag = carryFlag;
    }

    /**
     * Sets the Carry Flag
     */
    public void setCarryFlag() {
        this.carryFlag = true;
    }

    /**
     * Clears the Carry Flag
     */
    public void clearCarryFlag() {
        this.carryFlag = false;
    }

    /**
     * @return the zero flag
     */
    public boolean getZeroFlag() {
        return zeroFlag;
    }

    /**
     * @return 1 if the zero flag is set, 0 if it is clear.
     */
    public int getZeroBit() {
        return (zeroFlag ? 1 : 0);
    }

    /**
     * @param zeroFlag the zero flag to set
     */
    public void setZeroFlag(boolean zeroFlag) {
        this.zeroFlag = zeroFlag;
    }

    /**
     * Sets the Zero Flag
     */
    public void setZeroFlag() {
        this.zeroFlag = true;
    }

    /**
     * Clears the Zero Flag
     */
    public void clearZeroFlag() {
        this.zeroFlag = false;
    }

    /**
     * @return the irq disable flag
     */
    public boolean getIrqDisableFlag() {
        return irqDisableFlag;
    }

    /**
     * @return 1 if the interrupt disable flag is set, 0 if it is clear.
     */
    public int getIrqDisableBit() {
        return (irqDisableFlag ? 1 : 0);
    }

    /**
     * @param irqDisableFlag the irq disable flag to set
     */
    public void setIrqDisableFlag(boolean irqDisableFlag) {
        this.irqDisableFlag = irqDisableFlag;
    }

    public void setIrqDisableFlag() {
        this.irqDisableFlag = true;
    }

    public void clearIrqDisableFlag() {
        this.irqDisableFlag = false;
    }


    /**
     * @return the decimal mode flag
     */
    public boolean getDecimalModeFlag() {
        return decimalModeFlag;
    }

    /**
     * @return 1 if the decimal mode flag is set, 0 if it is clear.
     */
    public int getDecimalModeBit() {
        return (decimalModeFlag ? 1 : 0);
    }

    /**
     * @param decimalModeFlag the decimal mode flag to set
     */
    public void setDecimalModeFlag(boolean decimalModeFlag) {
        this.decimalModeFlag = decimalModeFlag;
    }

    /**
     * Sets the Decimal Mode Flag to true.
     */
    public void setDecimalModeFlag() {
        this.decimalModeFlag = true;
    }

    /**
     * Clears the Decimal Mode Flag.
     */
    public void clearDecimalModeFlag() {
        this.decimalModeFlag = false;
    }

    /**
     * @return the break flag
     */
    public boolean getBreakFlag() {
        return breakFlag;
    }

    /**
     * @return 1 if the break flag is set, 0 if it is clear.
     */
    public int getBreakBit() {
        return (carryFlag ? 1 : 0);
    }

    /**
     * @param breakFlag the break flag to set
     */
    public void setBreakFlag(boolean breakFlag) {
        this.breakFlag = breakFlag;
    }

    /**
     * Sets the Break Flag
     */
    public void setBreakFlag() {
        this.breakFlag = true;
    }

    /**
     * Clears the Break Flag
     */
    public void clearBreakFlag() {
        this.breakFlag = false;
    }

    /**
     * @return the overflow flag
     */
    public boolean getOverflowFlag() {
        return overflowFlag;
    }

    /**
     * @return 1 if the overflow flag is set, 0 if it is clear.
     */
    public int getOverflowBit() {
        return (overflowFlag ? 1 : 0);
    }

    /**
     * @param overflowFlag the overflow flag to set
     */
    public void setOverflowFlag(boolean overflowFlag) {
        this.overflowFlag = overflowFlag;
    }

    /**
     * Sets the Overflow Flag
     */
    public void setOverflowFlag() {
        this.overflowFlag = true;
    }

    /**
     * Clears the Overflow Flag
     */
    public void clearOverflowFlag() {
        this.overflowFlag = false;
    }

    /**
     * Set the illegal instruction trap.
     */
    public void setOpTrap() {
        this.opTrap = true;
    }

    /**
     * Clear the illegal instruction trap.
     */
    public void clearOpTrap() {
        this.opTrap = false;
    }

    /**
     * Get the status of the illegal instruction trap.
     */
    public boolean getOpTrap() {
        return this.opTrap;
    }

    public int getAccumulator() {
        return a;
    }

    public void setAccumulator(int val) {
        this.a = val;
    }

    public int getXRegister() {
        return x;
    }

    public void setXRegister(int val) {
        this.x = val;
    }

    public int getYRegister() {
        return y;
    }

    public void setYRegister(int val) {
        this.y = val;
    }

    public int getProgramCounter() {
        return pc;
    }

    public void setProgramCounter(int addr) {
        this.pc = addr;
    }

    public int getStackPointer() {
        return sp;
    }

    public void setStackPointer(int offset) {
        this.sp = offset;
    }

    public int getInstructionRegister() {
        return this.ir;
    }

    public void setInstructionRegister(int op) {
        this.ir = op;
    }

    public long getStepCounter() {
        return stepCounter;
    }

    public void setStepCounter(long stepCount) {
        this.stepCounter = stepCount;
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

    /**
     * @returns The value of the Process Status Register, as a byte.
     */
    public int getProcessorStatus() {
        int status = 0x20;
        if (getCarryFlag()) {
            status |= P_CARRY;
        }
        if (getZeroFlag()) {
            status |= P_ZERO;
        }
        if (getIrqDisableFlag()) {
            status |= P_IRQ_DISABLE;
        }
        if (getDecimalModeFlag()) {
            status |= P_DECIMAL;
        }
        if (getBreakFlag()) {
            status |= P_BREAK;
        }
        if (getOverflowFlag()) {
            status |= P_OVERFLOW;
        }
        if (getNegativeFlag()) {
            status |= P_NEGATIVE;
        }
        return status;
    }

    /**
     * @return A string representing the current status register state.
     */
    public String getProcessorStatusString() {
        StringBuffer sb = new StringBuffer("[");
        sb.append(getNegativeFlag() ? 'N' : '.');   // Bit 7
        sb.append(getOverflowFlag() ? 'V' : '.');   // Bit 6
        sb.append("-");                                // Bit 5 (always 1)
        sb.append(getBreakFlag() ? 'B' : '.');   // Bit 4
        sb.append(getDecimalModeFlag() ? 'D' : '.');   // Bit 3
        sb.append(getIrqDisableFlag() ? 'I' : '.');   // Bit 2
        sb.append(getZeroFlag() ? 'Z' : '.');   // Bit 1
        sb.append(getCarryFlag() ? 'C' : '.');   // Bit 0
        sb.append("]");
        return sb.toString();
    }

    public String getOpcodeStatus() {
        return opcode(ir, args[0], args[1]);
    }

    public String getAddressStatus() {
        return String.format("$%04X", addr);
    }

    public String getARegisterStatus() {
        return String.format("$%02X", a);
    }

    public String getXRegisterStatus() {
        return String.format("$%02X", x);
    }

    public String getYRegisterStatus() {
        return String.format("$%02X", y);
    }

    public String getProgramCounterStatus() {
        return String.format("$%04X", pc);
    }

    /**
     * Returns a string representing the CPU state.
     */
    public String toString() {
        String opcode = opcode(ir, args[0], args[1]);
        StringBuffer sb = new StringBuffer(String.format("$%04X", addr) +
                                           "   ");
        sb.append(String.format("%-14s", opcode));
        sb.append("A=" + String.format("$%02X", a) + "  ");
        sb.append("X=" + String.format("$%02X", x) + "  ");
        sb.append("Y=" + String.format("$%02X", y) + "  ");
        sb.append("PC=" + String.format("$%04X", pc) + "  ");
        sb.append("P=" + getProcessorStatusString());
        return sb.toString();
    }

    /**
     * Push an item onto the stack, and decrement the stack counter.
     * Will wrap-around if already at the bottom of the stack (This
     * is the same behavior as the real 6502)
     */
    void stackPush(int data) throws MemoryAccessException {
        bus.write(0x100 + sp, data);

        if (sp == 0) {
            sp = 0xff;
        } else {
            --sp;
        }

    }


    /**
     * Pre-increment the stack pointer, and return the top of the stack.
     * Will wrap-around if already at the top of the stack (This
     * is the same behavior as the real 6502)
     */
    int stackPop() throws MemoryAccessException {
        if (sp == 0xff) {
            sp = 0x00;
        } else {
            ++sp;
        }

        return bus.read(0x100 + sp);
    }

    /**
     * Peek at the value currently at the top of the stack
     */
    int stackPeek() throws MemoryAccessException {
        return bus.read(0x100 + sp + 1);
    }

    /*
    * Increment the PC, rolling over if necessary.
    */
    void incrementPC() {
        if (pc == 0xffff) {
            pc = 0;
        } else {
            ++pc;
        }
    }

    /**
     * Given two bytes, return an address.
     */
    int address(int lowByte, int hiByte) {
        return ((hiByte << 8) | lowByte) & 0xffff;
    }

    /**
     * Given a hi byte and a low byte, return the Absolute,X
     * offset address.
     */
    int xAddress(int lowByte, int hiByte) {
        return (address(lowByte, hiByte) + getXRegister()) & 0xffff;
    }

    /**
     * Given a hi byte and a low byte, return the Absolute,Y
     * offset address.
     */
    int yAddress(int lowByte, int hiByte) {
        return (address(lowByte, hiByte) + getYRegister()) & 0xffff;
    }

    /**
     * Given a single byte, compute the Zero Page,X offset address.
     */
    int zpxAddress(int zp) {
        return (zp + getXRegister()) & 0xff;
    }

    /**
     * Given a single byte, compute the offset address.
     */
    int relAddress(int offset) {
        // Cast the offset to a signed byte to handle negative offsets
        return (pc + (byte) offset) & 0xffff;
    }

    /**
     * Given a single byte, compute the Zero Page,Y offset address.
     */
    int zpyAddress(int zp) {
        return (zp + getYRegister()) & 0xff;
    }

    public void setResetVector(int address) throws MemoryAccessException {
        bus.write(RST_VECTOR_H, (address & 0xff00) >>> 8);
        bus.write(RST_VECTOR_L, address & 0x00ff);
    }

    /**
     * Given an opcode and its operands, return a formatted name.
     *
     * @param opcode The opcode
     * @param op1    The first operand
     * @param op2    The second operand
     * @return
     */
    String opcode(int opcode, int op1, int op2) {
        String opcodeName = Cpu.opcodeNames[opcode];
        if (opcodeName == null) {
            return "???";
        }

        StringBuffer sb = new StringBuffer(opcodeName);

        switch (Cpu.instructionModes[opcode]) {
            case ABS:
                sb.append(String.format(" $%04X", address(op1, op2)));
                break;
            case ABX:
                sb.append(String.format(" $%04X,X", address(op1, op2)));
                break;
            case ABY:
                sb.append(String.format(" $%04X,Y", address(op1, op2)));
                break;
            case IMM:
                sb.append(String.format(" #$%02X", op1));
                break;
            case IND:
                sb.append(String.format(" ($%04X)", address(op1, op2)));
                break;
            case XIN:
                sb.append(String.format(" ($%02X),X", op1));
                break;
            case INY:
                sb.append(String.format(" ($%02X,Y)", op1));
                break;
            case REL:
            case ZPG:
                sb.append(String.format(" $%02X", op1));
                break;
            case ZPX:
                sb.append(String.format(" $%02X,X", op1));
                break;
            case ZPY:
                sb.append(String.format(" $%02X,Y", op1));
                break;
        }

        return sb.toString();
    }
}