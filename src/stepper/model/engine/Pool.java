package stepper.model.engine;

import stepper.util.*;
import stepper.model.sql.*;
import stepper.model.engine.flows.*;

public class Pool {
    public int blockings;
    protected long full = -1;
    public long loc;
    public int[] pos;
    protected int[] x;
    protected int[] cols;
    protected int[] frees;
    protected long[] spans;
    protected Condition[] cd;
    
    protected Action act;
    protected Cell cell;
    protected Pack cells;
    
    protected boolean recol;
    
    public Pool(){}
    
    public Pool(Action act, int[] cols){
        this.act = act;
        this.cols = cols;
        this.cd = act.msCondition();
        this.spans = act.cube.spansS();
        this.pos = new int[act.cube.arityS()];
        this.recol = act.cdxes!=null;
    }
    
    public Pool(Action act, int[] cols, int[] frees){
        this.act = act;
        this.cols = cols;
        this.frees = frees;
        this.cd = act.msCondition();
        this.spans = act.cube.spansS();
        this.pos = new int[act.cube.arityS()];
        this.recol = act.cdxes!=null;
    }
    
    public Pool(Action act, int[] cols, int blockings){
        this.act = act;
        this.cols = cols;
        this.blockings = blockings;
        this.cd = act.msCondition();
        this.spans = act.cube.spansS();
        this.pos = new int[act.cube.arityS()];
        this.recol = act.cdxes!=null;
        if(act.cube.schema.isEmpty() && act.cube instanceof SparseCube) act.cube = new ArrayCube((SparseCube)act.cube);
        if(blockings==0) return;
        
        long size = act.cube.poolSize(blockings);
        if(size>50000000 && !(act.cube instanceof SparseCube)) act.cube = new SparseCube((ArrayCube)act.cube);
        else if(size<=50000000 && act.cube instanceof SparseCube) act.cube = new ArrayCube((SparseCube)act.cube);
    }
    
    public int estimate(int[] columns, int min){
        int nums = (int)(act.cube.cardinality() * act.cube.markedSize(columns)/act.cube.size());
        return nums>min ? nums : min;
    }
    
    public void reset(){}
    
    public void execute(){
        act.cube.compute(act);
    }
    
    public void pumping(ActionFlow flow){
        act.cube.compute(act);
        this.loop(flow);
    }
    
    public void loop(ActionFlow flow){
        double p = act.cube.cardinality() * 1.0;
        if(act.cube instanceof SparseCube){
            SparseCube cube = (SparseCube)act.cube;
            for(int i=0; i<cube.longs.size; i++){
                if(cube.bits!=null && !cube.bits.get(i)) continue;
                cube.coordinates(pos, cube.longs.data[i], spans);
                for(int k=0; k<cube.values.length; k++) flow.y[k] = cube.values[k][i];
                flow.push(-1, (i+1)/p);
            }
        }else{
            ArrayCube cube = (ArrayCube)act.cube;
            for(int r=0, i=cube.bits.nextSetBit(0); i>=0; i=cube.bits.nextSetBit(i+1), r++){
                cube.coordinates(pos, i, spans);
                for(int k=0; k<cube.values.length; k++) flow.y[k] = cube.values[k][i];
                flow.push(-1, (r+1)/p);
            }
        }
        flow.flush(-1);
    }
    
    public void push(PipeJoin flow, int i, double p){
        if(cd!=null && !yes(flow.y)) return;
        
        Cube cube = act.cube;
        Pool c1 = act.input.pool;
        if(recol){
            for(int k=0; k<cols.length; k++) if(!cube.marked(k, c1.pos[cols[k]])) return;
            for(int k=0; k<frees.length; k++) if(!cube.marked(cols.length + k, c1.pos[frees[k]])) return;
        }else{
            for(int k=0; k<pos.length; k++) if(!cube.marked(k, c1.pos[k])) return;
        }
        
        for(int k=0; k<act.cube.measures.length; k++){
            Attribute ms = act.cube.measures[k];
            SQLFunction func = ms instanceof SQLFunction ? (SQLFunction)ms : null;
            if(func!=null) flow.y[k] = func.compute(flow.y[k]);
        }
        
        if(recol){
            for(int k=0; k<cols.length; k++) pos[k] = c1.pos[cols[k]];
            for(int k=0; k<frees.length; k++) pos[cols.length+k] = c1.pos[frees[k]];
        }else{
            for(int k=0; k<cols.length; k++) pos[k] = c1.pos[cols[k]];
        }
        flow.push(i, p);
    }
    
