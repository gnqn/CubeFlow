package stepper.model.engine;

import java.util.*;
import stepper.util.*;
import stepper.model.*;
import stepper.model.sql.*;
import it.unimi.dsi.fastutil.ints.*;

public class Join extends Action{
    protected Action input2;
    protected Condition joins;
    
    public Join(Join copy){
        super(copy.name, copy.schema, copy.measures, copy.input);
        this.joins = copy.joins;
        this.input2 = copy.input2;
    }
    
    public Join(QArith node, Action in, Action in2){
        super(node.name(), node.getDimensions(), node.measures(), in);
        this.input2 = in2;
        this.joins = new Condition(node.getJoins());
        if(this.joins.isEmpty()) this.joins = null;
    }
    
    @Override
    public Action input2(){
        return this.input2;
    }
    
    public boolean hasNEQ(){
        return joins!=null && joins.hasNEQ();
    }
    
    @Override
    public boolean composable(){
        return true;
    }
    
    public DimensionSpace getEQSchema(){
        if(joins==null) return null;
        ArrayList<Dimension> list = new ArrayList();
        DimensionSpace dims = joins.getEQDimensions();
        for(Dimension dim: this.schema) if(dims.getDimension(dim.name())!=null) list.add(dim);
        return new DimensionSpace(list);
    }
    
    @Override
    public boolean ordered(ArrayList<Dimension> dims){
        if(joins==null) return dims.size()==1;
        for(Dimension dim: dims){
            Predicate pd = joins.getPredicate(dim.name());
            if(pd==null || !pd.isEqualityPredicate()) return false;
        }
        return this.input.ordered(dims);
    }
    
