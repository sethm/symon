package com.loomcom.symon.exceptions;

public class FifoUnderrunException extends Exception {
  public FifoUnderrunException(String msg) {
    super(msg);
  }
}