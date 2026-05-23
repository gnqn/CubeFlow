package stepper.model;

import stepper.model.sql.*;

public class QAgg extends QNode{
    public QAgg(int id, String name){
        super(id, name);
    }
    
    @Override
    public String getCharacter(){
        return "A";
    }
    
    @Override
    public QNode copy(){
        QAgg node = new QAgg(id, name);
        node.measures = this.measures;
        node.condition = this.condition;
        node.dimensions = this.dimensions;
        return node;
    }
    
    @Override
    public QNode making(){
        return new QAgg(id, name);
    }
    
    @Override
    public void taking(DimensionSpace ds, QNode node){
        this.dimensions = ds;
        this.measure = node.measure;
        this.condition = node.condition==null ? null : node.condition.making(ds);
    }
    
    @Override
    protected String html_where(){
        Condition cond = this.getCondition();
        return cond==null || cond.isEmpty() ? "" : ("<span>WHERE</span> " + cond.html());
    }
    
    @Override
    public String sql_from(boolean nosort, boolean clickhouse){
        if(input instanceof QRoot) return "FROM " + input.makingSQL(nosort, clickhouse);
        return "FROM (" + input.makingSQL(nosort, clickhouse) + ") " + input.name;
    }
    
    @Override
    public String sql_where(){
        Condition cond = this.getCondition();
        return cond==null || cond.isEmpty() ? "" : ("WHERE " + cond.sequence());
    }
    
    @Override
    public String sql_groupby(){
        DimensionSpace ds = this.getDimensions();
        if(ds==null || ds.isEmpty()) return "";
        for(Attribute attr: this.measures) if(attr instanceof SQLRAggregation) return "";
        return "GROUP BY " + ds.sequence();
    }
    
    @Override
    public String getProperties(){
        String props1 = "", props2 = "";
        for(Attribute ms: this.measures){
            props1 += SQLItem.SUB_SPLITOR + ms.property();
            if(props2.length()==0 && ms instanceof SQLFunction) props2 = ((SQLFunction)ms).marks();
        }
        if(props1.length()!=0) props1 = props1.substring(1);
        if(this.measures.size()>1 && props2.length()!=0) props2 += "...";
        return props1 + SQLItem.COMMON_SPLITOR + props2;
    }
    
    @Override
    public void setProperties(boolean insert, QNode input, QNode input2, String[] props)throws Exception{
        if(insert){
            this.input = input;
            this.input.insert(this);
        }else if(this.input!=input){
            this.input.children.remove(this);
            this.input = input;
            this.input.insert(this);
        }
        if(props[0].length()!=0) makingDimensions(props[0].split(","));
        else this.dimensions = new DimensionSpace();
        
        int i = 0;
        this.measures.clear();
        for(String item: props[1].split(SQLItem.SUB_SPLITOR)){
            SQLFunction func = SQLFunction.parseProps(this.input, item, "m"+i++);
            if(!(func instanceof SQLNone)) this.measures.add(func);
        }
        this.setCondition(SQLItem.parseCondition(input, input2, props[2]));
    }
    
    private void makingDimensions(String[] attrs){
        this.dimensions = DimensionSpace.makingSpace(this.input.getAttributes(attrs)).renaming();
    }
    
    @Override
    boolean isWith(){
        return !this.children.isEmpty();
    }
}
