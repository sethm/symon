package com.loomcom.symon.exceptions;


/**
 * Superclass for all symon Exceptions.
 */
public class SymonException extends Exception {
    public SymonException(String msg) {
        super(msg);
    }
    public SymonException() {
        super();
    }
}
