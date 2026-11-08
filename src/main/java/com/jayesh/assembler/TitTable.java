package com.jayesh.assembler;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class TitTable {
    private final List<TitEntry> entries = new ArrayList<>();

    public void add(String symbol,long address,int patch,int size,int disp) {
        for (TitEntry e: entries)
            if (e.symbol().equals(symbol) && e.instructionAddress()==address) return;
        entries.add(new TitEntry(symbol,address,patch,size,disp,false));
    }

    public void markResolved(String symbol,long address) {
        for (int i=0;i<entries.size();i++) {
            TitEntry e=entries.get(i);
            if(e.symbol().equals(symbol)&&e.instructionAddress()==address) {
                entries.set(i,new TitEntry(e.symbol(),e.instructionAddress(),e.patchOffset(),
                        e.instructionSize(),e.displacementSize(),true));
            }
        }
    }

    public List<TitEntry> entries(){ return entries; }

    public void write(Path file) {
        try(PrintWriter out=new PrintWriter(file.toFile())) {
            out.println("TABLE OF INCOMPLETE INSTRUCTIONS (TIT)");
            out.println("================================================================================");
            out.printf("%-20s %-24s %-14s %-20s %-14s%n",
                    "INSTRUCTION ADDRESS","SYMBOL","PATCH OFFSET","DISPLACEMENT SIZE","STATUS");
            for(TitEntry e:entries)
                out.printf("%-20s %-24s %-14d %-20d %-14s%n",
                        String.format("0x%08X",e.instructionAddress()),e.symbol(),
                        e.patchOffset(),e.displacementSize(),e.resolved()?"RESOLVED":"UNRESOLVED");
            out.println("================================================================================");
            out.println("Total entries: " + entries.size());
        }catch(IOException e){ throw new RuntimeException(e); }
    }
}
