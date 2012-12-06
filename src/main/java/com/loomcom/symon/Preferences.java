package com.loomcom.symon;

import javax.swing.*;

public interface Preferences {

    public static final int DEFAULT_PROGRAM_LOAD_ADDRESS = 0x0300;

    public static final int DEFAULT_BORDER_WIDTH = 10;

    public static final boolean DEFAULT_HALT_ON_BREAK = true;

    public JDialog getDialog();

    public int getProgramStartAddress();

    public int getBorderWidth();

    public boolean getHaltOnBreak();

    public void updateUi();
}
