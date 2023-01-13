package com.loomcom.symon;

import com.loomcom.symon.devices.Crtc;
import com.loomcom.symon.devices.DeviceChangeListener;
import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryAccessException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CrtcTest {

    Crtc crtc;
    Memory memory;

    @Mock
    DeviceChangeListener changeListener;

    @Before
    public void createDevices() throws Exception {
        memory = new Memory(0, 0x7fff);

        crtc = new Crtc(0x9000, memory);
        crtc.registerListener(changeListener);
    }

    @Test
    public void selectingRegisterDoesNotTriggedrCallback() throws Exception {
        crtc.write(0, 1);

        verify(changeListener, never()).deviceStateChanged();
    }

    @Test
    public void shouldChangeHorizontalDisplayed() throws Exception {
        crtc.write(0, 1);

        crtc.write(1, 80);
        assertEquals(80, crtc.getHorizontalDisplayed());

        crtc.write(1, 40);
        assertEquals(40, crtc.getHorizontalDisplayed());
    }

    @Test
    public void changeHorizontalDisplayedShouldTriggerCallback() throws Exception {
        crtc.write(0, 1);

        crtc.write(1, 80);
        verify(changeListener, times(1)).deviceStateChanged();
    }


    @Test
    public void shouldChangeVerticalDisplayed() throws Exception {
        crtc.write(0, 6);

        crtc.write(1, 23);
        assertEquals(23, crtc.getVerticalDisplayed());

        crtc.write(1, 26);
        assertEquals(26, crtc.getVerticalDisplayed());
    }

    @Test
    public void changeVerticalDisplayedShouldTriggerCallback() throws Exception {
        crtc.write(0, 6);

        crtc.write(1, 23);

        verify(changeListener, times(1)).deviceStateChanged();
    }

    @Test
    public void shouldChangeScanLinesPerRow() throws Exception {
        crtc.write(0, 9); // Select register 9, Scan Line

        crtc.write(1, 3);
        assertEquals(3, crtc.getScanLinesPerRow());

        crtc.write(1, 5);
        assertEquals(5, crtc.getScanLinesPerRow());

        crtc.write(1, 9);
        assertEquals(9, crtc.getScanLinesPerRow());
    }

    @Test
    public void changeScanLinesPerRowShouldTriggerCallback() throws Exception {
        crtc.write(0, 9); // Select register 9, Scan Line

        crtc.write(1, 3);

        verify(changeListener, times(1)).deviceStateChanged();
    }

    @Test
    public void shouldChangeCursorStartLine() throws Exception {
        crtc.write(0, 10);

        crtc.write(1, 0);
        assertEquals(0, crtc.getCursorStartLine());

        crtc.write(1, 1);
        assertEquals(1, crtc.getCursorStartLine());

        crtc.write(1, 4);
        assertEquals(4, crtc.getCursorStartLine());
    }

    @Test
    public void changeCursorStartLineShouldTriggerCallback() throws Exception {
        crtc.write(0, 10);

        crtc.write(1, 5);

        verify(changeListener, times(1)).deviceStateChanged();
    }

    @Test
    public void cursorStartLineRegisterChangesCursorVisibility() throws Exception {
        crtc.write(0, 10);

        crtc.write(1, 0x00); // Start line 0, no blinking.

        assertEquals(0, crtc.getCursorStartLine());
        assertEquals(0, crtc.getCursorBlinkRate());
        assertTrue(crtc.isCursorEnabled());

        crtc.write(1, 0x23); // Start line 3, no cursor.

        assertEquals(3, crtc.getCursorStartLine());
        assertEquals(0, crtc.getCursorBlinkRate());
        assertFalse(crtc.isCursorEnabled());
    }

    @Test
    public void cursorStartLineRegisterChangesCursorBlinkRate() throws Exception {
        crtc.write(0, 10);

        crtc.write(1, 0x40); // Start line 0, 500ms blink delay

        assertEquals(0, crtc.getCursorStartLine());
        assertEquals(500, crtc.getCursorBlinkRate());
        assertTrue(crtc.isCursorEnabled());

        crtc.write(1, 0x62); // Start line 3, 1000ms blink delay

        assertEquals(2, crtc.getCursorStartLine());
        assertEquals(1000, crtc.getCursorBlinkRate());
        assertTrue(crtc.isCursorEnabled());
    }


    @Test
    public void shouldChangeCursorStopLine() throws Exception {
        crtc.write(0, 11);

        crtc.write(1, 0);
        assertEquals(0, crtc.getCursorStopLine());

        crtc.write(1, 3);
        assertEquals(3, crtc.getCursorStopLine());

        crtc.write(1, 6);
        assertEquals(6, crtc.getCursorStopLine());
    }

    @Test
    public void changeCursorStopLineShouldTriggerCallback() throws Exception {
        crtc.write(0, 11);

        crtc.write(1, 7);

        verify(changeListener, times(1)).deviceStateChanged();
    }

    @Test
    public void shouldChangeScreenStartAddressHighByte() throws Exception {
        crtc.write(0, 12);

        crtc.write(1, 0x00);
        assertEquals(0x00, crtc.getStartAddress() >> 8);

        crtc.write(1, 0x30);
        assertEquals(0x30, crtc.getStartAddress() >> 8);

        crtc.write(1, 0x6f);
        assertEquals(0x6f, crtc.getStartAddress() >> 8);
    }

    @Test
    public void changeScreenStartAddressHighByteShouldTriggerCallback() throws Exception {
        crtc.write(0, 12);

        crtc.write(1, 0x30);

        verify(changeListener, times(1)).deviceStateChanged();
    }

    @Test
    public void shouldChangeScreenStartAddressLowByte() throws Exception {
        crtc.write(0, 13);

        crtc.write(1, 0x00);
        assertEquals(0x00, crtc.getStartAddress() & 0xff);

        crtc.write(1, 0x11);
        assertEquals(0x11, crtc.getStartAddress() & 0xff);

        crtc.write(1, 0xff);
        assertEquals(0xff, crtc.getStartAddress() & 0xff);
    }

    @Test
    public void changeScreenStartAddressLowByteShouldTriggerCallback() throws Exception {
        crtc.write(0, 13);

        crtc.write(1, 0xff);

        verify(changeListener, times(1)).deviceStateChanged();
    }


    @Test(expected = MemoryAccessException.class)
    public void shouldThrowMemoryAccessExceptionIfPageOutOfRange() throws Exception {
        crtc.write(0, 12);

        crtc.write(1, 0x7f); // Page of text will extend beyond 0x7fff
    }

    @Test
    public void readingStartAddressShouldDoNothing() throws Exception {
        crtc.write(0, 12); // High byte

        crtc.write(1, 0x03);
        assertEquals(0, crtc.read(1, true));

        crtc.write(1, 0x70);
        assertEquals(0, crtc.read(1, true));


        crtc.write(0, 13); // Low byte

        crtc.write(1, 0xff);
        assertEquals(0, crtc.read(1, true));

        crtc.write(1, 0x0e);
        assertEquals(0, crtc.read(1, true));
    }


    @Test
    public void shouldChangeCursorPositionHighByte() throws Exception {
        crtc.write(0, 14);   // Select register 14

        crtc.write(1, 0x73); // Set high cursor byte to $73
        assertEquals(0x73, crtc.getCursorPosition() >> 8);

        crtc.write(1, 0x3f);
        assertEquals(0x3f, crtc.getCursorPosition() >> 8);

        crtc.write(1, 0x7f);
        assertEquals(0x7f, crtc.getCursorPosition() >> 8);
    }


    @Test
    public void shouldBeAbleToReadCursorPositionHighByte() throws Exception {
        crtc.write(0, 14);

        crtc.write(1, 0x3f);
        assertEquals(0x3f, crtc.read(1, true));

        crtc.write(1, 0x70);
        assertEquals(0x70, crtc.read(1, true));
    }

    @Test
    public void changeCursorPositionHighByteShouldTriggerCallback() throws Exception {
        crtc.write(0, 14);

        crtc.write(1, 0x73);

        verify(changeListener, times(1)).deviceStateChanged();
    }


    @Test
    public void shouldChangeCursorPositionLowByte() throws Exception {
        crtc.write(0, 15);   // Select register 15

        crtc.write(1, 0x00); // Set low cursor byte to $00
        assertEquals(0x00, crtc.getCursorPosition() & 0xff);

        crtc.write(1, 0x1f); // Set low cursor byte to $1f
        assertEquals(0x1f, crtc.getCursorPosition() & 0xff);

        crtc.write(1, 0xff); // Set low cursor byte to $ff
        assertEquals(0xff, crtc.getCursorPosition() & 0xff);
    }

    @Test
    public void shouldBeAbleToReadCursorPositionLowByte() throws Exception {
        crtc.write(0, 15);

        crtc.write(1, 0x00);
        assertEquals(0x00, crtc.read(1, true));

        crtc.write(1, 0x1f);
        assertEquals(0x1f, crtc.read(1, true));

        crtc.write(1, 0xff);
        assertEquals(0xff, crtc.read(1, true));
    }


    @Test
    public void changeCursorPositionLowByteShouldTriggerCallback() throws Exception {
        crtc.write(0, 15);

        crtc.write(1, 0x01);

        verify(changeListener, times(1)).deviceStateChanged();
    }

    @Test(expected = MemoryAccessException.class)
    public void shouldThrowMemoryAccessExceptionIfCursorGoesOutOfRange() throws Exception {
        crtc.write(0, 14);   // Select register 14
        crtc.write(1, 0x80); // Can't position cursor
    }

    @Test
    public void shouldSetRowColumnAddressing() throws Exception {
        assertEquals(false, crtc.getRowColumnAddressing());
        crtc.write(0, 8);        // Select mode control register
        crtc.write(1, 0x04);
        assertEquals(true, crtc.getRowColumnAddressing());
    }

    @Test
    public void shouldSetDisplayEnableSkew() throws Exception {
        assertEquals(false, crtc.getDisplayEnableSkew());
        crtc.write(0, 8);        // Select mode control register
        crtc.write(1, 0x10);
        assertEquals(true, crtc.getDisplayEnableSkew());
    }

    @Test
    public void shouldSetCursorSkew() throws Exception {
        assertEquals(false, crtc.getCursorSkew());
        crtc.write(0, 8);        // Select mode control register
        crtc.write(1, 0x20);
        assertEquals(true, crtc.getCursorSkew());
    }

    @Test
    public void shouldDoStraightBinaryAddressing() throws Exception {
        crtc.write(0, 8);
        crtc.write(1, 0); // Select straight binary addressing

        // Fill the memory with a repeating pattern
        int videoMemoryBase = 0x7000;
        int j = 0;

        for (int i = 0; i < 2048; i++) {
            memory.write(videoMemoryBase + i, j);
            if (j == 255) {
                j = 0;
            } else {
                j++;
            }
        }

        // Now verify that straight-binary addressing of the CRTC works
        j = 0;

        for (int i = 0; i < 2048; i++) {
            assertEquals(j, crtc.getCharAtAddress(videoMemoryBase + i));
            if (j == 255) {
                j = 0;
            } else {
                j++;
            }
        }
    }
}
