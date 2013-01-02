package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;
import org.junit.*;

import static org.junit.Assert.*;

public class CpuImpliedModeTest {

    protected Cpu cpu;
    protected Bus bus;
    protected Memory mem;

    @Before
    public void setUp() throws MemoryRangeException, MemoryAccessException {
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
     * BRK - $00
     * CLC - $18
     * CLD - $d8
     * CLI - $58
     * CLV - $B8
     *
     * DEX - $ca
     * DEY - $88
     * INX - $e8
     * INY - $c8
     * NOP - $ea
     *
     * PHA - $48
     * PHP - $08
     * PLA - $68
     * PLP - $28
     * RTI - $40
     *
     * RTS - $60
     * SEC - $38
     * SED - $f8
     * SEI - $78
     * TAX - $aa
     *
     * TAY - $a8
     * TSX - $ba
     * TXA - $8a
     * TXS - $9a
     * TYA - $98
     */

    /* BRK Tests - 0x00 */

    @Test
    public void test_BRK() throws MemoryAccessException {
        cpu.setCarryFlag();
        cpu.setOverflowFlag();
        assertEquals(0x20 | Cpu.P_CARRY | Cpu.P_OVERFLOW,
                     cpu.getProcessorStatus());
        assertEquals(0x00, cpu.stackPeek());
        assertFalse(cpu.getBreakFlag());
        assertEquals(0x0200, cpu.getProgramCounter());
        assertEquals(0xff, cpu.getStackPointer());

        // Set the IRQ vector
        bus.write(0xffff, 0x12);
        bus.write(0xfffe, 0x34);

        bus.loadProgram(0xea,  // NOP
                        0xea,  // NOP
                        0xea,  // NOP
                        0x00,  // BRK
                        0xea,  // NOP
                        0xea); // NOP

        cpu.step(3); // Three NOP instructions

        assertEquals(0x203, cpu.getProgramCounter());

        cpu.step(); // Triggers the BRK

        // Was at PC = 0x204.  PC+1 should now be on the stack
        assertEquals(0x02, bus.read(0x1ff)); // PC high byte
        assertEquals(0x05, bus.read(0x1fe)); // PC low byte
        assertEquals(0x20 | Cpu.P_CARRY | Cpu.P_OVERFLOW | Cpu.P_BREAK,
                     bus.read(0x1fd));       // Processor Status, with B set

        // Interrupt vector held 0x1234, so we should be there.
        assertEquals(0x1234, cpu.getProgramCounter());
        assertEquals(0xfc, cpu.getStackPointer());

        // B and I flags should have been set on P
        assertEquals(0x20 | Cpu.P_CARRY | Cpu.P_OVERFLOW | Cpu.P_BREAK |
                             Cpu.P_IRQ_DISABLE,
                     cpu.getProcessorStatus());
    }

    @Test
    public void test_BRK_HonorsIrqDisableFlag() throws MemoryAccessException {
        cpu.setIrqDisableFlag();

        bus.loadProgram(0xea,
                        0xea,
                        0xea,
                        0x00,
                        0xea,
                        0xea);

        cpu.step(3); // Three NOP instructions

        assertEquals(0x203, cpu.getProgramCounter());

        // Triggers the BRK, which should do nothing because
        // of the Interrupt Disable flag
        cpu.step();

        // Reset to original contents of PC
        assertEquals(0x0204, cpu.getProgramCounter());
        // Empty stack
        assertEquals(0xff, cpu.getStackPointer());

        cpu.step(2); // Two more NOPs

        // Reset to original contents of PC
        assertEquals(0x0206, cpu.getProgramCounter());
        // Empty stack
        assertEquals(0xff, cpu.getStackPointer());
    }

    /* CLC - Clear Carry Flag - $18 */
    @Test
    public void test_CLC() throws MemoryAccessException {
        cpu.setCarryFlag();
        assertTrue(cpu.getCarryFlag());

        bus.loadProgram(0x18);
        cpu.step();

        assertFalse(cpu.getCarryFlag());
    }

    /* CLD - Clear Decimal Mode Flag - $d8 */
    @Test
    public void test_CLD() throws MemoryAccessException {
        cpu.setDecimalModeFlag();
        assertTrue(cpu.getDecimalModeFlag());

        bus.loadProgram(0xd8);
        cpu.step();

        assertFalse(cpu.getDecimalModeFlag());
    }

    /* CLI - Clear Interrupt Disabled Flag - $58 */
    @Test
    public void test_CLI() throws MemoryAccessException {
        cpu.setIrqDisableFlag();
        assertTrue(cpu.getIrqDisableFlag());

        bus.loadProgram(0x58);
        cpu.step();

        assertFalse(cpu.getIrqDisableFlag());
    }

    /* CLV - Clear Overflow Flag - $b8 */
    @Test
    public void test_CLV() throws MemoryAccessException {
        cpu.setOverflowFlag();
        assertTrue(cpu.getOverflowFlag());

        bus.loadProgram(0xb8);
        cpu.step();

        assertFalse(cpu.getOverflowFlag());
    }

    /* DEX - Decrement the X register - $ca */
    @Test
    public void test_DEX() throws MemoryAccessException {
        bus.loadProgram(0xca);
        cpu.setXRegister(0x02);
        cpu.step();
        assertEquals(0x01, cpu.getXRegister());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_DEX_SetsZeroFlagWhenZero() throws MemoryAccessException {
        bus.loadProgram(0xca);
        cpu.setXRegister(0x01);
        cpu.step();
        assertEquals(0x00, cpu.getXRegister());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_DEX_SetsNegativeFlagWhen() throws MemoryAccessException {
        bus.loadProgram(0xca);
        cpu.step();
        assertEquals(0xff, cpu.getXRegister());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
    }

    /* DEY - Decrement the Y register - $88 */
    @Test
    public void test_DEY() throws MemoryAccessException {
        bus.loadProgram(0x88);
        cpu.setYRegister(0x02);
        cpu.step();
        assertEquals(0x01, cpu.getYRegister());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_DEY_SetsZeroFlagWhenZero() throws MemoryAccessException {
        bus.loadProgram(0x88);
        cpu.setYRegister(0x01);
        cpu.step();
        assertEquals(0x00, cpu.getYRegister());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_DEY_SetsNegativeFlagWhen() throws MemoryAccessException {
        bus.loadProgram(0x88);
        cpu.step();
        assertEquals(0xff, cpu.getYRegister());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
    }

    /* INX - Increment the X register - $e8 */
    @Test
    public void test_INX() throws MemoryAccessException {
        bus.loadProgram(0xe8);
        cpu.step();
        assertEquals(0x01, cpu.getXRegister());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_INX_SetsNegativeFlagWhenNegative() throws MemoryAccessException {
        bus.loadProgram(0xe8);
        cpu.setXRegister(0x7f);
        cpu.step();
        assertEquals(0x80, cpu.getXRegister());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
    }

    @Test
    public void test_INX_SetsZeroFlagWhenZero() throws MemoryAccessException {
        bus.loadProgram(0xe8);
        cpu.setXRegister(0xff);
        cpu.step();
        assertEquals(0x00, cpu.getXRegister());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    /* INY - Increment the Y register - $c8 */
    @Test
    public void test_INY() throws MemoryAccessException {
        bus.loadProgram(0xc8);
        cpu.step();
        assertEquals(0x01, cpu.getYRegister());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_INY_SetsNegativeFlagWhenNegative() throws MemoryAccessException {
        bus.loadProgram(0xc8);
        cpu.setYRegister(0x7f);
        cpu.step();
        assertEquals(0x80, cpu.getYRegister());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
    }

    @Test
    public void test_INY_SetsZeroFlagWhenZero() throws MemoryAccessException {
        bus.loadProgram(0xc8);
        cpu.setYRegister(0xff);
        cpu.step();
        assertEquals(0x00, cpu.getYRegister());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    /* NOP - No Operation - $ea */
    @Test
    public void test_NOP() throws MemoryAccessException {
        bus.loadProgram(0xea);
        cpu.step();
        // Should just not change anything except PC
        assertEquals(0, cpu.getAccumulator());
        assertEquals(0, cpu.getXRegister());
        assertEquals(0, cpu.getYRegister());
        assertEquals(0x201, cpu.getProgramCounter());
        assertEquals(0xff, cpu.getStackPointer());
        assertEquals(0x20, cpu.getProcessorStatus());
    }

    /* PHA - Push Accumulator - $48 */
    @Test
    public void test_PHA() throws MemoryAccessException {
        bus.loadProgram(0x48);
        cpu.setAccumulator(0x3a);
        cpu.step();
        assertEquals(0xfe, cpu.getStackPointer());
        assertEquals(0x3a, cpu.stackPeek());
    }

    /* PHP - Push Processor Status - $08 */
    @Test
    public void test_PHP() throws MemoryAccessException {
        bus.loadProgram(0x08);
        cpu.setProcessorStatus(0x27);
        cpu.step();
        assertEquals(0xfe, cpu.getStackPointer());
        // PHP should have set the BREAK flag _on the stack_ (but not in the CPU)
        assertEquals(0x37, cpu.stackPeek());
        assertEquals(0x27, cpu.getProcessorStatus());
    }

    /* PLA - Pul Accumulator - $68 */
    @Test
    public void test_PLA() throws MemoryAccessException {
        cpu.stackPush(0x32);
        bus.loadProgram(0x68);
        cpu.step();
        assertEquals(0x32, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertFalse(cpu.getZeroFlag());
    }

    @Test
    public void test_PLA_SetsZeroIfAccumulatorIsZero() throws MemoryAccessException {
        cpu.stackPush(0x00);
        bus.loadProgram(0x68);
        cpu.step();
        assertEquals(0x00, cpu.getAccumulator());
        assertFalse(cpu.getNegativeFlag());
        assertTrue(cpu.getZeroFlag());
    }

    @Test
    public void test_PLA_SetsNegativeIfAccumulatorIsNegative() throws MemoryAccessException {
        cpu.stackPush(0xff);
        bus.loadProgram(0x68);
        cpu.step();
        assertEquals(0xff, cpu.getAccumulator());
        assertTrue(cpu.getNegativeFlag());
        assertFalse(cpu.getZeroFlag());
    }

    /* PLP - Pull Processor Status - $28 */
    @Test
    public void test_PLP() throws MemoryAccessException {
        cpu.stackPush(0x2f);
        bus.loadProgram(0x28);
        cpu.step();
        assertEquals(0x2f, cpu.getProcessorStatus());
    }

    /* RTI - Return from Interrupt - $40 */
    @Test
    public void test_RTI() throws MemoryAccessException {
        cpu.stackPush(0x0f); // PC hi
        cpu.stackPush(0x11); // PC lo
        cpu.stackPush(0x29); // status

        bus.loadProgram(0x40);
        cpu.step();

        assertEquals(0x0f11, cpu.getProgramCounter());
        assertEquals(0x29, cpu.getProcessorStatus());
    }

    /* RTS - Return from Subroutine - $60 */
    @Test
    public void test_RTS() throws MemoryAccessException {
        cpu.stackPush(0x0f); // PC hi
        cpu.stackPush(0x11); // PC lo

        bus.loadProgram(0x60);
        cpu.step();

        assertEquals(0x0f12, cpu.getProgramCounter());
        assertEquals(0x20, cpu.getProcessorStatus());
    }

    /* SEC - Set Carry Flag - $38 */
    @Test
    public void test_SEC() throws MemoryAccessException {
        bus.loadProgram(0x38);
        cpu.step();
        assertTrue(cpu.getCarryFlag());
    }

    /* SED - Set Decimal Mode Flag - $f8 */
    @Test
    public void test_SED() throws MemoryAccessException {
        bus.loadProgram(0xf8);
        cpu.step();
        assertTrue(cpu.getDecimalModeFlag());
    }

    /* SEI - Set Interrupt Disable Flag - $78 */
    @Test
    public void test_SEI() throws MemoryAccessException {
        bus.loadProgram(0x78);
        cpu.step();
        assertTrue(cpu.getIrqDisableFlag());
    }

    /* TAX - Transfer Accumulator to X Register - $aa */
    @Test
    public void test_TAX() throws MemoryAccessException {
        cpu.setAccumulator(0x32);
        bus.loadProgram(0xaa);
        cpu.step();
        assertEquals(0x32, cpu.getXRegister());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_TAX_SetsZeroFlagIfXIsZero() throws MemoryAccessException {
        cpu.setAccumulator(0x00);
        bus.loadProgram(0xaa);
        cpu.step();
        assertEquals(0x00, cpu.getXRegister());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_TAX_SetsNegativeFlagIfXIsNegative() throws MemoryAccessException {
        cpu.setAccumulator(0xff);
        bus.loadProgram(0xaa);
        cpu.step();
        assertEquals(0xff, cpu.getXRegister());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
    }

    /* TAY - Transfer Accumulator to Y Register - $a8 */
    @Test
    public void test_TAY() throws MemoryAccessException {
        cpu.setAccumulator(0x32);
        bus.loadProgram(0xa8);
        cpu.step();
        assertEquals(0x32, cpu.getYRegister());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_TAY_SetsZeroFlagIfYIsZero() throws MemoryAccessException {
        cpu.setAccumulator(0x00);
        bus.loadProgram(0xa8);
        cpu.step();
        assertEquals(0x00, cpu.getYRegister());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_TAY_SetsNegativeFlagIfYIsNegative() throws MemoryAccessException {
        cpu.setAccumulator(0xff);
        bus.loadProgram(0xa8);
        cpu.step();
        assertEquals(0xff, cpu.getYRegister());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
    }

    /* TSX - Transfer Stack Pointer to X Register - $ba */
    @Test
    public void test_TSX() throws MemoryAccessException {
        cpu.setStackPointer(0x32);
        bus.loadProgram(0xba);
        cpu.step();
        assertEquals(0x32, cpu.getXRegister());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_TSX_SetsZeroFlagIfXIsZero() throws MemoryAccessException {
        cpu.setStackPointer(0x00);
        bus.loadProgram(0xba);
        cpu.step();
        assertEquals(0x00, cpu.getXRegister());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_TSX_SetsNegativeFlagIfXIsNegative() throws MemoryAccessException {
        cpu.setStackPointer(0xff);
        bus.loadProgram(0xba);
        cpu.step();
        assertEquals(0xff, cpu.getXRegister());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
    }

    /* TXA - Transfer X Register to Accumulator - $8a */
    @Test
    public void test_TXA() throws MemoryAccessException {
        cpu.setXRegister(0x32);
        bus.loadProgram(0x8a);
        cpu.step();
        assertEquals(0x32, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_TXA_SetsZeroFlagIfAccumulatorIsZero() throws MemoryAccessException {
        cpu.setXRegister(0x00);
        bus.loadProgram(0x8a);
        cpu.step();
        assertEquals(0x00, cpu.getAccumulator());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_TXA_SetsNegativeFlagIfAccumulatorIsNegative() throws MemoryAccessException {
        cpu.setXRegister(0xff);
        bus.loadProgram(0x8a);
        cpu.step();
        assertEquals(0xff, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
    }

    /* TXS - Transfer X Register to Stack Pointer - $9a */
    @Test
    public void test_TXS() throws MemoryAccessException {
        cpu.setXRegister(0x32);
        bus.loadProgram(0x9a);
        cpu.step();
        assertEquals(0x32, cpu.getStackPointer());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_TXS_DoesNotAffectProcessorStatus() throws MemoryAccessException {
        cpu.setXRegister(0x00);
        bus.loadProgram(0x9a);
        cpu.step();
        assertEquals(0x00, cpu.getStackPointer());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());

        cpu.setXRegister(0x80);
        bus.loadProgram(0x9a);
        cpu.step();
        assertEquals(0x80, cpu.getStackPointer());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    /* TYA - Transfer Y Register to Accumulator - $98 */
    @Test
    public void test_TYA() throws MemoryAccessException {
        cpu.setYRegister(0x32);
        bus.loadProgram(0x98);
        cpu.step();
        assertEquals(0x32, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_TYA_SetsZeroFlagIfAccumulatorIsZero() throws MemoryAccessException {
        cpu.setYRegister(0x00);
        bus.loadProgram(0x98);
        cpu.step();
        assertEquals(0x00, cpu.getAccumulator());
        assertTrue(cpu.getZeroFlag());
        assertFalse(cpu.getNegativeFlag());
    }

    @Test
    public void test_TYA_SetsNegativeFlagIfAccumulatorIsNegative() throws MemoryAccessException {
        cpu.setYRegister(0xff);
        bus.loadProgram(0x98);
        cpu.step();
        assertEquals(0xff, cpu.getAccumulator());
        assertFalse(cpu.getZeroFlag());
        assertTrue(cpu.getNegativeFlag());
    }
}