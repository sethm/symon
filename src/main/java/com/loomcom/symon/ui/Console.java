package com.loomcom.symon.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import com.grahamedgecombe.jterminal.JTerminal;
import com.grahamedgecombe.jterminal.vt100.Vt100TerminalModel;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

/**
 * The Console is a simulated 80 column x 24 row VT-100 terminal attached to
 * the ACIA of the system. It provides basic keyboard I/O to Symon.
 */

public class Console extends JTerminal implements KeyListener, MouseListener {

    private static final int DEFAULT_COLUMNS = 80;
    private static final int DEFAULT_ROWS = 24;
    private static final int DEFAULT_BORDER_WIDTH = 10;

    private boolean hasInput = false;
    private char keyBuffer;

    public Console() {
        this(DEFAULT_COLUMNS, DEFAULT_ROWS);
    }

	public Console(int columns, int rows) {
        super(new Vt100TerminalModel(columns, rows));
        setBorderWidth(DEFAULT_BORDER_WIDTH);
        addKeyListener(this);
        addMouseListener(this);
        Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 0);
        Border bevelBorder = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
        Border compoundBorder = BorderFactory.createCompoundBorder(emptyBorder, bevelBorder);
        this.setBorder(compoundBorder);
	}

    /**
     * Reset the console. This will cause the console to be cleared and the cursor returned to the
     * home position.
     *
     */
    public void reset() {
        getModel().clear();
        getModel().setCursorColumn(0);
        getModel().setCursorRow(0);
        repaint();
        this.hasInput = false;
    }

    /**
     * Returns true if a key has been pressed since the last time input was read.
     *
     * @return
     */
    public boolean hasInput() {
        return hasInput;
    }

    /**
     * Handle a Key Typed event.
     *
     * @param keyEvent The key event.
     */
    public void keyTyped(KeyEvent keyEvent) {
        keyEvent.consume();
    }

    /**
     * Handle a Key Press event.
     *
     * @param keyEvent The key event.
     */
    public void keyPressed(KeyEvent keyEvent) {
        keyBuffer = keyEvent.getKeyChar();
        hasInput = true;
        keyEvent.consume();
    }

    /**
     * Read the most recently typed key from the single-char input buffer.
     *
     * @return The character typed.
     */
    public char readInputChar() {
        hasInput = false;
        return this.keyBuffer;
    }

    /**
     * Handle a key release event.
     *
     * @param keyEvent The key event.
     */
    public void keyReleased(KeyEvent keyEvent) {
        keyEvent.consume();
    }

    public void mouseClicked(MouseEvent mouseEvent) {
    }

    public void mousePressed(MouseEvent mouseEvent) {
        requestFocus();
        mouseEvent.consume();
    }

    public void mouseReleased(MouseEvent mouseEvent) {
    }

    public void mouseEntered(MouseEvent mouseEvent) {
    }

    public void mouseExited(MouseEvent mouseEvent) {
    }
}
