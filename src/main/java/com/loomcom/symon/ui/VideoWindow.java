package com.loomcom.symon.ui;

import javax.swing.*;
import java.awt.*;

public class VideoWindow extends JFrame {

    private CrtPanel crtPanel;

    public VideoWindow(CrtPanel crtPanel) {
        this.crtPanel = crtPanel;
        createUi();
    }

    public void createUi() {
        setTitle("Composite Video");

        int borderWidth = (int) (crtPanel.getWidth() * 0.08);
        int borderHeight = (int) (crtPanel.getHeight() * 0.08);

        JPanel containerPane = new JPanel();
        containerPane.setBorder(BorderFactory.createEmptyBorder(borderHeight, borderWidth, borderHeight, borderWidth));
        containerPane.setLayout(new BorderLayout());
        containerPane.setBackground(Color.black);
        containerPane.add(crtPanel, BorderLayout.CENTER);

        getContentPane().add(containerPane);
        setResizable(false);
        pack();
    }

    public void refreshDisplay() {
        // TODO: Verify whether this is necessary. Does `repaint()' do anything if the window is not visible?
        if (isVisible()) {
            repaint();
        }
    }

}
