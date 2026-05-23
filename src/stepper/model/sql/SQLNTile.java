package stepper.model.sql;

public class SQLNTile extends SQLFunction{
    public SQLNTile(String name){
        super(SQLItem.Operator.NTILE, name);
    }
    
    @Override
    protected SQLFunction makingSymbol(){
        this.symbol = new Symbol(Symbol.INTCATE);
        return this;
    }
    
    @Override
    public String func(){
        return "NTile";
    }
    
    @Override
    public String property(){
        return func() + "(" + this.sequenceOfParams() + ")";
    }
    
    @Override
    public String marks(){
        return this.property();
    }
    
    @Override
    public String makingSQL(String dimensions, String m1, String m2){
        return "NTile(" + params.get(0) + ") OVER(ORDER BY m0)";
    }
    
    @Override
    public String makingHTML(String dimensions, String m1, String m2){
        return "<span>NTile</span>(" + params.get(0) + ")";
    }
}
