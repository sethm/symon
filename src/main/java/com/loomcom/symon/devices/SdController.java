/*
 * Copyright (c) 2016 Seth J. Morabito <web@loomcom.com>
 *                    Maik Merten <maikmerten@googlemail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.loomcom.symon.devices;

import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Emulation for the SD-card controller of the MULTICOMP system.
 * Neiter comlete nor correct.
 */
public class SdController extends Device {

    private enum Status {
        IDLE,
        READ,
        WRITE
    }

    public static final int CONTROLLER_SIZE = 8;
    private final int SECTOR_SIZE = 512;
    private final static Logger logger = Logger.getLogger(SdController.class.getName());

    private File sdImageFile;
    private int lba0, lba1, lba2;
    private int position;
    private Status status = Status.IDLE;

    private final byte[] readBuffer = new byte[SECTOR_SIZE];
    private final byte[] writeBuffer = new byte[SECTOR_SIZE];
    private int readPosition = 0;
    private int writePosition = 0;


    public SdController(int address) throws MemoryRangeException {
        super(address, address + CONTROLLER_SIZE - 1, "SDCONTROLLER");

        sdImageFile = new File("sd.img");
        if (!sdImageFile.exists()) {
            sdImageFile = null;
            logger.log(Level.INFO, "Could not find SD card image 'sd.img'");
        }
    }


    @Override
    public void write(int address, int data) throws MemoryAccessException {
        switch (address) {
            case 0:
                writeData(data);
                return;
            case 1:
                writeCommand(data);
                return;
            case 2:
                this.lba0 = data;
                return;
            case 3:
                this.lba1 = data;
                return;
            case 4:
                this.lba2 = data;
        }
    }

    @Override
    public int read(int address, boolean cpuAccess) throws MemoryAccessException {
        switch (address) {
            case 0:
                return readData();
            case 1:
                return readStatus();
            default:
                return 0;
        }
    }

    private void computePosition() {
        this.position = lba0 + (lba1 << 8) + (lba2 << 16);
        // each sector is 512 bytes, so multiply accordingly
        this.position <<= 9;
    }

    private void prepareRead() {
        this.status = Status.READ;
        this.readPosition = 0;
        computePosition();

        if (sdImageFile != null) {
            try {
                FileInputStream fis = new FileInputStream(sdImageFile);
                fis.skip(this.position);
                int read = fis.read(readBuffer);
                if (read < SECTOR_SIZE) {
                    logger.log(Level.WARNING, "not enough data to fill read buffer from SD image file");
                }
                fis.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "could not fill read buffer from SD image file", ex);
            }
        }
    }

    private void prepareWrite() {
        this.status = Status.WRITE;
        this.writePosition = 0;
        computePosition();
    }


    private int readData() {
        if (status != Status.READ) {
            return 0;
        }

        int data = readBuffer[readPosition++];

        if (readPosition >= SECTOR_SIZE) {
            this.status = Status.IDLE;
        }

        return data;
    }

    private void writeData(int data) {
        if (status != Status.WRITE) {
            return;
        }

        writeBuffer[writePosition++] = (byte) data;

        if (writePosition >= SECTOR_SIZE) {
            if (sdImageFile != null) {
                try {
                    RandomAccessFile raf = new RandomAccessFile(sdImageFile, "rw");
                    raf.skipBytes(this.position);
                    raf.write(writeBuffer, 0, writeBuffer.length);
                    raf.close();
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "could not write data back to SD image file!", ex);
                }
            }

            this.status = Status.IDLE;
        }

    }

    private int readStatus() {
        switch (this.status) {
            case IDLE:
                return 128;
            case READ:
                return 224;
            case WRITE:
                return 160;
            default:
                return 0;
        }
    }

    private void writeCommand(int data) {
        switch (data) {
            case 0:
                prepareRead();
                return;
            case 1:
                prepareWrite();
                return;
            default:
                this.status = Status.IDLE;
        }
    }

    @Override
    public String toString() {
        return getName() + "@" + String.format("%04X", this.getMemoryRange().startAddress);
    }

}
