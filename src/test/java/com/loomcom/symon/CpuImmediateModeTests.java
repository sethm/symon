package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryRangeException;

import junit.framework.TestCase;

public class CpuImmediateModeTests extends TestCase {

	private Cpu cpu;
	private Bus bus;
	private Memory mem;

	public void setUp() throws MemoryRangeException {		
		this.cpu = new Cpu();
		this.bus = new Bus(0x0000, 0xffff);
		this.mem = new Memory(0x0000, 0x10000);
		bus.addCpu(cpu);
		bus.addDevice(mem);

		// All test programs start at 0x0200;
		bus.write(0xfffc, 0x00);
		bus.write(0xfffd, 0x02);

		cpu.reset();
	}

	/* ORA Immediate Mode Tests - 0x09 */
	
	public void test_ORA_SetsAccumulator() {
		bus.write(0x0200, 0x09);  // ORA #$00
		bus.write(0x0201, 0x00);
		cpu.step();
		// 0x00 | 0x00 == 0x00
		assertEquals(0x00, cpu.getAccumulator());
		
		bus.write(0x0202, 0x09);  // ORA #$11
		bus.write(0x0203, 0x11);
		cpu.step();
		// 0x00 | 0x11 == 0x11
		assertEquals(0x11, cpu.getAccumulator());

		bus.write(0x0204, 0x09);  // ORA #$22
		bus.write(0x0205, 0x22);
		cpu.step();
		// 0x11 | 0x22 == 0x33
		assertEquals(0x33, cpu.getAccumulator());
		
		bus.write(0x0206, 0x09);  // ORA #$44
		bus.write(0x0207, 0x44);
		cpu.step();
		// 0x33 | 0x44 == 0x77
		assertEquals(0x77, cpu.getAccumulator());

		bus.write(0x0208, 0x09);  // ORA #$88
		bus.write(0x0209, 0x88);
		cpu.step();
		// 0x77 | 0x88 == 0xFF
		assertEquals(0xff, cpu.getAccumulator());
	}
	
	public void test_ORA_SetsZeroFlagIfResultIsZero() {
		bus.write(0x0200, 0x09);  // ORA #$00
		bus.write(0x0201, 0x00);
		cpu.step();
		assertTrue(cpu.getZeroFlag());
	}
	
	public void test_ORA_DoesNotSetZeroFlagIfResultNotZero() {
		bus.write(0x0200, 0x09);  // ORA #$01
		bus.write(0x0201, 0x01);
		cpu.step();
		assertFalse(cpu.getZeroFlag());		
	}

	public void test_ORA_SetsNegativeFlagIfResultIsNegative() {
		bus.write(0x0200, 0x09);  // ORA #$80
		bus.write(0x0201, 0x80);
		cpu.step();
		assertTrue(cpu.getNegativeFlag());
	}
	
	public void test_ORA_DoesNotSetNegativeFlagIfResultNotNegative() {
		bus.write(0x0200, 0x09);  // ORA #$7F
		bus.write(0x0201, 0x7f);
		cpu.step();
		assertFalse(cpu.getNegativeFlag());		
	}

	/* AND Immediate Mode Tests - 0x29 */
	
	public void test_AND_SetsAccumulator() {
		bus.write(0x0200, 0x29);  // AND #$00
		bus.write(0x0201, 0x00);
		cpu.step();
		// 0x00 & 0x00 == 0x00
		assertEquals(0x00, cpu.getAccumulator());
		
		bus.write(0x0202, 0x29);  // AND #$FF
		bus.write(0x0203, 0x11);
		cpu.step();
		// 0x00 & 0xff == 0x00
		assertEquals(0x00, cpu.getAccumulator());
		
		// Load Accumulator with AA - %10101010
		
		bus.write(0x0204, 0xa9);  // LDA #$AA
		bus.write(0x0205, 0xaa);
		cpu.step();

		bus.write(0x0206, 0x29);  // AND #$FF
		bus.write(0x0207, 0xff);
		cpu.step();
		// 0xaa & 0xff == 0xaa
		assertEquals(0xaa, cpu.getAccumulator());

		bus.write(0x0208, 0x29);  // AND #$99
		bus.write(0x0209, 0x99);
		cpu.step();
		// 0xaa & 0x99 == 0x88

		assertEquals(0x88, cpu.getAccumulator());
		bus.write(0x020a, 0x29);  // AND #$99
		bus.write(0x020b, 0x11);
		cpu.step();
		// 0x88 & 0x11 == 0x00
		assertEquals(0x00, cpu.getAccumulator());
	}
	
	public void test_AND_SetsZeroFlagIfResultIsZero() {
		bus.write(0x0200, 0xa9);  // LDA #$88
		bus.write(0x0201, 0x88);
		cpu.step();
		bus.write(0x0202, 0x29);  // AND #$11
		bus.write(0x0203, 0x11);
		cpu.step();
		assertTrue(cpu.getZeroFlag());
	}
	
