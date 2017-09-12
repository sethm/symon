/*
 * Copyright (c) 2009-2011 Graham Edgecombe.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.loomcom.symon.jterminal.vt100;

import java.awt.Color;

import com.loomcom.symon.jterminal.AbstractTerminalModel;
import com.loomcom.symon.jterminal.TerminalCell;
import com.loomcom.symon.jterminal.TerminalModel;
import com.loomcom.symon.jterminal.bell.BellStrategy;
import com.loomcom.symon.jterminal.bell.NopBellStrategy;

/**
 * A VT100/ANSI-compatible terminal model.
 * @author Graham Edgecombe
 */
public class Vt100TerminalModel extends AbstractTerminalModel {

	/**
	 * A {@link AnsiControlSequenceListener} which modifies the
	 * {@link TerminalModel} appropriately when an event happens.
	 * @author Graham Edgecombe
	 */
	private class Vt100Listener implements AnsiControlSequenceListener {

		/**
		 * The saved cursor row.
		 */
		private int savedCursorRow = -1;

		/**
		 * The saved cursor column.
		 */
		private int savedCursorColumn = -1;

		@Override
		public void parsedControlSequence(AnsiControlSequence seq) {
			char command = seq.getCommand();
			String[] parameters = seq.getParameters();

			switch (command) {
			case 'A':
			case 'B':
			case 'C':
			case 'D':
				int n = 1;
				if (parameters.length == 1) {
					n = Integer.parseInt(parameters[0]);
				}
				if (command == 'A') {
					moveCursorUp(n);
				} else if (command == 'B') {
					moveCursorDown(n);
				} else if (command == 'C') {
					moveCursorForward(n);
				} else if (command == 'D') {
					moveCursorBack(n);
				}
				break;
			case 'E':
			case 'F':
				n = 1;
				if (parameters.length == 1) {
					n = Integer.parseInt(parameters[0]);
				}
				if (command == 'E') {
					moveCursorDown(n);
				} else if (command == 'F') {
					moveCursorUp(n);
				}
				setCursorColumn(0);
				break;
			case 'G':
				if (parameters.length == 1) {
					n = Integer.parseInt(parameters[0]);
					setCursorColumn(n - 1);
				}
				break;
			case 'H':
			case 'f':
				if (parameters.length == 2) {
					n = 1;
					int m = 1;
					if (parameters[0].length() > 0) {
						n = Integer.parseInt(parameters[0]);
					}
					if (parameters[1].length() > 0) {
						m = Integer.parseInt(parameters[1]);
					}
					setCursorRow(n - 1);
					setCursorColumn(m - 1);
				}
				break;
			case 'J':
				n = 0;
				if (parameters.length == 1) {
					n = Integer.parseInt(parameters[0]);
				}
				if (n == 0) {
					int row = cursorRow;
					int column = cursorColumn;
					while(row < rows) {
						while(column < columns) {
							cells[row][column] = null;
							column++;
						}
						column = 0;
						row++;
					}
				} else if (n == 1) {
					int row = cursorRow;
					int column = cursorColumn;
					while(row >= 0) {
						while(column >= 0) {
							cells[row][column] = null;
							column--;
						}
						column = columns - 1;
						row--;
					}
				} else if (n == 2) {
					clear();
				}
				break;
			case 'K':
				n = 0;
				if (parameters.length == 1) {
					n = Integer.parseInt(parameters[0]);
				}
				if (n == 0) {
					for (int row = cursorRow; row < rows; row++) {
						cells[row][cursorColumn] = null;
					}
				} else if (n == 1) {
					for (int row = cursorRow; row >= 0; row--) {
						cells[row][cursorColumn] = null;
					}
				} else if (n == 2) {
					for (int column = 0; column < columns; column++) {
						cells[cursorRow][column] = null;
					}
				}
				break;
			case 'm':
				if (parameters.length == 0) {
					parameters = new String[] { "0" };
				}
				for (String parameter : parameters) {
					if (parameter.equals("0")) {
						foregroundColor = DEFAULT_FOREGROUND_COLOR;
						backgroundColor = DEFAULT_BACKGROUND_COLOR;
						backgroundBold = DEFAULT_BACKGROUND_BOLD;
						foregroundBold = DEFAULT_FOREGROUND_BOLD;
					} else if (parameter.equals("2")) {
						backgroundBold = true;
						foregroundBold = true;
					} else if (parameter.equals("22")) {
						backgroundBold = false;
						foregroundBold = false;
					} else if ((parameter.startsWith("3") || parameter.startsWith("4")) && parameter.length() == 2) {
						int color = Integer.parseInt(parameter.substring(1));
						if (parameter.startsWith("3")) {
							foregroundColor = color;
						} else if (parameter.startsWith("4")) {
							backgroundColor = color;
						}
					}
				}
				break;
			case 'u':
				if (savedCursorColumn != -1 && savedCursorRow != -1) {
					cursorColumn = savedCursorColumn;
					cursorRow = savedCursorRow;
				}
				break;
			case 's':
				savedCursorColumn = cursorColumn;
				savedCursorRow = cursorRow;
				break;
			}
		}

