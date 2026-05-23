package stepper.model.engine.flows;

public class Cell {
    public int[] x;
    public double[] y;
    
    public Cell(int w1, int w2){
        this.x = new int[w1];
        this.y = new double[w2];
    }
    
    public void set(int[] x, double[] y){
        System.arraycopy(x, 0, this.x, 0, this.x.length);
        System.arraycopy(y, 0, this.y, 0, this.y.length);
    }
}
