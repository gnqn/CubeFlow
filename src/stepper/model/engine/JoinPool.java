package stepper.model.engine;

import java.util.*;
import stepper.util.*;
import stepper.model.engine.flows.*;

public class JoinPool extends Pool{
    protected int[][] op;
    protected int[] nums;       //cols, ncols, scols, cc1, cc2, gcols1, gcols2 
    protected int[][] x2x;
    protected int[][] sxx;
    protected int[][][] sx2x;
    
    //cols: cols, cols1, cols2, ncols, ncols1, ncols2, scols, scols1, scols2, cc1, frees1, cc2, frees2
    protected int[] gpos;
    protected long[] gspans;
    protected int cc2, frees2;
    protected int gcols1, gcols2;
    protected final BitSet[] marks;
    
    public JoinPool(Action act, int[] nums, int[] cols, int blockings, int[][] op, int[][] x2x, int[][] sxx, int[][][] sx2x){
        super(act, cols, blockings);
        this.op = op;
        this.x2x = x2x;
        this.sxx = sxx;
        this.sx2x = sx2x;
        this.nums = nums;
        
        Cube cube = act.cube, in = act.input.cube, in2 = act.input2().cube;
        marks = new BitSet[cube.dims.length];
        for(int i=0; i<cube.dims.length; i++) marks[i] = new BitSet(cube.dims[i].length==0 ? cube.g : cube.dims[i].length);
        
        cc2 = nums[0]*6+nums[3]*2;
        frees2 = cc2 + nums[4];
        gcols1 = nums[0]*6 + nums[3]*2 + nums[4]*2; 
        gcols2 = gcols1 + nums[5];
        gpos = new int[nums[5]+nums[6]];
        gspans = Cube.spansOf(sizesG(in, in2));
        if(blockings>=cube.schema.size() && (nums[4]!=0 || nums[5]!=0 || nums[6]!=0)) allocate();
    }
    
    final void allocate(){
        Cube cube = act.cube, in = act.input.cube, in2 = act.input2().cube;
        if(cube instanceof SparseCube){
            int size = nums[3]==0 && nums[4]==0 ? Math.min(in.cardinality(), in2.cardinality()) : Math.max(in.cardinality(), in2.cardinality());
            if(blockings>=cube.schema.size()) size = size<1000000 ? 1000000 : size;
            cube.allocate(size<100000 ? 100000 : size);
        }else{
            cube.allocate((int)cube.sizeS());
        }
    }
    
    @Override
    public void execute(){
        Cube cube = act.cube, in = act.input.cube;
        ArrayCube in2 = (ArrayCube)act.input2().cube;
        
        if(nums[2]==0){
            if(in2.arityS()==0){
                cube.cascade(in);
                if(!(cube instanceof SparseCube)) cube.bits = in.bits;
                cube.cascade(op, in2.values);
                return;
            }else if(nums[4]==0 && nums[5]==0 && nums[6]==0) cascade();
            else if(nums[4]==0 && nums[6]==0) valueValue(null);
            else cubeCube();
        }else{
            if(nums[4]==0 && nums[6]==0) valueValueL();
            
        }
        setMarks();
    }
    
    protected void setMarks(){
        act.cube.marks = marks;
        for(int i=0; i<act.cube.dims.length; i++) if(act.cube.marks[i]!=null && act.cube.marks[i].cardinality()==act.cube.dims[i].length) act.cube.marks[i]=null;
    }
    
    public void cascade(){
        ArrayCube in = (ArrayCube)act.input.cube, in2 = (ArrayCube)act.input2().cube;
        
        act.cube.cascade(in);
        //if(in2.values==in.values) in2.values = in2.values.clone();
        if(in instanceof SparseCube && in2 instanceof SparseCube) cascade((SparseCube)in, (SparseCube)in2);
        else if(in instanceof ArrayCube && in2 instanceof SparseCube) cascade(in, (SparseCube)in2);
        else if(in instanceof SparseCube && in2 instanceof ArrayCube) cascade((SparseCube)in, in2);
        else cascade(in, in2);
    }
    
    protected void valueValue(ActionFlow flow){
        ArrayCube in = (ArrayCube)act.input.cube, in2 = (ArrayCube)act.input2().cube;
        
        if(in instanceof SparseCube && in2 instanceof SparseCube) valueValue(flow, (SparseCube)in, (SparseCube)in2);
        else if(in instanceof ArrayCube && in2 instanceof SparseCube) valueValue(flow, in, (SparseCube)in2);
        else if(in instanceof SparseCube && in2 instanceof ArrayCube) valueValue(flow, (SparseCube)in, in2);
        else valueValue(flow, in, in2);
    }
    
