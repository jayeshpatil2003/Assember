package com.jayesh.assembler;

import java.nio.file.Path;

public class Assembler {
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Usage: java -cp out com.jayesh.assembler.Assembler program.asm Instruction_Byte_Diagram.txt [output.o]");
            return;
        }

        Path source = Path.of(args[0]);
        Path instructionTable = Path.of(args[1]);
        Path objectFile = args.length == 3
                ? Path.of(args[2])
                : Path.of(source.getFileName().toString().replaceFirst("\\.[^.]+$", "") + ".o");

        ErrorReporter errors = new ErrorReporter(Path.of("IA32_Predefined_Error_Table.txt"));
        InstructionSet instructionSet = new InstructionSet();
        instructionSet.load(instructionTable);

        if (instructionSet.isEmpty()) {
            errors.report("E513", 0, instructionTable.toString(),
                    "Opcode table contains no usable instruction definitions");
        }

        SymbolTable symbols = new SymbolTable();
        TitTable tit = new TitTable();

        FirstPass pass1 = new FirstPass(source, instructionSet, symbols, tit, errors);
        pass1.run();
        tit.write(Path.of("TIT.txt"));
        symbols.write(Path.of("symbol_table.txt"));
        errors.write(Path.of("error_report.txt"));

        if (errors.hasErrors()) {
            System.out.println("Assembly failed: " + errors.size() +
                    " error(s). See error_report.txt");
            return;
        }

        SecondPass pass2 = new SecondPass(source, instructionSet, symbols, tit, errors, objectFile);
        pass2.run();
        tit.write(Path.of("TIT.txt"));
        symbols.write(Path.of("symbol_table.txt"));
        errors.write(Path.of("error_report.txt"));

        if (!errors.hasErrors()) {
            System.out.println("Raw object bytes written to: " + objectFile);
        } else {
            System.out.println("Assembly failed: " + errors.size() +
                    " error(s). See error_report.txt");
        }
    }
}
