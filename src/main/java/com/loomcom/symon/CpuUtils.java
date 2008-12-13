package com.loomcom.symon;

import com.loomcom.symon.InstructionTable.Mode;

public class CpuUtils {

	/**
	 * Given two bytes, return an address.  
	 */
	public static int address(int lowByte, int hiByte) {
		return ((hiByte<<8)|lowByte);
	}

	/**
	 * Given an opcode and its operands, return a formatted name.
	 * 
	 * @param opcode
	 * @param operands
	 * @return
	 */
	public static String opcode(int opcode, int op1, int op2) {
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
