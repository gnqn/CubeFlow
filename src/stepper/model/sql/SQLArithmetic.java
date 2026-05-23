package stepper.model.sql;

import java.util.*;

public class SQLArithmetic extends SQLFunction{
    public SQLArithmetic(int op, String name){
        super(op, name);
    }
    
    @Override
    protected SQLFunction makingSymbol(){
        Attribute in = null, in2 = null;
        for(Object obj:params){
            if(!(obj instanceof Attribute)) continue;
            if(in==null) in = (Attribute)obj;
            else if(in2==null) in2 = (Attribute)obj;
        }
        if(in==null || in2==null) return this;
        
        this.symbol = in.symbol==null || in2.symbol==null ? new Symbol(Symbol.NUMERIC) : 
                      this.op==SQLItem.Operator.DIVISION || this.op==SQLItem.Operator.DIVISIONR || 
                      in.symbol.is(Symbol.NUMERIC) || in2.symbol.is(Symbol.NUMERIC) || 
                      (!in.symbol.is(Symbol.DATE) && !in2.symbol.is(Symbol.DATE)) ? 
                      new Symbol(Symbol.NUMERIC) : new Symbol(Symbol.INT);
        return this;
    }
    
    @Override
    public String func(){
        return op==SQLItem.Operator.MINUS ? "Minus" :
               op==SQLItem.Operator.ADD ? "Add" : 
               op==SQLItem.Operator.MULTIPLY ? "Product" :
               op==SQLItem.Operator.DIVISION ? "Divide" :
               op==SQLItem.Operator.MINUSR ? "MinusR" :
               op==SQLItem.Operator.DIVISIONR ? "DivideR" : "";
    }
    
    @Override
    public String property(){
        return params.get(0) + " " + SQLItem.Operator.toOperator(op, false) + " " + params.get(1);
    }
    
    @Override
    public String marks(){
        return params.get(0) + " " + SQLItem.Operator.toOperator(op, true) + " " + params.get(1);
    }
    
    @Override
    public String makingSQL(String dimensions, String m1, String m2){
        return expression(dimensions, m1, m2, false);
    }
    
    @Override
    public String makingHTML(String dimensions, String m1, String m2){
        return expression(dimensions, m1, m2, true);
    }
    
    private String expression(String dimensions, String m1, String m2, boolean html){
        SQLFunction f1 = this.isNestedFunction(params.get(0)) ? (SQLFunction)params.get(0) : null;
        SQLFunction f2 = this.isNestedFunction(params.get(1)) ? (SQLFunction)params.get(0) : null;
        String p1 = f1!=null ? (html ? f1.toHTML(dimensions, m1, m2, f1) : f1.toSQL(dimensions, m1, m2, f1)) : (m1 + params.get(0));
        String p2 = f2!=null ? (html ? f2.toHTML(dimensions, m1, m2, f2) : f2.toSQL(dimensions, m1, m2, f2)) : (m2 + params.get(1));
        return op==SQLItem.Operator.CONCAT ? (p1 + "," + p2) : 
               op==SQLItem.Operator.CONCATL ? p1 :
               op==SQLItem.Operator.CONCATR ? p2 : (p1 + " " + SQLItem.Operator.toOperator(op, html) + " " +  p2);
    }
    
    @Override
    public void compute(BitSet bits, double[] values){
        int idx = this.params.get(0) instanceof String ? 0 : this.params.get(1) instanceof String ? 1 : -1;
        if(idx==-1) return;
        
        double p = Double.parseDouble((String)this.params.get(idx));
        if(idx==0){
            for(int i=bits.nextSetBit(0); i>=0; i=bits.nextSetBit(i+1)){
                values[i] = op==SQLItem.Operator.ADD ? p + values[i] :
                            op==SQLItem.Operator.MINUS ? p - values[i] :
                            op==SQLItem.Operator.MINUSR ? values[i] - p:
                            op==SQLItem.Operator.MULTIPLY ? p * values[i] :
                            op==SQLItem.Operator.DIVISION ? p/values[i] : values[i]/p;
            }
        }else{
            for(int i=bits.nextSetBit(0); i>=0; i=bits.nextSetBit(i+1)){
                values[i] = op==SQLItem.Operator.ADD ? values[i] + p :
                            op==SQLItem.Operator.MINUS ? values[i] - p :
                            op==SQLItem.Operator.MINUSR ? p - values[i] :
                            op==SQLItem.Operator.MULTIPLY ? values[i] * p :
                            op==SQLItem.Operator.DIVISION ? values[i]/p : p/values[i];
            }
        }
    }
    
