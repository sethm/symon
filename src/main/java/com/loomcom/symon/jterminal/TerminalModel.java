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

import java.awt.Color;

import com.loomcom.symon.jterminal.bell.BellStrategy;

/**
 * Model for terminals - defines methods for getting/setting cells, printing
 * text to a terminal and getting the size of the terminal and buffer.
 */
public interface TerminalModel {

	/**
	 * Sets the bell strategy.
	 * @param strategy The bell strategy.
	 * @throws NullPointerException if the strategy is {@code null}.
	 */
    void setBellStrategy(BellStrategy strategy);

	/**
	 * Clears the terminal.
	 */
    void clear();

	/**
	 * Moves the cursor back n characters.
	 * @param n The number of characters.
	 * @throws IllegalArgumentException if n is not positive.
	 */
    void moveCursorBack(int n);

	/**
	 * Moves the cursor forward n characters.
	 * @param n The number of characters.
	 * @throws IllegalArgumentException if n is not positive.
	 */
    void moveCursorForward(int n);

	/**
	 * Moves the cursor down n characters.
	 * @param n The number of characters.
	 * @throws IllegalArgumentException if n is not positive.
	 */
    void moveCursorDown(int n);

	/**
	 * Moves the cursor up n characters.
	 * @param n The number of characters.
	 * @throws IllegalArgumentException if n is not positive.
	 */
    void moveCursorUp(int n);

	/**
	 * Sets a cell.
	 * @param column The column.
	 * @param row The row.
	 * @param cell The cell.
	 * @throws IndexOutOfBoundsException if the column and/or row number(s) are
	 * out of bounds.
	 */
    void setCell(int column, int row, TerminalCell cell);

	/**
	 * Gets a cell.
	 * @param column The column.
	 * @param row The row.
	 * @return The cell.
	 * @throws IndexOutOfBoundsException if the column and/or row number(s) are
	 * out of bounds.
	 */
    TerminalCell getCell(int column, int row);

	/**
	 * Prints the specified string to the terminal at the cursor position,
	 * interpreting any escape sequences/special ASCII codes the model may
	 * support. Lines will be wrapped if necessary.
	 * @param str The string to print.
	 * @throws NullPointerException if the string is {@code null}.
	 */
    void print(String str);

	/**
	 * Gets the number of columns.
	 * @return The number of columns.
	 */
    int getColumns();

	/**
	 * Gets the number of rows.
	 * @return The number of rows.
	 */
	int getRows();

	/**
	 * Gets the buffer size.
	 * @return The buffer size.
	 */
	int getBufferSize();

	/**
	 * Gets the cursor row.
	 * @return The cursor row.
	 */
	int getCursorRow();

	/**
	 * Sets the cursor row.
	 * @param row The cursor row.
	 * @throws IllegalArgumentException if the row is out of the valid range.
	 */
	void setCursorRow(int row);

	/**
	 * Gets the cursor column.
	 * @return The cursor column.
	 */
	int getCursorColumn();

	/**
	 * Sets the cursor column.
	 * @param column The cursor column.
	 * @throws IllegalArgumentException if the column is out of the valid range.
	 */
	void setCursorColumn(int column);

	/**
	 * Gets the default background color.
	 * @return The default background color.
	 */
	Color getDefaultBackgroundColor();

	/**
	 * Gets the default foreground color.
	 * @return The default foreground color.
	 */
	Color getDefaultForegroundColor();

}

