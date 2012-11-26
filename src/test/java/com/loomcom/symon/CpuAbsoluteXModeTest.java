package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryAccessException;

import junit.framework.TestCase;

public class CpuAbsoluteXModeTest extends TestCase {

    protected Cpu    cpu;
    protected Bus    bus;
    protected Memory mem;

    protected void setUp() throws Exception {
        this.cpu = new Cpu();
        this.bus = new Bus(0x0000, 0xffff);
        this.mem = new Memory(0x0000, 0x10000);
        bus.addCpu(cpu);
        bus.addDevice(mem);

        // Load the reset vector.
        bus.write(0xfffc, Bus.DEFAULT_LOAD_ADDRESS & 0x00ff);
        bus.write(0xfffd, (Bus.DEFAULT_LOAD_ADDRESS & 0xff00) >>> 8);

        cpu.reset();
        // Assert initial state
        assertEquals(0, cpu.getAccumulator());
        assertEquals(0, cpu.getXRegister());
        assertEquals(0, cpu.getYRegister());
        assertEquals(0x200, cpu.getProgramCounter());
        assertEquals(0xff, cpu.getStackPointer());
        assertEquals(0x20, cpu.getProcessorStatus());
    }

    /*
    * The following opcodes are tested for correctness in this file:
    *
    * ORA - $1d
    * ASL - $1e
    * AND - $3d
    * ROL - $3e
    * EOR - $5d
    * LSR - $5e
    * ADC - $7d
    * ROR - $7e
    * STA - $9d
    * LDY - $bc
    * LDA - $bd
    * CMP - $dd
    * DEC - $de
    * SBC - $fd
    * INC - $fe
    */

    /* ORA - Logical Inclusive OR - $1d */

