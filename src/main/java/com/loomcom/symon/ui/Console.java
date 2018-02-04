/*
 * Copyright (c) 2016 Seth J. Morabito <web@loomcom.com>
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

package com.loomcom.symon.ui;

import com.loomcom.symon.jterminal.JTerminal;
import com.loomcom.symon.jterminal.vt100.Vt100TerminalModel;
import com.loomcom.symon.exceptions.FifoUnderrunException;
import com.loomcom.symon.util.FifoRingBuffer;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * The Console is a simulated 80 column x 24 row VT-100 terminal attached to
 * the ACIA of the system. It provides basic keyboard I/O to Symon.
 */

public class Console extends JTerminal implements KeyListener, MouseListener {

	private static final long serialVersionUID = 6633818486963338126L;
	
	private static final int     DEFAULT_BORDER_WIDTH = 10;
    // If true, swap CR and LF characters.
    private static final boolean SWAP_CR_AND_LF       = true;

    // If true, send CRLF (0x0d 0x0a) whenever CR is typed
    private boolean sendCrForLf;
    private FifoRingBuffer<Character> typeAheadBuffer;

    public Console(int columns, int rows, Font font, boolean sendCrForLf) {
        super(new Vt100TerminalModel(columns, rows), font);
    		//super(new Vt100TerminalModel(columns, rows));
        // A small type-ahead buffer, as might be found in any real
        // VT100-style serial terminal.
        this.typeAheadBuffer = new FifoRingBuffer<>(128);
        this.sendCrForLf = sendCrForLf;
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

    /**
     * Returns true if a key has been pressed since the last time input was read.
     *
     * @return true if the type-ahead buffer has data
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
        char keyTyped = keyEvent.getKeyChar();

        if (SWAP_CR_AND_LF) {
            if (keyTyped == 0x0a) {
                keyTyped = 0x0d;
            } else if (keyTyped == 0x0d) {
                keyTyped = 0x0a;
            }
        }

        if (sendCrForLf && (keyTyped == 0x0d)) {
            typeAheadBuffer.push((char) 0x0d);
            typeAheadBuffer.push((char) 0x0a);
        } else {
            typeAheadBuffer.push(keyTyped);
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
