package stepper.model.sql;

public class SQLExtract extends SQLFunction{
    public SQLExtract(String name){
        super(SQLItem.Operator.EXTRACT, name);
    }
    
    @Override
    protected SQLFunction makingSymbol(){
        this.symbol = new Symbol(Symbol.DATE);
        return this;
    }
    
    @Override
    public String func(){
        return "extract";
    }
    
    @Override
    public String property(){
        return func() + "(" + this.params.get(0) + " FROM " + this.params.get(1) + ") AS " + this.name;
    }
    
    @Override
    public String marks(){
        return func();
    }
}
