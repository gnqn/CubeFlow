package stepper.model.sql;


public class SQLQuantile2 extends SQLQuantile{
    public SQLQuantile2(String name){
        super(SQLItem.Operator.QUANTILE2, name);
        p = 0.5;
    }
    
    @Override
    public String func(){
        return "Quantile2";
    }
    
}
