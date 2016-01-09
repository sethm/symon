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


package com.loomcom.symon;

import com.loomcom.symon.machines.MulticompMachine;
import com.loomcom.symon.machines.SimpleMachine;
import com.loomcom.symon.machines.SymonMachine;
import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    
     /**
     * Main entry point to the simulator. Creates a simulator and shows the main
     * window.
     *
     * @param args Program arguments
     */
    public static void main(String args[]) throws Exception {
        
        Class machineClass = SymonMachine.class;
        for(int i = 0; i < args.length; ++i) {
            String arg = args[i].toLowerCase(Locale.ENGLISH);
            if(arg.equals("-machine") && (i+1) < args.length) {
                String machine = args[i+1].trim().toLowerCase(Locale.ENGLISH);
                switch (machine) {
                    case "symon":
                        machineClass = SymonMachine.class;
                        break;
                    case "multicomp":
                        machineClass = MulticompMachine.class;
                        break;
                    case "simple":
                        machineClass = SimpleMachine.class;
                        break;
                }
            }
        }
        
        while (true) {
            if (machineClass == null) {
                Object[] possibilities = {"Symon", "Multicomp", "Simple"};
                String s = (String)JOptionPane.showInputDialog(
                                null,
                                "Please choose the machine type to be emulated:",
                                "Machine selection",
                                JOptionPane.PLAIN_MESSAGE,
                                null,
                                possibilities,
                                "Symon");
                

                if (s != null && s.equals("Multicomp")) {
                    machineClass = MulticompMachine.class;
                } else if (s != null && s.equals("Simple")) {
                    machineClass = SimpleMachine.class;
                } else {
                    machineClass = SymonMachine.class;
                }
            }
        
            final Simulator simulator = new Simulator(machineClass);
        
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                        // Create the main UI window
                        simulator.createAndShowUi();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        
        
            Simulator.MainCommand cmd = simulator.waitForCommand();
            if (cmd.equals(Simulator.MainCommand.SELECTMACHINE)) {
                machineClass = null;
            } else {
                break;
            }
        }
    }
}
