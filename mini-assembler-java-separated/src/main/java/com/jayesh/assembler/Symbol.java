package com.jayesh.assembler;

public class Symbol {
    public String name;
    public int size;
    public String value;
    public char section;
    public long address;
    public char status;
    public boolean instruction;

    public Symbol(String name, int size, String value, char section, long address,
                  char status, boolean instruction) {
        this.name = name;
        this.size = size;
        this.value = value;
        this.section = section;
        this.address = address;
        this.status = status;
        this.instruction = instruction;
    }
}
