package com.loomcom.lm6502;

import junit.framework.*;

import com.loomcom.lm6502.devices.*;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class AddressDecoderTest extends TestCase {

	public AddressDecoderTest(String testName) {
		super(testName);
	}

	public static Test suite() {
		return new TestSuite(AddressDecoderTest.class);
	}

	public void testCreatingWithTopAndBottom() {
		AddressDecoder ad = null;

		ad = new AddressDecoder(0x00, 0xff);
		assertEquals(0x00, ad.bottom());
		assertEquals(0xff, ad.top());

		ad = new AddressDecoder(0x20, 0xea);
		assertEquals(0x20, ad.bottom());
		assertEquals(0xea, ad.top());
	}

	public void testCreatingWithSize() {
		AddressDecoder ad = null;

		ad = new AddressDecoder(256);
		assertEquals(0x00, ad.bottom());
		assertEquals(0xff, ad.top());

		ad = new AddressDecoder(4096);
		assertEquals(0x000, ad.bottom());
		assertEquals(0xfff, ad.top());

		ad = new AddressDecoder(65536);
		assertEquals(0x0000, ad.bottom());
		assertEquals(0xffff, ad.top());
	}

	public void testAddDevice() throws MemoryRangeException {
		Device memory = new Memory(0x0000, 0x0100, null, true);
		Device rom    = new Memory(0x0100, 0x0200, null, false);

		AddressDecoder ad = new AddressDecoder(0x0000, 0xffff);

		assertEquals(0, ad.getDevices().size());
		ad.addDevice(memory);
		assertEquals(1, ad.getDevices().size());
		ad.addDevice(rom);
		assertEquals(2, ad.getDevices().size());
	}

	public void testOverlappingDevicesShouldFail() throws MemoryRangeException {
		Device memory = new Memory(0x0000, 0x0100, null, true);
		Device rom    = new Memory(0x00ff, 0x0200, null, false);

		AddressDecoder ad = new AddressDecoder(0x0000, 0xffff);

		ad.addDevice(memory);

		try {
			ad.addDevice(rom);
			fail("Should have thrown a MemoryRangeException.");
		} catch (MemoryRangeException ex) {
			// expected
		}
	}

	public void testIsCompleteWithFirstDeviceNotStartingAtBottom() throws MemoryRangeException {
		Device memory = new Memory(0x00ff, 0xff00, null, true);

		AddressDecoder ad = new AddressDecoder(0x0000, 0xffff);
		assertFalse("Address space was unexpectedly complete!", ad.isComplete());
		ad.addDevice(memory);
		assertFalse("Address space was unexpectedly complete!", ad.isComplete());
	}

	public void testIsCompleteWithOneDevice() throws MemoryRangeException {
		Device memory = new Memory(0x0000, 0x10000, null, true);

		AddressDecoder ad = new AddressDecoder(0x0000, 0xffff);
		assertFalse("Address space was unexpectedly complete!", ad.isComplete());
		ad.addDevice(memory);
		assertTrue("Address space should have been complete!", ad.isComplete());
	}

	public void testIsCompleteWithTwoDevices() throws MemoryRangeException {
		Device memory = new Memory(0x0000, 0x8000, null, true);
		Device rom    = new Memory(0x8000, 0x8000, null, false);

		AddressDecoder ad = new AddressDecoder(0x0000, 0xffff);
		assertFalse("Address space was unexpectedly complete!", ad.isComplete());
		ad.addDevice(memory);
		assertFalse("Address space was unexpectedly complete!", ad.isComplete());
		ad.addDevice(rom);
		assertTrue("Address space should have been complete!", ad.isComplete());
	}

	public void testIsCompleteWithThreeDevices() throws MemoryRangeException {
		Device memory = new Memory(0x0000, 0x8000, null, true);
		Device rom1   = new Memory(0x8000, 0x4000, null, false);
		Device rom2   = new Memory(0xC000, 0x4000, null, false);

		AddressDecoder ad = new AddressDecoder(0x0000, 0xffff);
		assertFalse("Address space was unexpectedly complete!", ad.isComplete());
		ad.addDevice(memory);
		assertFalse("Address space was unexpectedly complete!", ad.isComplete());
		ad.addDevice(rom1);
		assertFalse("Address space was unexpectedly complete!", ad.isComplete());
		ad.addDevice(rom2);
		assertTrue("Address space should have been complete!", ad.isComplete());
	}

}