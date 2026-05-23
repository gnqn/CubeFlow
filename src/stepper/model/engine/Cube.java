package stepper.model.engine;

import java.util.*;
import java.time.*;
import stepper.util.*;
import stepper.model.sql.*;
import org.apache.arrow.vector.types.*;
import org.apache.arrow.vector.types.pojo.*;

public class Cube {
    protected int g = 1;
    protected BitSet bits;
    protected int[][] bases;
    protected BitSet[] marks;
    protected Object[][] dims;
    protected Hyb2IntMap[] maps;
    protected DimensionSpace schema;
    protected double[][] values;
    public Attribute[] measures;
    
    protected BitSet[] marks2;
    
    public BitSet bits(){
        return this.bits;
    }
    
    public boolean getBit(long loc){
        return false;
    }
    
    public long nextBit(long loc, long idx){
        return -1;
    }
    
    public int arity(){
        return this.schema.size();
    }
    
    public int arityS(){
        return this.dims.length;
    }
    
    public final int[] dimensionsS(int[] cols){
        Ints list = new Ints();
        for(int i=0; i<this.dims.length; i++) if(!within(i, cols)) list.add(i);
        return list.toArray();
    }
    
    public long size(){
        long size = 1L;
        for(Object[] keys: this.dims) size *= keys.length;
        return size;
    }
    
    public long sizeS(){
        long size = 1L;
        for(int i=0; i<this.dims.length; i++) size *=  this.isLShadow(i) ? this.bases[i].length : this.dims[i].length==0 ? g : this.dims[i].length;
        return size;
    }
    
    public long sizeS(int[] cols){
        long size = 1L;
        for(int i: cols) size *= this.isLShadow(i) ? this.bases[i].length : this.dims[i].length==0 ? g : this.dims[i].length;
        return size;
    }
    
    public long sizeFrees(int frees){
        long size = 1L;
        for(int i=this.dims.length-frees; i<this.dims.length; i++){
            size *= this.isLShadow(i) ? this.bases[i].length : this.dims[i].length==0 ? g : this.dims[i].length;
        }
        return size;
    }
    
    public long markedSize(int[] cols){
        long size = 1L;
        for(int i: cols) size *= this.marks[i]==null ? this.dims[i].length : this.marks[i].cardinality();
        return size;
    }
    
    public final long[] spans(){
        int arity = this.arity();
        long[] spans = new long[arity];
        for(int i=arity-1; i>=0; i--) spans[i] = i==arity-1 ? 1 : this.dims[i+1].length * spans[i+1];
        return spans;
    }
    
    public long[] spans(int cols){
        long[] spans = new long[cols];
        for(int i=cols-1; i>=0; i--) spans[i] = i==cols-1 ? 1 : this.dims[i+1].length * spans[i+1];
        return spans;
    }
    
    public long[] spans(int[] cols){
        long[] spans = new long[cols.length];
        for(int i=cols.length-1; i>=0; i--) spans[i] = i==cols.length-1 ? 1 : this.dims[cols[i+1]].length * spans[i+1];
        return spans;
    }
    
    public long[] spansS(){
        int arity = this.arityS();
        long[] spans = new long[arity];
        for(int i=arity-1; i>=0; i--) spans[i] = i==arity-1 ? 1 : (this.isLShadow(i+1) ? this.bases[i+1].length : this.dims[i+1].length==0 ? g : this.dims[i+1].length) * spans[i+1];
        return spans;
    }
    
    public long[] spansS(int[] cols){
        long[] spans = new long[cols.length];
        for(int i=cols.length-1; i>=0; i--) spans[i] = i==cols.length-1 ? 1 : (this.isLShadow(cols[i+1]) ? this.bases[cols[i+1]].length : this.dims[cols[i+1]].length==0 ? g : this.dims[cols[i+1]].length) * spans[i+1];
        return spans;
    }
    
    public static long[] spansOf(int[] sizes){
        long[] spans = new long[sizes.length];
        for(int i=sizes.length-1; i>=0; i--) spans[i] = i==sizes.length-1 ? 1 : sizes[i+1] * spans[i+1];
        return spans;
    }
    
    public void coordinates(int[] pos, long loc, long[] spans){
        pos[0] = (int)(loc/spans[0]);
        for(int m=1; m<spans.length; m++) pos[m] = (int)((loc%spans[m-1])/spans[m]);
    }
    
    public void coordinates(int[] pos, int[] cols, long loc, long[] spans){
        pos[cols[0]] = (int)(loc/spans[0]);
        for(int m=1; m<spans.length; m++) pos[cols[m]] = (int)((loc%spans[m-1])/spans[m]);
    }
    
