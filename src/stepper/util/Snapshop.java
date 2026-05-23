package stepper.util;


public class Snapshop {
    public int size;
    public int[] loc;
    public int[][] pos;
    
    public Snapshop(int rows, int cols){
        loc = new int[rows];
        pos = new int[rows][cols];
    }
    
    public void add(int[] pos, int loc){
        if(size==this.loc.length) expand(0.5);
        this.loc[size] = loc;
        int a2 = this.pos[0].length, a1 = pos.length - a2;
        for(int i=0; i<a2; i++) this.pos[size][i] = pos[a1+i];
        this.size++;
    }
    
    public void add(double p, int loc, int[] pos, int[] cols){
        if(size==this.loc.length) expand(0.5);
        this.loc[size] = loc;
        for(int i=0; i<cols.length; i++) this.pos[size][i] = pos[cols[i]];
        this.size++;
    }
    
    private void expand(double p){
        double ratio = p==0 ? 2.0 : 1/p;
        int len = (int)((ratio<1.15 ? 1.15 : ratio>20 ? 20 : ratio) * size);
        
        int[] temp = new int[len];
        System.arraycopy(loc, 0, temp, 0, size);
        loc = temp;
        
        int[][] temp2 = new int[len][pos[0].length];
        System.arraycopy(pos, 0, temp2, 0, size);
        pos = temp2;
    }
}
