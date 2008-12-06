package com.loomcom.j6502;

/**
 * Main 6502 CPU Simulation.
 */
public class Cpu {

    /**
     * The Address Decoder responsible for routing memory
     * read/write requests to the correct IO devices.  Any
     * simulated device can be 
     */
    AddressDecoder m_adc;

    /**
     * The Program Counter.
     */
    int m_pc;

    /**
     * The system stack pointer.
     */
    int m_sp;

    /**
     * The internal 
     */

    public Cpu() {
	reset();
    }

    /**
     * Reset the CPU to known initial values.
     */
    public void reset() {
    }

    /** 
     * Trigger a maskable interrupt.
     */
    public void interrupt() {
    }

    /**
     * Trigger a nonmaskable interrupt.
     */
    public void nmiInterrupt() {
    }
    
}

