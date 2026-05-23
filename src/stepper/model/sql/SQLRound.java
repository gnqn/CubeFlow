package stepper.model.sql;

public class SQLRound extends SQLFunction{
    public SQLRound(String name){
        super(SQLItem.Operator.ROUND, name);
    }
    
    @Override
    protected SQLFunction makingSymbol(){
        this.symbol = new Symbol(Symbol.INT);
        return this;
    }
    
    @Override
    public String func(){
        return "Round";
    }
    
    @Override
    public String property(){
        return func() + "(" + this.sequenceOfParams() + ")";
    }
    
    @Override
    public String marks(){
        return func();
    }
    
    @Override
    public String makingSQL(String dimensions, String m1, String m2){
        return "Round(" + this.sequenceOfParams() + ")"; 
    }
    
    @Override
    public String makingHTML(String dimensions, String m1, String m2){
        return "<span>Round</span>(" + this.sequenceOfParams() + ")"; 
    }
}
