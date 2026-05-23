package stepper.model.engine;

import java.util.*;
import stepper.util.*;
import stepper.model.sql.*;
import it.unimi.dsi.fastutil.longs.*;
import stepper.model.engine.flows.*;

public class AggPool extends Pool{
    protected int[] dists;
    protected int[][] nums;
    protected Long2IntMap locs;
    protected ArrayList<BitSet>[] cmarks;
    protected final SQLAggregation[] aggs;
    
    protected int disnum;
    private final double[] qs;
    protected Long2ObjectMap buckets;
    
    public AggPool(Action act, int[] dins, int[] cols, int[] frees){
        super(act, cols, frees);
        int a2 = act.cube.measures.length;
        
        dists = dins;
        qs = new double[a2];
        nums = new int[a2][];
        x = new int[cols.length];
        aggs = new SQLAggregation[a2];
        
        long size = act.cube.sizeS(frees), size1 = act.cube.sizeS(cols);
        int len = act.input.cube.cardinality();
        if(len>5000000) len = (int)(len/(size+size1)*size);
        len = size<10000 ? (int)size : len==0 ? 10000 : len;
        init(len, size);
    }
    
    public AggPool(Action act, int[] dins, int[] cols, int blockings){
        super(act, cols, blockings);
        
        int a2 = act.cube.measures.length;
        
        dists = dins;
        qs = new double[a2];
        nums = new int[a2][];
        aggs = new SQLAggregation[a2];
        frees = act.input.cube.dimensionsS(cols);
        
        long size = act.cube.size(), size1 = act.input.cube.sizeS()/size;
        int len = act.input.cube.cardinality();
        if(len>5000000) len = (int)(len/(size+size1)*size);
        len = size<10000 ? (int)size : len==0 ? 10000 : len;
        init(len, size);
    }
    
    private void init(int len, long size){
        int a2 = act.cube.measures.length;
        ArrayCube cube = (ArrayCube)act.cube;
        
        if(cube.schema.isEmpty()){
            for(int i=0; i<a2; i++){
                aggs[i] = (SQLAggregation)cube.measures[i];
                if(aggs[i].isDistinct()){
                    if(cmarks==null) cmarks = new ArrayList[a2];
                    cmarks[i] = new ArrayList();
                }
                act.cube.allocate(1);
                nums[i] = aggs[i].isAvg() ? new int[1] : null;
            }
        }else if(cube instanceof SparseCube){
            locs = new Long2IntOpenHashMap((int)len);
            locs.defaultReturnValue(-1);
            for(int i=0; i<a2; i++){
                aggs[i] = (SQLAggregation)cube.measures[i];
                if(aggs[i].isQuantile()){
                    qs[i] = ((SQLQuantile)aggs[i]).p();
                    if(buckets!=null) continue;
                    buckets = new Long2ObjectOpenHashMap(len);
                    buckets.defaultReturnValue(null);
                }else if(aggs[i].isDistinct()){
                    disnum++;
                    if(cmarks==null) cmarks = new ArrayList[a2];
                    cmarks[i] = new ArrayList();
                }else{
                    if(cube.values[i]==null) cube.allocate(len);
                    nums[i] = aggs[i].isAvg() ? new int[(int)len] : null;
                }
            }
        }else{
            for(int i=0; i<a2; i++){
                aggs[i] = (SQLAggregation)cube.measures[i];
                if(aggs[i].isQuantile()){
                    qs[i] = ((SQLQuantile)aggs[i]).p();
                    if(buckets!=null) continue;
                    buckets = new Long2ObjectOpenHashMap(len);
                    buckets.defaultReturnValue(null);
                }else if(aggs[i].isDistinct()){
                    disnum++;
                    if(cmarks==null) cmarks = new ArrayList[a2];
                    cmarks[i] = new ArrayList();
                    if(locs==null){
                        locs = new Long2IntOpenHashMap(len);
                        locs.defaultReturnValue(-1);
                    }
                }
                if(cube.values[i]==null) act.cube.allocate((int)size);
                nums[i] = aggs[i].isAvg() ? new int[(int)size] : null;
            }
        }
    }
    
    @Override
    public void execute(){
        Cube input = act.input.cube;
        if(input instanceof SparseCube) execute((SparseCube)input);
        else execute((ArrayCube)input);
        putTogether();
    }
    
