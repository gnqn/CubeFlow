package stepper.model.sql;

import java.util.*;
import java.util.regex.*;

import stepper.model.*;

public class SQLItem {
    public static final int NONE = 0;
    public static final int NULL = -1;
    public static final String EMPTY = "_";
    public static String COMMON_SPLITOR = "♫";
    public static String AND_SPLITOR = "∧";
    public static String SUB_SPLITOR = "❊";
    
    
    public static String makingProperty(int typ){
        if(typ==SQLItem.Cell.NOTE) return "New Note";
        if(typ==SQLItem.Cell.AGG) return DEFAULT_DS + COMMON_SPLITOR + DEFAULT_MS + COMMON_SPLITOR;
        if(typ==SQLItem.Cell.TRANS) return "" + COMMON_SPLITOR;
        if(typ==SQLItem.Cell.MATH) return NONE + "";
        return "";
    }
    
    public static Condition parseCondition(QNode input, QNode input2, String cond)throws Exception{
        ArrayList<Predicate> ands = new ArrayList();
        for(String[] items: parsePredicates(cond)){
            Attribute attr1 = input==null ? null : input.getAttribute(items[0]);
            if(attr1==null) attr1 = input2==null ? null : input2.getAttribute(items[0]);
            if(attr1==null) throw new Exception("There is no attribute " + items[0] + " in " + cond);
            ands.add(new Predicate(attr1, Comparator.parse(items[1]), items[2]));
        }
        return new Condition(ands);
    }
    
    public static ArrayList<String[]> parsePredicates(String txt){
        ArrayList<String[]> predicates = new ArrayList();
        txt = " and " + txt.trim();
        String rule = "[ ]+and[ ]+(?<predicate>" + Comparator.getRegex() + ")";
        Pattern pattern = Pattern.compile(rule);
        Matcher matcher = pattern.matcher(txt.toLowerCase());
        while(matcher.find()){
            int start = matcher.start("predicate");
            int end = matcher.end("predicate");
            predicates.add(parsePredicate(txt.substring(start, end)));
        }
        return predicates;
    }
    
    public static String[] parsePredicate(String txt){
        String[] items = new String[3];
        String rule = "(?<attr>[^!>< ]+)(?<op>[ ]*(!=|>=|<=|>|<|=)[ ]*|[ ]+in[ ]*|[ ]+between[ ]+)(?<value>.+)$";
        Pattern pattern = Pattern.compile(rule);
        Matcher matcher = pattern.matcher(txt.trim().toLowerCase());
        if(matcher.find()){
            int start = matcher.start("attr");
            int end = matcher.end("attr");
            items[0] = txt.substring(start, end);
            start = matcher.start("op");
            end = matcher.end("op");
            items[1] = txt.substring(start, end).trim();
            start = matcher.start("value");
            end = matcher.end("value");
            items[2] = txt.substring(start, end).trim();
        }
        return items;
    }
    
    public static DimensionSpaces parseInputs(String dims, DimensionSpace all){
        DimensionSpaces inputs = new DimensionSpaces();
        for(String item: dims.split(SQLItem.COMMON_SPLITOR, -1)){
            if(item.length()==0){
                inputs.add(new DimensionSpace());
                continue;
            }
            ArrayList<Dimension> list = new ArrayList();
            for(String name: item.split(",")) list.add(all.getDimension(name));
            inputs.add(new DimensionSpace(list));
        }
        return inputs;
    }
    
    public static class Comparator{
        public static final int GREATER=1;
        public static final int LESS=2;
        public static final int EQUAL=3;
        public static final int GREATERE = 4;
        public static final int LESSE = 5;
        public static final int BETWEEN = 6;
        public static final int IN = 7;
        public static final int NEQUAL = 8;
        public static final String[] Operators = {"", ">", "<", "=", ">=", "<=", "BETWEEN", "IN", "!="};
        
        public static int parse(String op){
            for(int i=0; i<Operators.length; i++) if(op.equalsIgnoreCase(Operators[i])) return i;
            return NONE;
        }
        public static String toOperator(int op, boolean html){
            if(op<0 || op>=Operators.length) op = 0;
            String operator = (html && op==2) ? "&lt;" : Operators[op];
            if(op==BETWEEN || op==IN){
                if(html) operator = " <span>" + operator + "</span> ";
                else operator = " " + operator + " ";
            }
            return operator;
        }
        public static String getRegex(){
            return "[^!>< ]+(([ ]*(!=|>=|<=|>|<|=)[ ]*[^ ]+)|[ ]+in[ ]*\\([^\\)]+\\)|[ ]+between[ ]+[^ ]+[ ]+and[ ]+[^ ]+)";
        }
    }
    
