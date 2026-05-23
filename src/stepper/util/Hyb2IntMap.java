package stepper.util;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;

public class Hyb2IntMap{
    public Long2IntOpenHashMap map1;
    public Object2IntOpenHashMap map2;
    
    public Hyb2IntMap initLongs(int len){
        map1 = new Long2IntOpenHashMap(len);
        map1.defaultReturnValue(-1);
        return this;
    }
    
    public Hyb2IntMap initObjects(int len){
        map2 = new Object2IntOpenHashMap(len);
        map2.defaultReturnValue(-1);
        return this;
    }
    
    public int size(){
        return map1==null ? map2.size() : map1.size();
    }
    
    public int getInt(Object key){
        return map1==null ? map2.getInt(key) : map1.get((long)key);
    }
}
