package com.jayesh.assembler;

public class Encoder {
    public static Code encode(Instruction op, Operand a, Operand b, SymbolTable symbols, InstructionSet set) {
        Code c=new Code();
        boolean two=b.kind!=Operand.Kind.NONE;

        if(op.opcode().contains("+rd")){
            String base=op.opcode().substring(0,op.opcode().indexOf("+rd"));
            c.emitByte(Long.parseLong(base,16)+set.regCode(a.reg));
            if(op.form().contains("imm32"))c.emitLE(value(b,symbols),4);
            else if(op.form().contains("imm8"))c.emitByte(value(b,symbols));
            return c;
        }

        emitOpcode(op.opcode(),c);
        if(op.form().contains("r/m32"))modrm(op,a,b,c);
        Operand v=two?b:a;
        if(op.imm().contains("imm8"))c.emitByte(value(v,symbols));
        else if(op.imm().contains("imm32"))c.emitLE(value(v,symbols),4);
        else if(op.imm().contains("rel8"))c.emitByte(0);
        else if(op.imm().contains("rel32"))c.emitLE(0,4);
        return c;
    }

    private static long value(Operand o,SymbolTable symbols){
        if(o.kind==Operand.Kind.IMM)return Parser.number(o.text);
        Symbol s=symbols.find(o.text);
        if(s==null)return 0;
        if(s.size==0&&Parser.isNumber(s.value))return Parser.number(s.value);
        return s.address;
    }

    private static void emitOpcode(String opcode,Code c){
        String t=opcode.trim();
        for(int i=0;i<t.length();i+=2)c.emitByte(Long.parseLong(t.substring(i,i+2),16));
    }

    private static void modrm(Instruction op,Operand a,Operand b,Code c){
        Operand rm=op.form().equals("r/m32,r32")?a:b;
        int reg=setReg(op,a,b);
        int mod,rmCode,dispSize, sib=0;boolean hasSib=false;

        if(rm.kind==Operand.Kind.REG){mod=3;rmCode=rm.reg;dispSize=0;}
        else{
            if(rm.base<0){mod=0;rmCode=5;dispSize=4;}
            else{
                rmCode=(rm.base==4||rm.index>=0)?4:rm.base;
                if(rm.base==4||rm.index>=0){
                    hasSib=true;
                    int base=rm.base<0?5:rm.base;
                    int index=rm.index<0?4:rm.index;
                    int scale=switch(rm.scale){case 1->0;case 2->1;case 4->2;default->3;};
                    sib=(scale<<6)|(index<<3)|base;
                }
                if(!rm.hasDisp){mod=rm.base==5?1:0;dispSize=rm.base==5?1:0;}
                else if(rm.disp>=-128&&rm.disp<=127){mod=1;dispSize=1;}
                else{mod=2;dispSize=4;}
            }
        }
        c.emitByte((mod<<6)|(reg<<3)|rmCode);
        if(hasSib)c.emitByte(sib);
        if(dispSize==1)c.emitByte(rm.disp);
        else if(dispSize==4)c.emitLE(rm.disp,4);
    }

    private static int setReg(Instruction op,Operand a,Operand b){
        if(op.field().startsWith("/")&&op.field().length()>1&&Character.isDigit(op.field().charAt(1)))
            return op.field().charAt(1)-'0';
        if(op.form().equals("r/m32,r32"))return b.reg;
        if(op.form().equals("r32,r/m32"))return a.reg;
        return 0;
    }
}
