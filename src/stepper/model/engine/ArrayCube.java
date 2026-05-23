package stepper.model.engine;

import java.util.*;
import stepper.util.*;
import stepper.model.sql.*;

public class ArrayCube extends Cube{
    
    ArrayCube(){}
    
    ArrayCube(int size, int cols){
        this.bits = new BitSet(size);
        this.values = new double[cols][size];
    }
    
    ArrayCube(ArrayCube cube){
        this.g = cube.g;
        this.dims = cube.dims;
        this.maps = cube.maps;
        this.marks = cube.marks;
        this.schema = cube.schema;
        this.measures = cube.measures;
        this.values = cube.values;
        this.bases = cube.bases==null ? new int[cube.arityS()][] : cube.bases;
    }
    
    public ArrayCube(DimensionSpace schema, int rows, int cols){
        this.schema = schema;
        this.bits = new BitSet(rows);
        this.values = new double[cols][rows];
    }
    
    public ArrayCube(int arity, DimensionSpace schema, ArrayList<Attribute> measures){
        this.schema = schema;
        this.bases = new int[arity][];
        this.dims = new Object[arity][];
        this.maps = new Hyb2IntMap[arity];
        this.measures = measures.toArray(new Attribute[0]);
        this.values = new double[this.measures.length][];
    }
    
    @Override
    void reset(){
        this.bits.clear();
    }
    
    @Override
    void allocate(int len){
        this.bits = new BitSet(len);
        for(int i=0; i<this.values.length; i++) this.values[i] = new double[len];
    }
    
    @Override
    public boolean getBit(long loc){
        return this.bits.get((int)loc);
    }
    
    @Override
    public long nextBit(long loc, long idx){
        return this.bits.nextSetBit((int)loc);
    }
    
    @Override
    public void setMeasure(double p, long loc, double[] y){
        this.bits.set((int)loc);
        for(int i=0; i<this.values.length; i++) this.values[i][(int)loc] = y[i];
    }
    
    @Override
    public void setMeasure(int r, Cube in, int r1, int[][] op, SparseCube in2, long loc2){
        this.bits.set(r);
        for(int i=0; i<this.values.length; i++){
            this.values[i][r] = op[i][1]==SQLItem.Operator.CONCATL ? in.values[op[i][0]][r1] :
                    op[i][1]==SQLItem.Operator.CONCATR ? in2.values[op[i][2]][in2.longs.rank(loc2)] :
                    op[i][1]==SQLItem.Operator.ADD ? in.values[op[i][0]][r1] + in2.values[op[i][2]][in2.longs.rank(loc2)] :
                    op[i][1]==SQLItem.Operator.MINUS ? in.values[op[i][0]][r1] - in2.values[op[i][2]][in2.longs.rank(loc2)] :
                    op[i][1]==SQLItem.Operator.MULTIPLY ? in.values[op[i][0]][r1] * in2.values[op[i][2]][in2.longs.rank(loc2)] :
                    in.values[op[i][0]][r1] / in2.values[op[i][2]][in2.longs.rank(loc2)];
        }
    }
    
    @Override
    public void setMeasure(int r, Cube in, int r1, int[][] op, ArrayCube in2, int loc2){
        this.bits.set(r);
        for(int i=0; i<this.values.length; i++){
            this.values[i][r] = op[i][1]==SQLItem.Operator.CONCATL ? in.values[op[i][0]][r1] :
                    op[i][1]==SQLItem.Operator.CONCATR ? in2.values[op[i][2]][loc2] :
                    op[i][1]==SQLItem.Operator.ADD ? in.values[op[i][0]][r1] + in2.values[op[i][2]][loc2] :
                    op[i][1]==SQLItem.Operator.MINUS ? in.values[op[i][0]][r1] - in2.values[op[i][2]][loc2] :
                    op[i][1]==SQLItem.Operator.MULTIPLY ? in.values[op[i][0]][r1] * in2.values[op[i][2]][loc2] :
                    in.values[op[i][0]][r1] / in2.values[op[i][2]][loc2];
        }
    }
    
    @Override
    public void pick(int[] idxes, Cube in){
        super.pick(in);
        this.bits = in.bits;
        for(int i=0; i<idxes.length; i++) this.values[i] = in.values[idxes[i]];
    }
    
