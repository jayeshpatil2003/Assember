package com.jayesh.assembler;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class SymbolTable {
    private final List<Symbol> symbols = new ArrayList<>();

    public Symbol find(String name) {
        for (Symbol s : symbols)
            if (s.name.equalsIgnoreCase(name) && s.status == 'D') return s;
        return null;
    }

    public Symbol any(String name) {
        for (Symbol s : symbols)
            if (s.name.equalsIgnoreCase(name)) return s;
        return null;
    }

    public void addUndefined(String name) {
        if (name == null || name.isBlank() || any(name) != null) return;
        symbols.add(new Symbol(name, 0, "{}", '-', 0, 'U', false));
    }

    public void addTextLabel(String name, long address, ErrorReporter errors, int line, String source) {
        Symbol s = any(name);
        if (s != null) {
            if (s.status == 'D') {
                errors.report("E102", line, source,
                        "Symbol '" + name + "' is defined more than once");
                return;
            }
            s.section='T'; s.address=address; s.status='D'; s.value="{}";
            return;
        }
        symbols.add(new Symbol(name,0,"{}", 'T', address,'D',false));
    }

    public void addData(String name, String dir, String rest, char section, long address) {
        int width = Parser.directiveWidth(dir);
        if (width == 0 || name.isBlank()) return;

        int size;
        if (dir.startsWith("res")) {
            size = (int)(Parser.number(rest) * width);
        } else if (dir.equals("db") && rest.contains(""")) {
            size = Parser.dataSize(rest, width);
        } else {
            size = Parser.dataItemCount(rest) * width;
        }

        String value = dir.startsWith("res") ? "{}" : Parser.formatValue(rest);
        Symbol s = any(name);
        if (s == null) {
            symbols.add(new Symbol(name,size,value,section,address,'D',false));
        } else {
            s.size=size; s.value=value; s.section=section; s.address=address; s.status='D';
        }
    }

    public void addInstruction(String source, Code code, long address) {
        symbols.add(new Symbol(source.trim(), code.size(), code.size()==0 ? "??" : code.hex(),
                'T', address, 'D', true));
    }

    public Collection<Symbol> all() { return symbols; }

    public void write(Path file) {
        try (PrintWriter out = new PrintWriter(file.toFile())) {
            out.println("SYMBOL TABLE");
            out.println("================================================================================================");
            out.printf("%-5s %-30s %-8s %-20s %-10s %-12s %-8s%n",
                    "ID","NAME","SIZE","VALUE","SECTION","ADDRESS","STATUS");
            out.println("------------------------------------------------------------------------------------------------");
            int id=1;
            for (Symbol s : symbols) {
                out.printf("%-5d %-30s %-8d %-20s %-10s 0x%-10X %-8s%n",
                        id++,s.name,s.size,s.value,String.valueOf(s.section),s.address,String.valueOf(s.status));
            }
            out.println("================================================================================================");
            out.println("Total entries: " + symbols.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