    public void coordinates(int[] pos, int from, long loc, long[] spans){
        pos[from] = (int)(loc/spans[from]);
        for(int m=from+1; m<spans.length; m++) pos[m] = (int)((loc%spans[m-1])/spans[m]);
    }
    
    public long location(int[] pos, long[] spans){
        long loc = 0;
        for(int m=0; m<spans.length; m++) loc += pos[m] * spans[m];
        return loc;
    }
    
    public long location(int[] pos, int cols, long[] spans){
        long loc = 0;
        for(int m=0; m<cols; m++) loc += pos[m] * spans[m];
        return loc;
    }
    
    public long location(int[] pos, int[] cols, long[] spans){
        long loc = 0;
        for(int m=0; m<cols.length; m++) loc += pos[cols[m]] * spans[m];
        return loc;
    }
    
    public long poolSize(int num){
        long size = 1L;
        for(int i=0; i<num; i++) size *= this.bases[i]==null ? this.dims[i].length : this.bases[i].length==0 ? g : this.bases[i].length;
        return size;
    }
    
    public boolean isGShadow(int i){
        return this.bases[i]!=null && this.bases[i].length==0;
    }
    
    public boolean isLShadow(int i){
        return this.bases[i]!=null && this.bases[i].length!=0;
    }
    
    public boolean hasShadows(){
        for(int[] shadow: bases) if(shadow!=null) return true;
        return false;
    }
    
    public boolean determined(int[] frees){
        for(int k: frees) if(marks[k]==null || marks[k].cardinality()!=1) return false;
        return true;
    }
    
    public int[] c2g(){
        int[] c2g = new int[schema.size()];
        for(int i=0; i<schema.size(); i++){
            c2g[i] = i==0 ? 0 : (c2g[i-1] + 1);
            while(isGShadow(c2g[i])) c2g[i]++;
        }
        return c2g;
    }
    
    void reset(){}
    
    void allocate(int len){}
    
    boolean allocated(){
        return this.bits!=null;
    }
    
    public int cardinality(){
        return this.bits==null ? 0 : this.bits.cardinality();
    }
    
    public void pick(Cube in){
        this.dims = in.dims;
        this.maps = in.maps;
        this.marks = in.marks;
    }
    
    public void pick(int[] idxes, Cube in){}
    
    public boolean pick(int i, int k, Attribute attr, Condition cd, Cube child){
        this.dims[i] = child.dims[k];
        this.maps[i] = child.maps[k];
        return marking(i, k, attr, cd, child);
    }
    
    public boolean pick(int i, int k1, int k2, ArrayList<int[]> x2xs, Cube in, Cube in2){
        this.dims[i] = in.dims[k1];
        this.maps[i] = in.maps[k1];
        this.marks[i] = in.marks[k1];
        if(in.dims[k1]==in2.dims[k2] && !in.marked(k1) && !in2.marked(k2)){
            x2xs.add(null);
            return this.marks[i]==null || this.marks[i].cardinality()!=0;
        }
        
        int[] x2x;
        if(in.isLShadow(k1)){
            x2x = new int[in.bases[k1].length];
            this.bases[i] = in.bases[k1];
            this.marks[i] = new BitSet(in.bases[k1].length);
            for(int x1=0; x1<in.bases[k1].length; x1++){
                if(!in.marked(k1, x1)) continue;
                int x2 = in2.maps[k2].getInt(in.dims[k1][in.bases[k1][x1]]);
                if(x2==-1 || !in2.marked(k2, x2)) continue; 
                x2x[x1] = x2;
                this.marks[i].set(x1);
            }
        }else{
            if(in2.marks2==null) in2.marks2 = new BitSet[in2.marks.length];
            in2.marks2[k2] = new BitSet();
            
            x2x = new int[in.dims[k1].length];
            this.marks[i] = new BitSet(in.dims[k1].length);
            for(int x1=0; x1<in.dims[k1].length; x1++){
                if(!in.marked(k1, x1)) continue;
                int x2 = in2.maps[k2].getInt(in.dims[k1][x1]);
                if(x2==-1 || !in2.marked(k2, x2)) continue;
                x2x[x1] = x2;
                this.marks[i].set(x1);
                in2.marks2[k2].set(x2);
            }
        }
        x2xs.add(x2x);
        
        return this.marks[i]==null || this.marks[i].cardinality()!=0;
    }
    
