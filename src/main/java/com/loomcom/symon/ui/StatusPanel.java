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

import com.loomcom.symon.Cpu;
import com.loomcom.symon.CpuState;
import com.loomcom.symon.machines.Machine;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * UI component that displays the current state of the simulated CPU.
 */
public class StatusPanel extends JPanel {

    private ImageIcon carryOn;
    private ImageIcon carryOff;
    private ImageIcon zeroOn;
    private ImageIcon zeroOff;
    private ImageIcon irqOn;
    private ImageIcon irqOff;
    private ImageIcon decimalOn;
    private ImageIcon decimalOff;
    private ImageIcon breakOn;
    private ImageIcon breakOff;
    private ImageIcon overflowOn;
    private ImageIcon overflowOff;
    private ImageIcon negativeOn;
    private ImageIcon negativeOff;

    private JLabel carryFlagLabel;
    private JLabel zeroFlagLabel;
    private JLabel irqDisableFlagLabel;
    private JLabel decimalModeFlagLabel;
    private JLabel breakFlagLabel;
    private JLabel overflowFlagLabel;
    private JLabel negativeFlagLabel;

    private JTextField opcodeField;
    private JTextField pcField;
    private JTextField spField;
    private JTextField aField;
    private JTextField xField;
    private JTextField yField;

    private Machine machine;

    private static final int EMPTY_BORDER = 10;
    private static final Border LABEL_BORDER = BorderFactory.createEmptyBorder(0, 5, 0, 0);
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final Dimension LARGE_TEXT_FIELD_SIZE = new Dimension(134, 22);
    private static final Dimension SMALL_TEXT_FIELD_SIZE = new Dimension(65, 22);

    public StatusPanel(Machine machine) {
        super();
        this.machine = machine;
        createUi();
    }

    private void createUi() {
        Border emptyBorder = BorderFactory.createEmptyBorder(EMPTY_BORDER, EMPTY_BORDER,
                                                             EMPTY_BORDER, EMPTY_BORDER);
        Border etchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);

        setBorder(BorderFactory.createCompoundBorder(emptyBorder, etchedBorder));

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();

        setLayout(layout);

        JPanel statusFlagsPanel = new JPanel();
        statusFlagsPanel.setAlignmentX(LEFT_ALIGNMENT);

        carryOn = new ImageIcon(this.getClass().getResource("/C_on.png"));
        carryOff = new ImageIcon(this.getClass().getResource("/C_off.png"));
        zeroOn = new ImageIcon(this.getClass().getResource("/Z_on.png"));
        zeroOff = new ImageIcon(this.getClass().getResource("/Z_off.png"));
        irqOn = new ImageIcon(this.getClass().getResource("/I_on.png"));
        irqOff = new ImageIcon(this.getClass().getResource("/I_off.png"));
        decimalOn = new ImageIcon(this.getClass().getResource("/D_on.png"));
        decimalOff = new ImageIcon(this.getClass().getResource("/D_off.png"));
        breakOn = new ImageIcon(this.getClass().getResource("/B_on.png"));
        breakOff = new ImageIcon(this.getClass().getResource("/B_off.png"));
        overflowOn = new ImageIcon(this.getClass().getResource("/O_on.png"));
        overflowOff = new ImageIcon(this.getClass().getResource("/O_off.png"));
        negativeOn = new ImageIcon(this.getClass().getResource("/N_on.png"));
        negativeOff = new ImageIcon(this.getClass().getResource("/N_off.png"));

        // Initialize all to off
        carryFlagLabel = new JLabel(carryOff, JLabel.CENTER);
        zeroFlagLabel = new JLabel(zeroOff, JLabel.CENTER);
        irqDisableFlagLabel = new JLabel(irqOff, JLabel.CENTER);
        decimalModeFlagLabel = new JLabel(decimalOff, JLabel.CENTER);
        breakFlagLabel = new JLabel(breakOff, JLabel.CENTER);
        overflowFlagLabel = new JLabel(overflowOff, JLabel.CENTER);
        negativeFlagLabel = new JLabel(negativeOff, JLabel.CENTER);

