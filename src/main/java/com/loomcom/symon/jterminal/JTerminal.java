/*
 * Copyright (c) 2016 Seth J. Morabito <web@loomcom.com>
 * Portions Copyright (c) 2009-2011 Graham Edgecombe
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

package com.loomcom.symon.jterminal;

import com.loomcom.symon.jterminal.vt100.Vt100TerminalModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.JComponent;
import javax.swing.JScrollBar;

/**
 * Swing terminal emulation component
 * @author Seth J. Morabito
 *
 */
public class JTerminal extends JComponent {

	private static final long serialVersionUID = 2871625194146986567L;

	private int borderWidth = 0;

	private JScrollBar scrollBar;

	/**
	 * The terminal emulation model
	 */
	private TerminalModel model;

	/**
	 * The font this terminal uses
	 */
	private Font font;

	/**
	 * The cell width in pixels
	 */
	private int cellWidth;

	/**
	 * The cell height in pixels
	 */
	private int cellHeight;

	/**
	 * Max descender for font 
	 */
	private int maxDescender;

	public JTerminal(Font font) {
		this(new Vt100TerminalModel(), font);
	}

	public JTerminal(TerminalModel model, Font font) {
		setModel(model);
		setFont(font);
		init();
	}

	public void setBorderWidth(int borderWidth) {
		this.borderWidth = borderWidth;
		revalidate();
	}

	public int getBorderWidth() {
		return borderWidth;
	}

	public void setFont(Font font) {
		this.font = font;
		setCellWidthAndHeight(font);
		revalidate();
	}

	public Font getFont() {
		return font;
	}

	public void setModel(TerminalModel model) {
		if (model == null) {
			throw new NullPointerException("model");
		}
		this.model = model;
	}

	public TerminalModel getModel() {
		return model;
	}

	public void println(String str) {
		if (str == null) {
			throw new NullPointerException("str");
		}
		print(str.concat("\r\n"));
	}

	public void print(String str) {
		model.print(str);
	}

	public Dimension getMinimumSize() {
		return new Dimension(model.getColumns() * cellWidth + borderWidth * 2,
				model.getRows() * cellHeight + borderWidth * 2);
	}

	public Dimension getMaximumSize() {
		return getMinimumSize();
	}

	public Dimension getPreferredSize() {
		return getMinimumSize();
	}

	public void paint(Graphics g) {
		g.setFont(font);

		int width = model.getColumns();
		int height = model.getBufferSize();

		g.setColor(model.getDefaultBackgroundColor());
		g.fillRect(0, 0, width * cellWidth + borderWidth * 2, height * cellHeight + borderWidth * 2);

		int start = scrollBar == null ? 0 : scrollBar.getValue();
		for (int y = start; y < height; y++) {
			for (int x = 0; x < width; x++) {
				TerminalCell cell = model.getCell(x, y);
				boolean cursorHere = (model.getCursorRow() == y) && (model.getCursorColumn() == x);

				if ((cursorHere) && (cell == null)) {
					cell = new TerminalCell(' ', model.getDefaultBackgroundColor(), model.getDefaultForegroundColor());
				}

				if (cell != null) {
					int px = x * cellWidth + borderWidth;
					int py = (y - start) * cellHeight + borderWidth;

					g.setColor(cursorHere ? cell.getForegroundColor() : cell.getBackgroundColor());
					g.fillRect(px, py, cellWidth, cellHeight);

					g.setColor(cursorHere ? cell.getBackgroundColor() : cell.getForegroundColor());
					g.drawChars(new char[] { cell.getCharacter() }, 0, 1, px, py + cellHeight - maxDescender);
				}
			}
		}
	}

	private void init() {
		setLayout(new BorderLayout(0, 0));

		int rows = model.getRows();
		int bufferSize = model.getBufferSize();

		if (bufferSize > rows) {
			scrollBar = new JScrollBar(1, 0, rows, 0, bufferSize + 1);
			scrollBar.addAdjustmentListener(new AdjustmentListener() {
				public void adjustmentValueChanged(AdjustmentEvent evt) {
					repaint();
				}
			});
			add("After", scrollBar);
		}

		repaint();
	}

	private void setCellWidthAndHeight(Font font) {
		FontMetrics metrics = getFontMetrics(font);
		cellWidth = metrics.charWidth('W');
		cellHeight = metrics.getHeight();
		maxDescender = metrics.getMaxDescent();
	}
}