    public boolean pickL(int i, int k1, int k2, ArrayList<int[]> xxs, ArrayList<int[][]> x2xs, Cube in, Cube in2){
        this.dims[i] = in.dims[k1];
        this.maps[i] = in.maps[k1];
        
        int len = in2.dims[k2].length;
        Ints[] groups = new Ints[len];
        for(int m=0; m<len; m++) groups[m] = new Ints(in2.bases[k2].length/len + 1);
        for(int m=0; m<in2.bases[k2].length; m++) if(in2.marked(k2, m)) groups[in2.bases[k2][m]].add(m);
        
        int[][] gs = new int[len][];
        for(int m=0; m<len; m++) gs[m] = groups[m].size()==0 ? null : groups[m].toArray();
        
        len = 0;
        int[][] x2x;
        Ints froms, xx2;
        
        if(in.isLShadow(k1)){
            froms = new Ints(in.bases[k1].length);
            xx2 = new Ints(in.bases[k1].length);
            x2x = new int[in.bases[k1].length][];
            this.marks[i] = new BitSet(in.bases[k1].length);
            for(int x1=0; x1<in.bases[k1].length; x1++){
                if(!in.marked(k1, x1)) continue;
                int x2 = in2.maps[k2].getInt(in.dims[k1][in.bases[k1][x1]]);
                if(x2==-1 || gs[x2]==null) continue; 
                froms.add(len);
                x2x[x1] = gs[x2];
                xx2.add(in.bases[k1][x1]);
                len += gs[x2].length;
                this.marks[i].set(x1);
            }
        }else{
            froms = new Ints(in.dims[k1].length);
            xx2 = new Ints(in.dims[k1].length);
            x2x = new int[in.dims[k1].length][];
            this.marks[i] = new BitSet(in.dims[k1].length);
            for(int x1=0; x1<in.dims[k1].length; x1++){
                if(!in.marked(k1, x1)) continue;
                int x2 = in2.maps[k2].getInt(in.dims[k1][x1]);
                if(x2==-1 || gs[x2]==null) continue;
                froms.add(len);
                x2x[x1] = gs[x2];
                xx2.add(x1);
                len += gs[x2].length;
                this.marks[i].set(x1);
            }
        }
        if(froms.size()==0) return false;
        
        int[] xx = froms.toArray();
        this.bases[i] = new int[len];
        
        int k = 0;
        int[] kk = xx2.toArray();
        for(; k<xx.length-1; k++) Arrays.fill(this.bases[i], xx[k], xx[k+1], kk[k]);
        Arrays.fill(this.bases[i], xx[k], len, kk[k]);
        xxs.add(xx);
        x2xs.add(x2x);
        return true;
    }
    
    public boolean marking(int i, Attribute attr, Condition cd, Cube in){
        if(cd==null) return true;
        BitSet mark = cd.compute(attr, in.marks[i], in.dims[i]);
        if(mark!=null){
            if(this.marks[i]!=null) mark.and(this.marks[i]);
            this.marks[i] = mark;
        }
        return this.marks[i]==null || this.marks[i].cardinality()!=0;
    }
    
    public boolean marking(int i, int k, Attribute attr, Condition cd, Cube child){
        this.marks[i] = child.marks[k];
        if(cd==null) return true;
        BitSet mark = cd.compute(attr, child.marks[k], child.dims[k]);
        if(mark!=null){
            if(this.marks[i]!=null) mark.and(this.marks[i]);
            this.marks[i] = mark;
        }
        return this.marks[i]==null || this.marks[i].cardinality()!=0;
    }
    
    public boolean markingG(int i, int k, Dimension dim, Condition cond, Cube in){
        if(bases[i]==null) bases[i] = new int[0];
        return marking(i, k, dim, cond, in);
    }
    
    public boolean marked(int k){
        return marks[k]!=null && marks[k].cardinality()!=this.dims[k].length;
    }
    
    public boolean marked(int k, int i){
        return marks[k]==null || marks[k].get(i);
    }
    
    public void setMeasure(double p, long loc, double[] y){}
    
    public void setMeasure(int r, Cube in, int r1, int[][] op, SparseCube in2, long loc2){}
    
    public void setMeasure(int r, Cube in, int r1, int[][] op, ArrayCube in2, int loc2){}
    
    public Cube building(int arity, DimensionSpace schema, ArrayList<Attribute> ms){return null;}

    public Cube building(int arity, DimensionSpace schema, ArrayList<Attribute> measures, Ints gcols1, Ints gcols2, Cube in2){return null;}
    
    public void sort(){}
    
    public int snapshop(int i, int cols, Snapshop snap, Pool c){return 0;}
    
