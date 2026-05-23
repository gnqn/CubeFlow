package stepper.model.sql;


public class Symbol {
    protected int down;
    protected double n1;
    protected double n2;
    protected String label;
    
    private Symbol(String label){
        this.label = label;
    }
    
    public Symbol(String label, int level, double n1, double n2){
        this.label = label;
        this.down = level;
        this.n1 = n1;
        this.n2 = n2;
    }
    
    public Symbol(int typ){
        this.label = LABELS[typ];
    }
    
    public String label(){
        return this.label;
    }
    
    public boolean equalsWithName(Symbol symbol){
        return this.label.equals(symbol.label);
    }
    
    public boolean equalsWithLevel(Symbol symbol){
        if(!this.label.equals(symbol.label)) return false;
        return this.down==symbol.down || (this.down>0 && symbol.down>0);
    }
    
    public static boolean isOrdinal(int typ){
        return typ==DATE || typ==NUMERIC || typ==INT || typ==INTCATE;
    }
    
    public boolean isDate(){
        return this.label.equalsIgnoreCase(LABELS[DATE]);
    }
    
    public boolean is(int typ){
        return this.label.equals(Symbol.LABELS[typ]);
    }
    
    public boolean similar(Symbol symbol){
        return this.distance(symbol)>=0;
    }
    
    public int distance(Symbol symbol){
        if(!this.similar2(symbol)) return -1;
        return this.isDate() ? (this.down==symbol.down ? 0 : -1) : 0;
    }
    
    public boolean similar2(Symbol symbol){
        String l1 = this.label(), l2 = symbol.label();
        if(l1.equals(l2)) return true;
        
        if((l1.equals(LABELS[INT]) || l1.equals(LABELS[NUMERIC]) || l1.equals(LABELS[INTCATE])) && 
           (l2.equals(LABELS[INT]) || l2.equals(LABELS[NUMERIC]) || l2.equals(LABELS[INTCATE]))) return true;
        if((l1.equals(LABELS[LOCATION]) || l2.equals(LABELS[LOCATION])) && 
           (l1.equals(LABELS[CATEGORY]) || l2.equals(LABELS[CATEGORY]))) return true;
        if((l1.equals(LABELS[INTCATE]) || l1.equals(LABELS[CATEGORY])) && 
           (l2.equals(LABELS[INTCATE]) || l2.equals(LABELS[CATEGORY]))) return true;
        return false;
    }
    
    public double distance2(Symbol symbol){
        if(!this.similar2(symbol)) return -1;
        double d = this.label.equalsIgnoreCase(symbol.label) ? 0 : 1;
        d += Math.abs(this.down - symbol.down);
        if(symbol.isDate()) return d;
        double c1 = Math.abs(this.n1 - symbol.n1), c2 = Math.abs(this.n2 - symbol.n2);
        return d + (c1<0.4 ? 0 : 1) + (c2<0.4 ? 0 : 1);
    }
    
    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof Symbol)) return false;
        Symbol symbol = (Symbol)obj;
        return this.label.equals(symbol.label) && this.down==symbol.down;
    }
    
    @Override
    public String toString(){
        return label + "," + down + "," + n1 + "," + n2;
    }
    
    public static Symbol parsing(String[] items){
        if(items.length<4) return null;
        try{
            return new Symbol(items[0], Integer.parseInt(items[1]), Double.parseDouble(items[2]), Double.parseDouble(items[3]));
        }catch(NumberFormatException e){
            return null;
        }
    }
    
    public static String[] harmonies(String sym){
        if(sym.equals(LABELS[DATE])) return new String[]{LABELS[DATE]};
        if(sym.equals(LABELS[LOCATION])) return new String[]{LABELS[LOCATION]};
        if(sym.equals(LABELS[CATEGORY])) return new String[]{LABELS[LOCATION], LABELS[LOCATION], LABELS[INTCATE], LABELS[DATE]};
        if(sym.equals(LABELS[NUMERIC])) return new String[]{LABELS[NUMERIC], LABELS[INT], LABELS[INTCATE]};
        if(sym.equals(LABELS[INT])) return new String[]{LABELS[NUMERIC], LABELS[INT], LABELS[INTCATE]};
        if(sym.equals(LABELS[INTCATE])) return new String[]{LABELS[NUMERIC], LABELS[INT], LABELS[INTCATE]};
        return new String[0];
    }
    
    public static final String[] LABELS = {
        "", "date", "loc", "txt", "cat", "num", "int", "intcat"
    };
    public static final int DATE = 1;
    public static final int LOCATION = 2;
    public static final int Text = 3;
    public static final int CATEGORY = 4;
    public static final int NUMERIC = 5;
    public static final int INT = 6;
    public static final int INTCATE = 7;
}