    protected void cubeCube(){
        Cube in = act.input.cube, in2 = act.input2().cube;
        
        int[] cols2 = new int[nums[0]];
        System.arraycopy(cols, nums[0]*2, cols2, 0, nums[0]);
        //if(in2 instanceof SparseCube && prefixing(cols2)){
        //    if(in instanceof SparseCube) product((SparseCube)in, (SparseCube)in2);
        //    else product((ArrayCube)in, (SparseCube)in2);
        //    return;
        //}
        
        int[] cols22 = in2.dimensionsS(cols2);
        if(act.ordered()){
            Pool c2 = act.input2().pool;
            Snapshop snap = new Snapshop(c2.estimate(cols22, 10000), cols22.length);
            if(in instanceof SparseCube) cubeSnap((SparseCube)in, in2, snap);
            else cubeSnap((ArrayCube)in, in2, snap);
        }else{
            long cost = System.currentTimeMillis();
            IntHashSlices slices = act.input2().pool.slicing(cols2, cols22);
            System.out.println("Slice Cost:" + (System.currentTimeMillis() - cost) + "ms");
        
            if(slices==null){
                long loc2 = 0;
                Pool c2 = act.input2().pool;
                for(int k: cols22) loc2 += in2.marks[k].nextSetBit(0) * c2.spans[k];
                if(in instanceof SparseCube) cubeValue((SparseCube)in, (ArrayCube)in2, loc2);
                else cubeValue((ArrayCube)in, (ArrayCube)in2, loc2);
            }else{
                long[] spans2 = in2.spansS(cols2);
                if(in instanceof SparseCube && in2 instanceof SparseCube) cubeCube((SparseCube)in, (SparseCube)in2, spans2, slices);
                else if(in instanceof SparseCube && in2 instanceof ArrayCube) cubeCube((SparseCube)in, (ArrayCube)in2, spans2, slices);
                else if(in instanceof ArrayCube && in2 instanceof SparseCube) cubeCube((ArrayCube)in, (SparseCube)in2, spans2, slices);
                else cubeCube((ArrayCube)in, (ArrayCube)in2, spans2, slices);
            }
        }
    }
    