		@Override
		public void parsedString(String str) {
			for (char ch : str.toCharArray()) {
				switch (ch) {
				case '\0':
					continue;
				case '\r':
					cursorColumn = 0;
					continue;
				case '\n':
					cursorRow++;
					break;
				case '\t':
					while ((++cursorColumn % TAB_WIDTH) != 0);
					continue;
				case 127:
					if (cursorColumn > 0) {
						cells[cursorRow][--cursorColumn] = null;
					}
					continue;
				case 7:
					bellStrategy.soundBell();
					continue;
				}

				if (cursorColumn >= columns) {
					cursorColumn = 0;
					cursorRow++;
				}

				if (cursorRow >= bufferSize) {
					for (int i = 1; i < bufferSize; i++) {
						System.arraycopy(cells[i], 0, cells[i - 1], 0, columns);
					}
					for (int i = 0; i < columns; i++) {
						cells[bufferSize - 1][i] = null;
					}
					cursorRow--;
				}

				Color back = backgroundBold ? SgrColor.COLOR_BRIGHT[backgroundColor] : SgrColor.COLOR_NORMAL[backgroundColor];
				Color fore = foregroundBold ? SgrColor.COLOR_BRIGHT[foregroundColor] : SgrColor.COLOR_NORMAL[foregroundColor];
				if (ch != '\n') {
					cells[cursorRow][cursorColumn++] = new TerminalCell(ch, back, fore);
				}
			}
		}

	}

	/**
	 * The default number of columns.
	 */
	private static final int DEFAULT_COLUMNS = 80;

	/**
	 * The default number of rows.
	 */
	private static final int DEFAULT_ROWS = 25;

	/**
	 * The tab width in characters.
	 */
	private static final int TAB_WIDTH = 8;

	/**
	 * The default foreground bold flag.
	 */
	private static final boolean DEFAULT_FOREGROUND_BOLD = false;

	/**
	 * The default background bold flag.
	 */
	private static final boolean DEFAULT_BACKGROUND_BOLD = false;

	/**
	 * The default foreground color.
	 */
	private static final int DEFAULT_FOREGROUND_COLOR = 7;

	/**
	 * The default background color.
	 */
	private static final int DEFAULT_BACKGROUND_COLOR = 0;

	/**
	 * The ANSI control sequence listener.
	 */
	private final AnsiControlSequenceListener listener = this.new Vt100Listener();

	/**
	 * The ANSI control sequence parser.
	 */
	private final AnsiControlSequenceParser parser = new AnsiControlSequenceParser(listener);

	/**
	 * The current bell strategy.
	 */
	private BellStrategy bellStrategy = new NopBellStrategy();

	/**
	 * The array of cells.
	 */
	private TerminalCell[][] cells;

	/**
	 * The number of columns.
	 */
	private int columns;

	/**
	 * The number of rows.
	 */
	private int rows;

	/**
	 * The buffer size.
	 */
	private int bufferSize;

	/**
	 * The cursor row.
	 */
	private int cursorRow = 0;

	/**
	 * The cursor column.
	 */
	private int cursorColumn = 0;

	/**
	 * The current foreground bold flag.
	 */
	private boolean foregroundBold = DEFAULT_FOREGROUND_BOLD;

