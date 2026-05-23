package stepper.model.engine;


import java.util.*;
import stepper.util.*;
import stepper.model.sql.*;
import stepper.model.engine.flows.*;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.*;

public class JoinPipeNeqPool extends JoinPipePool{
    private final int[] cols1;
    protected BitSet[] kmarks;
    protected int[] neq_cols;
    protected int[][] pos2pos;
    protected Object[][] keys;
    protected ArrayList<BitSet[]> neq_joins;
    
    BitSet neq_yes;
    IntArrayList neq;
    Int2IntMap multis;
    Long2IntMap neq_idxes;
    
    private int[] nidxes;
    
    public JoinPipeNeqPool(Action act, int[] nums, int[] cols, int keys, int[][] op, int[][] x2x, int[][] sxx, int[][][] sx2x, IntArrayList neq){
        super(act, nums, cols, keys, op, x2x, sxx, sx2x);

        this.neq = neq;
        
        multis = new Int2IntOpenHashMap();
        multis.defaultReturnValue(-1);
        keys4Neq();
        kmarks = new BitSet[neq_cols.length];
        pos2pos = new int[neq_cols.length][];
        marking4Neq();
        
        int[] frees1 = new int[nums[3]];
        System.arraycopy(cols, nums[0]*6+nums[3], frees1, 0, nums[3]);
        
        int rows = act.input.pool.estimate(frees1, 1000);
        neq_yes = new BitSet(rows);
        neq_joins = new ArrayList(rows);
        neq_idxes = new Long2IntOpenHashMap(rows);
        neq_idxes.defaultReturnValue(-1);
        
        cols1 = new int[neq.size()/3 - multis.size()];
        for(int i=0, k=0; k<neq.size(); k+=3) if(multis.get(k/3)==-1) cols1[i] = neq.getInt(k/3);
        
        int[] cols22 = new int[nums[4]];
        System.arraycopy(cols, frees2, cols22, 0, nums[4]);
        nidxes = indexing(neq_cols, cols22);
    }
    
