package com.loomcom.symon;

import com.loomcom.symon.devices.Acia;
import com.loomcom.symon.devices.Acia6850;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class AciaTest6850 {
    
    private final static int CMD_STAT_REG = 0;
    private final static int DATA_REG = 1;
    
    
    private Acia newAcia() throws Exception {
        Acia acia = new Acia6850(0x0000);
        // by default rate is limited, have it unlimited for testing
        acia.setBaudRate(0);
        return acia;
    }
    

    @Test
    public void shouldTriggerInterruptOnRxFullIfRxIrqEnabled() throws Exception {
        Bus mockBus = mock(Bus.class);

        Acia acia = newAcia();
        acia.setBus(mockBus);

        // Disable TX IRQ, Enable RX IRQ
        acia.write(CMD_STAT_REG, 0x80);

        acia.rxWrite('a');

        verify(mockBus, atLeastOnce()).assertIrq();
    }

    @Test
    public void shouldNotTriggerInterruptOnRxFullIfRxIrqNotEnabled() throws Exception {
        Bus mockBus = mock(Bus.class);

        Acia acia = newAcia();
        acia.setBus(mockBus);

        // Disable TX IRQ, Disable RX IRQ
        acia.write(CMD_STAT_REG, 0x00);

        acia.rxWrite('a');

        verify(mockBus, never()).assertIrq();
    }

    @Test
    public void shouldTriggerInterruptOnTxEmptyIfTxIrqEnabled() throws Exception {
        Bus mockBus = mock(Bus.class);

        Acia acia = newAcia();
        acia.setBus(mockBus);

        // Enable TX IRQ, Disable RX IRQ
        acia.write(CMD_STAT_REG, 0x20);

        // Write data
        acia.write(1, 'a');

        verify(mockBus, never()).assertIrq();

        // Transmission should cause IRQ
        acia.txRead();

        verify(mockBus, atLeastOnce()).assertIrq();
    }

    @Test
    public void shouldNotTriggerInterruptOnTxEmptyIfTxIrqNotEnabled() throws Exception {
        Bus mockBus = mock(Bus.class);

        Acia acia = newAcia();
        acia.setBus(mockBus);

        // Disable TX IRQ, Disable RX IRQ
        acia.write(CMD_STAT_REG, 0x02);

        // Write data
        acia.write(DATA_REG, 'a');

        // Transmission should cause IRQ
        acia.txRead();

        verify(mockBus, never()).assertIrq();
    }

    @Test
    public void newAciaShouldHaveTxEmptyStatus() throws Exception {
        Acia acia = newAcia();

        assertEquals(0x02, acia.read(CMD_STAT_REG) & 0x02);
    }

    @Test
    public void aciaShouldHaveTxEmptyStatusOffIfTxHasData() throws Exception {
        Acia acia = newAcia();

        acia.txWrite('a');
        assertEquals(0x00, acia.read(CMD_STAT_REG) & 0x02);
    }

    @Test
    public void aciaShouldHaveRxFullStatusOnIfRxHasData() throws Exception {
        Acia acia = newAcia();

        acia.rxWrite('a');
       
        assertEquals(0x01, acia.read(CMD_STAT_REG) & 0x01);
    }

    @Test
    public void aciaShouldHaveTxEmptyAndRxFullStatusOffIfRxAndTxHaveData()
            throws Exception {
        Acia acia = newAcia();
       
        acia.rxWrite('a');
        acia.txWrite('b');

        assertEquals(0x01, acia.read(CMD_STAT_REG) & 0x03);
    }
    
    @Test
    public void aciaShouldOverrunAndReadShouldReset()
            throws Exception {
        
        Acia acia = newAcia();
        
        // overrun ACIA
        acia.rxWrite('a');
        acia.rxWrite('b');
        
        assertEquals(0x20, acia.read(CMD_STAT_REG) & 0x20);
        
        // read should reset
        acia.rxRead();
        assertEquals(0x00, acia.read(CMD_STAT_REG) & 0x20);
        
    }

    @Test
    public void readingBuffersShouldResetStatus()
            throws Exception {
        Acia acia = newAcia();

        assertEquals(0x00, acia.read(CMD_STAT_REG) & 0x01);
        
        acia.rxWrite('a');
        
        assertEquals(0x01, acia.read(CMD_STAT_REG) & 0x01);
        
        acia.rxRead();
        
        assertEquals(0x00, acia.read(CMD_STAT_REG) & 0x01);        
        
    }
}
