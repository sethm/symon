/*
 * Copyright (c) 2016 Seth J. Morabito <web@loomcom.com>
 *                    Maik Merten <maikmerten@googlemail.com>
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

import com.loomcom.symon.CpuState;
import com.loomcom.symon.util.FifoRingBuffer;

import javax.swing.*;
import java.awt.*;

/**
 * This frame displays a trace of CPU execution. The most recent <code>TRACE_LENGTH</code> lines
 * are captured in a buffer and rendered to the JFrame's main text area upon request.
 */
public class TraceLog extends JFrame {

    private final FifoRingBuffer<CpuState> traceLog;
    private final JTextArea                    traceLogTextArea;

    private static final Dimension MIN_SIZE       = new Dimension(320, 200);
    private static final Dimension PREFERRED_SIZE = new Dimension(640, 480);
    private static final int       MAX_LOG_LENGTH = 50000;

    public TraceLog() {
        traceLog = new FifoRingBuffer<>(MAX_LOG_LENGTH);
        setMinimumSize(MIN_SIZE);
        setPreferredSize(PREFERRED_SIZE);
        setResizable(true);
        setTitle("Trace Log");

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        traceLogTextArea = new JTextArea();
        traceLogTextArea.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        traceLogTextArea.setEditable(false);

        JScrollPane scrollableView = new JScrollPane(traceLogTextArea);

        getContentPane().add(scrollableView);
        pack();
    }

    /**
     * Redraw the display with the most recent <code>MAX_LOG_LENGTH</code>
     * trace events. <strong>CAUTION</strong>: This can be a very expensive
     * call.
     */
    public void refresh() {
        StringBuilder logString = new StringBuilder();        
        
        synchronized(traceLog) {
            for (CpuState state : traceLog) {
                logString.append(state.toTraceEvent());
            }
        }

        synchronized(traceLogTextArea) {
            traceLogTextArea.setText(logString.toString());
        }
    }

    /**
     * Reset the log area.
     */
    public void reset() {
        synchronized(traceLog) {
            traceLog.reset();
        }
        synchronized(traceLogTextArea) {
            traceLogTextArea.setText("");
            traceLogTextArea.setEnabled(true);
        }
    }

    /**
     * Append a CPU State to the trace log.
     *
     * @param state The CPU State to append.
     */
    public void append(CpuState state) {
        synchronized(traceLog) {
            traceLog.push(new CpuState(state));
        }
    }

    public void simulatorDidStart() {
        traceLogTextArea.setEnabled(false);
    }

    public void simulatorDidStop() {
        traceLogTextArea.setEnabled(true);
    }

    public boolean shouldUpdate() {
        return isVisible() && traceLogTextArea.isEnabled();
    }

}
