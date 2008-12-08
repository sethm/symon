package com.loomcom.lm6502;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for the j6502 Simulator class.
 */
public class SimulatorTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public SimulatorTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(SimulatorTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testSimulator() {
        assertTrue(true);
    }
}
