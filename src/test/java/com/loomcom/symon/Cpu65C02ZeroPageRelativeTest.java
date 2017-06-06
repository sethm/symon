package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryAccessException;
import junit.framework.*;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for Zero Page Indirect addressing mode, found on some instructions
 * in the 65C02 and 65816
 */
public class Cpu65C02ZeroPageRelativeTest extends TestCase {
    protected Cpu    cpu;
    protected Bus    bus;
    protected Memory mem;

    private void makeCmosCpu() throws Exception {
        makeCpu(InstructionTable.CpuBehavior.CMOS_6502);
    }

    private void makeNmosCpu() throws Exception {
        makeCpu(InstructionTable.CpuBehavior.NMOS_6502);
    }

    private void makeCpu(InstructionTable.CpuBehavior behavior) throws Exception {
        this.cpu = new Cpu(behavior);
        this.bus = new Bus(0x0000, 0xffff);
        this.mem = new Memory(0x0000, 0xffff);
        bus.addCpu(cpu);
        bus.addDevice(mem);

        // Load the reset vector.
        bus.write(0xfffc, Bus.DEFAULT_LOAD_ADDRESS & 0x00ff);
        bus.write(0xfffd, (Bus.DEFAULT_LOAD_ADDRESS & 0xff00) >>> 8);

        cpu.reset();
    }
    public void test_BRA() throws Exception {
        makeCmosCpu();
        // Positive Offset
        bus.loadProgram(0x80, 0x05);  // 65C02 BRA $05 ; *=$0202+$05 ($0207)
        cpu.step();
        assertEquals(0x207, cpu.getProgramCounter());

        // Negative Offset
        cpu.reset();
        bus.loadProgram(0x80, 0xfb);  // 65C02 BRA $fb ; *=$0202-$05 ($01fd)
        cpu.step();
        assertEquals(0x1fd, cpu.getProgramCounter());
    }

    public void test_BRArequiresCmosCpu() throws Exception {
        makeNmosCpu();
        // Should be a NOOP on NMOS and end up at $0202
        // Positive Offset
        bus.loadProgram(0x80, 0x05);  // BRA $05 ; *=$0202+$05 ($0207)
        cpu.step();
        assertEquals(0x202, cpu.getProgramCounter());

        // Negative Offset
        cpu.reset();
        bus.loadProgram(0x80, 0xfb);  // BRA $fb ; *=$0202-$05 ($01fd)
        cpu.step();
        assertEquals(0x202, cpu.getProgramCounter());
    }

