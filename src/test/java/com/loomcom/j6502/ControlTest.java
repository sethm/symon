package com.loomcom.j6502;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for the j6502 control class.
 */
public class ControlTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ControlTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(ControlTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testControl() {
        assertTrue(true);
    }
}
