package stepper.model.engine;

import java.util.*;
import stepper.model.*;
import stepper.model.sql.*;

public class Trans extends Action{
    protected Condition cond;
    protected final SQLFunction trans;
    protected DimensionSpace partition;
    
    private int[] parts;
    
    public Trans(QTra node, Action in){
        super(node.name(), node.getDimensions(), node.measures(), in);
        this.trans = node.getTrans();
        this.cond = node.getCondition();
        this.partition = node.getPartition();
        this.kord = !(this.trans instanceof SQLDateTrunc && this.dimOf(this.trans.getAttribute().name())<this.schema.arity()-1);
        if(this.partition.isEmpty()) this.partition = null;
        if(this.cond!=null && this.cond.isEmpty()) this.cond = null;
    }
    
    @Override
    public Condition condition(){
        return this.cond;
    }
    
    @Override
    public boolean composable(){
        return partition==null;
    }
    
    @Override
    public boolean isBlocking(){
        return partition!=null;
    }
    
    @Override
    public boolean isotonic(){
        return this.trans==null;
    }
    
    @Override
    public boolean makingPool(int blockings, boolean pipe){
        int arity = this.input.cube.arityS();
        this.cdxes = new int[arity];
        this.cube = input.cube.building(arity, schema, this.measures);
        this.cube.marks = new BitSet[arity];
        
        int[] cols1 = input.cols();
        for(int k=0; k<arity; k++){
            this.cube.dims[k] = input.cube.dims[k];
            this.cube.maps[k] = input.cube.maps[k];
            this.cube.bases[k] = input.cube.bases[k];
            this.cube.marks[k] = input.cube.marks[k];
            Dimension d1 = k>=input.schema.size() ? null : input.cdxes==null ? input.schema.get(k) : input.cdxes[k]==-1 ? null : input.schema.get(input.cdxes[k]);
            if(d1!=null && !this.cube.marking(k, d1, cond, input.cube)) return false;
            
            int i = d1==null ? -1 : this.dimOf(d1.name());
            cdxes[input.cdxes==null || input.cdxes[k]==-1 ? k : cols1[input.cdxes[k]]] = i;
            if(i==-1){
                if(this.cube.bases[k]==null) this.cube.bases[k] = new int[0];
            }else{
                this.schema.get(i).typ = d1.typ;
            }
        }
        
        this.cube.g = input.cube.g;
        int[] cols = this.cols(), frees = this.frees();
        int k = this.trans==null ? -1 : cols[this.dimOf(this.trans.getAttribute().name())];
        if(k!=-1) trans.trans(cols[k], cube.dims, cube.marks, cube.maps, cube.bases);
        
        for(int i=0; i<ords.length; i++){
            if(cols[i]<input.ords.length) ords[i] = input.ords[cols[i]];
            if(!ords[i] || cube.isLShadow(i)) break;
        }
        
        if(partition==null){
            this.pool = new Pool(this, cols, frees);
        }else{
            parts = new int[partition.size()];
            for(int i=0; i<partition.size(); i++){
                Dimension dim = this.partition.get(i);
                parts[i] = cols[this.input.dimOf(dim.name())];
            }
            this.pool = new RankPool(this, cols, frees, parts);
        }
        return true;
    }
    
    @Override
    public boolean execute(boolean nosort){
        if(this.cube==null && !makingPool(this.schema.size(), false)) return false;
        pool.execute();
        if(!nosort && !kord){this.cube.sort(); kord=true;}
        return this.cube.cardinality()>0;
    }
}
