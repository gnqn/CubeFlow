package stepper.model;

import java.util.*;
import stepper.model.sql.*;

public class QRoot extends QNode{
    private int rows;
    private final String table;
    private final ArrayList<Attribute> attrs = new ArrayList();
    
    public QRoot(int id, String name, String table){
        super(id, name);
        this.table = table;
    }
    
    public void addAttribute(String attr, int num){
        this.attrs.add(new Attribute(attr, num));
    }
    
    @Override
    public Attribute getAttribute(String name){
        if(name!=null) name = name.trim();
        for(Attribute attr: attrs) if(attr.name().equalsIgnoreCase(name)) return attr;
        return null;
    }
    
    @Override
    public DimensionSpace getDimensions(){
        ArrayList<Dimension> dims = new ArrayList();
        for(Attribute attr: attrs) dims.add(new Dimension(attr));
        return new DimensionSpace(dims);
    }

    @Override
    public String makingSQL(boolean nosort, boolean clickhouse){
        return table.endsWith(".csv") ? ("'D:/Program Files/duck/tables/" + table + "'") : table;
    }
    
    @Override
    public QRoot root(){
        return this;
    }
    
    public int rows(){
        return this.rows;
    }
    
    public void rows(int rows){
        this.rows = rows;
    }
    
    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof QRoot)) return false;
        return this.id==((QRoot)obj).id;
    }
}
