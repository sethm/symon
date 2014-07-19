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
        acia.txRead();

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

        // Transmission should cause IRQ
        acia.txRead();

        verify(mockBus, never()).assertIrq();
    }

    @Test
    public void newAciaShouldHaveTxEmptyStatus() throws Exception {
        Acia acia = new Acia6551(0x000);

        assertEquals(0x10, acia.read(0x0001));
    }

    @Test
    public void aciaShouldHaveTxEmptyStatusOffIfTxHasData() throws Exception {
        Acia acia = new Acia6551(0x000);

        acia.txWrite('a');
        assertEquals(0x00, acia.read(0x0001));
    }

    @Test
    public void aciaShouldHaveRxFullStatusOffIfRxHasData() throws Exception {
        Acia acia = new Acia6551(0x000);

        acia.rxWrite('a');
        assertEquals(0x18, acia.read(0x0001));
    }

    @Test
    public void aciaShouldHaveTxEmptyAndRxFullStatusOffIfRxAndTxHaveData()
            throws Exception {
        Acia acia = new Acia6551(0x000);

        acia.rxWrite('a');
        acia.txWrite('b');

        assertEquals(0x08, acia.read(0x0001));
    }
    
    @Test
    public void aciaShouldOverrunAndReadShouldReset()
            throws Exception {
        
        Acia acia = new Acia6551(0x0000);
        
        // overrun ACIA
        acia.rxWrite('a');
        acia.rxWrite('b');
        
        assertEquals(0x04, acia.read(0x0001) & 0x04);
        
        // read should reset
        acia.rxRead();
        assertEquals(0x00, acia.read(0x0001) & 0x04);
        
    }


    @Test
    public void readingBuffersShouldResetStatus()
            throws Exception {
        Acia acia = new Acia6551(0x0000);

        acia.rxWrite('a');
        acia.txWrite('b');


        assertEquals(0x08, acia.read(0x0001));

        assertEquals('a', acia.rxRead());
        assertEquals('b', acia.txRead());

        assertEquals(0x10, acia.read(0x0001));
    }
}
