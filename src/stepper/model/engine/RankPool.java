package stepper.model.engine;

import java.util.*;
import stepper.util.*;
import it.unimi.dsi.fastutil.longs.*;

public class RankPool extends Pool{
    private int mid;
    protected int[] parts;
    private Doubles buffer;
    
    public RankPool(Action act, int[] cols, int[] frees, int[] parts){
        super(act, cols, frees);
        this.parts = parts;
    }
    
    @Override
    public void execute(){
        long cost = System.currentTimeMillis();
        ArrayCube cube = (ArrayCube)act.cube, in = (ArrayCube)act.input.cube;
        cube.values = in.values.clone();
        cube.bits = cd==null ? in.bits : new BitSet(in.cardinality());
        if(cube instanceof SparseCube) ((SparseCube)cube).longs = ((SparseCube)in).longs;
        
        if(Cube.aligned(parts)){
            if(cube instanceof SparseCube) ranking1((SparseCube)cube);
            else ranking1((ArrayCube)cube);
        }else{
            ranking2();
        }
        System.out.println("Rank cost:\t" + (System.currentTimeMillis() - cost) + "ms");
    }
    
    private void ranking1(SparseCube cube){
        int from = 0;
        long last = 0;
        boolean yes = true;
        long[] part_spans = cube.spans(parts);
        
        buffer = cd==null ? null : new Doubles();
        for(int i=0; i<cube.longs.size; i++, yes = true, loc=0){
            if(cd!=null && !yes(i)) continue;
            cube.coordinates(pos, cube.longs.data[i], spans);
            for(int k=0; yes && k<cols.length; k++) yes &= cube.marked(k, pos[k]);
            if(!yes) continue;
            
            if(cd!=null) cube.bits.set(i);
            for(int k=0; k<parts.length; k++) loc += (cube.bases[k]==null ? pos[k] : cube.bases[k][pos[k]]) * part_spans[k];
            if(last==loc){if(buffer!=null) buffer.add(cube.values[mid][i]); continue;}
            
            if(buffer==null){
                int[] ranks = Doubles.ranks(from, i, cube.values[mid]);
                for(int k=from; k<i; k++) cube.values[mid][k] = ranks[k-from] + 1;
            }else if(!buffer.isEmpty()){
                int[] ranks = buffer.ranks();
                for(int m=0, k=from; k<i; k=cube.bits.nextSetBit(k+1)) cube.values[mid][k] = ranks[m++] + 1;
            }
            
            from = i;
            last = loc;
            if(buffer!=null) buffer.readd(cube.values[mid][i]);
        }
        
        if(buffer==null){
            int[] ranks = Doubles.ranks(from, cube.cardinality(), cube.values[mid]);
            for(int k=from; k<cube.cardinality(); k++) cube.values[mid][k] = ranks[k-from] + 1;
        }else if(!buffer.isEmpty()){
            int[] ranks = buffer.ranks();
            for(int m=0, k=from; k>0; k=cube.bits.nextSetBit(k+1)) cube.values[mid][k] = ranks[m++] + 1;
        }
    }
    
    private void ranking1(ArrayCube cube){
        int from = 0;
        long last = 0;
        boolean yes = true;
        long[] part_spans = cube.spans(parts);
        
        for(int i=cube.bits.nextSetBit(0); i>=0; i=cube.bits.nextSetBit(i+1), yes = true, loc=0){
            if(cd!=null && !yes(i)) continue;
            cube.coordinates(pos, i, spans);
            for(int k=0; yes && k<cols.length; k++) yes &= cube.marked(k, pos[k]);
            if(!yes) continue;
            
            if(cd!=null) cube.bits.set(i);
            for(int k=0; k<parts.length; k++) loc += (cube.bases[k]==null ? pos[k] : cube.bases[k][pos[k]]) * part_spans[k];
            if(last==loc) continue;
            
            int[] ranks = Doubles.ranks(from, i, cube.values[mid]);
            for(int k=from; k<i; k++) cube.values[mid][k] = ranks[k-from] + 1;
            
            from = i;
            last = loc;
        }
        
        int[] ranks = Doubles.ranks(from, cube.cardinality(), cube.values[mid]);
        for(int k=from; k<cube.cardinality(); k++) cube.values[mid][k] = ranks[k-from] + 1;
    }
    
    public void ranking2(){
        Pool c1 = act.input.pool;
        ArrayCube cube = (ArrayCube)act.cube, in = (ArrayCube)act.input.cube;
        
        int[] others = in.dimensionsS(parts);
        IntHashSlices slices = in instanceof SparseCube ? c1.slicing1(parts, others) : c1.slicing2(parts, others);
        LongIterator iterator = slices.keySet().longIterator();
        while(iterator.hasNext()){
            long key = iterator.nextLong();
            Ints ints = slices.get(key);
            int[] ii = ints.data();
            double[] slice = new double[ints.size()];
            for(int i=0; i<ints.size(); i++) slice[i] = in.values[mid][ii[i]];
            
            Integer[] idxes = new Integer[ints.size()];
            for(int i=0; i<ints.size(); i++) idxes[i] = i;
            Arrays.sort(idxes, Comparator.comparingDouble(i -> slice[i]));
            for(int i=0; i<ints.size(); i++) cube.values[mid][ii[idxes[i]]] = i + 1;
        }
    }
}
