package stepper.model.sql;

import java.util.*;

public class Condition extends ArrayList<Predicate>{
    public Condition(){}
    
    public Condition(ArrayList<Predicate> ands){
        if(ands!=null && !ands.isEmpty()) this.addAll(ands);
    }
    
    public DimensionSpace getEQDimensions(){
        DimensionSpace space = new DimensionSpace();
        this.stream().filter((p) -> p.isEqualityPredicate()).forEach((p) -> {
            Attribute attr = p.getAttribute();
            if(attr instanceof Dimension) space.add((Dimension)attr);
            else if(attr instanceof Attribute) space.add(new Dimension(attr));
        });
        return space;
    }
    
    public boolean hasNEQ(){
        for(Predicate pd: this) if(pd.op()!=SQLItem.Comparator.EQUAL) return true;
        return false;
    }
    
    public boolean has(Dimension d){
        return this.stream().anyMatch((p) -> (p.getAttribute().name.equalsIgnoreCase(d.name)));
    }
    
    public Predicate getPredicate(String attr){
        for(Predicate p: this) if(p.contains(attr)) return p;
        return null;
    }
    
    public Predicate getPredicate2(String attr){
        for(Predicate p: this) if(p.getAttribute().name.equalsIgnoreCase(attr)) return p;
        return null;
    }
    
    public Condition getAnds(String attr){
        Condition ands = new Condition();
        for(Predicate pd: this) if(pd.contains(attr)) ands.add(pd);
        return ands;
    }
    
    public Condition making(DimensionSpace ds){
        if(this.isEmpty() || ds.isEmpty()) return this;
        
        boolean ok = false;
        ArrayList<Predicate> list = new ArrayList();
        for(Predicate and: this){
            Predicate _and = and.making(ds);
            list.add(_and);
            if(and!=_and) ok = true;
        }
        return ok ? new Condition(list) : this;
    }
    
    public String html(){
        if(this.isEmpty()) return "";
        String and = " <span>AND</span> ", exp = "";
        for(Predicate p: this) exp += and + p.html();
        return exp.substring(and.length());
    }
    
    public String sequence(){
        if(this.isEmpty()) return "";
        String and = " AND ", exp = "";
        for(Predicate p: this) exp += and + p.sequence();
        return exp.substring(and.length());
    }
    
    public BitSet compute(String attr, BitSet mark, Object[] keys){
        return null;
    }
    
    public BitSet compute(Attribute attr, BitSet mark, Object[] keys){
        BitSet yes = null;
        for(Predicate pd: this.getAnds(attr.name)){
            if(yes==null) yes = mark==null ? pd.compute(attr.typ, keys) : pd.compute(mark, attr.typ, keys);
            else yes.and(mark==null ? pd.compute(attr.typ, keys) : pd.compute(mark, attr.typ, keys));
            if(yes.cardinality()==0) break;
        }
        return yes;
    }
    
    public boolean compute(double value){
        for(Predicate pd: this) if(!pd.compute(value)) return false;
        return true;
    }
    
    public void compute(BitSet bits, double[] values){
        for(Predicate pd: this) pd.compute(bits, values);
    }
        
    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof Condition)) return false;
        Condition c = (Condition)obj;
        if(this.size()!=c.size()) return false;
        for(Predicate p: this) if(c.indexOf(p)==-1) return false;
        return true;
    }
    
    
    @Override
    public String toString(){
        if(this.isEmpty()) return "";
        String and = " AND ", exp = "";
        for(Predicate p: this) exp += and + p;
        return exp.substring(and.length());
    }
}
