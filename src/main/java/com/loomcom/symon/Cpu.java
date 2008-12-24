package com.loomcom.symon;

import java.util.Arrays;

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

	/* Operands for the current instruction */
	private int[] operands = new int[2];
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

	/**
	 * Construct a new CPU.
	 */
	public Cpu() {}

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
	public void reset() {
		// Registers
		sp = 0xff;

		// Set the PC to the address stored in 0xfffc
		pc = CpuUtils.address(bus.read(0xfffc), bus.read(0xfffd));

		// Clear instruction register.
		ir = 0;

		// Clear status register bits.
		carryFlag = false;
		irqDisableFlag = false;
		decimalModeFlag = false;
		breakFlag = false;
		overflowFlag = false;
	}

	public void step(int num) {
		for (int i = 0; i < num; i++) {
			step();
		}
	}

	/**
	 * Performs an individual machine cycle.
	 */
	public void step() {
		int lo, hi; // Used in address calculation

		// Store the address from which the IR was read, for debugging
		addr = pc;

		// Fetch memory location for this instruction.
		ir = bus.read(pc);

		// TODO: The way we increment the PC may need
		// to change when interrupts are implemented

		// Increment PC
		incProgramCounter();

		// Decode the instruction and operands
		int size = Cpu.instructionSizes[ir];
		for (int i = 0; i < size-1; i++) {
			operands[i] = bus.read(pc);
			// Increment PC after reading
			incProgramCounter();
		}

		// Execute
		switch(ir) {

		case 0x00: // BRK - Force Interrupt, Implied
			if (!getIrqDisableFlag()) {
				stackPush((pc >> 8) & 0xff); // PC high byte
				stackPush(pc & 0xff);        // PC low byte
				stackPush(getProcessorStatus());
				// Load interrupt vector address into PC
				pc = CpuUtils.address(bus.read(0xfffc), bus.read(0xfffd));
				setBreakFlag();
			}
			break;
		case 0x01: // n/a
			break;
		case 0x02: // n/a
			break;
		case 0x03: // n/a
			break;
		case 0x04: // n/a
			break;
		case 0x05: // n/a
			break;
		case 0x06: // n/a
			break;
		case 0x07: // n/a
			break;
		case 0x08: // PHP - Push Processor Status - Implied
			stackPush(getProcessorStatus());
			break;
		case 0x09: // ORA - Immediate
			a |= operands[0];
			setArithmeticFlags(a);
			break;
		case 0x0a: // n/a
			break;
		case 0x0b: // n/a
			break;
		case 0x0c: // n/a
			break;
		case 0x0d: // n/a
			break;
		case 0x0e: // n/a
			break;
		case 0x0f: // n/a
			break;

		case 0x10: // n/a
			break;
		case 0x11: // n/a
			break;
		case 0x12: // n/a
			break;
		case 0x13: // n/a
			break;
		case 0x14: // n/a
			break;
		case 0x15: // n/a
			break;
		case 0x16: // n/a
			break;
		case 0x17: // n/a
			break;
		case 0x18: // CLC - Clear Carry Flag, Implied
			clearCarryFlag();
			break;
		case 0x19: // n/a
			break;
		case 0x1a: // n/a
			break;
		case 0x1b: // n/a
			break;
		case 0x1c: // n/a
			break;
		case 0x1d: // n/a
			break;
		case 0x1e: // n/a
			break;
		case 0x1f: // n/a
			break;

		case 0x20: // n/a
			break;
		case 0x21: // n/a
			break;
		case 0x22: // n/a
			break;
		case 0x23: // n/a
			break;
		case 0x24: // n/a
			break;
		case 0x25: // n/a
			break;
		case 0x26: // n/a
			break;
		case 0x27: // n/a
			break;
		case 0x28: // PLP - Pull Processor Status - Implied
			setProcessorStatus(stackPop());
			break;
		case 0x29: // n/a
			a &= operands[0];
			setArithmeticFlags(a);
			break;
		case 0x2a: // n/a
			break;
		case 0x2b: // n/a
			break;
		case 0x2c: // n/a
			break;
		case 0x2d: // n/a
			break;
		case 0x2e: // n/a
			break;
		case 0x2f: // n/a
			break;

		case 0x30: // n/a
			break;
		case 0x31: // n/a
			break;
		case 0x32: // n/a
			break;
		case 0x33: // n/a
			break;
		case 0x34: // n/a
			break;
		case 0x35: // n/a
			break;
		case 0x36: // n/a
			break;
		case 0x37: // n/a
			break;
		case 0x38: // SEC - Set Carry Flag, Implied
			setCarryFlag();
			break;
		case 0x39: // n/a
			break;
		case 0x3a: // n/a
			break;
		case 0x3b: // n/a
			break;
		case 0x3c: // n/a
			break;
		case 0x3d: // n/a
			break;
		case 0x3e: // n/a
			break;
		case 0x3f: // n/a
			break;

		case 0x40: // RTI - Return from Interrupt - Implied
			setProcessorStatus(stackPop());
			lo = stackPop();
			hi = stackPop();
			setProgramCounter(CpuUtils.address(lo, hi));
			break;
		case 0x41: // n/a
			break;
		case 0x42: // n/a
			break;
		case 0x43: // n/a
			break;
		case 0x44: // n/a
			break;
		case 0x45: // n/a
			break;
		case 0x46: // n/a
			break;
		case 0x47: // n/a
			break;
		case 0x48: // PHA - Push Accumulator - Implied
			stackPush(a);
			break;
		case 0x49: // EOR - Immediate
			a ^= operands[0];
			setArithmeticFlags(a);
			break;
		case 0x4a: // n/a
			break;
		case 0x4b: // n/a
			break;
		case 0x4c: // JMP - Absolute
			pc = CpuUtils.address(operands[0], operands[1]);
			break;
		case 0x4d: // n/a
			break;
		case 0x4e: // n/a
			break;
		case 0x4f: // n/a
			break;

		case 0x50: // n/a
			break;
		case 0x51: // n/a
			break;
		case 0x52: // n/a
			break;
		case 0x53: // n/a
			break;
		case 0x54: // n/a
			break;
		case 0x55: // n/a
			break;
		case 0x56: // n/a
			break;
		case 0x57: // n/a
			break;
		case 0x58: // CLI - Clear Interrupt Disable - Implied
			clearIrqDisableFlag();
			break;
		case 0x59: // n/a
			break;
		case 0x5a: // n/a
			break;
		case 0x5b: // n/a
			break;
		case 0x5c: // n/a
			break;
		case 0x5d: // n/a
			break;
		case 0x5e: // n/a
			break;
		case 0x5f: // n/a
			break;

		case 0x60: // RTS - Return from Subrouting - Implied
			lo = stackPop();
			hi = stackPop();
			setProgramCounter((CpuUtils.address(lo, hi) - 1) & 0xffff);

			break;
		case 0x61: // n/a
			break;
		case 0x62: // n/a
			break;
		case 0x63: // n/a
			break;
		case 0x64: // n/a
			break;
		case 0x65: // n/a
			break;
		case 0x66: // n/a
			break;
		case 0x67: // n/a
			break;
		case 0x68: // PLA - Pull Accumulator - Implied
			a = stackPop();
			setArithmeticFlags(a);
			break;
		case 0x69: // ADC - Immediate Mode
			a = adc(a, operands[0]);
			setArithmeticFlags(a);
			break;
		case 0x6a: // n/a
			break;
		case 0x6b: // n/a
			break;
		case 0x6c: // n/a
			break;
		case 0x6d: // n/a
			break;
		case 0x6e: // n/a
			break;
		case 0x6f: // n/a
			break;

		case 0x70: // n/a
			break;
		case 0x71: // n/a
			break;
		case 0x72: // n/a
			break;
		case 0x73: // n/a
			break;
		case 0x74: // n/a
			break;
		case 0x75: // n/a
			break;
		case 0x76: // n/a
			break;
		case 0x77: // n/a
			break;
		case 0x78: // SEI - Set Interrupt Disable, Implied
			setIrqDisableFlag();
			break;
		case 0x79: // n/a
			break;
		case 0x7a: // n/a
			break;
		case 0x7b: // n/a
			break;
		case 0x7c: // n/a
			break;
		case 0x7d: // n/a
			break;
		case 0x7e: // n/a
			break;
		case 0x7f: // n/a
			break;

		case 0x80: // n/a
			break;
		case 0x81: // n/a
			break;
		case 0x82: // n/a
			break;
		case 0x83: // n/a
			break;
		case 0x84: // n/a
			break;
		case 0x85: // n/a
			break;
		case 0x86: // n/a
			break;
		case 0x87: // n/a
			break;
		case 0x88: // DEY - Decrement the Y Register - Implied
			y = --y & 0xff;
			setArithmeticFlags(y);
			break;
		case 0x89: // n/a
			break;
		case 0x8a: // n/a
			break;
		case 0x8b: // n/a
			break;
		case 0x8c: // n/a
			break;
		case 0x8d: // n/a
			break;
		case 0x8e: // n/a
			break;
		case 0x8f: // n/a
			break;

		case 0x90: // n/a
			break;
		case 0x91: // n/a
			break;
		case 0x92: // n/a
			break;
		case 0x93: // n/a
			break;
		case 0x94: // n/a
			break;
		case 0x95: // n/a
			break;
		case 0x96: // n/a
			break;
		case 0x97: // n/a
			break;
		case 0x98: // n/a
			break;
		case 0x99: // n/a
			break;
		case 0x9a: // n/a
			break;
		case 0x9b: // n/a
			break;
		case 0x9c: // n/a
			break;
		case 0x9d: // n/a
			break;
		case 0x9e: // n/a
			break;
		case 0x9f: // n/a
			break;

		case 0xa0: // LDY - Immediate
			y = operands[0];
			setArithmeticFlags(y);
			break;
		case 0xa1: // n/a
			break;
		case 0xa2: // LDX - Immediate
			x = operands[0];
			setArithmeticFlags(x);
			break;
		case 0xa3: // n/a
			break;
		case 0xa4: // n/a
			break;
		case 0xa5: // n/a
			break;
		case 0xa6: // n/a
			break;
		case 0xa7: // n/a
			break;
		case 0xa8: // n/a
			break;
		case 0xa9: // LDA - Immediate
			a = operands[0];
			setArithmeticFlags(a);
			break;
		case 0xaa: // n/a
			break;
		case 0xab: // n/a
			break;
		case 0xac: // n/a
			break;
		case 0xad: // n/a
			break;
		case 0xae: // n/a
			break;
		case 0xaf: // n/a
			break;

		case 0xb0: // n/a
			break;
		case 0xb1: // n/a
			break;
		case 0xb2: // n/a
			break;
		case 0xb3: // n/a
			break;
		case 0xb4: // n/a
			break;
		case 0xb5: // n/a
			break;
		case 0xb6: // n/a
			break;
		case 0xb7: // n/a
			break;
		case 0xb8: // CLV - Clear Overflow Flag, Implied
			clearOverflowFlag();
			break;
		case 0xb9: // n/a
			break;
		case 0xba: // n/a
			break;
		case 0xbb: // n/a
			break;
		case 0xbc: // n/a
			break;
		case 0xbd: // n/a
			break;
		case 0xbe: // n/a
			break;
		case 0xbf: // n/a
			break;

		case 0xc0: // CPY - Immediate
			cmp(y, operands[0]);
			break;
		case 0xc1: // n/a
			break;
		case 0xc2: // n/a
			break;
		case 0xc3: // n/a
			break;
		case 0xc4: // n/a
			break;
		case 0xc5: // n/a
			break;
		case 0xc6: // n/a
			break;
		case 0xc7: // n/a
			break;
		case 0xc8: // INY - Increment the Y Register - Implied
			y = ++y & 0xff;
			setArithmeticFlags(y);
			break;
		case 0xc9: // CMP - Immediate
			cmp(a, operands[0]);
			break;
		case 0xca: // DEX - Decrement the X Register - Implied
			x = --x & 0xff;
			setArithmeticFlags(x);
			break;
		case 0xcb: // n/a
			break;
		case 0xcc: // n/a
			break;
		case 0xcd: // n/a
			break;
		case 0xce: // n/a
			break;
		case 0xcf: // n/a
			break;

		case 0xd0: // n/a
			break;
		case 0xd1: // n/a
			break;
		case 0xd2: // n/a
			break;
		case 0xd3: // n/a
			break;
		case 0xd4: // n/a
			break;
		case 0xd5: // n/a
			break;
		case 0xd6: // n/a
			break;
		case 0xd7: // n/a
			break;
		case 0xd8: // CLD - Clear Decimal Mode, Implied
			clearDecimalModeFlag();
			break;
		case 0xd9: // n/a
			break;
		case 0xda: // n/a
			break;
		case 0xdb: // n/a
			break;
		case 0xdc: // n/a
			break;
		case 0xdd: // n/a
			break;
		case 0xde: // n/a
			break;
		case 0xdf: // n/a
			break;

		case 0xe0: // CPX - Immediate
			cmp(x, operands[0]);
			break;
		case 0xe1: // n/a
			break;
		case 0xe2: // n/a
			break;
		case 0xe3: // n/a
			break;
		case 0xe4: // n/a
			break;
		case 0xe5: // n/a
			break;
		case 0xe6: // n/a
			break;
		case 0xe7: // n/a
			break;
		case 0xe8: // INX - Increment X Register - Implied
			x = ++x & 0xff;
			setArithmeticFlags(x);
			break;
		case 0xe9: // SBC - Subtract with Carry (Borrow) - Immediate
			a = sbc(a, operands[0]);
			setArithmeticFlags(a);
			break;
		case 0xea: // NOP
			break;
		case 0xeb: // n/a
			break;
		case 0xec: // n/a
			break;
		case 0xed: // n/a
			break;
		case 0xee: // n/a
			break;
		case 0xef: // n/a
			break;

		case 0xf0: // n/a
			break;
		case 0xf1: // n/a
			break;
		case 0xf2: // n/a
			break;
		case 0xf3: // n/a
			break;
		case 0xf4: // n/a
			break;
		case 0xf5: // n/a
			break;
		case 0xf6: // n/a
			break;
		case 0xf7: // n/a
			break;
		case 0xf8: // SED - Set Decimal Flag, Implied
			setDecimalModeFlag();
			break;
		case 0xf9: // n/a
			break;
		case 0xfa: // n/a
			break;
		case 0xfb: // n/a
			break;
		case 0xfc: // n/a
			break;
		case 0xfd: // n/a
			break;
		case 0xfe: // n/a
			break;
		case 0xff: // n/a
			break;
		}
	}

	/**
	 * Add with Carry, used by all addressing mode implementations of ADC.
	 * As a side effect, this will set the
	 *
	 * @param acc  The current value of the accumulator
	 * @param operand  The operand
	 * @return
	 */
	public int adc(int acc, int operand) {
		int result = operand + acc + (carryFlag ? 1 : 0);
		int carry = (operand & 0x7f) + (acc & 0x7f) + (carryFlag ? 1 : 0);
		setCarryFlag(result > 0xff);
		result = result & 0xff;
		setOverflowFlag(carryFlag ^ ((carry & 0x80) != 0));
		return result;
	}

	public int sbc(int acc, int operand) {
		// Equivalent to ADC of the 2's complement of the operand, minus the /carry
		return adc(acc, (--operand ^ 0xff));
	}

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
	 * @return the negative flag
	 */
	public boolean getNegativeFlag() {
		return negativeFlag;
	}

	/**
	 * @param register the register value to test for negativity
	 */
	public void setNegativeFlag(int register) {
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

	/**
	 * @value The value of the Process Status Register bits to be set.
	 */
	public void setProcessorStatus(int value) {
		if ((value&P_CARRY) != 0)
			setCarryFlag();
		else
			clearCarryFlag();

		if ((value&P_ZERO) != 0)
			setZeroFlag();
		else
			clearZeroFlag();

		if ((value&P_IRQ_DISABLE) != 0)
			setIrqDisableFlag();
		else
			clearIrqDisableFlag();

		if ((value&P_DECIMAL) != 0)
			setDecimalModeFlag();
		else
			clearDecimalModeFlag();

		if ((value&P_BREAK) != 0)
			setBreakFlag();
		else
			clearBreakFlag();

		if ((value&P_OVERFLOW) != 0)
			setOverflowFlag();
		else
			clearOverflowFlag();

		if ((value&P_NEGATIVE) != 0)
			setNegativeFlag();
		else
			clearNegativeFlag();
	}

	/**
	 * @returns The value of the Process Status Register, as a byte.
	 */
	public int getProcessorStatus() {
		int status = 0x20;
		if (getCarryFlag())       { status |= P_CARRY;       }
		if (getZeroFlag())        { status |= P_ZERO;        }
		if (getIrqDisableFlag())  { status |= P_IRQ_DISABLE; }
		if (getDecimalModeFlag()) { status |= P_DECIMAL;     }
		if (getBreakFlag())       { status |= P_BREAK;       }
		if (getOverflowFlag())    { status |= P_OVERFLOW;    }
		if (getNegativeFlag())    { status |= P_NEGATIVE;    }
		return status;
	}

	/**
	 * @return A string representing the current status register state.
	 */
	public String statusRegisterString() {
		StringBuffer sb = new StringBuffer();
		sb.append("(N:" + (getNegativeFlag() ? '1' : '0') + ", ");
		sb.append("V:" + (getOverflowFlag() ? '1' : '0') + ", ");
		sb.append("B:" + (getBreakFlag() ? '1' : '0') + ", ");
		sb.append("D:" + (getDecimalModeFlag() ? '1' : '0') + ", ");
		sb.append("I:" + (getIrqDisableFlag() ? '1' : '0') + ", ");
		sb.append("Z:" + (getZeroFlag() ? '1' : '0') + ", ");
		sb.append("C:" + (getCarryFlag() ? '1' : '0') + ")");
		return sb.toString();
	}

	/**
	 * Returns a string representing the CPU state.
	 */
	public String toString() {
		String opcode = CpuUtils.opcode(ir, operands[0], operands[1]);
		StringBuffer sb = new StringBuffer(String.format("$%04X", addr) +
		                                   "   ");
		sb.append(String.format("%-14s", opcode));
		sb.append("A="  + String.format("$%02X", a)  + "  ");
		sb.append("X="  + String.format("$%02X", x)  + "  ");
		sb.append("Y="  + String.format("$%02X", y)  + "  ");
		sb.append("PC=" + String.format("$%04X", pc)+ "  ");
		sb.append("P="  + statusRegisterString());
		return sb.toString();
	}

	/**
	 * Push an item onto the stack, and decrement the stack counter.
	 * Silently fails to push onto the stack if SP is
	 */
	void stackPush(int data) {
		bus.write(0x100+sp, data);

		if (sp == 0)
			sp = 0xff;
		else
			--sp;
	}


	/**
	 * Pre-increment the stack pointer, and return the top of the stack.
	 */
	int stackPop() {
		if (sp == 0xff)
			sp = 0x00;
		else
			++sp;

		int data = bus.read(0x100+sp);

		return data;
	}

	/**
	 * Peek at the value currently at the top of the stack
	 */
	int stackPeek() {
		return bus.read(0x100+sp+1);
	}

	/*
	 * Increment the PC, rolling over if necessary.
	 */
	protected void incProgramCounter() {
		if (pc == 0xffff) {
			pc = 0;
		} else {
			++pc;
		}
	}

}