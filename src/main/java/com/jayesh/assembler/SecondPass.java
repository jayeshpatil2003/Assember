package com.jayesh.assembler;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SecondPass {
    private final Path source,object; private final InstructionSet set; private final SymbolTable symbols;
    private final TitTable tit; private final ErrorReporter errors; private int lineNo=0;

    public SecondPass(Path source,InstructionSet set,SymbolTable symbols,TitTable tit,
                      ErrorReporter errors,Path object){
        this.source=source;this.set=set;this.symbols=symbols;this.tit=tit;this.errors=errors;this.object=object;
    }

    public void run(){
        String section="";long address=0;
        try(BufferedReader br=Files.newBufferedReader(source);
            OutputStream out=Files.newOutputStream(object)){
            String raw;
            while((raw=br.readLine())!=null){
                lineNo++;
                StringBuilder sb=new StringBuilder(raw);Parser.removeComment(sb);
                String line=sb.toString().trim();
                if(line.isEmpty())continue;
                String low=Parser.lower(line);
                if(low.startsWith("section")){section=low.substring(7).trim();address=0;continue;}
                if(low.startsWith("global "))continue;
                if(low.startsWith("extern ")){printRow(address,raw,null);continue;}

                if(section.equals(".data")||section.equals(".bss")){
                    Code c=new Code();
                    if(processData(line,c)){
                        if(c.size()>0){if(section.equals(".data"))out.write(c.toByteArray());printRow(address,raw,c);address+=c.size();}
                        else {int n=reservedSize(line);if(section.equals(".bss"))out.write(new byte[n]);printRow(address,raw,null);address+=n;}
                    }
                    continue;
                }
                if(!section.equals(".text"))continue;

                String work=line;int colon=work.indexOf(':');
                if(colon>=0){
                    work=work.substring(colon+1).trim();
                    if(work.isEmpty()){printRow(address,raw,null);continue;}
                }
                String[] p=Parser.instruction(work);if(p.length==0)continue;
                Operand a=Parser.operand(p[1]),b=p[2].isEmpty()?new Operand():Parser.operand(p[2]);
                Instruction op=set.find(p[0],a,b,!p[2].isEmpty());
                Code c=new Code();
                if(op!=null){
                    c=Encoder.encode(op,a,b,symbols,set);
                    resolveRelative(op,a,address,c);
                    out.write(c.toByteArray());address+=c.size();
                }
                symbols.addInstruction(work,c,address-c.size());
                printRow(address-c.size(),raw,c);
            }
        }catch(IOException e){throw new RuntimeException(e);}
    }

    private boolean processData(String line,Code c){
        String t=line.trim();int colon=t.indexOf(':');if(colon>=0)t=t.substring(colon+1).trim();
        String[] p=t.split("\\s+",3);
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

    private void resolveRelative(Instruction op,Operand target,long address,Code c){
        if(target.kind!=Operand.Kind.SYM)return;
        int width=op.imm().contains("rel8")?1:op.imm().contains("rel32")?4:0;
        if(width==0)return;
        Symbol s=symbols.find(target.text);
        if(s==null||s.section!='T')return;
        long disp=s.address-(address+c.size());
        c.patchLE(c.size()-width,disp,width);
        tit.markResolved(target.text,address);
    }

    private void printRow(long address,String source,Code c){
        System.out.printf("%-10s %-34s %s%n",String.format("%08X",address),
                c==null?"??":c.hex(),source==null?"":source);
    }
}
