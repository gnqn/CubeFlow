package stepper.model.sql;

public class SQLBinning extends SQLFunction{
    public SQLBinning(String name){
        super(SQLItem.Operator.BINNING, name);
    }
    
    @Override
    protected SQLFunction makingSymbol(){
        this.symbol = new Symbol(Symbol.CATEGORY);
        return this;
    }
    
    @Override
    public String func(){
        return "binning";
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
    protected boolean check(){
        return this.params.size()>3;
    }
    
    @Override
    public String makingSQL(String dimensions, String m1, String m2){
        String sql = "";
        int size = params.size();
        for(int i=1; i<size-1; i+=2) sql += "WHEN " + params.get(0) + "<" + params.get(i+1) + " THEN " + params.get(i) + " ";
        return "CASE " + sql + "ELSE " + params.get(size-1) + " END";
    }
    
    @Override
    public String makingHTML(String dimensions, String m1, String m2){
        return "<span>binning</span>(" + sequenceOfParams() + ")";
    }
}
