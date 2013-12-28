package com.loomcom.symon.devices;

import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;
import com.loomcom.symon.ui.CrtPanel;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * Simulation of a 6545 CRTC and virtual CRT output.
 */
public class Crtc extends Device {

    public static final int CHAR_WIDTH = 8;
    public static final int CHAR_HEIGHT = 8;
    public static final int SCAN_LINES = 9;
    public static final int COLUMNS = 40;
    public static final int ROWS = 25;
    public static final int SCALE = 2;

    public static final int REGISTER_SELECT = 0;
    public static final int REGISTER_WRITE = 1;

    public static String CHAR_ROM_RESOURCE = "/pet.rom";

    private CrtPanel crtPanel;
    private int currentRegister = 0;

    public Crtc(int deviceAddress, Memory memory, int videoRamStartAddress) throws MemoryRangeException, IOException {
        super(deviceAddress, 2, "CRTC");
        this.crtPanel = new CrtPanel(loadCharRom(CHAR_ROM_RESOURCE), memory.getDmaAccess(), COLUMNS, ROWS,
                                     CHAR_WIDTH, CHAR_HEIGHT,
                                     SCALE, SCALE, videoRamStartAddress);
    }

    private byte[] loadCharRom(String resource) throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(this.getClass().getResourceAsStream(resource));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (bis.available() > 0) {
                bos.write(bis.read());
            }
            bos.flush();
            bos.close();
            return bos.toByteArray();
        } finally {
            if (bis != null) {
                bis.close();
            }
        }
    }

    public CrtPanel getCrtPanel() {
        return crtPanel;
    }

    @Override
    public void write(int address, int data) throws MemoryAccessException {
        switch (address) {
            case REGISTER_SELECT:
                setCurrentRegister(data);
            case REGISTER_WRITE:
                writeRegisterValue(data);
            default:
                throw new MemoryAccessException("No such address.");
        }
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

    private int status() {
        return 0;
    }

    private void setCurrentRegister(int registerNumber) {
        this.currentRegister = registerNumber;
    }

    private void writeRegisterValue(int data) {

    }
}