        // Add tool-tip text
        carryFlagLabel.setToolTipText("Carry: The last operation caused an overflow " +
                                      "from bit 7 of the result or an underflow from bit 0");
        zeroFlagLabel.setToolTipText("Zero: The result of the last operation was 0");
        irqDisableFlagLabel.setToolTipText("Interrupt Disable: Processor will not respond to IRQ");
        decimalModeFlagLabel.setToolTipText("Decimal Mode");
        breakFlagLabel.setToolTipText("Break: BRK instruction occurred");
        overflowFlagLabel.setToolTipText("Overflow: The result of the last operation was " +
                                         "an invalid 2's complement result");
        negativeFlagLabel.setToolTipText("Negative: The result of the last operation set bit 7");


        statusFlagsPanel.add(negativeFlagLabel);
        statusFlagsPanel.add(overflowFlagLabel);
        statusFlagsPanel.add(breakFlagLabel);
        statusFlagsPanel.add(decimalModeFlagLabel);
        statusFlagsPanel.add(irqDisableFlagLabel);
        statusFlagsPanel.add(zeroFlagLabel);
        statusFlagsPanel.add(carryFlagLabel);

        // Create and add register and address labels
        JLabel statusFlagsLabel = makeLabel("Flags");
        JLabel opcodeLabel = makeLabel("Next IR");
        JLabel pcLabel = makeLabel("PC");
        JLabel spLabel = makeLabel("SP");
        JLabel aLabel = makeLabel("A");
        JLabel xLabel = makeLabel("X");
        JLabel yLabel = makeLabel("Y");

        statusFlagsLabel.setToolTipText("6502 Processor Status Flags");
        opcodeLabel.setToolTipText("Instruction Register");
        pcLabel.setToolTipText("Program Counter");
        spLabel.setToolTipText("Stack Pointer");

        opcodeField = makeTextField(LARGE_TEXT_FIELD_SIZE, false);
        pcField = makeTextField(LARGE_TEXT_FIELD_SIZE, true);
        spField = makeTextField(SMALL_TEXT_FIELD_SIZE, true);
        aField = makeTextField(SMALL_TEXT_FIELD_SIZE, true);
        xField = makeTextField(SMALL_TEXT_FIELD_SIZE, true);
        yField = makeTextField(SMALL_TEXT_FIELD_SIZE, true);