    @Override
    protected void product(ActionFlow flow, int id, double p, boolean stage, Snapshop slice, Cube in2){
        if(slice==null) return;
        
        BitSet[] neq_join = joinby();
        if(neq_join==null) return;
        
        boolean yes = true;
        ArrayCube cube = (ArrayCube)act.cube;
        for(int i=0; i<slice.size; i++, yes=true){
            for(int k=0; yes && k<neq_join.length; k++) yes &= neq_join[k].get(pos2pos[k][slice.pos[i][neq_cols[k]]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[6]; k++) gpos[nums[5]+k] = slice.pos[i][cols[gcols2+k]];
            for(int k=0; k<nums[4]; k++) pos[cols[cc2+k]] = slice.pos[i][cols[frees2+k]];
            if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
            for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
            
            if(stage) compute(flow, id, p, op, in2, slice.loc[i]);
            else cube.compute(p, cube.location(pos, spans), flow.y, op, in2, slice.loc[i]);
        }
    }
    
    @Override
    protected void looping(ActionFlow flow, int id, double p, Cell cell, boolean sink, Pack buf){
        boolean yes = true;
        ArrayCube cube = (ArrayCube)act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        
        for(int i=0; i<buf.size; i++, yes=true){
            BitSet[] neq_join = joinby(buf.getX(i));
            if(neq_join==null) continue;
            for(int k=0; yes && k<neq_join.length; k++) yes &= neq_join[k].get(pos2pos[k][cell.x[neq_cols[k]]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = buf.xx[cols[frees1+k]][i];
            for(int k=0; k<nums[5]; k++) gpos[k] = buf.xx[cols[gcols1+k]][i];
            if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
            for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
            
            if(sink) cube.compute(p, cube.location(pos, spans), buf, i, op, cell.y);
            else this.compute(flow, id, p, buf, i, op, cell.y);
        }
    }
    
    @Override
    protected void looping(ActionFlow flow, int id, double p, Pack cells, boolean sink, Pack buf){
        boolean yes = true;
        ArrayCube cube = (ArrayCube)act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        
        for(int i=0; i<buf.size; i++, yes=true){
            BitSet[] neq_join = joinby(buf.getX(i));
            if(neq_join==null) continue;
            
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = buf.xx[cols[frees1+k]][i];
            for(int k=0; k<nums[5]; k++) gpos[k] = buf.xx[cols[gcols1+k]][i];
            for(int m=0; m<cells.size; m++, yes=true){
                for(int k=0; yes && k<neq_join.length; k++) yes &= neq_join[k].get(pos2pos[k][cells.xx[neq_cols[k]][m]]);
                if(!yes) continue;
            
                for(int k=0; k<nums[4]; k++) pos[cols[cc2+k]] = cells.xx[cols[frees2+k]][m];
                for(int k=0; k<nums[6]; k++) gpos[nums[5]+k] = cells.xx[cols[gcols2+k]][m];
                if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
                for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
                
                if(sink) cube.compute(p, cube.location(pos, spans), buf, i, op, cells.getY(m));
                else this.compute(flow, id, p, buf, i, op, cells.getY(m));
            }
        }
    }
    
    @Override
    protected void cubeCube(ActionFlow flow, double p, SparseCube in, SparseCube in2){
        boolean yes = true;
        Cube cube = act.cube;
        
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=0){
            if(in.bits!=null && !in.bits.get(i)) continue;
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, in.longs.data[i], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            BitSet[] neq_join = joinby();
            if(neq_join==null) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
            product(flow, 0, (i+1)/p, i, neq_join, slices.get((int)c2.loc), in2);
        }
    }
    
    @Override
    protected void cubeCube(ActionFlow flow, double p, SparseCube in, ArrayCube in2){
        boolean yes = true;
        Cube cube = act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=0){
            if(in.bits!=null && !in.bits.get(i)) continue;
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, in.longs.data[i], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            BitSet[] neq_join = joinby();
            if(neq_join==null) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
            product(flow, 0, (i+1)/p, i, neq_join, slices.get((int)c2.loc), in2);
        }
    }
    
    @Override
    protected void cubeCube(ActionFlow flow, double p, ArrayCube in, SparseCube in2){
        boolean yes = true;
        Cube cube = act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        for(int r=1, i=in.bits.nextSetBit(0); i>=0; i=in.bits.nextSetBit(i+1), r++, yes = true, c2.loc=0){
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            BitSet[] neq_join = joinby();
            if(neq_join==null) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
            product(flow, 0, r/p, i, neq_join, slices.get((int)c2.loc), in2);
        }
    }
    
    @Override
    protected void cubeCube(ActionFlow flow, double p, ArrayCube in, ArrayCube in2){
        boolean yes = true;
        Cube cube = act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        for(int r=1, i=in.bits.nextSetBit(0); i>=0; i=in.bits.nextSetBit(i+1), r++, yes = true, c2.loc=0){
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            BitSet[] neq_join = joinby();
            if(neq_join==null) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
            product(flow, 0, r/p, i, neq_join, slices.get((int)c2.loc), in2);
        }
    }
    
    private void product(ActionFlow flow, int id, double p, int r, BitSet[] neq_join, Snapshop slice, Cube in2){
        if(slice==null) return;
        
        boolean yes = true;
        Cube cube = act.cube;
        for(int i=0; i<slice.size; i++, yes=true){
            for(int k=0; yes && k<neq_join.length; k++) yes &= neq_join[k].get(pos2pos[k][slice.pos[i][nidxes[k]]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[6]; k++) gpos[nums[5]+k] = slice.pos[i][cols[gcols2+k]-nums[0]];
            for(int k=0; k<nums[4]; k++) pos[cols[cc2+k]] = slice.pos[i][k];
            if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
            for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
            compute(flow, id, p, act.input.cube, r, op, in2, slice.loc[i]);
        }
    }
    
    private static int[] indexing(int[] xs, int[] coords){
        int[] idxes = new int[xs.length];
        for(int i=0; i<xs.length; i++) idxes[i] = indexing(xs[i], coords);
        return idxes;
    }
    
    private static int indexing(int c, int[] coords){
        for(int i=0; i<coords.length; i++) if(coords[i]==c) return i;
        return -1;
    }
    
    private BitSet[] joinby(){
        Pool c1 = act.input.pool;
        c1.loc = 0;
        for(int k=0; k<nums[3]; k++) c1.loc += c1.pos[cols[nums[0]*6+nums[3]+k]] * c1.spans[cols[nums[0]*6+nums[3]+k]];
        int idx = neq_idxes.get(c1.loc);
        if(idx==-1){
            idx = neq_idxes.size();
            neq_idxes.put(c1.loc, idx);
            neq_joins.add(new BitSet[neq_cols.length]);
            neq_yes.set(idx, matching4Neq(c1, neq_joins.get(idx)));
        }
        return neq_yes.get(idx) ? neq_joins.get(idx) : null;
    }
    
    private BitSet[] joinby(int[] pos1){
        Pool c1 = act.input.pool;
        c1.loc = 0;
        for(int k=0; k<cols1.length; k++) c1.loc += pos1[cols1[k]] * c1.spans[cols1[k]];
        int idx = neq_idxes.get(c1.loc);
        if(idx==-1){
            idx = neq_idxes.size();
            neq_idxes.put(c1.loc, idx);
            neq_joins.add(new BitSet[neq_cols.length]);
            neq_yes.set(idx, matching4Neq(pos1, neq_joins.get(idx)));
        }
        return neq_yes.get(idx) ? neq_joins.get(idx) : null;
    }
    
    private void keys4Neq(){
        keys = new Object[neq.size()/3][];
        IntArrayList neqs = new IntArrayList();
        
        Cube in2 = act.input2().cube;
        for(int i=0; i<neq.size(); i+=3){
            if(neq.getInt(i+2)==SQLItem.Comparator.NEQUAL){
                neqs.add(neq.getInt(i+1));
                keys[i/3] = in2.dims[neq.getInt(i+1)];
            }else{
                int k = keys4Neq(neq.getInt(i+1), neqs.size() + multis.size());
                if(k==-1){
                    neqs.add(neq.getInt(i+1));
                    keys[i/3] = in2.dims[neq.getInt(i+1)].clone();
                    Arrays.sort(keys[i/3]);
                }else{
                    keys[i/3] = keys[k];
                    multis.put(i/3, k);
                }
            }
        }
        neq_cols = neqs.toIntArray();
    }
    
    private void marking4Neq(){
        Cube in2 = act.input2().cube;
        for(int i=0; i<neq_cols.length; i++){
            int c = neq_cols[i];
            pos2pos[i] = new int[keys[i].length];
            if(in2.marked(c)) kmarks[i] = new BitSet(keys[i].length);
            for(int k=0; k<keys[i].length; k++){
                int r = in2.maps[c].getInt(keys[i][k]);
                pos2pos[i][r] = k;
                if(kmarks[i]!=null && in2.marked(c, r)) kmarks[i].set(k);
            }
        }
    }
    
    private boolean matching4Neq(Pool c1, BitSet[] joins){
        BitSet[] flags = new BitSet[neq.size()/3];
        Cube in = act.input.cube, in2 = act.input2().cube;
        for(int i=0, k=0; k<neq.size(); k+=3){
            Object key = in.dims[neq.getInt(k)][c1.pos[neq.getInt(k)]];
            flags[k/3] = matching4Neq(key, neq.getInt(k+2), keys[k/3], in2.maps[neq.getInt(k+1)]);
            if(kmarks[k/3]!=null) flags[k/3].and(kmarks[k/3]);
            if(flags[k/3].cardinality()==0) return false;
            int m = multis.get(k/3);
            if(m!=-1){
                flags[m].and(flags[k/3]);
                if(flags[m].cardinality()==0) return false;
            }else{
                joins[i++] = flags[k/3];
            }
        }
        return true;
    }
    
    private boolean matching4Neq(int[] pos1, BitSet[] joins){
        BitSet[] flags = new BitSet[neq.size()/3];
        Cube in = act.input.cube, in2 = act.input2().cube;
        for(int i=0, k=0; k<neq.size(); k+=3){
            Object key = in.dims[neq.getInt(k)][pos1[neq.getInt(k)]];
            flags[k/3] = matching4Neq(key, neq.getInt(k+2), keys[k/3], in2.maps[neq.getInt(k+1)]);
            if(kmarks[k/3]!=null) flags[k/3].and(kmarks[k/3]);
            if(flags[k/3].cardinality()==0) return false;
            int m = multis.get(k/3);
            if(m!=-1){
                flags[m].and(flags[k/3]);
                if(flags[m].cardinality()==0) return false;
            }else{
                joins[i++] = flags[k/3];
            }
        }
        return true;
    }
    
    private int keys4Neq(int col, int to){
        for(int i=0; i/3<to; i+=3) if(neq.getInt(i+1)==col && neq.getInt(i+2)!=SQLItem.Comparator.NEQUAL) return i/3;
        return -1;
    }
    
    private static BitSet matching4Neq(Object key, int op, Object[] keys, Hyb2IntMap map){
        BitSet yes = new BitSet(keys.length);
        if(op==SQLItem.Comparator.NEQUAL){
            yes.set(0, keys.length, true);
            int k = map.getInt(key);
            if(k!=-1) yes.clear(k);
            return yes;
        }
        
        int bound = findBound((Comparable)key, op, keys);
        switch(op){
            case SQLItem.Comparator.GREATER:
                if(bound!=-1) yes.set(0, bound, true);
                break;
            case SQLItem.Comparator.GREATERE:
                if(bound!=-1) yes.set(0, bound, true);
                break;
            case SQLItem.Comparator.LESS:
                if(bound==-1) bound++;
                if(bound<keys.length-1 && ((Comparable)key).compareTo(keys[bound+1])==0) bound++;
                if(bound<keys.length-1) yes.set(bound+1, keys.length, true);
                break;
            case SQLItem.Comparator.LESSE:
                if(bound==-1 || ((Comparable)key).compareTo(keys[bound])!=0) bound++;
                if(bound<keys.length) yes.set(bound, keys.length, true);
                break;
        }
        return yes;
    }
    
    private static int findBound(Comparable d, int op, Object[] data) {
        int left = 0;
        int right = data.length - 1;
        int result = -1;
        
        while(left <= right) {
            int mid = left + (right - left) / 2;
            switch(op){
                case SQLItem.Comparator.GREATER:
                    if(d.compareTo(data[mid])<0){
                        result = mid;
                        right = mid - 1;
                    }else{
                        left = mid + 1;
                    }
                    break;
                case SQLItem.Comparator.GREATERE:
                    if(d.compareTo(data[mid])<=0){
                        result = mid;
                        right = mid - 1;
                    }else{
                        left = mid + 1;
                    }
                    break;
                case SQLItem.Comparator.LESS:
                    if(d.compareTo(data[mid])>0){
                        result = mid;
                        left = mid + 1;
                    }else{
                        right = mid - 1;
                    }
                    break;
                case SQLItem.Comparator.LESSE:
                    if(d.compareTo(data[mid])>=0){
                        result = mid;
                        left = mid + 1;
                    }else{
                        right = mid - 1;
                    }
                    break;
            }
        }
        return result;
    }
}