    private void execute(SparseCube in){
        boolean yes = true;
        Cube cube = act.cube;
        Pool c1 = act.input.pool;
        double p = in.cardinality() * 1.0;
        double[] y = new double[aggs.length];
        if(in.longs.idxes==null) for(int i=0; i<in.longs.size; i++, yes = true, loc=0){
            if(in.bits!=null && !in.bits.get(i)) continue;
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, in.longs.data[i], c1.spans);
            for(int k=0; yes && k<cols.length; k++) yes &= cube.marked(k, c1.pos[cols[k]]);
            for(int k=0; yes && k<frees.length; k++) yes &= cube.marked(cols.length + k, c1.pos[frees[k]]);
            if(!yes) continue;
            
            for(int k=0; k<in.values.length; k++) y[k] = in.values[k][i];
            for(int k=0; k<cols.length; k++) loc += (in.bases[cols[k]]==null ? c1.pos[cols[k]] : in.bases[cols[k]][c1.pos[cols[k]]]) * spans[k];
            if(act.cube.schema.isEmpty()) aggregate(y);
            else if(cube instanceof SparseCube) aggregate((SparseCube)cube, (i+1)/p, y);
            else aggregate((ArrayCube)cube, (i+1)/p, y);
        } else for(int i=0; i<in.longs.size; i++, yes = true, loc=0){
            if(in.bits!=null && !in.bits.get(in.longs.idxes[i])) continue;
            if(cd!=null && !yes(in.longs.idxes[i])) continue;
            in.coordinates(c1.pos, in.longs.data[in.longs.idxes[i]], c1.spans);
            for(int k=0; yes && k<cols.length; k++) yes &= cube.marked(k, c1.pos[cols[k]]);
            for(int k=0; yes && k<frees.length; k++) yes &= cube.marked(cols.length + k, c1.pos[frees[k]]);
            if(!yes) continue;
            
            for(int k=0; k<in.values.length; k++) y[k] = in.values[k][in.longs.idxes[i]];
            for(int k=0; k<cols.length; k++) loc += (in.bases[cols[k]]==null ? c1.pos[cols[k]] : in.bases[cols[k]][c1.pos[cols[k]]]) * spans[k];
            if(act.cube.schema.isEmpty()) aggregate(y);
            else if(cube instanceof SparseCube) aggregate((SparseCube)cube, (i+1)/p, y);
            else aggregate((ArrayCube)cube, (i+1)/p, y);
        }
    }
    
    private void execute(ArrayCube in){
        boolean yes = true;
        Cube cube = act.cube;
        Pool c1 = act.input.pool;
        double p = in.cardinality() * 1.0;
        double[] y = new double[aggs.length];
        for(int r=1, i=in.bits.nextSetBit(0); i>=0; i=in.bits.nextSetBit(i+1), r++, yes = true, loc=0){
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<cols.length; k++) yes &= cube.marked(k, c1.pos[cols[k]]);
            for(int k=0; yes && k<frees.length; k++) yes &= cube.marked(cols.length + k, c1.pos[frees[k]]);
            if(!yes) continue;
            
            for(int k=0; k<in.values.length; k++) y[k] = in.values[k][i];
            for(int k=0; k<cols.length; k++) loc += (in.bases[cols[k]]==null ? c1.pos[cols[k]] : in.bases[cols[k]][c1.pos[cols[k]]]) * spans[k];
            if(act.cube.schema.isEmpty()) aggregate(y);
            else if(cube instanceof SparseCube) aggregate((SparseCube)cube, r/p, y);
            else aggregate((ArrayCube)cube, r/p, y);
        }
    }
    
    @Override
    public void pumping(ActionFlow flow){
        Cube input = act.input.cube;
        if(input instanceof SparseCube) pumping(flow, (SparseCube)input);
        else pumping(flow, (ArrayCube)input);
        looping(flow, 1);
        flow.flush(0);
    }
    
    private void pumping(ActionFlow flow, SparseCube in){
        boolean yes = true;
        Cube cube = act.cube;
        Pool c1 = act.input.pool;
        double p =in.cardinality() * 1.0, 
               gp = cube.sizeS(frees) * 1.0;
        double[] y = new double[aggs.length];
        if(in.longs.idxes==null) for(int i=0; i<in.longs.size; i++, yes = true, loc=0){
            if(in.bits!=null && !in.bits.get(i)) continue;
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, in.longs.data[i], c1.spans);
            for(int k=0; yes && k<cols.length; k++) yes &= cube.marked(k, c1.pos[cols[k]]);
            for(int k=0; yes && k<frees.length; k++) yes &= cube.marked(cols.length + k, c1.pos[frees[k]]);
            if(!yes) continue;
            
            for(int k=0; k<cols.length; k++){
                x[k] = in.bases[cols[k]]==null ? c1.pos[cols[k]] : in.bases[cols[k]][c1.pos[cols[k]]];
                loc += x[k] * spans[k];
            }
            if(isFull(loc)) looping(flow, (i+1)/p);
            
            loc = 0;
            System.arraycopy(x, 0, pos, 0, cols.length);
            for(int k=0; k<in.values.length; k++) y[k] = in.values[k][i];
            for(int k=0; k<frees.length; k++) loc += (in.bases[frees[k]]==null ? c1.pos[frees[k]] : in.bases[frees[k]][c1.pos[frees[k]]]) * spans[cols.length+k];
            if(cube instanceof SparseCube) aggregate((SparseCube)cube, loc/gp, y);
            else aggregate((ArrayCube)cube, loc/gp, y);
        } else for(int i=0; i<in.longs.size; i++, yes = true, loc=0){
            if(in.bits!=null && !in.bits.get(in.longs.idxes[i])) continue;
            if(cd!=null && !yes(in.longs.idxes[i])) continue;
            in.coordinates(c1.pos, in.longs.data[in.longs.idxes[i]], c1.spans);
            for(int k=0; yes && k<cols.length; k++) yes &= cube.marked(k, c1.pos[cols[k]]);
            for(int k=0; yes && k<frees.length; k++) yes &= cube.marked(cols.length + k, c1.pos[frees[k]]);
            if(!yes) continue;
            
            for(int k=0; k<cols.length; k++){
                x[k] = in.bases[cols[k]]==null ? c1.pos[cols[k]] : in.bases[cols[k]][c1.pos[cols[k]]];
                loc += x[k] * spans[k];
            }
            if(isFull(loc)) looping(flow, (i+1)/p);
            
            loc = 0;
            System.arraycopy(x, 0, pos, 0, cols.length);
            for(int k=0; k<in.values.length; k++) y[k] = in.values[k][in.longs.idxes[i]];
            for(int k=0; k<frees.length; k++) loc += (in.bases[frees[k]]==null ? c1.pos[frees[k]] : in.bases[frees[k]][c1.pos[frees[k]]]) * spans[cols.length+k];
            if(cube instanceof SparseCube) aggregate((SparseCube)cube, loc/gp, y);
            else aggregate((ArrayCube)cube, loc/gp, y);
        }
    }
    
    private void pumping(ActionFlow flow, ArrayCube in){
        boolean yes = true;
        Cube cube = act.cube;
        Pool c1 = act.input.pool;
        double p =in.cardinality() * 1.0, 
               gp = cube.sizeS(frees) * 1.0;
        double[] y = new double[aggs.length];
        for(int r=1, i=in.bits.nextSetBit(0); i>=0; i=in.bits.nextSetBit(i+1), r++, yes = true, loc=0){
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<cols.length; k++) yes &= cube.marked(k, c1.pos[cols[k]]);
            for(int k=0; yes && k<frees.length; k++) yes &= cube.marked(cols.length + k, c1.pos[frees[k]]);
            if(!yes) continue;
            
            for(int k=0; k<cols.length; k++){
                x[k] = in.bases[cols[k]]==null ? c1.pos[cols[k]] : in.bases[cols[k]][c1.pos[cols[k]]];
                loc += x[k] * spans[k];
            }
            if(isFull(loc)) looping(flow, r/p);
            
            loc = 0;
            System.arraycopy(x, 0, pos, 0, cols.length);
            for(int k=0; k<in.values.length; k++) y[k] = in.values[k][i];
            for(int k=0; k<frees.length; k++) loc += (in.bases[frees[k]]==null ? c1.pos[frees[k]] : in.bases[frees[k]][c1.pos[frees[k]]]) * spans[cols.length+k];
            if(cube instanceof SparseCube) aggregate((SparseCube)cube, loc/gp, y);
            else aggregate((ArrayCube)cube, loc/gp, y);
        }
    }
    
    private void looping(ActionFlow flow, double p){
        putTogether();
        if(act.cube instanceof SparseCube){
            SparseCube cube = (SparseCube)act.cube;
            for(int i=0; i<cube.longs.size; i++){
                cube.coordinates(pos, cols.length, cube.longs.data[i], spans);
                for(int k=0; k<cube.values.length; k++) flow.y[k] = cube.values[k][i];
                flow.push(0, p);
            }
        }else{
            ArrayCube cube = (ArrayCube)act.cube;
            for(int i=cube.bits.nextSetBit(0); i>=0; i=cube.bits.nextSetBit(i+1)){
                cube.coordinates(pos, cols.length, i, spans);
                for(int k=0; k<cube.values.length; k++) flow.y[k] = cube.values[k][i];
                flow.push(0, p);
            }
        }
        reset();
    }
    
    @Override
    public void push(PipeJoin flow, int i, double p){
        push((ActionFlow)flow, i, p);
    }
    
    @Override
    public void push(ActionFlow flow, int i, double p){
        if(cd!=null && !yes(flow.y)) return;
        
        loc = 0;
        Pool c1 = act.input.pool;
        Cube in = act.input.cube, cube = act.cube;
        for(int k=0; k<cols.length; k++) if(!cube.marked(k, c1.pos[cols[k]])) return;
        for(int k=0; k<frees.length; k++) if(!cube.marked(cols.length + k, c1.pos[frees[k]])) return;
         
        for(int k=0; k<cols.length; k++) loc += (in.bases[cols[k]]==null ? c1.pos[cols[k]] : in.bases[cols[k]][c1.pos[cols[k]]]) * spans[k];
        if(act.cube.schema.isEmpty()) aggregate(flow.y);
        else if(cube instanceof SparseCube) aggregate((SparseCube)cube, p, flow.y);
        else aggregate((ArrayCube)cube, p, flow.y);
    }
    
    @Override
    public void flush(PipeJoin flow, int i, double p){
        push(flow, i, p);
        putTogether();
    }
    
    @Override
    public void flush(ActionFlow flow, int i, double p){
        push(flow, i, p);
        putTogether();
    }
    
    
    @Override
    public void flush(PipeJoin flow, int i){
        putTogether();
    }
    
    @Override
    public void flush(ActionFlow flow, int i){
        putTogether();
    }
    
    private void aggregate(SparseCube cube, double p, double[] y){
        DoubleArrays doubles = null;
        for(int i=0; i<aggs.length; i++){
            if(aggs[i].isQuantile()){
                doubles = (DoubleArrays)buckets.get(loc);
                locs.putIfAbsent(loc, locs.size());
                if(doubles!=null) continue;
                doubles = new DoubleArrays(qs, 10000);
                buckets.put(loc, doubles);
            }else if(aggs[i].isDistinct()){
                int k = locs.get(loc);
                if(k==-1){
                    k = locs.size();
                    locs.put(loc, k);
                    cmarks[i].add(new BitSet());
                }
                cmarks[i].get(k).set(act.input.pool.pos[dists[i]]);
            }else{
                int k = locs.get(loc);
                if(k==-1){
                    k = locs.size();
                    locs.put(loc, k);
                    if(k>=cube.longs.data.length) cube.expands(this, p);
                    cube.longs.size++;
                    cube.longs.data[k] = loc;
                    if(nums[i]!=null) nums[i][k]++;
                    cube.values[i][k] = aggs[i].isCount() ? 1 : y[i];
                }else{
                    if(nums[i]!=null) nums[i][k]++;
                    cube.values[i][k] = aggs[i].isCount() ? ++cube.values[i][k] : aggs[i].compute(cube.values[i][k], y[i]);
                }
            }
        }
        if(doubles!=null) doubles.add(p, y);
    }
    
    private void aggregate(ArrayCube cube, double p, double[] y){
        DoubleArrays doubles = null;
        for(int i=0; i<aggs.length; i++){
            if(aggs[i].isQuantile()){
                doubles = (DoubleArrays)buckets.get(loc);
                cube.bits.set((int)loc);
                if(doubles!=null) continue;
                doubles = new DoubleArrays(qs, 10000);
                buckets.put(loc, doubles);
            }else if(aggs[i].isDistinct()){
                int k = locs.get(loc);
                if(k==-1){
                    k = locs.size();
                    locs.put(loc, k);
                    cmarks[i].add(new BitSet());
                }
                cmarks[i].get(k).set(act.input.pool.pos[dists[i]]);
            }else{
                int k = (int)loc;
                if(nums[i]!=null) nums[i][k]++;
                if(cube.bits.get(k)){
                    cube.values[i][k] = aggs[i].isCount() ? ++cube.values[i][k] : aggs[i].compute(cube.values[i][k], y[i]);
                }else{
                    cube.bits.set(k);
                    cube.values[i][k] = aggs[i].isCount() ? 1 : y[i];
                }
            }
        }
        if(doubles!=null) doubles.add(p, y);
    }
    
    private void aggregate(double[] y){
        for(int i=0; i<aggs.length; i++){
            if(aggs[i].isDistinct()){
                if(cmarks[i].isEmpty()){act.cube.bits.set(0); cmarks[i].add(new BitSet());}
                cmarks[i].get(0).set(act.input.pool.pos[dists[i]]);
            }else{
                if(nums[i]!=null) nums[i][0]++;
                if(act.cube.bits.get(0)){
                    act.cube.values[i][0] = aggs[i].isCount() ? ++act.cube.values[i][0] : aggs[i].compute(act.cube.values[i][0], y[i]);
                }else{
                    act.cube.bits.set(0);
                    act.cube.values[i][0] = aggs[i].isCount() ? 1 : y[i];
                }
            }
        }
    }
    
    protected void putTogether(){
        if(act.cube.schema.isEmpty()){
            for(int i=0; i<aggs.length; i++){
                if(aggs[i].isDistinct()) act.cube.values[i][0] = cmarks[i].get(0).cardinality();
                else if(nums[i]!=null) act.cube.values[i][0] /= nums[i][0];
            }
            return;
        }
        
        if(act.cube instanceof SparseCube){
            SparseCube cube = (SparseCube)act.cube;
            if(cube.longs==null || cube.longs.size<locs.size()){
                cube.allocate(locs.size());
                for(long key: locs.keySet()) cube.longs.data[cube.longs.size++] = key;
            }
            cube.longs.loc2idx = locs;
            if(buckets!=null) for(int i=0; i<cube.longs.size; i++) ((DoubleArrays)buckets.get(i)).compute(i, cube.values);
            for(int i=0; i<aggs.length; i++){
                if(nums[i]!=null) for(int k=0; k<cube.longs.size; k++) cube.values[i][k]/=nums[i][k];
                if(cmarks!=null && cmarks[i]!=null) for(int k=0; k<cube.longs.size; k++) cube.values[i][k] = cmarks[i].get(k).cardinality();
            }
        }else{
            ArrayCube cube = (ArrayCube)act.cube;
            if(disnum==aggs.length) for(long key: locs.keySet()) cube.bits.set((int)key);
            if(buckets!=null) for(int k=cube.bits.nextSetBit(0); k>=0; k=cube.bits.nextSetBit(k+1)) ((DoubleArrays)buckets.get(k)).compute(k, cube.values);
            for(int i=0; i<aggs.length; i++){
                if(nums[i]!=null) for(int k=cube.bits.nextSetBit(0); k>=0; k=cube.bits.nextSetBit(k+1)) cube.values[i][k] /= nums[i][k];
                if(cmarks!=null && cmarks[i]!=null) for(int k=cube.bits.nextSetBit(0), m=0; k>=0; k=cube.bits.nextSetBit(k+1)) cube.values[i][k] = cmarks[i].get(m++).cardinality();
            }
        }
    }
    
    @Override
    public void reset(){
        if(locs!=null) locs.clear();
        if(buckets!=null) for(long key: buckets.keySet()) ((DoubleArrays)buckets.get(key)).size = 0;
        for(int i=0; i<aggs.length; i++){
            if(nums[i]!=null) Arrays.fill(nums[i], 0);
            if(cmarks!=null && cmarks[i]!=null) cmarks[i].clear();
        }
        if(act.cube instanceof SparseCube) ((SparseCube)act.cube).longs.size=0;
        else act.cube.bits.clear();
    }
}
