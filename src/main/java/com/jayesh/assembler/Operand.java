package com.jayesh.assembler;

public class Operand {
    public enum Kind { NONE, REG, MEM, IMM, SYM }

    public Kind kind = Kind.NONE;
    public String text = "";
    public int reg = -1;
    public int base = -1;
    public int index = -1;
    public int scale = 1;
    public long disp = 0;
    public boolean hasDisp = false;
    public String symbol = "";
}