    @Override
    public boolean trans(int i, int k, Attribute attr, SQLFunction func, Condition cd, Cube in){
        if(!marking(i, k, attr, cd, in)) return false;
        
        this.dims[i] = in.dims[k];
        this.maps[i] = in.maps[k];
        this.marks[i] = in.marks[k];
        func.trans(i, dims, marks, maps, bases);
        return true;
    }
    
    @Override
    public void sort(){
        long cost = System.currentTimeMillis();
        long[] spans = this.spansS();
        long[] spans2 = this.spans();
        int[] pos = new int[this.dims.length];
        
        int loc, len = (int)this.size();
        BitSet b = new BitSet(len);
        double[][] y = new double[values.length][];
        for(int i=0; i<values.length; i++) y[i] = new double[len];
        
        for(int i=bits.nextSetBit(0); i>=0; i=bits.nextSetBit(i+1)){
            this.coordinates(pos, i, spans);
            for(int k=0; k<this.dims.length; k++) if(bases[k]!=null) pos[k] = bases[k][pos[k]];
            loc = (int)this.location(pos, spans2);
            b.set(loc);
            for(int k=0; k<values.length; k++) y[k][loc] = this.values[k][i];
        }
        this.bits = b;
        this.values = y;
        for(int k=0; k<this.dims.length; k++) bases[k] = null;
        System.out.println("Sort: " + (System.currentTimeMillis() - cost) + "ms");
    }
    
    @Override
    public int snapshop(int i, int cols, Snapshop snap, Pool c){
        snap.size = 0;
        long loc2 = -1;
        boolean yes = true;
        for(i=bits.nextSetBit(i); i>=0; i=bits.nextSetBit(i+1), yes=true){
            this.coordinates(c.pos, i, c.spans);
            for(int k=0; k<cols; k++) loc2 = c.pos[k] * c.spans[k];
            if(loc2<c.loc) continue;
            if(loc2>c.loc) return i;
            
            for(int k=0; yes && k<this.marks.length; k++) yes &= marked(k, c.pos[k]);
            if(yes) snap.add(c.pos, i);
        }
        return i;
    }
    
    @Override
    public void compute(Action act){
        if(act.cube.measures.length==0) return;
        
        Pool c = act.pool;
        ArrayCube in = (ArrayCube)act.input.cube;
        this.values = new double[act.cube.measures.length][];
        this.bits = c.cd==null ? in.bits : (BitSet)in.bits.clone();
        for(int i=0; i<act.cube.measures.length; i++){
            Attribute ms = act.cube.measures[i].getAttribute();
            int k = in.msOf(ms.name());
            if(k==-1) continue;
            Condition cdi = c.cd==null ? null : c.cd[k];
            SQLFunction func = act.cube.measures[i] instanceof SQLFunction ? (SQLFunction)act.cube.measures[i] : null;
            if(this.values[i]==null) this.values[i] = func==null ? in.values[k] : in.values[k].clone();
            if(func!=null) func.compute(bits, cdi, values[i]);
            else if(cdi!=null) cdi.compute(bits, values[i]);
        }
    }
    
    @Override
    public Cube building(int arity, DimensionSpace schema, ArrayList<Attribute> measures){
        return new ArrayCube(arity, schema, measures);
    }
    
    @Override
    public Cube building(int arity, DimensionSpace schema, ArrayList<Attribute> measures, Ints gcols1, Ints gcols2, Cube in2){
        long p1 = 1, p2 = 1;
        for(int i=0; i<this.bases.length; i++){
            if(this.bases[i]==null || this.bases[i].length!=0) continue; 
            gcols1.add(i);
            p1 *= this.marks[i]==null ? this.dims[i].length : this.marks[i].cardinality();
        }
        for(int i=0; i<in2.bases.length; i++){
            if(in2.bases[i]==null || in2.bases[i].length!=0) continue; 
            gcols2.add(i);
            p2 *= in2.marks[i]==null ? in2.dims[i].length : in2.marks[i].cardinality();
        }
        long all = p1 * p2;
        if(all>=100000000) all *= 0.01;
        if(all<=1) return building(arity, schema, measures);
        if(all>1000000000L) return null;
        
        Cube cube = building(arity+1, schema, measures);
        cube.g = (int)all;
        cube.bases[arity] = new int[0];
        cube.dims[arity] = new Object[0];
        return cube;
    }
    
