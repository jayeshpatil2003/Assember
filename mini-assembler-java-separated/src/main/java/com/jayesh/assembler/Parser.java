package com.jayesh.assembler;

import java.util.*;

public final class Parser {
    private static final String[] REGS={"eax","ecx","edx","ebx","esp","ebp","esi","edi"};
    private Parser(){}

    public static String clean(String s){ return s==null?"":s.trim(); }
    public static String lower(String s){ return clean(s).toLowerCase(Locale.ROOT); }

    public static void removeComment(StringBuilder s){
        boolean quote=false;
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);
            if(c=='"') quote=!quote;
            else if(c==';'&&!quote){s.setLength(i);return;}
        }
    }

    public static int regId(String s){
        s=lower(s);
        for(int i=0;i<REGS.length;i++) if(REGS[i].equals(s)) return i;
        return -1;
    }

    public static boolean isNumber(String s){
        try{ number(s); return true; }catch(Exception e){return false;}
    }

    public static long number(String s){
        s=clean(s).replace("_","");
        if(s.startsWith("-0x")||s.startsWith("-0X")) return -Long.parseLong(s.substring(3),16);
        if(s.startsWith("0x")||s.startsWith("0X")) return Long.parseLong(s.substring(2),16);
        if(s.startsWith("0b")||s.startsWith("0B")) return Long.parseLong(s.substring(2),2);
        return Long.parseLong(s);
    }

    public static Operand operand(String text){
        Operand o=new Operand();
        String t=clean(text);
        String low=lower(t);
        if(low.startsWith("dword")) t=clean(t.substring(5));
        else if(low.startsWith("word")) t=clean(t.substring(4));
        else if(low.startsWith("byte")) t=clean(t.substring(4));
        o.text=t;
        if(t.contains("[")&&parseMemory(t,o)) return o;
        o.reg=regId(t);
        if(o.reg>=0){o.kind=Operand.Kind.REG;return o;}
        if(isNumber(t)){o.kind=Operand.Kind.IMM;return o;}
        o.kind=Operand.Kind.SYM;
        return o;
    }

    private static boolean parseMemory(String text,Operand o){
        int l=text.indexOf('['), r=text.lastIndexOf(']');
        if(l<0||r<=l)return false;
        String inside=text.substring(l+1,r).replace("-","+");
        for(String token:inside.split("\\+")){
            token=clean(token);
            if(token.isEmpty())continue;
            int star=token.indexOf('*');
            if(star>=0){
                int reg=regId(token.substring(0,star));
                if(reg>=0&&isNumber(token.substring(star+1))){
                    o.index=reg;o.scale=(int)number(token.substring(star+1));continue;
                }
            }
            int reg=regId(token);
            if(reg>=0)o.base=reg;
            else if(isNumber(token)){o.disp=number(token);o.hasDisp=true;}
            else{o.symbol=token;o.hasDisp=true;}
        }
        o.kind=Operand.Kind.MEM;
        return true;
    }

    public static String[] splitOperands(String rest){
        int bracket=0;boolean quote=false;
        for(int i=0;i<rest.length();i++){
            char c=rest.charAt(i);
            if(c=='"')quote=!quote;
            else if(!quote){
                if(c=='[')bracket++;
                if(c==']')bracket--;
                if(c==','&&bracket==0)
                    return new String[]{clean(rest.substring(0,i)),clean(rest.substring(i+1))};
            }
        }
        return new String[]{clean(rest),""};
    }

    public static String[] instruction(String line){
        String t=clean(line);
        int colon=t.indexOf(':');
        if(colon>=0)t=clean(t.substring(colon+1));
        if(t.isEmpty())return new String[0];
        int sp=t.indexOf(' ');
        if(sp<0)return new String[]{lower(t),"",""};
        String mn=lower(t.substring(0,sp));
        String[] ab=splitOperands(t.substring(sp+1));
        return new String[]{mn,ab[0],ab[1]};
    }

    public static boolean dataDirective(String s){
        s=lower(s);
        return List.of("db","dw","dd","resb","resw","resd").contains(s);
    }

    public static int directiveWidth(String dir){
        return switch(lower(dir)){case "db","resb"->1;case "dw","resw"->2;case "dd","resd"->4;default->0;};
    }

    public static int dataItemCount(String text){
        int count=0;boolean quote=false,has=false;
        for(char c:text.toCharArray()){
            if(c=='"')quote=!quote;
            if(!quote&&c==','){if(has)count++;has=false;}
            else if(!Character.isWhitespace(c))has=true;
        }
        if(has)count++;
        return count;
    }

    public static int dataSize(String text,int width){
        int size=0;boolean quote=false;
        StringBuilder token=new StringBuilder();
        for(char c:text.toCharArray()){
            if(c=='"'){
                if(quote){size+=token.length();token.setLength(0);}
                quote=!quote;
            }else if(!quote&&c==','){
                if(token.length()>0){size+=width;token.setLength(0);}
            }else if(quote)token.append(c);
            else if(!Character.isWhitespace(c))token.append(c);
        }
        if(token.length()>0)size+=width;
        return size;
    }

    public static String formatValue(String s){ return clean(s).replaceAll("\\s+",""); }

    public static void parseLabel(String line, LabelConsumer consumer){
        int colon=line.indexOf(':');
        if(colon>=0){
            String label=clean(line.substring(0,colon));
            if(!label.isEmpty())consumer.accept(label);
        }
    }

    @FunctionalInterface public interface LabelConsumer { void accept(String label); }
}
