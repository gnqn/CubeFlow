package stepper.util;

import java.util.*;
import stepper.model.sql.*;

public class Doubles {
    protected int size;
    protected double[] data;
    
    public Doubles(){
        data = new double[10];
    }
    
    public Doubles(int len){
        data = new double[len==0 ? 10 : len];
    }
    
    public void add(double v){
        if(size==data.length-1) expand(1);
        data[size++] = v;
    }
    
    public void readd(double v){
        this.size = 0;
        data[size++] = v;
    }
    
    public void add(double p, double v){
        if(size==data.length-1) expand(p);
        data[size++] = v;
    }
    
    public double[] toArray(){
        if(size==data.length) return data;
        double[] array = new double[size];
        System.arraycopy(data, 0, array, 0, size);
        return array;
    }
    
    protected void expand(double p){
        double ratio = p==0 ? 2.0 : 1/p;
        int growth = (int)((ratio<1.15 ? 1.15 : ratio) * this.size);
        growth = growth>3000000 ? 3000000 : growth<10 ? 10 : growth;
        double[] temp = new double[data.length + growth];
        System.arraycopy(data, 0, temp, 0, size);
        this.data = temp;
    }
    
    public void reset(){
        this.size = 0;
    }
    
    public int size(){
        return this.size;
    }
    
    public double[] data(){
        return this.data;
    }
    
    public boolean isEmpty(){
        return this.size==0;
    }
    
    public int[] ranks(){
        int[] orders = new int[this.size];
        Integer[] idxes = new Integer[this.size];
        for(int i=0; i<this.size; i++) idxes[i] = i;
        Arrays.sort(idxes, Comparator.comparingDouble(i -> data[i]));
        for(int i=0; i<this.size; i++) orders[idxes[i]] = i;
        return orders;
    }
    
    public static int[] ranks(int from, int to, double[] values){
        int[] orders = new int[to - from];
        Integer[] idxes = new Integer[to - from];
        for(int i=from; i<to; i++) idxes[i-from] = i;
        Arrays.sort(idxes, 0, to-from, Comparator.comparingDouble(i -> values[i]));
        for(int i=from; i<to; i++) orders[idxes[i-from]-from] = i - from;
        return orders;
    }
    
    public double aggregate(int op){
        if(size==0) return 0;
        
        switch(op){
            case SQLItem.Operator.COUNT:
                return size;
            case SQLItem.Operator.SUM:
                double sum = 0;
                for(int i=0; i<size; i++) sum += data[i];
                return sum;
            case SQLItem.Operator.AVG:
                sum = 0;
                for(int i=0; i<size; i++) sum += data[i];
                return sum/size;
            case SQLItem.Operator.MAX:
                double max = Double.MIN_VALUE;
                for(int i=0; i<size; i++) if(data[i]>max) max = data[i];
                return max;
            case SQLItem.Operator.MIN:
                double min = Double.MAX_VALUE;
                for(int i=0; i<size; i++) if(data[i]<min) min = data[i];
                return min;
            default:
                return 0;
        }
    }
}
