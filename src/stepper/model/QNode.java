package stepper.model;

import java.util.*;
import stepper.model.sql.*;

public abstract class QNode {
    protected int id;
    protected QNode input;
    protected String name;
    protected Condition condition;
    protected Attribute measure;
    protected DimensionSpace dimensions;
    protected ArrayList<Attribute> auxis = new ArrayList();
    protected ArrayList<QNode> children = new ArrayList();
    protected ArrayList<Attribute> measures = new ArrayList();
    
    public QNode(int id, String name){
        this.id = id;
        this.name = name;
    }
    
    public int id(){
        return id;
    }
    
    public void id(int id){
        this.id = id;
    }
    
    public String name(){
        return this.name;
    }
    
    public QNode input(){
        return input;
    }
    
    public void input(QNode node){
        input = node;
    }
    
    public QNode input2(){
        return null;
    }
    
    public void input2(QNode node){}
    
    public Attribute measure(){
        return this.measure;
    }
    
    public ArrayList<Attribute> measures(){
        return this.measures;
    }
    
    public void setMeasure(Attribute measure){
        this.measure = measure;
    }
    
    public void nodes(ArrayList<QNode> nodes){
        if(!nodes.contains(this)) nodes.add(this);
        for(QNode child: children) ((QNode)child).nodes(nodes);
    }
    
    public QNode copy(){return null;}
    public String getCharacter(){return "";}
    
    public QNode making(){return null;}
    public void taking(DimensionSpace ds, QNode node){}
    
    public void setCondition(Condition cond){
        this.condition = cond;
        DimensionSpace ds = this.getDimensions();
        if(ds!=null) this.condition.getEQDimensions().stream().forEach((d) -> {ds.remove(d);});
    }
    
    public Condition getCondition(){
        return this.condition;
    }
    
    public ArrayList<Attribute> getAuxis(){
        return this.auxis;
    }
    
    public DimensionSpace getDimensions(){
        return this.dimensions;
    }
    
    public DimensionSpace getConditionDimensions(){
        return this.condition==null ? null : this.condition.getEQDimensions();
    }
    
    public DimensionSpace getTransDimensions(){
        return null;
    }
    
    public SQLFunction dimsOperation(){return null;}
    
    public void setDimensions(DimensionSpace ds){
        this.dimensions = ds;
    }
    
    public Attribute getAttribute(String name){
        if(name!=null) name = name.trim();
        for(Attribute attr: measures) if(attr.name().equalsIgnoreCase(name)) return attr;
        Attribute dim = dimensions==null ? null : dimensions.getDimension(name);
        if(dim!=null) return dim;
        for(Attribute attr: auxis) if(attr.property().equals(name) || attr.name().equalsIgnoreCase(name)) return attr;
        return null;
    }
    
    public ArrayList<Attribute> getAttributes(String[] names){
        ArrayList<Attribute> attrs = new ArrayList();
        for(String n: names) attrs.add(getAttribute(n));
        return attrs;
    }
    
    public QRoot root(){
        return input==null ? null : input.root();
    }
    
    public void getEnds(ArrayList<QNode> ends){
        if(!(this instanceof QRoot) && this.children.isEmpty() && !ends.contains(this)) ends.add(this);
        children.stream().forEach((child) -> {child.getEnds(ends);});
    }
    
    public ArrayList<Predicate> getNEQJoins(){
        return new ArrayList();
    }
    
    public void joining(){
        if(this.input!=null) this.input.joining();
        if(this.input2()!=null) this.input2().joining();
    }
    
    public DimensionSpaces pairingDimensions(DimensionSpaces spaces){
        if(this.input()!=null) this.input().pairingDimensions(spaces);
        if(this.input2()!=null) this.input2().pairingDimensions(spaces);
        return spaces;
    }
    
    public void insert(QNode child){
        children.add(child);
    }
    
    public void insert(QNode child, QNode temp){
        if(!this.isNext()) return;            //important!
        if(this.children().contains(child)){
            if(temp.input()!=temp.input2()) return;
            int num = 0;
            for(QNode n: this.children()) if(n==child) num++;
            if(num>1) return;
        }
        this.insert(child);
    }
    
    public ArrayList<QNode> children(){
        return new ArrayList(this.children);
    }
    
    public QNode get(int id){
        if(this.id==id) return this;
        for(QNode child: children){
            QNode node = child.get(id);
            if(node!=null) return node;
        }
        return null;
    }
    
    public boolean isNext(){
        return false;
    }
    
    public int type(){
        return SQLItem.Cell.getType(this);
    }
    
    public boolean has_measure(){
        return this.measure!=null && !(this.measure instanceof SQLNone);
    }
    
    public boolean isInputNode(boolean nullinput){
        return (!nullinput && this.input==null) || this.input instanceof QRoot;
    }
    
    public String html(){
        String sql_attrs = html_dimensions(), sql_ms = html_measures(sql_attrs);
        if(sql_ms.length()!=0) sql_attrs += (sql_attrs.length()!=0 ? "," : "") + sql_ms;
        String sql = "<span>SELECT</span> " + sql_attrs + " " + html_from() + " " + html_where() + 
                     " " + sql_groupby() + " " + html_orderby() + " " + sql_limit();
        return sql;
    }
    
    protected String html_dimensions(){
        return this.sql_dimensions();
    }
    
    public String html_measures(String dimensions){
        String sql = "";
        for(int i=0; i<this.measures.size(); i++){
            if(sql.length()!=0) sql += ",";
            String[] m = measure_modies(this.measures.get(i));
            String ms = this.measures.get(i).makingHTML(dimensions, m[0], m[1]);
            if(ms.length()!=0) sql += ms + " <span>AS</span> m" + i;
        }
        return sql;
    }
    
