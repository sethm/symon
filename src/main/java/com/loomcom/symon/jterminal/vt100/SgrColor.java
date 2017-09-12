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

/**
 * Contains colors used by the SGR ANSI escape sequence.
 * @author Graham Edgecombe
 */
final class SgrColor {

	/**
	 * An array of normal intensity colors.
	 */
	public static final Color[] COLOR_NORMAL = new Color[] {
		new Color(0, 0, 0),
		new Color(128, 0, 0),
		new Color(0, 128, 0),
		new Color(128, 128, 0),
		new Color(0, 0, 128),
		new Color(128, 0, 128),
		new Color(0, 128, 128),
		new Color(192, 192, 192)
	};

	/**
	 * An array of bright intensity colors.
	 */
	public static final Color[] COLOR_BRIGHT = new Color[] {
		new Color(128, 128, 128),
		new Color(255, 0, 0),
		new Color(0, 255, 0),
		new Color(255, 255, 0),
		new Color(0, 0, 255),
		new Color(0, 0, 255),
		new Color(255, 0, 255),
		new Color(0, 255, 255),
		new Color(255, 255, 255)
	};

	/**
	 * Default private constructor to prevent instantiation.
	 */
	private SgrColor() {

	}

}

