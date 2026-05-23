package stepper.model.sql;

public class SQLLn extends SQLFunction{
    public SQLLn(String name){
        super(SQLItem.Operator.LN, name);
    }
    
    @Override
    protected SQLFunction makingSymbol(){
        this.symbol = new Symbol(Symbol.NUMERIC);
        return this;
    }
    
    @Override
    public String func(){
        return "Ln";
    }
    
    @Override
    public String property(){
        return func() + "(" + params.get(0) + ")";
    }
    
    @Override
    public String marks(){
        return func();
    }
    
    @Override
    public String makingSQL(String dimensions, String m1, String m2){
        return "ln(" + params.get(0) + ")";
    }
    
    @Override
    public String makingHTML(String dimensions, String m1, String m2){
        return "<span>ln</span>(" + sequenceOfParams() + ")";
    }
}
