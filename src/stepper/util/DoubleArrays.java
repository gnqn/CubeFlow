package stepper.util;

import java.util.Arrays;


public class DoubleArrays {
    public int size;
    protected double[] qs;
    protected double[][] data;
    
    public DoubleArrays(double[] qs, int len){
        this.qs = qs;
        data = new double[qs.length][];
        for(int i=0; i<qs.length; i++) if(qs[i]!=0) data[i] = new double[len];
    }
    
    public void compute(double[] y){
        for(int i=0; i<data.length; i++){
            if(data[i]==null) continue;
            Arrays.sort(data[i], 0, size);
            double pos = qs[i] * (size + 1);
            if(pos<1) pos = 0;
            if(pos>=size) pos = size-1;
            int lowerPos = (int) Math.floor(pos) - 1; 
            double fraction = pos - Math.floor(pos);
            y[i] = data[i][lowerPos] + fraction * (data[i][lowerPos + 1] - data[i][lowerPos]);
        }
    }
    
    public void compute(int r, double[][] y){
        for(int i=0; i<data.length; i++){
            if(data[i]==null) continue;
            Arrays.sort(data[i], 0, size);
            double pos = qs[i] * (size + 1);
            if(pos<1) pos = 0;
            if(pos>=size) pos = size-1;
            int lowerPos = (int) Math.floor(pos) - 1; 
            double fraction = pos - Math.floor(pos);
            y[i][r] = data[i][lowerPos] + fraction * (data[i][lowerPos + 1] - data[i][lowerPos]);
        }
    }
    
    public void add(double p, double[] v){
        for(int i=0; i<data.length; i++){
            if(data[i]==null) continue;
            if(size==data[i].length) expand(p);
            data[i][size] = v[i];
        }
        size++;
    }
    
    public void add(double p, int r, double[][] v){
        for(int i=0; i<data.length; i++){
            if(data[i]==null) continue;
            if(size==data[i].length) expand(p);
            data[i][size] = v[i][r];
        }
        size++;
    }
    
    protected void expand(double p){
        double ratio = p==0 ? 2.0 : 1/p;
        int growth = (int)((ratio<1.15 ? 1.15 : ratio) * this.size);
        growth = growth>3000000 ? 3000000 : growth<10 ? 10 : growth;
        for(int i=0; i<data.length; i++){
            if(data[i]==null) continue;
            double[] temp = new double[data[i].length + growth];
            System.arraycopy(data[i], 0, temp, 0, size);
            this.data[i] = temp;
        }
    }
}
