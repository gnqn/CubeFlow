package stepper.model.engine.flows;

import stepper.model.engine.*;

public class PipeMultiJoin extends PipeJoin{
    public int[] x;
    public double y;
    
    public PipeMultiJoin(Action pumper, ActionFlow left, PipeJoin right){
        super(pumper, left, right);
    }
    
    @Override
    protected boolean init(){
        this.normalize();
        x  = new int[pumper.getSchema().arity()];
        for(int i=0; i<pipe.size(); i++){
            int arity = i==pipe.size()-1 ? (next==null ? this.get(i).getSchema().size() : 0) : 0;
            pipe.get(i).makingPool(arity, true);
        }
        return next==null || next.init();
    }
    
    @Override
    public boolean execute(boolean nosort){
        if(this.input().cube==null && !this.input().execute(nosort)) return false;
        if(!init()) return false;
        for(Action act: prefix) if(!act.execute(nosort)) return false;
        looping();
        if(next==null) for(Action act: suffix) if(!act.execute(nosort)) return false;
        return true;
    }
    
    @Override
    protected void looping(){
        if(pumper==pipe.input()) pumper.pool.loop(this);
        else pipe.get(0).pool.pumping(this);
    }
    
    @Override
    public void push(int i, double p){
        if(pumper==left.input()){
            System.arraycopy(pumper.pool.pos, 0, x, 0, x.length);
        }else if(pumper==pipe.get(i)){
            i++;
            System.arraycopy(pumper.pool.pos, 0, x, 0, x.length);
            //pumper.pool.reset();
        }else{
            //pipe.get(i++).pool.reset();
        }
        if(i<pipe.size()){
            pipe.get(i).pool.push(this, i, p);
        }else if(next!=null){
            next.looping();
        }
    }
}
