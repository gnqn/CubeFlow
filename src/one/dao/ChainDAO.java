package one.dao;

import java.sql.*;
import java.util.*;
import stepper.model.*;
import stepper.model.sql.*;

public class ChainDAO extends BaseDAO{
    private void showChains()throws SQLException{
        String sql = "SELECT a.id, a.name, c.size FROM scenarios a, prj_tables b, tables c " + 
                     "WHERE a.prjID=b.prjID AND b.tableID=c.id ORDER BY a.id";
        int sid = 0;
        String chain = "Chain: ";
        for(String[] fields: this.getRecords(sql)){
            if(sid!=Integer.parseInt(fields[0])){
                if(sid!=0){
                    System.out.println(chain);
                    chain = "Chain: ";
                }
                sid = Integer.parseInt(fields[0]);
                chain += fields[1] + "\tSizes: " + fields[2];
            }else{
                chain += "/" + fields[2];
            }
        }
        if(sid!=0) System.out.println(chain);
    }
    
    public void showChains(String chain, String node, double size, boolean cte, boolean deep, boolean nosort, boolean clickhouse)throws Exception{
        if(chain==null || chain.length()==0) showChains();
        else showSQL(chain, node==null ? "" : node, size, cte, deep, nosort, clickhouse);
    }
    
    public QNode getNode(String chain, String qname, double size)throws Exception{
        Flowchart chart = loading(chain, size);
        if(chart==null) throw new Exception("There is no chain " + chain + (size==1.0 ? "" : (" of size=" + size)) + ".");
        if(qname==null || qname.length()==0) return chart.getEnds().get(0);
        
        QNode node = chart.getNode(qname);
        if(node==null) throw new Exception("There is no query " + qname + " in " + chain + ".");
        return node;
    }
    
    private void showSQL(String chain, String qname, double size, boolean cte, boolean deep, boolean nosort, boolean clickhouse)throws Exception{
        Flowchart chart = loading(chain, size);
        if(chart==null) throw new Exception("There is no chain " + chain + (size==1.0 ? "" : (" of size=" + size)) + ".");
        if(qname.length()!=0){
            QNode node = chart.getNode(qname);
            if(node==null) throw new Exception("There is no query " + qname + " in " + chain + ".");
            System.out.println(node.name() + ":\t" + (cte ? node.makingWellSQL() : node.makingSQL(true, nosort, clickhouse)));
        }else{
            for(QNode node: chart.nodes()) System.out.println(node.name() + ":\t" + (cte ? node.makingWellSQL() : node.makingSQL(deep, nosort, clickhouse)));
        }
    }
    
    private QNode makingNode(Flowchart chart, int id, String name, int input, int input2, int typ, String props)throws Exception{
        QNode node = typ==SQLItem.Cell.AGG ? new QAgg(id, name) :  
                     typ==SQLItem.Cell.MATH ? new QArith(id, name) : 
                     typ==SQLItem.Cell.TRANS ? new QTra(id, name) : null;
        if(node==null) return node;
        
        node.setProperties(true, chart.get(input), chart.get(input2), props.split(SQLItem.COMMON_SPLITOR, -1));
        return node;
    }
    
    private Flowchart loading(String chain, double size)throws Exception{
        QRoot root = null;
        String sql = "SELECT c.id, c.name, c.file, d.name, count " + 
                     "FROM scenarios a, prj_tables b, tables c, attrs d " + 
                     "WHERE a.name='" + chain + "' AND a.prjID=b.prjID " + 
                     "AND b.tableID=c.id AND c.size=" + size + " AND c.tid=d.tid ORDER BY c.id, d.id";
        String sql2 = "SELECT b.id, b.name, input, input2, typ, props FROM scenarios a, notes b " + 
                      "WHERE a.name='" + chain + "' AND a.id=b.sid ORDER BY b.id";
        ArrayList<QRoot> roots = new ArrayList();
        try{
            this.open();
            for(String[] fields: this.queryRecords(sql)){
                int tid = Integer.parseInt(fields[0]);
                if(root==null || root.id()!=tid){
                    root = new QRoot(tid, fields[1], fields[2]);
                    roots.add(root);
                }
                root.addAttribute(fields[3], Integer.parseInt(fields[4]));
            }
            if(root==null) return null;
            Flowchart chart = new Flowchart(0, roots);
            for(String[] fields: this.queryRecords(sql2)){
                QNode node = makingNode(chart, Integer.parseInt(fields[0]), fields[1], Integer.parseInt(fields[2]), Integer.parseInt(fields[3]), Integer.parseInt(fields[4]), fields[5]);
                chart.addNode(node);
            }
            return chart;
        }finally{
            this.close();
        }
    }
}
