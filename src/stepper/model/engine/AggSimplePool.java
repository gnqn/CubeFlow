package stepper.model.engine;

import java.util.*;
import stepper.util.*;
import stepper.model.sql.*;
import stepper.model.engine.flows.*;

public class AggSimplePool extends Pool{
    protected int[] dists;
    protected int[] nums;
    protected double[] values;
    protected BitSet[] marks;
    protected final SQLAggregation[] aggs;
    
    private final double[] y;
    private DoubleArrays doubles;
    
    public AggSimplePool(Action act, int[] dins, int[] cols){
        super(act, cols);
        ArrayCube cube = (ArrayCube)act.cube;
        
        x = new int[cube.arity()];
        dists = dins;
        nums = new int[cube.measures.length];
        frees = act.input.cube.dimensionsS(cols);
        y = new double[act.input.cube.measures.length];
        aggs = new SQLAggregation[cube.measures.length];
        values = new double[aggs.length];
        if(dists!=null) marks = new BitSet[aggs.length];
        
        double[] qs = null;
        for(int i=0; i<aggs.length; i++){
            aggs[i] = (SQLAggregation)cube.measures[i];
            if(aggs[i].isQuantile()){if(qs==null) qs = new double[aggs.length]; qs[i] = ((SQLQuantile)aggs[i]).p();}
        }
        if(qs!=null){
            long len = (int)(act.input.cube.sizeS()/act.cube.size());
            len = (long)(len>10000000 ? len * 0.1 : len>5000000 ? len * 0.2 : len);
            doubles = new DoubleArrays(qs, len<10000 ? 10000 : (int)len);
        }
    }
    
    @Override
    public void pumping(ActionFlow flow){
        Cube input = act.input.cube;
        if(input instanceof SparseCube) pumping(flow, (SparseCube)input);
        else pumping(flow, (ArrayCube)input);
        putTogether(flow, 0, 1, true);
    }
    