    public void test_ORA() throws MemoryAccessException {
        // Set some initial values in memory
        bus.write(0x2c30, 0x00);
        bus.write(0x2c32, 0x11);
        bus.write(0x2c34, 0x22);
        bus.write(0x2c38, 0x44);
        bus.write(0x2c40, 0x88);

        // Set offset in X register.
        cpu.setXRegister(0x30);

        bus.loadProgram(0x1d, 0x00, 0x2c,  // ORA $2c00,X
                        0x1d, 0x02, 0x2c,  // ORA $2c02,X
                        0x1d, 0x04, 0x2c,  // ORA $2c04,X
                        0x1d, 0x08, 0x2c,  // ORA $2c08,X
                        0x1d, 0x10, 0x2c); // ORA $2c10,X

        cpu.step();
        assertEquals(0x00, cpu.getAccumulator());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x11, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x33, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x77, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0xff, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());

    }

    /* ASL - Arithmetic Shift Left - $1e */

    public void test_ASL() throws MemoryAccessException {
        bus.write(0x2c30, 0x00);
        bus.write(0x2c31, 0x01);
        bus.write(0x2c32, 0x02);
        bus.write(0x2c33, 0x44);
        bus.write(0x2c34, 0x80);

        // Set offset in X register.
        cpu.setXRegister(0x30);

        bus.loadProgram(0x1e, 0x00, 0x2c,  // ASL $2c00,X
                        0x1e, 0x01, 0x2c,  // ASL $2c01,X
                        0x1e, 0x02, 0x2c,  // ASL $2c02,X
                        0x1e, 0x03, 0x2c,  // ASL $2c03,X
                        0x1e, 0x04, 0x2c); // ASL $2c04,X

        cpu.step();
        assertEquals(0x00, bus.read(0x2c30));
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x02, bus.read(0x2c31));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x04, bus.read(0x2c32));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x88, bus.read(0x2c33));
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x00, bus.read(0x2c34));
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertTrue(cpu.getCarryFlag());
    }

    /* AND - Logical AND - $3d */

    public void test_AND() throws MemoryAccessException {
        bus.write(0x1a30, 0x00);
        bus.write(0x1a31, 0x11);
        bus.write(0x1a32, 0xff);
        bus.write(0x1a33, 0x99);
        bus.write(0x1a34, 0x11);
        bus.write(0x1a35, 0x0f);
        bus.write(0x1a02, 0x11);

        // Set offset in X register.
        cpu.setXRegister(0x30);

        bus.loadProgram(0x3d, 0x00, 0x1a,  // AND $1a00,X
                        0x3d, 0x01, 0x1a,  // AND $1a01,X
                        0xa9, 0xaa,        // LDA #$aa
                        0x3d, 0x02, 0x1a,  // AND $1a02,X
                        0x3d, 0x03, 0x1a,  // AND $1a03,X
                        0x3d, 0x04, 0x1a,  // AND $1a04,X
                        0xa9, 0xff,        // LDA #$ff
                        0x3d, 0x05, 0x1a,  // AND $1a05,X
                        0xa9, 0x01,        // LDA #$01
                        0x3d, 0xd2, 0x19); // AND $19d2,X
        cpu.step();
        assertEquals(0x00, cpu.getAccumulator());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x00, cpu.getAccumulator());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step(2);
        assertEquals(0xaa, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x88, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x00, cpu.getAccumulator());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step(2);
        assertEquals(0x0f, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step(2);
        assertEquals(0x01, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    /* ROL - Rotate Shift Left - $3e */

    public void test_ROL() throws MemoryAccessException {
        bus.write(0x1070, 0x00);
        bus.write(0x1071, 0x01);

        // Set offset in X register
        cpu.setXRegister(0x70);

        bus.loadProgram(0x3e, 0x00, 0x10,  // ROL $1000,X (m=%00000000, c=0)
                        0x3e, 0x01, 0x10,  // ROL $1001,X (m=%00000010, c=0)
                        0x38,              // SEC         (m=%00000010, c=1)
                        0x3e, 0x01, 0x10,  // ROL $1001,X (m=%00000101, c=0)
                        0x3e, 0x01, 0x10,  // ROL $1001,X (m=%00001010, c=0)
                        0x3e, 0x01, 0x10,  // ROL $1001,X (m=%00010100, c=0)
                        0x3e, 0x01, 0x10,  // ROL $1001,X (m=%00101000, c=0)
                        0x3e, 0x01, 0x10,  // ROL $1001,X (m=%01010000, c=0)
                        0x3e, 0x01, 0x10,  // ROL $1001,X (m=%10100000, c=0)
                        0x3e, 0x01, 0x10,  // ROL $1001,X (m=%01000000, c=1)
                        0x3e, 0x01, 0x10); // ROL $1001,X (m=%10000001, c=0)

        cpu.step();
        assertEquals(0x00, bus.read(0x1070));
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x02, bus.read(0x1071));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step(2);
        assertEquals(0x05, bus.read(0x1071));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x0a, bus.read(0x1071));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x14, bus.read(0x1071));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x28, bus.read(0x1071));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x50, bus.read(0x1071));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0xa0, bus.read(0x1071));
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x40, bus.read(0x1071));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x81, bus.read(0x1071));
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());
    }

    /* EOR - Exclusive OR - $5d */

    public void test_EOR() throws MemoryAccessException {
        bus.write(0xab40, 0x00);
        bus.write(0xab41, 0xff);
        bus.write(0xab42, 0x33);
        bus.write(0xab43, 0x44);

        cpu.setXRegister(0x30);

        bus.loadProgram(0xa9, 0x88,         // LDA #$88
                        0x5d, 0x10, 0xab,  // EOR $ab10,X
                        0x5d, 0x11, 0xab,  // EOR $ab11,X
                        0x5d, 0x12, 0xab,  // EOR $ab12,X
                        0x5d, 0x13, 0xab); // EOR $ab13,X
        cpu.step(2);
        assertEquals(0x88, cpu.getAccumulator());
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getZeroFlag());

        cpu.step();
        assertEquals(0x77, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getZeroFlag());

        cpu.step();
        assertEquals(0x44, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getZeroFlag());

        cpu.step();
        assertEquals(0x00, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertTrue(cpu.getZeroFlag());
    }

    /* LSR - Logical Shift Right - $5e */

    public void test_LSR() throws MemoryAccessException {
        bus.write(0xab30, 0x00);
        bus.write(0xab31, 0x01);
        bus.write(0xab32, 0x02);
        bus.write(0xab33, 0x44);
        bus.write(0xab34, 0x80);
        bus.write(0xab35, 0x02);

        cpu.setXRegister(0x30);

        bus.loadProgram(0x5e, 0x00, 0xab,  // LSR $ab00,X
                        0x5e, 0x01, 0xab,  // LSR $ab01,X
                        0x5e, 0x02, 0xab,  // LSR $ab02,X
                        0x5e, 0x03, 0xab,  // LSR $ab03,X
                        0x5e, 0x04, 0xab,  // LSR $ab04,X
                        0x38,              // SEC
                        0x5e, 0x05, 0xab); // LSR $ab05,X

        cpu.step();
        assertEquals(0x00, bus.read(0xab30));
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x00, bus.read(0xab31));
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x01, bus.read(0xab32));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x22, bus.read(0xab33));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x40, bus.read(0xab34));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        // Setting Carry should not affect the result.
        cpu.step(2);
        assertEquals(0x01, bus.read(0xab35));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());
    }

    /* ADC - Add with Carry - $7d */

    public void test_ADC() throws MemoryAccessException {
        bus.write(0xab40, 0x01);
        bus.write(0xab41, 0xff);

        cpu.setXRegister(0x30);

        bus.loadProgram(0xa9, 0x00,        // LDA #$00
                        0x7d, 0x10, 0xab); // ADC $ab10,X
        cpu.step(2);
        assertEquals(0x01, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setXRegister(0x30);

        bus.loadProgram(0xa9, 0x7f,        // LDA #$7f
                        0x7d, 0x10, 0xab); // ADC $ab10,X
        cpu.step(2);
        assertEquals(0x80, cpu.getAccumulator());
        assertTrue(cpu.getNegativeFlag());
        assertTrue(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setXRegister(0x30);

        bus.loadProgram(0xa9, 0x80,        // LDA #$80
                        0x7d, 0x10, 0xab); // ADC $ab10,X
        cpu.step(2);
        assertEquals(0x81, cpu.getAccumulator());
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setXRegister(0x30);

        bus.loadProgram(0xa9, 0xff,        // LDA #$ff
                        0x7d, 0x10, 0xab); // ADC $ab10,X
        cpu.step(2);
        assertEquals(0x00, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertTrue(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        cpu.setXRegister(0x30);
        bus.loadProgram(0xa9, 0x00,        // LDA #$00
                        0x7d, 0x11, 0xab); // ADC $ab11,X
        cpu.step(2);
        assertEquals(0xff, cpu.getAccumulator());
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setXRegister(0x30);
        bus.loadProgram(0xa9, 0x7f,        // LDA #$7f
                        0x7d, 0x11, 0xab); // ADC $ab11,X
        cpu.step(2);
        assertEquals(0x7e, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        cpu.setXRegister(0x30);
        bus.loadProgram(0xa9, 0x80,        // LDA #$80
                        0x7d, 0x11, 0xab); // ADC $ab11,X
        cpu.step(2);
        assertEquals(0x7f, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertTrue(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        cpu.setXRegister(0x30);
        bus.loadProgram(0xa9, 0xff,        // LDA #$ff
                        0x7d, 0x11, 0xab); // ADC $ab11,X
        cpu.step(2);
        assertEquals(0xfe, cpu.getAccumulator());
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());
    }

    public void test_ADC_IncludesCarry() throws MemoryAccessException {
        bus.write(0xab40, 0x01);

        bus.loadProgram(0xa9, 0x00,        // LDA #$00
                        0x38,              // SEC
                        0x7d, 0x10, 0xab); // ADC $ab10,X

        cpu.setXRegister(0x30);

        cpu.step(3);
        assertEquals(0x02, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());
    }

    public void test_ADC_DecimalMode() throws MemoryAccessException {
        bus.write(0xab40, 0x01);
        bus.write(0xab41, 0x99);

        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x01,        // LDA #$01
                        0x7d, 0x10, 0xab); // ADC $ab10,X

        cpu.setXRegister(0x30);

        cpu.step(3);
        assertEquals(0x02, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setXRegister(0x30);
        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x49,        // LDA #$49
                        0x7d, 0x10, 0xab); // ADC $ab10,X
        cpu.step(3);
        assertEquals(0x50, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setXRegister(0x30);
        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x50,        // LDA #$50
                        0x7d, 0x10, 0xab); // ADC $ab10,X
        cpu.step(3);
        assertEquals(0x51, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setXRegister(0x30);
        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x99,        // LDA #$99
                        0x7d, 0x10, 0xab); // ADC $ab10,X
        cpu.step(3);
        assertEquals(0x00, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertTrue(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        cpu.setXRegister(0x30);
        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x00,        // LDA #$00
                        0x7d, 0x11, 0xab); // ADC $ab10,X
        cpu.step(3);
        assertEquals(0x99, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setXRegister(0x30);
        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x49,        // LDA #$49
                        0x7d, 0x11, 0xab); // ADC $ab11,X
        cpu.step(3);
        assertEquals(0x48, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        cpu.setXRegister(0x30);
        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x50,        // LDA #$59
                        0x7d, 0x11, 0xab); // ADC $ab11,X
        cpu.step(3);
        assertEquals(0x49, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());
    }

    /* ROR - Rotate Right - $7e */

    public void test_ROR() throws MemoryAccessException {

        bus.write(0xab40, 0x00);
        bus.write(0xab41, 0x10);

        bus.loadProgram(0x7e, 0x10, 0xab,  // ROR $ab10 (m=%00000000, c=0)
                        0x7e, 0x11, 0xab,  // ROR $ab11 (m=%00001000, c=0)
                        0x7e, 0x11, 0xab,  // ROR $ab11 (m=%00000100, c=0)
                        0x7e, 0x11, 0xab,  // ROR $ab11 (m=%00000010, c=0)
                        0x7e, 0x11, 0xab,  // ROR $ab11 (m=%00000001, c=0)
                        0x7e, 0x11, 0xab,  // ROR $ab11 (m=%00000000, c=1)
                        0x7e, 0x11, 0xab,  // ROR $ab11 (m=%10000000, c=0)
                        0x7e, 0x11, 0xab,  // ROR $ab11 (m=%01000000, c=0)
                        0x7e, 0x11, 0xab,  // ROR $ab11 (m=%00100000, c=0)
                        0x7e, 0x11, 0xab); // ROR $ab11 (m=%00010000, c=0)

        cpu.setXRegister(0x30);

        cpu.step();
        assertEquals(0x00, bus.read(0xab40));
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x08, bus.read(0xab41));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x04, bus.read(0xab41));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x02, bus.read(0xab41));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x01, bus.read(0xab41));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x00, bus.read(0xab41));
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x80, bus.read(0xab41));
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x40, bus.read(0xab41));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x20, bus.read(0xab41));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.step();
        assertEquals(0x10, bus.read(0xab41));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getCarryFlag());
    }

    /* STA - Store Accumulator - $9d */

    public void test_STA() throws MemoryAccessException {
        cpu.setXRegister(0x30);

        cpu.setAccumulator(0x00);
        bus.loadProgram(0x9d, 0x10, 0xab); // STA $ab10,X
        cpu.step();
        assertEquals(0x00, bus.read(0xab40));
        // STA should have NO affect on flags.
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.reset();
        cpu.setXRegister(0x30);

        cpu.setAccumulator(0x0f);
        bus.loadProgram(0x9d, 0x10, 0xab); // STA $ab10,X
        cpu.step();
        assertEquals(0x0f, bus.read(0xab40));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.reset();
        cpu.setXRegister(0x30);

        cpu.setAccumulator(0x80);
        bus.loadProgram(0x9d, 0x10, 0xab); // STA $ab10,X
        cpu.step();
        assertEquals(0x80, bus.read(0xab40));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    /* LDY - Load Y Register - $bc */

    public void test_LDY() throws MemoryAccessException {
        bus.write(0xab45, 0x00);
        bus.write(0xab46, 0x0f);
        bus.write(0xab47, 0x80);

        bus.loadProgram(0xbc, 0x10, 0xab,  // LDY $ab10,X
                        0xbc, 0x11, 0xab,  // LDY $ab11,X
                        0xbc, 0x12, 0xab); // LDY $ab12,X

        cpu.setXRegister(0x35);

        cpu.step();
        assertEquals(0x00, cpu.getYRegister());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x0f, cpu.getYRegister());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x80, cpu.getYRegister());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
    }

    /* LDA - Load Accumulator - $bd */

    public void test_LDA() throws MemoryAccessException {
        bus.write(0xab42, 0x00);
        bus.write(0xab43, 0x0f);
        bus.write(0xab44, 0x80);

        bus.loadProgram(0xbd, 0x10, 0xab,  // LDA $ab10,X
                        0xbd, 0x11, 0xab,  // LDA $ab11,X
                        0xbd, 0x12, 0xab); // LDA $ab12,X

        cpu.setXRegister(0x32);

        cpu.step();
        assertEquals(0x00, cpu.getAccumulator());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x0f, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x80, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
    }

    /* CMP - Compare Accumulator - $dd */

    public void test_CMP() throws MemoryAccessException {
        bus.write(0xab40, 0x00);
        bus.write(0xab41, 0x80);
        bus.write(0xab42, 0xff);

        cpu.setAccumulator(0x80);

        bus.loadProgram(0xdd, 0x10, 0xab,  // CMP $ab10,X
                        0xdd, 0x11, 0xab,  // CMP $ab11,X
                        0xdd, 0x12, 0xab); // CMP $ab12,X

        cpu.setXRegister(0x30);

        cpu.step();
        assertTrue(cpu.getCarryFlag());    // m > y
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag()); // m - y < 0

        cpu.step();
        assertTrue(cpu.getCarryFlag());    // m = y
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag()); // m - y == 0

        cpu.step();
        assertFalse(cpu.getCarryFlag());    // m < y
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag()); // $80 - $ff = $81
    }

    /* DEC - Decrement Memory Location - $de */

    public void test_DEC() throws MemoryAccessException {
        bus.write(0xab40, 0x00);
        bus.write(0xab41, 0x01);
        bus.write(0xab42, 0x80);
        bus.write(0xab43, 0xff);

        bus.loadProgram(0xde, 0x10, 0xab,  // DEC $ab10,X
                        0xde, 0x11, 0xab,  // DEC $ab11,X
                        0xde, 0x12, 0xab,  // DEC $ab12,X
                        0xde, 0x13, 0xab); // DEC $ab13,X

        cpu.setXRegister(0x30);

        cpu.step();
        assertEquals(0xff, bus.read(0xab40));
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x00, bus.read(0xab41));
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x7f, bus.read(0xab42));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0xfe, bus.read(0xab43));
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
    }

    /* SBC - Subtract with Carry - $fd */

    public void test_SBC() throws MemoryAccessException {
        bus.write(0xab40, 0x01);

        bus.loadProgram(0xa9, 0x00,        // LDA #$00
                        0xfd, 0x10, 0xab); // SBC $ab10,X
        cpu.setXRegister(0x30);
        cpu.step(2);
        assertEquals(0xfe, cpu.getAccumulator());
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        bus.loadProgram(0xa9, 0x7f,        // LDA #$7f
                        0xfd, 0x10, 0xab); // SBC $ab10,X
        cpu.setXRegister(0x30);
        cpu.step(2);
        assertEquals(0x7d, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        bus.loadProgram(0xa9, 0x80,        // LDA #$80
                        0xfd, 0x10, 0xab); // SBC $ab10,X
        cpu.setXRegister(0x30);
        cpu.step(2);
        assertEquals(0x7e, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertTrue(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        bus.loadProgram(0xa9, 0xff,        // LDA #$ff
                        0xfd, 0x10, 0xab); // SBC $ab10,X
        cpu.setXRegister(0x30);
        cpu.step(2);
        assertEquals(0xfd, cpu.getAccumulator());
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        bus.loadProgram(0xa9, 0x02,        // LDA #$02
                        0xfd, 0x10, 0xab); // SBC $ab10,X
        cpu.setXRegister(0x30);
        cpu.step(2);
        assertEquals(0x00, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertTrue(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());
    }

    public void test_SBC_IncludesNotOfCarry() throws MemoryAccessException {
        bus.write(0xab40, 0x01);

        // Subtrace with Carry Flag cleared
        bus.loadProgram(0x18,              // CLC
                        0xa9, 0x05,        // LDA #$00
                        0xfd, 0x10, 0xab); // SBC $ab10,X
        cpu.setXRegister(0x30);
        cpu.step(3);
        assertEquals(0x03, cpu.getAccumulator());

        cpu.reset();

        // Subtrace with Carry Flag cleared
        bus.loadProgram(0x18,              // CLC
                        0xa9, 0x00,        // LDA #$00
                        0xfd, 0x10, 0xab); // SBC $ab10,X
        cpu.setXRegister(0x30);
        cpu.step(3);
        assertEquals(0xfe, cpu.getAccumulator());

        cpu.reset();

        // Subtract with Carry Flag set
        bus.loadProgram(0x38,              // SEC
                        0xa9, 0x05,        // LDA #$00
                        0xfd, 0x10, 0xab); // SBC $ab10,X
        cpu.setXRegister(0x30);
        cpu.step(3);
        assertEquals(0x04, cpu.getAccumulator());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();

        // Subtract with Carry Flag set
        bus.loadProgram(0x38,              // SEC
                        0xa9, 0x00,        // LDA #$00
                        0xfd, 0x10, 0xab); // SBC $ab10,X
        cpu.setXRegister(0x30);
        cpu.step(3);
        assertEquals(0xff, cpu.getAccumulator());
        assertFalse(cpu.getCarryFlag());

    }

    public void test_SBC_DecimalMode() throws MemoryAccessException {
        bus.write(0xab40, 0x01);
        bus.write(0xab50, 0x11);

        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x00,        // LDA #$00
                        0xfd, 0x10, 0xab); // SBC $ab10,X
        cpu.setXRegister(0x30);
        cpu.step(3);
        assertEquals(0x98, cpu.getAccumulator());
        assertFalse(cpu.getCarryFlag()); // borrow = set flag
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getDecimalModeFlag());

        cpu.reset();

        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x99,        // LDA #$99
                        0xfd, 0x10, 0xab); // SBC $ab10,X
        cpu.setXRegister(0x30);
        cpu.step(3);
        assertEquals(0x97, cpu.getAccumulator());
        assertTrue(cpu.getCarryFlag()); // No borrow = clear flag
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getDecimalModeFlag());

        cpu.reset();

        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x50,        // LDA #$50
                        0xfd, 0x10, 0xab); // SBC $ab10,X
        cpu.setXRegister(0x30);
        cpu.step(3);
        assertEquals(0x48, cpu.getAccumulator());
        assertTrue(cpu.getCarryFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getDecimalModeFlag());


        cpu.reset();

        bus.loadProgram(0xf8,               // SED
                        0xa9, 0x02,         // LDA #$02
                        0xfd, 0x10, 0xab);  // SBC $ab10,X
        cpu.setXRegister(0x30);
        cpu.step(3);
        assertEquals(0x00, cpu.getAccumulator());
        assertTrue(cpu.getCarryFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertTrue(cpu.getZeroFlag());
        assertTrue(cpu.getDecimalModeFlag());

        cpu.reset();

        bus.loadProgram(0xf8,               // SED
                        0xa9, 0x10,         // LDA #$10
                        0xfd, 0x20, 0xab);  // SBC $ab20,X
        cpu.setXRegister(0x30);
        cpu.step(3);
        assertEquals(0x98, cpu.getAccumulator());
        assertFalse(cpu.getCarryFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getDecimalModeFlag());

        cpu.reset();

        bus.loadProgram(0x38,               // SEC
                        0xf8,               // SED
                        0xa9, 0x05,         // LDA #$05
                        0xfd, 0x10, 0xab);  // SBC $ab10,X
        cpu.setXRegister(0x30);
        cpu.step(4);
        assertEquals(0x04, cpu.getAccumulator());
        assertTrue(cpu.getCarryFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getDecimalModeFlag());

        cpu.reset();

        bus.loadProgram(0x38,               // SEC
                        0xf8,               // SED
                        0xa9, 0x00,         // LDA #$00
                        0xfd, 0x10, 0xab);  // SBC $ab10,X
        cpu.setXRegister(0x30);
        cpu.step(4);
        assertEquals(0x99, cpu.getAccumulator());
        assertFalse(cpu.getCarryFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getDecimalModeFlag());
    }

    /* INC - Increment Memory Location - $fe */

    public void test_INC() throws MemoryAccessException {
        bus.write(0xab30, 0x00);
        bus.write(0xab31, 0x7f);
        bus.write(0xab32, 0xff);

        cpu.setXRegister(0x20);

        bus.loadProgram(0xfe, 0x10, 0xab,  // INC $ab10,X
                        0xfe, 0x11, 0xab,  // INC $ab11,X
                        0xfe, 0x12, 0xab); // INC $ab12,X

        cpu.step();
        assertEquals(0x01, bus.read(0xab30));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x80, bus.read(0xab31));
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x00, bus.read(0xab32));
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

}
