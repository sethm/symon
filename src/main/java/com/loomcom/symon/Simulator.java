package com.loomcom.symon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.StringTokenizer;

import com.loomcom.symon.devices.*;
import com.loomcom.symon.exceptions.*;

/**
 * Main control class for the J6502 Simulator.
 */
public class Simulator {

  /**
   * The CPU itself.
   */
  private Cpu cpu;

  /**
   * The Bus responsible for routing memory read/write requests to the
   * correct IO devices.
   */
  private Bus bus;
  
  private BufferedReader in;
  private BufferedWriter out;
  
  /* If true, trace execution of the CPU */
  private boolean trace = false;

  public Simulator() throws MemoryRangeException {
    cpu = new Cpu();
    bus = new Bus(0x0000, 0xffff);
    bus.addCpu(cpu);
    bus.addDevice(new Memory(0x0000, 0x10000));
    this.in = new BufferedReader(new InputStreamReader(System.in));
    this.out = new BufferedWriter(new OutputStreamWriter(System.out));
  }

  public void run() throws MemoryAccessException {
    try {
      greeting();
      prompt();
      String command = null;
      while (!shouldQuit(command = readLine())) {
        try {
          dispatch(command);
        } catch (CommandFormatException ex) {
          writeLine(ex.getMessage());
        }
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
  public void dispatch(String commandLine)
      throws MemoryAccessException, IOException, CommandFormatException {
    Command c = new Command(commandLine);
    String cmd = c.getCommand();
    if (cmd != null) {
      if ("test".equals(cmd)) {
        doTest();
      } else if (cmd.startsWith("s")) {
        doGetState();
      } else if (cmd.startsWith("r")) { 
        doReset();
      } else if (cmd.startsWith("e")) {
        doExamine(c);
      } else if (cmd.startsWith("d")) {
        doDeposit(c);
      } else if (cmd.startsWith("g")) {
        doGo(c);
      } else if (cmd.startsWith("h")) {
        doHelp(c);
      } else if (cmd.startsWith("t")) {
        doToggleTrace();
      } else if (cmd.startsWith("f")) {
        doFill(c);
      } else {
        writeLine("? Type h for help");
      }
    }
  }
  
  public void doHelp(Command c) throws IOException {
    writeLine("Symon 6502 Simulator");
    writeLine("");
    writeLine("All addresses must be in hexadecimal. Commands may be short or");
    writeLine("long (e.g. 'e' or 'ex' or 'examine'). Note that 'go' clears the");
    writeLine("Break processor status flag");
    writeLine("");
    writeLine("g [address  [steps]]       Start running at address.");
    writeLine("e [start    [end]]         Examine memory.");
    writeLine("d [address] [data]         Deposit data into address.");
    writeLine("f [start]   [end] [data]   Fill memory with data.");
    writeLine("r                          Reset simulator.");
    writeLine("s                          Show CPU state.");
    writeLine("t                          Toggle trace.");
    writeLine("q (or Control-D)           Quit.");
  }

  public void doGetState() throws IOException, MemoryAccessException {
    writeLine(cpu.toString());
    writeLine("Trace is " + (trace ? "on" : "off"));
  }

  public void doExamine(Command c) throws IOException, MemoryAccessException, CommandFormatException {
    try {
      if (c.numArgs() == 2) {
        int startAddress = stringToWord(c.getArgs()[0]);
        int endAddress   = stringToWord(c.getArgs()[1]);
        while (startAddress < endAddress) {
          StringBuffer line = new StringBuffer();
          int numBytes = 0;
          line.append(String.format("%04x  ", startAddress));
          while (numBytes++ < 8 && startAddress <= endAddress) {
            line.append(String.format("%02x ", bus.read(startAddress++)));
          }
          writeLine(line.toString());
        }
      } else if (c.numArgs() == 1) {
        int address = stringToWord(c.getArgs()[0]);
        writeLine(String.format("%04x  %02x", address, bus.read(address)));
      } else {
        throw new CommandFormatException("e [start [end]]");        
      }
    } catch (NumberFormatException ex) {
      throw new CommandFormatException("Address not understood");
    }
  }
  
  public void doDeposit(Command c) throws MemoryAccessException, CommandFormatException {
    if (c.numArgs() != 2) {
      throw new CommandFormatException("d [address] [data]");
    }
    try {
      int address = stringToWord(c.getArg(0));
      int data    = stringToByte(c.getArg(1));
      bus.write(address, data);
    } catch (NumberFormatException ex) {
      throw new CommandFormatException("Address not understood");
    }
  }
  
  public void doFill(Command c) throws MemoryAccessException, CommandFormatException {
    if (c.numArgs() != 3) {
      throw new CommandFormatException("f [start] [end] [data]");
    }
    try {
      int start = stringToWord(c.getArg(0));
      int end = stringToWord(c.getArg(1));
      int data = stringToByte(c.getArg(2));
      while (start < end) {
        bus.write(start, data);
        start++;
      }
    } catch (NumberFormatException ex) {
      throw new CommandFormatException("Address not understood");
    }
  }
  
  public void doGo(Command c) throws IOException, MemoryAccessException, CommandFormatException {
    if (c.numArgs() != 1 && c.numArgs() != 2) {
      throw new CommandFormatException("g [address [steps]]");
    }
    try {
      int start = stringToWord(c.getArg(0));
      int steps = -1;
      if (c.numArgs() == 2) {
        steps = stringToWord(c.getArg(1));
      }
      
      // Make a gross assumption: Restarting the CPU clears
      // the break flag and the IRQ disable flag.
      cpu.clearBreakFlag();
      cpu.clearIrqDisableFlag();
      
      cpu.setProgramCounter(start);
      while (!cpu.getBreakFlag() && (steps == -1 || steps-- > 0)) {
        cpu.step();
        if (trace) {
          writeLine(cpu.toString());
        }
      }
      if (!trace) {
        writeLine(cpu.toString());
      }
    } catch (NumberFormatException ex) {
      throw new CommandFormatException("Address not understood");
    }
  }
  
  public void doToggleTrace() throws IOException {
    this.trace = !trace;
    writeLine("Trace is now " + (trace ? "on" : "off"));
  }
  
  public void doReset() throws MemoryAccessException {
    cpu.reset();
    this.trace = false;
  }
  
  /**
   * Run a very simple test program that doesn't do
   * much other than exercise the accumulator a bit.
   */
  public void doTest() throws MemoryAccessException {
    int[] zpData = {
      0x39,             // $0000
      0x21,             // $0001
      0x12              // $0002
    };
    int[] data = {
      0xae,             // $c800
      0x13,             // $c801
      0x29              // $c802
    };
    int[] program = {
      0xa9, 0xff,       // LDA #$FF
      0xa0, 0x1a,       // LDY #$1A
      0xa2, 0x90,       // LDX #$90
      0xa2, 0x02,       // LDX #$02
      0x49, 0xff,       // EOR #$FF
      0xa9, 0x00,       // LDA #$00
      0xa2, 0x00,       // LDX #$00
      0x29, 0xff,       // AND #$FF
      0xa0, 0x00,       // LDY #$00
      0xa5, 0x00,       // LDA $00
      0xad, 0x00, 0xc8, // LDA $c800
      0x4c, 0x00, 0x03  // JMP #$0300
    };
    int programLength = 12;

    load(0x0000, zpData);
    load(0x0300, program);
    load(0xc800, data);
    cpu.setResetVector(0x0300);
    cpu.reset();

    for (int i = 0; i <= programLength; i++) {
      cpu.step();
      System.out.println(cpu.toString());
    }
  }

  /**
   * Main simulator routine.
   */
  public static void main(String[] args) throws MemoryAccessException {
    try {
      new Simulator().run();
    } catch (MemoryRangeException ex) {
      System.err.println("Error: " + ex.toString());
    }
  }
  
  /*******************************************************************
   * Private
   *******************************************************************/

  public void load(int address, int[] data)
    throws MemoryAccessException {
    int i = 0;
    for (int d : data) {
      bus.write(address + i++, d);
    }
  }
  
  private int stringToWord(String addrString) {
    return Integer.parseInt(addrString, 16) & 0xffff;
  }
  
  private int stringToByte(String dataString) {
    return Integer.parseInt(dataString, 16) & 0xff;
  }
  
  private void greeting() throws IOException {
    writeLine("Welcome to the Symon Simulator!");
  }

  private void prompt() throws IOException {
    out.write("symon> ");
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
  
  /**
   * Command line tokenizer class. Given a command line, tokenize
   * it and give easy access to the command and its arguments.
   */
  public static final class Command {
    private String command;
    private String[] args;

    public Command(String commandLine) {
      StringTokenizer st = new StringTokenizer(commandLine);
      int numTokens = st.countTokens();
      int idx = 0;
      args = new String[numTokens > 1 ? numTokens - 1 : 0];
      while (st.hasMoreTokens()) {
        if (command == null) {
          command = st.nextToken();
        } else {
          args[idx++] = st.nextToken();
        }
      }
    }
    
    public String getCommand() {
      return command;
    }
    
    public String[] getArgs() {
      return args;
    }
    
    public String getArg(int argNum) {
      if (argNum > args.length - 1) {
        return null;
      } else {
        return args[argNum];
      }
    }
    
    public int numArgs() {
      return args.length;
    }
    
    public boolean hasArgs() {
      return args.length > 0;
    } 
  }
}
