package stepper.model.engine.flows;

import java.util.*;
import stepper.model.engine.*;

public class ActionFlow  extends ArrayList<Action>{
    public double[] y;
    public Action pumper;
    protected ActionFlow inflow;
        
    public ActionFlow(){}
    
    public ActionFlow(Action act){
        this.add(act);
    }
    
    public ActionFlow(ActionFlow flow, Action act){
        this.addAll(flow);
        this.add(act);
    }
    
    public Action input(){
        return this.isEmpty() ? (inflow==null ? pumper : inflow.end()) : this.get(0).input();
    }
    
    public Action end(){
        return this.isEmpty() ? (inflow==null ? pumper : inflow.end()) : this.get(this.size()-1);
    }
    
    public ActionFlow right(Action act){
        int idx = this.indexOf(act);
        for(int i=0; i<idx+1; i++) this.remove(0);
        return this;
    }
    
    public final ActionFlow flowTo(int to){
        ActionFlow flow = new ActionFlow();
        for(int i=0; i<to; i++) flow.add(this.get(i));
        return flow;
    }
    
    public final ActionFlow flowFrom(int from){
        ActionFlow flow = new ActionFlow();
        for(int i=from; i<this.size(); i++) flow.add(this.get(i));
        return flow;
    }
    
    public final ActionFlow flow(int from, int to){
        ActionFlow flow = new ActionFlow();
        for(int i=from; i<to; i++) flow.add(this.get(i));
        return flow;
    }
    
    public Action common(ActionFlow flow){
        Action comm = null;
        for(Action act: flow){
            if(this.indexOf(act)==-1) break;
            comm = act;
        }
        return comm;
    }
    
    public void setInflow(ActionFlow flow){
        this.inflow = flow;
    }
    
    public static ActionFlow making(Action act){
        ActionFlow flow = new ActionFlow();
        while(!(act instanceof Picker) && !(act instanceof Join)){
            flow.add(0, act);
            act = act.input();
        }
        return flow;
    }
    
    protected boolean init(){
        int max = 0;
        for(int i=0; i<this.size(); i++){
            int arity = 0;
            if(i==0) arity = 0;
            if(i==this.size()-1) arity = this.get(i).getSchema().size();
            //else if(pipe.get(i) instanceof Join) arity = pipe.get(i).blockingDegree();
            this.get(i).makingPool(arity, true);
            if(this.get(i).cube.measures.length>max) max = this.get(i).cube.measures.length;
        }
        this.y = new double[max];
        return true;
    }
    
    public boolean execute(boolean nosort){
        if(this.isEmpty()) return this.input()==null || this.input().execute(nosort);
        if(this.input().cube==null && !this.input().execute(nosort)) return false;
        for(Action act: this) if(!act.execute(nosort)) return false;
        return true;
    }
    
    public void push(int i, double p){
        if(i==this.size()-1) this.get(i).pool.makeCell(y);
        else this.get(++i).pool.push(this, i, p);
    }
    
    public void flush(int i, double p){
        if(i==this.size()-1) this.get(i).pool.makeCell(y);
        else this.get(++i).pool.flush(this, i, p);
    }
    
    public void flush(int i){
        if(i!=this.size()-1) this.get(++i).pool.flush(this, i);
    }
    
    public ArrayList<ActionFlow> makingPipes(){
        return makingPipes(true, new ArrayList());
    }
    
    public ArrayList<ActionFlow> makingPipes(boolean deep, ArrayList<ActionFlow> flows){
        ActionFlow flow = new PipeFlow();
        if(deep && this.inflow!=null) this.inflow.makingPipes(deep, flows);
        for(Action act: this){
            if(!act.composable()){
                flow.add(act);
                flow.addTo(flows);
                flow = new PipeFlow();
            }else{
                flow.add(act);
            }
        }
        if(!flow.isEmpty()) flow.addTo(flows);
        if(this.isEmpty()) flows.add(new PipeFlow(this));
        return flows;
    }
    
    void addTo(ArrayList<ActionFlow> flows){
        if(flows.isEmpty()){flows.add(this); return;}
        if(this.linkTo(flows)) return;
        
        ActionFlow last = flows.get(flows.size()-1);
        Action comm = this.common(last);
        if(comm==null && this.input()==last.input()) comm = this.input();
        if(comm==null){flows.add(this); return;}
        
        if(this.end().input2()==last.end()){
            flows.remove(flows.size()-1);
            if(last instanceof PipeJoin) flows.add(new PipeMultiJoin(comm, this, (PipeJoin)last));
            else flows.add(new PipeJoin(comm, this, last));
        }else{
            flows.add(this);
        }
    }
    
    private boolean linkTo(ArrayList<ActionFlow> flows){
        ActionFlow last = flows.get(flows.size()-1);
        if(this.input()!=last.end()) return false;
        if(!last.composable()) return false;
        if(this instanceof PipeJoin){
            if(last instanceof PipeJoin){
                last.setNext((PipeJoin)this);
            }else{
                flows.remove(last);
                ((PipeJoin)this).left.prepend(last);
                flows.add(this);
            }
        }else{
            last.addAll(this);
        }
        return true;
    }
    
    void prepend(ActionFlow flow){
        for(int i=flow.size()-1; i>=0; i--) this.add(0, flow.get(i));
    }
    
    public void append(ActionFlow flow){
        this.addAll(flow);
    }
    
    public int blockNum(){
        int num = 0;
        for(Action act: this) if(act.isBlocking()) num++;
        return num;
    }
    
    public boolean composable(){
        return this.end().composable();
    }
    
    public boolean allComposable(){
        for(Action act: this) if(!act.composable()) return false;
        return true;
    }
    
    boolean isotonic(Action node){
        int k = node==null || node==this.input() ? 0 : -1;
        for(int i=0; i<this.size(); i++){
            if(k==-1 && this.get(i)==node) k = i;
            if(k==-1 && this.get(i)!=node) continue;
            if(!this.get(i).isotonic()) return false;
        }
        return true;
    }
    
    public void setNext(JoinFlow flow){}
    
    protected ArrayList<ActionFlow> inflows(){
        ArrayList<ActionFlow> list = new ArrayList();
        list.add(this);
        ActionFlow flow = this;
        while(flow.inflow!=null){
            list.add(0, flow.inflow);
            flow = flow.inflow;
        }
        return list;
    }
}