    public void test_BBR() throws Exception {
        makeCmosCpu();

        /* BBR0 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0xff);
        bus.loadProgram(0x0f, 0x0a, 0x05);  // 65C02 BBR0 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0xfe);
        bus.loadProgram(0x0f, 0x0a, 0x05);  // 65C02 BBR0 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0xfe);
        bus.loadProgram(0x0f, 0x0a, 0xfb);  // 65C02 BBR0 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

        /* BBR1 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0xff);
        bus.loadProgram(0x1f, 0x0a, 0x05);  // 65C02 BBR1 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0xfd);
        bus.loadProgram(0x1f, 0x0a, 0x05);  // 65C02 BBR1 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0xfd);
        bus.loadProgram(0x1f, 0x0a, 0xfb);  // 65C02 BBR1 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

        /* BBR2 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0xff);
        bus.loadProgram(0x2f, 0x0a, 0x05);  // 65C02 BBR2 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0xfb);
        bus.loadProgram(0x2f, 0x0a, 0x05);  // 65C02 BBR2 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0xfb);
        bus.loadProgram(0x2f, 0x0a, 0xfb);  // 65C02 BBR2 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

        /* BBR3 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0xff);
        bus.loadProgram(0x3f, 0x0a, 0x05);  // 65C02 BBR3 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0xf7);
        bus.loadProgram(0x3f, 0x0a, 0x05);  // 65C02 BBR3 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0xf7);
        bus.loadProgram(0x3f, 0x0a, 0xfb);  // 65C02 BBR3 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

        /* BBR4 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0xff);
        bus.loadProgram(0x4f, 0x0a, 0x05);  // 65C02 BBR4 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0xef);
        bus.loadProgram(0x4f, 0x0a, 0x05);  // 65C02 BBR4 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0xef);
        bus.loadProgram(0x4f, 0x0a, 0xfb);  // 65C02 BBR4 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

        /* BBR5 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0xff);
        bus.loadProgram(0x5f, 0x0a, 0x05);  // 65C02 BBR5 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0xdf);
        bus.loadProgram(0x5f, 0x0a, 0x05);  // 65C02 BBR5 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0xdf);
        bus.loadProgram(0x5f, 0x0a, 0xfb);  // 65C02 BBR5 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

        /* BBR6 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0xff);
        bus.loadProgram(0x6f, 0x0a, 0x05);  // 65C02 BBR6 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0xbf);
        bus.loadProgram(0x6f, 0x0a, 0x05);  // 65C02 BBR6 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0xbf);
        bus.loadProgram(0x6f, 0x0a, 0xfb);  // 65C02 BBR6 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

        /* BBR7 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0xff);
        bus.loadProgram(0x7f, 0x0a, 0x05);  // 65C02 BBR7 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0x7f);
        bus.loadProgram(0x7f, 0x0a, 0x05);  // 65C02 BBR7 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0x7f);
        bus.loadProgram(0x7f, 0x0a, 0xfb);  // 65C02 BBR7 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

    }

    public void test_BBRNeedsCmosCpu() throws Exception {
        makeNmosCpu();

        /* BBR0 */
        bus.write(0x0a,0xfe);
        bus.loadProgram(0x0f, 0x0a, 0x05);  // 65C02 BBR0 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

        /* BBR1 */
        cpu.reset();
        bus.write(0x0a,0xfd);
        bus.loadProgram(0x1f, 0x0a, 0x05);  // 65C02 BBR1 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

        /* BBR2 */
        cpu.reset();
        bus.write(0x0a,0xfb);
        bus.loadProgram(0x2f, 0x0a, 0x05);  // 65C02 BBR2 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

        /* BBR3 */
        cpu.reset();
        bus.write(0x0a,0xf7);
        bus.loadProgram(0x3f, 0x0a, 0x05);  // 65C02 BBR3 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

        /* BBR4 */
        cpu.reset();
        bus.write(0x0a,0xef);
        bus.loadProgram(0x4f, 0x0a, 0x05);  // 65C02 BBR4 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

        /* BBR5 */
        cpu.reset();
        bus.write(0x0a,0xdf);
        bus.loadProgram(0x5f, 0x0a, 0x05);  // 65C02 BBR5 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

        /* BBR6 */
        cpu.reset();
        bus.write(0x0a,0xbf);
        bus.loadProgram(0x6f, 0x0a, 0x05);  // 65C02 BBR6 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

        /* BBR7 */
        cpu.reset();
        bus.write(0x0a,0x7f);
        bus.loadProgram(0x7f, 0x0a, 0x05);  // 65C02 BBR7 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

    }

