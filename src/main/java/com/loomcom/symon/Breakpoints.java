/*
 * Copyright (c) 2008-2025 Seth J. Morabito <web@loomcom.com>
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

package com.loomcom.symon;

import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.util.Utils;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.TreeSet;

public class Breakpoints extends AbstractTableModel {

    private final TreeSet<Integer> breakpoints;
    private final Simulator simulator;

    public Breakpoints(Simulator simulator) {
        this.breakpoints = new TreeSet<>();
        this.simulator = simulator;
    }

    public boolean contains(int address) {
        return this.breakpoints.contains(address);
    }

    public void addBreakpoint(int address) {
        this.breakpoints .add(address);
        fireTableDataChanged();
    }

    public void removeBreakpointAtIndex(int index) {
        if (index < 0) {
            return;
        }

        ArrayList<Integer> values = new ArrayList<>(breakpoints);
        int value = values.get(index);
        this.breakpoints.remove(value);
        fireTableDataChanged();
    }

    public void refresh() {
        fireTableDataChanged();
    }

    @Override
    public String getColumnName(int index) {
        if (index == 0) {
            return "Address";
        } else {
            return "Inst";
        }
    }

    @Override
    public int getRowCount() {
        return breakpoints.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ArrayList<Integer> values = new ArrayList<>(breakpoints);

        if (columnIndex == 0) {
            return "$" + Utils.wordToHex(values.get(rowIndex));
        } else if (columnIndex == 1) {
            int address = values.get(rowIndex);
            try {
                return simulator.disassembleOpAtAddress(address);
            } catch (MemoryAccessException ex) {
                return "???";
            }
        } else {
            return null;
        }
    }
}
