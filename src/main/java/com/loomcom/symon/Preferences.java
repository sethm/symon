package com.loomcom.symon;

import javax.swing.*;

public interface Preferences {

    public static final int DEFAULT_PROGRAM_LOAD_ADDRESS = 0x0300;

    public static final int DEFAULT_ACIA_ADDRESS = 0xc000;

    public static final int DEFAULT_BORDER_WIDTH = 10;

    public JDialog getDialog();

    public int getProgramStartAddress();

    public int getAciaAddress();

    public int getBorderWidth();

    public void updateUi();
}
