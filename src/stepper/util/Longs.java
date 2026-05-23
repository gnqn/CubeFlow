package stepper.util;


import it.unimi.dsi.fastutil.longs.*;

public class Longs {
    public int size;
    public long[] data;
    public int[] idxes;
    public Long2IntMap loc2idx;
    
    public Longs(){}
    
    public Longs(int len){
        this.data = new long[len];
    }
    
    public Longs(long[] data){
        this.data = data;
        this.size = data.length;
    }
    
    public int rank(long loc){
        if(loc2idx!=null) return loc2idx.get(loc);
        for(int i=0; i<size; i++) if(this.data[i]==loc) return i;
        return -1;
    }
    
    public void add(long loc){
        if(this.data==null) this.data = new long[10000];
        if(size==this.data.length) expand(0.5);
        this.data[size++] = loc;
    }
    
    public void add(long loc, double p){
        if(this.data==null) this.data = new long[10000];
        if(size==this.data.length) expand(p);
        this.data[size++] = loc;
    }
    
    private void expand(double p){
        double ratio = p==0 ? 2.0 : 1/p;
        int len = (int)((ratio<1.15 ? 1.15 : ratio>20 ? 20 : ratio) * size);
        System.out.println("Expand:" + len);
        long[] temp = new long[len];
        System.arraycopy(this.data, 0, temp, 0, size);
        this.data = temp;
    }
    
    public void sort(){
        idxes = new int[this.size];
        for(int i=0; i<this.size; i++) idxes[i] = i;
        quickSortLong(0, size-1);
    }
    
    public void makingKeyMap(){
        if(loc2idx!=null) return;
        
        long cost = System.currentTimeMillis();
        loc2idx = new Long2IntOpenHashMap(size);
        loc2idx.defaultReturnValue(-1);
        for(int i=0; i<size; i++) loc2idx.put(data[i], i);
        System.out.println("Hashing: " + (System.currentTimeMillis()-cost) + "ms");
    }
    
    public static Longs newInstance(int len){
        Longs list = new Longs();
        list.data = new long[len];
        return list;
    }
    
    private void quickSortLong(int left, int right) {
        if (right - left < 32) {
            insertionSortLong(left, right);
            return;
        }
        
        // 三数取中法选择枢轴
        int mid = left + (right - left) / 2;
        if (data[idxes[left]] > data[idxes[mid]]) swap(left, mid);
        if (data[idxes[left]] > data[idxes[right]]) swap(left, right);
        if (data[idxes[mid]] > data[idxes[right]]) swap(mid, right);
        
        long pivot = data[idxes[mid]];
        int i = left, j = right;
        
        while (i <= j) {
            while (data[idxes[i]] < pivot) i++;
            while (data[idxes[j]] > pivot) j--;
            if (i <= j) {
                swap(i, j);
                i++;
                j--;
            }
        }
        
        if (left < j) quickSortLong(left, j);
        if (i < right) quickSortLong(i, right);
    }
    
    private void insertionSortLong(int left, int right) {
        for (int i = left + 1; i <= right; i++) {
            int keyIndex = idxes[i];
            long keyValue = data[keyIndex];
            int j = i - 1;
            
            while (j >= left && data[idxes[j]] > keyValue) {
                idxes[j + 1] = idxes[j];
                j--;
            }
            idxes[j + 1] = keyIndex;
        }
    }
    
    private void swap(int i, int j) {
        int temp = idxes[i];
        idxes[i] = idxes[j];
        idxes[j] = temp;
    }
}
