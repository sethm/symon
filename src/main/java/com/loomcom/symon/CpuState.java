package com.loomcom.symon;

import com.loomcom.symon.util.Utils;

/**
 * A compact, struct-like representation of CPU state.
 */
public class CpuState {
    /**
     * Accumulator
     */
    public int a;

    /**
     * X index regsiter
     */
    public int x;

    /**
     * Y index register
     */
    public int y;

    /**
     * Stack Pointer
     */
    public int sp;

    /**
     * Program Counter
     */
    public int pc;

    /**
     * Last Loaded Instruction Register
     */
    public int ir;

    /**
     * Peek-Ahead to next IR
     */
    public int nextIr;
    public int[] args = new int[2];
    public int[] nextArgs = new int[2];
    public int instSize;
    public boolean opTrap;
    public boolean irqAsserted;
    public boolean nmiAsserted;
    public int lastPc;

    /* Status Flag Register bits */
    public boolean carryFlag;
    public boolean negativeFlag;
    public boolean zeroFlag;
    public boolean irqDisableFlag;
    public boolean decimalModeFlag;
    public boolean breakFlag;
    public boolean overflowFlag;
    public long stepCounter = 0L;

    public CpuState() {}

    /**
     * Snapshot a copy of the CpuState.
     *
     * (This is a copy constructor rather than an implementation of <code>Cloneable</code>
     * based on Josh Bloch's recommendation)
     *
     * @param s The CpuState to copy.
     */
    public CpuState(CpuState s) {
        this.a = s.a;
        this.x = s.x;
        this.y = s.y;
        this.sp = s.sp;
        this.pc = s.pc;
        this.ir = s.ir;
        this.nextIr = s.nextIr;
        this.lastPc = s.lastPc;
        this.args[0] = s.args[0];
        this.args[1] = s.args[1];
        this.nextArgs[0] = s.nextArgs[0];
        this.nextArgs[1] = s.nextArgs[1];
        this.instSize = s.instSize;
        this.opTrap = s.opTrap;
        this.irqAsserted = s.irqAsserted;
        this.carryFlag = s.carryFlag;
        this.negativeFlag = s.negativeFlag;
        this.zeroFlag = s.zeroFlag;
        this.irqDisableFlag = s.irqDisableFlag;
        this.decimalModeFlag = s.decimalModeFlag;
        this.breakFlag = s.breakFlag;
        this.overflowFlag = s.overflowFlag;
        this.stepCounter = s.stepCounter;
    }

    /**
     * Returns a string formatted for the trace log.
     *
     * @return a string formatted for the trace log.
     */
    public String toTraceEvent() {
        String opcode = Cpu.disassembleOp(ir, args);
        return getInstructionByteStatus() + "  " +
                String.format("%-14s", opcode) +
                "A:" + Utils.byteToHex(a) + " " +
                "X:" + Utils.byteToHex(x) + " " +
                "Y:" + Utils.byteToHex(y) + " " +
                "F:" + Utils.byteToHex(getStatusFlag()) + " " +
                "S:1" + Utils.byteToHex(sp) + " " +
                getProcessorStatusString() + "\n";
    }

    /**
     * @return The value of the Process Status Register, as a byte.
     */
    public int getStatusFlag() {
        int status = 0x20;
        if (carryFlag) {
            status |= Cpu.P_CARRY;
        }
        if (zeroFlag) {
            status |= Cpu.P_ZERO;
        }
        if (irqDisableFlag) {
            status |= Cpu.P_IRQ_DISABLE;
        }
        if (decimalModeFlag) {
            status |= Cpu.P_DECIMAL;
        }
        if (breakFlag) {
            status |= Cpu.P_BREAK;
        }
        if (overflowFlag) {
            status |= Cpu.P_OVERFLOW;
        }
        if (negativeFlag) {
            status |= Cpu.P_NEGATIVE;
        }
        return status;
    }

    public String getInstructionByteStatus() {
        switch (Cpu.instructionSizes[ir]) {
            case 0:
            case 1:
                return Utils.wordToHex(lastPc) + "  " +
                       Utils.byteToHex(ir) + "      ";
            case 2:
                return Utils.wordToHex(lastPc) + "  " +
                       Utils.byteToHex(ir) + " " +
                       Utils.byteToHex(args[0]) + "   ";
            case 3:
                return Utils.wordToHex(lastPc) + "  " +
                       Utils.byteToHex(ir) + " " +
                       Utils.byteToHex(args[0]) + " " +
                       Utils.byteToHex(args[1]);
            default:
                return null;
        }
    }

    /**
     * @return A string representing the current status register state.
     */
    public String getProcessorStatusString() {
        return "[" + (negativeFlag ? 'N' : '.') +
                (overflowFlag ? 'V' : '.') +
                "-" +
                (breakFlag ? 'B' : '.') +
                (decimalModeFlag ? 'D' : '.') +
                (irqDisableFlag ? 'I' : '.') +
                (zeroFlag ? 'Z' : '.') +
                (carryFlag ? 'C' : '.') +
                "]";
    }
}
