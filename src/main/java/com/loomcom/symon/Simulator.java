package com.loomcom.symon;

import com.loomcom.symon.devices.Acia;
import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;
import com.loomcom.symon.exceptions.SymonException;
import com.loomcom.symon.ui.PreferencesDialog;
import com.loomcom.symon.ui.StatusPanel;
import com.loomcom.symon.ui.Console;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

public class Simulator implements ActionListener, Observer {

    // Constants used by the simulated system. These define the memory map.
    private static final int BUS_BOTTOM = 0x0000;
    private static final int BUS_TOP    = 0xffff;

    private static final int MEMORY_BASE = 0x0000;
    private static final int MEMORY_SIZE = 0xc000; // 48 KB

    private static final int ROM_BASE = 0xe000;
    private static final int ROM_SIZE = 0x2000; // 8 KB

    private static final int ACIA_BASE = 0xc000;

    // The delay in microseconds between steps.
    private static final int DELAY_BETWEEN_STEPS_NS = 1000;

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
    private final Memory ram;
    private final Memory rom;

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

    // TODO: loadMenuItem seriously violates encapsulation!
    // A far better solution would be to extend JMenu and add callback
    // methods to enable and disable menus as required.

    // Menu Items
    private JMenuItem    loadMenuItem;

    private JFileChooser fileChooser;
    private PreferencesDialog  preferences;

    public Simulator() throws MemoryRangeException {
        this.acia = new Acia(ACIA_BASE);
        this.bus = new Bus(BUS_BOTTOM, BUS_TOP);
        this.cpu = new Cpu();
        this.ram = new Memory(MEMORY_BASE, MEMORY_SIZE, false);

        // TODO: Load this ROM from a file, of course!
        this.rom = new Memory(ROM_BASE, ROM_SIZE, false);

        bus.addCpu(cpu);
        bus.addDevice(acia);
        bus.addDevice(ram);
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
        mainWindow.setJMenuBar(createMenuBar());

        // UI components used for I/O.
        this.console = new com.loomcom.symon.ui.Console();
        this.statusPane = new StatusPanel();

        // File Chooser
        fileChooser = new JFileChooser();
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

        loadMenuItem = new JMenuItem("Load Program");
        loadMenuItem.setMnemonic(KeyEvent.VK_L);

        JMenuItem prefsItem = new JMenuItem("Preferences...");
        prefsItem.setMnemonic(KeyEvent.VK_P);

        JMenuItem quitItem = new JMenuItem("Quit");
        quitItem.setMnemonic(KeyEvent.VK_Q);

        loadMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                handleProgramLoad();
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

        fileMenu.add(loadMenuItem);
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
            handleReset();
        } else if (actionEvent.getSource() == stepButton) {
            handleStep();
        } else if (actionEvent.getSource() == runStopButton) {
            // Shift focus to the console.
            console.requestFocus();
            if (runLoop != null && runLoop.isRunning()) {
                runLoop.requestStop();
                runLoop.interrupt();
                runLoop = null;
            } else {
                // Spin up the new run loop
                runLoop = new RunLoop();
                runLoop.start();
            }
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

    private void handleReset() {
        if (runLoop != null && runLoop.isRunning()) {
            runLoop.requestStop();
            runLoop.interrupt();
            runLoop = null;
        }

        try {
            logger.log(Level.INFO, "Reset requested. Resetting CPU and clearing memory.");
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
            // The simulator is lazy about updating the UI for performance reasons, so always request an
            // immediate update after stepping manually.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // Now update the state
                    statusPane.updateState(cpu);
                }
            });
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

        delayLoop();

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
        if (console.hasInput()) {
            acia.rxWrite((int)console.readInputChar());
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
        cpu.setResetVector(startAddress);

        int addr = startAddress, i;
        for (i = 0; i < program.length; i++) {
            bus.write(addr++, program[i] & 0xff);
        }

        logger.log(Level.INFO, "Loaded " + i + " bytes at address 0x" +
                               Integer.toString(startAddress, 16));

        // After loading, be sure to reset and
        // Reset (but don't clear memory, naturally)
        cpu.reset();

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
                    simulator.handleReset();
                } catch (MemoryRangeException e) {
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

    /*
     * Perform a busy-loop for DELAY_BETWEEN_STEPS_NS nanoseconds
     */
    private void delayLoop() {
        long startTime = System.nanoTime();
        long stopTime = startTime + DELAY_BETWEEN_STEPS_NS;
        // Busy loop
        while (System.nanoTime() < stopTime) { ; }
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
                    // Don't allow step while the simulator is running
                    stepButton.setEnabled(false);
                    loadMenuItem.setEnabled(false);
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
                    // Allow step while the simulator is stopped
                    stepButton.setEnabled(true);
                    loadMenuItem.setEnabled(true);
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