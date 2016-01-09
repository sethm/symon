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

import com.loomcom.symon.Breakpoints;
import com.loomcom.symon.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Simple window to enter breakpoints.
 */
public class BreakpointsWindow extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(BreakpointsWindow.class);

    private static final Dimension FRAME_SIZE = new Dimension(240, 280);
    private static final String EMPTY_STRING = "";

    private JFrame mainWindow;
    private Breakpoints breakpoints;

    public BreakpointsWindow(Breakpoints breakpoints,
                             JFrame mainWindow) {
        this.breakpoints = breakpoints;
        this.mainWindow = mainWindow;
        createUi();
    }

    private void createUi() {
        setTitle("Breakpoints");

        JPanel breakpointsPanel = new JPanel();
        JPanel controlPanel = new JPanel();

        breakpointsPanel.setLayout(new BorderLayout());
        breakpointsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        final JButton addButton = new JButton("Add");
        final JButton removeButton = new JButton("Del");
        removeButton.setEnabled(false);

        final JTextField addTextField = new JTextField(4);

        final JTable breakpointsTable = new JTable(breakpoints);
        breakpointsTable.setShowGrid(true);
        breakpointsTable.setGridColor(Color.LIGHT_GRAY);
        breakpointsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        breakpointsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getFirstIndex() > -1) {
                    removeButton.setEnabled(true);
                } else {
                    removeButton.setEnabled(false);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(breakpointsTable);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        breakpointsPanel.add(scrollPane, BorderLayout.CENTER);

        ActionListener addBreakpointListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int value;

                String newBreakpoint = addTextField.getText();

                if (newBreakpoint == null || newBreakpoint.isEmpty()) {
                    return;
                }

                try {
                    value = (Integer.parseInt(addTextField.getText(), 16) & 0xffff);
                } catch (NumberFormatException ex) {
                    logger.warn("Can't parse page number {}", newBreakpoint);
                    return;
                }

                if (value < 0) {
                    return;
                }

                breakpoints.addBreakpoint(value);

                logger.debug("Added breakpoint ${}", Utils.wordToHex(value));

                addTextField.setText(EMPTY_STRING);
            }
        };

        addButton.addActionListener(addBreakpointListener);
        addTextField.addActionListener(addBreakpointListener);

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                breakpoints.removeBreakpointAtIndex(breakpointsTable.getSelectedRow());
            }
        });

        controlPanel.add(addTextField);
        controlPanel.add(addButton);
        controlPanel.add(removeButton);

        setLayout(new BorderLayout());
        getContentPane().add(breakpointsPanel, BorderLayout.CENTER);
        getContentPane().add(controlPanel, BorderLayout.SOUTH);

        setMinimumSize(FRAME_SIZE);
        setMaximumSize(FRAME_SIZE);
        setPreferredSize(FRAME_SIZE);

        setLocationRelativeTo(mainWindow);
        setResizable(false);

        pack();
    }
}
