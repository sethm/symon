package com.loomcom.symon.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import com.grahamedgecombe.jterminal.JTerminal;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

/**
 * The Console is a simulated 80 column x 24 row VT-100 terminal attached to
 * the ACIA of the system. It provides basic keyboard I/O to Symon.
 */

public class Console extends JTerminal implements KeyListener, MouseListener {

    private boolean hasInput = false;
    private char keyBuffer;

	public Console() {
        super();
        addKeyListener(this);
        addMouseListener(this);
        Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 0);
        Border bevelBorder = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
        Border compoundBorder = BorderFactory.createCompoundBorder(emptyBorder, bevelBorder);
        this.setBorder(compoundBorder);
	}

    public void reset() {
        getModel().clear();
        getModel().setCursorColumn(0);
        getModel().setCursorRow(0);
        repaint();
        this.hasInput = false;
    }

    public void keyTyped(KeyEvent keyEvent) {
        keyEvent.consume();
    }

    public void keyPressed(KeyEvent keyEvent) {
        keyBuffer = keyEvent.getKeyChar();
        hasInput = true;

        System.out.println("Key Pressed (0x" + Integer.toString((int)keyBuffer, 16) + ") : " + keyBuffer);

        keyEvent.consume();
    }

    public boolean hasInput() {
        return hasInput;
    }

    public char readInputChar() {
        hasInput = false;
        return this.keyBuffer;
    }

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
