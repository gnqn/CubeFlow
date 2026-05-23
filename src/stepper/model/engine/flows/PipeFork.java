package stepper.model.engine.flows;

import java.util.*;
import stepper.model.engine.*;

public class PipeFork extends PipeJoin{
    protected ArrayList<ActionFlow> forks = new ArrayList();
    protected ArrayList<ActionFlow> flows = new ArrayList();
    
    public PipeFork(Action pumper, ActionFlow left, ActionFlow right, ArrayList<ActionFlow> flows1, ArrayList<ActionFlow> flows2){
        super(pumper, left, right);
        for(ActionFlow flow: flows1) if(!flow.isEmpty()) flows.add(flow);
        for(ActionFlow flow: flows2) if(!flow.isEmpty()) flows.add(flow);
    }
    
    public PipeFork(Action pumper, ArrayList<ActionFlow> forks, ArrayList<ActionFlow> flows1, ArrayList<ActionFlow> flows2){
        super(pumper, forks.remove(0), forks.remove(0));
        this.forks.addAll(forks);
        for(ActionFlow flow: flows1) if(!flow.isEmpty()) flows.add(flow);
        for(ActionFlow flow: flows2) if(!flow.isEmpty()) flows.add(flow);
    }
    
    @Override
    protected boolean init(){
        for(ActionFlow flow: forks) flow.init();
        this.left.init();
        this.right.init();
        this.y = this.left.y;
        for(ActionFlow flow: flows) flow.init();
        return next==null || next.init();
    }
    
    @Override
    public boolean execute(boolean nosort){
        if(this.input().cube==null && !this.input().execute(nosort)) return false;
        
        long cost = System.currentTimeMillis();
        if(!init()) return false;
        if(pumper==this.input()) pumper.pool.loop(this);
        else left.get(0).pool.pumping(this);
        if(!post(nosort)) return false; 
        
        System.out.println("Pipe Flow: " + (System.currentTimeMillis() - cost) + "ms");
        return true;
    }
    
    @Override
    protected boolean post(boolean nosort){
        if(next!=null) next.post(nosort);
        for(ActionFlow flow: flows) flow.execute(nosort);
        return true;
    }
    
    @Override
    public void push(int i, double p){
        if(i==-1 && this.prev!=null) System.arraycopy(this.prev.y, 0, y, 0, y.length);
        if((i==-1 && left.get(0).input()==pumper) || (i!=-1 && pumper==left.get(i))){
            System.arraycopy(y, 0, right.y, 0, right.y.length);
            right.get(0).pool.push(right, 0, p);
            for(ActionFlow fork: forks){
                System.arraycopy(y, 0, fork.y, 0, fork.y.length);
                fork.get(0).pool.push(fork, 0, p);
            }
        }
        
        if(i==left.size()-1){if(next!=null) next.push(-1, p); return;}
        if(++i<left.size()) left.get(i).pool.push(this, i, p);
    }
    
    @Override
    public void flush(int i, double p){
        if(i==-1 && this.prev!=null) System.arraycopy(this.prev.y, 0, y, 0, y.length);
        if((i==-1 && left.get(0).input()==pumper) || (i!=-1 && pumper==left.get(i))){
            System.arraycopy(y, 0, right.y, 0, right.y.length);
            right.get(0).pool.flush(right, 0, p);
            for(ActionFlow fork: forks){
                System.arraycopy(y, 0, fork.y, 0, fork.y.length);
                fork.get(0).pool.flush(fork, 0, p);
            }
        }
        
        if(i==left.size()-1){if(next!=null) next.flush(-1, p); return;}
        if(++i<left.size()) left.get(i).pool.flush(this, i, p);
    }
    
    @Override
    public void flush(int i){
        if((i==-1 && left.get(0).input()==pumper) || (i!=-1 && pumper==left.get(i))){
            if(!right.isEmpty()) right.get(0).pool.flush(right, 0);
            for(ActionFlow fork: forks) fork.get(0).pool.flush(fork, 0);
        }
        
        if(i==left.size()-1){if(next!=null) next.flush(-1); return;}
        if(++i<left.size()) left.get(i).pool.flush(this, i);
    }
    
    @Override
    public Action end(){
        return next==null ? left().end() : this.next.end();
    }
    
    @Override
    public Action input(){
        return left.get(0).input();
    }
    
    @Override
    public boolean addAll(Collection flow){
        return this.left().addAll(flow);
    }
    
    @Override
    public boolean composable(){
        return end().composable();
    }
    
    private ActionFlow left(){
        return this.flows.get(this.flows.size()-1);
    }
}