    public void push(ActionFlow flow, int i, double p){
        if(cd!=null && !yes(flow.y)) return;
        
        Cube cube = act.cube;
        Pool c1 = act.input.pool;
        if(recol){
            for(int k=0; k<cols.length; k++) if(!cube.marked(k, c1.pos[cols[k]])) return;
            for(int k=0; k<frees.length; k++) if(!cube.marked(cols.length + k, c1.pos[frees[k]])) return;
        }else{
            for(int k=0; k<pos.length; k++) if(!cube.marked(k, c1.pos[k])) return;
        }
        
        for(int k=0; k<act.cube.measures.length; k++){
            Attribute ms = act.cube.measures[k];
            SQLFunction func = ms instanceof SQLFunction ? (SQLFunction)ms : null;
            if(func!=null) flow.y[k] = func.compute(flow.y[k]);
        }
        if(recol){
            for(int k=0; k<cols.length; k++) pos[k] = c1.pos[cols[k]];
            for(int k=0; k<frees.length; k++) pos[cols.length+k] = c1.pos[frees[k]];
        }else{
            for(int k=0; k<cols.length; k++) pos[k] = c1.pos[cols[k]];
        }
        flow.push(i, p);
    }
    
    public void flush(PipeJoin flow, int i, double p){
        if(cd!=null && !yes(flow.y)){flow.flush(i, p); return;}
        
        Cube cube = act.cube;
        Pool c1 = act.input.pool;
        for(int k=0; k<cols.length; k++) if(!cube.marked(k, c1.pos[cols[k]])){flow.flush(i, p); return;}
        for(int k=0; k<frees.length; k++) if(!cube.marked(cols.length + k, c1.pos[frees[k]])){flow.flush(i, p); return;}
        
        for(int k=0; k<act.cube.measures.length; k++){
            Attribute ms = act.cube.measures[k];
            SQLFunction func = ms instanceof SQLFunction ? (SQLFunction)ms : null;
            if(func!=null) flow.y[k] = func.compute(flow.y[k]);
        }
        for(int k=0; k<cols.length; k++) pos[k] = c1.pos[cols[k]];
        for(int k=0; k<frees.length; k++) pos[cols.length+k] = c1.pos[frees[k]];
        flow.flush(i, p);
    }
    
    public void flush(ActionFlow flow, int i, double p){
        if(cd!=null && !yes(flow.y)){flow.flush(i, p); return;}
        
        Cube cube = act.cube;
        Pool c1 = act.input.pool;
        for(int k=0; k<cols.length; k++) if(!cube.marked(k, c1.pos[cols[k]])){flow.flush(i, p); return;}
        for(int k=0; k<frees.length; k++) if(!cube.marked(cols.length + k, c1.pos[frees[k]])){flow.flush(i, p); return;}
        
        for(int k=0; k<act.cube.measures.length; k++){
            Attribute ms = act.cube.measures[k];
            SQLFunction func = ms instanceof SQLFunction ? (SQLFunction)ms : null;
            if(func!=null) flow.y[k] = func.compute(flow.y[k]);
        }
        for(int k=0; k<cols.length; k++) pos[k] = c1.pos[cols[k]];
        for(int k=0; k<frees.length; k++) pos[cols.length+k] = c1.pos[frees[k]];
        flow.flush(i, p);
    }
    
    public void flush(PipeJoin flow, int i){
        flow.flush(i);
    }
    
    public void flush(ActionFlow flow, int i){
        flow.flush(i);
    }
    
    public boolean isFull(long loc){
        if(this.full==-1){this.full = loc; return false;}
        if(this.full!=loc){this.full = loc; return true;}
        return false;
    }
    
    public int[] repos(int[] pos, int[] repos){
        for(int i=0; i<cols.length; i++) repos[i] = pos[cols[i]];
        for(int i=0; i<frees.length; i++) repos[cols.length + i] = pos[frees[i]];
        return repos;
    }
    
    public void makeCell(double[] y){
        if(cells!=null) this.cells.add(pos, y);
        else if(cell!=null) this.cell.set(pos, y);
    }
    
