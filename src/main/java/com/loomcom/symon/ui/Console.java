package com.loomcom.symon.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import com.grahamedgecombe.jterminal.JTerminal;

/**
 * The Console is a simulated 80 column x 24 row VT-100 terminal attached to
 * the ACIA of the system. It provides basic keyboard I/O to Symon.
 */

public class Console extends JTerminal implements KeyListener, MouseListener {

    // TODO: Pass in io threads, read and write to ACIA.
	public Console() {
        super();
        addKeyListener(this);
        addMouseListener(this);
	}

    public void reset() {
        getModel().clear();
        getModel().setCursorColumn(0);
        getModel().setCursorRow(0);
        repaint();
    }

    public void keyTyped(KeyEvent keyEvent) {
        keyEvent.consume();
    }

    public void keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        int modifiersMask = keyEvent.getModifiers();
        int modifiersExMask = keyEvent.getModifiersEx();

        System.out.println("Key Pressed #" + keyEvent.getKeyCode() + " : " +
                KeyEvent.getKeyText(keyCode) + "   MASK : " +
                modifiersMask + "    EXT MASK : " +
                modifiersExMask
        );

        keyEvent.consume();
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
