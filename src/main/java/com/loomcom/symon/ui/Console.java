package com.loomcom.symon.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.grahamedgecombe.jterminal.JTerminal;
import com.grahamedgecombe.jterminal.vt100.Vt100TerminalModel;
import com.loomcom.symon.exceptions.FifoUnderrunException;
import com.loomcom.symon.util.FifoRingBuffer;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

/**
 * The Console is a simulated 80 column x 24 row VT-100 terminal attached to
 * the ACIA of the system. It provides basic keyboard I/O to Symon.
 */

public class Console extends JTerminal implements KeyListener, MouseListener {

    private static final int     DEFAULT_COLUMNS      = 80;
    private static final int     DEFAULT_ROWS         = 24;
    private static final int     DEFAULT_BORDER_WIDTH = 10;
    // If true, swap CR and LF characters.
    private static final boolean SWAP_CR_AND_LF       = true;
    // If true, send CRLF (0x0d 0x0a) whenever CR is typed
    private static final boolean SEND_CR_LF_FOR_CR    = false;

    // If true, the console is actively listening for key input.
    private boolean isListening;

    private FifoRingBuffer<Character> typeAheadBuffer;

    public Console() {
        this(DEFAULT_COLUMNS, DEFAULT_ROWS);
    }

    public Console(int columns, int rows) {
        super(new Vt100TerminalModel(columns, rows));
        // A small type-ahead buffer, as might be found in any real
        // VT100-style serial terminal.
        this.typeAheadBuffer = new FifoRingBuffer<Character>(128);
        this.isListening = false;
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
     */
    public void reset() {
        typeAheadBuffer.reset();
        getModel().clear();
        getModel().setCursorColumn(0);
        getModel().setCursorRow(0);
        repaint();
    }

    public void startListening() {
        this.isListening = true;
    }

    public void stopListening() {
        this.isListening = false;
    }

    /**
     * Returns true if a key has been pressed since the last time input was read.
     *
     * @return
     */
    public boolean hasInput() {
        return !typeAheadBuffer.isEmpty();
    }

    /**
     * Handle a Key Typed event.
     *
     * @param keyEvent The key event.
     */
    public void keyTyped(KeyEvent keyEvent) {
        if (isListening) {
            char keyTyped = keyEvent.getKeyChar();

            if (SWAP_CR_AND_LF) {
                if (keyTyped == 0x0a) {
                    keyTyped = 0x0d;
                } else if (keyTyped == 0x0d) {
                    keyTyped = 0x0a;
                }
            }

            if (SEND_CR_LF_FOR_CR && keyTyped == 0x0d) {
                typeAheadBuffer.push((char) 0x0d);
                typeAheadBuffer.push((char) 0x0a);
            } else {
                typeAheadBuffer.push(keyTyped);
            }
        }

        keyEvent.consume();
    }

    /**
     * Handle a Key Press event.
     *
     * @param keyEvent The key event.
     */
    public void keyPressed(KeyEvent keyEvent) {
        keyEvent.consume();
    }

    /**
     * Read the most recently typed key from the single-char input buffer.
     *
     * @return The character typed.
     */
    public char readInputChar() throws FifoUnderrunException {
        return typeAheadBuffer.pop();
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
