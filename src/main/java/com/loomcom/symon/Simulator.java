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
import com.loomcom.symon.ui.*;
import com.loomcom.symon.ui.Console;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
 * <p/>
 * This class provides a control and I/O system for the simulated 6502 system.
 * It includes the simulated CPU itself, as well as 32KB of RAM, 16KB of ROM,
 * and a simulated ACIA for serial I/O. The ACIA is attached to a dumb terminal
 * with a basic 80x25 character display.
 */
public class Simulator implements Observer {

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

    private static final int  DEFAULT_FONT_SIZE = 12;
    private static final Font DEFAULT_FONT      = new Font(Font.MONOSPACED, Font.PLAIN, DEFAULT_FONT_SIZE);

    // Since it is very expensive to update the UI with Swing's Event Dispatch Thread, we can't afford
    // to refresh the status view on every simulated clock cycle. Instead, we will only refresh the status view
    // after this number of steps when running normally.
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

    /**
     * The Main Window is the primary control point for the simulator.
     * It is in charge of the menu, and sub-windows. It also shows the
     * CPU status at all times.
     */
    private JFrame mainWindow;

    /**
     * The Trace Window shows the most recent 50,000 CPU states.
     */
    private TraceLog traceLog;

    /**
     * The Memory Window shows the contents of one page of memory.
     */
    private MemoryWindow memoryWindow;

    /**
     * The Zero Page Window shows the contents of page 0.
     */
    private JFrame zeroPageWindow;

    private SimulatorMenu menuBar;

    private RunLoop     runLoop;
    private Console     console;
    private StatusPanel statusPane;

    private JButton runStopButton;
    private JButton stepButton;
    private JButton resetButton;

