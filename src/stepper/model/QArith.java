package stepper.model;

import java.util.*;
import stepper.model.sql.*;

public class QArith extends QNode{
    protected int op;
    protected QNode input2;
    protected ArrayList<Predicate> joins = new ArrayList();
    
    public QArith(int id, String name){
        super(id, name);
    }
    
    public QArith(int id, String name, int op){
        super(id, name);
        this.op = op;
    }
    
    public int op(){
        return this.op;
    }
    
    @Override
    public QNode input2(){
        return input2;
    }
    
    @Override
    public void input2(QNode node){
        this.input2 = node;
    }
    
    @Override
    public String getCharacter(){
        return SQLItem.Operator.toOperator(op, false);
    }
    
    @Override
    public QNode copy(){
        QArith copy = new QArith(id, name);
        copy.op = this.op;
        copy.joins = this.joins;
        copy.dimensions = this.dimensions;
        copy.measures = this.measures;
        return copy;
    }
    
    @Override
    public QNode making(){
        return new QArith(id, name);
    }
    
    @Override
    public void taking(DimensionSpace ds, QNode node){
        this.dimensions = ds;
        this.measure = node.measure;
        if(node instanceof QArith){
            this.op = ((QArith)node).op;
            this.joins = ((QArith)node).joins;
        }
    }
    
    public ArrayList<Predicate> getJoins(){
        return this.joins;
    }
    
    @Override
    public ArrayList<Predicate> getNEQJoins(){
        ArrayList<Predicate> list = new ArrayList();
        for(Predicate join: joins) if(!join.isEqualityPredicate()) list.add(join);
        return list;
    }
    
    @Override
    public void joining(){
        if(this.input.dimensions.isEmpty() || this.input2.dimensions.isEmpty()){
            this.joins = new ArrayList();
            super.joining();
            return;
        }
        
        ArrayList<Predicate> list = new ArrayList();
        joins.stream().forEach((join) -> {
            String ln = join.getAttribute().name(), rn = ((Attribute)join.getParameter()).name();
            if(this.dimensions.getDimension(ln)!=null || this.dimensions.getDimension(rn)!=null) list.add(join);
        });
        this.joins = list;
        super.joining();
    }
    
    @Override
    public DimensionSpaces pairingDimensions(DimensionSpaces spaces){
        for(Predicate join: joins){
            if(!(join.getAttribute() instanceof Dimension) || !(join.getParameter() instanceof Dimension)) continue;
            Dimension dim1 = (Dimension)join.getAttribute(),
                      dim2 = (Dimension)join.getParameter();
            if(dim1.name().equalsIgnoreCase(dim2.name())) continue;
            
            String name1 = this.getDimensions().getDimension(dim1.name())==null ? dim2.name() : dim1.name(),
                   name2 = name1.equals(dim2.name()) ? dim1.name() : dim2.name();
            if(this.input.getDimensions().getDimension(name1)==null) this.input.getDimensions().getDimension(name2).pair(name1);
            if(this.input2.getDimensions().getDimension(name1)==null) this.input2.getDimensions().getDimension(name2).pair(name1);
            
            DimensionSpace space = spaces.getSpace(dim1.name());
            if(space!=null){
                space.add(dim2);
            }else{
                space = spaces.getSpace(dim2.name());
                if(space!=null){
                    space.add(dim1);
                }else{
                    space = new DimensionSpace(dim1);
                    space.add(dim2);
                    spaces.add(space);
                }
            }
        }
        return super.pairingDimensions(spaces);
    }
    
    @Override
    public String html_dimensions(){
        String sql = "";
        for(Dimension d: this.dimensions){
            sql += "," + (d.as()==null && this.input.dimensions.getDimension(d.name())!=null ? this.input.name : this.input2.name) + "." + d.name();
            if(d.as()!=null) sql += " <span>AS</span> " + d.as();
        }
        return sql.length()==0 ? "" : sql.substring(1);
    }
    
    @Override
    protected String html_where(){
        if(joins.isEmpty()) return "";
        
        String sql = "<span>WHERE</span> ";
        for(int i=0; i<joins.size(); i++){
            if(i!=0) sql += " <span>AND</span> ";
            sql += joins.get(i).sequence(this.input.name, this.input2.name, true);
        }
        return sql;
    }
    
    @Override
    public String html_orderby(){
        DimensionSpace ds = this.getDimensions();
        if(ds==null || ds.isEmpty()) return "";
        
        String sql = "";
        for(Dimension d: this.dimensions) sql += "," + (d.as()==null && this.input.dimensions.getDimension(d.name())!=null ? this.input.name : this.input2.name) + "." + d.name();
        return "<span>ORDER BY</span> " + sql.substring(1);
    }
    
    @Override
    public String sql_dimensions(){
        String sql = "";
        for(Dimension d: this.dimensions){
            sql += "," + (d.as()==null && this.input.dimensions.getDimension(d.name())!=null ? this.input.name : this.input2.name) + "." + d.name();
            if(d.as()!=null) sql += " AS " + d.as();
        }
        return sql.length()==0 ? "" : sql.substring(1);
    }
    
    @Override
    public String sql_from(boolean nosort, boolean clickhouse){
        return "FROM (" + input.makingSQL(nosort, clickhouse) + ") " + input.name + 
               ", (" + input2.makingSQL(nosort, clickhouse) + ") " + input2.name;
    }
    
    @Override
    public String sql_where(){
        if(joins.isEmpty()) return "";
        
        String sql = "WHERE ";
        for(int i=0; i<joins.size(); i++){
            if(i!=0) sql += " AND ";
            sql += joins.get(i).sequence(this.input.name, this.input2.name, false);
        }
        return sql;
    }
    
