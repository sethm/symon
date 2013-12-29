package com.loomcom.symon.devices;

import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;

import java.io.IOException;
import java.util.logging.Logger;


/**
 * Simulation of a 6545 CRTC and virtual CRT output.
 */
public class Crtc extends Device {

    private static final Logger logger = Logger.getLogger(Crtc.class.getName());


    // Memory locations in the CRTC address space
    public static final int REGISTER_SELECT          = 0;
    public static final int REGISTER_WRITE           = 1;

    // Registers
    public static final int HORIZONTAL_DISPLAYED     = 1;
    public static final int VERTICAL_DISPLAYED       = 6;
    public static final int MODE_CONTROL             = 8;
    public static final int SCAN_LINE                = 9;
    public static final int CURSOR_START             = 10;
    public static final int CURSOR_END               = 11;
    public static final int DISPLAY_START_HIGH       = 12;
    public static final int DISPLAY_START_LOW        = 13;
    public static final int CURSOR_POSITION_HIGH     = 14;
    public static final int CURSOR_POSITION_LOW      = 15;


    /*
     * These will determine how the Character ROM is decoded,
     * and are Character ROM dependent.
     */

    // R1 - Horizontal Displayed
    private int horizontalDisplayed;

    // R6 - Vertical Displayed
    private int verticalDisplayed;

    // R9 - Scan Lines: Number of scan lines per character, including spacing.
    private int scanLinesPerRow;

    // R10 - Cursor Start / Cursor Mode
    private int cursorStartLine;
    private boolean cursorEnabled;
    private int cursorBlinkRate;

    // R11 - Cursor End
    private int cursorStopLine;

    // R12, R13 - Display Start Address: The starting address in the video RAM of the displayed page.
    private int startAddress;

    // R14, R15 - Cursor Position
    private int cursorPosition;

    // The size, in bytes, of a displayed page of characters.
    private int pageSize;

    private int currentRegister = 0;

    private Memory memory;

    public Crtc(int deviceAddress, Memory memory) throws MemoryRangeException, IOException {
        super(deviceAddress, deviceAddress + 2, "CRTC");
        this.memory = memory;

        // Defaults
        this.horizontalDisplayed = 40;
        this.verticalDisplayed = 25;
        this.scanLinesPerRow = 9;
        this.cursorStartLine = 0;
        this.cursorStopLine = 7;
        this.startAddress = 0x7000;
        this.cursorPosition = startAddress;
        this.pageSize = horizontalDisplayed * verticalDisplayed;
        this.cursorEnabled = true;
        this.cursorBlinkRate = 500;
    }

    @Override
    public void write(int address, int data) throws MemoryAccessException {
        switch (address) {
            case REGISTER_SELECT:
                setCurrentRegister(data);
                break;
            case REGISTER_WRITE:
                writeRegisterValue(data);
                break;
            default:
                throw new MemoryAccessException("No such address.");
        }

        notifyListeners();
    }

    @Override
    public int read(int address) throws MemoryAccessException {
        switch (address) {
            case REGISTER_SELECT:
                return status();
            case REGISTER_WRITE:
                return 0;
            default:
                throw new MemoryAccessException("No such address.");
        }
    }

    @Override
    public String toString() {
        return null;
    }

    public int[] getDmaAccess() {
        return memory.getDmaAccess();
    }

    private int status() {
        return 0;
    }

    public int getHorizontalDisplayed() {
        return horizontalDisplayed;
    }

    public int getVerticalDisplayed() {
        return verticalDisplayed;
    }

    public int getScanLinesPerRow() {
        return scanLinesPerRow;
    }

    public int getCursorStartLine() {
        return cursorStartLine;
    }

    public int getCursorStopLine() {
        return cursorStopLine;
    }

    public int getCursorBlinkRate() {
        return cursorBlinkRate;
    }

    public boolean isCursorEnabled() {
        return cursorEnabled;
    }

    public int getStartAddress() {
        return startAddress;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public int getPageSize() {
        return pageSize;
    }

    private void setCurrentRegister(int registerNumber) {
        this.currentRegister = registerNumber;
    }

    private void writeRegisterValue(int data) {
        switch (currentRegister) {
            case HORIZONTAL_DISPLAYED:
                horizontalDisplayed = data;
                pageSize = horizontalDisplayed * verticalDisplayed;
                break;
            case VERTICAL_DISPLAYED:
                verticalDisplayed = data;
                pageSize = horizontalDisplayed * verticalDisplayed;
                break;
            case MODE_CONTROL:
                // TODO: Implement multiple addressing modes and cursor skew.
                break;
            case SCAN_LINE:
                scanLinesPerRow = data;
                break;
            case CURSOR_START:
                cursorStartLine = data & 0x1f;
                // Bits 5 and 6 define the cursor mode.
                int cursorMode = (data & 0x60) >> 5;
                switch (cursorMode) {
                    case 0:
                        cursorEnabled = true;
                        cursorBlinkRate = 0;
                        break;
                    case 1:
                        cursorEnabled = false;
                        cursorBlinkRate = 0;
                        break;
                    case 2:
                        cursorEnabled = true;
                        cursorBlinkRate = 500;
                        break;
                    case 3:
                        cursorEnabled = true;
                        cursorBlinkRate = 1000;
                        break;
                }
                break;
            case CURSOR_END:
                cursorStopLine = data & 0x1f;
                break;
            case DISPLAY_START_HIGH:
                startAddress = ((data & 0xff) << 8) | (startAddress & 0x00ff);
                // TODO: bounds checking.
                break;
            case DISPLAY_START_LOW:
                startAddress = ((data & 0xff) | (startAddress & 0xff00));
                // TODO: bounds checking.
                break;
            case CURSOR_POSITION_HIGH:
                cursorPosition = ((data & 0xff) << 8) | (cursorPosition & 0x00ff);
                // TODO: bounds checking.
                break;
            case CURSOR_POSITION_LOW:
                // TODO: bounds checking.
                cursorPosition = (data & 0xff) | (cursorPosition & 0xff00);
                break;
            default:
                break;
        }

        notifyListeners();
    }
}