    public static final class Operator{
        public static final int SUM = 1;
        public static final int AVG = 2;
        public static final int MAX = 3;
        public static final int MIN = 4;
        public static final int COUNT = 5;
        public static final int RAVG = 9;
        public static final int RMAX = 17;
        public static final int RMIN = 18;
        public static final int RSUM = 19;
        public static final int RAW = 10;
        public static final int QUANTILE = 6;
        public static final int QUANTILE2 = 7;
        public static final int QUANTILE3 = 8;
        public static final int COUNTDISTINCT = 19;
        
        public static final int ADD = 11;
        public static final int MINUS = 12;
        public static final int MULTIPLY = 13;
        public static final int DIVISION = 14;
        public static final int MINUSR = 15;
        public static final int DIVISIONR = 16;
        
        public static final int CONCAT = 36;
        public static final int CONCATL = 37;
        public static final int CONCATR = 38;
        
        public static final int POW = 22;
        public static final int RANK = 23;
        public static final int NTILE = 24;
        public static final int ROUND = 26;
        public static final int FLOOR = 27;
        public static final int DISTINCT = 28;
        public static final int ROWNUMBER = 29;
        public static final int BINNING = 30;
        public static final int DECILE = 31;
        public static final int LOG = 33;
        public static final int LN = 34;
        public static final int DATEDIFF = 25;
        public static final int DATEADD = 35;
        public static final int EXTRACT = 56;
        public static final int DATETRUNC = 39;
        
        public static boolean isArithmetic(int op){
            return op==ADD || op==MINUS || op==MULTIPLY || op==DIVISION;
        }
        
        public static boolean isArithmeticR(int op){
            return op==MINUSR || op==DIVISIONR;
        }
        
        public static boolean isAggregation(int op){
            return op==MIN || op==MAX || op==COUNT || op==SUM || op==AVG || op==RAVG ||
                   op==COUNTDISTINCT || op==QUANTILE || op==QUANTILE2 || op==QUANTILE3;
        }
        
        public static boolean isWindow(int op){
            return op==RANK || op==NTILE || op==ROWNUMBER;
        }
        
        public static int distance(Integer f1, Integer f2){
            if(f1==null && f2==null) return 0;
            if(f1==null || f2==null) return -1;
            if(f1.equals(f2)) return 0;
            
            if(f1==SUM || f1==COUNT) return f2==SUM || f2==COUNT ? 1 : -1;
            if(f2==SUM || f2==COUNT) return f1==SUM || f1==COUNT ? 1 : -1;
            if(f1==MAX || f1==MIN) return f2==MAX || f2==MIN ? 1 : -1;
            if(f2==MAX || f2==MIN) return f1==MAX || f1==MIN ? 1 : -1;
            if(f1==DIVISION || f1==DIVISIONR) return f2==DIVISION || f2==DIVISIONR ? 1 : -1;
            if(f2==DIVISION || f2==DIVISIONR) return f1==DIVISION || f1==DIVISIONR ? 1 : -1;
            if(f1==ADD || f1==MINUS || f1==MINUSR) return f2==ADD || f2==MINUS || f2==MINUSR ? 1 : -1;
            if(f2==ADD || f2==MINUS || f2==MINUSR) return f1==ADD || f1==MINUS || f1==MINUSR ? 1 : -1;
            if(f1==QUANTILE || f1==QUANTILE2 || f1==QUANTILE3) return f2==QUANTILE || f2==QUANTILE2 || f2==QUANTILE3 ? 1 : -1;
            if(f2==QUANTILE || f2==QUANTILE2 || f2==QUANTILE3) return f1==QUANTILE || f1==QUANTILE2 || f1==QUANTILE3 ? 1 : -1;
            
            return -1;
        }
        
