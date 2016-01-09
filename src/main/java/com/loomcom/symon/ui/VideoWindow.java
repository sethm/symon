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

import com.loomcom.symon.devices.Crtc;
import com.loomcom.symon.devices.DeviceChangeListener;
import com.loomcom.symon.exceptions.MemoryAccessException;

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
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.*;

/**
 * VideoWindow represents a graphics framebuffer backed by a 6545 CRTC.
 * Each time the window's VideoPanel is repainted, the video memory is
 * scanned and converted to the appropriate bitmap representation.
 * <p>
 * The graphical representation of each character is derived from a
 * character generator ROM image. For this simulation, the Commodore PET
 * character generator ROM was chosen, but any character generator ROM
 * could be used in its place.
 * <p>
 * It may be convenient to think of this as the View (in the MVC
 * pattern sense) to the Crtc's Model and Controller. Whenever the CRTC
 * updates state in a way that may require the view to update, it calls
 * the <tt>deviceStateChange</tt> callback on this Window.
 */
public class VideoWindow extends JFrame implements DeviceChangeListener {

    private static final Logger logger = Logger.getLogger(VideoWindow.class.getName());

    private static final int CHAR_WIDTH = 8;
    private static final int CHAR_HEIGHT = 8;

    private final int scaleX, scaleY;
    private final boolean shouldScale;

    private BufferedImage image;
    private int[] charRom;

    private int horizontalDisplayed;
    private int verticalDisplayed;
    private int scanLinesPerRow;
    private int cursorBlinkRate;
    private boolean hideCursor;

    private Dimension dimensions;
    private Crtc crtc;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> cursorBlinker;

    /**
     * A panel representing the composite video output, with fast Graphics2D painting.
     */
    private class VideoPanel extends JPanel {
        @Override
        public void paintComponent(Graphics g) {
            try {
                for (int i = 0; i < crtc.getPageSize(); i++) {
                    int address = crtc.getStartAddress() + i;
                    int originX = (i % horizontalDisplayed) * CHAR_WIDTH;
                    int originY = (i / horizontalDisplayed) * scanLinesPerRow;
                    image.getRaster().setPixels(originX, originY, CHAR_WIDTH, scanLinesPerRow, getGlyph(address));
                }
                Graphics2D g2d = (Graphics2D) g;
                if (shouldScale) {
                    g2d.scale(scaleX, scaleY);
                }
                g2d.drawImage(image, 0, 0, null);
            } catch (MemoryAccessException ex) {
                logger.log(Level.SEVERE, "Memory Access Exception, can't paint video window! " + ex.getMessage());
            }
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

    /**
     * Runnable task that blinks the cursor.
     */
    private class CursorBlinker implements Runnable {
        public void run() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (cursorBlinkRate > 0) {
                        hideCursor = !hideCursor;
                        VideoWindow.this.repaint();
                    }
                }
            });
        }
    }

    public VideoWindow(Crtc crtc, int scaleX, int scaleY) throws IOException {
        crtc.registerListener(this);

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.crtc = crtc;
        this.charRom = loadCharRom("/ascii.rom");
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

    /**
     * Called by the CRTC on state change.
     */
    public void deviceStateChanged() {

        boolean repackNeeded = false;

        // TODO: I'm not entirely happy with this pattern, and I'd like to make it a bit DRY-er.

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
                hideCursor = false;
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

    private void createAndShowUi() {
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

    /**
     * Returns an array of pixels (including extra scanlines, if any) corresponding to the
     * Character ROM plus cursor overlay (if any). The cursor overlay simulates an XOR
     * of the Character Rom output and the 6545 Cursor output.
     *
     * @param address The address of the character being requested.
     * @return An array of integers representing the pixel data.
     */
    private int[] getGlyph(int address) throws MemoryAccessException {
        int chr = crtc.getCharAtAddress(address);
        int romOffset = (chr & 0xff) * (CHAR_HEIGHT * CHAR_WIDTH);
        int[] glyph = new int[CHAR_WIDTH * scanLinesPerRow];

        // Populate the character
        arraycopy(charRom, romOffset, glyph, 0, CHAR_WIDTH * Math.min(CHAR_HEIGHT, scanLinesPerRow));

        // Overlay the cursor
        if (!hideCursor && crtc.isCursorEnabled() && crtc.getCursorPosition() == address) {
            int cursorStart = Math.min(glyph.length, crtc.getCursorStartLine() * CHAR_WIDTH);
            int cursorStop = Math.min(glyph.length, (crtc.getCursorStopLine() + 1) * CHAR_WIDTH);

            for (int i = cursorStart; i < cursorStop; i++) {
                glyph[i] ^= 0xff;
            }
        }

        return glyph;
    }

    private void buildImage() {
        int rasterWidth = CHAR_WIDTH * horizontalDisplayed;
        int rasterHeight = scanLinesPerRow * verticalDisplayed;
        this.image = new BufferedImage(rasterWidth, rasterHeight, BufferedImage.TYPE_BYTE_BINARY);
        this.dimensions = new Dimension(rasterWidth * scaleX, rasterHeight * scaleY);
    }

    /**
     * Load a Character ROM file and convert it into an array of pixel data usable
     * by the underlying BufferedImage's Raster.
     * <p>
     * Since the BufferedImage is a TYPE_BYTE_BINARY, the data must be converted
     * into a single byte per pixel, 0 for black and 255 for white.

     * @param resource The ROM file resource to load.
     * @return An array of glyphs, each ready for insertion.
     * @throws IOException
     */
    private int[] loadCharRom(String resource) throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(this.getClass().getResourceAsStream(resource));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (bis.available() > 0) {
                bos.write(bis.read());
            }
            bos.flush();
            bos.close();

            byte[] raw = bos.toByteArray();

            // Now convert the raw ROM image into a format suitable for
            // insertion directly into the BufferedImage.
            int[] converted = new int[raw.length * CHAR_WIDTH];

            int romIndex = 0;
            for (int i = 0; i < converted.length;) {
                byte charRow = raw[romIndex++];

                for (int j = 7; j >= 0; j--) {
                    converted[i++] = ((charRow & (1 << j)) == 0) ? 0 : 0xff;
                }
            }
            return converted;
        } finally {
            if (bis != null) {
                bis.close();
            }
        }
    }
}
