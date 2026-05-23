package stepper.model.sql;

public class Dimension extends Attribute{
    private String as;
    private String pair;
    
    public Dimension(Dimension copy){
        super(copy.name);
        this.as = copy.as;
        this.typ = copy.typ;
        this.pair = copy.name;
        this.symbol = copy.symbol;
    }
    
    public Dimension(Attribute attr){
        super(attr.name);
        this.typ = attr.typ;
        this.symbol = attr.symbol;
        if(attr instanceof Dimension) this.as = ((Dimension)attr).as;
    }
    
    public Dimension(Dimension copy, String name){
        super(name);
        this.as = copy.as;
        this.typ = copy.typ;
        this.pair = copy.name;
        this.symbol = copy.symbol;
    }
    
    public String as(){
        return this.as;
    }
    
    public Dimension as(String alias){
        this.as = alias;
        return this;
    }
    
    public String pair(){
        return this.pair;
    }
    
    public Dimension pair(String pair){
        this.pair = pair;
        return this;
    }
    
    public void aligning(Dimension dim){
        if(dim==null) return;
        this.as = dim.as;
        this.pair = dim.pair;
    }
    
    public Dimension renaming(){
        if(this.as!=null) this.name = this.as;
        this.as = null;
        return this;
    }
    
    public Dimension pairing(){
        return this.pair==null ? this : new Dimension(this, this.pair);
    }
    
    @Override
    public Symbol getSymbol(){
        return symbol;
    }
    
    public String toSymbol(){
        return symbol.toString();
    }
    
    public boolean hasName(String name){
        return name!=null && (name.equalsIgnoreCase(this.name) || name.equalsIgnoreCase(this.as));
    }
    
    public boolean hasName(Dimension d){
        if(this==d) return true;
        if(d==null || d.name==null) return false;
        if(this.as!=null && this.as.equals(d.name)) return true;
        if(!d.name.equals(this.name)) return false;
        if(d.as!=null && !d.as.equals(this.as)) return false;
        return this.as==null || this.as.equals(d.as);
    }
    
    public boolean similar(Symbol symbol){
        return this.symbol.similar(symbol);
    }
    
    public boolean isSymbol(Symbol symbol){
        return this.symbol.equalsWithName(symbol);
    }
    /*
    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof Dimension)) return false;
        Dimension d = (Dimension)obj;
        return this.name.equalsIgnoreCase(d.name) || 
               (this.as!=null && this.as.equalsIgnoreCase(d.name)) ||
               (d.as!=null && d.as.equalsIgnoreCase(this.name));
    }*/
}
