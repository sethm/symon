package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryAccessException;

import junit.framework.TestCase;

public class CpuAbsoluteYModeTest extends TestCase {

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
        bus.write(0xfffc, Cpu.DEFAULT_BASE_ADDRESS & 0x00ff);
        bus.write(0xfffd, (Cpu.DEFAULT_BASE_ADDRESS & 0xff00) >>> 8);

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
    * ORA - $19
    * AND - $39
    * EOR - $59
    * ADC - $79
    * STA - $99
    * LDA - $b9
    * LDX - $be
    * CMP - $d9
    * SBC - $f9
    */

    /* ORA - Logical Inclusive OR - $19 */

    public void test_ORA() throws MemoryAccessException {
        // Set some initial values in memory
        bus.write(0x2c30, 0x00);
        bus.write(0x2c32, 0x11);
        bus.write(0x2c34, 0x22);
        bus.write(0x2c38, 0x44);
        bus.write(0x2c40, 0x88);

        // Set offset in Y register.
        cpu.setYRegister(0x30);

        bus.loadProgram(0x19, 0x00, 0x2c,  // ORA $2c00,Y
                        0x19, 0x02, 0x2c,  // ORA $2c02,Y
                        0x19, 0x04, 0x2c,  // ORA $2c04,Y
                        0x19, 0x08, 0x2c,  // ORA $2c08,Y
                        0x19, 0x10, 0x2c); // ORA $2c10,Y

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

    /* AND - Logical AND - $39 */

    public void test_AND() throws MemoryAccessException {
        bus.write(0x1a30, 0x00);
        bus.write(0x1a31, 0x11);
        bus.write(0x1a32, 0xff);
        bus.write(0x1a33, 0x99);
        bus.write(0x1a34, 0x11);
        bus.write(0x1a35, 0x0f);
        bus.write(0x1a02, 0x11);

        // Set offset in Y register.
        cpu.setYRegister(0x30);

        bus.loadProgram(0x39, 0x00, 0x1a,  // AND $1a00,Y
                        0x39, 0x01, 0x1a,  // AND $1a01,Y
                        0xa9, 0xaa,        // LDA #$aa
                        0x39, 0x02, 0x1a,  // AND $1a02,Y
                        0x39, 0x03, 0x1a,  // AND $1a03,Y
                        0x39, 0x04, 0x1a,  // AND $1a04,Y
                        0xa9, 0xff,        // LDA #$ff
                        0x39, 0x05, 0x1a,  // AND $1a05,Y
                        0xa9, 0x01,        // LDA #$01
                        0x39, 0xd2, 0x19); // AND $19d2,Y
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

    /* EOR - Exclusive OR - $59 */

    public void test_EOR() throws MemoryAccessException {
        bus.write(0xab40, 0x00);
        bus.write(0xab41, 0xff);
        bus.write(0xab42, 0x33);
        bus.write(0xab43, 0x44);

        cpu.setYRegister(0x30);

        bus.loadProgram(0xa9, 0x88,        // LDA #$88
                        0x59, 0x10, 0xab,  // EOR $ab10,Y
                        0x59, 0x11, 0xab,  // EOR $ab11,Y
                        0x59, 0x12, 0xab,  // EOR $ab12,Y
                        0x59, 0x13, 0xab); // EOR $ab13,Y
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

    /* ADC - Add with Carry - $79 */

    public void test_ADC() throws MemoryAccessException {
        bus.write(0xab40, 0x01);
        bus.write(0xab41, 0xff);

        cpu.setYRegister(0x30);

        bus.loadProgram(0xa9, 0x00,        // LDA #$00
                        0x79, 0x10, 0xab); // ADC $ab10,Y
        cpu.step(2);
        assertEquals(0x01, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setYRegister(0x30);

        bus.loadProgram(0xa9, 0x7f,        // LDA #$7f
                        0x79, 0x10, 0xab); // ADC $ab10,Y
        cpu.step(2);
        assertEquals(0x80, cpu.getAccumulator());
        assertTrue(cpu.getNegativeFlag());
        assertTrue(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setYRegister(0x30);

        bus.loadProgram(0xa9, 0x80,        // LDA #$80
                        0x79, 0x10, 0xab); // ADC $ab10,Y
        cpu.step(2);
        assertEquals(0x81, cpu.getAccumulator());
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setYRegister(0x30);

        bus.loadProgram(0xa9, 0xff,        // LDA #$ff
                        0x79, 0x10, 0xab); // ADC $ab10,Y
        cpu.step(2);
        assertEquals(0x00, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertTrue(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        cpu.setYRegister(0x30);

        bus.loadProgram(0xa9, 0x00,        // LDA #$00
                        0x79, 0x11, 0xab); // ADC $ab11,Y
        cpu.step(2);
        assertEquals(0xff, cpu.getAccumulator());
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setYRegister(0x30);

        bus.loadProgram(0xa9, 0x7f,        // LDA #$7f
                        0x79, 0x11, 0xab); // ADC $ab11,Y
        cpu.step(2);
        assertEquals(0x7e, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        cpu.setYRegister(0x30);

        bus.loadProgram(0xa9, 0x80,        // LDA #$80
                        0x79, 0x11, 0xab); // ADC $ab11,Y
        cpu.step(2);
        assertEquals(0x7f, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertTrue(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        cpu.setYRegister(0x30);

        bus.loadProgram(0xa9, 0xff,        // LDA #$ff
                        0x79, 0x11, 0xab); // ADC $ab11,Y
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
                        0x79, 0x10, 0xab); // ADC $ab10,Y

        cpu.setYRegister(0x30);

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
                        0x79, 0x10, 0xab); // ADC $ab10,Y

        cpu.setYRegister(0x30);

        cpu.step(3);
        assertEquals(0x02, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setYRegister(0x30);

        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x49,        // LDA #$49
                        0x79, 0x10, 0xab); // ADC $ab10,Y
        cpu.step(3);
        assertEquals(0x50, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setYRegister(0x30);

        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x50,        // LDA #$50
                        0x79, 0x10, 0xab); // ADC $ab10,Y
        cpu.step(3);
        assertEquals(0x51, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setYRegister(0x30);

        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x99,        // LDA #$99
                        0x79, 0x10, 0xab); // ADC $ab10,Y
        cpu.step(3);
        assertEquals(0x00, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertTrue(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        cpu.setYRegister(0x30);

        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x00,        // LDA #$00
                        0x79, 0x11, 0xab); // ADC $ab10,Y
        cpu.step(3);
        assertEquals(0x99, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        cpu.setYRegister(0x30);

        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x49,        // LDA #$49
                        0x79, 0x11, 0xab); // ADC $ab11,Y
        cpu.step(3);
        assertEquals(0x48, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        cpu.setYRegister(0x30);

        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x50,        // LDA #$59
                        0x79, 0x11, 0xab); // ADC $ab11,Y
        cpu.step(3);
        assertEquals(0x49, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());
    }

    /* STA - Store Accumulator - $99 */

    public void test_STA() throws MemoryAccessException {
        cpu.setYRegister(0x30);

        cpu.setAccumulator(0x00);
        bus.loadProgram(0x99, 0x10, 0xab); // STA $ab10,Y
        cpu.step();
        assertEquals(0x00, bus.read(0xab40));
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.reset();
        cpu.setYRegister(0x30);


        cpu.setAccumulator(0x0f);
        bus.loadProgram(0x99, 0x10, 0xab); // STA $ab10,Y
        cpu.step();
        assertEquals(0x0f, bus.read(0xab40));
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.reset();
        cpu.setYRegister(0x30);

        cpu.setAccumulator(0x80);
        bus.loadProgram(0x99, 0x10, 0xab); // STA $ab10,Y
        cpu.step();
        assertEquals(0x80, bus.read(0xab40));
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
    }

    /* LDX - Load X Register - $be */

    public void test_LDX() throws MemoryAccessException {
        bus.write(0xab45, 0x00);
        bus.write(0xab46, 0x0f);
        bus.write(0xab47, 0x80);

        bus.loadProgram(0xbe, 0x10, 0xab,  // LDX $ab10,Y
                        0xbe, 0x11, 0xab,  // LDX $ab11,Y
                        0xbe, 0x12, 0xab); // LDX $ab12,Y

        cpu.setYRegister(0x35);

        cpu.step();
        assertEquals(0x00, cpu.getXRegister());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x0f, cpu.getXRegister());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.step();
        assertEquals(0x80, cpu.getXRegister());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
    }

    /* LDA - Load Accumulator - $b9 */

    public void test_LDA() throws MemoryAccessException {
        bus.write(0xab42, 0x00);
        bus.write(0xab43, 0x0f);
        bus.write(0xab44, 0x80);

        bus.loadProgram(0xb9, 0x10, 0xab,  // LDA $ab10,Y
                        0xb9, 0x11, 0xab,  // LDA $ab11,Y
                        0xb9, 0x12, 0xab); // LDA $ab12,Y

        cpu.setYRegister(0x32);

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

    /* CMP - Compare Accumulator - $d9 */

    public void test_CMP() throws MemoryAccessException {
        bus.write(0xab40, 0x00);
        bus.write(0xab41, 0x80);
        bus.write(0xab42, 0xff);

        cpu.setAccumulator(0x80);

        bus.loadProgram(0xd9, 0x10, 0xab,  // CMP $ab10,Y
                        0xd9, 0x11, 0xab,  // CMP $ab11,Y
                        0xd9, 0x12, 0xab); // CMP $ab12,Y

        cpu.setYRegister(0x30);

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
        assertFalse(cpu.getNegativeFlag()); // m - y > 0
    }

    /* SBC - Subtract with Carry - $f9 */

    public void test_SBC() throws MemoryAccessException {
        bus.write(0xab40, 0x01);

        bus.loadProgram(0xa9, 0x00,        // LDA #$00
                        0xf9, 0x10, 0xab); // SBC $ab10,Y
        cpu.setYRegister(0x30);
        cpu.step(2);
        assertEquals(0xfe, cpu.getAccumulator());
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getCarryFlag());

        cpu.reset();
        bus.loadProgram(0xa9, 0x7f,        // LDA #$7f
                        0xf9, 0x10, 0xab); // SBC $ab10,Y
        cpu.setYRegister(0x30);
        cpu.step(2);
        assertEquals(0x7d, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        bus.loadProgram(0xa9, 0x80,        // LDA #$80
                        0xf9, 0x10, 0xab); // SBC $ab10,Y
        cpu.setYRegister(0x30);
        cpu.step(2);
        assertEquals(0x7e, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertTrue(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        bus.loadProgram(0xa9, 0xff,        // LDA #$ff
                        0xf9, 0x10, 0xab); // SBC $ab10,Y
        cpu.setYRegister(0x30);
        cpu.step(2);
        assertEquals(0xfd, cpu.getAccumulator());
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();
        bus.loadProgram(0xa9, 0x02,        // LDA #$02
                        0xf9, 0x10, 0xab); // SBC $ab10,Y
        cpu.setYRegister(0x30);
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
                        0xf9, 0x10, 0xab); // SBC $ab10,Y
        cpu.setYRegister(0x30);
        cpu.step(3);
        assertEquals(0x03, cpu.getAccumulator());

        cpu.reset();

        // Subtrace with Carry Flag cleared
        bus.loadProgram(0x18,              // CLC
                        0xa9, 0x00,        // LDA #$00
                        0xf9, 0x10, 0xab); // SBC $ab10,Y
        cpu.setYRegister(0x30);
        cpu.step(3);
        assertEquals(0xfe, cpu.getAccumulator());

        cpu.reset();

        // Subtract with Carry Flag set
        bus.loadProgram(0x38,              // SEC
                        0xa9, 0x05,        // LDA #$00
                        0xf9, 0x10, 0xab); // SBC $ab10,Y
        cpu.setYRegister(0x30);
        cpu.step(3);
        assertEquals(0x04, cpu.getAccumulator());
        assertTrue(cpu.getCarryFlag());

        cpu.reset();

        // Subtract with Carry Flag set
        bus.loadProgram(0x38,              // SEC
                        0xa9, 0x00,        // LDA #$00
                        0xf9, 0x10, 0xab); // SBC $ab10,Y
        cpu.setYRegister(0x30);
        cpu.step(3);
        assertEquals(0xff, cpu.getAccumulator());
        assertFalse(cpu.getCarryFlag());

    }

    public void test_SBC_DecimalMode() throws MemoryAccessException {
        bus.write(0xab40, 0x01);
        bus.write(0xab50, 0x11);

        bus.loadProgram(0xf8,              // SED
                        0xa9, 0x00,        // LDA #$00
                        0xf9, 0x10, 0xab); // SBC $ab10,Y
        cpu.setYRegister(0x30);
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
                        0xf9, 0x10, 0xab); // SBC $ab10,Y
        cpu.setYRegister(0x30);
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
                        0xf9, 0x10, 0xab); // SBC $ab10,Y
        cpu.setYRegister(0x30);
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
                        0xf9, 0x10, 0xab);  // SBC $ab10,Y
        cpu.setYRegister(0x30);
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
                        0xf9, 0x20, 0xab);  // SBC $ab20,Y
        cpu.setYRegister(0x30);
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
                        0xf9, 0x10, 0xab);  // SBC $ab10,Y
        cpu.setYRegister(0x30);
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
                        0xf9, 0x10, 0xab);  // SBC $ab10,Y
        cpu.setYRegister(0x30);
        cpu.step(4);
        assertEquals(0x99, cpu.getAccumulator());
        assertFalse(cpu.getCarryFlag());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getDecimalModeFlag());
    }

}
