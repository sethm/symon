/*
 * Copyright (c) 2008-2012 Seth J. Morabito <sethm@loomcom.com>
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

package com.loomcom.symon;

import com.loomcom.symon.devices.Acia;
import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.devices.Via;
import com.loomcom.symon.exceptions.FifoUnderrunException;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;
import com.loomcom.symon.exceptions.SymonException;
import com.loomcom.symon.ui.Console;
import com.loomcom.symon.ui.PreferencesDialog;
import com.loomcom.symon.ui.StatusPanel;
import sun.rmi.rmic.iiop.DirectoryLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Symon Simulator Interface and Control.
 *
 * This class provides a control and I/O system for the simulated 6502 system.
 * It includes the simulated CPU itself, as well as 32KB of RAM, 16KB of ROM,
 * and a simulated ACIA for serial I/O. The ACIA is attached to a dumb terminal
 * with a basic 80x25 character display.
 *
 */
public class Simulator implements ActionListener, Observer {

    // Constants used by the simulated system. These define the memory map.
    private static final int BUS_BOTTOM = 0x0000;
    private static final int BUS_TOP    = 0xffff;

    // 32K of RAM from $0000 - $7FFF
    private static final int MEMORY_BASE = 0x0000;
    private static final int MEMORY_SIZE = 0x8000;

    // VIA at $8000-$800F
    private static final int VIA_BASE = 0x8000;

    // ACIA at $8800-$8803
    private static final int ACIA_BASE = 0x8800;

    // 16KB ROM at $C000-$FFFF
    private static final int ROM_BASE = 0xC000;
    private static final int ROM_SIZE = 0x4000;

    // Since it is very expensive to update the UI with Swing's Event Dispatch Thread, we can't afford
    // to refresh the view on every simulated clock cycle. Instead, we will only refresh the view after this
    // number of steps when running normally.
    //
    // Since we're simulating a 1MHz 6502 here, we have a 1 us delay between steps. Setting this to 10000
    // should give us a status update every 10 ms.
    //
    // TODO: Work around the event dispatch thread with custom painting code instead of relying on Swing.
    //
    private static final int MAX_STEPS_BETWEEN_UPDATES = 10000;

    private final static Logger logger = Logger.getLogger(Simulator.class.getName());

    // The simulated peripherals
    private final Bus    bus;
    private final Cpu    cpu;
    private final Acia   acia;
    private final Via    via;
    private final Memory ram;
    private       Memory rom;

    // A counter to keep track of the number of UI updates that have been
    // requested
    private int stepsSinceLastUpdate = 0;

    private JFrame       mainWindow;
    private RunLoop      runLoop;
    private Console      console;
    private StatusPanel  statusPane;

    private JButton      runStopButton;
    private JButton      stepButton;
    private JButton      resetButton;

    // The most recently read key code
    private char         keyBuffer;

    // TODO: loadProgramItem seriously violates encapsulation!
    // A far better solution would be to extend JMenu and add callback
    // methods to enable and disable menus as required.

    // Menu Items
    private JMenuItem    loadProgramItem;
    private JMenuItem    loadRomItem;

    private JFileChooser fileChooser;
    private PreferencesDialog  preferences;

    public Simulator() throws MemoryRangeException, IOException {
        this.acia = new Acia(ACIA_BASE);
        this.via = new Via(VIA_BASE);
        this.bus = new Bus(BUS_BOTTOM, BUS_TOP);
        this.cpu = new Cpu();
        this.ram = new Memory(MEMORY_BASE, MEMORY_SIZE, false);

        bus.addCpu(cpu);
        bus.addDevice(ram);
        bus.addDevice(via);
        bus.addDevice(acia);

        // TODO: Make this configurable, of course.
        File romImage = new File("rom.bin");
        if (romImage.canRead()) {
            logger.info("Loading ROM image from file " + romImage);
            this.rom = Memory.makeROM(ROM_BASE, ROM_SIZE, romImage);
            bus.addDevice(rom);
        }
    }

