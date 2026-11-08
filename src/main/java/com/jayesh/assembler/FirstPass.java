package com.jayesh.assembler;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FirstPass {
    private final Path source; private final InstructionSet set; private final SymbolTable symbols;
    private final TitTable tit; private final ErrorReporter errors;
    private int lineNo=0;

    public FirstPass(Path source,InstructionSet set,SymbolTable symbols,TitTable tit,ErrorReporter errors){
        this.source=source;this.set=set;this.symbols=symbols;this.tit=tit;this.errors=errors;
    }

    public void run(){
        String section=""; long address=0;
        try(BufferedReader br=Files.newBufferedReader(source)){
            String raw;
            while((raw=br.readLine())!=null){
                lineNo++;
                StringBuilder sb=new StringBuilder(raw);Parser.removeComment(sb);
                String line=sb.toString().trim();
                if(line.isEmpty())continue;
                String low=Parser.lower(line);
                if(low.startsWith("section")){section=low.substring(7).trim();address=0;continue;}
                if(low.startsWith("global "))continue;
                if(low.startsWith("extern ")){for(String n:line.substring(7).split("[, \\t]+"))symbols.addUndefined(n);continue;}

                if(section.equals(".data")||section.equals(".bss")){
                    collectData(line,section,address);
                    Code data=new Code();
                    if(processData(line,data))address+=data.size(); else address+=reservedSize(line);
                    continue;
                }
                if(!section.equals(".text"))continue;

                String work=line;
                int colon=work.indexOf(':');
                if(colon>=0){
                    String label=work.substring(0,colon).trim();
                    symbols.addTextLabel(label,address,errors,lineNo,raw);
                    work=work.substring(colon+1).trim();
                    if(work.isEmpty())continue;
                }

                String[] p=Parser.instruction(work); if(p.length==0)continue;
                Operand a=Parser.operand(p[1]), b=p[2].isEmpty()?new Operand():Parser.operand(p[2]);
                if(a.kind==Operand.Kind.SYM)symbols.addUndefined(a.text);
                if(b.kind==Operand.Kind.SYM)symbols.addUndefined(b.text);
                Instruction op=set.find(p[0],a,b,!p[2].isEmpty());
                if(op==null){
                    String detail=set.mnemonicExists(p[0])?
                            "No supported opcode form matches '"+p[0]+"' with these operands":
                            "Mnemonic '"+p[0]+"' is not present in the loaded opcode table";
                    errors.report(set.mnemonicExists(p[0])?"E302":"E301",lineNo,raw,detail);
                }else{
                    Code c=Encoder.encode(op,a,b,symbols,set);
                    if(a.kind==Operand.Kind.SYM&&(op.imm().contains("rel8")||op.imm().contains("rel32"))){
                        int w=op.imm().contains("rel8")?1:4;
                        tit.add(a.text,address,c.size()-w,c.size(),w);
                    }
                    address+=c.size();
                }
            }
        }catch(IOException e){throw new RuntimeException(e);}
        for(TitEntry e:new ArrayList<>(tit.entries())){
            Symbol s=symbols.find(e.symbol());
            if(s!=null&&s.section=='T')tit.markResolved(e.symbol(),e.instructionAddress());
            else errors.report("E401",lineNo,"",
                    "Reference to '"+e.symbol()+"' was not defined before end of assembly");
        }
    }

    private void collectData(String line,String section,long address){
        String[] p=Parser.instruction(line);
        if(p.length>=3&&Parser.dataDirective(p[0]))symbols.addData(p[1],p[0],p[2],section.equals(".data")?'D':'B',address);
        else{
            String[] t=line.trim().split("\\s+",3);
            if(t.length>=2&&Parser.dataDirective(t[0]))symbols.addData(t[0],t[0],t.length>1?t[1]:"",section.equals(".data")?'D':'B',address);
        }
    }

    private boolean processData(String line,Code c){
        String t=line.trim();
        int colon=t.indexOf(':');if(colon>=0)t=t.substring(colon+1).trim();
        String[] p=t.split("\\s+",3);
        if(p.length<2&&Parser.dataDirective(p[0]))return true;
        String dir,rest;
        if(p.length>=3&&Parser.dataDirective(p[1])){dir=p[1];rest=p[2];}
        else if(p.length>=2&&Parser.dataDirective(p[0])){dir=p[0];rest=p[1];}
        else return false;
        if(dir.startsWith("res"))return true;
        int width=Parser.directiveWidth(dir);
        for(String token:rest.split(",")){
            token=token.trim();
            if(token.startsWith(""")&&token.endsWith(""")){
                for(char ch:token.substring(1,token.length()-1).toCharArray())c.emitByte(ch);
            }else if(Parser.isNumber(token))c.emitLE(Parser.number(token),width);
        }
        return true;
    }

    private int reservedSize(String line){
        String t=line.trim();int colon=t.indexOf(':');if(colon>=0)t=t.substring(colon+1).trim();
        String[] p=t.split("\\s+",2);
        if(p.length<2||!p[0].startsWith("res"))return 0;
        return (int)(Parser.number(p[1])*Parser.directiveWidth(p[0]));
    }
}
