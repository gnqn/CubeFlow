package stepper.model.sql;


public class SQLDateDiff extends SQLFunction{
    public SQLDateDiff(String name){
        super(SQLItem.Operator.DATEDIFF, name);
    }
    
    @Override
    protected SQLFunction makingSymbol(){
        this.symbol = new Symbol(Symbol.INT);
        return this;
    }
    
    @Override
    public String func(){
        return "date_diff";
    }
    
    @Override
    public String property(){
        return func() + "(" + this.sequenceOfParams() + ")";
    }
    
    @Override
    public String marks(){
        return func();
    }
    
}
