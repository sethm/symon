package com.loomcom.symon.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.*;

/**
 * Simulates a CRT Display backed by a 6545 CRTC.
 */
public class CrtPanel extends JPanel {

    // Character width and height are hardware-implementation specific
    // and cannot be modified at runtime.
    private final int charWidth;
    private final int charHeight;
    private final int scaleX, scaleY;
    private final boolean shouldScale;

    private Dimension dimensions;
    private BufferedImage image;

    private int[] charRom;
    private int[] videoRam;

    /* Fields corresponding to internal registers in the MOS/Rockwell 6545 */

    // R1 - Horizontal Displayed
    private final int horizontalDisplayed;
    // R6 - Vertical Displayed
    private final int verticalDisplayed;
    // R9 - Scan Lines: Number of scan lines per character, including spacing.
    private int scanLinesPerRow = 9;
    // R10 - Cursor Start
    private int cursorStartLine;
    // R11 - Cursor End
    private int cursorStopLine;
    private boolean cursorEnabled;
    private int cursorBlinkDelay;
    private boolean cursorBlinkEnabled;
    // R12, R13 - Display Start Address: The starting address in the video RAM of the displayed page.
    private int startAddress;
    // R14, R15 - Cursor Position
    private int cursorPosition;

    // The size, in bytes, of a displayed page of characters.
    private int pageSize;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> cursorBlinker;

    private class CursorBlinker implements Runnable {
        public void run() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (cursorBlinkEnabled) {
                        cursorEnabled = !cursorEnabled;
                        repaint();
                    }
                }
            });
        }
    }

    public CrtPanel(byte[] charRom, int[] videoRam,
                    int horizontalDisplayed, int verticalDisplayed,
                    int charWidth, int charHeight,
                    int scaleX, int scaleY,
                    int startAddress) {
        super();

        this.charRom = convertCharRom(charRom, charWidth);
        this.videoRam = videoRam;
        this.horizontalDisplayed = horizontalDisplayed;
        this.verticalDisplayed = verticalDisplayed;
        this.pageSize = horizontalDisplayed * verticalDisplayed;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.startAddress = startAddress;
        this.charWidth = charWidth;
        this.charHeight = charHeight;
        this.scanLinesPerRow = charHeight + 1;
        this.cursorStartLine = 0;
        this.cursorStopLine = charHeight - 1;
        this.cursorBlinkEnabled = true;
        this.cursorBlinkDelay = 500; // ms

        this.shouldScale = (this.scaleX > 1 || this.scaleY > 1);

        buildImage();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        cursorBlinker = scheduler.scheduleAtFixedRate(new CursorBlinker(), cursorBlinkDelay, cursorBlinkDelay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void paintComponent(Graphics g) {
        for (int i = 0; i < pageSize; i++) {
            int address = startAddress + i;
            int originX = (i % horizontalDisplayed) * charWidth;
            int originY = (i / horizontalDisplayed) * scanLinesPerRow;
            image.getRaster().setPixels(originX, originY, charWidth, scanLinesPerRow, getGlyph(i, videoRam[address]));
        }
        Graphics2D g2d = (Graphics2D)g;
        if (shouldScale) {
            g2d.scale(scaleX, scaleY);
        }
        g2d.drawImage(image, 0, 0, null);
    }

    public void setStartAddress(int address) {
        startAddress = address;
        repaint();
    }

    public int getStartAddress() {
        return startAddress;
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
        int romOffset = (chr & 0xff) * (charHeight * charWidth);
        int[] glyph = new int[charWidth * scanLinesPerRow];

        // Populate the character
        for (int i = 0; i < (charWidth * Math.min(charHeight, scanLinesPerRow)); i++) {
            glyph[i] = charRom[romOffset + i];
        }

        // Overlay the cursor
        if (cursorEnabled && cursorPosition == position) {
            int cursorStart = Math.min(glyph.length, cursorStartLine * charWidth);
            int cursorStop = Math.min(glyph.length, (cursorStopLine + 1) * charWidth);

            for (int i = cursorStart; i < cursorStop; i++) {
                glyph[i] ^= 0xff;
            }
        }

        return glyph;
    }

    @Override
    public int getWidth() {
        return (int) dimensions.getWidth();
    }

    @Override
    public int getHeight() {
        return (int) dimensions.getHeight();
    }

    @Override
    public Dimension getPreferredSize() {
        return dimensions;
    }

    @Override
    public Dimension getMaximumSize() {
        return dimensions;
    }

    @Override
    public Dimension getMinimumSize() {
        return dimensions;
    }

    public int getCursorStartLine() {
        return cursorStartLine;
    }

    public void setCursorStartLine(int cursorStartLine) {
        this.cursorStartLine = cursorStartLine;
    }

    public int getCursorStopLine() {
        return cursorStopLine;
    }

    public void setCursorStopLine(int cursorStopLine) {
        this.cursorStopLine = cursorStopLine;
    }

    public int getCursorBlinkDelay() {
        return cursorBlinkDelay;
    }

    public void setCursorBlinkDelay(int cursorBlinkDelay) {
        this.cursorBlinkDelay = cursorBlinkDelay;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public void setCursorPosition(int cursorPosition) {
        this.cursorPosition = cursorPosition;
    }

    public boolean isCursorEnabled() {
        return cursorEnabled;
    }

    public void setCursorEnabled(boolean cursorEnabled) {
        this.cursorEnabled = cursorEnabled;
    }

    public boolean isCursorBlinkEnabled() {
        return cursorBlinkEnabled;
    }

    public void setCursorBlinkEnabled(boolean cursorBlinkEnabled) {
        if (cursorBlinkEnabled && cursorBlinker == null) {
            cursorBlinker = scheduler.scheduleAtFixedRate(new CursorBlinker(),
                                                          cursorBlinkDelay,
                                                          cursorBlinkDelay,
                                                          TimeUnit.MILLISECONDS);
        } else if (!cursorBlinkEnabled && cursorBlinker != null) {
            cursorBlinker.cancel(true);
            cursorBlinker = null;
        }

        this.cursorBlinkEnabled = cursorBlinkEnabled;
        repaint();
    }

    public void setScanLinesPerRow(int scanLinesPerRow) {
        this.scanLinesPerRow = scanLinesPerRow;
        buildImage();
    }

    public int getScanLinesPerRow() {
        return scanLinesPerRow;
    }

    private void buildImage() {
        int rasterWidth = charWidth * horizontalDisplayed;
        int rasterHeight = scanLinesPerRow * verticalDisplayed;

        this.image = new BufferedImage(rasterWidth, rasterHeight, BufferedImage.TYPE_BYTE_BINARY);
        this.dimensions = new Dimension(rasterWidth * scaleX, rasterHeight * scaleY);
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

}
