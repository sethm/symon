package com.loomcom.symon;

import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;
import com.loomcom.symon.exceptions.SymonException;
import com.loomcom.symon.ui.Console;
import com.loomcom.symon.ui.StatusPane;
import com.loomcom.symon.ui.UiUpdater;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

public class MainWindow extends JFrame implements ActionListener {

    // Hard coded for now.
    public static final int PROGRAM_START_ADDRESS = 0x0300;

    private final static Logger logger = Logger.getLogger(MainWindow.class.getName());

    private final Simulator    simulator;
    private       Thread       simulatorThread;
    private final Console      console;
    private final StatusPane   statusPane;
    private final JButton      loadButton;
    private final JButton      runButton;
    private final JButton      stepButton;
    private final JButton      resetButton;
    private final JButton      quitButton;
    private final JFileChooser fileChooser;

    private final UiUpdater uiUpdater;

    public MainWindow(final Simulator simulator) {

        this.setLayout(new BorderLayout());

        this.simulator = simulator;

        // UI components used for I/O.
        this.console = new Console();
        this.statusPane = new StatusPane(simulator.getCpu());

        JPanel buttonContainer = new JPanel();
        JPanel controlsContainer = new JPanel();

        // File Chooser
        fileChooser = new JFileChooser();

        buttonContainer.setLayout(new FlowLayout());
        controlsContainer.setLayout(new BorderLayout());

        this.loadButton = new JButton("Load");
        this.runButton = new JButton("Run");
        this.stepButton = new JButton("Step");
        this.resetButton = new JButton("Reset");
        this.quitButton = new JButton("Quit");

        buttonContainer.add(loadButton);
        buttonContainer.add(runButton);
        buttonContainer.add(stepButton);
        buttonContainer.add(resetButton);
        buttonContainer.add(quitButton);

        controlsContainer.add(buttonContainer, BorderLayout.PAGE_START);
        controlsContainer.add(statusPane, BorderLayout.PAGE_END);

        console.setBorder(BorderFactory.createBevelBorder(1));
        console.setBackground(Color.BLACK);
        console.setForeground(Color.WHITE);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 20));

        getContentPane().add(console, BorderLayout.CENTER);
        getContentPane().add(controlsContainer, BorderLayout.PAGE_END);

        quitButton.addActionListener(this);
        runButton.addActionListener(this);
        stepButton.addActionListener(this);
        resetButton.addActionListener(this);
        loadButton.addActionListener(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        console.requestFocus();

        // Finally create the UI Updater and hand a reference to the Simulator.
        this.uiUpdater = new UiUpdater(this);

        // The simulator must always have a reference to the same UI updater.
        this.simulator.setUiUpdater(uiUpdater);
    }


    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == loadButton) {
            handleProgramLoad();
        } else if (actionEvent.getSource() == resetButton) {
            handleReset();
        } else if (actionEvent.getSource() == stepButton) {
            handleStep();
        } else if (actionEvent.getSource() == quitButton) {
            handleQuit();
        } else if (actionEvent.getSource() == runButton) {
            if (simulator.isRunning()) {
                stop();
            } else {
                start();
            }
        }
    }

    private void handleProgramLoad() {
        try {
            int retVal = fileChooser.showOpenDialog(MainWindow.this);
            if (retVal == JFileChooser.APPROVE_OPTION) {
                File f = fileChooser.getSelectedFile();
                if (f.canRead()) {
                    long fileSize = f.length();

                    if (fileSize > simulator.memorySize()) {
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

                        // Now load the program at the starting address.
                        simulator.loadProgram(program, PROGRAM_START_ADDRESS);
                        // Reset (but don't clear memory, naturally)
                        simulator.reset();
                        // Update status and clear the screen
                        console.reset();
                        uiUpdater.updateUi();
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
        if (simulator.isRunning()) {
            stop();
        }

        try {
            logger.log(Level.INFO, "Reset requested. Resetting CPU and clearing memory.");
            // Reset and clear memory
            simulator.reset();
            simulator.clearMemory();
            // Clear the console.
            console.reset();
            // Update status.
            uiUpdater.updateUi();
        } catch (MemoryAccessException ex) {
            logger.log(Level.SEVERE, "Exception during simulator reset: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void handleStep() {
        try {
            simulator.step();
            // The simulator is lazy about updating the UI for
            // performance reasons, so always request an immediate update after stepping manually.
            uiUpdater.updateUi();
        } catch (SymonException ex) {
            logger.log(Level.SEVERE, "Exception during simulator step: " + ex.getMessage());
            ex.printStackTrace();
        }

    }

    private void handleQuit() {
        // TODO: Clean up and exit properly
        System.exit(0);
    }

    private void stop() {
        // Allow step while the simulator is stopped
        simulator.requestStop();
        if (simulatorThread != null) {
            simulatorThread.interrupt();
            simulatorThread = null;
        }
    }

    private void start() {
        simulatorThread = new Thread(simulator);
        simulatorThread.start();
    }

    public Console getConsole() {
        return this.console;
    }

    public StatusPane getStatusPane() {
        return statusPane;
    }

    public JButton getRunButton() {
        return runButton;
    }

    public JButton getLoadButton() {
        return loadButton;
    }

    public JButton getStepButton() {
        return stepButton;
    }

    public static void main(String args[]) {
        try {
            // Create the simulated system
            Simulator simulator = new Simulator();

            // Create the main UI window
            MainWindow app = new MainWindow(simulator);

            // Pack and display main window
            app.pack();
            app.setVisible(true);

            // Reset the simulator.
            simulator.reset();
        } catch (MemoryAccessException e) {
            e.printStackTrace();
        } catch (MemoryRangeException e) {
            e.printStackTrace();
        }
    }
}