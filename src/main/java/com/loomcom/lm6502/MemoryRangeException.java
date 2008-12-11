package com.loomcom.lm6502;

/**
 * Exception that will be thrown if devices conflict in the IO map.
 */
public class MemoryRangeException extends Exception {
	public MemoryRangeException(String msg) {
		super(msg);
	}
}