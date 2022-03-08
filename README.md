SYMON - A 6502 System Simulator
===============================

**Version:** 1.3.2

**Last Updated:** 08 March, 2021

See the file COPYING for license.

![Symon Simulator in Action](https://github.com/sethm/symon/raw/master/screenshots/full.jpg)

## 1.0 About

Symon is a general purpose simulator for systems based on the MOS
Technologies 6502 microprocessor and compatibles. Symon is implemented
in Java. Its core goals are accuracy, ease of development, clear
documentation, and extensive test suites for validating correctness.

Symon simulates a complete system with a 1 MHz NMOS 6502 or CMOS
65C02, 32KB of RAM, 16KB of ROM, a MOS 6551 or Motorola 6850 ACIA, a
MOS 6522 VIA, and an experimental 6545 CRTC.

Symon has extensive unit tests to verify correctness, and fully passes
Klaus Dormann's 6502 Functional Test Suite as of version 0.8.2
(See [this thread on the 6502.org Forums](http://forum.6502.org/viewtopic.php?f=2&t=2241)
for more information about this functional test suite).

Symon is under constant, active development. Feedback and patches
are always welcome.

## 2.0 Requirements

  - Java 1.8 or higher
  - Maven 2.0.x or higher (for building from source)
  - JUnit 4 or higher (for testing)

## 3.0 Features

Symon can simulate multiple 6502 based architectures. At present, three
machines are implemented: Symon (the default), MULTICOMP, and a "Simple"
machine useful for debugging.

### 3.1 Memory Maps

#### 3.1.1 Symon Memory Map

  - `$0000`--`$7FFF`: 32KB RAM
  - `$8000`--`$800F`: 6522 VIA
  - `$8800`--`$8803`: MOS 6551 ACIA (Serial Console)
  - `$9000`--`$9001`: MOS 6545 CRTC
  - `$C000`--`$FFFF`: 16KB ROM

The CRT Controller uses memory address `$7000` as the start of Video
memory.

#### 3.1.2 MULTICOMP Memory Map

  - `$0000`--`$DFFF`: 56KB RAM
  - `$E000`--`$FFFF`: 8KB ROM
  - `$FFD0`--`$FFD1`: Motorola 6850 ACIA
  - `$FFD8`--`$FFDF`: Controller for SD cards

### 3.1.3 Simple Memory Map

  - `$0000`--`$FFFF`: 64KB RAM

### 3.2 Serial Console and CPU Status

![Serial Console](https://github.com/sethm/symon/raw/master/screenshots/console.png)

The main window of the simulator acts as the primary Input/Output
system through a virtual serial terminal. The terminal is attached to
a simulated ACIA, including a programmable baud rate generator that
tries to approximate the correct "feel" of the programmed baud rate.
(The sample Enhanced BASIC ROM image is programmed for 9600 baud)

It also provides CPU status. Contents of the accumulator, index
registers, processor status flags, disassembly of the instruction
register, and stack pointer are all displayed.

![Font Selection](https://github.com/sethm/symon/raw/master/screenshots/font_selection.png)

The console supports font sizes from 10 to 20 points.

### 3.3 ROM Loading

![ROM Loading](https://github.com/sethm/symon/raw/master/screenshots/load_rom.png)

Symon can load any appropriately sized ROM image. The Symon
architecture expects as 16KB (16384 byte) ROM image, while the
MULTICOMP architecture expects an 8KB (8192 byte) ROM image. Images
are loaded via the "Load ROM..." action in the "File" menu. The
selected ROM file will be loaded into memory at the correct ROM base
address.

### 3.4 Memory Window

![Memory Window](https://github.com/sethm/symon/raw/master/screenshots/memory_window.png)

Memory contents can be viewed (and edited) one page at a time through the Memory Window.

### 3.5 Trace Log

![Trace Log](https://github.com/sethm/symon/raw/master/screenshots/trace_log.png)

The last 20,000 execution steps are disassembled and logged to the Trace Log
Window.

### 3.6 Simulator Speeds

![Speeds](https://github.com/sethm/symon/raw/master/screenshots/simulator_menu.png)

Simulated speeds may be set from 1MHz to 8MHz.

### 3.7 Breakpoints

![Breakpoints](https://github.com/sethm/symon/raw/master/screenshots/breakpoints.png)

Breakpoints can be set and removed through the Breakpoints window.

### 3.8 Experimental 6545 CRTC Video

![Composite Video](https://github.com/sethm/symon/raw/master/screenshots/video_window.png)

This feature is highly experimental. It's possible to open a video window
from the "View" menu.  This window simulates the output of a MOS 6545 CRT
Controller located at address `$9000` and `$9001`.

By default, the 40 x 25 character display uses video memory located at base
address `$7000`.  This means that the memory from address `$7000` (28672
decimal) to `$73E8` (29672 decimal) is directly mapped to video.

  - Address Register (at address `$9000`)
  - R1: Horizontal Displayed Columns
  - R6: Vertical Displayed Rows
  - R9: Scan Lines per Row
  - R10: Cursor Start Scan Line and Cursor Control Mode
  - R11: Cursor End Scan Line
  - R12: Display Start Address (High Byte)
  - R13: Display Start Address (Low Byte)
  - R14: Cursor Position (High Byte)
  - R15: Cursor Position (Low Byte)

Although the simulation is pretty good, there are a few key differences
between the simulated 6545 and a real 6545:

  - The simulated 6545 supports only the straight binary addressing
    mode of the real 6545, and not the Row/Column addressing mode.

  - The simulated 6545 has full 16 bit addressing, where the real 6545
    has only a 14-bit address bus.

  - The simulation is done at a whole-frame level, meaning that lots
    of 6545 programming tricks that were achieved by updating the
    frame address during vertical and horizontal sync times are not
    achievable.  There is no way (for example) to change the Display Start
    Address (R12 and R13) while a frame is being drawn.

For more information on the 6545 CRTC and its programming model, please see the following resources

  - [CRTC 6545/6845 Information (André Fachat)](http://6502.org/users/andre/hwinfo/crtc/index.html)
  - [CRTC Operation (André Fachat)](http://www.6502.org/users/andre/hwinfo/crtc/crtc.html)
  - [MOS 6545 Datasheet (PDF)](http://www.6502.org/users/andre/hwinfo/crtc/crtc.html)


#### 3.8.1 Example BASIC Program to test Video

This program will fill the video screen with all printable characters.

    10 J = 0
    20 FOR I = 28672 TO 29672
    30 POKE I,J
    40 IF J < 255 THEN J = J + 1 ELSE J = 0
    50 NEXT I
    60 END

## 4.0 Usage

### 4.1 Building

To build Symon with Apache Maven, just type:

    $ mvn package

Maven will build Symon, run unit tests, and produce a jar file in the
`target` directory containing the compiled simulator.

Symon is meant to be invoked directly from the jar file. To run with
Java 1.8 or greater, just type:

    $ java -jar symon-1.2.0.jar

When Symon is running, you should be presented with a simple graphical
interface.

#### 4.1.1 Command Line Options

Two command line options may be passed to the JAR file on startup,
to specify machine type and CPU type. The options are:

  - `-cpu 6502`: Use the NMOS 6502 CPU type by default.
  - `-cpu 65c02`: Use the CMOS 65C02 CPU type by default.
  - `-machine symon`: Use the **Symon** machine type by default.
  - `-machine multicomp`: Use the **Multicomp** machine type by default.
  - `-machine simple`: Use the **Simple** machine type by default.
  - `-rom <file>`: Use the specified file as the ROM image.

### 4.2 ROM images

The simulator requires a ROM image loaded into memory to work
properly. Without a ROM in memory, the simulator will not be able to
reset, since the reset vector for the 6502 is located in the ROM
address space.

By default, any file named `rom.bin` that exists in the same directory
where Symon is launched will be loaded as a ROM image. ROM images can
also be swapped out at run-time with the "Load ROM Image..." in the
File menu.

The "samples" directory contains a ROM image for the Symon
architecture named 'ehbasic.rom', containing Lee Davison's Enhanced
6502 BASIC. This serves as a good starting point for exploration.

*Note*: Presently, EhBASIC only works with the Symon machine
 architecture, not with MULTICOMP.

### 4.3 Loading A Program

In addition to ROM images, programs in the form of raw binary object files can
be loaded directly into memory from "Load Program..." in the File menu.

Programs are loaded starting at addres $0300.  After loading the program, the
simulated CPU's reset vector is loaded with the values $00, $03, and the CPU is
reset.

There are two very simple sample program in the "samples" directory,
for testing.

- 'echo.prg' will echo back anything typed at the console.

- 'hello.prg' will continuously print "Hello, 6502 World!" to the console.

### 4.4 Running

After loading a program or ROM image, clicking "Run" will start the simulator
running.

## 5.0 Revision History

  - **1.3.1:** 12 October, 2019 - Add support for new command line
    option `-cpu <type>` to specify one of `6502` or `65c02` on startup,
    and new option `-rom <file>` to specify a ROM file to load.

  - **1.3.0:** 24 February, 2018 - Adds support for 65C02 opcodes.

  - **1.2.1:** 8 January, 2016 - Remove dependency on Java 8. Now
    supports compiling and running under Java 1.7.

  - **1.2.0:** 3 January, 2016 - Add symbolic disassembly to breakpoints
    window.

  - **1.1.1:** 2 January, 2016 - Minor enhancement: Allows breakpoints
    to be added with the Enter key.

  - **1.1.0:** 31 December, 2015 - Fixed delay loop to better
    simulate various clock speeds. Added ability to select clock
    speed at runtime. Status display now shows the next instruction
    to be executed, instead of the last instruction executed.
    Added support for breakpoints.

  - **1.0.0:** 10 August, 2014 - Added "Simple" machine
    implementation, pure RAM with no IO. Added Klaus Dormann's
    6502 Functional Tests for further machine verification (these
    tests must be run in the "Simple" machine).

  - **0.9.9.1:** 27 July, 2014 - Pressing 'Control' while clicking
    'Reset' now performs a memory clear.

  - **0.9.9:** 26 July, 2014 - MULTICOMP and multi-machine support
    contributed by Maik Merten &lt;maikmerten@googlemail.com&gt;

  - **0.9.1:** 26 January, 2014 - Support for IRQ and NMI handling.

  - **0.9.0:** 29 December, 2013 - First pass at a 6545 CRTC simulation.

  - **0.8.5:** 30 March, 2013 - ASCII display for memory window.
    Allows user to select a step count from a drop-down box.

  - **0.8.4:** 4 March, 2013 - Fix for ZPX, ZPY display in the trace log
    (change contributed by jsissom)

  - **0.8.3:** 12 January, 2013 - Added tool-tip text. Memory is no longer
    cleared when resetting. Fixed swapped register labels.

  - **0.8.2:** 01 January, 2013 - Fully passes Klaus Dormann's 6502 Functional Test suite!

  - **0.8.1:** 30 December, 2012

  - **0.8.0:** 29 December, 2012

  - **0.7.0:** 9 December, 2012

  - **0.6:** 5 November, 2012

  - **0.5:** 21 October, 2012 - Able to run Enhanced BASIC for the first time.

  - **0.3:** 14 October, 2012

  - **0.2:** 22 April, 2012

  - **0.1:** 20 January, 2010

## 6.0 Roadmap

  - **1.2:** Better breakpoint support. Symbolic debugging of breakpoints.

  - **2.0:** Complete rewrite of the UI in JavaFX instead of Swing. Complete
    assembler and disassembler built in. Ability to attach source code for
    symbolic debugging.

## 7.0 To Do

  - Feedback (in the form of dialogs, status bar, etc).

  - Better debugging tools from the UI, including breakpoints
    and disassembly.

  - UI needs a ton more polish.

  - More extensive testing.

  - Clean up JavaDoc.

  - Implement CMOS 65C02 instructions and NMOS / CMOS mode flag.

  - Allow displaying ACIA status and dumping ACIA buffers, for
    debugging.

  - CRTC emulation is very naive. The whole frame is drawn in one
    CPU step. This should be improved by drawing scan lines during
    machine steps to approximate real NTSC/PAL refresh rates.

  - Symbolic debugging.

## 8.0 Copyright and Acknowledgements

**Copyright (c) 2014 Seth J. Morabito &lt;web@loomcom.com&gt;**

Portions Copyright (c) 2014 Maik Merten &lt;maikmerten@googlemail.com&gt;

Additional components used in this project are copyright their respective owners.

  - Enhanced 6502 BASIC Copyright (c) Lee Davison
  - 6502 Functional Tests Copyright (c) Klaus Dormann
  - JTerminal Copyright (c) Graham Edgecombe

This project would not have been possible without the following resources:

  - [Andrew Jacobs' 6502 Pages](http://obelisk.me.uk/6502/), for
    wonderfully detailed information about the 6502

  - [Neil Parker's "The 6502/65C02/65C816 Instruction Set Decoded"](http://www.llx.com/~nparker/a2/opcodes.html),
    for information about how instructions are coded

## 9.0 Licensing

Symon is free software.  It is distributed under the MIT License.
Please see the file 'COPYING' for full details of the license.
