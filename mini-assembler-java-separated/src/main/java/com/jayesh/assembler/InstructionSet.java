package com.jayesh.assembler;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class InstructionSet {
    private final List<Instruction> instructions=new ArrayList<>();
    private final int[] regCode={0,1,2,3,4,5,6,7};

    public void load(Path file){
        try(BufferedReader br=new BufferedReader(new FileReader(file.toFile()))){
            String line;
            while((line=br.readLine())!=null){
                line=line.trim();
                if(line.isEmpty())continue;
                String[] p=line.split("\\|");
                if(p.length>=2){
                    String bits=p[0].trim(), reg=p[1].trim().toLowerCase(Locale.ROOT);
                    if(bits.matches("[01]{3}")&&Parser.regId(reg)>=0){
                        regCode[Parser.regId(reg)]=Integer.parseInt(bits,2);continue;
                    }
                }
                if(p.length>=8){
                    String name=p[0].trim().toLowerCase(Locale.ROOT);
                    if(name.equals("instruction"))continue;
                    instructions.add(new Instruction(name,p[1].trim(),p[2].trim().replace(" ",""),
                            p[6].trim(),p[7].trim()));
                }
            }
        }catch(IOException e){throw new RuntimeException(e);}
    }

    public boolean isEmpty(){return instructions.isEmpty();}
    public int regCode(int r){return regCode[r];}

    public boolean mnemonicExists(String mn){
        return instructions.stream().anyMatch(i->i.name().equalsIgnoreCase(mn));
    }

    public Instruction find(String mn,Operand a,Operand b,boolean two){
        for(Instruction i:instructions)
            if(i.name().equals(mn)&&i.opcode().contains("+rd")&&match(i,a,b,two))return i;
        if(two&&b.kind==Operand.Kind.IMM&&b.text.matches("-?\\d+|0[xX][0-9a-fA-F]+")){
            for(Instruction i:instructions)
                if(i.name().equals(mn)&&i.form().contains("imm8")&&match(i,a,b,two))return i;
        }
        for(Instruction i:instructions)if(i.name().equals(mn)&&match(i,a,b,two))return i;
        return null;
    }

    private boolean value(Operand o){return o.kind==Operand.Kind.IMM||o.kind==Operand.Kind.SYM;}

    private boolean match(Instruction i,Operand a,Operand b,boolean two){
        String f=i.form().toLowerCase(Locale.ROOT).replace(" ","");
        if(!two){
            return switch(f){
                case "-"->true; case "r32"->a.kind==Operand.Kind.REG;
                case "r/m32"->a.kind==Operand.Kind.REG||a.kind==Operand.Kind.MEM;
                case "imm8","imm32","rel8","rel32"->value(a);
                default->false;
            };
        }
        return switch(f){
            case "r/m32,r32"->(a.kind==Operand.Kind.REG||a.kind==Operand.Kind.MEM)&&b.kind==Operand.Kind.REG;
            case "r32,r/m32"->a.kind==Operand.Kind.REG&&(b.kind==Operand.Kind.REG||b.kind==Operand.Kind.MEM);
            case "r/m32,imm8","r/m32,imm32"->(a.kind==Operand.Kind.REG||a.kind==Operand.Kind.MEM)&&value(b);
            case "r32,imm8","r32,imm32"->a.kind==Operand.Kind.REG&&value(b);
            case "eax,imm32"->a.kind==Operand.Kind.REG&&a.reg==0&&value(b);
            default->false;
        };
    }

    public int regField(Instruction i,Operand a,Operand b){
        if(i.field().startsWith("/")&&i.field().length()>1&&Character.isDigit(i.field().charAt(1)))
            return i.field().charAt(1)-'0';
        if(i.form().equals("r/m32,r32"))return b.reg;
        if(i.form().equals("r32,r/m32"))return a.reg;
        return 0;
    }
}
