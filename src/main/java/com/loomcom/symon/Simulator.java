package com.loomcom.symon;

import java.util.logging.*;

import com.loomcom.symon.devices.*;
import com.loomcom.symon.exceptions.*;
import com.loomcom.symon.ui.UiUpdater;

import javax.swing.*;

/**
 * Main entry point and control for the Symon Simulator.
 * This class is responsible for creating the UI and starting
 * the IO threads that pass data to and from the simulated
 * Bus and Cpu.
 */
public class Simulator implements Runnable {

    private final static Logger logger = Logger.getLogger(Simulator.class.getName());

    private Bus    bus;
    private Cpu    cpu;
    private Acia   acia;
    private Memory ram;
    private Memory rom;
    private boolean isRunning = false;

    private int updatesRequested = 0;

    private UiUpdater uiUpdater;

    private static final int BUS_BOTTOM = 0x0000;
    private static final int BUS_TOP    = 0xffff;

    private static final int MEMORY_BASE = 0x0000;
    private static final int MEMORY_SIZE = 0xc000; // 48 KB

    private static final int ROM_BASE = 0xe000;
    private static final int ROM_SIZE = 0x2000; // 8 KB

    public static final int ACIA_BASE = 0xc000;

    private static final int MAX_REQUESTS_BETWEEN_UPDATES = 25000;

    public Simulator() throws MemoryRangeException {
        this.acia = new Acia(ACIA_BASE);
        this.bus = new Bus(BUS_BOTTOM, BUS_TOP);
        this.cpu = new Cpu();
        this.ram = new Memory(MEMORY_BASE, MEMORY_SIZE, false);
        // TODO: Load this ROM from a file, naturally!
        this.rom = new Memory(ROM_BASE, ROM_SIZE, false);
        bus.addCpu(cpu);
        bus.addDevice(acia);
        bus.addDevice(ram);
        bus.addDevice(rom);
    }

    public void loadProgram(byte[] program, int startAddress) throws MemoryAccessException {
        cpu.setResetVector(startAddress);

        int addr = startAddress, i;
        for (i = 0; i < program.length; i++) {
            bus.write(addr++, program[i] & 0xff);
        }

        logger.log(Level.INFO, "Loaded " + i + " bytes at address 0x" +
                               Integer.toString(startAddress, 16));
    }

    public void setUiUpdater(UiUpdater uiUpdater) {
        this.uiUpdater = uiUpdater;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void requestStop() {
        isRunning = false;
    }

    public void reset() throws MemoryAccessException {
        cpu.reset();
    }

    public void clearMemory() {
        ram.fill(0x00);
    }

    public int getProcessorStatus() {
        return cpu.getProcessorStatus();
    }

    public Cpu getCpu() {
        return this.cpu;
    }

    public long memorySize() {
        return MEMORY_SIZE;
    }

    public void run() {
        logger.log(Level.INFO, "Entering 'run' on main Simulator thread");
        isRunning = true;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                uiUpdater.simulatorDidStart();
            }
        });

        try {
            while (isRunning && !cpu.getBreakFlag()) {
                step();
            }
        } catch (SymonException ex) {
            logger.log(Level.SEVERE, "Exception in main simulator run thread. Exiting run.");
            ex.printStackTrace();
        }

        logger.log(Level.INFO, "Exiting 'run'. BREAK=" + cpu.getBreakBit() + "; RUN_FLAG=" + isRunning);
        isRunning = false;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                uiUpdater.simulatorDidStop();
            }
        });

    }


    public void step() throws MemoryAccessException, FifoUnderrunException {

        cpu.step();

        // TODO: ACIA interrupt handling. For now, poll ACIA on each step.

        // Read from the ACIA and add to the UiUpdater buffer
        while (acia.hasTxChar()) {
            uiUpdater.consoleWrite(acia.txRead());
        }

        // This is a very expensive update, and we're doing it without
        // a delay, so we don't want to overwhelm the Swing event processing thread
        // with requests. Limit the number of ui updates that can be performed.
        if (updatesRequested++ > MAX_REQUESTS_BETWEEN_UPDATES) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    uiUpdater.updateUi();
                }
            });
            updatesRequested = 0;
        }
    }
}

