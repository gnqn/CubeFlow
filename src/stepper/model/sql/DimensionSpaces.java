package stepper.model.sql;

import java.util.*;

public class DimensionSpaces extends ArrayList<DimensionSpace>{
    
    public DimensionSpaces(){}
    
    public DimensionSpaces(DimensionSpace space){
        this.add(space);
    }
    
    public DimensionSpaces(ArrayList<DimensionSpace> list){
        this.addAll(list);
    }
    
    public DimensionSpaces(DimensionSpaces spaces){
        this.addAll(spaces);
    }
    
    public DimensionSpaces copy(){
        DimensionSpaces copy = new DimensionSpaces();
        this.stream().forEach((space) -> {copy.add(space.makingSpace());});
        return copy;
    }
    
    public void merge(DimensionSpaces spaces){
        spaces.stream().filter((space) -> (!this.contains(space))).forEach((space) -> {
            this.add(space);
        });
    }
    
    public DimensionSpaces pairingDimensions(DimensionSpace dims){
        this.stream().forEach((space) -> space.pairingDimensions(dims));
        return this;
    }
    
    public DimensionSpaces renaming(){
        this.stream().forEach((space) -> space.renaming());
        return this;
    }
    
    public DimensionSpaces pairing(){
        DimensionSpaces spaces = new DimensionSpaces();
        this.stream().forEach((space) -> spaces.add(space.pairing()));
        return spaces;
    }
    
    public DimensionSpace getSpace(String name){
        for(DimensionSpace space: this) if(space.getDimension(name)!=null) return space;
        return null;
    }
    
    public DimensionSpaces intersection(DimensionSpaces spaces){
        DimensionSpaces result = new DimensionSpaces();
        this.stream().filter((ds) -> (spaces.contains(ds))).forEach((ds) -> {result.add(ds);});
        return result;
    }
    
    public DimensionSpaces lessing(DimensionSpaces spaces){
        DimensionSpaces result = new DimensionSpaces();
        this.stream().filter((ds) -> (spaces.hasLargeThan(ds))).forEach((ds) -> {result.add(ds);});
        return result;
    }
    
    public DimensionSpaces larging(DimensionSpaces spaces){
        DimensionSpaces result = new DimensionSpaces();
        this.stream().filter((ds) -> (spaces.hasLessThan(ds))).forEach((ds) -> {result.add(ds);});
        return result;
    }
    
    @Override
    public DimensionSpace[] toArray(){
        return this.toArray(new DimensionSpace[0]);
    }
    
    public int maxArity(){
        int max = 0;
        for(DimensionSpace ds: this) if(ds.arity()>max) max = ds.arity();
        return max;
    }
    
    public boolean hasLessThan(DimensionSpace ds){
        return ds.isEmpty() ? true : this.stream().anyMatch((_ds) -> (_ds.lessThan(ds)));
    }
    
    public boolean hasLargeThan(DimensionSpace ds){
        return ds.isEmpty() ? false : this.stream().anyMatch((_ds) -> (ds.lessThan(_ds)));
    }
    
    public ArrayList<Dimension> dimensions(){
        ArrayList<Dimension> dims = new ArrayList();
        for(DimensionSpace ds: this){
            for(Dimension dim: ds.getSpace()) if(!dims.contains(dim)) dims.add(dim);
        }
        return dims;
    }
    
    public static DimensionSpaces get(String name, DimensionSpace ds, HashMap<String, DimensionSpaces> results){
        DimensionSpaces result = results.get(name);
        if(result==null){
            result = new DimensionSpaces();
            result.add(ds);
            results.put(name, result);
        }
        return result;
    }
}
