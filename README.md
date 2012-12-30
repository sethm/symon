SYMON - A 6502 System Simulator
===============================

**NOTE: THIS IS BETA QUALITY SOFTWARE UNDER ACTIVE DEVELOPMENT.  Feedback is
welcome!**

**Version:** 0.8.0

**Last Updated:** 29 December, 2012

Copyright (c) 2008-2012 Seth J. Morabito &lt;web@loomcom.com&gt;

See the file COPYING for license.


## 1.0 About

Symon is a general purpose simulator for systems based on the NMOS
Mostek 6502 microprocessor and compatibles.  Symon is implemented in
Java.  Its core goals are accuracy, ease of development, clear
documentation, and extensive test suites for validating correctness.

The initial goal is to simulate a system with an NMOS 6502 or CMOS
65C02 central processor; one or more 6522 VIAs; and one or more 6551
ACIAs.  More functionality may be considered as time goes on.


## 2.0 Requirements


  - Java 1.5 or higher
  - Maven 2.0.x or higher (for building from source)
  - JUnit 4 or higher (for testing)


## 3.0 Usage


### 3.1 Building

To build Symon with Apache Maven, just type:

    $ mvn package

Maven will build Symon, run unit tests, and produce a jar file in the
'target' directory containing the compiled simulator.

Symon is meant to be invoked directly from the jar file. To run with
Java 1.5 or greater, just type:

    $ java -jar symon-0.7.0.jar

When Symon is running, you should be presented with a simple graphical
interface.

### 3.2 ROM images

The simulator requires a 16KB ROM image loaded at address $C000 to $FFFF to
work properly. Without a ROM in memory, the simulator will not be able to
reset, since the reset vector for the 6502 is located in this address space.

By default, any 16KB file named 'rom.bin' that exists in the same directory
where Symon is launched will be loaded as a ROM image. ROM images can also
be swapped out at run-time with the "Load ROM Image..." in the File menu.

The "samples" directory contains a ROM image named 'ehbasic.rom', containing
Lee Davison's Enhanced 6502 BASIC. This serves as a good starting point for
exploration.

### 3.3 Loading A Program

In addition to ROM images, programs in the form of raw binary object files can
be loaded directly into memory from "Load Program..." in the File menu.

Programs are loaded starting at addres $0300.  After loading the program, the
simulated CPU's reset vector is loaded with the values $00, $03, and the CPU is
reset.

There are two very simple sample program in the "samples" directory,
for testing.
  
- 'echo.prg' will echo back anything typed at the console.

- 'hello.prg' will continuously print "Hello, 6502 World!" to the console.

### 3.4 Running

After loading a program or ROM image, clicking "Run" will start the simulator
running.

## 4.0 Revision History

  - 0.8.0: 29 December, 2012
  - 0.7.0: 9 December, 2012
  - 0.6: 5 November, 2012
  - 0.5: 21 October, 2012
  - 0.3: 14 October, 2012
  - 0.2: 22 April, 2012
  - 0.1: 20 January, 2010

## 5.0 To Do

- Feedback (in the form of dialogs, status bar, etc).

- Better ROM image handling

- Better debugging tools from the UI, including memory inspection,
  disassembly, breakpoints, and execution tracing.

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


## 6.0 Licensing

Symon is free software.  It is distributed under the MIT License.
Please see the file 'COPYING' for full details of the license.
