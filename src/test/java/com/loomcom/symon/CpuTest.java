package com.loomcom.symon;

import com.loomcom.symon.util.Utils;
import junit.framework.*;

import com.loomcom.symon.devices.*;
import com.loomcom.symon.exceptions.*;

/**
 *
 */
public class CpuTest extends TestCase {

    private Cpu    cpu;
    private Bus    bus;
    private Memory mem;

    public CpuTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(CpuTest.class);
    }

    public void setUp() throws MemoryRangeException, MemoryAccessException {
        this.cpu = new Cpu();
        this.bus = new Bus(0x0000, 0xffff);
        this.mem = new Memory(0x0000, 0xffff);
        bus.addCpu(cpu);
        bus.addDevice(mem);

        // All test programs start at 0x0200;
        bus.write(0xfffc, 0x00);
        bus.write(0xfffd, 0x02);

        cpu.reset();
    }

    public void testReset() {
        assertEquals(0, cpu.getAccumulator());
        assertEquals(0, cpu.getXRegister());
        assertEquals(0, cpu.getYRegister());
        assertEquals(0x0200, cpu.getProgramCounter());
        assertFalse(cpu.getCarryFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getIrqDisableFlag());
        assertFalse(cpu.getDecimalModeFlag());
        assertFalse(cpu.getBreakFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    public void testStack() throws MemoryAccessException {

        cpu.stackPush(0x13);
        assertEquals(0x13, cpu.stackPop());

        cpu.stackPush(0x12);
        assertEquals(0x12, cpu.stackPop());

        for (int i = 0x00; i <= 0xff; i++) {
            cpu.stackPush(i);
        }

        for (int i = 0xff; i >= 0x00; i--) {
            assertEquals(i, cpu.stackPop());
        }

    }

    public void testStackPush() throws MemoryAccessException {
        assertEquals(0xff, cpu.getStackPointer());
        assertEquals(0x00, bus.read(0x1ff, true));

        cpu.stackPush(0x06);
        assertEquals(0xfe, cpu.getStackPointer());
        assertEquals(0x06, bus.read(0x1ff, true));

        cpu.stackPush(0x05);
        assertEquals(0xfd, cpu.getStackPointer());
        assertEquals(0x06, bus.read(0x1ff, true));
        assertEquals(0x05, bus.read(0x1fe, true));

        cpu.stackPush(0x04);
        assertEquals(0xfc, cpu.getStackPointer());
        assertEquals(0x06, bus.read(0x1ff, true));
        assertEquals(0x05, bus.read(0x1fe, true));
        assertEquals(0x04, bus.read(0x1fd, true));

        cpu.stackPush(0x03);
        assertEquals(0xfb, cpu.getStackPointer());
        assertEquals(0x06, bus.read(0x1ff, true));
        assertEquals(0x05, bus.read(0x1fe, true));
        assertEquals(0x04, bus.read(0x1fd, true));
        assertEquals(0x03, bus.read(0x1fc, true));

        cpu.stackPush(0x02);
        assertEquals(0xfa, cpu.getStackPointer());
        assertEquals(0x06, bus.read(0x1ff, true));
        assertEquals(0x05, bus.read(0x1fe, true));
        assertEquals(0x04, bus.read(0x1fd, true));
        assertEquals(0x03, bus.read(0x1fc, true));
        assertEquals(0x02, bus.read(0x1fb, true));

        cpu.stackPush(0x01);
        assertEquals(0xf9, cpu.getStackPointer());
        assertEquals(0x06, bus.read(0x1ff, true));
        assertEquals(0x05, bus.read(0x1fe, true));
        assertEquals(0x04, bus.read(0x1fd, true));
        assertEquals(0x03, bus.read(0x1fc, true));
        assertEquals(0x02, bus.read(0x1fb, true));
        assertEquals(0x01, bus.read(0x1fa, true));
    }

    public void testStackPushWrapsAroundToStackTop() throws MemoryAccessException {
        cpu.setStackPointer(0x01);

        cpu.stackPush(0x01);
        assertEquals(0x01, bus.read(0x101, true));
        assertEquals(0x00, cpu.getStackPointer());

        cpu.stackPush(0x02);
        assertEquals(0x02, bus.read(0x100, true));
        assertEquals(0xff, cpu.getStackPointer());

        cpu.stackPush(0x03);
        assertEquals(0x03, bus.read(0x1ff, true));
        assertEquals(0xfe, cpu.getStackPointer());
    }


    public void testStackPop() throws MemoryAccessException {
        bus.write(0x1ff, 0x06);
        bus.write(0x1fe, 0x05);
        bus.write(0x1fd, 0x04);
        bus.write(0x1fc, 0x03);
        bus.write(0x1fb, 0x02);
        bus.write(0x1fa, 0x01);
        cpu.setStackPointer(0xf9);

        assertEquals(0x01, cpu.stackPop());
        assertEquals(0xfa, cpu.getStackPointer());

        assertEquals(0x02, cpu.stackPop());
        assertEquals(0xfb, cpu.getStackPointer());

        assertEquals(0x03, cpu.stackPop());
        assertEquals(0xfc, cpu.getStackPointer());

        assertEquals(0x04, cpu.stackPop());
        assertEquals(0xfd, cpu.getStackPointer());

        assertEquals(0x05, cpu.stackPop());
        assertEquals(0xfe, cpu.getStackPointer());

        assertEquals(0x06, cpu.stackPop());
        assertEquals(0xff, cpu.getStackPointer());
    }

    public void testStackPopWrapsAroundToStackBottom() throws MemoryAccessException {
        bus.write(0x1ff, 0x0f); // top of stack
        bus.write(0x100, 0xf0); // bottom of stack
        bus.write(0x101, 0xf1);
        bus.write(0x102, 0xf2);

        cpu.setStackPointer(0xfe);

        assertEquals(0x0f, cpu.stackPop());
        assertEquals(0xff, cpu.getStackPointer());

        assertEquals(0xf0, cpu.stackPop());
        assertEquals(0x00, cpu.getStackPointer());

        assertEquals(0xf1, cpu.stackPop());
        assertEquals(0x01, cpu.getStackPointer());

        assertEquals(0xf2, cpu.stackPop());
        assertEquals(0x02, cpu.getStackPointer());
    }

    public void testStackPeekDoesNotAlterStackPointer() throws MemoryAccessException {
        assertEquals(0x00, cpu.stackPeek());
        assertEquals(0xff, cpu.getStackPointer());

        cpu.stackPush(0x01);
        assertEquals(0x01, cpu.stackPeek());
        assertEquals(0xfe, cpu.getStackPointer());

        cpu.stackPush(0x02);
        assertEquals(0x02, cpu.stackPeek());
        assertEquals(0xfd, cpu.getStackPointer());

        cpu.stackPush(0x03);
        assertEquals(0x03, cpu.stackPeek());
        assertEquals(0xfc, cpu.getStackPointer());

        cpu.stackPush(0x04);
        assertEquals(0x04, cpu.stackPeek());
        assertEquals(0xfb, cpu.getStackPointer());
        assertEquals(0x04, cpu.stackPeek());
        assertEquals(0xfb, cpu.getStackPointer());
        assertEquals(0x04, cpu.stackPeek());
        assertEquals(0xfb, cpu.getStackPointer());
    }

    public void testGetProcessorStatus() {
        // By default, only "interrupt disable" is set.  Remember, bit 5
        // is always '1'.
        assertEquals(0x24, cpu.getProcessorStatus());
        cpu.setCarryFlag();
        assertEquals(0x25, cpu.getProcessorStatus());
        cpu.setZeroFlag();
        assertEquals(0x27, cpu.getProcessorStatus());
        cpu.setDecimalModeFlag();
        assertEquals(0x2f, cpu.getProcessorStatus());
        cpu.setBreakFlag();
        assertEquals(0x3f, cpu.getProcessorStatus());
        cpu.setOverflowFlag();
        assertEquals(0x7f, cpu.getProcessorStatus());
        cpu.setNegativeFlag();
        assertEquals(0xff, cpu.getProcessorStatus());

        cpu.clearCarryFlag();
        assertEquals(0xfe, cpu.getProcessorStatus());
        cpu.clearZeroFlag();
        assertEquals(0xfc, cpu.getProcessorStatus());
        cpu.clearIrqDisableFlag();
        assertEquals(0xf8, cpu.getProcessorStatus());
        cpu.clearDecimalModeFlag();
        assertEquals(0xf0, cpu.getProcessorStatus());
        cpu.clearBreakFlag();
        assertEquals(0xe0, cpu.getProcessorStatus());
        cpu.clearOverflowFlag();
        assertEquals(0xa0, cpu.getProcessorStatus());
        cpu.clearNegativeFlag();
        assertEquals(0x20, cpu.getProcessorStatus());

        cpu.setIrqDisableFlag();
        assertEquals(0x24, cpu.getProcessorStatus());
    }

    public void testSetProcessorStatus() {
        // Default
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getIrqDisableFlag());
        assertFalse(cpu.getDecimalModeFlag());
        assertFalse(cpu.getBreakFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.setProcessorStatus(0x20 | Cpu.P_CARRY);

        assertTrue(cpu.getCarryFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getIrqDisableFlag());
        assertFalse(cpu.getDecimalModeFlag());
        assertFalse(cpu.getBreakFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.setProcessorStatus(0x20 | Cpu.P_CARRY | Cpu.P_NEGATIVE);

        assertTrue(cpu.getCarryFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getIrqDisableFlag());
        assertFalse(cpu.getDecimalModeFlag());
        assertFalse(cpu.getBreakFlag());
        assertFalse(cpu.getOverflowFlag());
        assertTrue(cpu.getNegativeFlag());

        cpu.setProcessorStatus(0x20 | Cpu.P_CARRY | Cpu.P_NEGATIVE | Cpu.P_ZERO);

        assertTrue(cpu.getCarryFlag());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getIrqDisableFlag());
        assertFalse(cpu.getDecimalModeFlag());
        assertFalse(cpu.getBreakFlag());
        assertFalse(cpu.getOverflowFlag());
        assertTrue(cpu.getNegativeFlag());

        cpu.setProcessorStatus(0x20 | Cpu.P_CARRY | Cpu.P_NEGATIVE | Cpu.P_ZERO |
                               Cpu.P_OVERFLOW);

        assertTrue(cpu.getCarryFlag());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getIrqDisableFlag());
        assertFalse(cpu.getDecimalModeFlag());
        assertFalse(cpu.getBreakFlag());
        assertTrue(cpu.getOverflowFlag());
        assertTrue(cpu.getNegativeFlag());

        cpu.setProcessorStatus(0x20 | Cpu.P_CARRY | Cpu.P_NEGATIVE | Cpu.P_ZERO |
                               Cpu.P_OVERFLOW | Cpu.P_BREAK);

        assertTrue(cpu.getCarryFlag());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getIrqDisableFlag());
        assertFalse(cpu.getDecimalModeFlag());
        assertTrue(cpu.getBreakFlag());
        assertTrue(cpu.getOverflowFlag());
        assertTrue(cpu.getNegativeFlag());


        cpu.setProcessorStatus(0x20 | Cpu.P_CARRY | Cpu.P_NEGATIVE | Cpu.P_ZERO |
                               Cpu.P_OVERFLOW | Cpu.P_BREAK | Cpu.P_DECIMAL);

        assertTrue(cpu.getCarryFlag());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getIrqDisableFlag());
        assertTrue(cpu.getDecimalModeFlag());
        assertTrue(cpu.getBreakFlag());
        assertTrue(cpu.getOverflowFlag());
        assertTrue(cpu.getNegativeFlag());

        cpu.setProcessorStatus(0x20 | Cpu.P_CARRY | Cpu.P_NEGATIVE | Cpu.P_ZERO |
                               Cpu.P_OVERFLOW | Cpu.P_BREAK | Cpu.P_DECIMAL |
                               Cpu.P_IRQ_DISABLE);

        assertTrue(cpu.getCarryFlag());
        assertTrue(cpu.getZeroFlag());
        assertTrue(cpu.getIrqDisableFlag());
        assertTrue(cpu.getDecimalModeFlag());
        assertTrue(cpu.getBreakFlag());
        assertTrue(cpu.getOverflowFlag());
        assertTrue(cpu.getNegativeFlag());

        cpu.setProcessorStatus(0x20);

        assertFalse(cpu.getCarryFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getIrqDisableFlag());
        assertFalse(cpu.getDecimalModeFlag());
        assertFalse(cpu.getBreakFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.setProcessorStatus(0x00);

        assertFalse(cpu.getCarryFlag());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getIrqDisableFlag());
        assertFalse(cpu.getDecimalModeFlag());
        assertFalse(cpu.getBreakFlag());
        assertFalse(cpu.getOverflowFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    public void testIrq() throws Exception {
        // Ensure the IRQ disable flag is cleared
        cpu.clearIrqDisableFlag();

        // Set the IRQ vector
        bus.write(0xffff, 0x12);
        bus.write(0xfffe, 0x34);

        // Create an IRQ handler at 0x1234
        cpu.setProgramCounter(0x1234);
        bus.loadProgram(0xa9, 0x33,  // LDA #$33
                        0x69, 0x01); // ADC #$01

        cpu.setProgramCounter(0x0200);
        // Create a little program at 0x0200
        bus.loadProgram(0x18,        // CLC
                        0xa9, 0x01,  // LDA #$00
                        0x69, 0x01); // ADC #$01

        cpu.step();
        assertEquals(0x0201, cpu.getProgramCounter()); // First instruction executed.
        assertEquals(0x00, cpu.getAccumulator());

        cpu.step();
        assertEquals(0x0203, cpu.getProgramCounter());
        assertEquals(0x01, cpu.getAccumulator());

        cpu.assertIrq();

        cpu.step();
        assertTrue(cpu.getIrqDisableFlag()); // Should have been set by the IRQ
        assertEquals(0x1236, cpu.getProgramCounter());
        assertEquals(0x33, cpu.getAccumulator());

        // Be sure that the IRQ line is no longer held low
        assertFalse(cpu.getCpuState().irqAsserted);
    }

    public void testIrqDoesNotSetBRK() throws Exception {
        // Ensure the IRQ disable flag is cleared
        cpu.clearIrqDisableFlag();

        // Set the IRQ vector
        bus.write(0xffff, 0x12);
        bus.write(0xfffe, 0x34);

        // Create an IRQ handler at 0x1234
        cpu.setProgramCounter(0x1234);
        bus.loadProgram(0xa9, 0x33,  // LDA #$33
                        0x69, 0x01); // ADC #$01

        cpu.setProgramCounter(0x0200);
        // Create a little program at 0x0200
        bus.loadProgram(0x18,        // CLC
                        0xa9, 0x01,  // LDA #$00
                        0x69, 0x01); // ADC #$01

        cpu.step();
        assertEquals(0x0201, cpu.getProgramCounter()); // First instruction executed.
        assertEquals(0x00, cpu.getAccumulator());

        cpu.step();
        assertEquals(0x0203, cpu.getProgramCounter());
        assertEquals(0x01, cpu.getAccumulator());

        cpu.assertIrq();

        cpu.step();
        assertTrue(cpu.getIrqDisableFlag()); // Should have been set by the IRQ
        assertFalse(cpu.getBreakFlag());

    }

    public void testIrqHonorsIrqDisabledFlag() throws Exception {
        // Ensure the IRQ disable flag is set
        cpu.setIrqDisableFlag();

        // Set the IRQ vector
        bus.write(0xffff, 0x12);
        bus.write(0xfffe, 0x34);

        // Create an IRQ handler at 0x1234
        cpu.setProgramCounter(0x1234);
        bus.loadProgram(0xa9, 0x33,  // LDA #$33
                        0x69, 0x01); // ADC #$01

        cpu.setProgramCounter(0x0200);
        // Create a little program at 0x0200
        bus.loadProgram(0x18,        // CLC
                        0xa9, 0x01,  // LDA #$00
                        0x69, 0x01); // ADC #$01

        cpu.step();
        assertEquals(0x0201, cpu.getProgramCounter()); // First instruction executed.
        assertEquals(0x00, cpu.getAccumulator());

        cpu.step();
        assertEquals(0x0203, cpu.getProgramCounter());
        assertEquals(0x01, cpu.getAccumulator());

        cpu.assertIrq(); // Should be ignored, because the disable flag is set.

        cpu.step();
        assertTrue(cpu.getIrqDisableFlag()); // Should have been left alone.
        assertEquals(0x0205, cpu.getProgramCounter());
        assertEquals(0x02, cpu.getAccumulator());
    }

    public void testIrqPushesCorrectReturnAddressOntoStack() throws Exception {
        // Ensure the IRQs are enabled
        cpu.clearIrqDisableFlag();

        // Set the IRQ vector
        bus.write(0xffff, 0x10);
        bus.write(0xfffe, 0x00);

        // Create an IRQ handler at 0x1000 that just RTIs.
        cpu.setProgramCounter(0x1000);
        bus.loadProgram(0xea, 0xea, 0x40); // NOP, NOP, RTI

        cpu.setProgramCounter(0x0200);

        // Create a little program at 0x0200 with three instruction sizes.
        bus.loadProgram(0x18,               // CLC
                        0xa9, 0x01,         // LDA #$01
                        0x6d, 0x06, 0x02,   // ADC $0207
                        0x00,               // BRK
                        0x03);              // $03 (data @ $0206)

        cpu.step(); // CLC
        assertEquals(0x0201, cpu.getProgramCounter()); // First instruction executed.
        assertEquals(0x00,   cpu.getAccumulator());

        cpu.assertIrq();
        cpu.step(); // NOP
        assertEquals(0x1001, cpu.getProgramCounter());
        cpu.step(); // NOP
        assertEquals(0x1002, cpu.getProgramCounter());
        cpu.step(); // RTI: PC -> 0x0201
        assertEquals(0x0201, cpu.getProgramCounter());
        cpu.step(); // LDA $#01
        assertEquals(0x0203, cpu.getProgramCounter());

        cpu.assertIrq();
        cpu.step(); // NOP
        assertEquals(0x1001, cpu.getProgramCounter());
        cpu.step(); // NOP
        assertEquals(0x1002, cpu.getProgramCounter());
        cpu.step(); // RTI: PC -> 0x0203
        assertEquals(0x0203, cpu.getProgramCounter());
    }

    public void testNmi() throws Exception {
        // Set the NMI vector to 0x1000
        bus.write(0xfffb, 0x10);
        bus.write(0xfffa, 0x00);

        // Create an NMI handler at 0x1000
        cpu.setProgramCounter(0x1000);
        bus.loadProgram(0xa9, 0x33,  // LDA #$33
                        0x69, 0x01); // ADC #$01

        // Create a little program at 0x0200
        cpu.setProgramCounter(0x0200);
        bus.loadProgram(0x18,        // CLC
                        0xa9, 0x01,  // LDA #$00
                        0x69, 0x01); // ADC #$01

        cpu.step();
        assertEquals(0x0201, cpu.getProgramCounter()); // First instruction executed.
        assertEquals(0x00, cpu.getAccumulator());

        cpu.step();
        assertEquals(0x0203, cpu.getProgramCounter());
        assertEquals(0x01, cpu.getAccumulator());

        cpu.assertNmi();

        cpu.step();
        assertTrue(cpu.getIrqDisableFlag()); // Should have been set by the NMI
        assertEquals(0x1002, cpu.getProgramCounter());
        assertEquals(0x33, cpu.getAccumulator());

        // Be sure that the NMI line is no longer held low
        assertFalse(cpu.getCpuState().nmiAsserted);
    }

    public void testNmiDoesNotSetBRK() throws Exception {
        // Set the NMI vector to 0x1000
        bus.write(0xfffb, 0x10);
        bus.write(0xfffa, 0x00);

        // Create an NMI handler at 0x1000
        cpu.setProgramCounter(0x1000);
        bus.loadProgram(0xa9, 0x33,  // LDA #$33
                        0x69, 0x01); // ADC #$01

        // Create a little program at 0x0200
        cpu.setProgramCounter(0x0200);
        bus.loadProgram(0x18,        // CLC
                        0xa9, 0x01,  // LDA #$00
                        0x69, 0x01); // ADC #$01

        cpu.step();
        assertEquals(0x0201, cpu.getProgramCounter()); // First instruction executed.
        assertEquals(0x00, cpu.getAccumulator());

        cpu.step();
        assertEquals(0x0203, cpu.getProgramCounter());
        assertEquals(0x01, cpu.getAccumulator());

        cpu.assertNmi();

        cpu.step();
        assertFalse(cpu.getBreakFlag());

}

    public void testNmiIgnoresIrqDisableFlag() throws Exception {
        // Set the IRQ disable flag, which should be ignored by the NMI
        cpu.setIrqDisableFlag();

        // Set the NMI vector to 0x1000
        bus.write(0xfffb, 0x10);
        bus.write(0xfffa, 0x00);

        // Create an NMI handler at 0x1000
        cpu.setProgramCounter(0x1000);
        bus.loadProgram(0xa9, 0x33,  // LDA #$33
                        0x69, 0x01); // ADC #$01

        // Create a little program at 0x0200
        cpu.setProgramCounter(0x0200);
        bus.loadProgram(0x18,        // CLC
                        0xa9, 0x01,  // LDA #$00
                        0x69, 0x01); // ADC #$01

        cpu.step();
        assertEquals(0x0201, cpu.getProgramCounter()); // First instruction executed.
        assertEquals(0x00, cpu.getAccumulator());

        cpu.step();
        assertEquals(0x0203, cpu.getProgramCounter());
        assertEquals(0x01, cpu.getAccumulator());

        cpu.assertNmi();

        cpu.step();
        assertTrue(cpu.getIrqDisableFlag()); // Should have been set by the NMI
        assertEquals(0x1002, cpu.getProgramCounter());
        assertEquals(0x33, cpu.getAccumulator());

        // Be sure that the NMI line is no longer held low
        assertFalse(cpu.getCpuState().nmiAsserted);
    }


    public void testAddress() {
        assertEquals(0xf1ea, Utils.address(0xea, 0xf1));
        assertEquals(0x00ea, Utils.address(0xea, 0x00));
        assertEquals(0xf100, Utils.address(0x00, 0xf1));
        assertEquals(0x1234, Utils.address(0x34, 0x12));
        assertEquals(0xffff, Utils.address(0xff, 0xff));
    }

    public void testZpxAddress() {
        cpu.setXRegister(0x00);
        assertEquals(0x10, cpu.zpxAddress(0x10));
        cpu.setXRegister(0x10);
        assertEquals(0x20, cpu.zpxAddress(0x10));
        cpu.setXRegister(0x25);
        assertEquals(0x35, cpu.zpxAddress(0x10));
        cpu.setXRegister(0xf5);
        assertEquals(0x05, cpu.zpxAddress(0x10));

        cpu.setXRegister(0x00);
        assertEquals(0x80, cpu.zpxAddress(0x80));
        cpu.setXRegister(0x10);
        assertEquals(0x90, cpu.zpxAddress(0x80));
        cpu.setXRegister(0x25);
        assertEquals(0xa5, cpu.zpxAddress(0x80));
        cpu.setXRegister(0x95);
        assertEquals(0x15, cpu.zpxAddress(0x80));
    }

    public void testZpyAddress() {
        cpu.setYRegister(0x00);
        assertEquals(0x10, cpu.zpyAddress(0x10));
        cpu.setYRegister(0x10);
        assertEquals(0x20, cpu.zpyAddress(0x10));
        cpu.setYRegister(0x25);
        assertEquals(0x35, cpu.zpyAddress(0x10));
        cpu.setYRegister(0xf5);
        assertEquals(0x05, cpu.zpyAddress(0x10));

        cpu.setYRegister(0x00);
        assertEquals(0x80, cpu.zpyAddress(0x80));
        cpu.setYRegister(0x10);
        assertEquals(0x90, cpu.zpyAddress(0x80));
        cpu.setYRegister(0x25);
        assertEquals(0xa5, cpu.zpyAddress(0x80));
        cpu.setYRegister(0x95);
        assertEquals(0x15, cpu.zpyAddress(0x80));
    }

    // Test for GitHub symon issue #9, "LSR can yield wrong result"
    public void testRightShiftMasksBitsCorrectly() throws Exception {
        // Illegal value, the accumulator should only care about the low 8 bytes.
        // I'm a little uncomfortable with this test because really, setAccumulator should
        // defensively mask the value, but does not. Is this relying on a bug to test another bug?
        cpu.setAccumulator(0xff8);
        // Sanity check, in case I ever change my mind on setAccumulator's behavior
        assertEquals(0xff8, cpu.getAccumulator());

        bus.loadProgram(0x4a,     // LSR
                        0x4a);    // LSR

        cpu.step();
        assertEquals(0x7C, cpu.getAccumulator());

        cpu.step();
        assertEquals(0x3E, cpu.getAccumulator());
    }
}
