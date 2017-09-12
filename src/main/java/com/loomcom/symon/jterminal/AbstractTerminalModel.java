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

package com.loomcom.symon.jterminal;

/**
 * A {@link TerminalModel} which implements some common behaviour.
 * @author Graham Edgecombe
 */
public abstract class AbstractTerminalModel implements TerminalModel {

	@Override
	public void clear() {
		int rows = getRows(), columns = getColumns();
		for (int column = 0; column < columns; column++) {
			for (int row = 0; row < rows; row++) {
				setCell(column, row, null);
			}
		}
	}

	@Override
	public void moveCursorBack(int n) {
		if (n < 0) {
			throw new IllegalArgumentException("n must be positive");
		}
		int cursorColumn = getCursorColumn() - n;
		if (cursorColumn < 0) {
			cursorColumn = 0;
		}
		setCursorColumn(cursorColumn);
	}

	@Override
	public void moveCursorForward(int n) {
		if (n < 0) {
			throw new IllegalArgumentException("n must be positive");
		}
		int columns = getColumns();
		int cursorColumn = getCursorColumn() + n;
		if (cursorColumn >= columns) {
			cursorColumn = columns - 1;
		}
		setCursorColumn(cursorColumn);
	}

	@Override
	public void moveCursorDown(int n) {
		if (n < 0) {
			throw new IllegalArgumentException("n must be positive");
		}
		int bufferSize = getBufferSize();
		int cursorRow = getCursorRow() + n;
		if (cursorRow >= bufferSize) {
			cursorRow = bufferSize - 1;
		}
		setCursorRow(cursorRow);
	}

	@Override
	public void moveCursorUp(int n) {
		if (n < 0) {
			throw new IllegalArgumentException("n must be positive");
		}
		int cursorRow = getCursorRow() - n;
		if (cursorRow < 0) {
			cursorRow = 0;
		}
		setCursorRow(cursorRow);
	}

}

