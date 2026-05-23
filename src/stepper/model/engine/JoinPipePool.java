package stepper.model.engine;

import stepper.util.*;
import stepper.model.engine.flows.*;

public class JoinPipePool extends JoinPool{
    private final boolean vv;
    private final boolean sparse2;
    protected long[] spans2;
    protected SnapSlices slices;
    private double[] y;
    
    private long loc2;
    private Pack buffer;
    
    public JoinPipePool(Action act, int[] nums, int[] cols, int keys, int[][] op, int[][] x2x, int[][] sxx, int[][][] sx2x){
        super(act, nums, cols, keys, op, x2x, sxx, sx2x);
        
        if(blockings>=act.cube.schema.size() && !act.cube.allocated()) allocate();
        
        Cube in = act.input.cube, in2 = act.input2().cube;
        int[] cols2 = new int[nums[0]];
        System.arraycopy(cols, nums[0]*2, cols2, 0, nums[0]);
        spans2 = in2.spansS(cols2);
        vv = nums[2]==0 && nums[4]==0 && nums[5]==0 && nums[6]==0;
        
        if(keys!=0){
            this.makingCells();
            y = new double[in.measures.length];
        }
        sparse2 = in2 instanceof SparseCube;
    }
    
    @Override
    public void pumping(ActionFlow flow){
        if(nums[2]==0 && nums[4]==0 && nums[6]==0) valueValue(flow);
        else cubeCube(flow);
        setMarks();
        flow.flush(0);
    }
    
