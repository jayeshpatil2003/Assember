package com.jayesh.assembler;

public record ErrorOccurrence(String code,int line,String source,String detail) {}
