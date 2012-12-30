/*
 * Copyright (c) 2008-2012 Seth J. Morabito <sethm@loomcom.com>
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

import com.loomcom.symon.Bus;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.util.HexUtil;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Display a page of memory.
 */
public class MemoryWindow extends JFrame implements ActionListener {

    private MemoryTableModel memoryTableModel;
    private JTable memoryTable;
    private JScrollPane scrollPane;
    private JTextField pageNumberTextField;
    private JLabel pageNumberLabel;

    private static final Dimension MINIMUM_SIZE = new Dimension(400, 450);

    public MemoryWindow(Bus bus) {
        this.memoryTableModel = new MemoryTableModel(bus);
        this.createUi();
    }

    /**
     * Set the current memory page to be inspected by the table.
     *
     * @param pageNumber The page number, from 0 to 255 (00 to FF hex)
     */
    public void setPageNumber(int pageNumber) {
        memoryTableModel.setPageNumber(pageNumber);
    }

    public int getPageNumber() {
        return memoryTableModel.getPageNumber();
    }

    private void updatePageNumberTextField() {
        pageNumberTextField.setText(HexUtil.byteToHex(getPageNumber()));
    }

    private void createUi() {
        setTitle("Memory Contents");
        this.memoryTable = new JTable(memoryTableModel);

        memoryTable.setDragEnabled(false);
        memoryTable.setCellSelectionEnabled(false);
        memoryTable.setShowGrid(false);

        pageNumberLabel = new JLabel("Page Number");
        pageNumberTextField = new JTextField(8);
        pageNumberTextField.addActionListener(this);

        updatePageNumberTextField();

        JPanel controlPanel = new JPanel();
        JPanel memoryPanel = new JPanel();
        memoryPanel.setLayout(new BorderLayout());

        controlPanel.add(pageNumberLabel);
        controlPanel.add(pageNumberTextField);

        this.scrollPane = new JScrollPane(memoryTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        memoryPanel.add(scrollPane, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        getContentPane().add(controlPanel, BorderLayout.NORTH);
        getContentPane().add(memoryPanel, BorderLayout.CENTER);

        setMinimumSize(MINIMUM_SIZE);
        setPreferredSize(MINIMUM_SIZE);

        pack();
    }

    /**
     * Handle page numbers entered into the UI.
     *
     * @param e The action event
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == pageNumberTextField) {

            String pageNumberInput = pageNumberTextField.getText();
            try {
                // Try to parse a hex value out of the pageNumber.
                int newPageNumber = Integer.parseInt(pageNumberInput, 16);
                setPageNumber(newPageNumber & 0xff);
                memoryTable.updateUI();
            } catch (NumberFormatException ex) {
                // An invalid number was entered. Log the error, but otherwise
                // take no action.
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Can't parse page number " +
                        pageNumberInput);
            }
            updatePageNumberTextField();
        }
    }

    /**
     * The model that backs the Memory Table.
     */
    private class MemoryTableModel extends AbstractTableModel {

        private Bus bus;
        private int pageNumber;

        private static final int COLUMN_COUNT = 9;
        private static final int ROW_COUNT = 32;

        public MemoryTableModel(Bus bus) {
            this.bus = bus;
        }

        /**
         * Set the current memory page to be inspected by the table.
         *
         * @param pageNumber The page number, from 0 to 255 (00 to FF hex)
         */
        public void setPageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
        }

        /**
         * Returns the current page number being inspected by the table.
         *
         * @return The page number being inspected, from 0 to 255 (00 to FF hex)
         */
        public int getPageNumber() {
            return this.pageNumber;
        }

        public int getRowCount() {
            return ROW_COUNT;
        }

        public int getColumnCount() {
            return COLUMN_COUNT;
        }

        @Override
        public String getColumnName(int i) {
            return null;
        }

        @Override
        public Class<?> getColumnClass(int i) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column > 0;
        }

        public Object getValueAt(int row, int column) {
            try {
                if (column == 0) {
                    return HexUtil.wordToHex(fullAddress(row, 1));
                } else {
                    return HexUtil.byteToHex(bus.read(fullAddress(row, column)));
                }
            } catch (MemoryAccessException ex) {
                return "??";
            }
        }

        @Override
        public void setValueAt(Object o, int row, int column) {
            if (column > 0) {
                try {
                    String hexValue = (String)o;
                    int fullAddress = fullAddress(row, column);
                    int newValue = Integer.parseInt(hexValue, 16);
                    bus.write(fullAddress, newValue);
                } catch (MemoryAccessException ex) {
                    ;
                } catch (NumberFormatException ex) {
                    ;
                } catch (ClassCastException ex) {
                    ;
                }
                fireTableCellUpdated(row, column);
            }
        }

        private int fullAddress(int row, int column) {
            int pageAddress = ((row * 8) + (column - 1)) & 0xff;
            return (pageNumber << 8) | pageAddress;
        }

    }

}

