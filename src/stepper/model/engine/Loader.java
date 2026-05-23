package stepper.model.engine;


import java.util.*;
import stepper.util.*;
import stepper.model.*;
import stepper.model.sql.*;

public class Loader extends Action{
    protected Loader share;
    protected Hyb2IntMap[] maps;
    private Condition cond;
    private final QRoot root;
    private final ArrayList<Attribute> attrs = new ArrayList();
    
    public Loader(QNode node, Loader share){
        super(node.input().name(), node.getDimensions());
        this.share = share;
        this.root = (QRoot)node.input();
        this.cond = node.getCondition();
        if(this.cond!=null && this.cond.isEmpty()) this.cond = null;
        if(share!=null) share.maps = new Hyb2IntMap[share.schema.size()];
    }
    
    public QRoot getRoot(){
        return this.root;
    }
    
    public Condition getFilter(){
        return this.cond;
    }
    
    public ArrayList<Attribute> getAttributes(){
        return this.attrs;
    }
    
    public int addAttribute(Attribute attr){
        if(attr==null || attr instanceof SQLNone) return -1;
        int k = this.attrs.indexOf(attr);
        if(k==-1) attrs.add(attr);
        return k==-1 ? this.attrs.size()-1 : k;
    }
    
    @Override
    public boolean execute(boolean nosort){
        if(share!=null && share.cube==null && !share.execute(nosort)) return false;
        this.cube = new RTLoader(root, this).load();
        return true;
    }
    
    public int[] shareDims(){
        int[] dims = new int[this.schema.size()];
        for(int i=0; i<this.schema.size(); i++){
            Dimension dim = this.schema.get(i);
            dims[i] = this.share==null ? -1 : share.schema.indexOf(dim);
        }
        return dims;
    }
    
    public boolean isWindow(){
        return !attrs.isEmpty() && isWindow(attrs.get(0));
    }
    
    public boolean isAggregation(){
        return attrs.isEmpty() || isAggregation(attrs.get(0));
    }
    
    private boolean isWindow(Attribute attr){
        return attr instanceof SQLNTile || attr instanceof SQLRAggregation;
    }
    
    private boolean isAggregation(Attribute attr){
        return attr instanceof SQLAggregation;
    }
}
