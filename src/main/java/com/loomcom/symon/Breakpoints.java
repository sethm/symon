package com.loomcom.symon;

import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.util.Utils;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.TreeSet;

public class Breakpoints extends AbstractTableModel {

    private TreeSet<Integer> breakpoints;
    private Simulator simulator;

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

    public void removeBreakpoint(int address) {
        this.breakpoints.remove(address);
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
