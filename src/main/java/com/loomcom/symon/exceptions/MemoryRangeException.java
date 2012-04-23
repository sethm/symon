package com.loomcom.symon.exceptions;

/**
 * Exception that will be thrown if devices conflict in the IO map.
 */
public class MemoryRangeException extends SymonException {
  public MemoryRangeException(String msg) {
    super(msg);
  }
}