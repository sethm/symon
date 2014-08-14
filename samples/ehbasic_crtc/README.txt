This directoy contains a very slightly modified version of EhBASIC 2.22
to support the Symon simulator.

Usage
-----

The pre-assemled ROM image 'ehbasic.rom' in the "samples" directory is all you
need unless you want to rebuild this source code.

Just copy the image 'ehbasic.rom' to the directory where you run Symon, and
rename the file to 'rom.bin'. It will be loaded at memory location $D000 when
the simulator starts up. Click "Run" and you'll be presented with BASIC.

At the first prompt, type 'C' for a Cold Start

When prompted for free memory, type: $C000

Note that EhBASIC only accepts upper case input, so you'll need to use caps
lock (the cruise control for cool) to really make the most of it.


Building
--------


To build it, you'll need the CC65 tool chain from http://www.cc65.org/
and a suitable version of 'make'. Just typing:

   % make
   
in this directory should re-build everything. You'll get a listing,
some object files, and the ROM image itself.


Changes from the EhBASIC 2.22
-----------------------------

  - Minor syntax changes to allow assembly with the cc65 tool chain.

  - Memory map modified for Symon.

  - At reset, configure the 6551 ACIA for 8-N-1, 2400 baud.
 
  - Monitor routines 'ACIAin' and 'ACIAout' modified for the 6551 ACIA.
    Specifically, reading and writing will check the 6551 status register for
    the status of rx and tx registers before receive or transmit.

EhBASIC is copyright Lee Davison <leeedavison@googlemail.com>

My changes are so slight that I hesitate to even say this, but for "CYA"
reasons:

Changes are copyright Seth Morabito <web@loomcom.com> and are distributed under
the same license as EhBASIC. I claim no commercial interest whatsoever. Any
commercial use must be negotiated directly with Lee Davison.


Original EhBASIC 2.22 README
----------------------------

 Enhanced BASIC is a BASIC interpreter for the 6502 family microprocessors. It
 is constructed to be quick and powerful and easily ported between 6502 systems.
 It requires few resources to run and includes instructions to facilitate easy
 low level handling of hardware devices. It also retains most of the powerful
 high level instructions from similar BASICs.

 EhBASIC is free but not copyright free. For non commercial use there is only one
 restriction, any derivative work should include, in any binary image distributed,
 the string "Derived from EhBASIC" and in any distribution that includes human
 readable files a file that includes the above string in a human readable form
 e.g. not as a comment in an HTML file.

 For commercial use please contact me,  Lee Davison, at leeedavison@googlemail.com
 for conditions.

 For more information on EhBASIC, other versions of EhBASIC and other projects
 please visit my site at ..

	 http://mycorner.no-ip.org/index.html


 P.S. c't magazin, henceforth refered to as "those thieving german bastards", are
 prohibited from using this or any version of EhBASIC for any of their projects
 or products. The excuse "we don't charge people for it" doesn't wash, it adds
 value to your product so you owe me.
