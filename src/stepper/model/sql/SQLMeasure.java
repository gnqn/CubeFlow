package stepper.model.sql;


public class SQLMeasure extends Attribute{
    protected int op;
    protected String m1;
    protected String m2;
    
    public SQLMeasure(String m1, int op, String m2, String name){
        this.m1 = m1;
        this.op = op;
        this.m2 = m2;
        this.name = name;
    }
    
    public SQLMeasure(String m1, int op, String m2, Attribute attr){
        this.m1 = m1;
        this.op = op;
        this.m2 = m2;
        this.typ = attr.typ;
        this.name = attr.name;
    }
    
    public int op(){
        return this.op;
    }
    
    public String m1(){
        return this.m1;
    }
    
    public String m2(){
        return this.m2;
    }
    
    @Override
    public String property(){
        return m1 + "," + SQLItem.Operator.toOperator(op, true) + "," + m2;
    }
    
    @Override
    public String makingSQL(String dimensions, String m1, String m2){
        return expression(dimensions, m1, m2, false);
    }
    
    @Override
    public String makingHTML(String dimensions, String m1, String m2){
        return expression(dimensions, m1, m2, true);
    }
    
    private String expression(String dimensions, String in1, String in2, boolean html){
        return m1.length()==0 && m2.length()==0 ? "" :
               m1.length()==0 ? (in2 + this.m2) :
               m2.length()==0 ? (in1 + this.m1) :
               (in1 + this.m1 + " " + SQLItem.Operator.toOperator(op, html) + " " +  in2 + this.m2);
    }
}
