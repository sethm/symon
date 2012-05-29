/**
 *
 */
package com.loomcom.symon.ui;

import com.loomcom.symon.Cpu;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class StatusPanel extends JPanel {

    private final ImageIcon carryOn;
    private final ImageIcon carryOff;
    private final ImageIcon zeroOn;
    private final ImageIcon zeroOff;
    private final ImageIcon irqOn;
    private final ImageIcon irqOff;
    private final ImageIcon decimalOn;
    private final ImageIcon decimalOff;
    private final ImageIcon breakOn;
    private final ImageIcon breakOff;
    private final ImageIcon overflowOn;
    private final ImageIcon overflowOff;
    private final ImageIcon negativeOn;
    private final ImageIcon negativeOff;

    private final JLabel carryFlagLabel;
    private final JLabel zeroFlagLabel;
    private final JLabel irqDisableFlagLabel;
    private final JLabel decimalModeFlagLabel;
    private final JLabel breakFlagLabel;
    private final JLabel overflowFlagLabel;
    private final JLabel negativeFlagLabel;

    private JTextField opcodeField;
    private JTextField pcField;
    private JTextField aField;
    private JTextField xField;
    private JTextField yField;
//    private JTextField stepCountField;

    private final JLabel opcodeLabel;
    private final JLabel pcLabel;
    private final JLabel aLabel;
    private final JLabel xLabel;
    private final JLabel yLabel;
//    private final JLabel stepCountLabel;

    private static final int WIDTH = 140;
    private static final int HEIGHT = 200;

    public StatusPanel() {
        super();

        Dimension dimensions = new Dimension(WIDTH, HEIGHT);

        setMinimumSize(dimensions);
        setPreferredSize(dimensions);
        setMaximumSize(dimensions);

        setLayout(new GridLayout(13,1));

        JPanel statusFlagsPanel = new JPanel();

        carryOn = new ImageIcon(this.getClass().getResource("images/C_on.png"));
        carryOff = new ImageIcon(this.getClass().getResource("images/C_off.png"));
        zeroOn = new ImageIcon(this.getClass().getResource("images/Z_on.png"));
        zeroOff = new ImageIcon(this.getClass().getResource("images/Z_off.png"));
        irqOn = new ImageIcon(this.getClass().getResource("images/I_on.png"));
        irqOff = new ImageIcon(this.getClass().getResource("images/I_off.png"));
        decimalOn = new ImageIcon(this.getClass().getResource("images/D_on.png"));
        decimalOff = new ImageIcon(this.getClass().getResource("images/D_off.png"));
        breakOn = new ImageIcon(this.getClass().getResource("images/B_on.png"));
        breakOff = new ImageIcon(this.getClass().getResource("images/B_off.png"));
        overflowOn = new ImageIcon(this.getClass().getResource("images/O_on.png"));
        overflowOff = new ImageIcon(this.getClass().getResource("images/O_off.png"));
        negativeOn = new ImageIcon(this.getClass().getResource("images/N_on.png"));
        negativeOff = new ImageIcon(this.getClass().getResource("images/N_off.png"));

        // Initialize all to off
        carryFlagLabel = new JLabel(carryOff, JLabel.CENTER);
        zeroFlagLabel = new JLabel(zeroOff, JLabel.CENTER);
        irqDisableFlagLabel = new JLabel(irqOff, JLabel.CENTER);
        decimalModeFlagLabel = new JLabel(decimalOff, JLabel.CENTER);
        breakFlagLabel = new JLabel(breakOff, JLabel.CENTER);
        overflowFlagLabel = new JLabel(overflowOff, JLabel.CENTER);
        negativeFlagLabel = new JLabel(negativeOff, JLabel.CENTER);

        Border emptyBorder = BorderFactory.createEmptyBorder(8, 6, 0, 6);

        // Create and add register and address labels
        opcodeLabel = new JLabel("Instruction");
        opcodeLabel.setBorder(emptyBorder);
        pcLabel = new JLabel("Program Counter");
        pcLabel.setBorder(emptyBorder);
        aLabel = new JLabel("Accumulator");
        aLabel.setBorder(emptyBorder);
        xLabel = new JLabel("X Register");
        xLabel.setBorder(emptyBorder);
        yLabel = new JLabel("Y Register");
        yLabel.setBorder(emptyBorder);
//        stepCountLabel = new JLabel("Steps");
//        stepCountLabel.setBorder(emptyBorder);

        opcodeField = new JTextField("");
        pcField = new JTextField("");
        aField = new JTextField("");
        xField = new JTextField("");
        yField = new JTextField("");
//        stepCountField = new JTextField("");

        opcodeField.setEditable(false);
        pcField.setEditable(false);
        aField.setEditable(false);
        xField.setEditable(false);
        yField.setEditable(false);
//        stepCountField.setEditable(false);

        opcodeLabel.setMinimumSize(new Dimension(100, 20));
        pcLabel.setMinimumSize(new Dimension(80, 20));
        aLabel.setMinimumSize(new Dimension(60, 20));
        xLabel.setMinimumSize(new Dimension(60, 20));
        yLabel.setMinimumSize(new Dimension(60, 20));
//        stepCountLabel.setMinimumSize(new Dimension(120, 20));

        opcodeLabel.setPreferredSize(new Dimension(100, 20));
        pcLabel.setPreferredSize(new Dimension(80, 20));
        aLabel.setPreferredSize(new Dimension(60, 20));
        xLabel.setPreferredSize(new Dimension(60, 20));
        yLabel.setPreferredSize(new Dimension(60, 20));
//        stepCountLabel.setPreferredSize(new Dimension(120, 20));

        statusFlagsPanel.add(negativeFlagLabel);
        statusFlagsPanel.add(overflowFlagLabel);
        statusFlagsPanel.add(breakFlagLabel);
        statusFlagsPanel.add(decimalModeFlagLabel);
        statusFlagsPanel.add(irqDisableFlagLabel);
        statusFlagsPanel.add(zeroFlagLabel);
        statusFlagsPanel.add(carryFlagLabel);

        add(statusFlagsPanel);
        add(opcodeLabel);
        add(opcodeField);
        add(pcLabel);
        add(pcField);
        add(aLabel);
        add(aField);
        add(xLabel);
        add(xField);
        add(yLabel);
        add(yField);
//        add(stepCountLabel);
//        add(stepCountField);

        setBorder(BorderFactory.createBevelBorder(3));
    }

    /**
     * Update the display based on the current state of the CPU.
     *
     * @param cpu The simulated 6502 CPU.
     */
    public void updateState(Cpu cpu) {
        // Update the Processor Status Flag display
        int state = cpu.getProcessorStatus();

        carryFlagLabel.setIcon(iconForFlag(state, 0));
        zeroFlagLabel.setIcon(iconForFlag(state, 1));
        irqDisableFlagLabel.setIcon(iconForFlag(state, 2));
        decimalModeFlagLabel.setIcon(iconForFlag(state, 3));
        breakFlagLabel.setIcon(iconForFlag(state, 4));
        overflowFlagLabel.setIcon(iconForFlag(state, 6));
        negativeFlagLabel.setIcon(iconForFlag(state, 7));

        // Update the register and address displays
        opcodeField.setText(cpu.getOpcodeStatus());
        pcField.setText(cpu.getProgramCounterStatus());
        aField.setText(cpu.getARegisterStatus());
        xField.setText(cpu.getXRegisterStatus());
        yField.setText(cpu.getYRegisterStatus());
//        stepCountField.setText(Long.toString(cpu.getStepCounter()));

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

}
