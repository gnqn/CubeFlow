package stepper.model.sql;

import java.util.*;
import one.sys.*;

public class SQLNone extends SQLFunction{
    public SQLNone(){
        super(SQLItem.NONE, "");
    }
    
    @Override
    protected SQLFunction setParams(ArrayList params)throws SYSException{
        return this;
    }
}
