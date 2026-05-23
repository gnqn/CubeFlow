package stepper.model.engine;

import java.util.*;
import stepper.util.*;
import stepper.model.sql.*;

public class SparseCube extends ArrayCube{
    protected Longs longs;
    
    SparseCube(int init, int cols){
        this.values = new double[cols][init];
        this.longs = Longs.newInstance(init);
    }
    
    SparseCube(ArrayCube cube){
        super(cube);
    }
    
    public SparseCube(int arity, DimensionSpace schema, ArrayList<Attribute> measures){
        super(arity, schema, measures);
    }
    
    public SparseCube(DimensionSpace schema, int cols){
        this.schema = schema;
        this.longs = new Longs();
        this.values = new double[cols][];
    }
    
    @Override
    void reset(){
        this.longs.size = 0;
    }
    
    @Override
    void allocate(int len){
        this.longs = Longs.newInstance(len);
        for(int i=0; i<this.values.length; i++) this.values[i] = new double[len];
    }
    
    @Override
    boolean allocated(){
        return this.longs!=null;
    }
    
    @Override
    public boolean getBit(long loc){
        return this.longs.rank(loc)!=-1;
    }
    
    @Override
    public long nextBit(long ki, long idx){
        if(idx<0 || idx>=this.longs.size) return -1;
        if(longs.idxes!=null) idx = longs.idxes[(int)idx];
        return this.longs.data[(int)idx];
    }
    