    public void test_BBS() throws Exception {
        makeCmosCpu();

        /* BBS0 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0x00);
        bus.loadProgram(0x8f, 0x0a, 0x05);  // 65C02 BBS0 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0x01);
        bus.loadProgram(0x8f, 0x0a, 0x05);  // 65C02 BBS0 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0x01);
        bus.loadProgram(0x8f, 0x0a, 0xfb);  // 65C02 BBS0 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

        /* BBS1 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0x00);
        bus.loadProgram(0x9f, 0x00, 0x05);  // 65C02 BBS1 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0x02);
        bus.loadProgram(0x9f, 0x0a, 0x05);  // 65C02 BBS1 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0x02);
        bus.loadProgram(0x9f, 0x0a, 0xfb);  // 65C02 BBS1 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

        /* BBS2 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0x00);
        bus.loadProgram(0xaf, 0x0a, 0x05);  // 65C02 BBS2 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0x04);
        bus.loadProgram(0xaf, 0x0a, 0x05);  // 65C02 BBS2 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0x04);
        bus.loadProgram(0xaf, 0x0a, 0xfb);  // 65C02 BBS2 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

        /* BBS3 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0x00);
        bus.loadProgram(0xbf, 0x0a, 0x05);  // 65C02 BBS3 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0x08);
        bus.loadProgram(0xbf, 0x0a, 0x05);  // 65C02 BBS3 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0x08);
        bus.loadProgram(0xbf, 0x0a, 0xfb);  // 65C02 BBS3 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

        /* BBS4 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0x00);
        bus.loadProgram(0xcf, 0x0a, 0x05);  // 65C02 BBS4 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0x10);
        bus.loadProgram(0xcf, 0x0a, 0x05);  // 65C02 BBS4 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0x10);
        bus.loadProgram(0xcf, 0x0a, 0xfb);  // 65C02 BBS4 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

        /* BBS5 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0x00);
        bus.loadProgram(0xdf, 0x0a, 0x05);  // 65C02 BBS5 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0x20);
        bus.loadProgram(0xdf, 0x0a, 0x05);  // 65C02 BBS5 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0x20);
        bus.loadProgram(0xdf, 0x0a, 0xfb);  // 65C02 BBS5 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

        /* BBS6 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0x00);
        bus.loadProgram(0xef, 0x0a, 0x05);  // 65C02 BBS6 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0x40);
        bus.loadProgram(0xef, 0x0a, 0x05);  // 65C02 BBS6 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0x40);
        bus.loadProgram(0xef, 0x0a, 0xfb);  // 65C02 BBS6 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

        /* BBS7 */
        // Positive Offset
        cpu.reset();
        bus.write(0x0a,0x00);
        bus.loadProgram(0xff, 0x0a, 0x05);  // 65C02 BBS7 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter()); // Shouldn't have branched

        cpu.reset();
        bus.write(0x0a,0x80);
        bus.loadProgram(0xff, 0x0a, 0x05);  // 65C02 BBS7 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x208, cpu.getProgramCounter()); // Should have branched

        // Negative Offset
        cpu.reset();
        bus.write(0x0a,0x80);
        bus.loadProgram(0xff, 0x0a, 0xfb);  // 65C02 BBS7 $00 $fb ; *=$0203-$05 ($01fe)
        cpu.step();
        assertEquals(0x1fe, cpu.getProgramCounter());

    }

    public void test_BBSNeedsCmosCpu() throws Exception {
        makeNmosCpu();

        /* BBS0 */
        cpu.reset();
        bus.write(0x0a,0x01);
        bus.loadProgram(0x8f, 0x0a, 0x05);  // 65C02 BBS0 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

        /* BBS1 */
        cpu.reset();
        bus.write(0x0a,0x02);
        bus.loadProgram(0x9f, 0x0a, 0x05);  // 65C02 BBS1 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

        /* BBS2 */
        cpu.reset();
        bus.write(0x0a,0x04);
        bus.loadProgram(0xaf, 0x0a, 0x05);  // 65C02 BBS2 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

        /* BBS3 */
        cpu.reset();
        bus.write(0x0a,0x08);
        bus.loadProgram(0xbf, 0x0a, 0x05);  // 65C02 BBS3 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

        /* BBS4 */
        cpu.reset();
        bus.write(0x0a,0x10);
        bus.loadProgram(0xcf, 0x0a, 0x05);  // 65C02 BBS4 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

        /* BBS5 */
        cpu.reset();
        bus.write(0x0a,0x20);
        bus.loadProgram(0xdf, 0x0a, 0x05);  // 65C02 BBS5 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

        /* BBS6 */
        cpu.reset();
        bus.write(0x0a,0x40);
        bus.loadProgram(0xef, 0x0a, 0x05);  // 65C02 BBS6 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

        /* BBS7 */
        cpu.reset();
        bus.write(0x0a,0x80);
        bus.loadProgram(0xff, 0x0a, 0x05);  // 65C02 BBS7 $00 $05 ; *=$0202+$05 ($0208)

        cpu.step();
        assertEquals(0x203, cpu.getProgramCounter());

    }

}
