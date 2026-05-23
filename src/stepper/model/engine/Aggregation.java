package stepper.model.engine;

import java.util.*;
import stepper.util.*;
import stepper.model.*;
import stepper.model.sql.*;

public class Aggregation extends Action{
    protected Condition cond;
    
    public Aggregation(QAgg node, Action in){
        super(node.name(), node.getDimensions(), node.measures(), in);
        this.cond = node.getCondition();
        if(this.cond!=null && this.cond.isEmpty()) this.cond = null;
    }
    
    @Override
    public Condition condition(){
        return this.cond;
    }
    
    @Override
    public boolean composable(){
        return !this.schema.isEmpty() && this.input.schema.startsWith(schema) && this.input.ordered(schema);
    }
    
    @Override
    public boolean makingPool(int blockings, boolean pipe){
        int[] c2g1 = this.input.cube.c2g();
        this.cube = input.cube.building(schema.size(), schema, this.measures);
        
        Ints cols = new Ints();
        int arity = input.cube.arityS();
        this.cube.marks = new BitSet[arity];
        
        for(int i=0; i<this.schema.size(); i++){
            Dimension dim = this.schema.get(i);
            int k = this.input.dimOf(dim.name());
            if(cols.contains(k)) k = this.input.dimOf(dim.name(), k+1);
            cols.add(c2g1[k]);
            dim.typ = input.cube.schema.get(k).typ;
            if(!this.cube.pick(i, c2g1[k], dim, cond, input.cube)) return false;
        }
        
        for(int i=0, k=0, m=0; k<arity; k++){
            if(cols.contains(k)){m++; continue;}
            if(input.cube.isGShadow(k)) cube.marks[cols.size+i++] = input.cube.marks[k];
            else if(!cube.marking(cols.size+i++, k, input.schema.get(m++), cond, input.cube)) return false;
        }
        
        int[] dins = null;
        for(int i=0; i<this.measures.size(); i++){
            Attribute attr = this.measures.get(i);
            if(!(attr instanceof SQLAggregation)) continue;
            SQLAggregation ms = (SQLAggregation)attr;
            if(!ms.isDistinct()) continue;
            if(dins==null){
                dins = new int[this.measures.size()];
                Arrays.fill(dins, -1);
            }
            Attribute dim = (Attribute)ms.params().get(0);
            dins[i] = c2g1[this.input.dimOf(dim.name())];
        }
        
        arity = this.schema.size();
        int[] frees = null, recols = null;
        if(blockings==0) for(int i=0; frees==null && i<arity; i++) if(input.cube.isLShadow(cols.ints[i])) frees = new int[arity-(i+1)];
        if(frees!=null){
            recols = new int[arity - frees.length];
            System.arraycopy(cols.ints, 0, recols, 0, recols.length);
            System.arraycopy(cols.ints, arity - frees.length, frees, 0, frees.length);
        }
        
        for(int i=0; i<ords.length; i++) ords[i] = input.ords[cols.ints[i]];
        
        this.pool = frees!=null ? new AggPool(this, dins, recols, frees) : 
                    !this.schema.isEmpty() && blockings==0 ? new AggSimplePool(this, dins, cols.toArray()) : 
                                                             new AggPool(this, dins, cols.toArray(), blockings);
        
        return true;
    }
    
    @Override
    public boolean execute(boolean nosort){
        long cost = System.currentTimeMillis();
        if(this.cube==null && !makingPool(this.schema.size(), false)) return false;
        this.pool.execute();
        System.out.println("Agg(" + name + ") cost:\t" + (System.currentTimeMillis() - cost) + "ms \t(" + this.cube.cardinality() + " rows)");
        return this.cube.cardinality()>0;
    }
}
