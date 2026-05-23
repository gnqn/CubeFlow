package stepper.model;

import java.util.*;
import stepper.model.sql.*;

public class QTra extends QNode{
    protected String limit;
    protected String order;
    protected SQLFunction trans;
    protected DimensionSpace partition = new DimensionSpace();
    
    public QTra(int id, String name){
        super(id, name);
    }
    
    @Override
    public SQLFunction dimsOperation(){
        return this.trans;
    }
    
    public void setTrans(SQLFunction f){
        this.trans = f;
    }
    
    public SQLFunction getTrans(){
        return this.trans;
    }
    
    public DimensionSpace getPartition(){
        return this.partition;
    }
    
    @Override
    public DimensionSpace getTransDimensions(){
        return this.trans==null ? null : this.trans.getDimensions();
    }
    
    @Override
    public String getCharacter(){
        return "T";
    }
    
    @Override
    public QNode copy(){
        QTra node = new QTra(id, name);
        node.trans = this.trans;
        node.measures = this.measures;
        node.condition = this.condition;
        node.dimensions = this.dimensions;
        node.limit = this.limit;
        node.order = this.order;
        return node;
    }
    
    @Override
    public QNode making(){
        return new QTra(id, name);
    }
    
    @Override
    protected String html_dimensions(){
        String sql = "";
        Attribute attr = trans==null ? null : trans.getAttribute();
        for(Dimension d: this.dimensions){
            if(sql.length()!=0) sql += ",";
            if(attr!=null && attr.name().equalsIgnoreCase(d.name())) sql += trans.makingHTML("", "", null) + " <span>AS</span> " + (d.as()==null ? d.name() : d.as());
            else sql += d.name() + (d.as()==null ? "" : " <span>AS</span> " + d.as());
        }
        for(Attribute auxi: this.auxis){
            if(sql.length()!=0) sql += ",";
            sql += auxi.property();
        }
        return sql;
    }
    
    @Override
    public String html_measures(String dimensions){
        String sql = "";
        for(int i=0; i<this.measures.size(); i++){
            sql += ",";
            Attribute attr = this.measures.get(i);
            if(!(attr instanceof SQLRank)){
                String[] m = measure_modies(attr);
                String ms = attr.makingHTML(dimensions, m[0], m[1]);
                if(ms.length()!=0) sql += ms + " <span>AS</span> m" + i;
            }else{
                sql += attr.makingHTML(dimensions, "", "") + " OVER(" + (this.partition.isEmpty() ? "" : (this.partition.sequence() + ",")) + "m0) <span>AS</span> "  + attr.name();
            }
        }
        return sql.length()==0 ? "" : sql.substring(1);
    }
    
    @Override
    public String html_orderby(){
        String orderby = "<span>ORDER BY</span> ";
        return order==null || order.length()==0 ? "" : (orderby + order);
    }
    
    @Override
    public String sql_from(boolean nosort, boolean clickhouse){
        return "FROM (" + input.makingSQL(nosort, clickhouse) + ") " + input.name;
    }
    
    @Override
    public String sql_dimensions(){
        String sql = "";
        Attribute attr = trans==null ? null : trans.getAttribute();
        for(Dimension d: this.dimensions){
            if(sql.length()!=0) sql += ",";
            if(attr!=null && attr.name().equalsIgnoreCase(d.name())) sql += trans.makingSQL("", "", "") + " AS " + (d.as()==null ? d.name() : d.as());
            else sql += d.name() + (d.as()==null ? "" : " AS " + d.as());
        }
        for(Attribute auxi: this.auxis){
            if(sql.length()!=0) sql += ",";
            sql += auxi.property();
        }
        return sql;
    }
    
    @Override
    public String sql_measures(String dimensions, boolean clickhouse){
        String sql = "";
        for(int i=0; i<this.measures.size(); i++){
            sql += ",";
            Attribute attr = this.measures.get(i);
            if(!(attr instanceof SQLRank)){
                String[] m = measure_modies(attr);
                String ms = attr.makingSQL(dimensions, m[0], m[1]);
                if(ms.length()!=0) sql += ms + " AS m" + i;
            }else{
                sql += attr.makingSQL(dimensions, "", "") + " OVER(" + (this.partition.isEmpty() ? "" : ("PARTITION BY " + this.partition.sequence() + " ")) + " ORDER BY m0) AS "  + attr.name();
            }
        }
        return sql.length()==0 ? "" : sql.substring(1);
    }
    
