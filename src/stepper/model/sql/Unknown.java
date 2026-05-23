package stepper.model.sql;


public class Unknown extends Attribute{
    public Unknown(Symbol symbol){
        this.name = "?";
        this.symbol = symbol;
    }
}
