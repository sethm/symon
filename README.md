SYMON - A 6502 System Simulator
===============================

**NOTE: THIS SOFTWARE IS UNDER ACTIVE DEVELOPMENT. Feedback is welcome!**

**Version:** 0.8.5

**Last Updated:** 30 March, 2013

Copyright (c) 2008-2013 Seth J. Morabito &lt;web@loomcom.com&gt;

See the file COPYING for license.


![Symon Simulator in Action] (https://github.com/sethm/symon/raw/master/screenshots/full.jpg)

## 1.0 About

Symon is a general purpose simulator for systems based on the MOS
Technologies 6502 microprocessor and compatibles. Symon is implemented
in Java. Its core goals are accuracy, ease of development, clear
documentation, and extensive test suites for validating correctness.

Symon simulates a complete system with a 1 MHz NMOS 6502, 32KB of RAM,
16KB of ROM, a 6551 ACIA, and a 6522 VIA.

Symon has extensive unit tests to verify correctness, and fully passes
Klaus Dormann's 6502 Functional Test Suite as of version 0.8.2
(See [this thread on the 6502.org Forums] (http://forum.6502.org/viewtopic.php?f=2&t=2241)
for more information about this functional test suite).

## 2.0 Requirements

  - Java 1.5 or higher
  - Maven 2.0.x or higher (for building from source)
  - JUnit 4 or higher (for testing)

## 3.0 Features

### 3.1 Memory Map

  - `$0000`--`$7FFF`: 32KB RAM
  - `$8000`--`$800F`: 6522 VIA
  - `$8800`--`$8803`: 6551 ACIA (Serial Console)
  - `$C000`--`$FFFF`: 16KB ROM

### 3.2 Serial Console and CPU Status

![Serial Console] (https://github.com/sethm/symon/raw/master/screenshots/console.png)

The main window of the simulator acts as the primary Input/Output
system through a virtual serial terminal. The terminal is attached to
a simulated MOS 6551 ACIA, including a programmable baud rate
generator that tries to approximate the correct "feel" of the
programmed baud rate. (The sample Enhanced BASIC ROM image is
programmed for 9600 baud)

It also provides CPU status. Contents of the accumulator, index
registers, processor status flags, disassembly of the instruction
register, and stack pointer are all displayed.

![Font Selection] (https://github.com/sethm/symon/raw/master/screenshots/font_selection.png)

The console supports font sizes from 10 to 20 points.

### 3.3 16 KB ROM Loading

![ROM Loading] (https://github.com/sethm/symon/raw/master/screenshots/load_rom.png)

Symon can load any 16 KB (16384 bytes) ROM image from the "File"
menu. The selected ROM will be placed in memory from locations `$C000`
to `$FFFF`

### 3.4 Memory Window

![Memory Window] (https://github.com/sethm/symon/raw/master/screenshots/memory_window.png)

Memory contents can be viewed (and edited) one page at a time through the Memory Window.

### 3.5 Trace Log

![Trace Log] (https://github.com/sethm/symon/raw/master/screenshots/trace_log.png)

The last 20,000 execution steps are disassembled and logged to the Trace Log Window.

## 4.0 Usage

### 4.1 Building

To build Symon with Apache Maven, just type:

    $ mvn package

Maven will build Symon, run unit tests, and produce a jar file in the
`target` directory containing the compiled simulator.

Symon is meant to be invoked directly from the jar file. To run with
Java 1.5 or greater, just type:

    $ java -jar symon-0.8.5.jar

When Symon is running, you should be presented with a simple graphical
interface.

### 4.2 ROM images

The simulator requires a 16KB ROM image loaded at address `$C000` to `$FFFF` to
work properly. Without a ROM in memory, the simulator will not be able to
reset, since the reset vector for the 6502 is located in this address space.

By default, any 16KB file named `rom.bin` that exists in the same directory
where Symon is launched will be loaded as a ROM image. ROM images can also
be swapped out at run-time with the "Load ROM Image..." in the File menu.

The "samples" directory contains a ROM image named 'ehbasic.rom', containing
Lee Davison's Enhanced 6502 BASIC. This serves as a good starting point for
exploration.

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

## 6.0 To Do

  - Feedback (in the form of dialogs, status bar, etc).

  - Better debugging tools from the UI, including breakpoints
    and disassembly.

  - More accurate timing.
  
  - Smarter interrupt handling.

  - UI needs a ton more polish.

  - More extensive testing.

  - Clean up JavaDoc.

  - Busses are defined by start address and length. Devices are defined
    by start address and end address. They should both use start/end
    address.

  - Implement CMOS 65C02 instructions and NMOS / CMOS mode flag.

  - Allow displaying ACIA status and dumping ACIA buffers, for
    debugging.

## 7.0 Acknowledgements

This project would not have been possible without the following resources:

  - [Graham Edgecombe's JTerminal project] (https://github.com/grahamedgecombe/jterminal),
    which forms the core of Symon's console.

  - [Andrew Jacobs' 6502 Pages] (http://www.obelisk.demon.co.uk/6502/), for 
    wonderfully detailed information about the 6502

  - [Neil Parker's "The 6502/65C02/65C816 Instruction Set Decoded"] (http://www.llx.com/~nparker/a2/opcodes.html),
    for information about how instructions are coded

## 8.0 Licensing

Symon is free software.  It is distributed under the MIT License.
Please see the file 'COPYING' for full details of the license.