	public void test_AND_DoesNotSetZeroFlagIfResultNotZero() {
		bus.write(0x0200, 0xa9);  // LDA #$88
		bus.write(0x0201, 0x88);
		cpu.step();
		bus.write(0x0202, 0x29);  // AND #$f1
		bus.write(0x0203, 0xf1);
		cpu.step();
		assertFalse(cpu.getZeroFlag());
	}

	public void test_AND_SetsNegativeFlagIfResultIsNegative() {
		bus.write(0x0200, 0xa9);  // LDA #$88
		bus.write(0x0201, 0x88);
		cpu.step();
		bus.write(0x0202, 0x29);  // AND #$F0
		bus.write(0x0203, 0xf0);
		cpu.step();
		assertTrue(cpu.getNegativeFlag());
	}
	
	public void test_AND_DoesNotSetNegativeFlagIfResultNotNegative() {
		bus.write(0x0200, 0xa9);  // LDA #$88
		bus.write(0x0201, 0x88);
		cpu.step();
		bus.write(0x0202, 0x29);  // AND #$0F
		bus.write(0x0203, 0x0f);
		cpu.step();
		assertFalse(cpu.getNegativeFlag());
	}
	
	/* EOR Immediate Mode Tests - 0x49 */
	
	public void test_EOR_SetsAccumulator() {
		bus.write(0x0200, 0xa9);  // LDA #$88
		bus.write(0x0201, 0x88);
		cpu.step();

		bus.write(0x0202, 0x49);  // EOR #$00
		bus.write(0x0203, 0x00);
		cpu.step();
		assertEquals(0x88, cpu.getAccumulator());
		
		bus.write(0x0204, 0x49);  // EOR #$ff
		bus.write(0x0205, 0xff);
		cpu.step();
		assertEquals(0x77, cpu.getAccumulator());

		bus.write(0x0206, 0x49);  // EOR #$33
		bus.write(0x0207, 0x33);
		cpu.step();
		assertEquals(0x44, cpu.getAccumulator());	
	}
	
	/* ADC Immediate Mode Tests - 0x69 */
	
	public void test_ADC_SetsAccumulator() {
		bus.write(0x200, 0x69);
		bus.write(0x201, 0x01);
		cpu.step();
		assertEquals(0x01, cpu.getAccumulator());
		
		bus.write(0x202, 0x69);
		bus.write(0x203, 0xa0);
		cpu.step();
		assertEquals(0xa1, cpu.getAccumulator());

		bus.write(0x204, 0x69);
		bus.write(0x205, 0x02);
		cpu.step();
		assertEquals(0xa3, cpu.getAccumulator());

		bus.write(0x206, 0x69);
		bus.write(0x207, 0x06);
		cpu.step();
		assertEquals(0xa9, cpu.getAccumulator());
	}
	
	public void test_ADC_IncludesCarry() {
		cpu.setCarryFlag(true);
		bus.write(0x200, 0x69);
		bus.write(0x201, 0x01);
		cpu.step();
		assertEquals(0x02, cpu.getAccumulator());
	}
	
	public void test_ADC_SetsCarryIfResultOverflows() {
		bus.write(0x200, 0xa9); // LDA #$fe
		bus.write(0x201, 0xff);
		cpu.step();
		bus.write(0x202, 0x69); // ADC #$02
		bus.write(0x203, 0x02);
		cpu.step();
		// $ff + $02 = $101 = [c] + $01
		assertEquals(0x01, cpu.getAccumulator());
		assertTrue(cpu.getCarryFlag());
	}
	
	public void test_ADC_SetsOverflowIfResultChangesSign() {
		bus.write(0x200, 0xa9); // LDA #$7f
		bus.write(0x201, 0x7f);
		cpu.step();
		bus.write(0x202, 0x69); // ADC #$01
		bus.write(0x203, 0x01);
		cpu.step();
		assertEquals(0x80, cpu.getAccumulator());
		assertTrue(cpu.getOverflowFlag());
	}
	
	public void test_ADC_SetsNegativeFlagIfResultIsNegative() {
		bus.write(0x200, 0xa9); // LDA #$7f
		bus.write(0x201, 0x7f);
		cpu.step();
		bus.write(0x202, 0x69); // ADC #$01
		bus.write(0x203, 0x01);
		cpu.step();
		assertEquals(0x80, cpu.getAccumulator());
		assertTrue(cpu.getNegativeFlag());	
	}
	
	public void test_ADC_SetsZeroFlagIfResultIsZero() {
		bus.write(0x200, 0xa9); // LDA #$ff
		bus.write(0x201, 0xff);
		cpu.step();
		bus.write(0x202, 0x69); // ADC #$01
		bus.write(0x203, 0x01);
		cpu.step();
		assertEquals(0x00, cpu.getAccumulator());
		assertTrue(cpu.getZeroFlag());		
	}
	
