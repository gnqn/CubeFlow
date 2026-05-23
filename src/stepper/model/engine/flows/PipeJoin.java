package stepper.model.engine.flows;

import stepper.model.engine.*;

public class PipeJoin extends JoinFlow{
    
    public PipeJoin(PipeJoin join){
        super(join.pumper, join.left, join.right);
    }
    
    public PipeJoin(Action pumper, ActionFlow left, ActionFlow right){
        super(pumper, left, right);
    }
    
    @Override
    protected boolean init(){
        int max = 0;
        for(int i=0; i<pipe.size(); i++){
            int arity = 0;
            if(rightOfJoin(pipe.get(i))) for(Action act: right) act.makingPool(0, true);
            if(next==null){
                if(i==pipe.size()-1) arity = pipe.get(i).getSchema().size();
                else if(pipe.get(i) instanceof Join) arity = pipe.get(i).blockingDegree();
            }else if(pipe.get(i) instanceof Join) arity = pipe.get(i).blockingDegree();
            pipe.get(i).makingPool(arity, true);
            if(pipe.get(i).cube.measures.length>max) max = pipe.get(i).cube.measures.length;
        }
        this.y = new double[max];
        
        max = pumper.cube.measures.length;
        for(Action act: right) if(act.cube.measures.length>max) max = act.cube.measures.length;
        right.y = new double[max];
        
        return next==null || next.init();
    }
    
    @Override
    public boolean execute(boolean nosort){
        if(this.input().cube==null && !this.input().execute(nosort)) return false;
        
        long cost = System.currentTimeMillis();
        this.normalize();
        for(Action act: prefix) if(!act.execute(nosort)) return false;
        
        if(!init()) return false;
        if(next==null && pipe.size()==1) pipe.get(0).execute(nosort);
        else if(pumper==this.input()) pumper.pool.loop(this);
        else pipe.get(0).pool.pumping(this);
        if(!post(nosort)) return false;
        
        System.out.println("Pipe Flow: " + (System.currentTimeMillis() - cost) + "ms");
        return true;
    }
    
    @Override
    protected boolean post(boolean nosort){
        if(next!=null) next.post(nosort);
        for(Action act: endFlow().suffix) if(!act.execute(nosort)) return false;
        return true;
    }
    
    @Override
    public void push(int i, double p){
        if(i==pipe.size()-1){
            if(next!=null) next.push(-1, p); return;}
        
        if(i==-1 && this.prev!=null) System.arraycopy(this.prev.y, 0, y, 0, y.length);
        if((i==-1 && pipe.get(0).input()==pumper) || (i!=-1 && pumper==pipe.get(i))){
            System.arraycopy(y, 0, right.y, 0, right.y.length);
            if(right.isEmpty()) pumper.pool.makeCell(y);
            else right.get(0).pool.push(right, 0, p);
        }
        
        if(++i<pipe.size()) pipe.get(i).pool.push(this, i, p);
    }
    
    @Override
    public void flush(int i, double p){
        if(i==pipe.size()-1){if(next!=null) next.flush(-1, p); return;}
        
        if(i==-1 && this.prev!=null) System.arraycopy(this.prev.y, 0, y, 0, y.length);
        if((i==-1 && pipe.get(0).input()==pumper) || (i!=-1 && pumper==pipe.get(i))){
            System.arraycopy(y, 0, right.y, 0, right.y.length);
            if(right.isEmpty()) pumper.pool.makeCell(y);
            else right.get(0).pool.flush(right, 0, p);
        }
        
        if(++i<pipe.size()) pipe.get(i).pool.flush(this, i, p);
    }
    
    @Override
    public void flush(int i){
        if(i==pipe.size()-1){if(next!=null) next.flush(-1); return;}
        
        if((i==-1 && pipe.get(0).input()==pumper) || (i!=-1 && pumper==pipe.get(i))){
            if(!right.isEmpty()) right.get(0).pool.flush(right, 0);
        }
        
        if(++i<pipe.size()) pipe.get(i).pool.flush(this, i);
    }
    
    private boolean rightOfJoin(Action act){
        if(!(act instanceof Join)) return false;
        return act.input2()==right.end();
    }
}
