package one.sys;

import one.dao.*;
import java.sql.*;
import java.util.*;
import stepper.model.engine.*;

public class Command{
    private int cmd;
    private String chain;
    private final ArrayList<String> params = new ArrayList();
    
    public Command(String line){
        cmd = UNKNOWN;
        if(line.trim().equalsIgnoreCase("exit") || line.trim().equalsIgnoreCase("quit")) cmd = EXIT;
        else if(line.trim().equalsIgnoreCase("help") || line.trim().equals("?")) cmd = HELP;
        else if(line.startsWith("list") || line.startsWith("show")){
            String[] items = line.split(" ");
            if(items[0].equalsIgnoreCase("list") || items[0].equalsIgnoreCase("show")){
                cmd = LIST;
                if(items.length>1) chain = items[1].toUpperCase();
                for(int i=2; i<items.length; i++){
                    if(items[i].startsWith("--")) params.add(items[i].substring(2).toUpperCase());
                }
            }
        }else if(line.startsWith("query") || line.startsWith("exec")){
            String[] items = line.split(" ");
            if(items[0].equalsIgnoreCase("query") || items[0].equalsIgnoreCase("exec")){
                cmd = QUERY;
                if(items.length>1) chain = items[1].toUpperCase();
                for(int i=2; i<items.length; i++){
                    if(items[i].startsWith("--")) params.add(items[i].substring(2).toUpperCase());
                }
            }
        }
    }
    
    public boolean execute(){
        if(cmd==EXIT) return false;
        if(cmd==HELP) return help();
        if(cmd==UNKNOWN){
            System.out.println("Unsupport");
            return true;
        }
        try{
            if(cmd==LIST) showChains();
            else if(cmd==QUERY) queryChains();
        }catch(SQLException e){
            System.out.println("SQLException: " + e.getMessage());
        }catch(Exception e){
            System.out.println("Exception: " + e.getMessage());
        }
        return true;
    }
    
    public void showChains()throws Exception{
        String node = null;
        if(chain!=null){
            String[] items = chain.split("\\.");
            chain = items[0];
            if(items.length>1) node = items[1];
        }
        double size = 1.0;
        boolean well = params.contains("CTE");
        boolean deep = params.contains("DEEP");
        boolean nosort = params.contains("NOSORT");
        boolean clickhouse = params.contains("CLICKHOUSE");
        for(String param: params){
            if(param.startsWith("SIZE=")){
                try{size = Double.parseDouble(param.substring("size=".length()));}catch(Exception e){}
            }
        }
        new ChainDAO().showChains(chain, node, size, well, deep, nosort, clickhouse);
    }
    
    public void queryChains()throws Exception{
        if(chain==null || chain.length()==0) throw new Exception("Please give the chain to query.");
        
        String qname = null;
        String[] items = chain.split("\\.");
        chain = items[0];
        if(items.length>1) qname = items[1];
        
        int limit = 10;
        double size = 1.0;
        boolean pipe = params.contains("PIPE");
        boolean nosort = params.contains("NOSORT");
        for(String param: params){
            if(param.startsWith("SIZE=")){
                try{size = Double.parseDouble(param.substring("size=".length()));}catch(Exception e){}
            }else if(param.startsWith("LIMIT=")){
                try{limit = Integer.parseInt(param.substring("limit=".length()));}catch(Exception e){}
            }
        }
        new ActionChart(new ChainDAO().getNode(chain, qname, size), pipe).query(nosort, limit);
    }
    
    private boolean help() {
        System.out.println("====================================");
        System.out.println("SQLChains Prototype (VLDB Supplemental)");
        System.out.println("JDK8-based query chain execution tool");
        System.out.println("====================================");
        System.out.println("Core Commands:");
        System.out.println("  1. list [chain_name] [options]");
        System.out.println("     - List query chain details/SQL statements");
        System.out.println("     - Usage examples:");
        System.out.println("       CFP> list                # List all query chains");
        System.out.println("       CFP> list tpc-c1         # List each query in tpc-c1");
        System.out.println("       CFP> list tpc-c1-v1      # List each query in tpc-c1-v1 variant");
        System.out.println("       CFP> list tpc-c1.q14     # List full nested SQL of tpc-c1 (q14 ending)");
        System.out.println("     - Optional parameters:");
        System.out.println("       --size=N      # Scale dataset (default: 1, e.g., --size=3 for 3x scale)");
        System.out.println("       --ClickHouse  # Output ClickHouse-compatible SQL (default: DuckDB)");
        System.out.println("       --nosort      # Remove ORDER BY (except input query)");
        System.out.println("       --cte         # Output SQL in CTE format (default: nested SQL, better performance)");
        System.out.println("  2. exec chain_name [options]");
        System.out.println("     - Execute full/sub query chain and output execution cost for key steps");
        System.out.println("     - Usage examples:");
        System.out.println("       CFP> exec tpc-c1         # Execute tpc-c1 chain (step-by-step)");
        System.out.println("       CFP> exec tpc-c1.q8      # Execute sub-chain of tpc-c1 ending at q8");
        System.out.println("     - Optional parameters:");
        System.out.println("       --pipe        # Pipeline execution");
        System.out.println("       --size=N      # Execute on Nx scale dataset (default: 1)");
        System.out.println("       --nosort      # Execute without sorting");
        System.out.println("       --limit=N     # Output top N results (default: 10)");
        System.out.println("  3. help");
        System.out.println("     - Show this help information");
        System.out.println("  4. quit");
        System.out.println("     - Exit the program");
        System.out.println("====================================");
        System.out.println("Note: ");
        System.out.println("- All execution costs are variable (environment-dependent)");
        System.out.println("- Experimental results in the paper use DEFAULT nested SQL (CTE format has worse performance in DuckDB/ClickHouse comparisons)");
        System.out.println("- For full reproducibility, refer to the README.md file");
        return true;
    }
    
    public static final int UNKNOWN = 0;
    public static final int EXIT = 1;
    public static final int HELP = 2;
    public static final int LIST = 3;
    public static final int QUERY = 4;
}