    public void aggregate(AggPool pool, Cube in, double p, int r){
        int idx = (int)pool.loc;
        for(int i=0; i<pool.aggs.length; i++){
            if(pool.aggs[i].isDistinct()) continue;
            if(pool.nums[i]!=null) pool.nums[i][idx]++;
            if(this.bits.get(idx)){
                this.values[i][idx] = pool.aggs[i].isCount() ? ++this.values[i][idx] : pool.aggs[i].compute(this.values[i][idx], in.values[i][r]);
            }else{
                this.bits.set(idx);
                this.values[i][idx] = pool.aggs[i].isCount() ? 1 : in.values[i][r];
            }
        }
    }
    
    public void aggregate(AggPool pool, Cube in, double p, double[] y){
        int idx = (int)pool.loc;
        for(int i=0; i<pool.aggs.length; i++){
            if(pool.aggs[i].isDistinct()) continue;
            if(pool.nums[i]!=null) pool.nums[i][idx]++;
            if(this.bits.get(idx)){
                this.values[i][idx] = pool.aggs[i].isCount() ? ++this.values[i][idx] : pool.aggs[i].compute(this.values[i][idx], y[i]);
            }else{
                this.bits.set(idx);
                this.values[i][idx] = pool.aggs[i].isCount() ? 1 : y[i];
            }
        }
    }
    
    @Override
    public void cascade(Cube in){
        if(!(in instanceof ArrayCube)) return;
        this.bits = new BitSet(in.bits.size());
        for(int i=0; i<in.values.length && i<this.values.length; i++) this.values[i] = in.values[i].clone();
        int size = in.values.length==0 ? (int)this.size() : in.values[0].length;
        for(int i=in.values.length; i<this.values.length; i++) this.values[i] = new double[size];
    }
    
    @Override
    public void cascade(int[][] op, double[][] y2){
        for(int r=bits.nextSetBit(0); r>=0; r=bits.nextSetBit(r+1)){
            for(int i=0; i<this.values.length; i++){
                values[i][r] = op[i][1]==SQLItem.Operator.CONCATL ? values[op[i][0]][r] :
                    op[i][1]==SQLItem.Operator.CONCATR ? y2[op[i][2]][0] :
                    op[i][1]==SQLItem.Operator.ADD ? values[op[i][0]][r] + y2[op[i][2]][0] :
                    op[i][1]==SQLItem.Operator.MINUS ? values[op[i][0]][r] - y2[op[i][2]][0] :
                    op[i][1]==SQLItem.Operator.MULTIPLY ? values[op[i][0]][r] * y2[op[i][2]][0] :
                    values[op[i][0]][r] / y2[op[i][2]][0];
            }
        }
    }
    
    public void compute(double p, long loc, double[] y1, int[][] op, Cube in2, int r2){
        this.bits.set((int)loc);
        for(int i=0; i<this.values.length; i++){
            this.values[i][(int)loc] = op[i][1]==SQLItem.Operator.CONCATL ? y1[op[i][0]] :
                    op[i][1]==SQLItem.Operator.CONCATR ? in2.values[op[i][2]][r2] :
                    op[i][1]==SQLItem.Operator.ADD ? y1[op[i][0]] + in2.values[op[i][2]][r2] :
                    op[i][1]==SQLItem.Operator.MINUS ? y1[op[i][0]] - in2.values[op[i][2]][r2] :
                    op[i][1]==SQLItem.Operator.MULTIPLY ? y1[op[i][0]] * in2.values[op[i][2]][r2] :
                    y1[op[i][0]] / in2.values[op[i][2]][r2];
        }
    }
    