    protected void valueValueL(){
        boolean yes = true;
        ArrayCube cube = (ArrayCube)act.cube;
        Cube in = act.input.cube, in2 = act.input2().cube;
        long cost = System.currentTimeMillis();
        if(in2 instanceof SparseCube) ((SparseCube)in2).longs.makingKeyMap();
        System.out.println("Hashing: " + (System.currentTimeMillis()-cost) + "ms");
        
        int[] xpos = new int[nums[2]];
        int[][] xx2x = new int[nums[2]][];
        
        int ncols = nums[0]*3, ncols1 = ncols + nums[1], ncols2 = ncols1 + nums[1];
        int scols = nums[0]*3 + nums[1]*3, scols1 = scols + nums[2], scols2 = scols1 + nums[2];
        
        double p = in.cardinality() * 1.0;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        for(long r=0, i=in.nextBit(0, r); i>=0; i=in.nextBit(i+1, ++r), yes = true, c2.loc=0){
            if(in instanceof SparseCube && in.bits!=null && !in.bits.get((int)r)) continue;
            if(cd!=null && !yes(in instanceof SparseCube ? (int)r : (int)i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            long batch = 1;
            for(int k=0; k<nums[2]; k++){
                xpos[k] = sxx[k][c1.pos[cols[scols1+k]]];
                xx2x[k] = sx2x[k][c1.pos[cols[scols1+k]]];
                batch *= xx2x[k].length;
            }
            
            for(int k=0; k<nums[1]; k++) pos[cols[ncols+k]] = c1.pos[cols[ncols1+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[1]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[ncols1+k]] : x2x[k][c1.pos[cols[ncols1+k]]]) * c2.spans[cols[ncols2+k]];
            for(long m=0, loc2=c2.loc; m<batch; m++, loc2=c2.loc){
                for(int k=0; k<nums[2]; k++) loc2 += xx2x[k][xpos[k]] * c2.spans[cols[scols2+k]];
                if(in2.getBit(loc2)){
                    for(int k=0; k<nums[2]; k++) pos[cols[scols+k]] = xpos[k];
                    if(cube.g!=1) pos[cube.arity()-1] = (int)cube.location(gpos, gspans);
                    for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
                    
                    //cube.compute((r+1)/p, cube.location(pos, spans), in, in instanceof SparseCube ? (int)r : (int)i, op, in2, loc2);
                }
                int k = nums[2] - 1;
                while(k>=0 && ++xpos[k]==xx2x[k].length) xpos[k--] = sxx[k][c1.pos[cols[scols1+k]]];
            }
        }
    }
    
    private void cascade(SparseCube in, SparseCube in2){
        boolean yes = true;
        in2.longs.makingKeyMap();
        ArrayCube cube = (ArrayCube)act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        if(in.longs.idxes==null) for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=0){
            in.coordinates(c1.pos, in.longs.data[i], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]*2+k]];
            if(!in2.getBit(c2.loc)) continue;
            
            for(int k=0; k<nums[0]; k++) marks[cols[k]].set(c1.pos[cols[nums[0]+k]]);
            for(int k=0; k<nums[3]; k++) marks[cols[cc1+k]].set(c1.pos[cols[frees1+k]]);
            
            cube.setMeasure(i, in, i, op, in2, c2.loc);
        } else for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=0){
            in.coordinates(c1.pos, in.longs.data[in.longs.idxes[i]], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]*2+k]];
            if(!in2.getBit(c2.loc)) continue;
            
            for(int k=0; k<nums[0]; k++) marks[cols[k]].set(c1.pos[cols[nums[0]+k]]);
            for(int k=0; k<nums[3]; k++) marks[cols[cc1+k]].set(c1.pos[cols[frees1+k]]);
            
            cube.setMeasure(i, in, in.longs.idxes[i], op, in2, c2.loc);
        }
    }
    
    private void cascade(ArrayCube in, SparseCube in2){
        boolean yes = true;
        in2.longs.makingKeyMap();
        ArrayCube cube = (ArrayCube)act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        for(int i=in.bits.nextSetBit(0); i>=0; i=in.bits.nextSetBit(i+1), yes = true, c2.loc=0){
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]*2+k]];
            if(!in2.getBit(c2.loc)) continue;
            
            for(int k=0; k<nums[0]; k++) marks[cols[k]].set(c1.pos[cols[nums[0]+k]]);
            for(int k=0; k<nums[3]; k++) marks[cols[cc1+k]].set(c1.pos[cols[frees1+k]]);
            
            cube.setMeasure(i, in, i, op, in2, c2.loc);
        }
    }
    
    private void cascade(SparseCube in, ArrayCube in2){
        boolean yes = true;
        ArrayCube cube = (ArrayCube)act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        if(in.longs.idxes==null) for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=0){
            in.coordinates(c1.pos, in.longs.data[i], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]*2+k]];
            if(!in2.getBit(c2.loc)) continue;
            
            for(int k=0; k<nums[0]; k++) marks[cols[k]].set(c1.pos[cols[nums[0]+k]]);
            for(int k=0; k<nums[3]; k++) marks[cols[cc1+k]].set(c1.pos[cols[frees1+k]]);
            
            cube.setMeasure(i, in, i, op, in2, (int)c2.loc);
        } else for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=0){
            in.coordinates(c1.pos, in.longs.data[in.longs.idxes[i]], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]*2+k]];
            if(!in2.getBit(c2.loc)) continue;
            
            for(int k=0; k<nums[0]; k++) marks[cols[k]].set(c1.pos[cols[nums[0]+k]]);
            for(int k=0; k<nums[3]; k++) marks[cols[cc1+k]].set(c1.pos[cols[frees1+k]]);
            
            cube.setMeasure(i, in, in.longs.idxes[i], op, in2, (int)c2.loc);
        }
    }
    
    private void cascade(ArrayCube in, ArrayCube in2){
        boolean yes = true;
        ArrayCube cube = (ArrayCube)act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        for(int i=in.bits.nextSetBit(0); i>=0; i=in.bits.nextSetBit(i+1), yes = true, c2.loc=0){
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]*2+k]];
            if(!in2.getBit(c2.loc)) continue;
            
            for(int k=0; k<nums[0]; k++) marks[cols[k]].set(c1.pos[cols[nums[0]+k]]);
            for(int k=0; k<nums[3]; k++) marks[cols[cc1+k]].set(c1.pos[cols[frees1+k]]);
            
            cube.setMeasure(i, in, i, op, in2, (int)c2.loc);
        }
    }
    
    private void valueValue(ActionFlow flow, SparseCube in, SparseCube in2){
        in2.longs.makingKeyMap();
        
        boolean yes = true;
        ArrayCube cube = (ArrayCube)act.cube;
        double p = in.cardinality() * 1.0;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        if(in.longs.idxes==null) for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=0){
            if(in.bits!=null && !in.bits.get(i)) continue;
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, in.longs.data[i], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]*2+k]];
            if(in2.getBit(c2.loc)){
                for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
                for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
                for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
                if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
                for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
                
                if(flow!=null) compute(flow, 0, (i+1)/p, in, i, op, in2, in2.longs.rank(c2.loc));
                else cube.compute((i+1)/p, cube.location(pos, spans), in, i, op, in2, in2.longs.rank(c2.loc));
            }
        } else for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=0){
            if(in.bits!=null && !in.bits.get(in.longs.idxes[i])) continue;
            if(cd!=null && !yes(in.longs.idxes[i])) continue;
            in.coordinates(c1.pos, in.longs.data[in.longs.idxes[i]], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]*2+k]];
            if(in2.getBit(c2.loc)){
                for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
                for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
                for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
                if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
                for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
                
                if(flow!=null) compute(flow, 0, (i+1)/p, in, in.longs.idxes[i], op, in2, in2.longs.rank(c2.loc));
                else cube.compute((i+1)/p, cube.location(pos, spans), in, in.longs.idxes[i], op, in2, in2.longs.rank(c2.loc));
            }
        }
    }
    
    private void valueValue(ActionFlow flow, ArrayCube in, SparseCube in2){
        in2.longs.makingKeyMap();
        
        boolean yes = true;
        ArrayCube cube = (ArrayCube)act.cube;
        double p = in.cardinality() * 1.0;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        for(int r=1, i=in.bits.nextSetBit(0); i>=0; i=in.bits.nextSetBit(i+1), r++, yes = true, c2.loc=0){
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]*2+k]];
            if(in2.getBit(c2.loc)){
                for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
                for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
                for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
                if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
                for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
                
                if(flow!=null) compute(flow, 0, r/p, in, i, op, in2, in2.longs.rank(c2.loc));
                else cube.compute(r/p, cube.location(pos, spans), in, i, op, in2, in2.longs.rank(c2.loc));
            }
        }
    }
    
    private void valueValue(ActionFlow flow, SparseCube in, ArrayCube in2){
        boolean yes = true;
        ArrayCube cube = (ArrayCube)act.cube;
        double p = in.cardinality() * 1.0;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        if(in.longs.idxes==null) for(int r=1, i=0; i<in.longs.size; i++, r++, yes = true, c2.loc=0){
            if(in.bits!=null && !in.bits.get(i)) continue;
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, in.longs.data[i], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]*2+k]];
            if(in2.getBit(c2.loc)){
                for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
                for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
                for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
                if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
                for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
                
                if(flow!=null) compute(flow, 0, (r+1)/p, in, i, op, in2, (int)c2.loc);
                else cube.compute((r+1)/p, cube.location(pos, spans), in, i, op, in2, (int)c2.loc);
            }
        } else for(int r=1, i=0; i<in.longs.size; i++, r++, yes = true, c2.loc=0){
            if(in.bits!=null && !in.bits.get(in.longs.idxes[i])) continue;
            if(cd!=null && !yes(in.longs.idxes[i])) continue;
            in.coordinates(c1.pos, in.longs.data[in.longs.idxes[i]], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]*2+k]];
            if(in2.getBit(c2.loc)){
                for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
                for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
                for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
                if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
                for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
                
                if(flow!=null) compute(flow, 0, (r+1)/p, in, in.longs.idxes[i], op, in2, (int)c2.loc);
                else cube.compute((r+1)/p, cube.location(pos, spans), in, in.longs.idxes[i], op, in2, (int)c2.loc);
            }
        }
    }
    
    private void valueValue(ActionFlow flow, ArrayCube in, ArrayCube in2){
        boolean yes = true;
        ArrayCube cube = (ArrayCube)act.cube;
        double p = in.cardinality() * 1.0;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        for(int r=1, i=in.bits.nextSetBit(0); i>=0; i=in.bits.nextSetBit(i+1), r++, yes = true, c2.loc=0){
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]*2+k]];
            if(in2.getBit(c2.loc)){
                for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
                for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
                for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
                if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
                for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
                
                if(flow!=null) compute(flow, 0, r/p, in, i, op, in2, (int)c2.loc);
                else cube.compute(r/p, cube.location(pos, spans), in, i, op, in2, (int)c2.loc);
            }
        }
    }
    
    protected void cubeValue(SparseCube in, ArrayCube in2, long loc2){
        boolean yes = true;
        double p = in.cardinality() * 1.0;
        ArrayCube cube = (ArrayCube)act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        c2.loc = loc2;
        if(in.longs.idxes==null) for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=loc2){
            if(in.bits!=null && !in.bits.get(i)) continue;
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, in.longs.data[i], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]+k]];
            
            if(!in2.getBit(c2.loc)) continue;
            for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
            cube.compute((i+1)/p, cube.location(pos, spans), in, i, op, in2, (int)c2.loc);
        } else for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=loc2){
            if(in.bits!=null && !in.bits.get(in.longs.idxes[i])) continue;
            if(cd!=null && !yes(in.longs.idxes[i])) continue;
            in.coordinates(c1.pos, in.longs.data[in.longs.idxes[i]], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]+k]];
            
            if(!in2.getBit(c2.loc)) continue;
            for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
            cube.compute((i+1)/p, cube.location(pos, spans), in, in.longs.idxes[i], op, in2, (int)c2.loc);
        }
    }
    
    protected void cubeValue(ArrayCube in, ArrayCube in2, long loc2){
        boolean yes = true;
        double p = in.cardinality() * 1.0;
        ArrayCube cube = (ArrayCube)act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        c2.loc = loc2;
        for(int r=1, i=in.bits.nextSetBit(0); i>=0; i=in.bits.nextSetBit(i+1), r++, yes = true, c2.loc=loc2){
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]+k]];
            
            if(!in2.getBit(c2.loc)) continue;
            for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
            cube.compute(r/p, cube.location(pos, spans), in, i, op, in2, (int)c2.loc);
        }
    }
    
    protected void cubeSnap(SparseCube in, Cube in2, Snapshop snap){
        long loc2 = -1;
        boolean yes = true;
        Cube cube = act.cube;
        double p = in.cardinality() * 1.0;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        if(in.longs.idxes==null) for(int i=0, i2=0; i<in.longs.size; i++, yes = true, c2.loc=0){
            if(in.bits!=null && !in.bits.get(i)) continue;
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, in.longs.data[i], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[k];
            if(c2.loc!=loc2) i2 = in2.snapshop(i2, nums[0], snap, c2);
            loc2 = c2.loc;
            product((i+1)/p, i, snap, in2);
        } else for(int i=0, i2=0; i<in.longs.size; i++, yes = true, c2.loc=0){
            if(in.bits!=null && !in.bits.get(in.longs.idxes[i])) continue;
            if(cd!=null && !yes(in.longs.idxes[i])) continue;
            in.coordinates(c1.pos, in.longs.data[in.longs.idxes[i]], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[k];
            if(c2.loc!=loc2) i2 = in2.snapshop(i2, nums[0], snap, c2);
            loc2 = c2.loc;
            product((i+1)/p, in.longs.idxes[i], snap, in2);
        }
    }
    
    protected void cubeSnap(ArrayCube in, Cube in2, Snapshop snap){
        long loc2 = -1;
        boolean yes = true;
        Cube cube = act.cube;
        double p = in.cardinality() * 1.0;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        for(int r=1, i=in.bits.nextSetBit(0), i2=0; i>=0; i=in.bits.nextSetBit(i+1), r++, yes = true, c2.loc=0){
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[k];
            if(c2.loc!=loc2) i2 = in2.snapshop(i2, nums[0], snap, c2);
            loc2 = c2.loc;
            product(r/p, i, snap, in2);
        }
    }
    
    protected void cubeCube(SparseCube in, SparseCube in2, long[] spans2, IntHashSlices slices){
        boolean yes = true;
        Cube cube = act.cube;
        double p = in.cardinality() * 1.0;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        if(in.longs.idxes==null) for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=0){
            if(in.bits!=null && !in.bits.get(i)) continue;
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, in.longs.data[i], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
            product((i+1)/p, i, slices.get((int)c2.loc), in2);
        } else for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=0){
            if(in.bits!=null && !in.bits.get(in.longs.idxes[i])) continue;
            if(cd!=null && !yes(in.longs.idxes[i])) continue;
            in.coordinates(c1.pos, in.longs.data[in.longs.idxes[i]], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
            product((i+1)/p, in.longs.idxes[i], slices.get((int)c2.loc), in2);
        }
    }
    
    protected void cubeCube(SparseCube in, ArrayCube in2, long[] spans2, IntHashSlices slices){
        boolean yes = true;
        Cube cube = act.cube;
        double p = in.cardinality() * 1.0;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        if(in.longs.idxes==null) for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=0){
            if(in.bits!=null && !in.bits.get(i)) continue;
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, in.longs.data[i], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
            product((i+1)/p, i, slices.get((int)c2.loc), in2);
        } else for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=0){
            if(in.bits!=null && !in.bits.get(in.longs.idxes[i])) continue;
            if(cd!=null && !yes(in.longs.idxes[i])) continue;
            in.coordinates(c1.pos, in.longs.data[in.longs.idxes[i]], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
            product((i+1)/p, in.longs.idxes[i], slices.get((int)c2.loc), in2);
        }
    }
    
    protected void cubeCube(ArrayCube in, SparseCube in2, long[] spans2, IntHashSlices slices){
        boolean yes = true;
        Cube cube = act.cube;
        double p = in.cardinality() * 1.0;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        for(int r=1, i=in.bits.nextSetBit(0); i>=0; i=in.bits.nextSetBit(i+1), r++, yes = true, c2.loc=0){
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
            product(r/p, i, slices.get((int)c2.loc), in2);
        }
    }
    
    protected void cubeCube(ArrayCube in, ArrayCube in2, long[] spans2, IntHashSlices slices){
        boolean yes = true;
        Cube cube = act.cube;
        double p = in.cardinality() * 1.0;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        for(int r=1, i=in.bits.nextSetBit(0); i>=0; i=in.bits.nextSetBit(i+1), r++, yes = true, c2.loc=0){
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
            product(r/p, i, slices.get((int)c2.loc), in2);
        }
    }
    
    private void product(double p, int r, Ints slice, SparseCube in2){
        if(slice==null) return;
        
        Pool c2 = act.input2().pool;
        ArrayCube cube = (ArrayCube)act.cube;
        for(int i: slice.toArray()){
            boolean yes = true;
            in2.coordinates(c2.pos, in2.longs.data[i], c2.spans);
            for(int k=0; yes && k<nums[6]; k++) yes &= in2.marked(cols[gcols2+k], c2.pos[cols[gcols2+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[6]; k++) gpos[nums[5]+k] = c2.pos[cols[gcols2+k]];
            for(int k=0; k<nums[4]; k++) pos[cols[cc2+k]] = c2.pos[cols[frees2+k]];
            if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
            for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
            cube.compute(p, cube.location(pos, spans), act.input.cube, r, op, in2, i);
        }
    }
    
    private void product(double p, int r, Ints slice, ArrayCube in2){
        if(slice==null) return;
        
        Pool c2 = act.input2().pool;
        ArrayCube cube = (ArrayCube)act.cube;
        for(int i: slice.toArray()){
            boolean yes = true;
            in2.coordinates(c2.pos, i, c2.spans);
            for(int k=0; yes && k<nums[6]; k++) yes &= in2.marked(cols[gcols2+k], c2.pos[cols[gcols2+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[6]; k++) gpos[nums[5]+k] = c2.pos[cols[gcols2+k]];
            for(int k=0; k<nums[4]; k++) pos[cols[cc2+k]] = c2.pos[cols[frees2+k]];
            if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
            for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
            cube.compute(p, cube.location(pos, spans), act.input.cube, r, op, in2, i);
        }
    }
    
    private void product(double p, int r, Snapshop snap, Cube in2){
        ArrayCube cube = (ArrayCube)act.cube;
        for(int i=0; i<snap.size; i++){
            for(int k=0; k<nums[6]; k++) gpos[nums[5]+k] = snap.pos[i][cols[gcols2+k]-nums[0]];
            for(int k=0; k<nums[4]; k++) pos[cols[cc2+k]] = snap.pos[i][cols[frees2+k]-nums[0]];
            if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
            for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
            cube.compute(p, cube.location(pos, spans), act.input.cube, r, op, in2, snap.loc[i]);
        }
    }
    
    private void product(SparseCube in, SparseCube in2){
        boolean yes = true;
        double p = in.cardinality() * 1.0;
        ArrayCube cube = (ArrayCube)act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        
        int m = 0, r2 = 0;
        int[] pos2 = new int[nums[0]];
        for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=0, m=0){
            if(in.bits!=null && !in.bits.get(i)) continue;
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, in.longs.data[i], c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            
            for(int k=0; k<nums[0]; k++) pos2[k] = x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]];
            for(int k=0; k<nums[0]; k++) c2.loc += pos2[k] * c2.spans[cols[nums[0]*2+k]];
            
            while(r2<in2.longs.size && in2.longs.data[r2]<c2.loc) r2++;
            while(r2+m<in2.longs.size){
                in2.coordinates(c2.pos, in2.longs.data[r2+m], c2.spans);
                if(changed(c2.pos, pos2)) break;
                for(int k=0; k<nums[6]; k++) gpos[nums[5]+k] = c2.pos[cols[gcols2+k]];
                for(int k=0; k<nums[4]; k++) pos[cols[cc2+k]] = c2.pos[cols[frees2+k]];
                if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
                for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
                cube.compute((i+1)/p, cube.location(pos, spans), in, i, op, in2, r2+m++);
            }
        }
    }
    
    private void product(ArrayCube in, SparseCube in2){
        boolean yes = true;
        double p = in.cardinality() * 1.0;
        ArrayCube cube = (ArrayCube)act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        
        int m = 0, r2 = 0;
        int[] pos2 = new int[nums[0]];
        for(int r=1, i=in.bits.nextSetBit(0); i>=0; i=in.bits.nextSetBit(i+1), r++, yes = true, c2.loc=0, m=0){
            if(cd!=null && !yes(i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            
            for(int k=0; k<nums[0]; k++) pos2[k] = x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]];
            for(int k=0; k<nums[0]; k++) c2.loc += pos2[k] * c2.spans[cols[nums[0]*2+k]];
            
            while(r2<in2.longs.size && in2.longs.data[r2]<c2.loc) r2++;
            while(r2+m<in2.longs.size){
                in2.coordinates(c2.pos, in2.longs.data[r2], c2.spans);
                if(changed(c2.pos, pos2)) break;
                for(int k=0; k<nums[6]; k++) gpos[nums[5]+k] = c2.pos[cols[gcols2+k]];
                for(int k=0; k<nums[4]; k++) pos[cols[cc2+k]] = c2.pos[cols[frees2+k]];
                if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
                for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
                cube.compute(r/p, cube.location(pos, spans), in, r, op, in2, r2+m++);
            }
        }
    }
    
    protected boolean prefixing(int[] cols2){
        for(int i=0; i<cols2.length; i++) if(cols2[i]!=i) return false;
        return true;
    }
    
    protected boolean changed(int[] pos1, int[] pos2){
        for(int k=0; k<nums[0]; k++) if(pos1[k]!=pos2[k]) return true;
        return false;
    }
    
    protected final int[] sizesG(Cube in, Cube in2){
        int n = nums[5] + nums[6];
        int[] sizes = new int[n];
        for(int i=0; i<nums[5]; i++) sizes[i] = in.marks[cols[gcols1+i]]==null ? in.dims[cols[gcols1+i]].length : in.marks[cols[gcols1+i]].cardinality();
        for(int i=0; i<nums[6]; i++) sizes[nums[5]+i] = in2.marks[cols[gcols2+i]]==null ? in2.dims[cols[gcols2+i]].length : in2.marks[cols[gcols2+i]].cardinality();
        return sizes;
    }
    
    /*
    public void cascade(){
        ArrayCube cube = (ArrayCube)act.cube;
        ArrayCube in = (ArrayCube)act.input.cube, in2 = (ArrayCube)act.input2().cube;
        
        cube.cascade(in);
        if(in2.values==in.values) in2.values = in2.values.clone();
        if(in2 instanceof SparseCube) ((SparseCube)in2).longs.makingKeyMap();
        
        boolean yes = true;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        for(long r=0, i=in.nextBit(0, r); i>=0; i=in.nextBit(i+1, ++r), yes = true, c2.loc=0){
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]*2+k]];
            if(!in2.getBit(c2.loc)) continue;
            
            for(int k=0; k<nums[0]; k++) marks[cols[k]].set(c1.pos[cols[nums[0]+k]]);
            for(int k=0; k<nums[3]; k++) marks[cols[cc1+k]].set(c1.pos[cols[frees1+k]]);
            
            cube.setMeasure(in instanceof SparseCube ? (int)r : (int)i, in, in instanceof SparseCube ? (int)r : (int)i, op, in2, c2.loc);
        }
    }
    
    protected void valueValue(ActionFlow flow){
        ArrayCube cube = (ArrayCube)act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        Cube in = act.input.cube, in2 = act.input2().cube;
        
        boolean yes = true;
        double p = in.cardinality() * 1.0;
        if(in2 instanceof SparseCube) ((SparseCube)in2).longs.makingKeyMap();
        for(long r=0, i=in.nextBit(0, r); i>=0; i=in.nextBit(i+1, ++r), yes = true, c2.loc=0){
            if(in instanceof SparseCube && in.bits!=null && !in.bits.get((int)r)) continue;
            if(cd!=null && !yes(in instanceof SparseCube ? (int)r : (int)i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * c2.spans[cols[nums[0]*2+k]];
            if(in2.getBit(c2.loc)){
                for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
                for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
                for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
                if(cube.g!=1) pos[cube.arity()-1] = (int)cube.location(gpos, gspans);
                for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
                
                if(flow!=null) compute(flow, 0, (r+1)/p, in, in instanceof SparseCube ? (int)r : (int)i, op, in2, c2.loc);
                else cube.compute((r+1)/p, cube.location(pos, spans), in, in instanceof SparseCube ? (int)r : (int)i, op, in2, c2.loc);
            }
        }
    }
    
    private void cubeCube(){
        boolean yes = true;
        Cube cube = act.cube, in = act.input.cube, in2 = act.input2().cube;
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        
        int[] cols2 = new int[nums[0]];
        System.arraycopy(cols, nums[0]*2, cols2, 0, nums[0]);
        long[] spans2 = in2.spansS(cols2);
        
        long cost = System.currentTimeMillis();
        IntHashSlices slices = c2.slicing(nums[0], cols);
        System.out.println("Slice Cost:" + (System.currentTimeMillis() - cost) + "ms");
        
        //long batch = cube.markedSize(cc2);
        double p = in.cardinality() * 1.0;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        for(long r=0, i=in.nextBit(0, r); i>=0; i=in.nextBit(i+1, ++r), yes = true, c2.loc=0){
            if(in instanceof SparseCube && in.bits!=null && !in.bits.get((int)r)) continue;
            if(cd!=null && !yes(in instanceof SparseCube ? (int)r : (int)i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            BitSet[] neq_joins = joinby();
            if(this instanceof JoinNeqPool && neq_joins==null) continue;
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
            product((r+1)/p, in instanceof SparseCube ? (int)r : (int)i, neq_joins, slices.get((int)c2.loc));
        }
    }
    
    private void product(double p, int r, BitSet[] neq_joins, Ints slice){
        if(slice==null) return;
        
        Pool c2 = act.input2().pool;
        ArrayCube cube = (ArrayCube)act.cube;
        Cube in = act.input.cube, in2 = act.input2().cube;
        JoinNeqPool neq = this instanceof JoinNeqPool ? (JoinNeqPool)this : null;
        for(int i: slice.toArray()){
            boolean yes = true;
            in2.coordinates(c2.pos, in2 instanceof SparseCube ? ((SparseCube)in2).longs.data[i] : i, c2.spans);
            for(int k=0; yes && k<nums[6]; k++) yes &= in2.marked(cols[gcols2+k], c2.pos[cols[gcols2+k]]);
            for(int k=0; neq!=null && yes && k<neq_joins.length; k++) yes &= neq_joins[k].get(neq.pos2pos[k][c2.pos[neq.neq_cols[k]]]);
            if(!yes) continue;
            
            for(int k=0; k<nums[6]; k++) gpos[k] = c2.pos[cols[gcols2+k]];
            for(int k=0; k<nums[4]; k++) pos[cols[cc2+k]] = c2.pos[cols[frees2+k]];
            if(cube.g!=1) pos[cube.arity()-1] = (int)cube.location(gpos, gspans);
            for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
            
            cube.compute(p, cube.location(pos, spans), in, r, op, in2, i);
        }
    }
    
    protected void valueValueL(){
        boolean yes = true;
        ArrayCube cube = (ArrayCube)act.cube;
        Cube in = act.input.cube, in2 = act.input2().cube;
        long cost = System.currentTimeMillis();
        if(in2 instanceof SparseCube) ((SparseCube)in2).longs.makingKeyMap();
        System.out.println("Hashing: " + (System.currentTimeMillis()-cost) + "ms");
        
        int[] xpos = new int[nums[2]];
        int[][] xx2x = new int[nums[2]][];
        
        int ncols = nums[0]*3, ncols1 = ncols + nums[1], ncols2 = ncols1 + nums[1];
        int scols = nums[0]*3 + nums[1]*3, scols1 = scols + nums[2], scols2 = scols1 + nums[2];
        
        double p = in.cardinality() * 1.0;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        for(long r=0, i=in.nextBit(0, r); i>=0; i=in.nextBit(i+1, ++r), yes = true, c2.loc=0){
            if(in instanceof SparseCube && in.bits!=null && !in.bits.get((int)r)) continue;
            if(cd!=null && !yes(in instanceof SparseCube ? (int)r : (int)i)) continue;
            in.coordinates(c1.pos, i, c1.spans);
            for(int k=0; yes && k<nums[0]; k++) yes &= cube.marked(cols[k], c1.pos[cols[nums[0]+k]]);
            for(int k=0; yes && k<nums[3]; k++) yes &= cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]]);
            for(int k=0; yes && k<nums[5]; k++) yes &= in.marked(cols[gcols1+k], c1.pos[cols[gcols1+k]]);
            if(!yes) continue;
            
            long batch = 1;
            for(int k=0; k<nums[2]; k++){
                xpos[k] = sxx[k][c1.pos[cols[scols1+k]]];
                xx2x[k] = sx2x[k][c1.pos[cols[scols1+k]]];
                batch *= xx2x[k].length;
            }
            
            for(int k=0; k<nums[1]; k++) pos[cols[ncols+k]] = c1.pos[cols[ncols1+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[1]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[ncols1+k]] : x2x[k][c1.pos[cols[ncols1+k]]]) * c2.spans[cols[ncols2+k]];
            for(long m=0, loc2=c2.loc; m<batch; m++, loc2=c2.loc){
                for(int k=0; k<nums[2]; k++) loc2 += xx2x[k][xpos[k]] * c2.spans[cols[scols2+k]];
                if(in2.getBit(loc2)){
                    for(int k=0; k<nums[2]; k++) pos[cols[scols+k]] = xpos[k];
                    if(cube.g!=1) pos[cube.arity()-1] = (int)cube.location(gpos, gspans);
                    for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
                    
                    cube.compute((r+1)/p, cube.location(pos, spans), in, in instanceof SparseCube ? (int)r : (int)i, op, in2, loc2);
                }
                int k = nums[2] - 1;
                while(k>=0 && ++xpos[k]==xx2x[k].length) xpos[k--] = sxx[k][c1.pos[cols[scols1+k]]];
            }
        }
    }
    */
}