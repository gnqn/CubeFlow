package stepper.model.sql;

import one.sys.*;
import java.time.*;
import java.util.*;
import stepper.util.*;
import it.unimi.dsi.fastutil.objects.*;

public class SQLDateTrunc extends SQLFunction{
    private int opt = -1;
    
     public SQLDateTrunc(String name){
        super(SQLItem.Operator.DATETRUNC, name);
    }
    
    @Override
    protected SQLFunction makingSymbol(){
        this.symbol = new Symbol(Symbol.DATE);
        return this;
    }
    
     @Override
    public boolean isUnkey(){
        return true;
    }
    
    @Override
    public String func(){
        return "date_trunc";
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
        if(!(this.params.get(1) instanceof Attribute)) return false;
        Symbol symb = ((Attribute)params.get(1)).getSymbol();
        return symb.isDate();
    }
    
    @Override
    protected SQLFunction setParams(ArrayList params)throws SYSException{
        super.setParams(params);
        
        opt = ((String)params.get(0)).equalsIgnoreCase("'MONTH'") ? 1 : 
              ((String)params.get(0)).equalsIgnoreCase("'YEAR'") ? 2 : 0;
        
        return this;
    }
    
    @Override
    public String makingSQL(String dimensions, String m1, String m2){
        return "date_trunc(" + params.get(0) + "," + params.get(1) + ")";
    }
    
    @Override
    public String makingHTML(String dimensions, String m1, String m2){
        return "<span>date_trunc</span>(" + sequenceOfParams() + ")";
    }
    
    @Override
    public void trans(Object[] dims, int i, BitSet[] marks, Object[][] elements, Object2IntMap[] maps, int[][] bases){
        int opt = ((String)params.get(0)).equalsIgnoreCase("'MONTH'") ? 1 : 
                  ((String)params.get(0)).equalsIgnoreCase("'YEAR'") ? 2 : 0;
        
        int[] old = bases[i];
        Object[] todims = new Object[dims.length];
        bases[i] = new int[dims.length];
        maps[i] = new Object2IntOpenHashMap(dims.length);
        maps[i].defaultReturnValue(-1);
        for(int k=0; k<dims.length; k++){
            if(marks[i]!=null && !marks[i].get(k)) continue;
            LocalDate date = (LocalDate)dims[k];
            if(opt==1) date = date.withDayOfMonth(1);
            int idx = maps[i].getInt(date);
            if(idx==-1){
                idx = maps[i].size();
                todims[idx] = date;
                maps[i].put(date, idx);
            }
            bases[i][k] = old==null ? idx : old[idx];
        }
        elements[i] = new Object[maps[i].size()];
        System.arraycopy(todims, 0, elements[i], 0, maps[i].size());
    }
    
    @Override
    public void trans(int i, Object[][] dims, BitSet[] marks, Hyb2IntMap[] maps, int[][] bases){
        bases[i] = new int[dims[i].length];
        maps[i] = new Hyb2IntMap().initLongs(dims[i].length);
        switch(opt){
            case 1:
                dims[i] = DateHelper.truncToMonth(dims[i], marks[i], maps[i], bases[i]);
                break;
            case 2:
                dims[i] = DateHelper.truncToYear(dims[i], marks[i], maps[i], bases[i]);
                break;
        }
    }
}
