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

/**
 * Represents a single terminal cell which contains a character, background
 * color and foreground color.
 * @author Graham Edgecombe
 */
public class TerminalCell {

	/**
	 * The character.
	 */
	private final char character;

	/**
	 * The background color.
	 */
	private final Color backgroundColor;

	/**
	 * The foreground color.
	 */
	private final Color foregroundColor;

	/**
	 * Creates a terminal cell with the specified character, background color
	 * and foreground color.
	 * @param character The character.
	 * @param backgroundColor The background color.
	 * @param foregroundColor The foreground color.
	 * @throws NullPointerException if the background or foreground color(s)
	 * are {@code null}.
	 */
	public TerminalCell(char character, Color backgroundColor, Color foregroundColor) {
		if (backgroundColor == null) {
			throw new NullPointerException("backgroundColor");
		}
		if (foregroundColor == null) {
			throw new NullPointerException("foregroundColor");
		}

		this.character = character;
		this.backgroundColor = backgroundColor;
		this.foregroundColor = foregroundColor;
	}

	/**
	 * Gets the character.
	 * @return The character.
	 */
	public char getCharacter() {
		return character;
	}

	/**
	 * Gets the background color.
	 * @return The background color.
	 */
	public Color getBackgroundColor() {
		return backgroundColor;
	}

	/**
	 * Gets the foreground color.
	 * @return The foreground color.
	 */
	public Color getForegroundColor() {
		return foregroundColor;
	}

}