    private JFileChooser      fileChooser;
    private PreferencesDialog preferences;

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
        } else {
            logger.info("Default ROM file " + romImage +
                        " not found, loading empty R/W memory image.");
            this.rom = Memory.makeRAM(ROM_BASE, ROM_SIZE);
        }
        bus.addDevice(rom);
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
        menuBar = new SimulatorMenu();
        mainWindow.setJMenuBar(menuBar);

        // UI components used for I/O.
        this.console = new com.loomcom.symon.ui.Console(80, 25, DEFAULT_FONT);
        this.statusPane = new StatusPanel();

        // File Chooser
        fileChooser = new JFileChooser(System.getProperty("user.dir"));
        preferences = new PreferencesDialog(mainWindow, true);
        preferences.addObserver(this);

        // Panel for Console and Buttons
        JPanel consoleContainer = new JPanel();
        JPanel buttonContainer = new JPanel();

        consoleContainer.setLayout(new BorderLayout());
        consoleContainer.setBorder(new EmptyBorder(10, 10, 10, 0));
        buttonContainer.setLayout(new FlowLayout());

        runStopButton = new JButton("Run");
        stepButton = new JButton("Step");
        resetButton = new JButton("Reset");

        buttonContainer.add(runStopButton);
        buttonContainer.add(stepButton);
        buttonContainer.add(resetButton);

        // Left side - console
        consoleContainer.add(console, BorderLayout.CENTER);
        mainWindow.getContentPane().add(consoleContainer, BorderLayout.LINE_START);

        // Right side - status pane
        mainWindow.getContentPane().add(statusPane, BorderLayout.LINE_END);

        // Bottom - buttons.
        mainWindow.getContentPane().add(buttonContainer, BorderLayout.PAGE_END);

        runStopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (runLoop != null && runLoop.isRunning()) {
                    handleStop();
                } else {
                    handleStart();
                }
            }
        });

        stepButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                handleStep();
            }
        });

        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                handleReset();
            }
        });

        // Prepare the log window
        traceLog = new TraceLog();

        // Prepare the memory window
        memoryWindow = new MemoryWindow(bus);

        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        console.requestFocus();

        mainWindow.pack();
        mainWindow.setVisible(true);
    }

    private void handleStart() {
        // Shift focus to the console.
        console.requestFocus();
        // Spin up the new run loop
        runLoop = new RunLoop();
        runLoop.start();
        traceLog.simulatorDidStart();
    }

    private void handleStop() {
        runLoop.requestStop();
        runLoop.interrupt();
        runLoop = null;
    }

    /*
     * Perform a reset.
     */
    private void handleReset() {
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
            // Reset the trace log.
            traceLog.reset();
            // Update status.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // Now update the state
                    statusPane.updateState(cpu);
                }
            });
        } catch (MemoryAccessException ex) {
            logger.log(Level.SEVERE, "Exception during simulator reset: " + ex.getMessage());
        }
    }

    /**
     * Step once, and immediately refresh the UI.
     */
    private void handleStep() {
        try {
            step();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (traceLog.isVisible()) {
                        traceLog.refresh();
                    }
                    statusPane.updateState(cpu);
                }
            });
        } catch (SymonException ex) {
            logger.log(Level.SEVERE, "Exception during simulator step: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Perform a single step of the simulated system.
     */
    private void step() throws MemoryAccessException {
        cpu.step();

        traceLog.append(cpu.getCpuState());

        // Read from the ACIA and immediately update the console if there's
        // output ready.
        if (acia.hasTxChar()) {
            // This is thread-safe
            console.print(Character.toString((char) acia.txRead()));
            console.repaint();
        }

        // If a key has been pressed, fill the ACIA.
        // TODO: Interrupt handling.
        try {
            if (console.hasInput()) {
                acia.rxWrite((int) console.readInputChar());
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

    /**
     * The configuration has changed. Re-load.
     *
     * @param observable
     * @param o
     */
    public void update(Observable observable, Object o) {
        // Instance equality should work here, there is only one instance.
        if (observable == preferences) {
            int oldBorderWidth = console.getBorderWidth();
            if (oldBorderWidth != preferences.getBorderWidth()) {
                // Resize the main window if the border width has changed.
                console.setBorderWidth(preferences.getBorderWidth());
                mainWindow.pack();
            }
        }
    }

    /**
     * Main entry point to the simulator. Creates a simulator and shows the main
     * window.
     *
     * @param args
     */
    public static void main(String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    // Create the main UI window
                    Simulator simulator = new Simulator();
                    simulator.createAndShowUi();
                    // Reset the simulator.
                    simulator.handleReset();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
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
                    menuBar.simulatorDidStart();
                    // Toggle the state of the run button
                    runStopButton.setText("Stop");
                }
            });

            try {
                while (isRunning && !(preferences.getHaltOnBreak() && cpu.getBreakFlag())) {
                    step();
                }
            } catch (SymonException ex) {
                logger.log(Level.SEVERE, "Exception in main simulator run thread. Exiting run.");
                ex.printStackTrace();
            }

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    statusPane.updateState(cpu);
                    runStopButton.setText("Run");
                    stepButton.setEnabled(true);
                    if (traceLog.isVisible()) {
                        traceLog.refresh();
                    }
                    menuBar.simulatorDidStop();
                    traceLog.simulatorDidStop();
                    // TODO: Update memory window, if frame is visible.
                }
            });

            isRunning = false;
        }
    }

    class LoadProgramAction extends AbstractAction {
        public LoadProgramAction() {
            super("Load Program...", null);
            putValue(SHORT_DESCRIPTION, "Load a program into memory");
            putValue(MNEMONIC_KEY, KeyEvent.VK_L);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            // TODO: Error dialogs on failure.
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
                logger.log(Level.SEVERE, "Unable to read program file: " + ex.getMessage());
            } catch (MemoryAccessException ex) {
                logger.log(Level.SEVERE, "Memory access error loading program: " + ex.getMessage());
            }
        }
    }

    class LoadRomAction extends AbstractAction {
        public LoadRomAction() {
            super("Load ROM...", null);
            putValue(SHORT_DESCRIPTION, "Load a ROM image");
            putValue(MNEMONIC_KEY, KeyEvent.VK_R);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            // TODO: Error dialogs on failure.
            try {
                int retVal = fileChooser.showOpenDialog(mainWindow);
                if (retVal == JFileChooser.APPROVE_OPTION) {
                    File romFile = fileChooser.getSelectedFile();
                    if (romFile.canRead()) {
                        long fileSize = romFile.length();

                        if (fileSize != ROM_SIZE) {
                            throw new IOException("ROM file must be exactly " + String.valueOf(ROM_SIZE) + " bytes.");
                        } else {
                            if (rom != null) {
                                // Unload the existing ROM image.
                                bus.removeDevice(rom);
                            }
                            // Load the new ROM image
                            rom = Memory.makeROM(ROM_BASE, ROM_SIZE, romFile);
                            bus.addDevice(rom);

                            // Now, reset
                            cpu.reset();

                            logger.log(Level.INFO, "ROM File `" + romFile.getName() + "' loaded at " +
                                                   String.format("0x%04X", ROM_BASE));
                        }
                    }
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Unable to read ROM file: " + ex.getMessage());
            } catch (MemoryRangeException ex) {
                logger.log(Level.SEVERE, "Memory range error while loading ROM file: " + ex.getMessage());
            } catch (MemoryAccessException ex) {
                logger.log(Level.SEVERE, "Memory access error while loading ROM file: " + ex.getMessage());
            }
        }
    }

    class ShowPrefsAction extends AbstractAction {
        public ShowPrefsAction() {
            super("Preferences...", null);
            putValue(SHORT_DESCRIPTION, "Show Preferences Dialog");
            putValue(MNEMONIC_KEY, KeyEvent.VK_P);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            preferences.getDialog().setVisible(true);
        }
    }

    class QuitAction extends AbstractAction {
        public QuitAction() {
            super("Quit", null);
            putValue(SHORT_DESCRIPTION, "Exit the Simulator");
            putValue(MNEMONIC_KEY, KeyEvent.VK_Q);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            if (runLoop != null && runLoop.isRunning()) {
                runLoop.requestStop();
                runLoop.interrupt();
            }
            System.exit(0);
        }
    }

    class SetFontAction extends AbstractAction {
        private int size;

        public SetFontAction(int size) {
            super(Integer.toString(size) + " pt", null);
            this.size = size;
            putValue(SHORT_DESCRIPTION, "Set font to " + Integer.toString(size) + "pt.");
        }

        public void actionPerformed(ActionEvent actionEvent) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    console.setFont(new Font("Monospaced", Font.PLAIN, size));
                    mainWindow.pack();
                }
            });
        }
    }

    class ToggleTraceWindowAction extends AbstractAction {
        public ToggleTraceWindowAction() {
            super("Trace Log", null);
            putValue(SHORT_DESCRIPTION, "Show or Hide the Trace Log Window");
        }

        public void actionPerformed(ActionEvent actionEvent) {
            synchronized (traceLog) {
                if (traceLog.isVisible()) {
                    traceLog.setVisible(false);
                } else {
                    traceLog.refresh();
                    traceLog.setVisible(true);
                }
            }
        }
    }

    class ToggleMemoryWindowAction extends AbstractAction {
        public ToggleMemoryWindowAction() {
            super("Memory Window", null);
            putValue(SHORT_DESCRIPTION, "Show or Hide the Memory Window");
        }

        public void actionPerformed(ActionEvent actionEvent) {
            synchronized (memoryWindow) {
                if (memoryWindow.isVisible()) {
                    memoryWindow.setVisible(false);
                } else {
                    memoryWindow.setVisible(true);
                }
            }
        }
    }

    class SimulatorMenu extends JMenuBar {
        // Menu Items
        private JMenuItem loadProgramItem;
        private JMenuItem loadRomItem;

        /**
         * Create a new SimulatorMenu instance.
         */
        public SimulatorMenu() {
            initMenu();
        }

        /**
         * Disable menu items that should not be available during simulator execution.
         */
        public void simulatorDidStart() {
            loadProgramItem.setEnabled(false);
            loadRomItem.setEnabled(false);
        }

        /**
         * Enable menu items that should be available while the simulator is stopped.
         */
        public void simulatorDidStop() {
            loadProgramItem.setEnabled(true);
            loadRomItem.setEnabled(true);
        }

        private void initMenu() {
            /*
             * File Menu
             */

            JMenu fileMenu = new JMenu("File");

            loadProgramItem = new JMenuItem(new LoadProgramAction());
            loadRomItem = new JMenuItem(new LoadRomAction());
            JMenuItem prefsItem = new JMenuItem(new ShowPrefsAction());
            JMenuItem quitItem = new JMenuItem(new QuitAction());

            fileMenu.add(loadProgramItem);
            fileMenu.add(loadRomItem);
            fileMenu.add(prefsItem);
            fileMenu.add(quitItem);

            add(fileMenu);

            /*
             * View Menu
             */

            JMenu viewMenu = new JMenu("View");
            JMenu fontSubMenu = new JMenu("Console Font Size");
            ButtonGroup fontSizeGroup = new ButtonGroup();
            makeFontSizeMenuItem(10, fontSubMenu, fontSizeGroup);
            makeFontSizeMenuItem(11, fontSubMenu, fontSizeGroup);
            makeFontSizeMenuItem(12, fontSubMenu, fontSizeGroup);
            makeFontSizeMenuItem(13, fontSubMenu, fontSizeGroup);
            makeFontSizeMenuItem(14, fontSubMenu, fontSizeGroup);
            makeFontSizeMenuItem(15, fontSubMenu, fontSizeGroup);
            makeFontSizeMenuItem(16, fontSubMenu, fontSizeGroup);
            makeFontSizeMenuItem(17, fontSubMenu, fontSizeGroup);
            makeFontSizeMenuItem(18, fontSubMenu, fontSizeGroup);
            makeFontSizeMenuItem(19, fontSubMenu, fontSizeGroup);
            makeFontSizeMenuItem(20, fontSubMenu, fontSizeGroup);
            viewMenu.add(fontSubMenu);

            JRadioButtonMenuItem showTraceLog = new JRadioButtonMenuItem(new ToggleTraceWindowAction());
            viewMenu.add(showTraceLog);

            JRadioButtonMenuItem showMemoryTable = new JRadioButtonMenuItem(new ToggleMemoryWindowAction());
            viewMenu.add(showMemoryTable);

            add(viewMenu);
        }

        private void makeFontSizeMenuItem(int size, JMenu fontSubMenu, ButtonGroup group) {
            Action action = new SetFontAction(size);

            JRadioButtonMenuItem item = new JRadioButtonMenuItem(action);
            item.setSelected(size == DEFAULT_FONT_SIZE);
            fontSubMenu.add(item);
            group.add(item);
        }
    }

}