	public void test_ADC_DoesNotSetNegativeFlagIfResultNotNegative() {
		bus.write(0x200, 0xa9); // LDA #$7f
		bus.write(0x201, 0x7e);
		cpu.step();
		bus.write(0x202, 0x69); // ADC #$01
		bus.write(0x203, 0x01);
		cpu.step();
		assertEquals(0x7f, cpu.getAccumulator());
		assertFalse(cpu.getNegativeFlag());
	}
	
	public void test_ADC_DoesNotSetZeroFlagIfResultNotZero() {
		bus.write(0x200, 0xa9); // LDA #$ff
		bus.write(0x201, 0xff);
		cpu.step();
		bus.write(0x202, 0x69); // ADC #$01
		bus.write(0x203, 0x03);
		cpu.step();
		assertEquals(0x2, cpu.getAccumulator());
		assertFalse(cpu.getZeroFlag());		
	}
	
	/* LDY Immediate Mode Tests - 0xa0 */
	
	public void test_LDY_SetsYRegister() {
		bus.write(0x0200, 0xa0);
		bus.write(0x0201, 0x12);
		cpu.step();
		assertEquals(0x12, cpu.getYRegister());
	}
	
	public void test_LDY_SetsZeroFlagIfArgIsZero() {
		bus.write(0x0200, 0xa0);
		bus.write(0x0201, 0x00);
		cpu.step();
		assertTrue(cpu.getZeroFlag());
	}

	public void test_LDY_DoesNotSetZeroFlagIfResultNotZero() {
		bus.write(0x0200, 0xa0);
		bus.write(0x0201, 0x12);
		cpu.step();
		assertFalse(cpu.getZeroFlag());		
	}
	
	public void test_LDY_SetsNegativeFlagIfResultIsNegative() {
		bus.write(0x0200, 0xa0);
		bus.write(0x0201, 0x80);
		cpu.step();
		assertTrue(cpu.getNegativeFlag());
	}
	
	public void test_LDY_DoesNotSetNegativeFlagIfResultNotNegative() {
		bus.write(0x0200, 0xa0);
		bus.write(0x0201, 0x7f);
		cpu.step();
		assertFalse(cpu.getNegativeFlag());
	}

	/* LDX Immediate Mode Tests - 0xa2 */
	
	public void test_LDX_SetsXRegister() {
		bus.write(0x0200, 0xa2);
		bus.write(0x0201, 0x12);
		cpu.step();
		assertEquals(0x12, cpu.getXRegister());
	}
	
	public void test_LDX_SetsZeroFlagIfResultIsZero() {
		bus.write(0x0200, 0xa2);
		bus.write(0x0201, 0x00);
		cpu.step();
		assertTrue(cpu.getZeroFlag());
	}

	public void test_LDX_DoesNotSetZeroFlagIfResultNotZero() {
		bus.write(0x0200, 0xa2);
		bus.write(0x0201, 0x12);
		cpu.step();
		assertFalse(cpu.getZeroFlag());		
	}
	
	public void test_LDX_SetsNegativeFlagIfResultIsNegative() {
		bus.write(0x0200, 0xa2);
		bus.write(0x0201, 0x80);
		cpu.step();
		assertTrue(cpu.getNegativeFlag());
	}
	
	public void test_LDX_DoesNotSetNegativeFlagIfResultNotNegative() {
		bus.write(0x0200, 0xa2);
		bus.write(0x0201, 0x7f);
		cpu.step();
		assertFalse(cpu.getNegativeFlag());
	}

	/* LDA Immediate Mode Tests - 0xa9 */

	public void test_LDA_SetsAccumulator() {
		bus.write(0x0200, 0xa9);
		bus.write(0x0201, 0x12);
		cpu.step();
		assertEquals(0x12, cpu.getAccumulator());
	}
	
	public void test_LDA_SetsZeroFlagIfResultIsZero() {
		bus.write(0x0200, 0xa9);
		bus.write(0x0201, 0x00);
		cpu.step();
		assertTrue(cpu.getZeroFlag());
	}

	public void test_LDA_DoesNotSetZeroFlagIfResultNotZero() {
		bus.write(0x0200, 0xa9);
		bus.write(0x0201, 0x12);
		cpu.step();
		assertFalse(cpu.getZeroFlag());		
	}
	
	public void test_LDA_SetsNegativeFlagIfResultIsNegative() {
		bus.write(0x0200, 0xa9);
		bus.write(0x0201, 0x80);
		cpu.step();
		assertTrue(cpu.getNegativeFlag());
	}
	
	public void test_LDA_DoesNotSetNegativeFlagIfResultNotNegative() {
		bus.write(0x0200, 0xa9);
		bus.write(0x0201, 0x7f);
		cpu.step();
		assertFalse(cpu.getNegativeFlag());
	}

	/* CPY Immediate Mode Tests - 0xc0 */

	/* CMP Immediate Mode Tests - 0xc9 */

	/* CPX Immediate Mode Tests - 0xe0 */
	
	/* SBC Immediate Mode Tests - 0xe9 */
}
