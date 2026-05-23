package stepper.model.sql;

import java.util.*;

public class SymbolSpace extends ArrayList<Symbol>{
    
    public SymbolSpace(){}
    
    public SymbolSpace(ArrayList<Symbol> symbols){
        this.addAll(symbols);
    }
    
    public int arity(){
        return this.size();
    }
    
    public ArrayList<Symbol> symbols(){
        return this;
    }
    
    public SymbolSpace subtract(SymbolSpace space){
        SymbolSpace remains = new SymbolSpace(this);
        for(Symbol symbol: space) if(!remains.remove(symbol)) return null;
        return remains;
    }
    
    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof SymbolSpace)) return false;
        SymbolSpace space = (SymbolSpace)obj;
        if(this.size()!=space.size()) return false;
        return this.contains(space, false) && space.contains(this, false);
    }
    
    public boolean contains(SymbolSpace space, boolean similar){
        if(space.arity()>this.arity()) return false;
        return space.stream().noneMatch((symbol) -> (!this.contains(symbol, similar)));
    }
    
    public boolean contains(Symbol symbol, boolean similar){
        if(!similar) return this.contains(symbol);
        return this.stream().anyMatch((s) -> (s.similar(symbol)));
    }
    
    public ArrayList<Integer> indexesOf(Symbol symbol){
        ArrayList<Integer> levels = new ArrayList();
        ArrayList<Integer> labels = new ArrayList();
        for(int i=0; i<this.size(); i++){
            if(this.get(i).equalsWithLevel(symbol)) levels.add(i);
            else if(this.get(i).equalsWithName(symbol)) labels.add(i);
        }
        return levels.isEmpty() ? labels : levels;
    }
    
    public String sequence(){
        if(this.isEmpty()) return "";
        
        String symbols = "";
        symbols = this.stream().map((s) -> "," + s.toString()).reduce(symbols, String::concat);
        return symbols.substring(1);
    }
    
    @Override
    public String toString(){
        if(this.isEmpty()) return "";
        
        String symbols = "";
        symbols = this.stream().map((s) -> "," + s.label()).reduce(symbols, String::concat);
        return "<" + symbols.substring(1) + ">";
    }
    
    public boolean checking(HashMap<String, Integer> cons){
        int num = 0;
        for(Integer n: cons.values()) num += n;
        if(num>this.arity()) return false;
        for(String sym: cons.keySet()) if(count(Arrays.asList(Symbol.harmonies(sym)))<cons.get(sym)) return false;
        return true;
    }
    
    private int count(List<String> labels){
        int num = 0;
        for(Symbol sym: this) if(labels.contains(sym.label)) num++;
        return num;
    }
}
