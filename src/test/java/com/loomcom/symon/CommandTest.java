package com.loomcom.symon;

import org.junit.*;
import static org.junit.Assert.*;

public class CommandTest {
  
  @Test
  public void testCommandShouldParseCorrectNumberOfArguments() {
    Simulator.Command c;
    
    c = new Simulator.Command("foo");
    assertEquals("foo", c.getCommand());
    assertEquals(0, c.numArgs());
    
    c = new Simulator.Command("foo bar");
    assertEquals("foo", c.getCommand());
    assertEquals(1, c.numArgs());
    assertEquals("bar", c.getArgs()[0]);
    
    c = new Simulator.Command("foo bar baz quux 0 100");
    assertEquals("foo",  c.getCommand());
    assertEquals(5,      c.numArgs());
    assertEquals("bar",  c.getArgs()[0]);
    assertEquals("baz",  c.getArgs()[1]);
    assertEquals("quux", c.getArgs()[2]);
    assertEquals("0",    c.getArgs()[3]);
    assertEquals("100",  c.getArgs()[4]);
  }

  @Test
  public void testCommandShouldIgnoreWhitespaceBetweenTokens() {
    Simulator.Command c;
    
    c = new Simulator.Command("foo    bar     baz");
    assertEquals("foo", c.getCommand());
    assertEquals(2, c.numArgs());
    assertEquals("bar", c.getArgs()[0]);
    assertEquals("baz", c.getArgs()[1]);
  }

  @Test
  public void testCommandShouldIgnoreWhitespaceBeforeCommand() {
    Simulator.Command c;
    
    c = new Simulator.Command("     foo bar baz");
    assertEquals("foo", c.getCommand());
    assertEquals(2, c.numArgs());
    assertEquals("bar", c.getArgs()[0]);
    assertEquals("baz", c.getArgs()[1]);
  }

  @Test
  public void testCommandShouldIgnoreWhitespaceAfterCommand() {
    Simulator.Command c;
    
    c = new Simulator.Command("foo bar baz      ");
    assertEquals("foo", c.getCommand());
    assertEquals(2, c.numArgs());
    assertEquals("bar", c.getArgs()[0]);
    assertEquals("baz", c.getArgs()[1]);
  }

}
