package stepper.util;


public class Pack {
    public int size;
    public int[] x;
    public int[][] xx;
    public double[] y;
    public double[][] yy;
    
    private int[] cols;
    
    public Pack(int w1, int w2){
        this(w1, w2, 0, null);
    }
    
    public Pack(int w1, int w2, int len){
        this(w1, w2, len, null);
    }
    
    public Pack(int w1, int w2, int len, int[] cols){
        if(len<1000) len = 1000;
        x = new int[w1];
        y = new double[w2];
        xx = new int[w1][len];
        yy = new double[w2][len];
        this.cols = cols;
    }
    
    public int merge(int from, Pack pack){
        if(this.size==0){
            int r = pack.spacingTo(from);
            if(r==-1) append(pack);
            else append(from, r-from, pack);
            return r;
        }
        
        int r = this.spacingTo(from, pack);
        if(r==0) return r;
        if(r==-1) append(pack);
        else append(from, r-from, pack);
        return r;
    }
    
    public boolean notAlign(int[] pos){
        if(size!=0) for(int i=0; i<cols.length; i++) if(pos[cols[i]]!=xx[cols[i]][0]) return true;
        return false;
    }
    
    public void reset(){
        size = 0;
    }
    
    private int spacingTo(int b){
        int r = b;
        while(++r<size) for(int i=0; i<x.length; i++) if(xx[i][r]!=xx[i][b]) return r;
        return -1;
    }
    
    private int spacingTo(int b, Pack pack){
        int r=-1, k=pack.size;
        while(++r<size) for(int i=0; i<x.length; i++) if(xx[i][r]!=pack.xx[i][k]) return r;
        return -1;
    }
    
    public void add(int[] pos, double[] v){
        if(xx[0].length==size) expand(0.5);
        for(int i=0; i<x.length; i++) xx[i][size] = pos[i];
        for(int i=0; i<y.length; i++) yy[i][size] = v[i];
        size++;
    }
    
    public void add(double p, int[] pos, double[] v){
        if(xx[0].length==size) expand(p);
        for(int i=0; i<x.length; i++) xx[i][size] = pos[i];
        for(int i=0; i<y.length; i++) yy[i][size] = v[i];
        size++;
    }
    
    public void append(Pack pack){
        append(0, pack.size, pack);
    }
    
    public void appendHeadOf(Pack pack, int to){
        append(0, to, pack);
    }
    
    public void appendTailOf(Pack pack, int from){
        append(from, pack.size-from, pack);
    }
    
    private void append(int from, int len, Pack pack){
        if(this.xx[0].length - this.size<len) expand(len);
        for(int i=0; i<x.length; i++) System.arraycopy(pack.xx[i], from, xx[i], size, len);
        for(int i=0; i<y.length; i++) System.arraycopy(pack.yy[i], from, yy[i], size, len);
        size += len;
    }
    
    public int[] getX(int r){
        for(int i=0; i<x.length; i++) x[i] = xx[i][r];
        return x;
    }
    
    public double[] getY(int r){
        for(int i=0; i<y.length; i++) y[i] = yy[i][r];
        return y;
    }
    
    protected void expand(double p){
        expand(growth(p));
    }
    
    protected void expand(int growth){
        int len = xx.length;
        for(int i=0; i<x.length; i++){
            int[] temp = new int[len + growth];
            System.arraycopy(xx[i], 0, temp, 0, size);
            xx[i] = temp;
        }
        for(int i=0; i<y.length; i++){
            double[] temp = new double[len + growth];
            System.arraycopy(yy[i], 0, temp, 0, size);
            yy[i] = temp;
        }
    }
    
    protected int growth(double p){
        double ratio = p==0 ? 2.0 : 1/p;
        int growth = (int)((ratio<1.15 ? 1.15 : ratio) * this.size);
        return growth>3000000 ? 3000000 : growth<1000 ? 10000 : growth;
    }
}
