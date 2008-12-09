package com.loomcom.lm6502;

import junit.framework.*;

import java.util.HashMap;
import java.util.Map;

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

	public void testGetStartAddress() throws MemoryRangeException {
		MemoryRange r = new MemoryRange(0x101, 0x202);
		assertEquals(0x101, r.getStartAddress());
	}

	public void testGetEndAddress() throws MemoryRangeException {
		MemoryRange r = new MemoryRange(0x101, 0x202);
		assertEquals(0x202, r.getEndAddress());
	}

	public void testOverlapsWith() throws MemoryRangeException {
		MemoryRange a = new MemoryRange(0x0000, 0x0fff);
		MemoryRange b = new MemoryRange(0x2000, 0x2fff);
		MemoryRange c = new MemoryRange(0x1000, 0x1fff); // fits between a and b
		MemoryRange d = new MemoryRange(0x0f00, 0x10ff); // overlaps with a
		MemoryRange e = new MemoryRange(0x1fff, 0x20ff); // overlaps with b
		MemoryRange f = new MemoryRange(0x0fff, 0x2000); // overlaps a and b
		MemoryRange g = new MemoryRange(0x00ff, 0x0100); // Overlaps (inside) a, below b
		MemoryRange h = new MemoryRange(0x20ff, 0x2100); // Overlaps (inside) b, above a

		assertFalse(c.overlapsWith(a));
		assertFalse(c.overlapsWith(b));
		assertFalse(a.overlapsWith(c));
		assertFalse(b.overlapsWith(c));
		assertFalse(a.overlapsWith(b));
		assertFalse(b.overlapsWith(a));

		assertTrue(d.overlapsWith(a));
		assertTrue(a.overlapsWith(d));

		assertTrue(e.overlapsWith(b));
		assertTrue(b.overlapsWith(e));

		assertTrue(f.overlapsWith(a));
		assertTrue(a.overlapsWith(f));
		assertTrue(f.overlapsWith(b));
		assertTrue(b.overlapsWith(f));

		assertTrue(a.overlapsWith(g));
		assertTrue(g.overlapsWith(a));
		assertFalse(b.overlapsWith(g));
		assertFalse(g.overlapsWith(b));

		assertFalse(a.overlapsWith(h));
		assertFalse(h.overlapsWith(a));
		assertTrue(b.overlapsWith(h));
		assertTrue(h.overlapsWith(b));
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