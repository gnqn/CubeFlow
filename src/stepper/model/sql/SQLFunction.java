package stepper.model.sql;

import java.util.*;
import java.util.regex.*;
import one.sys.*;
import stepper.util.*;
import stepper.model.*;
import it.unimi.dsi.fastutil.objects.*;

public class SQLFunction extends Attribute{
    protected int op;
    protected ArrayList params = new ArrayList();
    
    public SQLFunction(int op, String name){
        this.op = op;
        this.name = name;
    }
    
    public int op(){
        return this.op;
    }
    
    public ArrayList params(){
        return this.params;
    }
    
    @Override
    public boolean outFrom(Attribute input){
        for(Object obj: params){
            if(obj instanceof Attribute && ((Attribute)obj).outFrom(input)) return true;
        }
        return false;
    }
    
    public boolean undetermined(){
        return params.stream().anyMatch((param) -> (param instanceof Unknown));
    }
    
    public SQLFunction instancing(QNode node){
        return this;
    }
    
    @Override
    public Symbol inputSymbol(){
        Attribute attr = null;
        for(Object obj: params){
            if(!(obj instanceof Attribute)) continue;
            if(attr!=null) return null;
            attr = (Attribute)obj;
        }
        return attr==null ? null : attr.getSymbol();
    }
    
    @Override
    public Attribute getAttribute(){
        for(Object obj: params) if(obj instanceof Attribute) return (Attribute)obj;
        return null;
    }
    
    public ArrayList<Attribute> getAttributes(){
        ArrayList<Attribute> attrs = new ArrayList();
        for(Object obj: params) if(obj instanceof Attribute) attrs.add((Attribute)obj);
        return attrs;
    }
    
    public ArrayList<Symbol> getSymbols(){
        ArrayList<Symbol> attrs = new ArrayList();
        for(Object obj: params) if(obj instanceof Symbol) attrs.add((Symbol)obj);
        return attrs;
    }
    
    public DimensionSpace getDimensions(){
        DimensionSpace space = new DimensionSpace();
        for(Object obj: params){
            Dimension dim = obj instanceof Dimension ? (Dimension)obj : obj instanceof Attribute ? new Dimension((Attribute)obj) : null;
            if(dim!=null && !space.contains(dim)) space.add(dim);
        }
        return space;
    }
    
    @Override
    public boolean noneAttributes(){
        return params.stream().noneMatch((obj) -> (obj instanceof Attribute));
    }
    
    public String func(){
        return "";
    }
    
    @Override
    public String property(){
        return "";
    }
    
    public String marks(){
        return "";
    }
    
    protected String toHTML(String dimensions, String m1, String m2, Object param){
        if(isNestedFunction(param)) return ((SQLFunction)param).makingHTML(dimensions, m1, m2);
        return param.toString();
    }
    
    protected String toSQL(String dimensions, String m1, String m2, Object param){
        if(isNestedFunction(param)) return ((SQLFunction)param).makingSQL(dimensions, m1, m2);
        return param.toString();
    }
    
    protected boolean isNestedFunction(Object param){
        if(!(param instanceof SQLFunction)) return false;
        SQLFunction fun = (SQLFunction)param;
        return fun.name.length()==0;
    }
    
    protected String sequenceOfParams(){
        String seq = "";
        for(int i=0; i<params.size(); i++) seq += (i==0 ? "" : ",") + params.get(i);
        return seq;
    }
    
    protected boolean check(){return true;}
    
    protected SQLFunction setParams(ArrayList params)throws SYSException{
        this.params = params;
        return this;
    }
    
