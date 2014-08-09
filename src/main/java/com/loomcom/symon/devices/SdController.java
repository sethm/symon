/*
 * Copyright (c) 2008-2013 Seth J. Morabito <sethm@loomcom.com>
 *                         Maik Merten <maikmerten@googlemail.com>
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Emulation for the SD-card controller of the MULTICOMP system.
 * Neiter comlete nor correct.
 * 
 */
public class SdController extends Device {
    
    private enum Status {
        IDLE,
        READ,
        WRITE
    }
    
    public static final int CONTROLLER_SIZE = 8;
    private final static Logger logger = Logger.getLogger(SdController.class.getName());
    private byte[] sdcontent;
    
    private int lba0,lba1,lba2;
    private int command;
    private int position;
    private Status status;
   
    
    public SdController(int address) throws MemoryRangeException {
        super(address, address + CONTROLLER_SIZE - 1, "SDCONTROLLER");
        
        // assume an empty 64K SD card by default
        sdcontent = new byte[64 * 1024];
        
        // try to load an actual SD card image
        try {
            FileInputStream fis = new FileInputStream(new File("sd.img"));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[2048];
            int read = fis.read(buf);
            while(read > 0) {
                baos.write(buf, 0, read);
                read = fis.read(buf);
            }
            sdcontent = baos.toByteArray();
        } catch (IOException ex) {
            logger.log(Level.INFO, "Could not load image for SD card from file 'sd.img'");
        }
    }
    

    @Override
    public void write(int address, int data) throws MemoryAccessException {
        switch(address) {
            case 0 :
                writeData(data);
                break;
            case 1 :
                writeCommand(data);
                break;
            case 2 :
                this.lba0 = data;
                computePosition();
                break;
            case 3 : 
                this.lba1 = data;
                computePosition();
                break;
            case 4 :
                this.lba2 = data;
                computePosition();
                break;
        }
    }

    @Override
    public int read(int address) throws MemoryAccessException {
        switch(address) {
            case 0:
                return readData();
            case 1:
                return readStatus();
            case 2:
                return lba0;
            case 3:
                return lba1;
            case 4:
                return lba2;
            default:
                return 0;
        }
    }
    
    private void computePosition() {
        this.position = lba0 + (lba1 << 8) + (lba2 << 16);
        // each sector is 512 bytes, so multiply accordingly
        this.position <<= 9;
     }
    
    private int readData() {
        this.position %= this.sdcontent.length;
        return this.sdcontent[this.position++];
    }
    
    private void writeData(int data) {
        this.position %= this.sdcontent.length;
        this.sdcontent[this.position++] = (byte) data;
    }
    
    private int readStatus() {
        switch(this.status) {
            case IDLE:
                return 128;
            case READ:
                return 224;
            default:
                return 0;
        }
    }
    
    private void writeCommand(int data) {
        this.command = data;
        switch(this.command) {
            case 0 :
                this.status = Status.READ;
                break;
            case 1 :
                this.status = Status.WRITE;
                break;
            default:
                this.status = Status.IDLE;
        }
    }

    @Override
    public String toString() {
        return getName() + "@" + String.format("%04X", this.getMemoryRange().startAddress);
    }
    
}