    /**
     * Display the main simulator UI.
     */
    public void createAndShowUi() {
        mainWindow = new JFrame();
        mainWindow.setTitle("Symon 6502 Simulator");
        mainWindow.setResizable(false);
        mainWindow.getContentPane().setLayout(new BorderLayout());

        // The Menu
        mainWindow.setJMenuBar(createMenuBar());

        // UI components used for I/O.
        this.console = new com.loomcom.symon.ui.Console();
        this.statusPane = new StatusPanel();

        // File Chooser
        fileChooser = new JFileChooser(System.getProperty("user.dir"));
        preferences = new PreferencesDialog(mainWindow, true);
        preferences.addObserver(this);

        // Panel for Console and Buttons
        JPanel controlsContainer = new JPanel();
        JPanel buttonContainer = new JPanel();
        Dimension buttonPanelSize = new Dimension(console.getWidth(), 36);

        buttonContainer.setMinimumSize(buttonPanelSize);
        buttonContainer.setMaximumSize(buttonPanelSize);
        buttonContainer.setPreferredSize(buttonPanelSize);

        controlsContainer.setLayout(new BorderLayout());
        buttonContainer.setLayout(new FlowLayout());

        runStopButton = new JButton("Run");
        stepButton = new JButton("Step");
        resetButton = new JButton("Reset");

        buttonContainer.add(runStopButton);
        buttonContainer.add(stepButton);
        buttonContainer.add(resetButton);

        // Left side - console
        controlsContainer.add(console, BorderLayout.PAGE_START);
        mainWindow.getContentPane().add(controlsContainer, BorderLayout.LINE_START);

        // Right side - status pane
        mainWindow.getContentPane().add(statusPane, BorderLayout.LINE_END);

        // Bottom - buttons.
        mainWindow.getContentPane().add(buttonContainer, BorderLayout.PAGE_END);

        runStopButton.addActionListener(this);
        stepButton.addActionListener(this);
        resetButton.addActionListener(this);

        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        console.requestFocus();

        mainWindow.pack();
        mainWindow.setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");

        menuBar.add(fileMenu);

        loadProgramItem = new JMenuItem("Load Program");
        loadProgramItem.setMnemonic(KeyEvent.VK_L);

        loadRomItem = new JMenuItem("Load ROM...");
        loadRomItem.setMnemonic(KeyEvent.VK_R);

        JMenuItem prefsItem = new JMenuItem("Preferences...");
        prefsItem.setMnemonic(KeyEvent.VK_P);

        JMenuItem quitItem = new JMenuItem("Quit");
        quitItem.setMnemonic(KeyEvent.VK_Q);

        loadProgramItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                handleProgramLoad();
            }
        });

        loadRomItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                handleRomLoad();
            }
        });

        prefsItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                showAndUpdatePreferences();
            }
        });

        quitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                handleQuit();
            }
        });

        fileMenu.add(loadProgramItem);
        fileMenu.add(loadRomItem);
        fileMenu.add(prefsItem);
        fileMenu.add(quitItem);

        return menuBar;
    }

    public void showAndUpdatePreferences() {
        preferences.getDialog().setVisible(true);
    }

    /**
     * Receive an ActionEvent from the UI, and act on it.
     */
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == resetButton) {
            coldReset();
        } else if (actionEvent.getSource() == stepButton) {
            handleStep();
        } else if (actionEvent.getSource() == runStopButton) {
            if (runLoop != null && runLoop.isRunning()) {
                handleStop();
            } else {
                handleStart();
            }
        }
    }

    private void handleStart() {
        // Shift focus to the console.
        console.requestFocus();
        // Spin up the new run loop
        runLoop = new RunLoop();
        runLoop.start();
    }

    private void handleStop() {
        runLoop.requestStop();
        runLoop.interrupt();
        runLoop = null;
        simulatorDidStop();
    }

    /*
     * Handle post-stop actions.
     */
    private void simulatorDidStop() {
        // The simulator is lazy about updating the UI for performance reasons, so always request an
        // immediate update after stopping.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Now update the state
                statusPane.updateState(cpu);
            }
        });

        // TODO: Write to Log window, if frame is visible.
        // TODO: Update memory window, if frame is visible.
    }

    // TODO: Alert user of errors.
    private void handleRomLoad() {
        try {
            int retVal = fileChooser.showOpenDialog(mainWindow);
            if (retVal == JFileChooser.APPROVE_OPTION) {
                File romFile = fileChooser.getSelectedFile();
                if (romFile.canRead()) {
                    long fileSize = romFile.length();

                    if (fileSize != ROM_SIZE) {
                        throw new IOException("ROM file must be exactly " + String.valueOf(fileSize) + " bytes.");
                    } else {
                        if (rom != null) {
                            // Unload the existing ROM image.
                            bus.removeDevice(rom);
                        }
                        // Load the new ROM image
                        rom = Memory.makeROM(ROM_BASE, ROM_SIZE, romFile);
                        bus.addDevice(rom);

                        logger.log(Level.INFO, "ROM File `" + romFile.getName() + "' loaded at " +
                                               String.format("0x%04X", ROM_BASE));
                    }
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to read file: " + ex.getMessage());
            ex.printStackTrace();
        } catch (MemoryRangeException ex) {
            logger.log(Level.SEVERE, "Memory range error loading ROM");
            ex.printStackTrace();
        }
    }

    /**
     * Display a file chooser prompting the user to load a binary program.
     * After the user selects a file, read it in starting at PROGRAM_START_ADDRESS.
     */
    private void handleProgramLoad() {
        try {
            int retVal = fileChooser.showOpenDialog(mainWindow);
            if (retVal == JFileChooser.APPROVE_OPTION) {
                File f = fileChooser.getSelectedFile();
                if (f.canRead()) {
                    long fileSize = f.length();

                    if (fileSize > MEMORY_SIZE) {
                        throw new IOException("Program will not fit in available memory.");
                    } else {
                        byte[] program = new byte[(int) fileSize];
                        int i = 0;
                        FileInputStream fis = new FileInputStream(f);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        DataInputStream dis = new DataInputStream(bis);
                        while (dis.available() != 0) {
                            program[i++] = dis.readByte();
                        }

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                console.reset();
                            }
                        });

                        // Now load the program at the starting address.
                        loadProgram(program, preferences.getProgramStartAddress());
                    }
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to read file: " + ex.getMessage());
            ex.printStackTrace();
        } catch (MemoryAccessException ex) {
            logger.log(Level.SEVERE, "Memory access error loading program");
            ex.printStackTrace();
        }
    }

    private void coldReset() {
        if (runLoop != null && runLoop.isRunning()) {
            runLoop.requestStop();
            runLoop.interrupt();
            runLoop = null;
        }

        try {
            logger.log(Level.INFO, "Cold reset requested. Resetting CPU and clearing memory.");
            // Reset and clear memory
            cpu.reset();
            ram.fill(0x00);
            // Clear the console.
            console.reset();
            // Update status.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // Now update the state
                    statusPane.updateState(cpu);
                }
            });
        } catch (MemoryAccessException ex) {
            logger.log(Level.SEVERE, "Exception during simulator reset: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Step once, and immediately refresh the UI.
     */
    private void handleStep() {
        try {
            step();
            simulatorDidStop();
        } catch (SymonException ex) {
            logger.log(Level.SEVERE, "Exception during simulator step: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Handle a request to quit.
     */
    private void handleQuit() {
        if (runLoop != null && runLoop.isRunning()) {
            runLoop.requestStop();
            runLoop.interrupt();
        }
        System.exit(0);
    }

    /**
     * Perform a single step of the simulated system.
     */
    private void step() throws MemoryAccessException {

        cpu.step();

        // Read from the ACIA and immediately update the console if there's
        // output ready.
        if (acia.hasTxChar()) {
            // This is thread-safe
            console.print(Character.toString((char)acia.txRead()));
            console.repaint();
        }

        // If a key has been pressed, fill the ACIA.
        // TODO: Interrupt handling.
        try {
            if (console.hasInput()) {
                acia.rxWrite((int)console.readInputChar());
            }
        } catch (FifoUnderrunException ex) {
            logger.severe("Console type-ahead buffer underrun!");
        }

        // This is a very expensive update, and we're doing it without
        // a delay, so we don't want to overwhelm the Swing event processing thread
        // with requests. Limit the number of ui updates that can be performed.
        if (stepsSinceLastUpdate++ > MAX_STEPS_BETWEEN_UPDATES) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // Now update the state
                    statusPane.updateState(cpu);
                }
            });
            stepsSinceLastUpdate = 0;
        }

    }

    /**
     * Load a program into memory at the simulatorDidStart address.
     */
    private void loadProgram(byte[] program, int startAddress) throws MemoryAccessException {
        int addr = startAddress, i;
        for (i = 0; i < program.length; i++) {
            bus.write(addr++, program[i] & 0xff);
        }

        logger.log(Level.INFO, "Loaded " + i + " bytes at address 0x" +
                               Integer.toString(startAddress, 16));

        // After loading, be sure to reset and
        // Reset (but don't clear memory, naturally)
        cpu.reset();

        // Reset the stack program counter
        cpu.setProgramCounter(preferences.getProgramStartAddress());

        // Immediately update the UI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Now update the state
                statusPane.updateState(cpu);
            }
        });
    }

    public static void main(String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    // Create the main UI window
                    Simulator simulator = new Simulator();
                    simulator.createAndShowUi();
                    // Reset the simulator.
                    simulator.coldReset();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * The configuration has changed. Re-load.
     *
     * @param observable
     * @param o
     */
    public void update(Observable observable, Object o) {
        // Instance equality should work here, there is only one instance.
        if (observable == preferences) {
            // TODO: Update ACIA base address if it has changed.

            int oldBorderWidth = console.getBorderWidth();
            if (oldBorderWidth != preferences.getBorderWidth()) {
                // Resize the main window if the border width has changed.
                console.setBorderWidth(preferences.getBorderWidth());
                mainWindow.pack();
            }
        }
    }


    /**
     * The main run thread.
     */
    class RunLoop extends Thread {
        private boolean isRunning = false;

        public boolean isRunning() {
            return isRunning;
        }

        public void requestStop() {
            isRunning = false;
        }

        public void run() {
            logger.log(Level.INFO, "Starting main run loop.");
            isRunning = true;

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // Tell the console to start handling key presses
                    console.startListening();
                    // Don't allow step while the simulator is running
                    stepButton.setEnabled(false);
                    loadProgramItem.setEnabled(false);
                    loadRomItem.setEnabled(false);
                    // Toggle the state of the run button
                    runStopButton.setText("Stop");
                }
            });

            try {
                // TODO: Interrupts - both software and hardware. i.e., jump to address stored in FFFE/FFFF on BRK.
                while (isRunning && !cpu.getBreakFlag()) {
                    step();
                }
            } catch (SymonException ex) {
                logger.log(Level.SEVERE, "Exception in main simulator run thread. Exiting run.");
                ex.printStackTrace();
            }

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    console.stopListening();
                    // Allow step while the simulator is stopped
                    stepButton.setEnabled(true);
                    loadProgramItem.setEnabled(true);
                    loadRomItem.setEnabled(true);
                    runStopButton.setText("Run");
                    // Now update the state
                    statusPane.updateState(cpu);
                }
            });

            logger.log(Level.INFO, "Exiting main run loop. BREAK=" + cpu.getBreakBit() + "; RUN_FLAG=" + isRunning);
            isRunning = false;
        }
    }
}
