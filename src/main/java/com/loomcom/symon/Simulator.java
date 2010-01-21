package com.loomcom.symon;

import java.io.*;
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

  /**
   * The ACIA, used for charater in/out.
   *
   * By default, the simulator uses base address c000 for the ACIA.
   */
  private Acia acia;

  private BufferedReader in;
  private BufferedWriter out;

  /* If true, trace execution of the CPU */
  private boolean trace = false;
  private int nextExamineAddress = 0;

  private static final int BUS_BOTTOM  = 0x0000;
  private static final int BUS_TOP     = 0xffff;

  private static final int ACIA_BASE   = 0xc000;

  private static final int MEMORY_BASE = 0x0000;
  private static final int MEMORY_SIZE = 0xc000; // 48 KB

  private static final int ROM_BASE    = 0xe000;
  private static final int ROM_SIZE    = 0x2000; // 8 KB

  public Simulator() throws MemoryRangeException {
    this.bus  = new Bus(BUS_BOTTOM, BUS_TOP);
    this.cpu  = new Cpu();
    this.acia = new Acia(ACIA_BASE);

    bus.addCpu(cpu);
    bus.addDevice(new Memory(MEMORY_BASE, MEMORY_SIZE, false));
    bus.addDevice(acia);
    // TODO: This should be read-only memory. Add a method
    // to allow one-time initialization of ROM with a loaded
    // ROM binary file.
    bus.addDevice(new Memory(ROM_BASE, ROM_SIZE, false));

    this.in = new BufferedReader(new InputStreamReader(System.in));
    this.out = new BufferedWriter(new OutputStreamWriter(System.out));
  }

  public void run() throws MemoryAccessException, FifoUnderrunException {
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
      writeLine("\nGoodbye!");
    } catch (IOException ex) {
      System.err.println("Error: " + ex.toString());
      System.exit(1);
    }
  }

  /**
   * Dispatch the command.
   */
  public void dispatch(String commandLine) throws MemoryAccessException,
                                                  IOException,
                                                  CommandFormatException,
                                                  FifoUnderrunException {
    Command c = new Command(commandLine);
    String cmd = c.getCommand();
    if (cmd != null) {
      if (cmd.startsWith("ste")) {
        doStep(c);
      } else if (cmd.startsWith("sta")) {
        doGetState();
      } else if (cmd.startsWith("se")) {
        doSet(c);
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
      } else if (cmd.startsWith("l")) {
        doLoad(c);
      } else {
        writeLine("? Type h for help");
      }
    }
  }

  public void doHelp(Command c) throws IOException {
    writeLine("Symon 6502 Simulator");
    writeLine("");
    writeLine("All addresses must be in hexadecimal.");
    writeLine("Commands may be short or long (e.g. 'e' or 'ex' or 'examine').");
    writeLine("Note that 'go' clears the BREAK processor status flag.");
    writeLine("");
    writeLine("h                       Show this help file.");
    writeLine("e [start] [end]         Examine memory at PC, start, or start-end.");
    writeLine("d <address> <data>      Deposit data into address.");
    writeLine("f <start> <end> <data>  Fill memory with data.");
    writeLine("set {pc,a,x,y} [data]   Set register to data value.");
    writeLine("load <file> <address>   Load binary file at address.");
    writeLine("g [address] [steps]     Start running at address, or at PC.");
    writeLine("step [address]          Step once, optionally starting at address.");
    writeLine("stat                    Show CPU state.");
    writeLine("reset                   Reset simulator.");
    writeLine("trace                   Toggle trace.");
    writeLine("q (or Control-D)        Quit.\n");
  }

  public void doGetState() throws IOException, MemoryAccessException {
    writeLine(cpu.toString());
    writeLine("Trace is " + (trace ? "on" : "off"));
  }

  public void doLoad(Command c) throws IOException,
                                       MemoryAccessException,
                                       CommandFormatException {
    if (c.numArgs() != 2) {
      throw new CommandFormatException("load <file> <address>");
    }

    File binFile    = new File(c.getArg(0));
    int address     = stringToWord(c.getArg(1));

    if (!binFile.exists()) {
      throw new CommandFormatException("File '" + binFile +
                                       "' does not exist.");
    }
    writeLine("Loading file '" + binFile + "' at address " +
              String.format("%04x", address) + "...");

    int bytesLoaded = 0;

    FileInputStream fis = new FileInputStream(binFile);

    try {
      int b = 0;
      while ((b = fis.read()) > -1 && address <= bus.endAddress()) {
        bus.write(address++, b);
        bytesLoaded++;
      }
    } finally {
      fis.close();
    }

    writeLine("Loaded " + bytesLoaded + " (" +
              String.format("$%04x", bytesLoaded) + ") bytes");
  }

  public void doSet(Command c) throws MemoryAccessException,
                                      CommandFormatException {
    if (c.numArgs() != 2) {
      throw new CommandFormatException("set {a, x, y, pc} <value>");
    }
    try {
      String reg = c.getArg(0).toLowerCase();
      String data = c.getArg(1);
      if ("a".equals(reg)) {
        cpu.setAccumulator(stringToByte(data));
      } else if ("x".equals(reg)) {
        cpu.setXRegister(stringToByte(data));
      } else if ("y".equals(reg)) {
        cpu.setYRegister(stringToByte(data));
      } else if ("pc".equals(reg)) {
        cpu.setProgramCounter(stringToWord(data));
      } else {
        throw new CommandFormatException("set {a, x, y, pc} <value>");
      }
    } catch (NumberFormatException ex) {
      throw new CommandFormatException("Illegal address");
    }
  }

  public void doExamine(Command c) throws IOException,
                                          MemoryAccessException,
                                          CommandFormatException {
    try {
      if (c.numArgs() == 2) {
        int startAddress = stringToWord(c.getArgs()[0]);
        int endAddress   = stringToWord(c.getArgs()[1]);
        while (startAddress < endAddress) {
          StringBuffer line = new StringBuffer();
          int numBytes = 0;
          line.append(String.format("%04x  ", startAddress));
          while (numBytes++ < 16 && startAddress <= endAddress) {
            line.append(String.format("%02x ", bus.read(startAddress++)));
            if (numBytes % 8 == 0) {
              line.append(" ");
            }
          }
          writeLine(line.toString());
        }
        nextExamineAddress = endAddress + 1;
      } else if (c.numArgs() == 1) {
        int address = stringToWord(c.getArgs()[0]);
        writeLine(String.format("%04x  %02x", address, bus.read(address)));
        nextExamineAddress = address + 1;
      } else if (c.numArgs() == 0) {
        writeLine(String.format("%04x  %02x", nextExamineAddress,
                                bus.read(nextExamineAddress)));
        nextExamineAddress++;
      } else {
        throw new CommandFormatException("e [start [end]]");
      }
    } catch (NumberFormatException ex) {
      throw new CommandFormatException("Illegal Address");
    }
  }

  public void doDeposit(Command c) throws MemoryAccessException,
                                          CommandFormatException {
    if (c.numArgs() != 2) {
      throw new CommandFormatException("d [address] [data]");
    }
    try {
      int address = stringToWord(c.getArg(0));
      int data    = stringToByte(c.getArg(1));
      bus.write(address, data);
    } catch (NumberFormatException ex) {
      throw new CommandFormatException("Illegal Address");
    }
  }

  public void doFill(Command c) throws MemoryAccessException,
                                       CommandFormatException {
    if (c.numArgs() != 3) {
      throw new CommandFormatException("f [start] [end] [data]");
    }
    try {
      int start = stringToWord(c.getArg(0));
      int end = stringToWord(c.getArg(1));
      int data = stringToByte(c.getArg(2));
      while (start <= end) {
        bus.write(start, data);
        start++;
      }
    } catch (NumberFormatException ex) {
      throw new CommandFormatException("Illegal Address");
    }
  }

  public void doStep(Command c) throws IOException,
                                       MemoryAccessException,
                                       FifoUnderrunException,
                                       CommandFormatException {
    try {
      if (c.numArgs() > 0) {
        cpu.setProgramCounter(stringToWord(c.getArg(1)));
      }
      cpu.step();
      writeLine(cpu.toString());  // Always show status after stepping
    } catch (NumberFormatException ex) {
      throw new CommandFormatException("Illegal Address");
    }
  }

  public void doGo(Command c) throws IOException,
                                     MemoryAccessException,
                                     FifoUnderrunException,
                                     CommandFormatException {
    int readChar;
    int stepCount = 0;

    if (c.numArgs() > 2) {
      throw new CommandFormatException("g [address] [steps]");
    }
    try {
      int start = 0;
      int steps = -1;

      if (c.numArgs() > 0) {
        start = stringToWord(c.getArg(0));
      } else {
        start = cpu.getProgramCounter();
      }
      if (c.numArgs() == 2) {
        steps = stringToWord(c.getArg(1));
      }

      // Make a gross assumption: Restarting the CPU clears
      // the break flag and the IRQ disable flag.
      cpu.clearBreakFlag();
      cpu.clearIrqDisableFlag();

      cpu.setProgramCounter(start);
      outer:
      while (!cpu.getBreakFlag() && (steps == -1 || steps-- > 0)) {
        cpu.step();
        if (trace) {
          writeLine(cpu.toString());
        }
        // Wake up and scan keyboard every 500 steps
        if (stepCount++ >= 500) {
          // Reset step count
          stepCount = 0;

          //
          // Do output if available.
          //
          while (acia.hasTxChar()) {
            out.write(acia.txRead());
            out.flush();
          }

          //
          // Consume input if available.
          //
          // NOTE: On UNIX systems, System.in.available() returns 0
          // until Enter is pressed. So to interrupt we must ALWAYS
          // type "^E<enter>". Sucks hard. But such is life.
          if (System.in.available() > 0) {
            while ((readChar = in.read()) > -1) {
              // Keep consuming unless ^E is found.
              //
              // TODO: This will probably lead to a lot of spurious keyboard
              // entry. Gotta keep an eye on that.
              //
              if (readChar == 0x05) {
                break outer;
              } else {
                // Buffer keyboard input into the simulated ACIA's
                // read buffer.
                acia.rxWrite(readChar);
              }
            }
          }

        }
      }
      if (!trace) {
        writeLine(cpu.toString());
      }
    } catch (NumberFormatException ex) {
      throw new CommandFormatException("Illegal Address");
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
   * Main simulator routine.
   */
  public static void main(String[] args) throws MemoryAccessException,
                                                FifoUnderrunException {
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
    writeLine("Welcome to the Symon 6502 Simulator. Type 'h' for help.");
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
