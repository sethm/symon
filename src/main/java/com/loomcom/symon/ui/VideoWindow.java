/*
 * Copyright (c) 2013 Seth J. Morabito <web@loomcom.com>
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

import com.loomcom.symon.devices.Crtc;
import com.loomcom.symon.devices.DeviceChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class VideoWindow extends JFrame implements DeviceChangeListener {

    private static final Logger logger = Logger.getLogger(VideoWindow.class.getName());

    private static final int CHAR_WIDTH = 8;
    private static final int CHAR_HEIGHT = 8;

    private final int scaleX, scaleY;
    private final boolean shouldScale;

    private BufferedImage image;
    private int[] charRom;
    private int[] videoRam;

    private int horizontalDisplayed;
    private int verticalDisplayed;
    private int scanLinesPerRow;
    private int cursorBlinkRate;
    private boolean showCursor;

    private Dimension dimensions;
    private Crtc crtc;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> cursorBlinker;

    private class VideoPanel extends JPanel {
        @Override
        public void paintComponent(Graphics g) {
            for (int i = 0; i < crtc.getPageSize(); i++) {
                int address = crtc.getStartAddress() + i;
                int originX = (i % horizontalDisplayed) * CHAR_WIDTH;
                int originY = (i / horizontalDisplayed) * scanLinesPerRow;
                image.getRaster().setPixels(originX, originY, CHAR_WIDTH, scanLinesPerRow, getGlyph(i, videoRam[address]));
            }
            Graphics2D g2d = (Graphics2D)g;
            if (shouldScale) {
                g2d.scale(scaleX, scaleY);
            }
            g2d.drawImage(image, 0, 0, null);
        }

        @Override
        public Dimension getMinimumSize() {
            return dimensions;
        }

        @Override
        public Dimension getPreferredSize() {
            return dimensions;
        }

    }

    private class CursorBlinker implements Runnable {
        public void run() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (cursorBlinkRate > 0) {
                        showCursor = !showCursor;
                        repaint();
                    }
                }
            });
        }
    }

    public VideoWindow(Crtc crtc, int scaleX, int scaleY) throws IOException {
        crtc.registerListener(this);

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.crtc = crtc;
        this.charRom = convertCharRom(loadCharRom("/pet.rom"), CHAR_WIDTH);
        this.videoRam = crtc.getDmaAccess();
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.shouldScale = (scaleX > 1 || scaleY > 1);
        this.cursorBlinkRate = crtc.getCursorBlinkRate();

        if (cursorBlinkRate > 0) {
            this.cursorBlinker = scheduler.scheduleAtFixedRate(new CursorBlinker(),
                                                               cursorBlinkRate,
                                                               cursorBlinkRate,
                                                               TimeUnit.MILLISECONDS);
        }

        // Capture some state from the CRTC that will define the
        // window size. When these values change, the window will
        // need to re-pack and redraw.
        this.horizontalDisplayed = crtc.getHorizontalDisplayed();
        this.verticalDisplayed = crtc.getVerticalDisplayed();
        this.scanLinesPerRow = crtc.getScanLinesPerRow();

        buildImage();

        createAndShowUi();

    }

    private byte[] loadCharRom(String resource) throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(this.getClass().getResourceAsStream(resource));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (bis.available() > 0) {
                bos.write(bis.read());
            }
            bos.flush();
            bos.close();
            return bos.toByteArray();
        } finally {
            if (bis != null) {
                bis.close();
            }
        }
    }

    public void createAndShowUi() {
        setTitle("Composite Video");

        int borderWidth = 20;
        int borderHeight = 20;

        JPanel containerPane = new JPanel();
        containerPane.setBorder(BorderFactory.createEmptyBorder(borderHeight, borderWidth, borderHeight, borderWidth));
        containerPane.setLayout(new BorderLayout());
        containerPane.setBackground(Color.black);

        containerPane.add(new VideoPanel(), BorderLayout.CENTER);

        getContentPane().add(containerPane, BorderLayout.CENTER);
        setResizable(false);
        pack();
    }

    private void buildImage() {
        int rasterWidth = CHAR_WIDTH * horizontalDisplayed;
        int rasterHeight = scanLinesPerRow * verticalDisplayed;
        this.image = new BufferedImage(rasterWidth, rasterHeight, BufferedImage.TYPE_BYTE_BINARY);
        this.dimensions = new Dimension(rasterWidth * scaleX, rasterHeight * scaleY);
    }

    /**
     * Called by the CRTC on state change.
     */
    public void deviceStateChanged() {

        // Certain state
        boolean repackNeeded = false;

        if (horizontalDisplayed != crtc.getHorizontalDisplayed()) {
            horizontalDisplayed = crtc.getHorizontalDisplayed();
            repackNeeded = true;
        }

        if (verticalDisplayed != crtc.getVerticalDisplayed()) {
            verticalDisplayed = crtc.getVerticalDisplayed();
            repackNeeded = true;
        }

        if (scanLinesPerRow != crtc.getScanLinesPerRow()) {
            scanLinesPerRow = crtc.getScanLinesPerRow();
            repackNeeded = true;
        }

        if (cursorBlinkRate != crtc.getCursorBlinkRate()) {
            cursorBlinkRate = crtc.getCursorBlinkRate();

            if (cursorBlinker != null) {
                cursorBlinker.cancel(true);
                cursorBlinker = null;
            }

            if (cursorBlinkRate > 0) {
                cursorBlinker = scheduler.scheduleAtFixedRate(new CursorBlinker(),
                                                              cursorBlinkRate,
                                                              cursorBlinkRate,
                                                              TimeUnit.MILLISECONDS);
            }
        }

        if (repackNeeded) {
            buildImage();
            invalidate();
            pack();
        }
    }

    /**
     * Convert a raw binary Character ROM image into an array of pixel data usable
     * by the Raster underlying the display's BufferedImage.
     *
     * @param rawBytes
     * @param charWidth
     * @return
     */
    private int[] convertCharRom(byte[] rawBytes, int charWidth) {
        int[] converted = new int[rawBytes.length * charWidth];

        int romIndex = 0;
        for (int i = 0; i < converted.length;) {
            byte charRow = rawBytes[romIndex++];

            for (int j = 7; j >= 0; j--) {
                converted[i++] = ((charRow & (1 << j)) == 0) ? 0 : 0xff;
            }
        }
        return converted;
    }

    /**
     * Returns an array of pixels (including extra scanlines, if any) corresponding to the
     * Character ROM plus cursor overlay (if any). The cursor overlay simulates an XOR
     * of the Character Rom output and the 6545 Cursor output.
     *
     * @param position The position within the character field, from 0 to (horizontalDisplayed * verticalDisplayed)
     * @param chr The character value within the ROM to display.
     * @return
     */
    private int[] getGlyph(int position, int chr) {
        int romOffset = (chr & 0xff) * (CHAR_HEIGHT * CHAR_WIDTH);
        int[] glyph = new int[CHAR_WIDTH * scanLinesPerRow];

        // Populate the character
        for (int i = 0; i < (CHAR_WIDTH * Math.min(CHAR_HEIGHT, scanLinesPerRow)); i++) {
            glyph[i] = charRom[romOffset + i];
        }

        // Overlay the cursor
        if (showCursor && crtc.isCursorEnabled() && crtc.getCursorPosition() == position) {
            int cursorStart = Math.min(glyph.length, crtc.getCursorStartLine() * CHAR_WIDTH);
            int cursorStop = Math.min(glyph.length, (crtc.getCursorStopLine() + 1) * CHAR_WIDTH);

            for (int i = cursorStart; i < cursorStop; i++) {
                glyph[i] ^= 0xff;
            }
        }

        return glyph;
    }

    public void refreshDisplay() {
        // TODO: Verify whether this is necessary. Does `repaint()' do anything if the window is not visible?
        if (isVisible()) {
            repaint();
        }
    }

}
