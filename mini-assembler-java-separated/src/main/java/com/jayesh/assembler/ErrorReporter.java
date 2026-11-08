package com.jayesh.assembler;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class ErrorReporter {
    private final Path table;
    private final Map<String,ErrorDefinition> definitions = new LinkedHashMap<>();
    private final List<ErrorOccurrence> errors = new ArrayList<>();

    public ErrorReporter(Path table){ this.table=table; load(); }

    private void load(){
        if(!table.toFile().exists()) {
            System.err.println("Cannot open predefined error catalogue: " + table);
            return;
        }
        boolean inside=false;
        try(BufferedReader br=new BufferedReader(new FileReader(table.toFile()))) {
            String line;
            while((line=br.readLine())!=null){
                line=line.trim();
                if(line.equals("[ERROR_DEFINITIONS]")){inside=true;continue;}
                if(line.equals("[END_ERROR_DEFINITIONS]")) break;
                if(!inside||line.isEmpty()||line.startsWith("#")) continue;
                String[] f=line.split("\\|",5);
                if(f.length==5) definitions.put(f[0].trim(),
                        new ErrorDefinition(f[0].trim(),f[1].trim(),f[2].trim(),f[3].trim(),f[4].trim()));
            }
        }catch(IOException e){ throw new RuntimeException(e); }
    }

    public void report(String code,int line,String source,String detail){
        if(!definitions.containsKey(code)) {
            System.err.println("Internal: error code " + code + " is absent from " + table);
            return;
        }
        for(ErrorOccurrence e:errors)
            if(e.code().equals(code)&&e.line()==line&&e.detail().equals(detail==null?"":detail)) return;
        errors.add(new ErrorOccurrence(code,line,source==null?"":source,detail==null?"":detail));
    }

    public boolean hasErrors(){ return !errors.isEmpty(); }
    public int size(){ return errors.size(); }

    public void write(Path file){
        try(PrintWriter out=new PrintWriter(file.toFile())){
            out.println("ASSEMBLER ERROR REPORT");
            out.println("================================================================================");
            if(errors.isEmpty()) out.println("No errors detected.");
            for(ErrorOccurrence e:errors){
                ErrorDefinition d=definitions.get(e.code());
                out.printf("%nERROR %s | Line %d%n",e.code(),e.line());
                if(d!=null) out.printf("Category   : %s%nError      : %s%nDescription: %s%n",
                        d.category(),d.name(),d.description());
                out.printf("Source     : %s%nDetail     : %s%n",e.source(),e.detail());
            }
            out.println("%n================================================================================");
            out.println("Total errors: " + errors.size());
            out.println("Assembly status: " + (errors.isEmpty()?"SUCCESS":"FAILED"));
        }catch(IOException e){ throw new RuntimeException(e); }
    }
}
