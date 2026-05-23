package stepper.util;

public class Ints {
    public int size;
    public int[] ints;
    
    public Ints(){
        ints = new int[10];
    }
    
    public Ints(int len){
        ints = new int[len==0 ? 10 : len];
    }
    
    public void add(int key){
        if(size==ints.length) expand(1);
        ints[size++] = key;
    }
    
    public void add(int k1, int k2){
        if(size+2>ints.length) expand(1);
        ints[size++] = k1;
        ints[size++] = k2;  
    }
    
    public void add(int k1, int k2, int k3, int k4){
        if(size+4>ints.length) expand(1);
        ints[size++] = k1;
        ints[size++] = k2;
        ints[size++] = k3;
        ints[size++] = k4;
    }
    
    public void add(double p, int key){
        if(size==ints.length) expand(p);
        ints[size++] = key;
    }
    
    public int get(int idx){
        return ints[idx];
    }
    
    public boolean isEmpty(){
        return size==0;
    }
    
    public int size(){
        return this.size;
    }
    
    public void reset(){
        this.size = 0;
    }
    
    public int[] data(){
        return this.ints;
    }
    
    public int[] toArray(){
        if(size==ints.length) return ints;
        int[] array = new int[size];
        System.arraycopy(ints, 0, array, 0, size);
        return array;
    }
    
    public boolean contains(int k){
        for(int i=0; i<size; i++) if(ints[i]==k) return true;
        return false;
    }
    
    public int indexOf(int k){
        for(int i=0; i<size; i++) if(ints[i]==k) return i;
        return -1;
    }
    
    protected void expand(double p){
        double ratio = p==0 ? 2.0 : 1/p;
        int growth = (int)((ratio<1.15 ? 1.15 : ratio) * this.size);
        growth = growth>3000000 ? 3000000 : growth<10 ? 10 : growth;
        int[] temp = new int[ints.length + growth];
        System.arraycopy(ints, 0, temp, 0, size);
        this.ints = temp;
    }
}