    public void compute(double p, long loc, Cube in, int r1, int[][] op, Cube in2, int r2){
        this.bits.set((int)loc);
        for(int i=0; i<this.values.length; i++){
            this.values[i][(int)loc] = op[i][1]==SQLItem.Operator.CONCATL ? in.values[op[i][0]][r1] :
                    op[i][1]==SQLItem.Operator.CONCATR ? in2.values[op[i][2]][r2] :
                    op[i][1]==SQLItem.Operator.ADD ? in.values[op[i][0]][r1] + in2.values[op[i][2]][r2] :
                    op[i][1]==SQLItem.Operator.MINUS ? in.values[op[i][0]][r1] - in2.values[op[i][2]][r2] :
                    op[i][1]==SQLItem.Operator.MULTIPLY ? in.values[op[i][0]][r1] * in2.values[op[i][2]][r2] :
                    in.values[op[i][0]][r1] / in2.values[op[i][2]][r2];
        }
    }
    
    public void compute(double p, long loc, Pack in, int r1, int[][] op, double[] y2){
        this.bits.set((int)loc);
        for(int i=0; i<this.values.length; i++){
            this.values[i][(int)loc] = op[i][1]==SQLItem.Operator.CONCATL ? in.yy[op[i][0]][r1] :
                    op[i][1]==SQLItem.Operator.CONCATR ? y2[op[i][2]] :
                    op[i][1]==SQLItem.Operator.ADD ? in.yy[op[i][0]][r1] + y2[op[i][2]] :
                    op[i][1]==SQLItem.Operator.MINUS ? in.yy[op[i][0]][r1] - y2[op[i][2]] :
                    op[i][1]==SQLItem.Operator.MULTIPLY ? in.yy[op[i][0]][r1] * y2[op[i][2]] :
                    in.yy[op[i][0]][r1] / y2[op[i][2]];
        }
    }
    
    @Override
    public void output(Action act, int limit){
        String line = "";
        int arity = this.arityS();
        int a1 = this.schema.size(), a2 = this.measures.length;
        for(int i=0; i<a1; i++) line += this.schema.get(i).name() + "\t";
        for(int i=0; i<a2; i++) line += this.measures[i].name() + "\t";
        System.out.println(line);
        line = "_";
        for(int i=0; i<a1+a2; i++) line += "__________";
        System.out.println(line);
        
        String rcd = "";
        if(arity==0){
            for(int i=0; i<a2; i++) rcd += String.valueOf(this.values[i][0]) + "\t";
            System.out.println(rcd);
            System.out.println(line);
            return;
        }
        
        boolean fail = false;
        int[] cols = act.cols();
        int[] pos = new int[arity];
        long[] spans = this.spansS();
        int[] typs = new int[a1 + a2];
        for(int k=0; k<a1; k++) typs[k] = this.schema.get(k).typ;
        
        for(int i=bits.nextSetBit(0), rows=0; i>=0 && rows<limit; i=bits.nextSetBit(i+1), fail=false, rcd=""){
            this.coordinates(pos, i, spans);
            for(int k=0; k<arity; k++) if(fail = marks[k]!=null && !marks[k].get(pos[k])) break;
            if(fail) continue;
            
            if(cols==null) for(int k=0; k<a1; k++){
                if(typs[k]!=0) rcd += (bases[k]==null ? DateHelper.toIsoString(typs[k], dims[k][pos[k]]) : DateHelper.toIsoString(typs[k], dims[k][bases[k][pos[k]]])) + "\t";
                else rcd += (bases[k]==null ? dims[k][pos[k]].toString() : dims[k][bases[k][pos[k]]].toString()) + "\t";
            }else for(int k=0; k<a1; k++){
                if(typs[k]!=0) rcd += (bases[cols[k]]==null ? DateHelper.toIsoString(typs[k], dims[cols[k]][pos[cols[k]]]) : DateHelper.toIsoString(typs[k], dims[cols[k]][bases[cols[k]][pos[cols[k]]]])) + "\t";
                else rcd += (bases[cols[k]]==null ? dims[cols[k]][pos[cols[k]]].toString() : dims[cols[k]][bases[cols[k]][pos[cols[k]]]].toString()) + "\t";
            }
            for(int k=0; k<a2; k++) rcd += String.valueOf(this.values[k][i]) + "\t";
            
            rows++;
            System.out.println(rcd);
        }
        System.out.println(line);
    }
}