    private void pumping(ActionFlow flow, SparseCube in){
        boolean yes = true;
        Cube cube = act.cube;
        Pool c1 = act.input.pool;
        double p = in.cardinality() * 1.0;
        if(in.longs.idxes==null) for(int i=0; i<in.longs.size; i++, yes = true, loc=0){
            if(in.bits!=null && !in.bits.get(i)) continue;
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, in.longs.data[i], c1.spans);
            for(int k=0; yes && k<cols.length; k++) yes &= cube.marked(k, c1.pos[cols[k]]);
            for(int k=0; yes && k<frees.length; k++) yes &= cube.marked(cols.length + k, c1.pos[frees[k]]);
            if(!yes) continue;
            
            for(int k=0; k<cols.length; k++) x[k] = in.bases[cols[k]]==null ? c1.pos[cols[k]] : in.bases[cols[k]][c1.pos[cols[k]]];
            for(int k=0; k<cols.length; k++) loc += x[k] * spans[k];
            if(isFull(loc)){putTogether(flow, 0, (i+1)/p, false); reset();}
            pos = x.clone();
            aggregate((i+1)/p, i);
        } else for(int i=0; i<in.longs.size; i++, yes = true, loc=0){
            if(in.bits!=null && !in.bits.get(in.longs.idxes[i])) continue;
            if(cd!=null && !yes(in.longs.idxes[i])) continue;
            in.coordinates(c1.pos, in.longs.data[in.longs.idxes[i]], c1.spans);
            for(int k=0; yes && k<cols.length; k++) yes &= cube.marked(k, c1.pos[cols[k]]);
            for(int k=0; yes && k<frees.length; k++) yes &= cube.marked(cols.length + k, c1.pos[frees[k]]);
            if(!yes) continue;
            
            for(int k=0; k<cols.length; k++) x[k] = in.bases[cols[k]]==null ? c1.pos[cols[k]] : in.bases[cols[k]][c1.pos[cols[k]]];
            for(int k=0; k<cols.length; k++) loc += x[k] * spans[k];
            if(isFull(loc)){putTogether(flow, 0, (i+1)/p, false); reset();}
            pos = x.clone();
            aggregate((i+1)/p, in.longs.idxes[i]);
        }
    }
    
    private void pumping(ActionFlow flow, ArrayCube in){
        boolean yes = true;
        Cube cube = act.cube;
        Pool c1 = act.input.pool;
        double p = in.cardinality() * 1.0;
        for(int r=1, i=in.bits.nextSetBit(0); i>=0; i=in.bits.nextSetBit(i+1), r++, yes = true, loc=0){
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<cols.length; k++) yes &= cube.marked(k, c1.pos[cols[k]]);
            for(int k=0; yes && k<frees.length; k++) yes &= cube.marked(cols.length + k, c1.pos[frees[k]]);
            if(!yes) continue;
            
            for(int k=0; k<cols.length; k++) x[k] = in.bases[cols[k]]==null ? c1.pos[cols[k]] : in.bases[cols[k]][c1.pos[cols[k]]];
            for(int k=0; k<cols.length; k++) loc += x[k] * spans[k];
            if(isFull(loc)){putTogether(flow, 0, r/p, false); reset();}
            pos = x.clone();
            aggregate(r/p, i);
        }
    }
    
    @Override
    public void push(PipeJoin flow, int i, double p){
        if(cd!=null && !yes(flow.y)) return;
        
        loc = 0;
        Pool c1 = act.input.pool;
        Cube input = act.input.cube, cube = act.cube;
        for(int k=0; k<cols.length; k++) if(!cube.marked(k, c1.pos[cols[k]])) return;
        for(int k=0; k<frees.length; k++) if(!cube.marked(cols.length + k, c1.pos[frees[k]])) return;
            
        for(int k=0; k<cols.length; k++) x[k] = input.bases[cols[k]]==null ? c1.pos[cols[k]] : input.bases[cols[k]][c1.pos[cols[k]]];
        for(int k=0; k<cols.length; k++) loc += x[k] * spans[k];
        if(this.isFull(loc)){putTogether(flow, i, p, false); reset();}
        pos = x.clone();
        aggregate(flow, p);
    }
    
    @Override
    public void push(ActionFlow flow, int i, double p){
        if(cd!=null && !yes(flow.y)) return;
        
        loc = 0;
        Pool c1 = act.input.pool;
        Cube input = act.input.cube, cube = act.cube;
        for(int k=0; k<cols.length; k++) if(!cube.marked(k, c1.pos[cols[k]])) return;
        for(int k=0; k<frees.length; k++) if(!cube.marked(cols.length + k, c1.pos[frees[k]])) return;
            
        for(int k=0; k<cols.length; k++) x[k] = input.bases[cols[k]]==null ? c1.pos[cols[k]] : input.bases[cols[k]][c1.pos[cols[k]]];
        for(int k=0; k<cols.length; k++) loc += x[k] * spans[k];
        if(this.isFull(loc)){putTogether(flow, i, p, false); reset();}
        pos = x.clone();
        aggregate(flow, p);
    }
    
    @Override
    public void flush(PipeJoin flow, int i, double p){
        flush((ActionFlow)flow, i, p);
    }
    
    @Override
    public void flush(ActionFlow flow, int i, double p){
        if(cd!=null && !yes(flow.y)){putTogether(flow, i, 1, true); return;}
        
        Pool c1 = act.input.pool;
        Cube input = act.input.cube, cube = act.cube;
        for(int k=0; k<cols.length; k++) if(!cube.marked(k, c1.pos[cols[k]])){putTogether(flow, i, 1, true); return;}
        for(int k=0; k<frees.length; k++) if(!cube.marked(cols.length + k, c1.pos[frees[k]])){putTogether(flow, i, 1, true); return;}
        
        for(int k=0; k<cols.length; k++) pos[k] = input.bases[cols[k]]==null ? c1.pos[cols[k]] : input.bases[cols[k]][c1.pos[cols[k]]];
        aggregate(flow, p);
        putTogether(flow, i, 1, true);
    }
    
    @Override
    public void flush(PipeJoin flow, int i){
        flush((ActionFlow)flow, i);
    }
    
    @Override
    public void flush(ActionFlow flow, int i){
        putTogether(flow, i, 1, true);
    }
    
    private void aggregate(double p, int r){
        if(doubles!=null) doubles.add(p, r, act.input.cube.values);
        for(int i=0; i<aggs.length; i++){
            if(aggs[i].isQuantile()) continue;
            if(aggs[i].isDistinct()){
                if(marks[i]==null) marks[i] = new BitSet();
                marks[i].set(act.input.pool.pos[dists[i]]);
            }else if(nums[i]++==0) values[i] = aggs[i].isCount() ? 1 : act.input.cube.values[i][r];
            else values[i] = aggs[i].isCount() ? ++values[i] : aggs[i].compute(values[i], act.input.cube.values[i][r]);
        }
    }
    
    private void aggregate(ActionFlow flow, double p){
        if(doubles!=null) doubles.add(p, flow.y);
        for(int i=0; i<aggs.length; i++){
            if(aggs[i].isQuantile()) continue;
            if(aggs[i].isDistinct()){
                if(marks[i]==null) marks[i] = new BitSet();
                marks[i].set(act.input.pool.pos[dists[i]]);
            }else if(nums[i]++==0) values[i] = aggs[i].isCount() ? 1 : flow.y[i];
            else values[i] = aggs[i].isCount() ? ++values[i] : aggs[i].compute(values[i], flow.y[i]);
        }
    }
    
    protected void putTogether(ActionFlow flow, int id, double p, boolean flush){
        if(!flush) System.arraycopy(flow.y, 0, y, 0, y.length);
        if(doubles!=null) doubles.compute(flow.y);
        for(int i=0; i<aggs.length; i++){
            if(!aggs[i].isQuantile()) flow.y[i] = aggs[i].isDistinct() ? marks[i].cardinality() : aggs[i].isAvg() ? values[i]/nums[i] : values[i];
        }
        if(flush) flow.flush(id, p);
        else{flow.push(id, p); System.arraycopy(y, 0, flow.y, 0, y.length);}
    }
    
    @Override
    public void reset(){
        Arrays.fill(nums, 0);
        Arrays.fill(values, 0);
        if(marks!=null) Arrays.fill(marks, null);
        if(doubles!=null) doubles.size = 0;
    }
}
