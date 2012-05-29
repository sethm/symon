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
  public void readingBuffersShouldResetStatus()
      throws Exception {
    Acia acia = new Acia(0x0000);

    acia.rxWrite('a');
    acia.txWrite('b');

    assertEquals(0x08, acia.read(0x0001));

    assertEquals('a', acia.rxRead());
    assertEquals('b', acia.txRead());

    assertEquals(0x10, acia.read(0x0001));
  }

}
