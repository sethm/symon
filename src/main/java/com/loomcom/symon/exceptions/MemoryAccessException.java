package com.loomcom.symon.exceptions;

/**
 * Exception that will be thrown if access to memory or IO cannot be
 * accessed.
 */
public class MemoryAccessException extends Exception {
  public MemoryAccessException(String msg) {
    super(msg);
  }
}
