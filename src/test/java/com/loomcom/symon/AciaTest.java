package com.loomcom.symon;

import com.loomcom.symon.devices.Acia;
import com.loomcom.symon.devices.Acia6551;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class AciaTest {

    @Test
    public void shouldTriggerInterruptOnRxFullIfRxIrqEnabled() throws Exception {
        Bus mockBus = mock(Bus.class);

        Acia acia = new Acia6551(0x000);
        acia.setBus(mockBus);

        // Disable TX IRQ, Enable RX IRQ
        acia.write(2, 0x00);

        acia.rxWrite('a');

        verify(mockBus, atLeastOnce()).assertIrq();
    }

    @Test
    public void shouldNotTriggerInterruptOnRxFullIfRxIrqNotEnabled() throws Exception {
        Bus mockBus = mock(Bus.class);

        Acia acia = new Acia6551(0x000);
        acia.setBus(mockBus);

        // Disable TX IRQ, Disable RX IRQ
        acia.write(2, 0x02);

        acia.rxWrite('a');

        verify(mockBus, never()).assertIrq();
    }

    @Test
    public void shouldTriggerInterruptOnTxEmptyIfTxIrqEnabled() throws Exception {
        Bus mockBus = mock(Bus.class);

        Acia acia = new Acia6551(0x000);
        acia.setBus(mockBus);

        // Enable TX IRQ, Disable RX IRQ
        acia.write(2, 0x06);

        // Write data
        acia.write(0, 'a');

        verify(mockBus, never()).assertIrq();

        // Transmission should cause IRQ
        acia.txRead(true);

        verify(mockBus, atLeastOnce()).assertIrq();
    }

    @Test
    public void shouldNotTriggerInterruptOnTxEmptyIfTxIrqNotEnabled() throws Exception {
        Bus mockBus = mock(Bus.class);

        Acia acia = new Acia6551(0x000);
        acia.setBus(mockBus);

        // Disable TX IRQ, Disable RX IRQ
        acia.write(2, 0x02);

        // Write data
        acia.write(0, 'a');

        // Transmission should not cause IRQ
        acia.txRead(true);

        verify(mockBus, never()).assertIrq();
    }

    @Test
    public void shouldTriggerInterruptFlagOnRxFullIfRxIrqEnabled() throws Exception {
        Bus mockBus = mock(Bus.class);

        Acia acia = new Acia6551(0x000);
        acia.setBus(mockBus);

        // Disable TX IRQ, Enable RX IRQ
        acia.write(2, 0x00);

        acia.rxWrite('a');

        // Receive should cause IRQ flag to be set
        assertEquals(0x80, acia.read(0x0001, true) & 0x80);
    }

    @Test
    public void shouldNotTriggerInterruptFlagOnRxFullIfRxIrqNotEnabled() throws Exception {
        Bus mockBus = mock(Bus.class);

        Acia acia = new Acia6551(0x000);
        acia.setBus(mockBus);

        // Disable TX IRQ, Disable RX IRQ
        acia.write(2, 0x02);

        acia.rxWrite('a');

        // Receive should not cause IRQ flag to be set
        assertEquals(0x00, acia.read(0x0001, true) & 0x80);
    }

    @Test
    public void shouldTriggerInterruptFlagOnTxEmptyIfTxIrqEnabled() throws Exception {
        Bus mockBus = mock(Bus.class);

        Acia acia = new Acia6551(0x000);
        acia.setBus(mockBus);

        // Enable TX IRQ, Disable RX IRQ
        acia.write(2, 0x06);

        // Write data
        acia.write(0, 'a');

        verify(mockBus, never()).assertIrq();

        // Transmission should cause IRQ flag to be set
        acia.txRead(true);

        assertEquals(0x80, acia.read(0x0001, true) & 0x80);
    }

    @Test
    public void shouldNotTriggerInterruptFlagOnTxEmptyIfTxIrqNotEnabled() throws Exception {
        Bus mockBus = mock(Bus.class);

        Acia acia = new Acia6551(0x000);
        acia.setBus(mockBus);

        // Disable TX IRQ, Disable RX IRQ
        acia.write(2, 0x02);

        // Write data
        acia.write(0, 'a');

        // Transmission should not cause IRQ flag to be set
        acia.txRead(true);

        assertEquals(0x00, acia.read(0x0001, true) & 0x80);
    }

    @Test
    public void newAciaShouldHaveTxEmptyStatus() throws Exception {
        Acia acia = new Acia6551(0x000);

        assertEquals(0x10, acia.read(0x0001, true));
    }

    @Test
    public void aciaShouldHaveTxEmptyStatusOffIfTxHasData() throws Exception {
        Acia acia = new Acia6551(0x000);

        acia.txWrite('a');
        assertEquals(0x00, acia.read(0x0001, true));
    }

    @Test
    public void aciaShouldHaveRxFullStatusOffIfRxHasData() throws Exception {
        Acia acia = new Acia6551(0x000);

        acia.rxWrite('a');
        assertEquals(0x18, acia.read(0x0001, true));
    }

    @Test
    public void aciaShouldHaveTxEmptyAndRxFullStatusOffIfRxAndTxHaveData()
            throws Exception {
        Acia acia = new Acia6551(0x000);

        acia.rxWrite('a');
        acia.txWrite('b');

        assertEquals(0x08, acia.read(0x0001, true));
    }
    
    @Test
    public void aciaShouldOverrunAndReadShouldReset()
            throws Exception {
        
        Acia acia = new Acia6551(0x0000);
        
        // overrun ACIA
        acia.rxWrite('a');
        acia.rxWrite('b');
        
        assertEquals(0x04, acia.read(0x0001, true) & 0x04);
        
        // read should reset
        acia.rxRead(true);
        assertEquals(0x00, acia.read(0x0001, true) & 0x04);
        
    }

    @Test
    public void aciaShouldOverrunAndMemoryWindowReadShouldNotReset()
            throws Exception {

        Acia acia = new Acia6551(0x0000);

        // overrun ACIA
        acia.rxWrite('a');
        acia.rxWrite('b');

        assertEquals(0x04, acia.read(0x0001, true) & 0x04);

        // memory window read should not reset
        acia.rxRead(false);
        assertEquals(0x04, acia.read(0x0001, true) & 0x04);

    }

    @Test
    public void readingBuffersShouldResetStatus()
            throws Exception {
        Acia acia = new Acia6551(0x0000);

        acia.rxWrite('a');
        acia.txWrite('b');


        assertEquals(0x08, acia.read(0x0001, true));

        assertEquals('a', acia.rxRead(true));
        assertEquals('b', acia.txRead(true));

        assertEquals(0x10, acia.read(0x0001, true));
    }

    @Test
    public void A()
            throws Exception {
        Acia acia = new Acia6551(0x0000);

        acia.rxWrite('a');
        acia.txWrite('b');


        assertEquals(0x08, acia.read(0x0001, true));

        assertEquals('a', acia.rxRead(false));
        assertEquals('b', acia.txRead(false));

        assertEquals(0x08, acia.read(0x0001, true));
    }

    @Test
    public void statusRegisterInitializedAtHardwareReset() throws Exception {
        Acia6551 acia = new Acia6551(0x0000);

        assertEquals(0x10, acia.read(0x0001, false));
    }

    @Test
    public void commandRegisterInitializedAtHardwareReset() throws Exception {
        Acia6551 acia = new Acia6551(0x0000);

        assertEquals(0x02, acia.read(0x0002, false));
    }

    @Test
    public void controlRegisterInitializedAtHardwareReset() throws Exception {
        Acia6551 acia = new Acia6551(0x0000);

        assertEquals(0x00, acia.read(0x0003, false));
    }

    @Test
    public void programResetClearsOverrunStatus() throws Exception {
        Acia6551 acia = new Acia6551(0x0000);
        Bus bus = new Bus(acia.ACIA_SIZE);
        acia.setBus(bus);

        // Change as many status bits as we can.
        acia.write(0x0002, 0x00); // enable receive interrupt
        acia.rxWrite('a');
        acia.rxWrite('b'); // overrun, receive full, interrupt signalled
        acia.write(0x0000, 'c'); // Transmitter Data Register not empty

        // Check that all the bits we expected to be set actually are
        assertEquals(0x8C, acia.read(0x0001, false));

        // Do a "program reset". The value is ignored.
        acia.write(0x0001, 0xFF);

        // Check that only bit 2 was cleared.
        assertEquals(0x88, acia.read(0x0001, false));
    }

    @Test
    public void programResetKeepsParitySettings() throws Exception {
        Acia6551 acia = new Acia6551(0x0000);

        // Set all the command register bits
        acia.write(0x0002, 0xFF);

        // Do a "program reset". The value is ignored.
        acia.write(0x0001, 0xFF);

        // The top 3 bits should be kept as-is,
        // the bottom 5 bits should be reset to defaults.
        assertEquals(0xE2, acia.read(0x0002, false));
    }

    @Test
    public void programResetLeavesControlRegisterUnchanged() throws Exception {
        Acia6551 acia = new Acia6551(0x0000);

        // Set all the control register bits
        acia.write(0x0003, 0xFF);

        // Do a "program reset". The value is ignored.
        acia.write(0x0001, 0xFF);

        // No bits should have changed.
        assertEquals(0xFF, acia.read(0x0003, false));
    }
}
