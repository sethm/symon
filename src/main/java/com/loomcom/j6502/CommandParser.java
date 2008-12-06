package com.loomcom.j6502;

import java.io.*;

public class CommandParser {
    
    private BufferedReader m_in;
    private BufferedWriter m_out;
    private Simulator m_simulator;
    
    public CommandParser(InputStream in, OutputStream out, Simulator s) {
	m_in  = new BufferedReader(new InputStreamReader(in));
	m_out = new BufferedWriter(new OutputStreamWriter(out));
    }

    public void run() {
	try {
	    String command = null;
	    greeting();
	    prompt();
	    while (!shouldQuit(command = readLine())) {
		dispatch(command);
		prompt();
	    }
	    writeLine("\n\nGoodbye!");
	} catch (IOException ex) {
	    System.err.println("Error: " + ex.toString());
	    System.exit(1);
	}
    }
    
    /**
     * Dispatch the command.
     */
    public void dispatch(String command) throws IOException {
	writeLine("You entered: " + command);
    }

    /*******************************************************************
     * Private
     *******************************************************************/

    private void greeting() throws IOException {
	writeLine("Welcome to the j6502 Simulator!");
    }

    private void prompt() throws IOException {
	m_out.write("j6502> ");
	m_out.flush();
    }

    private String readLine() throws IOException {
	String line = m_in.readLine();
	if (line == null) { return null; }
	return line.trim();
    }

    private void writeLine(String line) throws IOException {
	m_out.write(line);
	m_out.newLine();
	m_out.flush();
    }
    
    /**
     * Returns true if the line is a quit.
     */
    private boolean shouldQuit(String line) {
	return (line == null || "q".equals(line.toLowerCase()));
    }
    
}