    @Override
    public String sql_limit(){
        String sql = "LIMIT ";
        return limit==null || limit.length()==0 ? "" : (sql + limit);
    }
    
    @Override
    public String sql_orderby(){
        String orderby = "ORDER BY ";
        return order==null || order.length()==0 ? "" : (orderby + order);
    }
    
    @Override
    public String sql_where(){
        return condition==null || condition.isEmpty() ? "" : ("WHERE " + condition.sequence());
    }
    
    @Override
    public String getProperties(){
        String alias = "", props = "", marks = "";
        for(Dimension dim: this.dimensions) if(dim.as()!=null) alias += " " + dim.name() + " AS " + dim.as();
        SQLFunction fun = this.trans!=null ? this.trans : null;
        if(fun==null) for(Attribute attr: measures){
            if(!(attr instanceof SQLFunction)) continue;
            fun = (SQLFunction)attr;
            props += (props.length()==0 ? "" : " ") + fun.property();
            if(marks.length()==0) marks = fun.marks();
        }else{
            props = fun.property();
            marks = fun.marks();
        }
        props += SQLItem.COMMON_SPLITOR + marks + (trans==null && marks.length()>0 && measures.size()>1 ? "..." : "");
        return partition.sequence() + SQLItem.COMMON_SPLITOR + props + 
               SQLItem.COMMON_SPLITOR + (order==null ? "" : order) + 
               SQLItem.COMMON_SPLITOR + (limit==null ? "" : limit) +
               SQLItem.COMMON_SPLITOR + (alias.length()==0 ? "" : alias.substring(1));
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
        
        makingTrans(props[0].length()==0 ? new String[0] : props[0].split(",",-1), props[3]);
        this.auxis = SQLFunction.parseAuxis(input, props[1]);
        
        for(String attr: props[2].split(",")){
            Dimension dim = this.dimensions.getDimension(attr);
            if(dim!=null) partition.add(dim);
        }
        
        this.setCondition(SQLItem.parseCondition(input, input2, props[4]));
        if(props[5].length()!=0) this.order = props[5];
        if(props[6].length()!=0) this.limit = props[6];
        if(props[7].length()!=0){
            String[] alias = props[7].split(" ");
            for(int i=0; (i+2)<alias.length; i+=3){
                Dimension dim = this.dimensions.getDimension(alias[i]);
                if(dim!=null) dim.as(alias[i+2]);
            }
        }
    }
    
    private void makingTrans(String[] dims, String props)throws Exception{
        //Attribute ms = this.input.measure;
        DimensionSpace ds = this.input.getDimensions();
        this.measures.clear();
        for(Attribute ms: this.input.measures) this.measures.add(new Attribute(ms));
        
        ArrayList<Dimension> list = new ArrayList();
        for(String attr: dims){
            Dimension dim = ds.getDimension(attr);
            if(dim==null) throw new Exception("Incorrect dimension " + attr);
            list.add(new Dimension(dim));
        }
        
        if(props.length()==0){
            this.dimensions = list.isEmpty() ? ds.makingSpace().renaming() : new DimensionSpace(list);
        }else{
            if(list.isEmpty()) for(Dimension d: ds) list.add(new Dimension(d));
            for(SQLFunction func: SQLFunction.parseTrans(this.input, props)){
                Attribute attr = func.getAttribute();
                if(attr!=null){
                    int idx = dimOf(list, attr.name());
                    if(idx!=-1){
                        trans = func;
                        list.remove(idx);
                        list.add(idx, new Dimension(func));
                    }else{
                        idx = msOf(measures, attr.name());
                        if(idx!=-1){
                            this.measures.remove(idx);
                            this.measures.add(idx, func);
                        }else{
                            this.measures.add(func);
                        }
                    }
                }else{
                    int idx = msOf(measures, "m0");
                    this.measures.remove(idx);
                    this.measures.add(idx, func);
                }
            }
            this.dimensions = new DimensionSpace(list);
        }
    }
    
    private static int msOf(ArrayList<Attribute> list, String name){
        for(int i=0; i<list.size(); i++) if(list.get(i).name().equals(name)) return i;
        return -1;
    }
    
    private static int dimOf(ArrayList<Dimension> list, String name){
        for(int i=0; i<list.size(); i++) if(list.get(i).name().equals(name)) return i;
        return -1;
    }
}
