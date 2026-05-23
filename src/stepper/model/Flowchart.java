package stepper.model;

import java.util.*;
import stepper.model.sql.*;

public class Flowchart {
    private int id;
    private final ArrayList<QRoot> roots;
    private final ArrayList<QNode> nodes = new ArrayList();
    
    public Flowchart(ArrayList<QRoot> roots){
        this.roots = roots;
    }
    
    public Flowchart(int id, ArrayList<QRoot> roots){
        this.id = id;
        this.roots = roots;
    }
    
    public QNode getNode(String name){
        for(QNode node: nodes) if(node.name.equalsIgnoreCase(name)) return node;
        return null;
    }
    
    public ArrayList<QNode> nodes(){
        return this.nodes;
    }
    
    public DimensionSpace getDimensions(){
        DimensionSpace space = new DimensionSpace();
        for(QRoot root: roots) space.merge(root.getDimensions());
        return space;
    }
    
    public ArrayList<Attribute> getAttributes(ArrayList<Dimension> dims){
        ArrayList<Attribute> attrs = new ArrayList();
        for(Dimension dim: dims){
            for(QRoot root: roots){
                Attribute attr = root.getAttribute(dim.name());
                if(attr!=null && !attrs.contains(attr)){attrs.add(attr); break;}
            }
        }
        return attrs;
    }
    
    public ArrayList<QNode> getEnds(){
        ArrayList<QNode> ends = new ArrayList();
        for(QRoot root: roots) root.getEnds(ends);
        return ends;
    }
    
    public QNode getEnd(String name){
        for(QNode end: getEnds()) if(end.name().equals(name)) return end;
        return null;
    }
    
    public QNode get(int id){
        for(QRoot root: roots) if(root.id==id) return root;
        for(QNode n: nodes) if(n.id()==id) return n;
        return null;
    }
    
    public ArrayList<QRoot> roots(){
        return this.roots;
    }
    
    public QRoot getRoot(int id){
        for(QRoot root: roots) if(root.id()==id) return root;
        return null;
    }
    
    public QRoot getRoot(DimensionSpace dims){
        for(QRoot root: roots) if(root.getDimensions().has(dims)) return root;
        return null;
    }
    
    public QNode addNode(QNode node){
        if(node!=null && get(node.id())==null) nodes.add(node);
        return node;
    }
    
    public int id(){
        return this.id;
    }
}