    protected SQLFunction makingSymbol(){
        return this;
    }
    
    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof SQLFunction)) return false;
        SQLFunction f = (SQLFunction)obj;
        return this.op==f.op && this.params.equals(f.params);
    }
    
    public boolean isCount(){
        return this.op==SQLItem.Operator.COUNT;
    }
    
    public boolean isDistinct(){
        return this.op==SQLItem.Operator.COUNTDISTINCT;
    }
    
    public boolean isAvg(){
        return this.op==SQLItem.Operator.AVG;
    }
    
    public boolean isQuantile(){
        return this.op==SQLItem.Operator.QUANTILE || 
               this.op==SQLItem.Operator.QUANTILE2 || 
               this.op==SQLItem.Operator.QUANTILE3;
    }
    
    public boolean isUnkey(){
        return false;
    }
    
    public double compute(double value){return 0;}
    
    public void compute(double[] values){}
    
    public void compute(BitSet bits, double[] values){}
    
    public void compute(BitSet bits, Condition cd, double[] values){}
    
    public double aggregate(BitSet bits, double[] values){return 0;}
        
    public void trans(Object[] dims, int i, BitSet[] marks, Object[][] elements, Object2IntMap[] maps, int[][] bases){}
    
    public void trans(int i, Object[][] dims, BitSet[] marks, Hyb2IntMap[] maps, int[][] bases){}
    
    public static ArrayList<Attribute> parseAuxis(QNode input, String exp)throws SYSException{
        ArrayList<Attribute> list = new ArrayList();
        if(exp.length()==0) return list;
        String rule = "EXTRACT\\((?<p1>YEAR|MONTH) FROM (?<attr>[^\\)]+)\\) AS (?<as>[a-zA-Z][\\w\\_]*)";
        Pattern pattern = Pattern.compile(rule);
        Matcher matcher = pattern.matcher(exp.toUpperCase());
        while(matcher.find()){
            String p1 = exp.substring(matcher.start("p1"), matcher.end("p1"));
            String p2 = exp.substring(matcher.start("attr"), matcher.end("attr"));
            String as = exp.substring(matcher.start("as"), matcher.end("as"));
            ArrayList params = new ArrayList();
            params.add(p1);
            Attribute attr = input.getAttribute(p2);
            params.add(attr==null ? p2 : attr);
            list.add(SQLFunction.making(SQLItem.Operator.EXTRACT, as, params));
        }
        return list;
    }
    
    public static ArrayList<SQLFunction> parseTrans(QNode input, String exp)throws SYSException{
        ArrayList<SQLFunction> funcs = parseFunction(input, exp);
        return funcs.isEmpty() ? parseExpression(input, exp) : funcs;
    }
    
    public static ArrayList<SQLFunction> parseFunction(QNode input, String exp)throws SYSException{
        ArrayList<SQLFunction> funcs = new ArrayList();
        String rule = "(?<func>[a-zA-Z][\\w\\_]*)\\((?<params>('?[a-zA-Z]\\w*'?|\\-?[\\d\\.]+)?([ ]*,[ ]*('?[a-zA-Z]\\w*'?|\\-?[\\d\\.]+|\\-?[\\d\\.]+[ ]+[a-zA-Z]+))*)\\)";
        Pattern pattern = Pattern.compile(rule);
        Matcher matcher = pattern.matcher(exp);
        
        while(matcher.find()){
            String name = null;
            String[] params = matcher.group("params").split(",");
            ArrayList attrs = new ArrayList();
            for(String param: params){
                Attribute attr = input.getAttribute(param);
                attrs.add(attr==null ? param : attr);
                if(name==null && attr!=null) name = attr.name;
            }
            if(name==null) name = "m0";
            SQLFunction fun = SQLFunction.parsing(matcher.group("func"), name);
            if(fun!=null) funcs.add(fun.setParams(attrs));
        }
        return funcs;
    }
    
    public static ArrayList<SQLFunction> parseExpression(QNode input, String exp)throws SYSException{
        ArrayList params = new ArrayList();
        ArrayList<SQLFunction> funcs = new ArrayList();
        //simplified expression needed to be extended to include functions and brackets
        String rule = "^(?<attr>[a-zA-Z]\\w*|\\-?[\\d\\.]+)(?<items>[ ]*[+\\-\\\\*/][ ]*([a-zA-Z]\\w*|\\-?[\\d\\.]+))$";
        String subrule = "(?<op>[+\\-\\\\*/])[ ]*(?<attr>[a-zA-Z]\\w*|\\-?[\\d\\.]+)";
        Pattern pattern = Pattern.compile(rule);
        Matcher matcher = pattern.matcher(exp);
        if(!matcher.find()) return funcs;
        
        String name = "undefined";
        String param = matcher.group("attr");
        Attribute attr = input.getAttribute(param);
        params.add(attr==null ? param : attr);
        if(attr!=null) name = attr.name();
        
        matcher = Pattern.compile(subrule).matcher(matcher.group("items"));
        if(!matcher.find()) return funcs;
        
        param = matcher.group("attr");
        attr = input.getAttribute(param);
        params.add(attr==null ? param : attr);
        if(attr!=null) name = attr.name();
        
        SQLFunction fun = SQLFunction.parsing(matcher.group("op"), name);
        if(fun!=null) funcs.add(fun.setParams(params));
        return funcs;
    }
    
    public static SQLFunction parseProps(QNode input, String txt, String name)throws SYSException{
        String[] items = txt.split(",", -1);
        
        SQLFunction fun = parsing(items[0], name);
        if(fun==null) return new SQLNone();
        
        ArrayList params = new ArrayList();
        for(int i=1; i<items.length; i++){
            if(items[i].length()==0) continue;
            Attribute attr = input.getAttribute(items[i]);
            params.add(attr==null ? items[i] : attr);
        }
        return fun.setParams(params);
    }
    
    public static SQLFunction making(int op, String name, ArrayList params)throws SYSException{
        SQLFunction fun = making(op, name);
        return fun==null ? null : fun.setParams(params);
    }
    
    private static SQLFunction making(int op, String name){
        if(op==SQLItem.Operator.ADD) return new SQLArithmetic(SQLItem.Operator.ADD, name);
        if(op==SQLItem.Operator.MINUS) return new SQLArithmetic(SQLItem.Operator.MINUS, name);
        if(op==SQLItem.Operator.MULTIPLY) return new SQLArithmetic(SQLItem.Operator.MULTIPLY, name);
        if(op==SQLItem.Operator.DIVISION) return new SQLArithmetic(SQLItem.Operator.DIVISION, name);
        if(op==SQLItem.Operator.MINUSR) return new SQLArithmetic(SQLItem.Operator.MINUSR, name);
        if(op==SQLItem.Operator.DIVISIONR) return new SQLArithmetic(SQLItem.Operator.DIVISIONR, name);
        
        if(op==SQLItem.Operator.CONCAT) return new SQLArithmetic(SQLItem.Operator.CONCAT, name);
        if(op==SQLItem.Operator.CONCATL) return new SQLArithmetic(SQLItem.Operator.CONCATL, name);
        if(op==SQLItem.Operator.CONCATR) return new SQLArithmetic(SQLItem.Operator.CONCATR, name);
        
        if(op==SQLItem.Operator.RAW) return new SQLAggregation(SQLItem.Operator.RAW, name);
        if(op==SQLItem.Operator.SUM) return new SQLAggregation(SQLItem.Operator.SUM, name);
        if(op==SQLItem.Operator.COUNT) return new SQLAggregation(SQLItem.Operator.COUNT, name);
        if(op==SQLItem.Operator.AVG) return new SQLAggregation(SQLItem.Operator.AVG, name);
        if(op==SQLItem.Operator.MIN) return new SQLAggregation(SQLItem.Operator.MIN, name);
        if(op==SQLItem.Operator.MAX) return new SQLAggregation(SQLItem.Operator.MAX, name);
        if(op==SQLItem.Operator.COUNTDISTINCT) return new SQLAggregation(SQLItem.Operator.COUNTDISTINCT, name);
        
        if(op==SQLItem.Operator.RSUM) return new SQLRAggregation(SQLItem.Operator.RSUM, name);
        if(op==SQLItem.Operator.RAVG) return new SQLRAggregation(SQLItem.Operator.RAVG, name);
        if(op==SQLItem.Operator.RMAX) return new SQLRAggregation(SQLItem.Operator.RMAX, name);
        if(op==SQLItem.Operator.RMIN) return new SQLRAggregation(SQLItem.Operator.RMIN, name);
        
        if(op==SQLItem.Operator.QUANTILE) return new SQLQuantile(name);
        if(op==SQLItem.Operator.QUANTILE2) return new SQLQuantile2(name);
        if(op==SQLItem.Operator.QUANTILE3) return new SQLQuantile3(name);
        
        if(op==SQLItem.Operator.LN) return new SQLLn(name);
        if(op==SQLItem.Operator.LOG) return new SQLLog(name);
        if(op==SQLItem.Operator.POW) return new SQLPow(name);
        if(op==SQLItem.Operator.BINNING) return new SQLBinning(name);
        if(op==SQLItem.Operator.DATEADD) return new SQLDateAdd(name);
        if(op==SQLItem.Operator.DATEDIFF) return new SQLDateDiff(name);
        if(op==SQLItem.Operator.DATETRUNC) return new SQLDateTrunc(name);
        if(op==SQLItem.Operator.EXTRACT) return new SQLExtract(name);
        
        if(op==SQLItem.Operator.NTILE) return new SQLNTile(name);
        if(op==SQLItem.Operator.RANK) return new SQLRank(name);
        if(op==SQLItem.Operator.ROUND) return new SQLRound(name);
        
        if(op==SQLItem.NONE) return new SQLNone();
        
        return null;
    }
    
    private static SQLFunction parsing(String op, String name){
        if(op.equals("+")) return new SQLArithmetic(SQLItem.Operator.ADD, name);
        if(op.equals("-")) return new SQLArithmetic(SQLItem.Operator.MINUS, name);
        if(op.equals("*")) return new SQLArithmetic(SQLItem.Operator.MULTIPLY, name);
        if(op.equals("×")) return new SQLArithmetic(SQLItem.Operator.MULTIPLY, name);
        if(op.equals("/")) return new SQLArithmetic(SQLItem.Operator.DIVISION, name);

        if(op.equalsIgnoreCase("Add")) return new SQLArithmetic(SQLItem.Operator.ADD, name);
        if(op.equalsIgnoreCase("Minus")) return new SQLArithmetic(SQLItem.Operator.MINUS, name);
        if(op.equalsIgnoreCase("Product")) return new SQLArithmetic(SQLItem.Operator.MULTIPLY, name);
        if(op.equalsIgnoreCase("Divide")) return new SQLArithmetic(SQLItem.Operator.DIVISION, name);
        if(op.equalsIgnoreCase("MinusR")) return new SQLArithmetic(SQLItem.Operator.MINUSR, name);
        if(op.equalsIgnoreCase("DivideR")) return new SQLArithmetic(SQLItem.Operator.DIVISIONR, name);
        
        if(op.equalsIgnoreCase("RAW")) return new SQLAggregation(SQLItem.Operator.RAW, name);
        if(op.equalsIgnoreCase("SUM")) return new SQLAggregation(SQLItem.Operator.SUM, name);
        if(op.equalsIgnoreCase("COUNT")) return new SQLAggregation(SQLItem.Operator.COUNT, name);
        if(op.equalsIgnoreCase("AVG")) return new SQLAggregation(SQLItem.Operator.AVG, name);
        if(op.equalsIgnoreCase("MIN")) return new SQLAggregation(SQLItem.Operator.MIN, name);
        if(op.equalsIgnoreCase("MAX")) return new SQLAggregation(SQLItem.Operator.MAX, name);
        if(op.equalsIgnoreCase("COUNTDISTINCT")) return new SQLAggregation(SQLItem.Operator.COUNTDISTINCT, name);
        
        if(op.equalsIgnoreCase("RSUM")) return new SQLRAggregation(SQLItem.Operator.RSUM, name);
        if(op.equalsIgnoreCase("RAVG")) return new SQLRAggregation(SQLItem.Operator.RAVG, name);
        if(op.equalsIgnoreCase("RMAX")) return new SQLRAggregation(SQLItem.Operator.RMAX, name);
        if(op.equalsIgnoreCase("RMIN")) return new SQLRAggregation(SQLItem.Operator.RMIN, name);
        
        if(op.equalsIgnoreCase("QUANTILE")) return new SQLQuantile(name);
        if(op.equalsIgnoreCase("QUANTILE2")) return new SQLQuantile2(name);
        if(op.equalsIgnoreCase("QUANTILE3")) return new SQLQuantile3(name);
        
        if(op.equalsIgnoreCase("LN")) return new SQLLn(name);
        if(op.equalsIgnoreCase("LOG")) return new SQLLog(name);
        if(op.equalsIgnoreCase("POW")) return new SQLPow(name);
        if(op.equalsIgnoreCase("BINNING")) return new SQLBinning(name);
        if(op.equalsIgnoreCase("DATE_ADD")) return new SQLDateAdd(name);
        if(op.equalsIgnoreCase("DATE_DIFF")) return new SQLDateDiff(name);
        if(op.equalsIgnoreCase("DATE_TRUNC")) return new SQLDateTrunc(name);
        if(op.equalsIgnoreCase("EXTRACT")) return new SQLExtract(name);
        
        if(op.equalsIgnoreCase("NTILE")) return new SQLNTile(name);
        if(op.equalsIgnoreCase("RANK")) return new SQLRank(name);
        if(op.equalsIgnoreCase("ROUND")) return new SQLRound(name);
    
        return null;
    }
}