    public boolean trans(int i, int k, Attribute attr, SQLFunction func, Condition cd, Cube child){
        return false;
    }
    
    public void compute(Action act){}
    
    public void cascade(Cube in){}
    
    public void cascade(int[][] op, double[][] y2){}
    
    public int dimOf(String dim){
        return dimOf(dim, 0);
    }
    
    public int dimOf(String name, int from){
        for(int i=from; i<this.schema.size(); i++) if(this.schema.get(i).hasName(name)) return i;
        return -1;
    }
    
    public int msOf(String ms){
        for(int i=0; i<this.measures.length; i++) if(this.measures[i].name().equalsIgnoreCase(ms)) return i;
        return -1;
    }
    
    public static boolean within(int d, int[] list){
        for(int k: list) if(k==d) return true;
        return false;
    }
    
    public void output(Action act, int limit){}
    
    public static boolean changed(int cols, int[] pos1, int[] pos2){
        for(int i=0; i<cols; i++) if(pos1[i]!=pos2[i]) return true;
        return false;
    }
    
    public static boolean aligned(int[] parts){
        for(int i=0; i<parts.length; i++) if(parts[i]!=i) return false;
        return true;
    }
    
    public static Cube instance(RTLoader rt){
        long size = 1L;
        int arity = rt.maps.length;
        DimensionSpace schema = rt.loader.schema;
        for(int k=0; k<arity; k++) size *= rt.maps[k].size();
        Cube cube = size<=50000000 ? new ArrayCube(schema, (int)size, rt.vnum) : new SparseCube(schema, rt.vnum);
        
        cube.maps = new Hyb2IntMap[arity];
        cube.marks = new BitSet[arity];
        cube.dims = new Object[arity][];
        Cube share = rt.loader.share==null ? null : rt.loader.share.cube;
        cube.measures = rt.loader.getAttributes().toArray(new Attribute[0]);
        for(int i=0; i<arity; i++){
            if(rt.shares[i]!=-1){
                cube.dims[i] = share.dims[rt.shares[i]];
                cube.maps[i] = share.maps[rt.shares[i]];
                continue;
            }
            
            cube.dims[i] = new Object[rt.maps[i].size()];
            if(rt.dtypes[i] instanceof ArrowType.Utf8){
                Hyb2IntMap map = new Hyb2IntMap().initObjects(rt.maps[i].map2.size());
                schema.get(i).typ = Attribute.Type.UTF8;
                for(Object key: rt.maps[i].map2.keySet()){
                    String v = key.toString();
                    int k = rt.maps[i].map2.getInt(key);
                    map.map2.put(v, k);
                    cube.dims[i][k] = v;
                }
                cube.maps[i] = map;
            }else if(rt.maps[i].map1!=null){
                cube.maps[i] = rt.maps[i];
                for(long key: rt.maps[i].map1.keySet()) cube.dims[i][rt.maps[i].map1.get(key)] = key;
                if(rt.dtypes[i] instanceof ArrowType.Date) schema.get(i).typ = Attribute.Type.DATEDAY;
                else if(rt.dtypes[i] instanceof ArrowType.Time) schema.get(i).typ = Attribute.Type.TIME;
            }else{
                cube.maps[i] = rt.maps[i];
                for(Object key: rt.maps[i].map2.keySet()) cube.dims[i][rt.maps[i].map2.getInt(key)] = read(key, rt.dtypes[i]);
            }
        }
        return cube;
    }
    
    public static Object read(Object key, ArrowType type){
        if(type instanceof ArrowType.Date){
            ArrowType.Date dateType = (ArrowType.Date)type;
            DateUnit dateUnit = dateType.getUnit();
            switch(dateUnit){
                case DAY:
                    return LocalDate.ofEpochDay((Integer)key);
                case MILLISECOND:
                    return Instant.ofEpochMilli((Long)key).atZone(ZoneId.systemDefault()).toLocalDate();
            }
        }
        if(type instanceof ArrowType.Time){
            ArrowType.Time timeType = (ArrowType.Time)type;
            TimeUnit unit = timeType.getUnit();
            switch(unit){
                case SECOND:
                    return LocalTime.ofNanoOfDay((Long)key * 1_000_000_000L);
                case MILLISECOND:
                    return LocalTime.ofNanoOfDay((Long)key * 1_000_000L);
                case MICROSECOND:
                    return LocalTime.ofNanoOfDay((Long)key * 1_000L);
                case NANOSECOND:
                    return LocalTime.ofNanoOfDay((Long)key);    
            }
        }
        return key;
    }
}
