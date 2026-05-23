package stepper.model.sql;

import java.util.*;

public class SQLPow extends SQLFunction{
    public SQLPow(String name){
        super(SQLItem.Operator.POW, name);
    }
    
    @Override
    protected SQLFunction makingSymbol(){
        this.symbol = new Symbol(Symbol.NUMERIC);
        return this;
    }
    
    @Override
    public String func(){
        return "Pow";
    }
    
    @Override
    public String property(){
        return func() + "(" + this.sequenceOfParams() + ")";
    }
    
    @Override
    public String marks(){
        return func() + "(" + this.params.get(1) + ")";
    }
    
    @Override
    public String makingSQL(String dimensions, String m1, String m2){
        return "pow(" + sequenceOfParams() + ")";
    }
    
    @Override
    public String makingHTML(String dimensions, String m1, String m2){
        return "<span>Pow</span>(" + sequenceOfParams() + ")";
    }
    
    @Override
    public double compute(double value){
        return Math.pow(value, Double.parseDouble((String)this.params.get(1)));
    }
    
    @Override
    public void compute(double[] values){
        double p = Double.parseDouble((String)this.params.get(1));
        for(int i=0; i<values.length; i++) values[i] = Math.pow(values[i], p);
    }
    
    @Override
    public void compute(BitSet bits, Condition cd, double[] values){
        if(cd!=null && cd.isEmpty()) cd = null;
        double p = Double.parseDouble((String)this.params.get(1));
        
        for(int i=bits.nextSetBit(0); i>=0; i=bits.nextSetBit(i+1)){
            if(cd!=null && !cd.compute(values[i])){bits.clear(i); continue;}
            values[i] = Math.pow(values[i], p);
        }
    }
}
