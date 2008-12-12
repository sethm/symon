package com.loomcom.lm6502;

import junit.framework.TestCase;

public class CpuUtilsTest extends TestCase {

	public void testAddress() {
		assertEquals(0xf1ea, CpuUtils.address(0xea, 0xf1));
		assertEquals(0x00ea, CpuUtils.address(0xea, 0x00));
		assertEquals(0xf100, CpuUtils.address(0x00, 0xf1));
		assertEquals(0x1234, CpuUtils.address(0x34, 0x12));
		assertEquals(0xffff, CpuUtils.address(0xff, 0xff));
	}

}
