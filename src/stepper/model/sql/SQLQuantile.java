package stepper.model.sql;

import java.util.*;
import it.unimi.dsi.fastutil.doubles.*;

public class SQLQuantile extends SQLAggregation{
    protected double p;
    public SQLQuantile(String name){
        this(SQLItem.Operator.QUANTILE, name);
    }
    
    public SQLQuantile(int op, String name){
        super(op, name);
        p = 0.25;
    }
    
    @Override
    protected SQLFunction makingSymbol(){
        this.symbol = this.inputSymbol();
        return this;
    }
    
    @Override
    public String func(){
        return "Quantile";
    }
    
    public double p(){
        return p;
    }
    
    @Override
    public String makingSQL(String dimensions, String m1, String m2, boolean clickhouse){
        return clickhouse ? ("quantile(" + p + ")(" + params.get(0) + ")") : ("Quantile(" + params.get(0) + ", " + p + ")"); 
    }
    
    @Override
    public String makingHTML(String dimensions, String m1, String m2){
        return "<span>Quantile</span>(" + params.get(0) + ", " + p + ")"; 
    }
    
    public double compute(DoubleArrayList inputs){
        double[] data = inputs.toDoubleArray();
        Arrays.sort(data);
        double pos = p * (data.length + 1);
        if(pos<1) return data[0];
        if(pos>=data.length) return data[data.length-1];
        int lowerPos = (int) Math.floor(pos) - 1; // 向下取整的位置
        double fraction = pos - Math.floor(pos);
        return data[lowerPos] + fraction * (data[lowerPos + 1] - data[lowerPos]);
    }
}
