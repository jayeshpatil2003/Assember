# IA-32 Mini Assembler - Java

Separated Java conversion of the C assembler project.

## Folder structure

src/main/java/com/jayesh/assembler/
- Assembler.java
- FirstPass.java
- SecondPass.java
- Encoder.java
- Parser.java
- InstructionSet.java
- Instruction.java
- Operand.java
- Code.java
- Symbol.java
- SymbolTable.java
- TitEntry.java
- TitTable.java
- ErrorDefinition.java
- ErrorOccurrence.java
- ErrorReporter.java

Put `IA32_Predefined_Error_Table.txt` and `Instruction_Byte_Diagram.txt` in the project root.

## Compile

```bash
mkdir -p out
javac -d out $(find src/main/java -name '*.java')
```

## Run

```bash
java -cp out com.jayesh.assembler.Assembler examples/program.asm Instruction_Byte_Diagram.txt
```

Optional object output:

```bash
java -cp out com.jayesh.assembler.Assembler examples/program.asm Instruction_Byte_Diagram.txt program.o
```
