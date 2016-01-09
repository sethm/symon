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

import com.loomcom.symon.Preferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;

/**
 * Dialog window that allows the user to modify selected run-time settings of the simulator.
 */
public class PreferencesDialog extends Observable implements Preferences {

    private final JDialog dialog;

    private JCheckBox  haltOnBreakCheckBox;
    private JTextField programLoadAddressField;

    private int programLoadAddress = DEFAULT_PROGRAM_LOAD_ADDRESS;
    private boolean haltOnBreak = DEFAULT_HALT_ON_BREAK;

    public PreferencesDialog(Frame parent, boolean modal) {
        this.dialog = new JDialog(parent, modal);

        createUi();
        updateUi();
    }

    public JDialog getDialog() {
        return dialog;
    }

    /**
     * TODO: Validation of input.
     */
    private void createUi() {
        dialog.setTitle("Preferences");
        Container contents = dialog.getContentPane();

        JPanel settingsContainer = new JPanel();
        JPanel buttonsContainer = new JPanel();

        GridBagLayout layout = new GridBagLayout();
        settingsContainer.setLayout(layout);

        final JLabel haltOnBreakLabel = new JLabel("Halt on BRK");
        final JLabel programLoadAddressLabel = new JLabel("Program Load Address");

        haltOnBreakCheckBox = new JCheckBox();
        programLoadAddressField = new JTextField(8);

        programLoadAddressLabel.setLabelFor(programLoadAddressField);

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        settingsContainer.add(haltOnBreakLabel, constraints);

        constraints.gridx = 1;
        settingsContainer.add(haltOnBreakCheckBox, constraints);

        constraints.gridy = 1;
        constraints.gridx = 0;
        settingsContainer.add(programLoadAddressLabel, constraints);

        constraints.gridx = 1;
        settingsContainer.add(programLoadAddressField, constraints);

        JButton applyButton = new JButton("Apply");
        JButton cancelButton = new JButton("Cancel");

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                PreferencesDialog.this.updateUi();
                dialog.setVisible(false);
            }
        });

        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                haltOnBreak = haltOnBreakCheckBox.isSelected();
                programLoadAddress = PreferencesDialog.this.hexToInt(programLoadAddressField.getText());
                PreferencesDialog.this.updateUi();
                // TODO: Actually check to see if values have changed, don't assume.
                PreferencesDialog.this.setChanged();
                PreferencesDialog.this.notifyObservers();
                dialog.setVisible(false);
            }
        });

        buttonsContainer.add(applyButton);
        buttonsContainer.add(cancelButton);

        contents.add(settingsContainer, BorderLayout.PAGE_START);
        contents.add(buttonsContainer, BorderLayout.PAGE_END);

        dialog.pack();
    }

    public int getProgramStartAddress() {
        return programLoadAddress;
    }

    /**
     * @return True if 'halt on break' is desired, false otherwise.
     */
    public boolean getHaltOnBreak() {
        return haltOnBreak;
    }

    public void updateUi() {
        haltOnBreakCheckBox.setSelected(haltOnBreak);
        programLoadAddressField.setText(intToHex(programLoadAddress));
    }

    private String intToHex(int i) {
        return String.format("%04x", i);
    }

    private int hexToInt(String s) {
        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException ex) {
            // TODO: Handle Displaying error.
            return 0;
        }
    }

}