    @Override
    public void setMeasure(double p, long loc, double[] y){
        this.longs.add(loc, p);
        for(int i=0; i<this.values.length; i++){
            if(this.values[i].length<this.longs.data.length){
                double[] temp = new double[this.longs.data.length];
                System.arraycopy(this.values[i], 0, temp, 0, this.values[i].length);
                this.values[i] = temp;
            }
            this.values[i][this.longs.size-1] = y[i];
        }
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
    public int cardinality(){
        return this.bits==null ? (this.longs==null ? 0 : this.longs.size) : this.bits.cardinality();
    }
    
    @Override
    public void pick(int[] idxes, Cube in){
        super.pick(in);
        this.longs = ((SparseCube)in).longs;
        for(int i=0; i<idxes.length; i++) this.values[i] = in.values[idxes[i]];
    }
    
    @Override
    public Cube building(int arity, DimensionSpace schema, ArrayList<Attribute> measures){
        return new SparseCube(arity, schema, measures);
    }
    
    @Override
    public void sort(){
        long cost = System.currentTimeMillis();
        long[] spans = this.spansS();
        long[] spans2 = this.spans();
        int[] pos = new int[this.dims.length];
        long[] data = new long[longs.size];
        for(int i=0; i<longs.size; i++){
            this.coordinates(pos, longs.data[i], spans);
            for(int k=0; k<this.dims.length; k++) if(bases[k]!=null) pos[k] = bases[k][pos[k]];
            data[i] = this.location(pos, spans2);
        }
        //long[] temp = longs.data;
        this.longs.data = data;
        this.longs.sort();
        //this.longs.data = temp;
        for(int k=0; k<this.dims.length; k++) bases[k] = null;
        System.out.println("Sort: " + (System.currentTimeMillis() - cost) + "ms");
    }
    
    @Override
    public int snapshop(int i, int cols, Snapshop snap, Pool c){
        snap.size = 0;
        boolean yes = true;
        for(long loc=0; i<longs.size; i++, yes=true, loc=0){
            this.coordinates(c.pos, longs.data[i], c.spans);
            for(int k=0; k<cols; k++) loc += c.pos[k] * c.spans[k];
            if(loc<c.loc) continue;
            if(loc>c.loc) return i;
            
            for(int k=0; yes && k<this.marks.length; k++) yes &= marked(k, c.pos[k]);
            if(yes) snap.add(c.pos, i);
        }
        return i;
    }
    
    @Override
    public void compute(Action act){
        if(act.cube.measures.length==0) return;
        
        Pool c = act.pool;
        SparseCube in = (SparseCube)act.input.cube;
        this.bits = in.bits;
        this.longs = in.longs;
        this.values = new double[act.cube.measures.length][];
        
        if(c.cd!=null){
            if(this.bits==null){
                this.bits = new BitSet(in.cardinality());
                this.bits.set(0, in.cardinality());
            }else{
                this.bits = (BitSet)in.bits.clone();
            }
        }
        
        for(int i=0; i<act.cube.measures.length; i++){
            Attribute ms = act.cube.measures[i].getAttribute();
            int k = in.msOf(ms.name());
            if(k==-1) continue;
            Condition cdi = c.cd==null ? null : c.cd[k];
            SQLFunction func = act.cube.measures[i] instanceof SQLFunction ? (SQLFunction)act.cube.measures[i] : null;
            if(this.values[i]==null) this.values[i] = func==null ? in.values[k] : in.values[k].clone();
            if(func!=null){
                if(this.bits==null) func.compute(this.values[i]);
                else func.compute(this.bits, cdi, this.values[i]);
            }else if(cdi!=null) cdi.compute(this.bits, this.values[i]);
        }
    }
    
    @Override
    public void aggregate(AggPool pool, Cube in, double p, int r){
        int k = pool.locs.get(pool.loc);
        if(k==-1){
            pool.locs.put(pool.loc, longs.size);
            if(longs.size>=longs.data.length) expands(pool, p);
        }
        
        for(int i=0; i<pool.aggs.length; i++){
            if(pool.aggs[i].isDistinct()) continue;
            
            if(k==-1){
                if(pool.nums[i]!=null) pool.nums[i][longs.size]++;
                this.longs.data[longs.size] = pool.loc;
                this.values[i][longs.size] = pool.aggs[i].isCount() ? 1 : in.values[i][r];
            }else{
                if(pool.nums[i]!=null) pool.nums[i][k]++;
                this.values[i][k] = pool.aggs[i].isCount() ? ++this.values[i][k] : pool.aggs[i].compute(this.values[i][k], in.values[i][r]);
            }
        }
        longs.size++;
    }
    
    @Override
    public void aggregate(AggPool pool, Cube in, double p, double[] y){
        int k = pool.locs.get(pool.loc);
        if(k==-1){
            pool.locs.put(pool.loc, longs.size);
            if(longs.size>=longs.data.length) expands(pool, p);
        }
        
        for(int i=0; i<pool.aggs.length; i++){
            if(pool.aggs[i].isDistinct()) continue;
            
            if(k==-1){
                if(pool.nums[i]!=null) pool.nums[i][longs.size]++;
                this.longs.data[longs.size] = pool.loc;
                this.values[i][longs.size] = pool.aggs[i].isCount() ? 1 : y[i];
            }else{
                if(pool.nums[i]!=null) pool.nums[i][k]++;
                this.values[i][k] = pool.aggs[i].isCount() ? ++this.values[i][k] : pool.aggs[i].compute(this.values[i][k], y[i]);
            }
        }
        longs.size++;
    }
    
    @Override
    public void cascade(Cube in){
        if(!(in instanceof SparseCube)) return;
        SparseCube cube = (SparseCube)in;
        this.bits = new BitSet(in.cardinality());
        this.longs = cube.longs;
        for(int i=0; i<in.values.length && i<this.values.length; i++) this.values[i] = in.values[i].clone();
        for(int i=cube.values.length; i<this.values.length; i++) this.values[i] = new double[this.longs.size];
    }
    
    @Override
    public void cascade(int[][] op, double[][] y2){
        for(int r=0; r<longs.size; r++){
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
    
    @Override
    public void compute(double p, long loc, double[] y1, int[][] op, Cube in2, int r2){
        this.longs.add(loc, p);
        for(int i=0; i<this.values.length; i++){
            if(this.values[i].length<this.longs.data.length){
                double[] temp = new double[this.longs.data.length];
                System.arraycopy(this.values[i], 0, temp, 0, this.values[i].length);
                this.values[i] = temp;
            }
            this.values[i][this.longs.size-1] = op[i][1]==SQLItem.Operator.CONCATL ? y1[op[i][0]] :
                    op[i][1]==SQLItem.Operator.CONCATR ? in2.values[op[i][2]][r2] :
                    op[i][1]==SQLItem.Operator.ADD ? y1[op[i][0]] + in2.values[op[i][2]][r2] :
                    op[i][1]==SQLItem.Operator.MINUS ? y1[op[i][0]] - in2.values[op[i][2]][r2] :
                    op[i][1]==SQLItem.Operator.MULTIPLY ? y1[op[i][0]] * in2.values[op[i][2]][r2] :
                    y1[op[i][0]] / in2.values[op[i][2]][r2];
        }
    }
    
    @Override
    public void compute(double p, long loc, Cube in, int r1, int[][] op, Cube in2, int r2){
        this.longs.add(loc, p);
        for(int i=0; i<this.values.length; i++){
            if(this.values[i].length<this.longs.data.length){
                double[] temp = new double[this.longs.data.length];
                System.arraycopy(this.values[i], 0, temp, 0, this.values[i].length);
                this.values[i] = temp;
            }
            this.values[i][this.longs.size-1] = op[i][1]==SQLItem.Operator.CONCATL ? in.values[op[i][0]][r1] :
                    op[i][1]==SQLItem.Operator.CONCATR ? in2.values[op[i][2]][r2] :
                    op[i][1]==SQLItem.Operator.ADD ? in.values[op[i][0]][r1] + in2.values[op[i][2]][r2] :
                    op[i][1]==SQLItem.Operator.MINUS ? in.values[op[i][0]][r1] - in2.values[op[i][2]][r2] :
                    op[i][1]==SQLItem.Operator.MULTIPLY ? in.values[op[i][0]][r1] * in2.values[op[i][2]][r2] :
                    in.values[op[i][0]][r1] / in2.values[op[i][2]][r2];
        }
    }
    
    @Override
    public void compute(double p, long loc, Pack in, int r1, int[][] op, double[] y2){
        this.longs.add(loc, p);
        for(int i=0; i<this.values.length; i++){
            if(this.values[i].length<this.longs.data.length){
                double[] temp = new double[this.longs.data.length];
                System.arraycopy(this.values[i], 0, temp, 0, this.values[i].length);
                this.values[i] = temp;
            }
            this.values[i][this.longs.size-1] = op[i][1]==SQLItem.Operator.CONCATL ? in.yy[op[i][0]][r1] :
                    op[i][1]==SQLItem.Operator.CONCATR ? y2[op[i][2]] :
                    op[i][1]==SQLItem.Operator.ADD ? in.yy[op[i][0]][r1] + y2[op[i][2]] :
                    op[i][1]==SQLItem.Operator.MINUS ? in.yy[op[i][0]][r1] - y2[op[i][2]] :
                    op[i][1]==SQLItem.Operator.MULTIPLY ? in.yy[op[i][0]][r1] * y2[op[i][2]] :
                    in.yy[op[i][0]][r1] / y2[op[i][2]];
        }
    }
    
    public void expands(AggPool pool, double p){
        double ratio = p==0 ? 2.0 : 1/p;
        int growth = (int)((ratio<1.15 ? 1.15 : ratio) * this.longs.size);
        if(growth>3000000) growth = 3000000;
        
        if(this.longs.data.length - this.longs.size>=growth) return;
        growth += this.longs.size;
        
        long[] temp = new long[growth];
        System.arraycopy(this.longs.data, 0, temp, 0, this.longs.size);
        this.longs.data = temp;
        
        for(int i=0; i<pool.aggs.length; i++){
            if(pool.aggs[i].isDistinct()) continue;
            double[] temp2 = new double[growth];
            System.arraycopy(this.values[i], 0, temp2, 0, this.longs.size);
            this.values[i] = temp2;
            
            if(pool.nums[i]==null) continue;
            int[] temp3 = new int[growth];
            System.arraycopy(pool.nums[i], 0, temp3, 0, this.longs.size);
            pool.nums[i] = temp3;
        }
    }
    
    public void expand(int len){
        long[] temps = new long[longs.size + len];
        if(longs.size>0) System.arraycopy(longs.data, 0, temps, 0, longs.size);
        longs.data = temps;
        for(int k=0; k<this.values.length; k++){
            double[] temps2 = new double[longs.size + len];
            if(values[k]!=null) System.arraycopy(values[k], 0, temps2, 0, longs.size);
            this.values[k] = temps2;
        }
    }
    
    @Override
    public void output(Action act, int limit){
        String line = "";
        int a1 = this.schema.size(), a2 = this.measures.length;
        for(int i=0; i<a1; i++) line += this.schema.get(i).name() + "\t";
        for(int i=0; i<a2; i++) line += this.measures[i].name() + "\t";
        System.out.println(line);
        line = "_";
        for(int i=0; i<a1+a2; i++) line += "__________";
        System.out.println(line);
        
        boolean fail = false;
        int arity = this.arityS();
        int[] cols = act.cols();
        int[] pos = new int[arity];
        long[] spans = this.spansS();
        int[] typs = new int[a1 + a2];
        for(int k=0; k<a1; k++) typs[k] = this.schema.get(k).typ;
        
        String rcd = "";
        
        for(int i=0, num=0; i<this.longs.size && num<limit; i++, fail=false, rcd=""){
            if(this.bits!=null && !this.bits.get(longs.idxes==null ? i : longs.idxes[i])) continue;
            this.coordinates(pos, this.longs.data[longs.idxes==null ? i : longs.idxes[i]], spans);
            for(int k=0; k<arity; k++) if(fail = marks[k]!=null && !marks[k].get(pos[k])) break;
            if(fail) continue;
            
            if(cols==null) for(int k=0; k<a1; k++){
                if(typs[k]!=0) rcd += (bases[k]==null ? DateHelper.toIsoString(typs[k], dims[k][pos[k]]) : DateHelper.toIsoString(typs[k], dims[k][bases[k][pos[k]]])) + "\t";
                else rcd += (bases[k]==null ? dims[k][pos[k]].toString() : dims[k][bases[k][pos[k]]].toString()) + "\t";
            }else for(int k=0; k<a1; k++){
                if(typs[k]!=0) rcd += (bases[cols[k]]==null ? DateHelper.toIsoString(typs[k], dims[cols[k]][pos[cols[k]]]) : DateHelper.toIsoString(typs[k], dims[cols[k]][bases[cols[k]][pos[cols[k]]]])) + "\t";
                else rcd += (bases[cols[k]]==null ? dims[cols[k]][pos[cols[k]]].toString() : dims[cols[k]][bases[cols[k]][pos[cols[k]]]].toString()) + "\t";
            }
            for(int k=0; k<a2; k++) rcd += String.valueOf(this.values[k][longs.idxes==null ? i : longs.idxes[i]]) + "\t";
            
            num++;
            System.out.println(rcd);
        }
        System.out.println(line);
    }
}
