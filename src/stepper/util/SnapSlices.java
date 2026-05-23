package stepper.util;

import it.unimi.dsi.fastutil.longs.*;

public class SnapSlices {
    protected int rows;
    protected Long2ObjectMap idxes;
    
    public SnapSlices(int rows){
        idxes = new Long2ObjectOpenHashMap(rows);
    }
    
    public SnapSlices(int size, long subspace, long space){
        double r = subspace*1.0/space;
        if(r<0.01) r=0.01;
        int slots = (int)(size * r);
        this.rows = size/slots;
        if(slots<1000) slots = 1000;
        if(rows<100) rows = 100;
        idxes = new Long2ObjectOpenHashMap(slots);
    }
    
    public Snapshop get(long loc){
        return (Snapshop)idxes.get(loc);
    }
    
    public Snapshop getOrMake(long loc, int cols){
        Snapshop snap = (Snapshop)idxes.get(loc);
        if(snap!=null) return snap;
        snap = new Snapshop(rows, cols);
        idxes.put(loc, snap);
        return snap;
    }
    
    public LongSet keySet(){
        return idxes.keySet();
    }
}
