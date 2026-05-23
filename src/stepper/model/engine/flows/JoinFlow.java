package stepper.model.engine.flows;

import java.util.*;
import stepper.model.engine.*;

public class JoinFlow extends ActionFlow{
    public ActionFlow right;
    
    protected ActionFlow left;
    protected JoinFlow next;
    protected JoinFlow prev;
    
    protected ActionFlow pipe;
    protected ActionFlow prefix;
    protected ActionFlow suffix;
    
    JoinFlow(Action pumper, ActionFlow left){
        this(pumper, left, null);
    }
    
    public JoinFlow(Action pumper, ActionFlow left, ActionFlow right){
        this.left = left;
        this.right = right;
        this.pumper = pumper;
        this.inflow = left.inflow;
    }
    
    @Override
    public Action input(){
        return left.input();
    }
    
    @Override
    public Action end(){
        return next==null ? left.get(left.size()-1) : this.next.end();
    }
    
    public JoinFlow endFlow(){
        return next==null ? this : next.endFlow();
    }
    
    @Override
    public void setNext(JoinFlow flow){
        if(this.next==null){
            this.next = flow;
            flow.prev = this;
        }else{
            this.next.setNext(flow);
        }
    }
    
    @Override
    public boolean addAll(Collection flow){
        return this.next==null ? this.left.addAll(flow) : this.next.addAll(flow);
    }
    
    @Override
    public boolean execute(boolean nosort){
        if(this.input().cube==null && !this.input().execute(nosort)) return false;
        if(this.pumper==this.input()) return right.execute(nosort) && left.execute(nosort);
        for(Action act:left){
            if(!act.execute(nosort)) return false;
            if(act==this.pumper && !this.right.execute(nosort)) return false;
        }
        return true;
    }
    
    protected boolean post(boolean nosort){
        return true;
    }
    
    protected void looping(){}
    
    public void normalize(){
        int from = pumper==this.input() ? 0 : -1, to = -1;
        for(int i=0; i<left.size(); i++){
            if(from==-1 && left.get(i)==pumper){from = i; continue;}
            if(left.get(i) instanceof Trans) continue;
            to = i + 1;
            if(from==-1) from = i;
        }
        if(to==-1) to = left.size();
        if(from==-1) from = left.size();
        
        if(this.next==null && this.prev==null){
            this.prefix = left.flowTo(from);
            this.suffix = left.flowFrom(to);
            this.pipe = left.flow(from, to);
        }else if(this.next==null){
            this.pipe = left.flowTo(to);
            this.suffix = left.flowFrom(to);
            this.prefix = new ActionFlow();
        }else if(this.prev==null){
            this.pipe = left.flowFrom(from);
            this.prefix = left.flowTo(from);
            this.suffix = new ActionFlow();
        }else{
            this.pipe = left;
            this.prefix = new ActionFlow();
            this.suffix = new ActionFlow();
        }
        if(this.next!=null) this.next.normalize();
    }
    
    @Override
    public ArrayList<ActionFlow> makingPipes(boolean deep, ArrayList<ActionFlow> flows){
        if(this.pumper==null){
            int il, ir = -1;
            ArrayList<ActionFlow> rflows = right.inflows();
            ArrayList<ActionFlow> lflows = left.inflows();
            for(il=lflows.size()-1; il>=0; il--){
                ir = indexOf(lflows.get(il), rflows);
                if(ir!=-1) break;
            }
            if(ir==-1){
                right.makingPipes(true, flows);
                left.makingPipes(true, flows);
                return flows;
            }
            
            pumper = rflows.get(ir).end();
            rflows.get(ir).makingPipes(true, flows);
            ArrayList<ActionFlow> forks = new ArrayList();
            ArrayList<ActionFlow> rs = new ArrayList(), ls = new ArrayList();
            for(ir++; ir<rflows.size(); ir++) rflows.get(ir).makingPipes(false, rs);
            for(il++; il<lflows.size(); il++) lflows.get(il).makingPipes(false, ls);
            makingFork(flows, forks, rs);
            makingFork(flows, forks, ls);
            if(forks.size()<2){
                flows.addAll(forks);
                flows.addAll(rs);
                flows.addAll(ls);
            }else{
                new PipeFork(pumper, forks, rs, ls).addTo(flows);
            }
            return flows;
        }
        
        if(deep && inflow!=null) inflow.makingPipes(deep, flows);
        ActionFlow last = flows.isEmpty() ? null : flows.get(flows.size()-1);
        if(this.composable()){
            PipeJoin joinpipe = new PipeJoin(pumper, left, right);
            if(last instanceof PipeJoin){
                last.setNext(joinpipe);
                return flows;
            }
            if(last!=null && last.composable()) joinpipe.prepend(flows.remove(flows.size()-1));
            flows.add(joinpipe);
            return flows;
        }
        
        if(left.blockNum()==1 && right.blockNum()==0){
            ActionFlow flow2 = fullright(left, right);
            if(!flow2.isEmpty() || flow2.pumper!=null) flows.add(new PipeFlow(flow2));
            flows.add(new PipeFlow(shortleft(left)));
            return flows;
        }
        
        ArrayList<ActionFlow> flows2 = right.makingPipes(false, new ArrayList());
        if(left.size()==1){
            if(last!=null && last.end().composable()) last.append(flows2.remove(flows2.size()-1));
            flows.addAll(flows2);
            flows.add(new PipeFlow(left));
            return flows;
        }
        
        ArrayList<ActionFlow> flows1 = left.makingPipes(false, new ArrayList());
        if(flows1.size()==1) flows1.add(new PipeFlow(new ActionFlow(flows1.get(0).remove(flows1.get(0).size()-1))));
        makingFlow(flows, flows2.remove(0), flows1, flows2);
        return flows;
    }
    