    protected String html_from(){
        return "<span>FROM</span> " + input.name + (input2()==null ? "" : (", " + input2().name));
    }
    
    protected String html_where(){
        return this.sql_where();
    }
    
    public String html_orderby(){
        DimensionSpace ds = this.getDimensions();
        if(this.measure instanceof SQLAggregation && ((SQLAggregation)this.measure).op()==SQLItem.Operator.RAW) return "";
        return (ds==null || ds.isEmpty()) ? "" : ("<span>ORDER BY</span> " + html_dimensions());
    }
    
    public String makingSQL(){
        return makingSQL(false, false);
    }
    
    public String makingSQL(boolean nosort, boolean clickhouse){
        return makingSQL(true, nosort, clickhouse);
    }
    
    public String makingSQL(boolean deep, boolean nosort, boolean clickhouse){
        String sql_attrs = sql_dimensions(), sql_ms = sql_measures(sql_attrs, clickhouse);
        if(sql_ms.length()!=0) sql_attrs += (sql_attrs.length()!=0 ? "," : "") + sql_ms;
        String sql = "SELECT " + sql_attrs + " ";
        if(deep) sql += sql_from(nosort, clickhouse) + " ";
        else if(input instanceof QRoot) sql += "FROM " + input.makingSQL(nosort, clickhouse); 
        else sql += "FROM " + input.name + (input2()==null ? "" : (", " + input2().name)) + " ";
        sql += sql_where() + " " + sql_groupby() + " ";
        if(this.input instanceof QRoot || !nosort) sql += sql_orderby() + " ";   
        return sql + sql_limit();
    }
    
    public String sql_from(boolean nosort, boolean clickhouse){return "";}
    public String sql_where(){return "";}
    public String sql_groupby(){return "";}
    public String sql_limit(){return "";}
    
    public String sql_orderby(){
        DimensionSpace ds = this.getDimensions();
        if(this.measure instanceof SQLAggregation && ((SQLAggregation)this.measure).op()==SQLItem.Operator.RAW) return "";
        return (ds==null || ds.isEmpty()) ? "" : ("ORDER BY " + sql_dimensions());
    }
    
    public String sql_dimensions(){
        DimensionSpace ds = this.getDimensions();
        return ds==null ? "" : ds.sequence();
    }
    
    public String sql_measures(String dimensions, boolean clickhouse){
        String sql = "";
        for(int i=0; i<this.measures.size(); i++){
            if(sql.length()!=0) sql += ",";
            String[] m = measure_modies(this.measures.get(i));
            String ms = this.measures.get(i).makingSQL(dimensions, m[0], m[1], clickhouse);
            if(ms.length()!=0) sql += ms + " AS m" + i;
        }
        return sql;
    }
    
    protected String[] measure_modies(Attribute ms){
        boolean modify = input!=null && input2()!=null && ms instanceof SQLMeasure;
        return modify ? new String[]{input.name + ".", input2().name + "."} : new String[]{"", ""};
    }
    
    public String makingWellSQL(){
        HashMap<String, String> views = new HashMap();
        StringBuilder with = new StringBuilder("WITH ");
        
        String sql = makingWellSQL(false, with, views);
        return views.isEmpty() ? sql : (with + "\n" + sql);
    }
    
    public String makingWellSQL(boolean v, StringBuilder with, HashMap<String, String> views){
        if(v && views.containsKey(this.name)) return "";
        
        String from = this.well_from(with, views);
        String sql_attrs = this.well_dimensions(views), sql_ms = sql_measures(sql_attrs, false);
        if(sql_ms.length()!=0) sql_attrs += (sql_attrs.length()!=0 ? "," : "") + sql_ms;
        String sql = "SELECT " + sql_attrs + " " + from + " " + well_where(views) + 
                     " " + sql_groupby() + " " + well_orderby(views) + " " + sql_limit();
        if(v){
            String view = this.name;
            if(!views.isEmpty()) with.append(",\n");
            with.append(view).append(" AS (").append(sql).append(")");
            views.put(this.name, view);
        }
        return sql;
    }
    
    String well_dimensions(HashMap<String, String> views){
        return this.sql_dimensions();
    }
    
    String well_from(StringBuilder with, HashMap<String, String> views){
        String from = "FROM ";
        if(this.input instanceof QRoot) return from + this.input.makingSQL();
        
        this.input.makingWellSQL(true, with, views);
        from += views.get(this.input.name);
        if(this.input2()==null) return from;
        
        this.input2().makingWellSQL(true, with, views);
        from += "," + views.get(this.input2().name);
        return from;
    }
    
    String well_where(HashMap<String, String> views){
        return this.sql_where();
    }
    
    String well_orderby(HashMap<String, String> views){
        return this.sql_orderby();
    }
    
    boolean isWith(){
        return this.children.size()>1;
    }
    
    public String makingProperties(){
        return "";
    }
    
    public String getProperties(){
        return "";
    }
    
    public void setProperties(boolean insert, QNode input, QNode input2, String[] props)throws Exception{}
    
    @Override
    public String toString(){
        return this.name;
    }
    
    public ArrayList<QNode> getNodes(ArrayList<QNode> nodes){
        if(this.input!=null && !(this.input instanceof QRoot)) this.input.getNodes(nodes);
        if(this.input2()!=null && !(this.input2() instanceof QRoot)) this.input2().getNodes(nodes);
        if(!nodes.contains(this)) nodes.add(this);
        return nodes;
    }
}
