/*
 * Copyright (c) 2008-2013 Seth J. Morabito <sethm@loomcom.com>
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

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.FifoUnderrunException;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;
import com.loomcom.symon.exceptions.SymonException;
import com.loomcom.symon.machines.Machine;
import com.loomcom.symon.ui.*;
import com.loomcom.symon.ui.Console;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
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
public class Simulator {

    // UI constants
    private static final int  DEFAULT_FONT_SIZE    = 12;
    private static final Font DEFAULT_FONT         = new Font(Font.MONOSPACED, Font.PLAIN, DEFAULT_FONT_SIZE);
    private static final int  CONSOLE_BORDER_WIDTH = 10;

    // Since it is very expensive to update the UI with Swing's Event Dispatch Thread, we can't afford
    // to refresh the status view on every simulated clock cycle. Instead, we will only refresh the status view
    // after this number of steps when running normally.
    //
    // Since we're simulating a 1MHz 6502 here, we have a 1 us delay between steps. Setting this to 20000
    // should give us a status update about every 100 ms.
    //
    // TODO: Work around the event dispatch thread with custom painting code instead of relying on Swing.
    //
    private static final int MAX_STEPS_BETWEEN_UPDATES = 20000;

    private final static Logger logger = Logger.getLogger(Simulator.class.getName());

    // The simulated machine
    private Machine machine;

    // Number of CPU steps between CRT repaints.
    // TODO: Dynamically refresh the value at runtime based on performance figures to reach ~ 30fps.
    private long stepsBetweenCrtcRefreshes = 2500;

    // A counter to keep track of the number of UI updates that have been
    // requested
    private int stepsSinceLastUpdate = 0;
    private int stepsSinceLastCrtcRefresh = 0;

    // The number of steps to run per click of the "Step" button
    private int stepsPerClick = 1;

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

    private VideoWindow videoWindow;

    private SimulatorMenu menuBar;

    private RunLoop     runLoop;
    private Console     console;
    private StatusPanel statusPane;

    private JButton runStopButton;
    private JButton stepButton;
    private JButton softResetButton;
    private JButton hardResetButton;
    private JComboBox<String> stepCountBox;

    private JFileChooser      fileChooser;
    private PreferencesDialog preferences;

    private final Object commandMonitorObject = new Object();
    private MAIN_CMD command = MAIN_CMD.NONE;
    public static enum MAIN_CMD  {
        NONE,
        SELECTMACHINE
    }
    
    /**
     * The list of step counts that will appear in the "Step" drop-down.
     */
    private static final String[] STEPS = {"1", "5", "10", "20", "50", "100"};

    public Simulator(Class machineClass) throws Exception {
        this.machine = (Machine) machineClass.getConstructors()[0].newInstance();
    }

    /**
     * Display the main simulator UI.
     */
    public void createAndShowUi() throws IOException {
        mainWindow = new JFrame();
        mainWindow.setTitle("6502 Simulator - " + machine.getName());
        mainWindow.setResizable(false);
        mainWindow.getContentPane().setLayout(new BorderLayout());

        // UI components used for I/O.
        this.console = new com.loomcom.symon.ui.Console(80, 25, DEFAULT_FONT);
        this.statusPane = new StatusPanel(machine);

        console.setBorderWidth(CONSOLE_BORDER_WIDTH);

        // File Chooser
        fileChooser = new JFileChooser(System.getProperty("user.dir"));
        preferences = new PreferencesDialog(mainWindow, true);

        // Panel for Console and Buttons
        JPanel consoleContainer = new JPanel();
        JPanel buttonContainer = new JPanel();

        consoleContainer.setLayout(new BorderLayout());
        consoleContainer.setBorder(new EmptyBorder(10, 10, 10, 0));
        buttonContainer.setLayout(new FlowLayout());

        runStopButton = new JButton("Run");
        stepButton = new JButton("Step");
        softResetButton = new JButton("Soft Reset");
        hardResetButton = new JButton("Hard Reset");

        stepCountBox = new JComboBox<String>(STEPS);
        stepCountBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    JComboBox cb = (JComboBox) actionEvent.getSource();
                    stepsPerClick = Integer.parseInt((String) cb.getSelectedItem());
                } catch (NumberFormatException ex) {
                    stepsPerClick = 1;
                    stepCountBox.setSelectedIndex(0);
                }
            }
        });

        buttonContainer.add(runStopButton);
        buttonContainer.add(stepButton);
        buttonContainer.add(stepCountBox);
        buttonContainer.add(softResetButton);
        buttonContainer.add(hardResetButton);

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
                handleStep(stepsPerClick);
            }
        });

        softResetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                // If this was a CTRL-click, do a hard reset.
                handleReset(false);
            }
        });

        hardResetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                // If this was a CTRL-click, do a hard reset.
                handleReset(true);
            }
        });

        // Prepare the log window
        traceLog = new TraceLog();

        // Prepare the memory window
        memoryWindow = new MemoryWindow(machine.getBus());

        // Composite Video and 6545 CRTC
        if(machine.getCrtc() != null) {
            videoWindow = new VideoWindow(machine.getCrtc(), 2, 2);
        }

        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // The Menu. This comes last, because it relies on other components having
        // already been initialized.
        menuBar = new SimulatorMenu();
        mainWindow.setJMenuBar(menuBar);

        mainWindow.pack();
        mainWindow.setVisible(true);

        console.requestFocus();
        handleReset(false);
    }
    
    public MAIN_CMD waitForCommand() {
        synchronized(commandMonitorObject) {
            try {
                commandMonitorObject.wait();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        return command;
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
    private void handleReset(boolean isColdReset) {
        if (runLoop != null && runLoop.isRunning()) {
            runLoop.requestStop();
            runLoop.interrupt();
            runLoop = null;
        }

        try {
            logger.log(Level.INFO, "Reset requested. Resetting CPU.");
            // Reset CPU
            machine.getCpu().reset();
            // Clear the console.
            console.reset();
            // Reset the trace log.
            traceLog.reset();
            // If we're doing a cold reset, clear the memory.
            if (isColdReset) {
                Memory mem = machine.getRam();
                if (mem != null) {
                    mem.fill(0);
                }
            }
            // Update status.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // Now update the state
                    statusPane.updateState();
                    memoryWindow.updateState();
                }
            });
        } catch (MemoryAccessException ex) {
            logger.log(Level.SEVERE, "Exception during simulator reset: " + ex.getMessage());
        }
    }

    /**
     * Step the requested number of times, and immediately refresh the UI.
     */
    private void handleStep(int numSteps) {
        try {
            for (int i = 0; i < numSteps; i++) {
                step();
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (traceLog.isVisible()) {
                        traceLog.refresh();
                    }
                    statusPane.updateState();
                    memoryWindow.updateState();
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
        machine.getCpu().step();

        traceLog.append(machine.getCpu().getCpuState());

        // Read from the ACIA and immediately update the console if there's
        // output ready.
        if (machine.getAcia() != null && machine.getAcia().hasTxChar()) {
            // This is thread-safe
            console.print(Character.toString((char) machine.getAcia().txRead()));
            console.repaint();
        }

        // If a key has been pressed, fill the ACIA.
        // TODO: Interrupt handling.
        try {
            if (machine.getAcia() != null && console.hasInput()) {
                machine.getAcia().rxWrite((int) console.readInputChar());
            }
        } catch (FifoUnderrunException ex) {
            logger.severe("Console type-ahead buffer underrun!");
        }

        if (videoWindow != null && stepsSinceLastCrtcRefresh++ > stepsBetweenCrtcRefreshes) {
            stepsSinceLastCrtcRefresh = 0;
        }

        // This is a very expensive update, and we're doing it without
        // a delay, so we don't want to overwhelm the Swing event processing thread
        // with requests. Limit the number of ui updates that can be performed.
        if (stepsSinceLastUpdate++ > MAX_STEPS_BETWEEN_UPDATES) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // Now update the state
                    statusPane.updateState();
                    memoryWindow.updateState();
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
            machine.getBus().write(addr++, program[i] & 0xff);
        }

        logger.log(Level.INFO, "Loaded " + i + " bytes at address 0x" +
                               Integer.toString(startAddress, 16));

        // After loading, be sure to reset and
        // Reset (but don't clear memory, naturally)
        machine.getCpu().reset();

        // Reset the stack program counter
        machine.getCpu().setProgramCounter(preferences.getProgramStartAddress());

        // Immediately update the UI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Now update the state
                statusPane.updateState();
                memoryWindow.updateState();
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
                    // Don't allow step while the simulator is running
                    stepButton.setEnabled(false);
                    stepCountBox.setEnabled(false);
                    menuBar.simulatorDidStart();
                    // Toggle the state of the run button
                    runStopButton.setText("Stop");
                }
            });

            try {
                do {
                    step();
                } while (shouldContinue());
            } catch (SymonException ex) {
                logger.log(Level.SEVERE, "Exception in main simulator run thread. Exiting run.");
                ex.printStackTrace();
            }

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    statusPane.updateState();
                    memoryWindow.updateState();
                    runStopButton.setText("Run");
                    stepButton.setEnabled(true);
                    stepCountBox.setEnabled(true);
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

        /**
         * Returns true if the run loop should proceed to the next step.
         *
         * @return True if the run loop should proceed to the next step.
         */
        private boolean shouldContinue() {
            return isRunning && !(preferences.getHaltOnBreak() && machine.getCpu().getInstruction() == 0x00);
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

                        if (fileSize > machine.getMemorySize()) {
                            throw new IOException("Program of size $" +
                                    Integer.toString((int)fileSize, 16) +
                                    " will not fit in available memory of size $" +
                                    Integer.toString(machine.getMemorySize(), 16) +
                                    ".");
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

                        if (fileSize != machine.getRomSize()) {
                            throw new IOException("ROM file must be exactly " + String.valueOf(machine.getRomSize()) + " bytes.");
                        } else {
                            
                            // Load the new ROM image
                            Memory rom = Memory.makeROM(machine.getRomBase(), machine.getRomBase() + machine.getRomSize() - 1, romFile);
                            machine.setRom(rom);

                            // Now, reset
                            machine.getCpu().reset();

                            logger.log(Level.INFO, "ROM File `" + romFile.getName() + "' loaded at " +
                                                   String.format("0x%04X", machine.getRomBase()));
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
    
    class SelectMachineAction extends AbstractAction {
        public SelectMachineAction() {
            super("Switch emulated machine...", null);
            putValue(SHORT_DESCRIPTION, "Select the type of the machine to be emulated");
            putValue(MNEMONIC_KEY, KeyEvent.VK_M);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            if(runLoop != null) {
                runLoop.requestStop();
            }

            memoryWindow.dispose();
            traceLog.dispose();
            if(videoWindow != null) {
                videoWindow.dispose();
            }
            mainWindow.dispose();

            command = MAIN_CMD.SELECTMACHINE;
            synchronized(commandMonitorObject) {
                commandMonitorObject.notifyAll();
            }
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

    class ToggleVideoWindowAction extends AbstractAction {
        public ToggleVideoWindowAction() {
            super("Video Window", null);
            putValue(SHORT_DESCRIPTION, "Show or Hide the Video Window");
        }

        public void actionPerformed(ActionEvent actionEvent) {
            synchronized (videoWindow) {
                if (videoWindow.isVisible()) {
                    videoWindow.setVisible(false);
                } else {
                    videoWindow.setVisible(true);
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
            if (loadRomItem != null) {
                loadRomItem.setEnabled(false);
            }
        }

        /**
         * Enable menu items that should be available while the simulator is stopped.
         */
        public void simulatorDidStop() {
            loadProgramItem.setEnabled(true);
            if (loadRomItem != null) {
                loadRomItem.setEnabled(true);
            }
        }

        private void initMenu() {
            /*
             * File Menu
             */

            JMenu fileMenu = new JMenu("File");

            loadProgramItem = new JMenuItem(new LoadProgramAction());
            fileMenu.add(loadProgramItem);

            // Simple Machine does not implement a ROM, so it makes no sense to
            // offer a ROM load option.
            if (machine.getRom() != null) {
                loadRomItem = new JMenuItem(new LoadRomAction());
                fileMenu.add(loadRomItem);
            }

            JMenuItem prefsItem = new JMenuItem(new ShowPrefsAction());
            fileMenu.add(prefsItem);

            JMenuItem selectMachineItem = new JMenuItem(new SelectMachineAction());
            fileMenu.add(selectMachineItem);

            JMenuItem quitItem = new JMenuItem(new QuitAction());
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

            final JCheckBoxMenuItem showTraceLog = new JCheckBoxMenuItem(new ToggleTraceWindowAction());
            // Un-check the menu item if the user closes the window directly
            traceLog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    showTraceLog.setSelected(false);
                }
            });
            viewMenu.add(showTraceLog);

            final JCheckBoxMenuItem showMemoryTable = new JCheckBoxMenuItem(new ToggleMemoryWindowAction());
            // Un-check the menu item if the user closes the window directly
            memoryWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    showMemoryTable.setSelected(false);
                }
            });
            viewMenu.add(showMemoryTable);

            if(videoWindow != null) {
                final JCheckBoxMenuItem showVideoWindow = new JCheckBoxMenuItem(new ToggleVideoWindowAction());
                videoWindow.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        showVideoWindow.setSelected(false);
                    }
                });
                viewMenu.add(showVideoWindow);
            }

            add(viewMenu);
        }

        private void makeFontSizeMenuItem(int size, JMenu fontSubMenu, ButtonGroup group) {
            Action action = new SetFontAction(size);

            JCheckBoxMenuItem item = new JCheckBoxMenuItem(action);
            item.setSelected(size == DEFAULT_FONT_SIZE);
            fontSubMenu.add(item);
            group.add(item);
        }
    }

}