    @Override
    public double compute(double value){
        int idx = this.params.get(0) instanceof String ? 0 : 1;
        
        double p = Double.parseDouble((String)this.params.get(idx));
        return idx==0 ? (op==SQLItem.Operator.ADD ? p + value :
                         op==SQLItem.Operator.MINUS ? p - value :
                         op==SQLItem.Operator.MINUSR ? value - p:
                         op==SQLItem.Operator.MULTIPLY ? p * value :
                         op==SQLItem.Operator.DIVISION ? p/value : value/p) 
                      :
                        (op==SQLItem.Operator.ADD ? value + p :
                         op==SQLItem.Operator.MINUS ? value - p :
                         op==SQLItem.Operator.MINUSR ? p - value :
                         op==SQLItem.Operator.MULTIPLY ? value * p :
                         op==SQLItem.Operator.DIVISION ? value/p : p/value);
    }
    
    @Override
    public void compute(double[] values){
        int idx = this.params.get(0) instanceof String ? 0 : this.params.get(1) instanceof String ? 1 : -1;
        if(idx==-1) return;
        double p = Double.parseDouble((String)this.params.get(idx));
        for(int i=0; i<values.length; i++){
            values[i] = idx==0 ? (op==SQLItem.Operator.ADD ? p + values[i] :
                                  op==SQLItem.Operator.MINUS ? p - values[i] :
                                  op==SQLItem.Operator.MINUSR ? values[i] - p:
                                  op==SQLItem.Operator.MULTIPLY ? p * values[i] :
                                  op==SQLItem.Operator.DIVISION ? p/values[i] : values[i]/p) 
                               : 
                                 (op==SQLItem.Operator.ADD ? values[i] + p :
                                  op==SQLItem.Operator.MINUS ? values[i] - p :
                                  op==SQLItem.Operator.MINUSR ? p - values[i] :
                                  op==SQLItem.Operator.MULTIPLY ? values[i] * p :
                                  op==SQLItem.Operator.DIVISION ? values[i]/p : p/values[i]);
        }
    }
    
    @Override
    public void compute(BitSet bits, Condition cd, double[] values){
        int idx = this.params.get(0) instanceof String ? 0 : this.params.get(1) instanceof String ? 1 : -1;
        if(idx==-1) return;
        
        if(cd!=null && cd.isEmpty()) cd = null;
        double p = Double.parseDouble((String)this.params.get(idx));
        
        for(int i=bits.nextSetBit(0); i>=0; i=bits.nextSetBit(i+1)){
            if(cd!=null && !cd.compute(values[i])){bits.clear(i); continue;}
            values[i] = idx==0 ? (op==SQLItem.Operator.ADD ? p + values[i] :
                                  op==SQLItem.Operator.MINUS ? p - values[i] :
                                  op==SQLItem.Operator.MINUSR ? values[i] - p:
                                  op==SQLItem.Operator.MULTIPLY ? p * values[i] :
                                  op==SQLItem.Operator.DIVISION ? p/values[i] : values[i]/p) 
                               : 
                                 (op==SQLItem.Operator.ADD ? values[i] + p :
                                  op==SQLItem.Operator.MINUS ? values[i] - p :
                                  op==SQLItem.Operator.MINUSR ? p - values[i] :
                                  op==SQLItem.Operator.MULTIPLY ? values[i] * p :
                                  op==SQLItem.Operator.DIVISION ? values[i]/p : p/values[i]);
        }
    }
}
