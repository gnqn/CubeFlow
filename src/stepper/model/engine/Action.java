package stepper.model.engine;

import java.util.*;
import stepper.model.sql.*;

public class Action {
    public Cube cube;
    public Pool pool;
    protected String name;
    protected Action input;
    protected DimensionSpace schema;
    protected ArrayList<Attribute> measures;
    
    protected int[] cdxes;
    protected boolean[] ords;
    protected boolean kord = true;
    
    protected Action(){}
    
    protected Action(String name, DimensionSpace schema){
        this(name, schema, null, null);
    }
    
    public Action(String name, DimensionSpace schema, ArrayList<Attribute> measures, Action input){
        this.name = name;
        this.schema = schema;
        this.input = input;
        this.measures = measures;
        this.ords = new boolean[this.schema.size()];
    }
    
    public Action input(){
        return this.input;
    }
    
    public Action input2(){
        return null;
    }
    
    public DimensionSpace getSchema(){
        return this.schema;
    }
    
    public int[] cols(){
        if(cdxes==null) return null;
        int[] cols = new int[schema.size()];
        for(int i=0, k=0; i<cdxes.length; i++) if(cdxes[i]!=-1) cols[k++] = i;
        return cols;
    }
    
    public int[] frees(){
        if(cdxes==null) return null;
        int[] frees = new int[cdxes.length - schema.size()];
        for(int i=0, k=0; i<cdxes.length; i++) if(cdxes[i]==-1) frees[k++] = i;
        return frees;
    }
    
    public boolean execute(boolean nosort){
        return false;
    }
    
    public Condition condition(){
        return null;
    }
    
    public Condition[] msCondition(){
        Condition cd = this.condition();
        if(cd==null) return null;
        
        Condition[] cs = new Condition[this.input.measures.size()];
        for(int i=0; i<this.input.measures.size(); i++){
            Attribute ms = this.input.measures.get(i);
            cs[i] = cd.getAnds(ms.name());
            if(cs[i]!=null && cs[i].isEmpty()) cs[i] = null;
        }
        boolean valid = false;
        for(Condition c: cs) if(c!=null) valid = true;
        return valid ? cs : null;
    }
    
    public boolean isBlocking(){
        return true;
    }
    
    public boolean isotonic(){
        return true;
    }
    
    public boolean composable(){
        return false;
    }
    
    public boolean ordered(){
        if(!this.kord) return false;
        if(this.input!=null && !this.input.ordered()) return false;
        return !(this.input2()!=null && !this.input2().ordered());
    }
    
    public boolean ordered(ArrayList<Dimension> dims){
        return true;
    }
    
    public int blockingDegree(){
        return 0;
    }
    
    public boolean makingPool(int keys, boolean pipe){
        return false;
    }
    
    @Override
    public String toString(){
        return this.name;
    }
    
    public int dimOf(String dim){
        return dimOf(dim, 0);
    }
    
    public int dimOf(String name, int from){
        for(int i=from; i<this.schema.size(); i++) if(this.schema.get(i).hasName(name)) return i;
        return -1;
    }
    
    public int msOf(String ms){
        for(int i=0; i<this.measures.size(); i++) if(this.measures.get(i).name().equalsIgnoreCase(ms)) return i;
        return -1;
    }
}
