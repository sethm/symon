package com.loomcom.symon;

import java.io.*;

public class CommandParser {

  private BufferedReader in;
  private BufferedWriter out;
  private Simulator simulator;

  public CommandParser(InputStream i, OutputStream o, Simulator s) {
    this.in = new BufferedReader(new InputStreamReader(i));
    this.out = new BufferedWriter(new OutputStreamWriter(o));
    this.simulator = s;
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
    out.write("j6502> ");
    out.flush();
  }

  private String readLine() throws IOException {
    String line = in.readLine();
    if (line == null) { return null; }
    return line.trim();
  }

  private void writeLine(String line) throws IOException {
    out.write(line);
    out.newLine();
    out.flush();
  }

  /**
   * Returns true if the line is a quit.
   */
  private boolean shouldQuit(String line) {
    return (line == null || "q".equals(line.toLowerCase()));
  }

}