    protected final void makingCells(){
        Cube in = act.input.cube, in2 = act.input2().cube;
        int d = in2.schema.size() - in.schema.size();
        int w1 = in2.arityS();
        int w2 = in2.measures.length;
        if(d<=0){
            act.input2().pool.cell = new Cell(w1, w2);
        }else{
            long len = in2.sizeFrees(w1 - in.schema.size());
            if(len>5000000) return;
            len = len>3000000 ? 3000000 : len>1000000 ? 1000000 : len;
            act.input2().pool.cells = new Pack(w1, w2, (int)len);
        }
    }
    
    public boolean yes(int r){
        for(int i=0; i<cd.length; i++) if(cd[i]!=null && !cd[i].compute(act.input.cube.values[i][r])) return false;
        return true;
    }
    
    public boolean yes(double[] y){
        for(int i=0; i<cd.length; i++) if(cd[i]!=null && !cd[i].compute(y[i])) return false;
        return true;
    }
    
    public void compute(ActionFlow flow, int id, double p, int[][] op, Cube in2, int loc2){
        for(int i=0; i<act.cube.values.length; i++){
            flow.y[i] = op[i][1]==SQLItem.Operator.CONCATL ? flow.y[op[i][0]] :
                        op[i][1]==SQLItem.Operator.CONCATR ? in2.values[op[i][2]][loc2] :
                        op[i][1]==SQLItem.Operator.ADD ? flow.y[op[i][0]] + in2.values[op[i][2]][loc2] :
                        op[i][1]==SQLItem.Operator.MINUS ? flow.y[op[i][0]] - in2.values[op[i][2]][loc2] :
                        op[i][1]==SQLItem.Operator.MULTIPLY ? flow.y[op[i][0]] * in2.values[op[i][2]][loc2] :
                        flow.y[op[i][0]] / in2.values[op[i][2]][loc2];
        }
        flow.push(id, p);
    }
    
    public void compute(ActionFlow flow, int id, double p, Cube in, int r1, int[][] op, Cube in2, int loc2){
        for(int i=0; i<act.cube.values.length; i++){
            flow.y[i] = op[i][1]==SQLItem.Operator.CONCATL ? in.values[op[i][0]][r1] :
                        op[i][1]==SQLItem.Operator.CONCATR ? in2.values[op[i][2]][loc2] :
                        op[i][1]==SQLItem.Operator.ADD ? in.values[op[i][0]][r1] + in2.values[op[i][2]][loc2] :
                        op[i][1]==SQLItem.Operator.MINUS ? in.values[op[i][0]][r1] - in2.values[op[i][2]][loc2] :
                        op[i][1]==SQLItem.Operator.MULTIPLY ? in.values[op[i][0]][r1] * in2.values[op[i][2]][loc2] :
                        in.values[op[i][0]][r1] / in2.values[op[i][2]][loc2];
        }
        flow.push(id, p);
    }
    
    public void compute(ActionFlow flow, int id, double p, Pack in, int r1, int[][] op, double[] y2){
        for(int i=0; i<act.cube.values.length; i++){
            flow.y[i] = op[i][1]==SQLItem.Operator.CONCATL ? in.yy[op[i][0]][r1] :
                        op[i][1]==SQLItem.Operator.CONCATR ? y2[op[i][2]] :
                        op[i][1]==SQLItem.Operator.ADD ? in.yy[op[i][0]][r1] + y2[op[i][2]] :
                        op[i][1]==SQLItem.Operator.MINUS ? in.yy[op[i][0]][r1] - y2[op[i][2]] :
                        op[i][1]==SQLItem.Operator.MULTIPLY ? in.yy[op[i][0]][r1] * y2[op[i][2]] :
                        in.yy[op[i][0]][r1] / y2[op[i][2]];
        }
        flow.push(id, p);
    }
    
    public SnapSlices snapping(int num, int[] cols){
        int[] cols1 = new int[num];
        System.arraycopy(cols, num*2, cols1, 0, num);
        int[] frees1 = this.act.cube.dimensionsS(cols1);
        return act.cube instanceof SparseCube ? snapping((SparseCube)act.cube, cols1, frees1) : 
                                                snapping((ArrayCube)act.cube, cols1, frees1);
    }
    
    public SnapSlices snapping(int[] cols, int[] frees){
        return act.cube instanceof SparseCube ? snapping((SparseCube)act.cube, cols, frees) : 
                                                snapping((ArrayCube)act.cube, cols, frees);
    }
    
