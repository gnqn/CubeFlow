package stepper.util;

import it.unimi.dsi.fastutil.longs.*;

public class IntHashSlices {
    protected int cols;
    protected Long2ObjectMap idxes;
    
    public IntHashSlices(int rows){
        idxes = new Long2ObjectOpenHashMap(rows);
    }
    
    public IntHashSlices(int size, long subspace, long space){
        double r = subspace*1.0/space;
        if(r<0.01) r=0.01;
        int rows = (int)(size * r);
        this.cols = size/rows;
        if(rows<1000) rows = 1000;
        if(cols<100) cols = 100;
        idxes = new Long2ObjectOpenHashMap(rows);
    }
    
    public Ints get(long loc){
        return (Ints)idxes.get(loc);
    }
    
    public Ints getOrMake(long loc){
        Ints list = (Ints)idxes.get(loc);
        if(list!=null) return list;
        list = new Ints();
        idxes.put(loc, list);
        return list;
    }
    
    public LongSet keySet(){
        return idxes.keySet();
    }
}
