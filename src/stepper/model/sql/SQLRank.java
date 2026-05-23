package stepper.model.sql;

import java.util.*;

public class SQLRank extends SQLFunction{
    public SQLRank(String name){
        super(SQLItem.Operator.RANK, name);
    }
    
    @Override
    protected SQLFunction makingSymbol(){
        this.symbol = new Symbol(Symbol.INTCATE);
        return this;
    }
    
    @Override
    public String func(){
        return "Rank";
    }
    
    @Override
    public String property(){
        return func() + "()";
    }
    
    @Override
    public String marks(){
        return func();
    }
    
    @Override
    public String makingSQL(String dimensions, String m1, String m2){
        return "Rank()"; 
    }
    
    @Override
    public String makingHTML(String dimensions, String m1, String m2){
        return "<span>Rank</span>()"; 
    }
}