    @Override
    public void push(PipeJoin flow, int i, double p){
        loc = 0;
        Cube cube = act.cube;
        Pool c1 = act.input.pool;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        for(int k=0; k<nums[0]; k++) if(!cube.marked(cols[k], c1.pos[cols[nums[0]+k]])) return;
        for(int k=0; k<nums[3]; k++) if(!cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]])) return;
        
        if(blockings==0){
            if(vv) valueValue(flow, i, 1, true);
            else product(flow, i, 1, true);
        }else{
            if(buffer==null) makeBuffer(p);
            if(buffer.notAlign(c1.pos)){loop(flow, i, p, blockings==cube.dims.length); reset();}
            buffer.add(p, c1.pos, flow.y);
        }
    }
    
    @Override
    public void push(ActionFlow flow, int i, double p){
        loc = 0;
        Cube cube = act.cube;
        Pool c1 = act.input.pool;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        for(int k=0; k<nums[0]; k++) if(!cube.marked(cols[k], c1.pos[cols[nums[0]+k]])) return;
        for(int k=0; k<nums[3]; k++) if(!cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]])) return;
        
        if(vv) valueValue(flow, i, 1, blockings==0);
        else product(flow, i, p, blockings==0);
    }
    
    @Override
    public void flush(PipeJoin flow, int i, double p){
        loc = 0;
        Cube cube = act.cube;
        Pool c1 = act.input.pool;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        for(int k=0; k<nums[0]; k++) if(!cube.marked(cols[k], c1.pos[cols[nums[0]+k]])){flush(flow,i); return;}
        for(int k=0; k<nums[3]; k++) if(!cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]])){flush(flow,i); return;}
        
        if(blockings==0){
            if(vv) valueValue(flow, i, 1, true);
            else product(flow, i, 1, true);
        }else{
            if(buffer==null) makeBuffer(p);
            buffer.add(p, c1.pos, flow.y);
            loop(flow, i, 1, blockings==cube.dims.length);
        }
        setMarks();
        flow.flush(i);
    }
    
    @Override
    public void flush(ActionFlow flow, int i, double p){
        loc = 0;
        Cube cube = act.cube;
        Pool c1 = act.input.pool;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        for(int k=0; k<nums[0]; k++) if(!cube.marked(cols[k], c1.pos[cols[nums[0]+k]])){flush(flow,i); return;}
        for(int k=0; k<nums[3]; k++) if(!cube.marked(cols[cc1+k], c1.pos[cols[frees1+k]])){flush(flow,i); return;}
        
        if(vv) valueValue(flow, i, 1, blockings==0);
        else product(flow, i, 1, blockings==0);
        setMarks();
        flow.flush(i);
    }
    
    @Override
    public void flush(PipeJoin flow, int i){
        flush((ActionFlow)flow, i);
    }
    
    @Override
    public void flush(ActionFlow flow, int i){
        if(buffer!=null) loop(flow, i, 1, blockings==act.cube.dims.length);
        //else if(vv) valueValue(flow, i, 1, blockings==0);
        //else product(flow, i, 1, blockings==0);
        setMarks();
        flow.flush(i);
    }
    
    protected void valueValue(ActionFlow flow, int id, double p, boolean stage){
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        ArrayCube cube = (ArrayCube)act.cube, in2 = (ArrayCube)act.input2().cube;
        if(sparse2 && ((SparseCube)in2).longs.loc2idx==null) ((SparseCube)in2).longs.makingKeyMap();
        
        c2.loc = 0;
        for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
        if(in2.getBit(c2.loc)){
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
                
            if(stage) this.compute(flow, id, p, op, in2, sparse2 ? ((SparseCube)in2).longs.rank(c2.loc) : (int)c2.loc);
            else cube.compute(p, cube.location(pos, spans), flow.y, op, in2, sparse2 ? ((SparseCube)in2).longs.rank(c2.loc) : (int)c2.loc);
        }
    }
    
    protected void product(ActionFlow flow, int id, double p, boolean stage){
        Cube in2 = act.input2().cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        
        if(slices==null && loc2==0){
            long cost = System.currentTimeMillis();
            slices = c2.snapping(nums[0], cols);
            System.out.println("Slice Cost:" + (System.currentTimeMillis() - cost) + "ms");
        }
        
        if(slices==null && loc2==0){
            int[] cols2 = new int[nums[0]];
            System.arraycopy(cols, nums[0]*2, cols2, 0, nums[0]);
            int[] cols22 = in2.dimensionsS(cols2);
            for(int k: cols22) loc2 += in2.marks[k].nextSetBit(0) * c2.spans[k];
        }
        
        c2.loc = loc2;
        for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
        for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
        for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
        for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
        if(slices==null) product(flow, id, p, stage, act.input2().cube);
        else product(flow, id, p, stage, slices.get((int)c2.loc), act.input2().cube);
    }
    
    protected void product(ActionFlow flow, int id, double p, boolean stage, Cube in2){
        Pool c2 = act.input2().pool;
        if(!in2.bits.get((int)c2.loc)) return;
        
        ArrayCube cube = (ArrayCube)act.cube;
        for(int k=0; k<nums[6]; k++) gpos[nums[5]+k] = c2.pos[cols[gcols2+k]];
        for(int k=0; k<nums[4]; k++) pos[cols[cc2+k]] = c2.pos[cols[frees2+k]];
        if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
        for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
        
        if(stage) compute(flow, id, p, op, in2, (int)c2.loc);
        else cube.compute(p, cube.location(pos, spans), flow.y, op, in2, (int)c2.loc);
    }
    
    protected void product(ActionFlow flow, int id, double p, boolean stage, Snapshop slice, Cube in2){
        if(slice==null) return;
        
        ArrayCube cube = (ArrayCube)act.cube;
        for(int i=0; i<slice.size; i++){
            for(int k=0; k<nums[6]; k++) gpos[nums[5]+k] = slice.pos[i][cols[gcols2+k]];
            for(int k=0; k<nums[4]; k++) pos[cols[cc2+k]] = slice.pos[i][cols[frees2+k]];
            if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
            for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
            
            if(stage) compute(flow, id, p, op, in2, slice.loc[i]);
            else cube.compute(p, cube.location(pos, spans), flow.y, op, in2, slice.loc[i]);
        }
    }
    
    private void loop(ActionFlow flow, int idx, double p, boolean sink){
        Pool c2 = act.input2().pool;
        if(!sink) System.arraycopy(flow.y, 0, y, 0, y.length);
        for(int k=0; k<nums[0]; k++) pos[k] = buffer.xx[cols[nums[0]+k]][0];
        for(int k=0; k<nums[0]; k++) if(c2.cell.x[cols[nums[0]*2+k]]!=(x2x[k]==null ? pos[cols[k]] : x2x[k][pos[cols[k]]])) return;
        if(c2.cells==null) looping(flow, idx, p, c2.cell, sink, buffer);
        else looping(flow, idx, p, c2.cells, sink, buffer);
        if(!sink) System.arraycopy(y, 0, flow.y, 0, y.length);
    }
    
    protected void looping(ActionFlow flow, int id, double p, Cell cell, boolean sink, Pack buf){
        ArrayCube cube = (ArrayCube)act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        
        for(int i=0; i<buf.size; i++){
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = buf.xx[cols[frees1+k]][i];
            for(int k=0; k<nums[5]; k++) gpos[k] = buf.xx[cols[gcols1+k]][i];
            if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
            for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
            
            if(sink) cube.compute(p, cube.location(pos, spans), buf, i, op, cell.y);
            else this.compute(flow, id, p, buf, i, op, cell.y);
        }
    }
    
    protected void looping(ActionFlow flow, int id, double p, Pack cells, boolean sink, Pack buf){
        ArrayCube cube = (ArrayCube)act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        
        for(int i=0; i<buf.size; i++){
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = buf.xx[cols[frees1+k]][i];
            for(int k=0; k<nums[5]; k++) gpos[k] = buf.xx[cols[gcols1+k]][i];
            for(int m=0; m<cells.size; m++){
                for(int k=0; k<nums[4]; k++) pos[cols[cc2+k]] = cells.xx[cols[frees2+k]][m];
                for(int k=0; k<nums[6]; k++) gpos[nums[5]+k] = cells.xx[cols[gcols2+k]][m];
                if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
                for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
                
                if(sink) cube.compute(p, cube.location(pos, spans), buf, i, op, cells.getY(m));
                else this.compute(flow, id, p, buf, i, op, cells.getY(m));
            }
        }
    }
    
    protected void cubeCube(ActionFlow flow){
        Cube in = act.input.cube, in2 = act.input2().cube;
        
        double p = in.cardinality() * 1.0;
        if(p==0) p = in.size();
        
        int[] cols2 = new int[nums[0]];
        System.arraycopy(cols, nums[0]*2, cols2, 0, nums[0]);
        int[] cols22 = in2.dimensionsS(cols2);
        
        long cost = System.currentTimeMillis();
        slices = act.input2().pool.snapping(cols2, cols22);
        System.out.println("Slice Cost:" + (System.currentTimeMillis() - cost) + "ms");
        
        if(slices==null){
            Pool c2 = act.input2().pool;
            for(int k: cols22) loc2 += in2.marks[k].nextSetBit(0) * c2.spans[k];
            if(in instanceof SparseCube) cubeValue(flow, p, (SparseCube)in, (ArrayCube)in2);
            else cubeValue(flow, p, (ArrayCube)in, (ArrayCube)in2);
        }else{
            if(in instanceof SparseCube && in2 instanceof SparseCube) cubeCube(flow, p, (SparseCube)in, (SparseCube)in2);
            else if(in instanceof SparseCube && in2 instanceof ArrayCube) cubeCube(flow, p, (SparseCube)in, (ArrayCube)in2);
            else if(in instanceof ArrayCube && in2 instanceof SparseCube) cubeCube(flow, p, (ArrayCube)in, (SparseCube)in2);
            else cubeCube(flow, p, (ArrayCube)in, (ArrayCube)in2);
        }
    }
    
    protected void cubeValue(ActionFlow flow, double p, SparseCube in, ArrayCube in2){
        boolean yes = true;
        Cube cube = act.cube;
        int cc1 = nums[0]*6, frees1 = cc1 + nums[3];
        Pool c1 = act.input.pool, c2 = act.input2().pool;
        c2.loc = loc2;
        for(int i=0; i<in.longs.size; i++, yes = true, c2.loc=loc2){
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
            compute(flow, 0, (i+1)/p, in, i, op, in2, (int)c2.loc);
        }
    }
    
    protected void cubeValue(ActionFlow flow, double p, ArrayCube in, ArrayCube in2){
        boolean yes = true;
        Cube cube = act.cube;
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
            compute(flow, 0, r/p, in, i, op, in2, (int)c2.loc);
        }
    }
    
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
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
            product(flow, 0, (i+1)/p, i, slices.get((int)c2.loc), in2);
        }
    }
    
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
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
            product(flow, 0, (i+1)/p, i, slices.get((int)c2.loc), in2);
        }
    }
    
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
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
            product(flow, 0, r/p, i, slices.get((int)c2.loc), in2);
        }
    }
    
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
            
            for(int k=0; k<nums[5]; k++) gpos[k] = c1.pos[cols[gcols1+k]];
            for(int k=0; k<nums[0]; k++) pos[cols[k]] = c1.pos[cols[nums[0]+k]];
            for(int k=0; k<nums[3]; k++) pos[cols[cc1+k]] = c1.pos[cols[frees1+k]];
            for(int k=0; k<nums[0]; k++) c2.loc += (x2x[k]==null ? c1.pos[cols[nums[0]+k]] : x2x[k][c1.pos[cols[nums[0]+k]]]) * spans2[k];
            product(flow, 0, r/p, i, slices.get((int)c2.loc), in2);
        }
    }
    
    private void product(ActionFlow flow, int id, double p, int r, Snapshop slice, Cube in2){
        if(slice==null) return;
        
        Cube cube = act.cube;
        for(int i=0; i<slice.size; i++){
            for(int k=0; k<nums[6]; k++) gpos[nums[5]+k] = slice.pos[i][cols[gcols2+k]-nums[0]];
            for(int k=0; k<nums[4]; k++) pos[cols[cc2+k]] = slice.pos[i][cols[frees2+k]-nums[0]];
            if(cube.g!=1) pos[cube.arity()] = (int)cube.location(gpos, gpos.length, gspans);
            for(int k=0; k<cube.arity(); k++) marks[k].set(pos[k]);
            compute(flow, id, p, act.input.cube, r, op, in2, slice.loc[i]);
        }
    }
    
    private void makeBuffer(double p){
        Cube input = act.input.cube;
        int[] cols1 = new int[nums[0]];
        System.arraycopy(cols, nums[0], cols1, 0, nums[0]);
        
        long z = (long)(1/p);
        int[] gfrees1 = input.dimensionsS(cols1);
        long size = input.sizeS(gfrees1);
        if(size>z) z = size;
        z = z>5000000 ? 3000000 : z>3000000 ? 1000000 : z>1000000 ? 600000 : z;
        
        buffer = new Pack(input.dims.length, input.measures.length, (int)z, cols1);
        for(int k=0; k<nums[0]; k++) pos[cols[k]] = act.input.pool.pos[cols[nums[0]+k]];
    }
    
    @Override
    public void reset(){
        buffer.reset();
        if(act.input2().pool.cells!=null) act.input2().pool.cells.reset();
        for(int k=0; k<nums[0]; k++) pos[cols[k]] = act.input.pool.pos[cols[nums[0]+k]];
    }
}