    @Override
    public String sql_orderby(){
        DimensionSpace ds = this.getDimensions();
        if(ds==null || ds.isEmpty()) return "";
        
        String sql = "";
        for(Dimension d: this.dimensions) sql += "," + (d.as()==null && this.input.dimensions.getDimension(d.name())!=null ? this.input.name : this.input2.name) + "." + d.name();
        return "ORDER BY " + sql.substring(1);
    }
    
    @Override
    public String getProperties(){
        String props1 = "", props2 = "", props3 = "";
        for(Attribute attr: measures) props1 += "," + attr.property();
        for(Predicate p: joins) props2 += "," + p.getAttribute().name() + "," + p.getOperator() + "," + p.getParameter();
        if(props1.length()!=0) props1 =props1.substring(1);
        if(props2.length()!=0) props2 =props2.substring(1);
        if(!measures.isEmpty()) props3 = SQLItem.Operator.toOperator(((SQLMeasure)measures.get(0)).op(), true);
        if(measures.size()>1) props3 += "...";
        return props1 + SQLItem.COMMON_SPLITOR + props2 + SQLItem.COMMON_SPLITOR + props3;
    }
    
    @Override
    public void setProperties(boolean insert, QNode input, QNode input2, String[] props)throws Exception{
        if(insert){
            this.input = input;
            this.input2 = input2;
            this.input.insert(this);
            this.input2.insert(this);
        }else if(this.input!=input){
            this.input.children.remove(this);
            this.input2.children.remove(this);
            this.input = input;
            this.input2 = input2;
            this.input.insert(this);
            this.input2.insert(this);
        }
        
        this.measures.clear();
        String[] items = props[0].split(",", -1);
        for(int i=0; items.length%3==0 && i<items.length; i+=3){
            int op = items[i+1].length()==0 ? 0 : Integer.parseInt(items[i+1]);
            this.measures.add(new SQLMeasure(items[i], op, items[i+2], "m" + i/3));
        }
        
        items = props[1].length()==0 ? new String[0] : props[1].split(",",-1);
        DimensionSpace diffs = new DimensionSpace();
        DimensionSpace dims = input.getDimensions().makingSpace().renaming();
        DimensionSpace dims2 = input2.getDimensions().makingSpace().renaming();
        for(int i=0; i<items.length; i+=3){
            Dimension d1 = dims.getDimension(items[i]);
            Dimension d2 = dims2.getDimension(items[i+2]);
            joins.add(new Predicate(d1, SQLItem.Comparator.parse(items[i+1]), d2));
            if(joins.get(joins.size()-1).isEqualityPredicate()) diffs.add(d2);
        }
        this.dimensions = dims;
        add(dims, dims2, diffs);
        ArrayList<String> visits = new ArrayList();
        for(Dimension d: this.dimensions){
            if(visits.contains(d.name())) d.as(d.name() + "2");
            visits.add(d.name());
        }
        /*
        if(this.input.measure instanceof SQLNone && this.input2.measure instanceof SQLNone){
            this.op = SQLItem.NONE;
            this.measure = new SQLNone();
        }else if(this.input.measure instanceof SQLNone){
            this.op = SQLItem.NONE;
            this.measure = new Attribute(this.input2.measure);
        }else if(this.input2.measure instanceof SQLNone){
            this.op = SQLItem.NONE;
            this.measure = new Attribute(this.input.measure);
        }else{
            ArrayList params = new ArrayList();
            params.add(this.input.measure);
            params.add(this.input2.measure);
            this.measure = SQLFunction.making(op, "m0", params);
        }*/
    }
    
    @Override
    String well_dimensions(HashMap<String, String> views){
        String sql = "";
        String modi = views.get(this.input.name), modi2 = views.get(this.input2.name);
        if(modi==null) modi = this.input.name;
        if(modi2==null) modi2 = this.input2.name;
        for(Dimension d: this.dimensions){
            sql += "," + (d.as()==null && this.input.dimensions.getDimension(d.name())!=null ? modi : modi2) + "." + d.name();
            if(d.as()!=null) sql += " AS " + d.as();
        }
        return sql.length()==0 ? "" : sql.substring(1);
    }
    
    @Override
    String well_where(HashMap<String, String> views){
        if(joins.isEmpty()) return "";
        
        String m1 = views.get(this.input.name), m2 = views.get(this.input2.name);
        if(m1==null) m1 = this.input.name;
        if(m2==null) m2 = this.input2.name;
        
        String sql = "WHERE ";
        for(int i=0; i<joins.size(); i++){
            if(i!=0) sql += " AND ";
            sql += joins.get(i).sequence(m1, m2, false);
        }
        return sql;
    }
    
    @Override
    String well_orderby(HashMap<String, String> views){
        DimensionSpace ds = this.getDimensions();
        if(ds==null || ds.isEmpty()) return "";
        
        String m1 = views.get(this.input.name), m2 = views.get(this.input2.name);
        if(m1==null) m1 = this.input.name;
        if(m2==null) m2 = this.input2.name;
        
        String sql = "";
        for(Dimension d: this.dimensions) sql += "," + (d.as()==null && this.input.dimensions.getDimension(d.name())!=null ? m1 : m2) + "." + d.name();
        return "ORDER BY " + sql.substring(1);
    }
    
    @Override
    boolean isWith(){
        return !this.children.isEmpty();
    }
    
    private void add(DimensionSpace dims, DimensionSpace dims2, DimensionSpace diffs){
        dims2.subtractDimensions(diffs).stream().forEach((d) -> {
            int i = 0, j = dims2.indexOf(d);
            for(Dimension dd: diffs){i = dims.indexOf(dd); if(i>j) break;}
            if(i>j) dims.add(i-1, d);
            else dims.add(d);
        });
    }
}
