SYMON - A 6502 System Simulator
===============================

**NOTE: THIS IS ALPHA QUALITY SOFTWARE UNDER ACTIVE DEVELOPMENT. IT IS
NOT YET FULLY FUNCTIONAL. IT MAY BE USEFUL, BUT IT IS NOT YET INTENDED
TO BE USED BY ANYONE BUT DEVELOPERS. Feedback is welcome!**

**Version:** 0.3
**Last Updated:** 14 October, 2012  

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

    $ java -jar symon-0.3-jar-with-dependencies.jar

When Symon is running, you should be presented with a simple graphical
interface.


### 3.2 Loading A Program

Programs in the form of raw binary object files can be loaded directly
into memory with the "Load" button.

Right now, all programs are loaded starting at addres $0300.  After
loading, the simulated CPU's reset vector is loaded with the values
$00, $03, and the CPU is reset.

There are two very simple sample program in the "samples" directory,
for testing.
  
- 'echo.prg' will echo back anything typed at the console.

- 'hello.prg' will continuously print "Hello, 6502 World!" to the console.

### 3.3 Running

After loading a program, clicking "Run" will start the simulator
running at address $0300.


## 4.0 To Do

- Accurate timing (all simulated instructions currently take
  only one step to execute)
  
- Interrupt handling!

- UI needs a ton more polish.

- Add a simple menu interface for common tasks.

- More extensive testing.

- Clean up JavaDoc.

- Busses are defined by start address and length. Devices are defined
  by start address and end address. They should both use start/end
  address.

- Implement CMOS 65C02 instructions and NMOS / CMOS mode flag.

- Allow a flag to disable breaking to monitor on BRK.

- Allow displaying ACIA status and dumping ACIA buffers, for
  debugging.


## 5.0 Licensing

Symon is free software.  It is distributed under the MIT License.
Please see the file 'COPYING' for full details of the license.
