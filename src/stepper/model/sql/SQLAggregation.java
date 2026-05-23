package stepper.model.sql;

import java.util.*;
import it.unimi.dsi.fastutil.objects.*;

public class SQLAggregation extends SQLFunction{
    public SQLAggregation(int op, String name){
        super(op, name);
    }
    
    @Override
    protected SQLFunction makingSymbol(){
        this.symbol = this.op==SQLItem.Operator.MAX || this.op==SQLItem.Operator.MIN ||
                      this.op==SQLItem.Operator.SUM || this.op==SQLItem.Operator.RAW ? this.inputSymbol() : 
                      this.op==SQLItem.Operator.COUNT || this.op==SQLItem.Operator.COUNTDISTINCT ? 
                      new Symbol(Symbol.INT) : new Symbol(Symbol.NUMERIC);
        return this;
    }
    
    @Override
    public String func(){
        return op==SQLItem.Operator.RAW ? "Raw" :
               op==SQLItem.Operator.AVG ? "Avg" :
               op==SQLItem.Operator.SUM ? "Sum" :
               op==SQLItem.Operator.MAX ? "Max" :
               op==SQLItem.Operator.MIN ? "Min" :
               op==SQLItem.Operator.COUNT ? "Count" :
               op==SQLItem.Operator.COUNTDISTINCT ? "CountDistinct" : "";
    }
    
    @Override
    public String property(){
        String func = func(), ps = this.sequenceOfParams();
        return func.length()==0 ? "" : ps.length()==0 ? func : (func + "," + ps);
    }
    
    @Override
    public String marks(){
        return func();
    }
    
    @Override
    protected boolean check(){
        //return (op != SQLItem.Operator.SUM && op != SQLItem.Operator.AVG && op != SQLItem.Operator.RAVG && op != SQLItem.Operator.MAX && op != SQLItem.Operator.MIN && op != SQLItem.Operator.COUNTDISTINCT) 
        //       || this.getAttribute()!=null || !this.getSymbols().isEmpty();
        return true;
    }
    
    @Override
    public String makingSQL(String dimensions, String m1, String m2){
        if(op==SQLItem.Operator.RAW) return toSQL(dimensions, m1, m2, params.get(0));
        if(op==SQLItem.Operator.AVG) return "AVG(" + toSQL(dimensions, m1, m2, params.get(0)) + ")";
        if(op==SQLItem.Operator.SUM) return "SUM(" + toSQL(dimensions, m1, m2, params.get(0)) + ")";
        if(op==SQLItem.Operator.MAX) return "MAX(" + toSQL(dimensions, m1, m2, params.get(0)) + ")";
        if(op==SQLItem.Operator.MIN) return "MIN(" + toSQL(dimensions, m1, m2, params.get(0)) + ")";
        if(op==SQLItem.Operator.COUNT) return "COUNT(" + this.sequenceOfParams() + ")";
        if(op==SQLItem.Operator.COUNTDISTINCT) return "COUNT(DISTINCT(" + toSQL(dimensions, m1, m2, params.get(0)) + "))";
        return "";
    }
    
    @Override
    public String makingHTML(String dimensions, String m1, String m2){
        if(op==SQLItem.Operator.RAW) return toHTML(dimensions, m1, m2, params.get(0));
        if(op==SQLItem.Operator.AVG) return "<span>AVG</span>(" + toHTML(dimensions, m1, m2, params.get(0)) + ")";
        if(op==SQLItem.Operator.SUM) return "<span>SUM</span>(" + toHTML(dimensions, m1, m2, params.get(0)) + ")";
        if(op==SQLItem.Operator.MAX) return "<span>MAX</span>(" + toHTML(dimensions, m1, m2, params.get(0)) + ")";
        if(op==SQLItem.Operator.MIN) return "<span>MIN</span>(" + toHTML(dimensions, m1, m2, params.get(0)) + ")";
        if(op==SQLItem.Operator.COUNT) return "<span>COUNT</span>(" + this.sequenceOfParams() + ")";
        if(op==SQLItem.Operator.COUNTDISTINCT) return "<span>COUNT</span>(<span>DISTINCT</span>(" + toHTML(dimensions, m1, m2, params.get(0)) + "))";
        return "";
    }
    
