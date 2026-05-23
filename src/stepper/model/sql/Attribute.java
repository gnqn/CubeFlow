package stepper.model.sql;

import java.util.*;

public class Attribute{
    public int typ;
    protected int num;
    protected String name;
    protected Symbol symbol;
    
    public Attribute(){}
    
    public Attribute(String name){
        this.name = name;
    }
    
    public Attribute(Attribute attr){
        this.typ = attr.typ;
        this.name = attr.name;
    }
    
    public Attribute(String name, int num){
        this.num = num;
        this.name = name;
    }
    
    public String name(){
        return this.name;
    }
    
    public int num(){
        return this.num;
    }
    
    public void type(int typ){
        this.typ = typ;
    }
    
    public Attribute getAttribute(){
        return this;
    }
    
    public Symbol getSymbol(){
        return symbol;
    }
    
    public Symbol inputSymbol(){
        return this.symbol;
    }
    
    public boolean noneAttributes(){
        return false;
    }
    
    public String property(){
        return this.name;
    }
    
    public String makingSQL(String dimensions, String m1, String m2){
        return this.name;
    }
    
    public String makingSQL(String dimensions, String m1, String m2, boolean clickhouse){
        return makingSQL(dimensions, m1, m2);
    }
    
    public String makingHTML(String dimensions, String m1, String m2){
        return this.name;
    }
    
    public boolean isDate(){
        return this.symbol==null ? false : this.symbol.isDate();
    }
    
    public boolean outFrom(Attribute input){
        return this==input || this.name.equalsIgnoreCase(input.name);
    }
    
    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof Attribute)) return false;
        return this.name().equals(((Attribute)obj).name);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + Objects.hashCode(this.name);
        return hash;
    }
    
    @Override
    public String toString(){
        return this.name();
    }
    
    public static class Type{
        public static final int UTF8 = 1;
        public static final int DATEDAY = 2;
        public static final int TIME = 3;
    }
}