    private void makingFlow(ArrayList<ActionFlow> flows, ActionFlow f2, ArrayList<ActionFlow> flows1, ArrayList<ActionFlow> flows2){
        ActionFlow f1 = flows1.remove(0);
        if(f1.input()!=pumper && !f1.contains(pumper)){
            if(f1.blockNum()==0) flows.add(f1); 
            else f1.addTo(flows);
            makingFlow(flows, f2, flows1, flows2);
        }
        
        ActionFlow last = flows.isEmpty() ? null : flows.get(flows.size()-1);
        if(f1.end()==pumper){
            if(last!=null && last.end().composable()) last.append(f1);
            else flows.add(f1);
            flows.add(f2);
            makingFlow(flows, flows1, flows2);
        }else if(f1.blockNum()==0 && f2.blockNum()==0){
            if(f1.end()==f2.input()){
                f1.addAll(f2);
                flows.add(f1);
            }else{
                flows.add(f1);
                flows.add(f2);
            }
            makingFlow(flows, flows1, flows2);
        }else if(f1.blockNum()==0){
            fullright(f1, f2).addTo(flows);
            flows.add(shortleft(f1));
            makingFlow(flows, flows1, flows2);
        }else if(f2.blockNum()==0){
            if(f1.end()==f2.input()){
                f1.addAll(f2);
                f1.addTo(flows);
            }else{
                f1.addTo(flows);
                flows.add(f2);
            }
            makingFlow(flows, flows1, flows2);
        }else{
            new PipeFork(pumper, f1, f2, flows1, flows2).addTo(flows);
        }
    }
    
    private void makingFlow(ArrayList<ActionFlow> flows, ArrayList<ActionFlow> flows1, ArrayList<ActionFlow> flows2){
        for(ActionFlow flow: flows2) if(!flow.isEmpty()) flows.add(flow);
        for(ActionFlow flow: flows1) if(!flow.isEmpty()) flows.add(flow);
    }
    
    private void makingFork(ArrayList<ActionFlow> flows, ArrayList<ActionFlow> forks, ArrayList<ActionFlow> list){
        ArrayList<ActionFlow> suffis = new ArrayList();
        ActionFlow fork = list.isEmpty() || list.get(0).input()!=pumper ? null : list.remove(0);
        while(fork!=null){
            if(fork.blockNum()==0) flows.add(fork);
            else forks.add(normalize(fork, suffis));
            fork = list.isEmpty() || list.get(0).input()!=pumper ? null : list.remove(0);
        }
        for(int i=suffis.size()-1; i>=0; i--) list.add(0, suffis.get(i));
    }
    
    private ActionFlow normalize(ActionFlow fork, ArrayList<ActionFlow> suffis){
        int to = fork.size()-1;
        PipeFlow suf = new PipeFlow();
        while(fork.get(to) instanceof Trans) suf.add(0, fork.remove(to--));
        if(!suf.isEmpty()) suffis.add(suf);
        return fork;
    }
    
    @Override
    public boolean composable(){
        if(pumper==null || !right.allComposable()) return false;
        if(!(left.end() instanceof Join)) return left.end().composable();
        if(!this.left.isotonic(pumper) || !this.right.isotonic(pumper)) return false;
        Join join = (Join)left.end();
        return !join.hasNEQ() && left.allComposable() && 
               join.input().getSchema().startsWith(join.getEQSchema());
               //join.getSchema().equals(join.input().getSchema());
    }
    
    
    @Override
    void prepend(ActionFlow flow){
        for(int i=flow.size()-1; i>=0; i--) left.add(0, flow.get(i));
    }
    
    @Override
    public void append(ActionFlow flow){
        if(next==null) this.left.addAll(flow);
        else next.append(flow);
    }
    
    private ActionFlow fullright(ActionFlow f1, ActionFlow f2){
        if(this.pumper==null || this.pumper==f1.input()) return f2;
        f2.add(0, this.pumper);
        while(f2.get(0).input()!=f1.input()) f2.add(0, f2.get(0).input());
        return f2;
    }
    
    private ActionFlow shortleft(ActionFlow f1){
        if(this.pumper==null) return f1;
        while(f1.get(0).input()!=pumper) f1.remove(0);
        return f1;
    }
    
    private static int indexOf(ActionFlow flow, ArrayList<ActionFlow> flows){
        for(int i=0; i<flows.size(); i++) if(flows.get(i)==flow) return i;
        return -1;
    }
}