    public void compute(int loc, double[] values, BitSet bits, double[] inputs, Predicate pd, BitSet bs){
        int num = bs.cardinality();
        if(num==0) return;
        
        bits.set(loc);
        if(op==SQLItem.Operator.COUNT){
            values[loc] = num;
        }else if(op==SQLItem.Operator.SUM){
            double sum = 0;
            for(int i=bs.nextSetBit(0); i>=0; i=bs.nextSetBit(i+1)) sum += inputs[i];
            values[loc] = sum;
        }else if(op==SQLItem.Operator.AVG){
            double sum = 0;
            for(int i=bs.nextSetBit(0); i>=0; i=bs.nextSetBit(i+1)) sum += inputs[i];
            values[loc] = sum/num;
        }else if(op==SQLItem.Operator.MAX){
            double max = Double.MIN_VALUE;
            for(int i=bs.nextSetBit(0); i>=0; i=bs.nextSetBit(i+1)) if(inputs[i]>max) max = inputs[i];
            values[loc] = max;
        }else if(op==SQLItem.Operator.MIN){
            double min = Double.MAX_VALUE;
            for(int i=bs.nextSetBit(0); i>=0; i=bs.nextSetBit(i+1)) if(inputs[i]<min) min = inputs[i];
            values[loc] = min;
        }
    }
    
    @Override
    public double aggregate(BitSet bits, double[] values){
        switch(op){
            case SQLItem.Operator.COUNT:
                return bits.cardinality();
            case SQLItem.Operator.SUM:
                double sum = 0;
                for(int i=bits.nextSetBit(0); i>=0; i=bits.nextSetBit(i+1)) sum += values[i];
                return sum;
            case SQLItem.Operator.AVG:
                sum = 0;
                for(int i=bits.nextSetBit(0); i>=0; i=bits.nextSetBit(i+1)) sum += values[i];
                return sum/bits.cardinality();
            case SQLItem.Operator.MAX:
                double max = Double.MIN_VALUE;
                for(int i=bits.nextSetBit(0); i>=0; i=bits.nextSetBit(i+1)) if(values[i]>max) max = values[i];
                return max;
            case SQLItem.Operator.MIN:
                double min = Double.MAX_VALUE;
                for(int i=bits.nextSetBit(0); i>=0; i=bits.nextSetBit(i+1)) if(values[i]<min) min = values[i];
                return min;
            default:
                return 0;
        }
    }
    
    public double aggregate(double[] inputs){
        switch(op){
            case SQLItem.Operator.COUNT:
                return inputs.length;
            case SQLItem.Operator.SUM:
                double sum = 0;
                for(double d: inputs) sum += d;
                return sum;
            case SQLItem.Operator.AVG:
                sum = 0;
                for(double d: inputs) sum += d;
                return sum/inputs.length;
            case SQLItem.Operator.MAX:
                double max = Double.MIN_VALUE;
                for(double d: inputs) if(d>max) max = d;
                return max;
            case SQLItem.Operator.MIN:
                double min = Double.MAX_VALUE;
                for(double d: inputs) if(d<min) min = d;
                return min;
            default:
                return 0;
        }
    }
    
    public double compute(double v1, double v2){
        switch(op){
            case SQLItem.Operator.COUNT:
                return v1 + 1;
            case SQLItem.Operator.SUM:
                return v1 + v2;
            case SQLItem.Operator.AVG:
                return v1 + v2;
            case SQLItem.Operator.MAX:
                return v1>=v2 ? v1 : v2;
            case SQLItem.Operator.MIN:
                return v1<=v2 ? v1 : v2;
            default:
                return 0;
        }
    }
    
    public void compute(Object[] dims, Object2IntMap maps, double v1, double v2){
        int idx = maps.computeIfAbsent(v1, k->{
            dims[dims.length-1] = k;
            return dims.length;
        });
    }
}
