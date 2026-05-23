package stepper.model.sql;


public class SQLQuantile3 extends SQLQuantile{
    public SQLQuantile3(String name){
        super(SQLItem.Operator.QUANTILE3, name);
        p = 0.75;
    }
    
    @Override
    public String func(){
        return "Quantile3";
    }
    
}
