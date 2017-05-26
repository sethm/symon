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

import com.loomcom.symon.Bus;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;


/**
 * This Frame displays the contents of a page of memory. The page number to be displayed
 * is selectable by the user.
 */
public class MemoryWindow extends JFrame implements ActionListener {

    private static final Logger logger = LoggerFactory.getLogger(MemoryWindow.class);

    private MemoryTableModel memoryTableModel;
    private JTable memoryTable;
    private JTextField pageNumberTextField;
    private JButton previousPageButton;
    private JButton nextPageButton;

    private static final Dimension MINIMUM_SIZE = new Dimension(320, 600);

    // The width of column 0 (address), in pixels
    private static final int ADDR_COL_WIDTH = 48;

    private static final int HEX_COL_WIDTH = 32;

    // The width of the ASCII cells, in pixels
    private static final int ASCII_COL_WIDTH = 8;

    // The start/end columns of the ASCII view
    private static final int ASCII_COL_START = 9;
    private static final int ASCII_COL_END = 16;

    /**
     * Initialize a new MemoryWindow frame with the specified Bus.
     * The MemoryWindow frame will not be visible.
     *
     * @param bus The Bus the memory window will query for data.
     */
    public MemoryWindow(Bus bus) {
        this.memoryTableModel = new MemoryTableModel(bus);
        createUi();
    }

    /**
     * Set the current memory page to be inspected by the table.
     *
     * @param pageNumber The page number, from 0 to 255 (00 to FF hex)
     */
    public void setPageNumber(int pageNumber) {
        memoryTableModel.setPageNumber(pageNumber);
    }

    /**
     * Returns the current page number being inspected by the table.
     *
     * @return The page number being inspected, from 0 to 255 (00 to FF hex)
     */
    public int getPageNumber() {
        return memoryTableModel.getPageNumber();
    }

    /**
     * Set the contents of the page number text field with the current
     * page number, in hex.
     */
    private void updateControls() {
        int pageNumber = getPageNumber();

        previousPageButton.setEnabled(pageNumber > 0x00);
        nextPageButton.setEnabled(pageNumber < 0xff);
        pageNumberTextField.setText(Utils.byteToHex(pageNumber));
    }

