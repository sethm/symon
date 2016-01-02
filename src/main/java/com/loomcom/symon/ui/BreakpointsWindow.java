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

import com.loomcom.symon.util.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.SortedSet;

/**
 * Simple window to enter breakpoints.
 */
public class BreakpointsWindow extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(BreakpointsWindow.class);

    private static final Dimension FRAME_SIZE = new Dimension(240, 280);
    private static final String EMPTY_STRING = "";

    private JFrame mainWindow;
    private SortedSet<Integer> breakpoints;

    /**
     * Simple ListModel to back the list of breakpoints.
     */
    private class BreakpointsListModel extends AbstractListModel<String> {
        private SortedSet<Integer> breakpoints;

        public BreakpointsListModel(SortedSet<Integer> breakpoints) {
            this.breakpoints = breakpoints;
        }

        @Override
        public int getSize() {
            return breakpoints.size();
        }

        @Override
        public String getElementAt(int index) {
            ArrayList<Integer> values = new ArrayList<>(breakpoints);
            return "$" + HexUtil.wordToHex(values.get(index));
        }

        public void addElement(Integer breakpoint) {
            breakpoints.add(breakpoint);
            ArrayList<Integer> values = new ArrayList<>(breakpoints);
            int index = values.indexOf(breakpoint);
            fireIntervalAdded(this, index, index);
        }

        public void removeElement(int index) {
            ArrayList<Integer> values = new ArrayList<>(breakpoints);
            Integer breakpoint = values.get(index);
            breakpoints.remove(breakpoint);
            fireIntervalRemoved(this, index, index);
        }
    }

    public BreakpointsWindow(SortedSet<Integer> breakpoints,
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

        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Del");
        removeButton.setEnabled(false);

        JTextField addTextField = new JTextField(5);

        BreakpointsListModel listModel = new BreakpointsListModel(breakpoints);
        JList<String> breakpointsList = new JList<>(listModel);
        breakpointsList.setFont(new Font("Monospace", Font.PLAIN, 14));
        breakpointsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(breakpointsList);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        breakpointsPanel.add(scrollPane, BorderLayout.CENTER);


        breakpointsList.addListSelectionListener(le -> {
            int idx = breakpointsList.getSelectedIndex();

            if (idx == -1) {
                removeButton.setEnabled(false);
            } else {
                removeButton.setEnabled(true);
            }
        });

        ActionListener addBreakpointListener = e -> {
            int value = -1;

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

            listModel.addElement(value);

            logger.debug("Added breakpoint ${}", HexUtil.wordToHex(value));

            addTextField.setText(EMPTY_STRING);
        };

        addButton.addActionListener(addBreakpointListener);
        addTextField.addActionListener(addBreakpointListener);

        removeButton.addActionListener(e -> listModel.removeElement(breakpointsList.getSelectedIndex()));

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