	/**
	 * The current background bold flag.
	 */
	private boolean backgroundBold = DEFAULT_BACKGROUND_BOLD;

	/**
	 * The current foreground color.
	 */
	private int foregroundColor = DEFAULT_FOREGROUND_COLOR;

	/**
	 * The current background color.
	 */
	private int backgroundColor = DEFAULT_BACKGROUND_COLOR;

	/**
	 * Creates the terminal model with the default number of columns and rows,
	 * and the default buffer size.
	 */
	public Vt100TerminalModel() {
		this(DEFAULT_COLUMNS, DEFAULT_ROWS);
	}

	/**
	 * Creates the terminal model with the specified number of columns and
	 * rows. The buffer size is set to the number of rows.
	 * @param columns The number of columns.
	 * @param rows The number of rows.
	 * @throws IllegalArgumentException if the number of rows or columns is
	 * negative.
	 */
	public Vt100TerminalModel(int columns, int rows) {
		this(columns, rows, rows);
	}

	/**
	 * Creates the terminal model with the specified number of columns and rows
	 * and the specified buffer size.
	 * @param columns The number of columns.
	 * @param rows The number of rows.
	 * @param bufferSize The buffer size.
	 * @throws IllegalArgumentException if the number of rows or columns is
	 * negative, or if the buffer size is less than the number of rows.
	 */
	public Vt100TerminalModel(int columns, int rows, int bufferSize) {
		if (columns < 0 || rows < 0 || bufferSize < 0) {
			throw new IllegalArgumentException("Zero or positive values only allowed for columns, rows and buffer size.");
		}
		if (bufferSize < rows) {
			throw new IllegalArgumentException("The buffer is too small");
		}
		this.columns = columns;
		this.rows = rows;
		this.bufferSize = bufferSize;
		init();
	}

	/**
	 * Initializes the terminal model.
	 */
	private void init() {
		cells = new TerminalCell[bufferSize][columns];
	}

	@Override
	public int getCursorRow() {
		return cursorRow;
	}

	@Override
	public void setCursorRow(int row) {
		if (row < 0 || row >= bufferSize) {
			throw new IllegalArgumentException("row out of range");
		}
		cursorRow = row;
	}

	@Override
	public int getCursorColumn() {
		return cursorColumn;
	}

	@Override
	public void setCursorColumn(int column) {
		if (column < 0 || column >= columns) {
			throw new IllegalArgumentException("column out of range");
		}
		cursorColumn = column;
	}

	@Override
	public TerminalCell getCell(int column, int row) {
		if (column < 0 || row < 0 || column >= columns || row >= bufferSize) {
			throw new IndexOutOfBoundsException();
		}
		return cells[row][column];
	}

	@Override
	public void setCell(int column, int row, TerminalCell cell) {
		if (column < 0 || row < 0 || column >= columns || row >= bufferSize) {
			throw new IndexOutOfBoundsException();
		}
		cells[row][column] = cell;
	}

	@Override
	public void print(String str) {
		if (str == null) {
			throw new NullPointerException("str");
		}
		parser.parse(str);
	}

	@Override
	public int getColumns() {
		return columns;
	}

	@Override
	public int getRows() {
		return rows;
	}

	@Override
	public int getBufferSize() {
		return bufferSize;
	}

	@Override
	public BellStrategy getBellStrategy() {
		return bellStrategy;
	}

	@Override
	public void setBellStrategy(BellStrategy strategy) {
		if (strategy == null) {
			throw new NullPointerException("strategy");
		}
		this.bellStrategy = strategy;
	}

	@Override
	public Color getDefaultBackgroundColor() {
		final int bg = DEFAULT_BACKGROUND_COLOR;
		return DEFAULT_BACKGROUND_BOLD ? SgrColor.COLOR_BRIGHT[bg] : SgrColor.COLOR_NORMAL[bg];
	}

	@Override
	public Color getDefaultForegroundColor() {
		final int fg = DEFAULT_FOREGROUND_COLOR;
		return DEFAULT_FOREGROUND_BOLD ? SgrColor.COLOR_BRIGHT[fg] : SgrColor.COLOR_NORMAL[fg];
	}

}

