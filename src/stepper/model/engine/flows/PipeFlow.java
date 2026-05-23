package stepper.model.engine.flows;

import stepper.model.engine.*;

public class PipeFlow extends ActionFlow{
    protected ActionFlow pipe;
    protected ActionFlow prefix;
    protected ActionFlow suffix;
    
    public PipeFlow(){}
    
    public PipeFlow(ActionFlow flow){
        this.addAll(flow);
        this.pumper = flow.pumper;
    }
    
    @Override
    public boolean execute(boolean nosort){
        if(this.isEmpty()) return this.input()==null || this.input().execute(nosort);
        if(this.input().cube==null && !this.input().execute(nosort)) return false;
        if(this.blockNum()==0){for(Action act: this) act.execute(nosort); return true;}
        
        long cost = System.currentTimeMillis();
        
        normalize();
        for(Action act: prefix) if(!act.execute(nosort)) return false;
        if(pipe.isEmpty()) return true;
        
        pipe.init();
        if(pipe.size()==1) pipe.get(0).execute(nosort);
        else pipe.get(0).pool.pumping(pipe);
        
        for(Action act: suffix) if(!act.execute(nosort)) return false;
        
        System.out.println("Pipe Flow: " + (System.currentTimeMillis() - cost) + "ms");
        return true;
    }
    
    private void normalize(){
        int from = -1, to = -1;
        for(int i=0; i<this.size(); i++){
            if(get(i) instanceof Trans) continue;
            to = i + 1;
            if(from==-1) from = i;
        }
        if(to==-1) to = this.size();
        if(from==-1) from = this.size();
        
        this.prefix = this.flowTo(from);
        this.suffix = this.flowFrom(to);
        this.pipe = this.flow(from, to);
    }
}