        // Make fields editable
        pcField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int newVal = StatusPanel.this.getHexVal(pcField) & 0xffff;
                    machine.getCpu().setProgramCounter(newVal);
                } catch (Exception ex) {
                    // Swallow exception
                }

                StatusPanel.this.updateState();
            }
        });

        spField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int newVal = StatusPanel.this.getHexVal(spField) & 0xff;
                    machine.getCpu().setStackPointer(newVal);
                } catch (Exception ex) {
                    // Swallow exception
                }

                StatusPanel.this.updateState();
            }
        });

        aField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int newVal = StatusPanel.this.getHexVal(aField) & 0xff;
                    machine.getCpu().setAccumulator(newVal);
                } catch (Exception ex) {
                    // Swallow exception
                }

                StatusPanel.this.updateState();
            }
        });

        xField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int newVal = StatusPanel.this.getHexVal(xField) & 0xff;
                    machine.getCpu().setXRegister(newVal);
                } catch (Exception ex) {
                    // Swallow exception
                }

                StatusPanel.this.updateState();
            }
        });

        yField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int newVal = StatusPanel.this.getHexVal(yField) & 0xff;
                    machine.getCpu().setYRegister(newVal);
                } catch (Exception ex) {
                    // Swallow exception
                }

                StatusPanel.this.updateState();
            }
        });

        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.gridwidth = 2;
        constraints.gridx = 0;
        constraints.gridy = 0;
        add(statusFlagsLabel, constraints);
        constraints.gridy = 1;
        add(statusFlagsPanel, constraints);


        constraints.insets = new Insets(5, 0, 0, 0);
        constraints.gridy = 2;
        add(opcodeLabel, constraints);

        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.gridy = 3;
        add(opcodeField, constraints);

        constraints.insets = new Insets(5, 0, 0, 0);
        constraints.gridy = 4;
        add(pcLabel, constraints);

        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.gridy = 5;
        add(pcField, constraints);

        constraints.insets = new Insets(5, 0, 0, 0);
        constraints.gridwidth = 1;
        constraints.gridy = 6;
        add(spLabel, constraints);
        constraints.gridx = 1;
        add(aLabel, constraints);

        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.gridx = 0;
        constraints.gridy = 7;
        add(spField, constraints);
        constraints.gridx = 1;
        add(aField, constraints);

        constraints.insets = new Insets(5, 0, 0, 0);
        constraints.gridx = 0;
        constraints.gridy = 8;
        add(xLabel, constraints);
        constraints.gridx = 1;
        add(yLabel, constraints);

        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.gridx = 0;
        constraints.gridy = 9;
        add(xField, constraints);
        constraints.gridx = 1;
        add(yField, constraints);
    }

    /**
     * Update the display based on the current state of the CPU.
     */
    public void updateState() {
        Cpu cpu = machine.getCpu();
        CpuState cpuState = cpu.getCpuState();

        // Update the Processor Status Flag display
        int status = cpuState.getStatusFlag();

        carryFlagLabel.setIcon(iconForFlag(status, 0));
        zeroFlagLabel.setIcon(iconForFlag(status, 1));
        irqDisableFlagLabel.setIcon(iconForFlag(status, 2));
        decimalModeFlagLabel.setIcon(iconForFlag(status, 3));
        breakFlagLabel.setIcon(iconForFlag(status, 4));
        overflowFlagLabel.setIcon(iconForFlag(status, 6));
        negativeFlagLabel.setIcon(iconForFlag(status, 7));

        // Update the register and address displays

        // We always want to show the NEXT instruction that will be executed
        opcodeField.setText(cpu.disassembleNextOp());
        pcField.setText(cpu.getProgramCounterStatus());
        spField.setText(cpu.getStackPointerStatus());
        aField.setText(cpu.getAccumulatorStatus());
        xField.setText(cpu.getXRegisterStatus());
        yField.setText(cpu.getYRegisterStatus());

        repaint();
    }

    private ImageIcon iconForFlag(int state, int flagIndex) {
        ImageIcon imageIcon = null;

        if ((((state & 0xff) >> flagIndex) & 0x01) == 1) {
            switch (flagIndex) {
                case 0:
                    imageIcon = carryOn;
                    break;
                case 1:
                    imageIcon = zeroOn;
                    break;
                case 2:
                    imageIcon = irqOn;
                    break;
                case 3:
                    imageIcon = decimalOn;
                    break;
                case 4:
                    imageIcon = breakOn;
                    break;
                case 6:
                    imageIcon = overflowOn;
                    break;
                case 7:
                    imageIcon = negativeOn;
                    break;
            }
        } else {
            switch (flagIndex) {
                case 0:
                    imageIcon = carryOff;
                    break;
                case 1:
                    imageIcon = zeroOff;
                    break;
                case 2:
                    imageIcon = irqOff;
                    break;
                case 3:
                    imageIcon = decimalOff;
                    break;
                case 4:
                    imageIcon = breakOff;
                    break;
                case 6:
                    imageIcon = overflowOff;
                    break;
                case 7:
                    imageIcon = negativeOff;
                    break;
            }

        }

        return imageIcon;
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(LABEL_BORDER);
        label.setFont(LABEL_FONT);
        return label;
    }

    private JTextField makeTextField(Dimension size, boolean editable) {
        JTextField textField = new JTextField("");
        textField.setAlignmentX(LEFT_ALIGNMENT);
        textField.setEditable(editable);
        textField.setMinimumSize(size);
        textField.setMaximumSize(size);
        textField.setPreferredSize(size);
        textField.setBackground(Color.WHITE);
        return textField;
    }

    private int getHexVal(JTextField source) throws NumberFormatException {
        String val = source.getText().replaceAll("[^0-9a-fA-F]", "");
        return Integer.parseInt(val, 16);
    }
}