    public SnapSlices snapping(SparseCube c, int[] cols1, int[] frees1){
        boolean yes = true;
        int arity = c.arityS();
        if(pos==null) pos = new int[arity];
        long[] spans1 = c.spansS(cols1);
        SnapSlices slices = new SnapSlices(c.cardinality(), c.markedSize(cols1), c.size());
        
        double p = c.cardinality() * 1.0;
        for(int i=0; i<c.longs.size; i++, yes=true){
            if(c.bits!=null && !c.bits.get(i)) continue;
            c.coordinates(pos, c.longs.data[i], spans);
            for(int k=0; yes && k<arity; k++) yes &= c.marked(k, pos[k]);
            if(!yes) continue;
            loc = c.location(pos, cols1, spans1);
            slices.getOrMake(loc, frees1.length).add((i+1)/p, i, pos, frees1);
        }
        
        loc = 0;
        for(int i=0; i<c.arity(); i++) pos[i] = 0;
        return slices;
    }
    
    public SnapSlices snapping(ArrayCube c, int[] cols1, int[] frees1){
        if(c.determined(frees1)) return null;
        
        boolean yes = true;
        int arity = c.arityS();
        if(pos==null) pos = new int[arity];
        long[] spans1 = c.spansS(cols1);
        SnapSlices slices = new SnapSlices(c.cardinality(), c.markedSize(cols1), c.size());
        
        double p = c.cardinality() * 1.0;
        for(int i=c.bits.nextSetBit(0); i>=0; i=c.bits.nextSetBit(i+1), yes=true){
            c.coordinates(pos, i, spans);
            for(int k=0; yes && k<arity; k++) yes &= c.marked(k, pos[k]);
            if(!yes) continue;
            loc = c.location(pos, cols1, spans1);
            slices.getOrMake(loc, frees1.length).add((i+1)/p, i, pos, frees1);
        }
        
        loc = 0;
        for(int i=0; i<c.arity(); i++) pos[i] = 0;
        return slices;
    }
    
    public IntHashSlices slicing(int num, int[] cols){
        int[] cols1 = new int[num];
        System.arraycopy(cols, num*2, cols1, 0, num);
        int[] frees1 = this.act.cube.dimensionsS(cols1);
        return act.cube instanceof SparseCube ? slicing1(cols1, frees1) : slicing2(cols1, frees1);
    }
    
    public IntHashSlices slicing(int[] cols, int[] frees){
        return act.cube instanceof SparseCube ? slicing1(cols, frees) : slicing2(cols, frees);
    }
    
    public IntHashSlices slicing1(int[] cols1, int[] frees1){
        SparseCube c = (SparseCube)act.cube;
        //if(c.longs.loc2idx!=null) return null;
        
        boolean yes = true;
        int arity = c.arityS();
        if(pos==null) pos = new int[arity];
        long[] spans1 = c.spansS(cols1);
        IntHashSlices slices = new IntHashSlices(c.cardinality(), c.markedSize(cols1), c.size());
        
        double p = c.cardinality() * 1.0;
        for(int i=0; i<c.longs.size; i++, yes=true){
            if(c.bits!=null && !c.bits.get(i)) continue;
            c.coordinates(pos, c.longs.data[i], spans);
            for(int k=0; yes && k<arity; k++) yes &= c.marked(k, pos[k]);
            if(!yes) continue;
            loc = c.location(pos, cols1, spans1);
            slices.getOrMake(loc).add((i+1)/p, i);
        }
        
        loc = 0;
        for(int i=0; i<c.arity(); i++) pos[i] = 0;
        
        return slices;
    }
    
    public IntHashSlices slicing2(int[] cols1, int[] frees1){
        ArrayCube c = (ArrayCube)act.cube;
        if(c.determined(frees1)) return null;
        
        boolean yes = true;
        if(pos==null) pos = new int[c.arityS()];
        long[] spans1 = c.spansS(cols1);
        IntHashSlices slices = new IntHashSlices(c.cardinality(), c.markedSize(cols1), c.size());
        
        double p = c.cardinality() * 1.0;
        for(int i=c.bits.nextSetBit(0); i>=0; i=c.bits.nextSetBit(i+1), yes=true){
            c.coordinates(pos, i, spans);
            for(int k=0; yes && k<pos.length; k++) yes &= c.marked(k, pos[k]);
            if(!yes) continue;
            loc = c.location(pos, cols1, spans1);
            slices.getOrMake(loc).add((i+1)/p, i);
        }
        
        loc = 0;
        for(int i=0; i<c.arity(); i++) pos[i] = 0;
        
        return slices;
    }
}
