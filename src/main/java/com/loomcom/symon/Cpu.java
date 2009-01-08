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

  // NMI vector
  public static final int IRQ_VECTOR_L  = 0xfffa;
  public static final int IRQ_VECTOR_H  = 0xfffb;
  // Reset vector
  public static final int RST_VECTOR_L  = 0xfffc;
  public static final int RST_VECTOR_H  = 0xfffd;
  // IRQ vector
  public static final int NMI_VECTOR_L  = 0xfffe;
  public static final int NMI_VECTOR_H  = 0xffff;

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

  /* Internal scratch space */
  private int lo = 0, hi = 0;  // Used in address calculation
  private int j  = 0, k  = 0;  // Used for temporary storage

	/* Unimplemented instruction flag */
	private boolean opTrap = false;

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

		// Clear illegal opcode trap.
		opTrap = false;
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
    // Store the address from which the IR was read, for debugging
    addr = pc;

    // Fetch memory location for this instruction.
    ir = bus.read(pc);

    // Increment PC
    incrementPC();

		// Clear the illegal opcode trap.
		clearOpTrap();

    // Decode the instruction and operands
    int size = Cpu.instructionSizes[ir];
    for (int i = 0; i < size-1; i++) {
      operands[i] = bus.read(pc);
      // Increment PC after reading
      incrementPC();
    }

    // Execute
    switch(ir) {

    case 0x00: // BRK - Force Interrupt - Implied
      if (!getIrqDisableFlag()) {
        // Set the break flag before pushing.
        setBreakFlag();
        // Push program counter + 2 onto the stack
        stackPush((pc+2 >> 8) & 0xff); // PC high byte
        stackPush(pc+2 & 0xff);        // PC low byte
        stackPush(getProcessorStatus());
        // Set the Interrupt Disabled flag.  RTI will clear it.
        setIrqDisableFlag();
        // Load interrupt vector address into PC
        pc = address(bus.read(IRQ_VECTOR_L), bus.read(IRQ_VECTOR_H));
      }
      break;
    case 0x01: // TODO: implement
      break;
    case 0x05: // ORA - Logical Inclusive OR - Zero Page
      a |= bus.read(operands[0]);
      setArithmeticFlags(a);
      break;
    case 0x06: // ASL - Arithmetic Shift Left - Zero Page
      j = bus.read(operands[0]);
      k = asl(j);
      bus.write(operands[0], k);
      setArithmeticFlags(k);
      break;
    case 0x08: // PHP - Push Processor Status - Implied
      stackPush(getProcessorStatus());
      break;
    case 0x09: // ORA - Logical Inclusive OR - Immediate
      a |= operands[0];
      setArithmeticFlags(a);
      break;
    case 0x0a: // ASL - Arithmetic Shift Left - Accumulator
			a = asl(a);
			setArithmeticFlags(a);
      break;
    case 0x0d: // ORA - Logical Inclusive OR - Absolute
      j = bus.read(address(operands[0], operands[1]));
      a |= j;
      setArithmeticFlags(a);
      break;
    case 0x0e: // ASL - Arithmetic Shift Left - Absolute
      j = bus.read(address(operands[0], operands[1]));
      k = asl(j);
      bus.write(address(operands[0], operands[1]), k);
      setArithmeticFlags(k);
      break;

    case 0x10: // BPL - Branch if Positive - Relative
      if (!getNegativeFlag()) {
        pc = relAddress(operands[0]);
      }
      break;
    case 0x11: // TODO: implement
      break;
    case 0x15: // ORA - Logical Inclusive OR - Zero Page,X
      a |= bus.read(zpxAddress(operands[0]));
      setArithmeticFlags(a);
      break;
    case 0x16: // ASL - Arithmetic Shift Left - Zero Page,X
      j = bus.read(zpxAddress(operands[0]));
      k = asl(j);
      bus.write(zpxAddress(operands[0]), k);
      setArithmeticFlags(k);
      break;
    case 0x18: // CLC - Clear Carry Flag - Implied
      clearCarryFlag();
      break;
    case 0x19: // ORA - Logical Inclusive Or - Absolute,Y
      j = bus.read(yAddress(operands[0], operands[1]));
      a |= j;
      setArithmeticFlags(a);
      break;
    case 0x1d: // ORA - Logical Inclusive OR - Absolute,X
      j = bus.read(xAddress(operands[0], operands[1]));
      a |= j;
      setArithmeticFlags(a);
      break;
    case 0x1e: // ASL - Arithmetic Shift Left - Absolute,X
      j = bus.read(xAddress(operands[0], operands[1]));
      k = asl(j);
      bus.write(xAddress(operands[0], operands[1]), k);
      setArithmeticFlags(k);
      break;

    case 0x20: // JSR - Jump to Subroutine - $20
      stackPush((pc-1 >> 8) & 0xff); // PC high byte
      stackPush(pc-1 & 0xff);        // PC low byte
      pc = address(operands[0], operands[1]);
      break;
    case 0x21: // TODO: implement
      break;
    case 0x24: // BIT - Bit Test - Zero Page
      j = bus.read(operands[0]);
      k = a & j;
      setZeroFlag(k == 0);
      setNegativeFlag((k & 0x80) != 0);
      setOverflowFlag((k & 0x40) != 0);
      break;
    case 0x25: // AND - Logical AND - Zero Page
      j = bus.read(operands[0]);
      a &= j;
      setArithmeticFlags(a);
      break;
    case 0x26: // ROL - Rotate Left - Zero Page
      j = bus.read(operands[0]);
      k = rol(j);
      bus.write(operands[0], k);
      setArithmeticFlags(k);
      break;
    case 0x28: // PLP - Pull Processor Status - Implied
      setProcessorStatus(stackPop());
      break;
    case 0x29: // AND - Logical AND - Immediate
      a &= operands[0];
      setArithmeticFlags(a);
      break;
    case 0x2a: // ROL - Rotate Left - Accumulator
			a = rol(a);
			setArithmeticFlags(a);
      break;
    case 0x2c: // BIT - Bit Test - Absolute
      j = bus.read(address(operands[0], operands[1]));
      k = a & j;
      setZeroFlag(k == 0);
      setNegativeFlag((k & 0x80) != 0);
      setOverflowFlag((k & 0x40) != 0);
      break;
    case 0x2d: // AND - Logical AND - Absolute
      j = bus.read(address(operands[0], operands[1]));
      a &= j;
      setArithmeticFlags(a);
      break;
    case 0x2e: // ROL - Rotate Left - Absolute
      j = bus.read(address(operands[0], operands[1]));
      k = rol(j);
      bus.write(address(operands[0], operands[1]), k);
      setArithmeticFlags(k);
      break;

    case 0x30: // BMI - Branch if Minus - Relative
      if (getNegativeFlag()) {
        pc = relAddress(operands[0]);
      }
      break;
    case 0x31: // TODO: implement
      break;
    case 0x35: // AND - Logical AND - Zero Page,X
      j = bus.read(zpxAddress(operands[0]));
      a &= j;
      setArithmeticFlags(a);
      break;
    case 0x36: // ROL - Rotate Shift Left - Zero Page,X
      j = bus.read(zpxAddress(operands[0]));
      k = rol(j);
      bus.write(zpxAddress(operands[0]), k);
      setArithmeticFlags(k);
      break;
    case 0x38: // SEC - Set Carry Flag - Implied
      setCarryFlag();
      break;
    case 0x39: // AND - Logical AND - Absolute,Y
      j = bus.read(yAddress(operands[0], operands[1]));
      a &= j;
      setArithmeticFlags(a);
      break;
    case 0x3d: // AND - Logical AND - Absolute,X
      j = bus.read(xAddress(operands[0], operands[1]));
      a &= j;
      setArithmeticFlags(a);
      break;
    case 0x3e: // ROL - Rotate Left - Absolute,X
      j = bus.read(xAddress(operands[0], operands[1]));
      k = rol(j);
      bus.write(xAddress(operands[0], operands[1]), k);
      setArithmeticFlags(k);
      break;

    case 0x40: // RTI - Return from Interrupt - Implied
      setProcessorStatus(stackPop());
      lo = stackPop();
      hi = stackPop();
      setProgramCounter(address(lo, hi));
      break;
    case 0x41: // TODO: implement
      break;
    case 0x45: // EOR - Exclusive OR - Zero Page
      j = bus.read(operands[0]);
      a ^= j;
      setArithmeticFlags(a);
      break;
    case 0x46: // LSR - Logical Shift Right - Zero Page
      j = bus.read(operands[0]);
      k = lsr(j);
      bus.write(operands[0], k);
      setArithmeticFlags(k);
      break;
    case 0x48: // PHA - Push Accumulator - Implied
      stackPush(a);
      break;
    case 0x49: // EOR - Exclusive OR - Immediate
      a ^= operands[0];
      setArithmeticFlags(a);
      break;
    case 0x4a: // LSR - Logical Shift Right - Accumulator
			a = lsr(a);
			setArithmeticFlags(a);
      break;
    case 0x4c: // JMP - Jump - Absolute
      pc = address(operands[0], operands[1]);
      break;
    case 0x4d: // EOR - Exclusive OR - Absolute
      a ^= bus.read(address(operands[0], operands[1]));
      setArithmeticFlags(a);
      break;
    case 0x4e: // LSR - Logical Shift Right - Absolute
      j = bus.read(address(operands[0], operands[1]));
      k = lsr(j);
      bus.write(address(operands[0], operands[1]), k);
      setArithmeticFlags(k);
      break;

    case 0x50: // BVC - Branch if Overflow Clear - Relative
      if (!getOverflowFlag()) {
        pc = relAddress(operands[0]);
      }
      break;
    case 0x51: // TODO: implement
      break;
    case 0x55: // EOR - Exclusive OR - Zero Page,X
      a ^= bus.read(zpxAddress(operands[0]));
      setArithmeticFlags(a);
      break;
    case 0x56: // LSR - Logical Shift Right - Zero Page,X
      j = bus.read(zpxAddress(operands[0]));
      k = lsr(j);
      bus.write(zpxAddress(operands[0]), k);
      setArithmeticFlags(k);
      break;
    case 0x58: // CLI - Clear Interrupt Disable - Implied
      clearIrqDisableFlag();
      break;
    case 0x59: // EOR - Exclusive OR - Absolute,Y
      a ^= bus.read(yAddress(operands[0], operands[1]));
      setArithmeticFlags(a);
      break;
    case 0x5d: // EOR - Exclusive OR - Absolute,X
      a ^= bus.read(xAddress(operands[0], operands[1]));
      setArithmeticFlags(a);
      break;
    case 0x5e: // LSR - Logical Shift Right - Absolute,X
      j = bus.read(xAddress(operands[0], operands[1]));
      k = lsr(j);
      bus.write(xAddress(operands[0], operands[1]), k);
      setArithmeticFlags(k);
      break;

    case 0x60: // RTS - Return from Subroutine - Implied
      lo = stackPop();
      hi = stackPop();
      setProgramCounter((address(lo, hi) + 1) & 0xffff);
      break;
    case 0x61: // TODO: implement
      break;
    case 0x65: // ADC - Add with Carry - Zero Page
      j = bus.read(operands[0]);
      if (decimalModeFlag) {
        a = adcDecimal(a, j);
      } else {
        a = adc(a, j);
      }
      break;
    case 0x66: // ROR - Rotate Right - Zero Page
      j = bus.read(operands[0]);
      k = ror(j);
      bus.write(operands[0], k);
      setArithmeticFlags(k);
      break;
    case 0x68: // PLA - Pull Accumulator - Implied
      a = stackPop();
      setArithmeticFlags(a);
      break;
    case 0x69: // ADC - Add with Carry - Immediate
      if (decimalModeFlag) {
        a = adcDecimal(a, operands[0]);
      } else {
        a = adc(a, operands[0]);
      }
      break;
    case 0x6a: // ROR - Rotate Right - Accumulator
			a = ror(a);
			setArithmeticFlags(a);
      break;
    case 0x6c: // JMP - Jump - Indirect
      lo = address(operands[0], operands[1]); // Address of low byte
      hi = lo+1; // Address of high byte

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
    case 0x6d: // ADC - Add with Carry - Absolute
      j = bus.read(address(operands[0], operands[1]));
      if (decimalModeFlag) {
        a = adcDecimal(a, j);
      } else {
        a = adc(a, j);
      }
      break;
    case 0x6e: // ROR - Rotate Right - Absolute
      j = bus.read(address(operands[0], operands[1]));
      k = ror(j);
      bus.write(address(operands[0], operands[1]), k);
      setArithmeticFlags(k);
      break;

    case 0x70: // BVS - Branch if Overflow Set - Relative
      if (getOverflowFlag()) {
        pc = relAddress(operands[0]);
      }
      break;
    case 0x71: // TODO: implement
      break;
    case 0x75: // ADC - Add with Carry - Zero Page,X
      j = bus.read(zpxAddress(operands[0]));
      if (decimalModeFlag) {
        a = adcDecimal(a, j);
      } else {
        a = adc(a, j);
      }
      break;
    case 0x76: // ROR - Rotate Right - Zero Page,X
      j = bus.read(zpxAddress(operands[0]));
      k = ror(j);
      bus.write(zpxAddress(operands[0]), k);
      setArithmeticFlags(k);
      break;
    case 0x78: // SEI - Set Interrupt Disable - Implied
      setIrqDisableFlag();
      break;
    case 0x79: // ADC - Add with Carry - Absolute,Y
      j = bus.read(yAddress(operands[0], operands[1]));
      if (decimalModeFlag) {
        a = adcDecimal(a, j);
      } else {
        a = adc(a, j);
      }
      break;
    case 0x7d: // ADC - Add with Carry - Absolute,X
      j = bus.read(xAddress(operands[0], operands[1]));
      if (decimalModeFlag) {
        a = adcDecimal(a, j);
      } else {
        a = adc(a, j);
      }
      break;
    case 0x7e: // ROR - Rotate Right - Absolute,X
      j = bus.read(xAddress(operands[0], operands[1]));
      k = ror(j);
      bus.write(xAddress(operands[0], operands[1]), k);
      setArithmeticFlags(k);
      break;

    case 0x81: // TODO: implement
      break;
    case 0x84: // STY - Store Y Register - Zero Page
      bus.write(operands[0], y);
      setArithmeticFlags(y);
      break;
    case 0x85: // STA - Store Accumulator - Zero Page
      bus.write(operands[0], a);
      setArithmeticFlags(a);
      break;
    case 0x86: // STX - Store X Register - Zero Page
      bus.write(operands[0], x);
      setArithmeticFlags(x);
      break;
    case 0x88: // DEY - Decrement Y Register - Implied
      y = --y & 0xff;
      setArithmeticFlags(y);
      break;
    case 0x8a: // TXA - Transfer X to Accumulator - Implied
      a = x;
      setArithmeticFlags(a);
      break;
    case 0x8c: // STY - Store Y Register - Absolute
      bus.write(address(operands[0], operands[1]), y);
      setArithmeticFlags(y);
      break;
    case 0x8d: // STA - Store Accumulator - Absolute
      bus.write(address(operands[0], operands[1]), a);
      setArithmeticFlags(a);
      break;
    case 0x8e: // STX - Store X Register - Absolute
      bus.write(address(operands[0], operands[1]), x);
      setArithmeticFlags(x);
      break;

    case 0x90: // BCC - Branch if Carry Clear - Relative
      if (!getCarryFlag()) {
        pc = relAddress(operands[0]);
      }
      break;
    case 0x91: // TODO: implement
      break;
    case 0x94: // STY - Store Y Register - Zero Page,X
      bus.write(zpxAddress(operands[0]), y);
      setArithmeticFlags(y);
      break;
    case 0x95: // STA - Store Accumulator - Zero Page,X
      bus.write(zpxAddress(operands[0]), a);
      setArithmeticFlags(a);
      break;
    case 0x96: // STX - Store X Register - Zero Page,Y
      bus.write(zpyAddress(operands[0]), x);
      setArithmeticFlags(x);
      break;
    case 0x98: // TYA - Transfer Y to Accumulator - Implied
      a = y;
      setArithmeticFlags(a);
      break;
    case 0x99: // STA - Store Accumulator - Absolute,Y
      bus.write(yAddress(operands[0], operands[1]), a);
      setArithmeticFlags(a);
      break;
    case 0x9a: // TXS - Transfer X to Stack Pointer - Implied
      setStackPointer(x);
      break;
    case 0x9d: // STA - Store Accumulator - Absolute,X
      bus.write(xAddress(operands[0], operands[1]), a);
      setArithmeticFlags(a);
      break;

    case 0xa0: // LDY - Load Y Register - Immediate
      y = operands[0];
      setArithmeticFlags(y);
      break;
    case 0xa1: // TODO: implement
      break;
    case 0xa2: // LDX - Load X Register - Immediate
      x = operands[0];
      setArithmeticFlags(x);
      break;
    case 0xa4: // LDY - Load Y Register - Zero Page
      y = bus.read(operands[0]);
      setArithmeticFlags(y);
      break;
    case 0xa5: // LDA - Load Accumulator - Zero Page
      a = bus.read(operands[0]);
      setArithmeticFlags(a);
      break;
    case 0xa6: // LDX - Load X Register - Zero Page
      x = bus.read(operands[0]);
      setArithmeticFlags(x);
      break;
    case 0xa8: // TAY - Transfer Accumulator to Y - Implied
      y = a;
      setArithmeticFlags(y);
      break;
    case 0xa9: // LDA - Load Accumulator - Immediate
      a = operands[0];
      setArithmeticFlags(a);
      break;
    case 0xaa: // TAX - Transfer Accumulator to X - Implied
      x = a;
      setArithmeticFlags(x);
      break;
    case 0xac: // LDY - Load Y Register - Absolute
      y = bus.read(address(operands[0], operands[1]));
      setArithmeticFlags(y);
      break;
    case 0xad: // LDA - Load Accumulator - Absolute
      a = bus.read(address(operands[0], operands[1]));
      setArithmeticFlags(a);
      break;
    case 0xae: // LDX - Load X Register - Absolute
      x = bus.read(address(operands[0], operands[1]));
      setArithmeticFlags(x);
      break;

    case 0xb0: // BCS - Branch if Carry Set - Relative
      if (getCarryFlag()) {
        pc = relAddress(operands[0]);
      }
      break;
    case 0xb1: // TODO: implement
      break;
    case 0xb4: // LDY - Load Y Register - Zero Page,X
      y = bus.read(zpxAddress(operands[0]));
      setArithmeticFlags(y);
      break;
    case 0xb5: // LDA - Load Accumulator - Zero Page,X
      a = bus.read(zpxAddress(operands[0]));
      setArithmeticFlags(a);
      break;
    case 0xb6: // LDX - Load X Register - Zero Page,Y
      x = bus.read(zpyAddress(operands[0]));
      setArithmeticFlags(x);
      break;
    case 0xb8: // CLV - Clear Overflow Flag - Implied
      clearOverflowFlag();
      break;
    case 0xb9: // LDA - Load Accumulator - Absolute,Y
      a = bus.read(yAddress(operands[0], operands[1]));
      setArithmeticFlags(a);
      break;
    case 0xba: // TSX - Transfer Stack Pointer to X - Implied
      x = getStackPointer();
      setArithmeticFlags(x);
      break;
    case 0xbc: // LDY - Load Y Register - Absolute,X
      y = bus.read(xAddress(operands[0], operands[1]));
      setArithmeticFlags(y);
      break;
    case 0xbd: // LDA - Load Accumulator - Absolute,X
      a = bus.read(xAddress(operands[0], operands[1]));
      setArithmeticFlags(a);
      break;
    case 0xbe: // LDX - Load X Register - Absolute,Y
      x = bus.read(yAddress(operands[0], operands[1]));
      setArithmeticFlags(x);
      break;

    case 0xc0: // CPY - Compare Y Register - Immediate
      cmp(y, operands[0]);
      break;
    case 0xc1: // TODO: implement
      break;
    case 0xc4: // CPY - Compare Y Register - Zero Page
      cmp(y, bus.read(operands[0]));
      break;
    case 0xc5: // CMP - Compare Accumulator - Zero Page
      cmp(a, bus.read(operands[0]));
      break;
    case 0xc6: // DEC - Decrement Memory - Zero Page
      j = bus.read(operands[0]);
      k = --j & 0xff;
      bus.write(operands[0], k);
      setArithmeticFlags(k);
      break;
    case 0xc8: // INY - Increment Y Register - Implied
      y = ++y & 0xff;
      setArithmeticFlags(y);
      break;
    case 0xc9: // CMP - Immediate
      cmp(a, operands[0]);
      break;
    case 0xca: // DEX - Decrement X Register - Implied
      x = --x & 0xff;
      setArithmeticFlags(x);
      break;
    case 0xcc: // CPY - Compare Y Register - Absolute
      cmp(y, bus.read(address(operands[0], operands[1])));
      break;
    case 0xcd: // CMP - Compare Accumulator - Absolute
      cmp(a, bus.read(address(operands[0], operands[1])));
      break;
    case 0xce: // DEC - Decrement Memory - Absolute
      j = bus.read(address(operands[0], operands[1]));
      k = --j & 0xff;
      bus.write(address(operands[0], operands[1]), k);
      setArithmeticFlags(k);
      break;

    case 0xd0: // BNE - Branch if Not Equal to Zero - Relative
      if (!getZeroFlag()) {
        pc = relAddress(operands[0]);
      }
      break;
    case 0xd1: // TODO: implement
      break;
    case 0xd5: // CMP - Compare Accumulator - Zero Page,X
      cmp(a, bus.read(zpxAddress(operands[0])));
      break;
    case 0xd6: // DEC - Decrement Memory - Zero Page, X
      j = bus.read(zpxAddress(operands[0]));
      k = --j & 0xff;
      bus.write(zpxAddress(operands[0]), k);
      setArithmeticFlags(k);
      break;
    case 0xd8: // CLD - Clear Decimal Mode - Implied
      clearDecimalModeFlag();
      break;
    case 0xd9: // CMP - Compare Accumulator - Absolute,Y
      cmp(a, bus.read(yAddress(operands[0], operands[1])));
      break;
    case 0xdd: // CMP - Compare Accumulator - Absolute,X
      cmp(a, bus.read(xAddress(operands[0], operands[1])));
      break;
    case 0xde: // DEC - Decrement Memory - Absolute,X
      j = bus.read(xAddress(operands[0], operands[1]));
      k = --j & 0xff;
      bus.write(xAddress(operands[0], operands[1]), k);
      setArithmeticFlags(k);
      break;

    case 0xe0: // CPX - Compare X Register - Immediate
      cmp(x, operands[0]);
      break;
    case 0xe1: // TODO: implement
      break;
    case 0xe4: // CPX - Compare X Register - Zero Page
      cmp(x, bus.read(operands[0]));
      break;
    case 0xe5: // SBC - Subtract with Carry (Borrow) - Zero Page
      j = bus.read(operands[0]);
      if (decimalModeFlag) {
        a = sbcDecimal(a, j);
      } else {
        a = sbc(a, j);
      }
      break;
    case 0xe6: // INC - Increment Memory - Zero Page
      j = bus.read(operands[0]);
      k = ++j & 0xff;
      bus.write(operands[0], k);
      setArithmeticFlags(k);
      break;
    case 0xe8: // INX - Increment X Register - Implied
      x = ++x & 0xff;
      setArithmeticFlags(x);
      break;
    case 0xe9: // SBC - Subtract with Carry (Borrow) - Immediate
      if (decimalModeFlag) {
        a = sbcDecimal(a, operands[0]);
      } else {
        a = sbc(a, operands[0]);
      }
      break;
    case 0xea: // NOP
      // Do nothing.
      break;
    case 0xec: // CPX - Compare X Register - Absolute
      cmp(x, bus.read(address(operands[0], operands[1])));
      break;
    case 0xed: // SBC - Subtract with Carry - Absolute
      j = bus.read(address(operands[0], operands[1]));
      if (decimalModeFlag) {
        a = sbcDecimal(a, j);
      } else {
        a = sbc(a, j);
      }
      break;
    case 0xee: // INC - Increment Memory - Absolute
      j = bus.read(address(operands[0], operands[1]));
      k = ++j & 0xff;
      bus.write(address(operands[0], operands[1]), k);
      setArithmeticFlags(k);
      break;

    case 0xf0: // BEQ - Branch if Equal to Zero - Relative
      if (getZeroFlag()) {
        pc = relAddress(operands[0]);
      }
      break;
    case 0xf1: // TODO: implement
      break;
    case 0xf5: // SBC - Subtract with Carry - Zero Page,X
      j = bus.read(zpxAddress(operands[0]));
      if (decimalModeFlag) {
        a = sbcDecimal(a, j);
      } else {
        a = sbc(a, j);
      }
      break;
    case 0xf6: // INC - Increment Memory Location - Zero Page,X
      j = bus.read(zpxAddress(operands[0]));
      k = ++j & 0xff;
      bus.write(zpxAddress(operands[0]), k);
      setArithmeticFlags(k);
      break;
    case 0xf8: // SED - Set Decimal Flag - Implied
      setDecimalModeFlag();
      break;
    case 0xf9: // SBC - Subtract with Carry - Absolute,Y
      j = bus.read(yAddress(operands[0], operands[1]));
      if (decimalModeFlag) {
        a = sbcDecimal(a, j);
      } else {
        a = sbc(a, j);
      }
      break;
    case 0xfd: // SBC - Subtract with Carry - Absolute,X
      j = bus.read(xAddress(operands[0], operands[1]));
      if (decimalModeFlag) {
        a = sbcDecimal(a, j);
      } else {
        a = sbc(a, j);
      }
      break;
    case 0xfe: // INC - Increment Memory Location - Absolute,X
      j = bus.read(xAddress(operands[0], operands[1]));
      k = ++j & 0xff;
      bus.write(xAddress(operands[0], operands[1]), k);
      setArithmeticFlags(k);
      break;

			/* Unimplemented Instructions */

    case 0x02:
    case 0x03:
    case 0x04:
		case 0x07:
		case 0x0b:
		case 0x0c:
		case 0x0f:

		case 0x12:
		case 0x13:
		case 0x14:
		case 0x17:
		case 0x1a:
		case 0x1b:
		case 0x1c:
		case 0x1f:

		case 0x22:
		case 0x23:
		case 0x27:
		case 0x2b:
		case 0x2f:

		case 0x32:
		case 0x33:
		case 0x34:
		case 0x37:
		case 0x3a:
		case 0x3b:
		case 0x3c:
		case 0x3f:

		case 0x42:
		case 0x43:
		case 0x44:
		case 0x47:
		case 0x4b:
		case 0x4f:

		case 0x52:
		case 0x53:
		case 0x54:
		case 0x57:
		case 0x5a:
		case 0x5b:
		case 0x5c:
		case 0x5f:

		case 0x62:
		case 0x63:
		case 0x64:
		case 0x67:
		case 0x6b:
		case 0x6f:

		case 0x72:
		case 0x73:
		case 0x74:
		case 0x77:
		case 0x7a:
		case 0x7b:
		case 0x7c:
		case 0x7f:

		case 0x80:
		case 0x82:
		case 0x83:
		case 0x87:
		case 0x89:
		case 0x8b:
		case 0x8f:

		case 0x92:
		case 0x93:
		case 0x97:
		case 0x9b:
		case 0x9c:
		case 0x9e:
		case 0x9f:

		case 0xa3:
		case 0xa7:
		case 0xab:
		case 0xaf:

		case 0xb2:
		case 0xb3:
		case 0xb7:
		case 0xbb:
		case 0xbf:

		case 0xc2:
		case 0xc3:
		case 0xc7:
		case 0xcb:
		case 0xcf:

		case 0xd2:
		case 0xd3:
		case 0xd4:
		case 0xd7:
		case 0xda:
		case 0xdb:
		case 0xdc:
		case 0xdf:

		case 0xe2:
		case 0xe3:
		case 0xe7:
		case 0xeb:
		case 0xef:

		case 0xf2:
		case 0xf3:
		case 0xf4:
		case 0xf7:
		case 0xfa:
		case 0xfb:
		case 0xfc:
		case 0xff:

			setOpTrap();
			break;

    }
  }

  /**
   * Add with Carry, used by all addressing mode implementations of ADC.
   * As a side effect, this will set the overflow and carry flags as
   * needed.
   *
   * @param acc  The current value of the accumulator
   * @param operand  The operand
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
    StringBuffer sb = new StringBuffer("[");
    sb.append(getNegativeFlag()    ? 'N' : '.');   // Bit 7
    sb.append(getOverflowFlag()    ? 'V' : '.');   // Bit 6
    sb.append("-");                                // Bit 5 (always 1)
    sb.append(getBreakFlag()       ? 'B' : '.');   // Bit 4
    sb.append(getDecimalModeFlag() ? 'D' : '.');   // Bit 3
    sb.append(getIrqDisableFlag()  ? 'I' : '.');   // Bit 2
    sb.append(getZeroFlag()        ? 'Z' : '.');   // Bit 1
    sb.append(getCarryFlag()       ? 'C' : '.');   // Bit 0
    sb.append("]");
    return sb.toString();
  }

  /**
   * Returns a string representing the CPU state.
   */
  public String toString() {
    String opcode = opcode(ir, operands[0], operands[1]);
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
    return ((hiByte<<8)|lowByte);
  }

  /**
   * Given a hi byte and a low byte, return the Absolute,X
   * offset address.
   */
  int xAddress(int lowByte, int hiByte) {
    return (address(lowByte, hiByte)+getXRegister()) & 0xffff;
  }

  /**
   * Given a hi byte and a low byte, return the Absolute,Y
   * offset address.
   */
  int yAddress(int lowByte, int hiByte) {
    return (address(lowByte, hiByte)+getYRegister()) & 0xffff;
  }

  /**
   * Given a single byte, compute the Zero Page,X offset address.
   */
  int zpxAddress(int zp) {
    return (zp+getXRegister())&0xff;
  }

  /**
   * Given a single byte, compute the offset address.
   */
  int relAddress(int offset) {
    // Cast the offset to a signed byte to handle negative offsets
    return (pc + (byte)offset) & 0xffff;
  }

  /**
   * Given a single byte, compute the Zero Page,Y offset address.
   */
  int zpyAddress(int zp) {
    return (zp+getYRegister())&0xff;
  }

  /**
   * Given an opcode and its operands, return a formatted name.
   *
   * @param opcode
   * @param operands
   * @return
   */
  String opcode(int opcode, int op1, int op2) {
    String opcodeName = Cpu.opcodeNames[opcode];
    if (opcodeName == null) { return "???"; }

    StringBuffer sb = new StringBuffer(opcodeName);

    switch (Cpu.instructionModes[opcode]) {
    case ABS:
      sb.append(String.format(" $%04X", address(op1, op2)));
      break;
    case IMM:
      sb.append(String.format(" #$%02X", op1));
    }

    return sb.toString();
  }
}