/**
 *
 */
package com.loomcom.symon.ui;

import com.loomcom.symon.Cpu;

import javax.swing.*;
import java.awt.*;

public class StatusPane extends JPanel {

    // The CPU to ask for state information.
    private final Cpu cpu;

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

    private final JLabel opcodeLabel;
    private final JLabel pcLabel;
    private final JLabel aLabel;
    private final JLabel xLabel;
    private final JLabel yLabel;
    private final JLabel stepCountLabel;

    private static final int WIDTH = 134;
    private static final int HEIGHT = 27;

    public StatusPane(Cpu cpu) {
        super();

        this.cpu = cpu;

        Dimension dimensions = new Dimension(WIDTH, HEIGHT);

        setMinimumSize(dimensions);
        setPreferredSize(dimensions);
        setMaximumSize(dimensions);

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

        // Create and add register and address labels

        this.opcodeLabel = new JLabel();
        this.pcLabel = new JLabel();
        this.aLabel = new JLabel();
        this.xLabel = new JLabel();
        this.yLabel = new JLabel();
        this.stepCountLabel = new JLabel();

        this.opcodeLabel.setMinimumSize(new Dimension(100, 20));
        this.pcLabel.setMinimumSize(new Dimension(80, 20));
        this.aLabel.setMinimumSize(new Dimension(60, 20));
        this.xLabel.setMinimumSize(new Dimension(60, 20));
        this.yLabel.setMinimumSize(new Dimension(60, 20));
        this.stepCountLabel.setMinimumSize(new Dimension(120, 20));

        this.opcodeLabel.setPreferredSize(new Dimension(100, 20));
        this.pcLabel.setPreferredSize(new Dimension(80, 20));
        this.aLabel.setPreferredSize(new Dimension(60, 20));
        this.xLabel.setPreferredSize(new Dimension(60, 20));
        this.yLabel.setPreferredSize(new Dimension(60, 20));
        this.stepCountLabel.setPreferredSize(new Dimension(120, 20));

        this.setLayout(new FlowLayout());

        this.add(negativeFlagLabel);
        this.add(overflowFlagLabel);
        this.add(breakFlagLabel);
        this.add(decimalModeFlagLabel);
        this.add(irqDisableFlagLabel);
        this.add(zeroFlagLabel);
        this.add(carryFlagLabel);

        this.add(opcodeLabel);
        this.add(pcLabel);
        this.add(aLabel);
        this.add(xLabel);
        this.add(yLabel);
        this.add(stepCountLabel);

        updateState();
    }

    public void updateState() {
        // Update the Processor Status Flag display
        int state = this.cpu.getProcessorStatus();

        carryFlagLabel.setIcon(iconForFlag(state, 0));
        zeroFlagLabel.setIcon(iconForFlag(state, 1));
        irqDisableFlagLabel.setIcon(iconForFlag(state, 2));
        decimalModeFlagLabel.setIcon(iconForFlag(state, 3));
        breakFlagLabel.setIcon(iconForFlag(state, 4));
        overflowFlagLabel.setIcon(iconForFlag(state, 6));
        negativeFlagLabel.setIcon(iconForFlag(state, 7));

        // Update the register and address displays
        opcodeLabel.setText(cpu.getOpcodeStatus());
        pcLabel.setText(cpu.getProgramCounterStatus());
        aLabel.setText(cpu.getARegisterStatus());
        xLabel.setText(cpu.getXRegisterStatus());
        yLabel.setText(cpu.getYRegisterStatus());
        stepCountLabel.setText(Long.toString(cpu.getStepCounter()));

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
