package stepper.model.sql;

import java.time.*;
import java.util.*;

public class Predicate {
    private final Attribute attr;
    private final Object param;
    private int op;
    
    public Predicate(Attribute attr, int op, String value){
        this.attr = attr;
        this.op = op;
        this.param = value;
    }
    
    public Predicate(Attribute attr, int op, Attribute attr2){
        this.attr = attr;
        this.op = op;
        this.param = attr2;
    }
    
    public Predicate(Attribute attr, Attribute attr2){
        this(attr, SQLItem.Comparator.EQUAL, attr2);
    }
    
    public int op(){
        return this.op;
    }
    
    public boolean isEqualityPredicate(){
        return this.op==SQLItem.Comparator.EQUAL;
    }
    
    public Attribute getAttribute(){
        return this.attr;
    }
    
    public Object getParameter(){
        return this.param;
    }
    
    public Object wellParameter(){
        if(this.param instanceof String){
            String p = (String)this.param;
            if(p.startsWith("'") && p.endsWith("'")) return p.substring(1, p.length()-1);
        }
        return this.param;
    }
    
    public Object wellParameter(int typ){
        if(this.param instanceof String){
            String p = (String)this.param;
            if(p.startsWith("'") && p.endsWith("'")) p = p.substring(1, p.length()-1);
            return typ==Attribute.Type.DATEDAY ? LocalDate.parse(p).toEpochDay() : p;
        }
        return this.param;
    }
    
    public String getOperator(){
        return SQLItem.Comparator.toOperator(op, false);
    }
    
    public void reverse(){
        switch (this.op) {
            case SQLItem.Comparator.GREATER:
                this.op = SQLItem.Comparator.LESS;
                break;
            case SQLItem.Comparator.GREATERE:
                this.op = SQLItem.Comparator.LESSE;
                break;
            case SQLItem.Comparator.LESS:
                this.op = SQLItem.Comparator.GREATER;
                break;
            case SQLItem.Comparator.LESSE:
                this.op = SQLItem.Comparator.GREATERE;
                break;
            default:
                break;
        }
    }
    
    public boolean contains(Attribute attr){
        return this.attr.equals(attr) || (this.param instanceof Attribute && this.param.equals(attr));
    }
    
    public boolean contains(String attr){
        return this.attr.name.equalsIgnoreCase(attr) || 
               (this.param instanceof Attribute && ((Attribute)this.param).name.equalsIgnoreCase(attr));
    }
    
    public Predicate making(DimensionSpace ds){
        if(ds.isEmpty() || this.attr==null) return this;
        Dimension dim = ds.getDimension(this.attr.name());
        return dim==null ? this : this.param instanceof String ? new Predicate(dim, this.op, (String)this.param) : new Predicate(dim, this.op, (Attribute)this.param);
    }
    
    public String html(){
        if(param!=null && param.equals("null")){
            if(op==SQLItem.Comparator.EQUAL) return attr.name() + " <span>IS NULL</span>";
            if(op==SQLItem.Comparator.NEQUAL) return attr.name() + " <span>IS NOT NULL</span>";
        }
        return attr.name() + SQLItem.Comparator.toOperator(op, true) + param;
    }
    
    public String sequence(){
        if(param!=null && param.equals("null")){
            if(op==SQLItem.Comparator.EQUAL) return attr.name() + " IS NULL";
            if(op==SQLItem.Comparator.NEQUAL) return attr.name() + " IS NOT NULL";
        }
        return attr.name() + SQLItem.Comparator.toOperator(op, false) + param;
    }
    
    public String sequence(String in, String in2, boolean html){
        return in + "." + attr.name() + SQLItem.Comparator.toOperator(op, html) + in2 + "." + param;
    }
    
    public boolean compute(double value){
        double p = Double.parseDouble((String)param);
        return (this.op==SQLItem.Comparator.EQUAL && value==p) ||
               (this.op==SQLItem.Comparator.NEQUAL && value!=p) || 
               (this.op==SQLItem.Comparator.LESS && value<p) ||
               (this.op==SQLItem.Comparator.LESSE && value<=p) || 
               (this.op==SQLItem.Comparator.GREATER && value>p) ||
               (this.op==SQLItem.Comparator.GREATERE && value>=p);
    }
    
    public void compute(BitSet bits, double[] values){
        double p = Double.parseDouble((String)param);
        for(int i=bits.nextSetBit(0); i>=0; i=bits.nextSetBit(i+1)){
            if(!((this.op==SQLItem.Comparator.EQUAL && values[i]==p) ||
               (this.op==SQLItem.Comparator.NEQUAL && values[i]!=p) || 
               (this.op==SQLItem.Comparator.LESS && values[i]<p) ||
               (this.op==SQLItem.Comparator.LESSE && values[i]<=p) || 
               (this.op==SQLItem.Comparator.GREATER && values[i]>p) ||
               (this.op==SQLItem.Comparator.GREATERE && values[i]>=p))) bits.clear(i);
        }
    }
    
    public BitSet compute(BitSet bits, int typ, Object[] keys){
        Object ob = this.wellParameter(typ);
        BitSet yes = new BitSet(keys.length);
        for(int i=bits.nextSetBit(0); i>=0; i=bits.nextSetBit(i+1)){
            Comparable c = (Comparable)keys[i];
            if((this.op==SQLItem.Comparator.EQUAL && c.equals(ob)) || 
               (this.op==SQLItem.Comparator.NEQUAL && !c.equals(ob)) ||
               (this.op==SQLItem.Comparator.LESS && c.compareTo(ob)<0) ||
               (this.op==SQLItem.Comparator.LESSE && c.compareTo(ob)<=0) || 
               (this.op==SQLItem.Comparator.GREATER && c.compareTo(ob)>0) ||
               (this.op==SQLItem.Comparator.GREATERE && c.compareTo(ob)>=0)) yes.set(i);
        }
        return yes;
    }
    
    public BitSet compute(int typ, Object[] keys){
        Object ob = this.wellParameter(typ);
        BitSet yes = new BitSet(keys.length);
        for(int i=0; i<keys.length; i++){
            Comparable c = (Comparable)keys[i];
            if((this.op==SQLItem.Comparator.EQUAL && c.equals(ob)) || 
               (this.op==SQLItem.Comparator.NEQUAL && !c.equals(ob)) ||
               (this.op==SQLItem.Comparator.LESS && c.compareTo(ob)<0) ||
               (this.op==SQLItem.Comparator.LESSE && c.compareTo(ob)<=0) || 
               (this.op==SQLItem.Comparator.GREATER && c.compareTo(ob)>0) ||
               (this.op==SQLItem.Comparator.GREATERE && c.compareTo(ob)>=0)) yes.set(i);
        }
        return yes;
    }
    
    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof Predicate)) return false;
        Predicate pd = (Predicate)obj;
        return pd.attr.equals(this.attr) && pd.op==this.op && pd.param.equals(this.param);
    }
    
    
    @Override
    public String toString(){
        return attr.name() + SQLItem.Comparator.toOperator(op, false) + param;
    }
}
