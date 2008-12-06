package com.loomcom.j6502;

import java.io.IOException;

/**
 * Main control class for the J6502 Simulator.
 */
public class Simulator {
    
    CommandParser m_parser;
    Cpu m_cpu;
    
    public Simulator() {
	m_cpu = new Cpu();
	m_parser = new CommandParser(System.in, System.out, this);
    }

    public void run() {
	m_parser.run();
    }
    
    public static void main(String[] args) {
	new Simulator().run();
    }

    public void step() {
	
    }
    
}