    /**
     * Set-up the UI.
     */
    private void createUi() {
        setTitle("Memory Contents");
        this.memoryTable = new MemoryTable(memoryTableModel);

        memoryTable.setDragEnabled(false);
        memoryTable.setCellSelectionEnabled(false);
        memoryTable.setIntercellSpacing(new Dimension(0, 0));
        memoryTable.getTableHeader().setReorderingAllowed(false);
        memoryTable.getTableHeader().setResizingAllowed(false);
        memoryTable.getTableHeader().setVisible(false);
        memoryTable.setShowGrid(false);

        memoryTable.getColumnModel().getColumn(0).setMaxWidth(ADDR_COL_WIDTH);

        for (int i = 1; i < ASCII_COL_START; i++) {
            memoryTable.getColumnModel().getColumn(i).setMaxWidth(HEX_COL_WIDTH);
        }

        for (int i = ASCII_COL_START; i <= ASCII_COL_END; i++) {
            memoryTable.getColumnModel().getColumn(i).setMaxWidth(ASCII_COL_WIDTH);
        }

        MemoryTableCellRenderer memoryTableCellRenderer = new MemoryTableCellRenderer();

        memoryTableCellRenderer.setHorizontalAlignment(JLabel.CENTER);
        memoryTable.setDefaultRenderer(String.class, memoryTableCellRenderer);

        // Turn off tool-tips for the table.
        ToolTipManager.sharedInstance().unregisterComponent(memoryTable);
        ToolTipManager.sharedInstance().unregisterComponent(memoryTable.getTableHeader());

        JLabel pageNumberLabel = new JLabel("Page");
        pageNumberTextField = new JTextField(8);
        pageNumberTextField.addActionListener(this);

        nextPageButton = new JButton("Next >>");
        previousPageButton = new JButton("<< Prev");
        nextPageButton.addActionListener(this);
        previousPageButton.addActionListener(this);

        updateControls();

        JPanel controlPanel = new JPanel();
        JPanel memoryPanel = new JPanel();
        memoryPanel.setLayout(new BorderLayout());
        memoryPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        controlPanel.add(previousPageButton);
        controlPanel.add(pageNumberLabel);
        controlPanel.add(pageNumberTextField);
        controlPanel.add(nextPageButton);

        JScrollPane scrollPane = new JScrollPane(memoryTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        memoryPanel.add(scrollPane, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        getContentPane().add(controlPanel, BorderLayout.NORTH);
        getContentPane().add(memoryPanel, BorderLayout.CENTER);

        setMinimumSize(MINIMUM_SIZE);
        memoryPanel.setPreferredSize(memoryTable.getPreferredSize());
        setPreferredSize(memoryPanel.getPreferredSize());

        pack();
    }

    /**
     * Handle page numbers entered into the UI.
     *
     * @param e The action event
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == previousPageButton) {
            int currentPage = getPageNumber();
            if (currentPage > 0x00) {
                setPageNumber(currentPage - 1);
                updateControls();
                updateState();
            }
        } else if (e.getSource() == nextPageButton) {
            int currentPage = getPageNumber();
            if (currentPage < 0xff) {
                setPageNumber(currentPage + 1);
                updateControls();
                updateState();
            }
        } else if (e.getSource() == pageNumberTextField) {
            String pageNumberInput = pageNumberTextField.getText();
            try {
                // Try to parse a hex value out of the pageNumber.
                int newPageNumber = Integer.parseInt(pageNumberInput, 16);
                setPageNumber(newPageNumber & 0xff);
                updateState();
            } catch (NumberFormatException ex) {
                // An invalid number was entered. Log the error, but otherwise
                // take no action.
                logger.warn("Can't parse page number {}", pageNumberInput);
            }

            updateControls();
        }
    }

    /**
     * Refresh the view of memory
     */
    public void updateState() {
        memoryTable.updateUI();
    }

    /**
     * A JTable that will automatically select all text in a cell
     * being edited.
     */
    private class MemoryTable extends JTable {

        public MemoryTable(TableModel tableModel) {
            super(tableModel);
        }

        @Override
        public boolean editCellAt(int row, int col, EventObject e) {
            boolean result = super.editCellAt(row, col, e);

            final Component editor = getEditorComponent();

            if (editor != null && editor instanceof JTextComponent) {
                ((JTextComponent) editor).selectAll();
            }

            return result;
        }
    }

    private class MemoryTableCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int col) {
            final Component cell = super.getTableCellRendererComponent(table, value,
                                                                       isSelected, hasFocus,
                                                                       row, col);

            if (isSelected) {
                cell.setBackground(Color.LIGHT_GRAY);
                cell.setForeground(Color.BLACK);
            }

            return cell;
        }
    }

    /**
     * The model that backs the Memory Table.
     */
    private class MemoryTableModel extends AbstractTableModel {

        private Bus bus;
        private int pageNumber;

        private static final int COLUMN_COUNT = 17;
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
            return (column > 0 && column < ASCII_COL_START);
        }

        public Object getValueAt(int row, int column) {
            try {
                if (column == 0) {
                    return Utils.wordToHex(fullAddress(row, 1));
                } else if (column < 9) {
                    // Display hex value of the data
                    return Utils.byteToHex(bus.read(fullAddress(row, column), false));
                } else {
                    // Display the ASCII equivalent (if printable)
                    return Utils.byteToAscii(bus.read(fullAddress(row, column - 8), false));
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
                    int newValue = Integer.parseInt(hexValue, 16) & 0xff;
                    bus.write(fullAddress, newValue);
                } catch (MemoryAccessException | NumberFormatException | ClassCastException ex) {
                    // Intentionally swallow exception
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

