package com.loomcom.lm6502;

import junit.framework.*;

import java.util.HashMap;
import java.util.Map;

import com.loomcom.lm6502.exceptions.*;

/**
 *
 */
public class MemoryRangeTest extends TestCase {

	public MemoryRangeTest(String testName) {
		super(testName);
	}

	public static Test suite() {
		return new TestSuite(MemoryRangeTest.class);
	}

	public void testConstructorThrowsWhenInvalid() {
		// All of these should raise MemoryRangeException
		doInvalidMemoryRangeTest(0, -1);
		doInvalidMemoryRangeTest(-1, 0);
		doInvalidMemoryRangeTest(0, 0);
		doInvalidMemoryRangeTest(1, 1);
		doInvalidMemoryRangeTest(1, 0);
		doInvalidMemoryRangeTest(2, 1);
	}

	public void testConstructorShouldNotThrow() throws MemoryRangeException {
		// None of these should throw.
		new MemoryRange(0, 1);
		new MemoryRange(0, 2);
		new MemoryRange(1, 2);
		new MemoryRange(2, 10);
	}

	public void testStartAddress() throws MemoryRangeException {
		MemoryRange r = new MemoryRange(0x101, 0x202);
		assertEquals(0x101, r.startAddress());
	}

	public void testEndAddress() throws MemoryRangeException {
		MemoryRange r = new MemoryRange(0x101, 0x202);
		assertEquals(0x202, r.endAddress());
	}

	public void testOverlaps() throws MemoryRangeException {
		MemoryRange a = new MemoryRange(0x0100, 0x01ff);
		MemoryRange b = new MemoryRange(0x0200, 0x02ff);

		MemoryRange c = new MemoryRange(0x01a0, 0x01af); // inside a
		MemoryRange d = new MemoryRange(0x00ff, 0x01a0); // overlaps a
		MemoryRange e = new MemoryRange(0x01a0, 0x02ff); // overlaps a

		MemoryRange f = new MemoryRange(0x02a0, 0x02af); // inside b
		MemoryRange g = new MemoryRange(0x01ff, 0x02a0); // overlaps b
		MemoryRange h = new MemoryRange(0x02a0, 0x03ff); // overlaps b

		assertFalse(a.overlaps(b));
		assertFalse(b.overlaps(a));

		assertTrue(a.overlaps(c));
		assertTrue(c.overlaps(a));

		assertTrue(a.overlaps(d));
		assertTrue(d.overlaps(a));

		assertTrue(a.overlaps(e));
		assertTrue(e.overlaps(a));

		assertTrue(b.overlaps(f));
		assertTrue(f.overlaps(b));

		assertTrue(b.overlaps(g));
		assertTrue(g.overlaps(b));

		assertTrue(b.overlaps(h));
		assertTrue(h.overlaps(b));
	}

	public void testToString() throws MemoryRangeException {
		MemoryRange a = new MemoryRange(0x0abf, 0xff00);
		assertEquals("@0x0abf-0xff00", a.toString());

		MemoryRange b = new MemoryRange(0, 255);
		assertEquals("@0x0000-0x00ff", b.toString());

		MemoryRange c = new MemoryRange(0, 65535);
		assertEquals("@0x0000-0xffff", c.toString());
	}

	public void testIncluded() throws MemoryRangeException {
		MemoryRange a = new MemoryRange(0x0100, 0x0fff);

		assertFalse(a.includes(0x0000));
		assertFalse(a.includes(0x00ff));
		assertTrue(a.includes(0x0100));
		assertTrue(a.includes(0x0fff));
		assertFalse(a.includes(0x1000));
		assertFalse(a.includes(0xffff));
	}

	public void testCompareTo() throws MemoryRangeException {
		MemoryRange a = new MemoryRange(0x0000, 0x0100);
		MemoryRange b = new MemoryRange(0x0200, 0x0300);
		MemoryRange c = new MemoryRange(0x0200, 0x0300);
		// a < b
		assertTrue(a.compareTo(b) == -1);
		// b > a
		assertTrue(b.compareTo(a) == 1);
		// Identity
		assertTrue(a.compareTo(a) == 0);
		assertTrue(b.compareTo(b) == 0);
		// Equality
		assertTrue(b.compareTo(c) == 0);
		assertTrue(c.compareTo(b) == 0);
		// Null
		try {
			a.compareTo(null);
			fail("Should have thrown NullPointerException");
		} catch (NullPointerException ex) {
			// expected
		}
	}

	// Helper method.
	public void doInvalidMemoryRangeTest(int start, int end) {
		try {
			new MemoryRange(start, end);
			fail("MemoryRangeException should have been thrown.");
		} catch (MemoryRangeException e) {
			// Expected.
		}
	}

}