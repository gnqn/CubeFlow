package stepper.model.sql;

public class SQLLog extends SQLFunction{
    public SQLLog(String name){
        super(SQLItem.Operator.LOG, name);
    }
    
    @Override
    protected SQLFunction makingSymbol(){
        this.symbol = new Symbol(Symbol.NUMERIC);
        return this;
    }
    
    @Override
    public String func(){
        return "Log";
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
        return "log(" + params.get(0) + ")";
    }
    
    @Override
    public String makingHTML(String dimensions, String m1, String m2){
        return "<span>log</span>(" + params.get(0) + ")";
    }
}
