package com.loomcom.symon.ui;

import com.loomcom.symon.MainWindow;

/**
 * Update the console and status display. All direct manipulation of the
 * main UI should go through this class. When not called from the Swing
 * event dispatch thread, these methods should be called using
 * SwingUtilities.invokeLater.
 */
public class UiUpdater {
    private final Console    console;
    private final MainWindow mainWindow;
    private final StatusPane statusPane;

    private final StringBuffer data;

    public UiUpdater(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        this.console = mainWindow.getConsole();
        this.statusPane = mainWindow.getStatusPane();
        this.data = new StringBuffer();
    }

    public void consoleWrite(int i) {
        data.append((char) i);
    }

    /**
     * Callback called by the simulator before exiting its run method.
     */
    public void simulatorDidStop() {
        mainWindow.getStepButton().setEnabled(true);
        mainWindow.getLoadButton().setEnabled(true);
        mainWindow.getRunButton().setText("Run");
        updateUi();
    }

    /**
     * Callback called by the simulator when entering its run method.
     */
    public void simulatorDidStart() {
        // Don't allow step while the simulator is running
        mainWindow.getStepButton().setEnabled(false);
        mainWindow.getLoadButton().setEnabled(false);
        // Toggle the state of the run button
        mainWindow.getRunButton().setText("Stop");
        updateUi();
    }

    public void updateUi() {
        // Update the console with any text
        if (data.length() > 0) {
            console.getModel().print(data.toString());
            console.repaint();
            // Clear the buffer
            data.delete(0, data.length());
        }

        // Update the status UI.
        statusPane.updateState();
    }
}
