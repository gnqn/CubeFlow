package stepper.model.sql;

import java.util.*;

public class DimensionSpace extends ArrayList<Dimension>{
    
    public DimensionSpace(){}
    
    public DimensionSpace(Dimension d){
        this.add(d);
    }
    
    public DimensionSpace(DimensionSpace copy){
        if(copy!=null) this.addAll(copy);
    }
    
    public DimensionSpace(DimensionSpace base, Dimension d){
        this.addAll(base);
        this.add(d);
    }
    
    public DimensionSpace(ArrayList<Dimension> list){
        if(list!=null) this.addAll(list);
    }
    
    public int arity(){
        return this.size();
    }
    
    public ArrayList<Dimension> getSpace(){
        return this;
    }
    
    //public DimensionSpace aligning(DimensionSpace reference){
    //    DimensionSpace space = new DimensionSpace(reference);
    //    for(Dimension dim: this) if(space.indexOf(dim)==-1) space.add(dim);
    //    return space;
    //}
    
    public boolean has(Object obj){
        if(obj instanceof Dimension) return this.contains((Dimension)obj);
        if(obj instanceof Symbol) return this.stream().anyMatch((dim) -> (dim.isSymbol((Symbol)obj)));
        if(obj instanceof SymbolSpace) return ((SymbolSpace)obj).stream().noneMatch((symbol) -> (!this.has(symbol)));
        if(obj instanceof DimensionSpace) return ((DimensionSpace)obj).stream().noneMatch((dim) -> (!this.has(dim)));
        return obj==null;
    }
    
    public boolean has_similar(Object obj){
        if(obj instanceof Symbol) return this.stream().anyMatch((dim) -> (dim.similar((Symbol)obj)));
        if(obj instanceof SymbolSpace) return ((SymbolSpace)obj).stream().noneMatch((symbol) -> (!this.has_similar(symbol)));
        return obj==null;
    }
    
    public boolean lessThan(DimensionSpace ds){
        if(ds.arity()>=this.arity()) return false;
        DimensionSpace dims1 = this.makingSpace().renaming();
        DimensionSpace dims2 = ds.makingSpace().renaming();
        return dims2.stream().noneMatch((d) -> (!dims1.contains(d)));
    }
    
    public boolean porder(DimensionSpace ds){
        if(ds.arity()>this.arity()) return false;
        return ds.stream().noneMatch((d) -> (!this.contains(d)));
    }
    
    public DimensionSpace min(DimensionSpace ds){
        return this.porder(ds) ? this : (ds.porder(this) ? ds : null);
    }
    
    public DimensionSpace commons(DimensionSpace ds){
        DimensionSpace comm = new DimensionSpace();
        this.stream().filter((dim) -> (ds.contains(dim))).forEach((dim) -> comm.add(dim));
        return comm;
    }
    
    public SymbolSpace toSymbolSpace(){
        ArrayList<Symbol> symbols = new ArrayList();
        this.stream().forEach((d) -> {symbols.add(d.getSymbol());});
        return new SymbolSpace(symbols);
    }
    
    public DimensionSpace aligning(DimensionSpace space){
        DimensionSpace list = new DimensionSpace();
        for(Dimension dim: space) if(this.getDimension(dim.name)!=null) list.add(dim);
        for(Dimension dim: this) if(space.getDimension(dim.name)==null) list.add(dim);
        return list;
    }
    
    public DimensionSpace leftCommons(DimensionSpace space){
        DimensionSpace list = new DimensionSpace();
        for(int i=0; i<this.size() && i<space.size(); i++){
            if(space.getDimension(this.get(i).name)==null) break; 
            list.add(this.get(i));
        }
        return list;
    }
    
    public DimensionSpace ending(DimensionSpace start){
        DimensionSpace space = new DimensionSpace();
        for(int i=start.size(); i<this.size(); i++) space.add(this.get(i));
        return space;
    }
    
    public boolean startsWith(DimensionSpace space){
        if(space==null || space.size()>this.size()) return false;
        for(int i=0; i<space.size(); i++) if(!this.get(i).hasName(space.get(i))) return false;
        return true;
    }
    
    public DimensionSpace makingSpace(){
        ArrayList<Dimension> list = new ArrayList();
        this.stream().forEach((d) -> {list.add(new Dimension(d));});
        return new DimensionSpace(list);
    }
    
    public static DimensionSpace makingSpace(ArrayList<Attribute> attrs){
        ArrayList<Dimension> ds = new ArrayList();
        for(Attribute attr: attrs) ds.add(new Dimension(attr));
        return new DimensionSpace(ds);
    }
    
