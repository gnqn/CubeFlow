package stepper.model.sql;

public class SQLRAggregation extends SQLAggregation{
    public SQLRAggregation(int op, String name){
        super(op, name);
    }
    
    public SQLRAggregation(String name){
        super(SQLItem.Operator.RAVG, name);
    }
    
    @Override
    protected SQLFunction makingSymbol(){
        this.symbol = this.op==SQLItem.Operator.RMAX || this.op==SQLItem.Operator.RMIN ||
                      this.op==SQLItem.Operator.RSUM ? this.inputSymbol() : new Symbol(Symbol.NUMERIC);
        return this;
    }
    
    @Override
    public String func(){
        return op==SQLItem.Operator.RAVG ? "RAvg" :
               op==SQLItem.Operator.RSUM ? "RSum" :
               op==SQLItem.Operator.RMAX ? "RMax" :
               op==SQLItem.Operator.RMIN ? "RMin" : "";
    }
    
    private String _func(){
        return op==SQLItem.Operator.RAVG ? "Avg" :
               op==SQLItem.Operator.RSUM ? "Sum" :
               op==SQLItem.Operator.RMAX ? "Max" :
               op==SQLItem.Operator.RMIN ? "Min" : "";
    }
    
    @Override
    public String makingSQL(String dimensions, String m1, String m2){
        return _func() + "(" + params.get(0) + ") OVER(ORDER BY " + dimensions + " ROWS BETWEEN " + params.get(1) + " PRECEDING AND CURRENT ROW)";
    }
    
    @Override
    public String makingHTML(String dimensions, String m1, String m2){
        return "<span>" + func() + "</span>(" + this.sequenceOfParams() + ")"; 
    }
}