    @Override
    public boolean makingPool(int blockings, boolean pipe){
        int arity = this.schema.size();
        Ints gcols1 = new Ints();
        Ints gcols2 = new Ints();
        this.cube = input.cube.building(arity, schema, measures, gcols1, gcols2, input2.cube);
        if(this.cube==null){
            System.out.println("Not support at current.");
            return false;
        }
        this.cube.marks = new BitSet[this.cube.arityS()];
        if(blockings!=0 && this.cube.g!=1) blockings++;
        
        Ints cols = new Ints(); 
        Ints cols1 = new Ints(); 
        Ints cols2 = new Ints();
        Ints ncols = new Ints();
        Ints ncols1 = new Ints();
        Ints ncols2 = new Ints();
        Ints scols = new Ints();
        Ints scols1 = new Ints();
        Ints scols2 = new Ints();
        Ints cc1 = new Ints();
        Ints cc2 = new Ints();
        Ints frees1 = new Ints();
        Ints frees2 = new Ints();
        
        IntSet v1 = new IntArraySet();
        HashSet<Predicate> visits = new HashSet();
        IntArrayList neq = new IntArrayList();
        
        int[] nums = new int[7];
        int[] c2g1 = input.cube.c2g();
        int[] c2g2 = input2.cube.c2g();
        ArrayList<int[]> xxs = new ArrayList();
        ArrayList<int[]> x2xs = new ArrayList();
        ArrayList<int[][]> lx2xs = new ArrayList();
        
        nums[5] = gcols1.size();
        nums[6] = gcols2.size();
        for(int i=0; i<arity; i++){
            Dimension d = this.schema.get(i);
            int idx1 = this.input.cube.dimOf(d.name()),
                idx2 = this.input2.cube.dimOf(d.name());
            if(idx1!=-1) d.typ = input.cube.schema.get(idx1).typ;
            else if(idx2!=-1) d.typ = input2.cube.schema.get(idx1).typ;
            
            Predicate pd = joins==null ? null : joins.getPredicate2(d.name());
            if(pd==null){
                if(idx1!=-1 && !v1.contains(idx1)){
                    nums[3]++;
                    cc1.add(i);
                    v1.add(idx1);
                    frees1.add(c2g1[idx1]);
                    if(!this.cube.pick(i, c2g1[idx1], d, null, this.input.cube)) return false;
                }else if(idx2!=-1){
                    nums[4]++;
                    cc2.add(i);
                    frees2.add(c2g2[idx2]);
                    if(!this.cube.pick(i, c2g2[idx2], d, null, this.input2.cube)) return false;
                }
                continue;
            }
            if(visits.contains(pd)) continue;
            
            visits.add(pd);
            Dimension d1 = (Dimension)pd.getAttribute();
            Attribute d2 = (Attribute)pd.getParameter();
            idx1 = this.input.cube.dimOf(d1.name());
            idx2 = this.input2.cube.dimOf(d2.name());
            int i2 = this.dimOf(d2.name(), i+1);
            if(idx1==-1){
                nums[4]++;
                cc2.add(i);
                frees2.add(c2g2[idx2]);
                if(!this.cube.pick(i, c2g2[idx2], d, null, this.input2.cube)) return false;
            }else if(idx2==-1){
                nums[3]++;
                cc1.add(i);
                frees1.add(c2g2[idx1]);
                if(!this.cube.pick(i, c2g2[idx1], d, null, this.input.cube)) return false;
            }else if(pd.isEqualityPredicate()){
                nums[0]++;
                cols.add(i);
                cols1.add(c2g1[idx1]);
                cols2.add(c2g2[idx2]);
                if(input2.cube.isLShadow(c2g2[idx2])){
                    nums[2]++;
                    scols.add(i);
                    scols1.add(c2g1[idx1]);
                    scols2.add(c2g2[idx2]);
                    if(!this.cube.pickL(i, c2g1[idx1], c2g2[idx2], xxs, lx2xs, input.cube, input2.cube)) return false;
                }else{
                    nums[1]++;
                    ncols.add(i);
                    ncols1.add(c2g1[idx1]);
                    ncols2.add(c2g2[idx2]);
                    if(!this.cube.pick(i, c2g1[idx1], c2g2[idx2], x2xs, this.input.cube, this.input2.cube)) return false;
                }
            }else{
                nums[3]++;
                neq.add(c2g1[idx1]);
                neq.add(c2g2[idx2]);
                neq.add(pd.op());
                cc1.add(i);
                frees1.add(c2g1[idx1]);
                if(!this.cube.pick(i, c2g1[idx1], d, null, this.input.cube)) return false;
                if(i2!=-1){
                    nums[4]++;
                    cc2.add(i2);
                    frees2.add(c2g2[idx2]);
                    if(!this.cube.pick(i2, c2g2[idx2], d, null, this.input2.cube)) return false;
                }
            }
        }
        
        int n = nums[0]*3 + nums[1]*3 + nums[2]*3 + nums[3]*2 + nums[4]*2 + nums[5] + nums[6];
        int[] idxes = new int[n];
        System.arraycopy(cols.data(), 0, idxes, 0, nums[0]);
        System.arraycopy(cols1.data(), 0, idxes, nums[0], nums[0]);
        System.arraycopy(cols2.data(), 0, idxes, nums[0]*2, nums[0]);
        System.arraycopy(ncols.data(), 0, idxes, nums[0]*3, nums[1]);
        System.arraycopy(ncols1.data(), 0, idxes, nums[0]*3+nums[1], nums[1]);
        System.arraycopy(ncols2.data(), 0, idxes, nums[0]*3+nums[1]*2, nums[1]);
        System.arraycopy(scols.data(), 0, idxes, nums[0]*3+nums[1]*3, nums[2]);
        System.arraycopy(scols1.data(), 0, idxes, nums[0]*3+nums[1]*3+nums[2], nums[2]);
        System.arraycopy(scols2.data(), 0, idxes, nums[0]*3+nums[1]*3+nums[2]*2, nums[2]);
        System.arraycopy(cc1.data(), 0, idxes, nums[0]*6, nums[3]);
        System.arraycopy(frees1.data(), 0, idxes, nums[0]*6+nums[3], nums[3]);
        System.arraycopy(cc2.data(), 0, idxes, nums[0]*6+nums[3]*2, nums[4]);
        System.arraycopy(frees2.data(), 0, idxes, nums[0]*6+nums[3]*2+nums[4], nums[4]);
        System.arraycopy(gcols1.data(), 0, idxes, nums[0]*6+nums[3]*2+nums[4]*2, nums[5]);
        System.arraycopy(gcols2.data(), 0, idxes, nums[0]*6+nums[3]*2+nums[4]*2+nums[5], nums[6]);
        
        this.kord = neq.isEmpty() && orderedEQ(cols, cols1, cols2);
        int[][] op = new int[this.measures.size()][3];
        for(int i=0; i<this.measures.size(); i++){
            SQLMeasure ms = (SQLMeasure)this.measures.get(i);
            op[i][0] = input.msOf(ms.m1());
            op[i][2] = input2.msOf(ms.m2());
            op[i][1] = op[i][0]==-1 ? SQLItem.Operator.CONCATR : op[i][2]==-1 ? SQLItem.Operator.CONCATL : ms.op();
        }
        
        this.pool = !pipe && neq.isEmpty() ? new JoinPool(this, nums, idxes, blockings, op, x2xs.toArray(new int[0][]), xxs.toArray(new int[0][]), lx2xs.toArray(new int[0][][])) :
                    !pipe && !neq.isEmpty() ? new JoinNeqPool(this, nums, idxes, blockings, op, x2xs.toArray(new int[0][]), xxs.toArray(new int[0][]), lx2xs.toArray(new int[0][][]), neq) :
                    pipe && neq.isEmpty() ? new JoinPipePool(this, nums, idxes, blockings, op, x2xs.toArray(new int[0][]), xxs.toArray(new int[0][]), lx2xs.toArray(new int[0][][])) :
                                            new JoinPipeNeqPool(this, nums, idxes, blockings, op, x2xs.toArray(new int[0][]), xxs.toArray(new int[0][]), lx2xs.toArray(new int[0][][]), neq);
        return true;
    }
    
    @Override
    public boolean execute(boolean nosort){
        long cost = System.currentTimeMillis();
        if(this.cube==null && !makingPool(this.schema.size(), false)) return false;
        this.pool.execute();
        System.out.println("Join(" + name + ") cost:\t" + (System.currentTimeMillis() - cost) + "ms \t(" + this.cube.cardinality() + " rows)");
        return this.cube.cardinality()>0;
    }
    
    @Override
    public int blockingDegree(){
        int arity = this.schema.size();
        for(int k=0; k<arity; k++) if(!withinEQ(this.schema.get(k))) return arity - (k==0 ? 1 : k);
        return 0;
    }
    
    private boolean withinEQ(Dimension d){
        if(joins==null) return false;
        for(Predicate pd: joins) if(pd.contains(d)) return true;
        return false;
    }
    
    private boolean orderedEQ(Ints cols, Ints cols1, Ints cols2){
        int m = 0;
        for(int i=0; i<cols.size; i++) if(cols.ints[i]!=cols1.ints[i] || cols.ints[i]!=cols2.ints[i]) return false;
        for(int i=0; i<cols.size; i++) if(cols.ints[i]>m) m = cols.ints[i];
        return m==cols.size-1;
    }
}
