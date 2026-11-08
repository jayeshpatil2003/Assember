package com.jayesh.assembler;

public record TitEntry(String symbol, long instructionAddress, int patchOffset,
                       int instructionSize, int displacementSize, boolean resolved) {}
