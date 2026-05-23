package stepper.model.engine;

import stepper.model.*;
import stepper.model.sql.*;

public class Picker extends Action{
    protected int[] idxes;
    
    public Picker(QNode node, Loader input){
        super(node.name(), node.getDimensions(), node.measures(), input);
        idxes = new int[this.measures.size()];
        for(int i=0; i<ords.length; i++) ords[i] = true;
        for(int i=0; i<idxes.length; i++) idxes[i] = input.addAttribute(this.measures.get(i));
    }
    
    @Override
    public boolean execute(boolean nosort){
        if(this.input.cube==null && !this.input.execute(nosort)) return false;
        for(Dimension dim: this.schema){
            int idx1 = input.cube.dimOf(dim.name());
            if(idx1!=-1) dim.typ = input.cube.schema.get(idx1).typ;
        }
        this.cube = input.cube.building(schema.size(), schema, this.measures);
        this.cube.pick(idxes, this.input.cube);
        this.pool = new Pool(this, null, 0);
        return true;
    }
}