        public static int parsingOp(String op){
            if(op.equals("+")) return ADD;
            if(op.equals("-")) return MINUS;
            if(op.equals("*")) return MULTIPLY;
            if(op.equals("×")) return MULTIPLY;
            if(op.equals("/")) return DIVISION;
            if(op.equalsIgnoreCase("Add")) return ADD;
            if(op.equalsIgnoreCase("Minus")) return MINUS;
            if(op.equalsIgnoreCase("Product")) return MULTIPLY;
            if(op.equalsIgnoreCase("Divide")) return DIVISION;
            if(op.equalsIgnoreCase("MinusR")) return MINUSR;
            if(op.equalsIgnoreCase("DivideR")) return DIVISIONR;
            if(op.equalsIgnoreCase("SUM")) return SUM;
            if(op.equalsIgnoreCase("COUNT")) return COUNT;
            if(op.equalsIgnoreCase("AVG")) return AVG;
            if(op.equalsIgnoreCase("MIN")) return MIN;
            if(op.equalsIgnoreCase("MAX")) return MAX;
            if(op.equalsIgnoreCase("RAVG")) return RAVG;
            if(op.equalsIgnoreCase("RMAX")) return RMAX;
            if(op.equalsIgnoreCase("RMIN")) return RMIN;
            if(op.equalsIgnoreCase("RSUM")) return RSUM;
            if(op.equalsIgnoreCase("RAW")) return RAW;
            if(op.equalsIgnoreCase("QUANTILE")) return QUANTILE;
            if(op.equalsIgnoreCase("QUANTILE2")) return QUANTILE2;
            if(op.equalsIgnoreCase("QUANTILE3")) return QUANTILE3;
            if(op.equalsIgnoreCase("POW")) return POW;
            if(op.equalsIgnoreCase("LOG")) return LOG;
            if(op.equalsIgnoreCase("LN")) return LN;
            if(op.equalsIgnoreCase("NTILE")) return NTILE;
            if(op.equalsIgnoreCase("DECILE")) return DECILE;
            if(op.equalsIgnoreCase("BINNING")) return BINNING;
            if(op.equalsIgnoreCase("DATETRUNC")) return DATETRUNC;
            if(op.equalsIgnoreCase("EXTRACT")) return EXTRACT;
            if(op.equalsIgnoreCase("COUNTDISTINCT")) return COUNTDISTINCT;
            return NONE;
        }
        
        public static String toOperator(int op, boolean html){
            if(op==ADD) return "+";
            if(op==MINUS || op==DATEDIFF) return "-";
            if(op==MULTIPLY) return html ? "×" : "*";
            if(op==DIVISION) return html ? "÷" : "/";
            if(op==CONCAT) return "C";
            if(op==CONCATL) return "CL";
            if(op==CONCATR) return "CR";
            return "";
        }
        
        public static String toName(int op){
            return op==SUM ? "Sum" :
                   op==AVG ? "Avg" :
                   op==MAX ? "Max" :
                   op==MIN ? "Min" :
                   op==COUNT ? "Count" :
                   op==RAVG ? "RAvg" :
                   op==RMAX ? "RMax" :
                   op==RMIN ? "RMin" :
                   op==RSUM ? "RSum" :
                   op==RAW ? "Raw" :
                   op==COUNTDISTINCT ? "CountDistinct" :
                   op==QUANTILE ? "Quantile" :
                   op==QUANTILE2 ? "Quantile2" :
                   op==QUANTILE3 ? "Quantile3" :
                   op==MINUS ? "Minus" :
                   op==ADD ? "Add" : 
                   op==MULTIPLY ? "Product" :
                   op==DIVISION ? "Divide" :
                   op==MINUSR ? "MinusR" :
                   op==DIVISIONR ? "DivideR" :
                   op==POW ? "Pow" :
                   op==LOG ? "Log" :
                   op==LN ? "Ln" :
                   op==NTILE ? "NTile" :
                   op==DECILE ? "Decile" :
                   op==BINNING ? "Binning" :
                   op==DATETRUNC ? "DateTrunc" :
                   op==NULL ? EMPTY :
                   op==NONE ? "" :
                   "";
        }
    }
    
    public static final class Cell{
        public static final int NOTE = 3;
        public static final int AGG = 11;
        public static final int MATH = 12;
        public static final int TRANS = 13;
        
        public static int getType(QNode node){
            if(node instanceof QAgg) return AGG;
            if(node instanceof QArith) return MATH;
            if(node instanceof QTra) return TRANS;
            return NONE;
        }
    }
    
    public static final class DAO{
        public static int max_note = 0;
        public static int max_attr = 0;
        public static int max_scenario = 0;
    }
    
    public static int DEFAULT_INPUT = 0;
    public static int DEFAULT_INPUT2 = 0;
    
    public static final String DEFAULT_DS = "";
    public static final String DEFAULT_MS = ",,,";
}