    public DimensionSpace pairingDimensions(DimensionSpace dims){
        if(this.arity()!=dims.arity()) return this;
        for(int i=0; i<this.size(); i++){
            if(this.get(i).name().equalsIgnoreCase(dims.get(i).name())) continue;
            this.get(i).pair(dims.get(i).name());
        }
        return this;
    }
    
    public DimensionSpace subtract(Dimension dim){
        DimensionSpace space = new DimensionSpace();
        this.stream().filter((d) -> (!d.equals(dim))).forEach((d) -> {space.add(d);});
        return space;
    }
    
    public DimensionSpace subtractDimensions(ArrayList<Dimension> dims){
        if(dims==null || dims.isEmpty()) return this;
        DimensionSpace space = new DimensionSpace();
        this.stream().filter((d) -> (!dims.contains(d))).forEach((d) -> {space.add(d);});
        return space;
    }
    
    public DimensionSpace merge(DimensionSpace ds){
        ds.stream().filter((d) -> (!this.contains(d))).forEach((d) -> {this.add(d);});
        return this;
    }
    
    public DimensionSpace umerge(DimensionSpace ds){
        return this.makingSpace().merge(ds);
    }
    
    public DimensionSpaces getEqualities(DimensionSpaces spaces){
        DimensionSpaces result = new DimensionSpaces();
        spaces.stream().filter((space) -> (this.equals(space))).forEach((space) -> result.add(space));
        return result;
    }
    
    public DimensionSpaces getLesses(DimensionSpaces spaces){
        DimensionSpaces result = new DimensionSpaces();
        spaces.stream().filter((space) -> (this.lessThan(space))).forEach((space) -> result.add(space));
        return result;
    }
    
    public DimensionSpaces getLarges(DimensionSpaces spaces){
        DimensionSpaces result = new DimensionSpaces();
        spaces.stream().filter((space) -> (space.lessThan(this))).forEach((space) -> result.add(space));
        return result;
    }
    
    public Dimension getDimension(int i){
        return this.get(i);
    }
    
    public Dimension getDimension(String attr){
        for(Dimension d: this) if(d.name().equalsIgnoreCase(attr) || attr.equalsIgnoreCase(d.as())) return d;
        return null;
    }
    
    public Dimension getDimension(Symbol symbol, DimensionSpace nots){
        DimensionSpace space = this.getDimensions(symbol, nots);
        if(space.isEmpty()) return null;
        if(space.size()==1) return space.get(0);
        
        ArrayList<Double> distances = new ArrayList();
        for(Dimension d: space) distances.add(d.getSymbol().distance2(symbol));
        
        int idx = -1;
        for(int i=0; i<space.size(); i++){
            if(distances.get(i)<0) continue;
            if(idx==-1 || distances.get(i)<distances.get(idx)) idx = i;
        }
        return idx==-1 ? null : space.get(idx);
    }
    
    public ArrayList<Dimension> getDimensions(Symbol symbol){
        ArrayList<Dimension> options = new ArrayList();
        ArrayList<Integer> indexes = this.toSymbolSpace().indexesOf(symbol);
        indexes.stream().forEach((i) -> {options.add(this.get(i));});
        return options;
    }
    
    public DimensionSpace getDimensions(Symbol symbol, DimensionSpace nots){
        DimensionSpace space = new DimensionSpace();
        for(Dimension d:this){
            if(nots.contains(d)) continue;
            if(d.symbol.equalsWithName(symbol)) space.add(d);
        }
        return space;
    }
    
    public ArrayList<Attribute> toAttributes(){
        ArrayList<Attribute> attrs = new ArrayList();
        this.stream().forEach((d) -> {attrs.add(d);});
        return attrs;
    }
    
    public String sequence(){
        if(this.isEmpty()) return "";
        
        String seq = "";
        seq = this.stream().map((d) -> "," + d.name()).reduce(seq, String::concat);
        return seq.substring(1);
    }
    
    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof DimensionSpace)) return false;
        DimensionSpace ds = (DimensionSpace)obj;
        if(ds.size()!=this.size()) return false;
        return this.porder(ds) && ds.porder(this);
    }
    
    @Override
    public int hashCode() {
        int hash = 47 * 5;
        hash = this.stream().map((d) -> 31 * d.hashCode()).reduce(hash, Integer::sum);
        return hash;
    }
    
    @Override
    public String toString(){
        return this.sequence();
    }
    
    public DimensionSpace renaming(){
        this.stream().forEach((d) -> d.renaming());
        return this;
    }
    
    public DimensionSpace pairing(){
        DimensionSpace space = new DimensionSpace();
        this.stream().forEach((d) -> space.add(d.pairing()));
        return space;
    }
}
