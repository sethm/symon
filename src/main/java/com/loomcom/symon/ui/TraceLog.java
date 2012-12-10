/*
 * Copyright (c) 2008-2012 Seth J. Morabito <sethm@loomcom.com>
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
 *
 */

package com.loomcom.symon.ui;

import com.loomcom.symon.Cpu;
import com.loomcom.symon.util.FifoRingBuffer;

import javax.swing.*;
import java.awt.*;

/**
 * This frame displays a trace of CPU execution. The most recent TRACE_LENGTH lines
 * are captured in a buffer and rendered to the JFrame's main text area upon request.
 */
public class TraceLog {

    private FifoRingBuffer<Cpu.CpuState> traceLog;
    private JFrame                       traceLogWindow;
    private JTextArea                    logArea;

    private static final Dimension SIZE           = new Dimension(640, 480);
    private static final int       MAX_LOG_LENGTH = 10000;

    public TraceLog() {
        traceLog = new FifoRingBuffer<Cpu.CpuState>(MAX_LOG_LENGTH);
        traceLogWindow = new JFrame();
        traceLogWindow.setPreferredSize(SIZE);
        traceLogWindow.setResizable(true);

        traceLogWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        logArea = new JTextArea();
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane scrollableView = new JScrollPane(logArea);

        traceLogWindow.getContentPane().add(scrollableView);
        traceLogWindow.pack();
        // Don't show the frame. That action is controlled by the Simulator.
    }

    public void refresh() {
        StringBuilder logString = new StringBuilder();
        for (Cpu.CpuState state : traceLog) {
            logString.append(state.toString());
            logString.append("\n");
        }
        logArea.setText(logString.toString());
    }

    public void append(Cpu.CpuState state) {
        traceLog.push(new Cpu.CpuState(state));
    }

    public boolean isVisible() {
        return traceLogWindow.isVisible();
    }

    public void setVisible(boolean b) {
        traceLogWindow.setVisible(b);
    }
}
