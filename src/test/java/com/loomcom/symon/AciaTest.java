package com.loomcom.symon;

import org.junit.*;

import com.loomcom.symon.devices.Acia;
import com.loomcom.symon.exceptions.FifoUnderrunException;

import static org.junit.Assert.*;

public class AciaTest {

  @Test
  public void newAciaShouldHaveTxEmptyStatus() throws Exception {
    Acia acia = new Acia(0x000);

    assertEquals(0x10, acia.read(0x0001));
  }

  @Test
  public void aciaShouldHaveTxEmptyStatusOffIfTxHasData() throws Exception {
    Acia acia = new Acia(0x000);

    acia.txWrite('a');
    assertEquals(0x00, acia.read(0x0001));
  }

  @Test
  public void aciaShouldHaveRxFullStatusOffIfRxHasData() throws Exception {
    Acia acia = new Acia(0x000);

    acia.rxWrite('a');
    assertEquals(0x18, acia.read(0x0001));
  }

  @Test
  public void aciaShouldHaveTxEmptyAndRxFullStatusOffIfRxAndTxHaveData()
      throws Exception {
    Acia acia = new Acia(0x000);

    acia.rxWrite('a');
    acia.txWrite('b');

    assertEquals(0x08, acia.read(0x0001));
  }

  @Test
  public void readingBuffersUntilEmptyShouldResetStatus()
      throws Exception {
    Acia acia = new Acia(0x0000);

    acia.rxWrite('a');
    acia.txWrite('b');

    assertEquals(0x08, acia.read(0x0001));

    assertEquals('a', acia.rxRead());
    assertEquals('b', acia.txRead());

    assertEquals(0x10, acia.read(0x0001));
  }

  @Test
  public void readingPastEmptyRxBufferShouldThrowException()
    throws Exception {
    Acia acia = new Acia(0x0000);

    acia.rxWrite('a');
    assertEquals(0x18, acia.read(0x0001)); // not empty

    assertEquals('a', acia.rxRead());
    assertEquals(0x10, acia.read(0x0001)); // becomes empty

    // Should raise (note: I prefer this style to @Test(expected=...)
    // because it allows much finer grained control over asserting
    // exactly where the exception is expected to be raised.)
    try {
      // Should cause an underrun
      acia.rxRead();
      fail("Should have raised FifoUnderrunException.");
    } catch (FifoUnderrunException ex) {}

    assertEquals(0x10, acia.read(0x0001)); // still again

    for (int i = 0; i < Acia.BUF_LEN; i++) {
      acia.rxWrite('a');
    }

    // Should NOT cause an overrun
    acia.rxWrite('a'); // nothing thrown
  }

  @Test
  public void readingPastEmptyTxBufferShouldThrowException()
    throws Exception {
    Acia acia = new Acia(0x0000);

    acia.txWrite('a');
    assertEquals(0x00, acia.read(0x0001)); // not empty

    assertEquals('a', acia.txRead());
    assertEquals(0x10, acia.read(0x0001)); // becomes empty

    // Should raise (note: I prefer this style to @Test(expected=...)
    // because it allows much finer grained control over asserting
    // exactly where the exception is expected to be raised.)
    try {
      // Should cause an underrun
      acia.txRead();
      fail("Should have raised FifoUnderrunException.");
    } catch (FifoUnderrunException ex) {}

    assertEquals(0x10, acia.read(0x0001)); // still empty

    for (int i = 0; i < Acia.BUF_LEN; i++) {
      acia.txWrite('a');
    }

    // Should NOT cause an overrun
    acia.txWrite('a'); // Nothing thrown.
  }

}
