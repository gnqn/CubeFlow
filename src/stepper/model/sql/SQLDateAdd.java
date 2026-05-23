package stepper.model.sql;

import one.sys.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import stepper.util.*;
import it.unimi.dsi.fastutil.objects.*;

public class SQLDateAdd extends SQLFunction{
    private int opt = -1;
    private int delta;
    
    public SQLDateAdd(String name){
        super(SQLItem.Operator.DATEADD, name);
    }
    
    @Override
    protected SQLFunction makingSymbol(){
        this.symbol = new Symbol(Symbol.DATE);
        return this;
    }
    
    @Override
    public String func(){
        return "date_add";
    }
    
    @Override
    public String property(){
        return func() + "(" + this.sequenceOfParams() + ")";
    }
    
    @Override
    public String marks(){
        return func();
    }
    
    @Override
    protected boolean check(){
        if(!(this.params.get(0) instanceof Attribute)) return false;
        Symbol symb = ((Attribute)params.get(0)).getSymbol();
        return symb.isDate();
    }
    
    @Override
    protected SQLFunction setParams(ArrayList params)throws SYSException{
        super.setParams(params);
        
        Pattern pattern = Pattern.compile("^(?<num>[\\+\\-]?\\d+) *(?<typ>MONTH|DAY|YEAR|HOUR|MINUTE|SECOND)$");
        Matcher matcher = pattern.matcher(((String)params.get(1)).trim().toUpperCase());
        if(!matcher.find()) return this;
        
        String typ = matcher.group("typ");
        delta = Integer.parseInt(matcher.group("num"));
        opt = typ.equalsIgnoreCase("MONTH") ? 1 : 
              typ.equalsIgnoreCase("YEAR") ? 2 : 
              typ.equalsIgnoreCase("DAY") ? 3 : 
              typ.equalsIgnoreCase("HOUR") ? 4 : 
              typ.equalsIgnoreCase("MINUTE") ? 5 : 6;
        
        return this;
    }
    
    @Override
    public String makingSQL(String dimensions, String m1, String m2){
        if(opt>3) return params.get(0) + " + INTERVAL " + params.get(1) + " ";
        return opt==3 ? ("CAST(date_add(" + params.get(0) + ", INTERVAL '" + params.get(1) + "') AS DATE)") : 
                        ("date_add(" + params.get(0) + ", INTERVAL '" + params.get(1) + "')");
    }
    
    @Override
    public String makingHTML(String dimensions, String m1, String m2){
        return "<span>date_add</span>(" + sequenceOfParams() + ")";
    }
    
    @Override
    public void trans(Object[] dims, int i, BitSet[] marks, Object[][] elements, Object2IntMap[] maps, int[][] bases){
        Pattern pattern = Pattern.compile("^(?<num>[\\+\\-]?\\d+) *(?<typ>MONTH|DAY|YEAR)$");
        Matcher matcher = pattern.matcher(((String)params.get(1)).toUpperCase());
        if(!matcher.find()) return;
        
        
        String typ = matcher.group("typ");
        int n = Integer.parseInt(matcher.group("num"));
        int opt = typ.equalsIgnoreCase("MONTH") ? 1 : 
                  typ.equals("YEAR") ? 2 : 0;
        
        elements[i] = new Object[dims.length];
        maps[i] = new Object2IntOpenHashMap(dims.length);
        maps[i].defaultReturnValue(-1);
        
        for(int k=0; k<dims.length; k++){
            if(marks[i]!=null && !marks[i].get(k)) continue;
            LocalDate date = (LocalDate)dims[k];
            switch (opt) {
                case 0:
                    elements[i][k] = date.plusDays(n);
                    break;
                case 1:
                    elements[i][k] = date.plusMonths(n);
                    break;
                case 2:
                    elements[i][k] = date.plusYears(n);
                    break;
                default:
                    break;
            }
            maps[i].put(elements[i][k], k);
        }
    }
    
    @Override
    public void trans(int i, Object[][] dims, BitSet[] marks, Hyb2IntMap[] maps, int[][] bases){
        if(opt==-1) return;
        
        maps[i] = new Hyb2IntMap().initLongs(dims[i].length);
        switch(opt){
            case 3:
                dims[i] = DateHelper.addDays(delta, dims[i], marks[i], maps[i]);
                break;
            case 1:
                dims[i] = DateHelper.addMonths(delta, dims[i], marks[i], maps[i]);
                break;
            case 4:
                dims[i] = DateHelper.addHours(delta, dims[i], marks[i], maps[i]);
                break;
            case 5:
                dims[i] = DateHelper.addMinutes(delta, dims[i], marks[i], maps[i]);
                break;
            case 6:
                dims[i] = DateHelper.addSeconds(delta, dims[i], marks[i], maps[i]);
                break;
        }
    }
}