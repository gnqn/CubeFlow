package stepper.model.engine;

import java.util.*;
import stepper.model.*;
import stepper.model.sql.*;
import stepper.model.engine.flows.*;

public class ActionChart {
    protected Action out;
    protected ActionFlow flow;
    protected HashMap<QNode, Action> cubes = new HashMap();
    protected ArrayList<Loader> inputs = new ArrayList();
    
    protected boolean pipe;
    
    public ActionChart(QNode node, boolean pipe){
        HashMap<Action, ActionFlow> flows = pipe ? new HashMap() : null;
        this.pipe = pipe;
        this.out = makeAction(node, flows);
        if(flows==null) return;
        
        this.flow = flows.get(out);
        if(this.flow==null){
            this.flow = ActionFlow.making(out);
            this.flow.setInflow(flows.get(flow.input()));
        }
    }
    
    public void query(boolean nosort, int limit){
        long cost = System.currentTimeMillis();
        boolean ok = pipe ? pumping(nosort) : query(nosort, out);
        cost = System.currentTimeMillis() - cost;
        
        System.out.println("\n\n");
        out.cube.output(out, limit);
        System.out.println("first " + limit + " of " + out.cube.cardinality() + " rows.");
        System.out.println((pipe ? "Cube Piping Execution Cost: " : "Cube Execution One by One Cost: ") + cost + "ms");
    }
    
    private boolean query(boolean nosort, Action act){
        boolean ok = true;
        if(act.input!=null && act.input.cube==null) ok = query(nosort, act.input);
        if(ok && act.input2()!=null && act.input2().cube==null) ok = query(nosort, act.input2());
        return ok && (act.cube!=null || act.execute(nosort));
    }
    
    private boolean pumping(boolean nosort){
        ArrayList<ActionFlow> flows = flow.makingPipes();
        for(ActionFlow pipe: flows) if(!pipe.execute(nosort)) return false;
        return true;
    }
    
    private Action makeAction(QNode node, HashMap<Action, ActionFlow> flows){
        if(node==null || node instanceof QRoot) return null;
        
        Action input = makeAction(node.input(), flows);
        Action input2 = makeAction(node.input2(), flows);
        
        Action act = cubes.get(node);
        if(act==null){
            if(node instanceof QAgg) act = input==null ? makeRoot((QAgg)node) : new Aggregation((QAgg)node, input);
            else if(node instanceof QTra) act = new Trans((QTra)node, input);
            else if(node instanceof QArith) act = makeJoin((QArith)node, input, input2, flows);
            cubes.put(node, act);
        }
        return act;
    }
    
    private Action makeRoot(QAgg node){
        Loader loader = getRoot(node);
        if(loader!=null){
            DimensionSpace schema = loader.getSchema();
            if(schema.equals(node.getDimensions())) return new Picker(node, loader);
            if(schema.lessThan(node.getDimensions())) return new Picker(node, loader);
        }
        
        loader = new Loader(node, getShare(node));
        inputs.add(loader);
        return new Picker(node, loader);
    }
    
    private Loader getRoot(QAgg node){
        Condition cond = node.getCondition();
        if(cond!=null && cond.isEmpty()) cond = null;
        DimensionSpace space = node.getDimensions();
        for(Loader cube: inputs){
            if(cube.getRoot()!=node.input()) continue;
            //if(!cube.getSchema().porder(space)) continue;
            if(!cube.getSchema().equals(space)) continue;
            Condition filter = cube.getFilter();
            if(cond==null && filter==null) return cube;
            if(cond!=null && cond.equals(filter)) return cube;
        }
        return null;
    }
    
    private Loader getShare(QAgg node){
        int max = 0;
        Loader share = null;
        Condition cond = node.getCondition();
        if(cond!=null && cond.isEmpty()) cond = null;
        DimensionSpace space = node.getDimensions();
        for(Loader input: inputs){
            if(input.getRoot()!=node.input()) continue;
            Condition filter = input.getFilter();
            if((cond==null && filter==null) || (cond!=null && cond.equals(filter))){
                int c = input.schema.commons(space).arity();
                if(c>max){
                    max = c;
                    share = input;
                }
            }
        }
        return share;
    }
    
    private Action makeJoin(QArith node, Action input, Action input2, HashMap<Action, ActionFlow> flows){
        Join join = new Join(node, input, input2);
        if(flows==null) return join;
        
        ActionFlow left = ActionFlow.making(input);
        ActionFlow right = ActionFlow.making(input2);
        left.add(join);
        
        Action act = left.common(right);
        if(act!=null) right.right(act);
        if(act==null && left.input()==right.input()) act = left.input();
        if(act==null && right.isEmpty() && left.input()==input2) act = left.input();
        
        left.setInflow(flows.get(left.input()));
        if(right.isEmpty() && input2 instanceof Join) right = flows.get(input2);
        else if(right.isEmpty()) right.pumper = input2;
        else if(act==null) right.setInflow(flows.get(right.input()));
        flows.put(join, new JoinFlow(act, left, right));
        
        return join;
    }
}
