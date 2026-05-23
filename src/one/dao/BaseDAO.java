package one.dao;

import java.sql.*;
import java.util.*;

public abstract class BaseDAO {
    public Connection conn;
    public Statement stmt;
    public ResultSet rst;
    
    public void submit(ArrayList<String> batch)throws SQLException{
        try{
            this.beginCommit();
            this.batchUpdate(batch.toArray(new String[0]));
        }catch(SQLException e){
            this.doException(e);
            throw e;
        }finally{
            this.endCommit();
        }
    }
    
    public void open()throws SQLException{
        try{
            if(conn!=null) close();
            Class.forName(driver);
            conn = DriverManager.getConnection(url);
        }catch(ClassNotFoundException e){
            throw new SQLException("未找到驱动程序：" + e.getMessage());
        }
    }
    
    public void close()throws SQLException{
        try{
            if(conn!=null) conn.close();
        }catch(SQLException e){
            doException(e);
        }
        this.rst = null;
        this.stmt = null;
        this.conn = null;
    }
    
    public void reset()throws SQLException{
        if(rst!=null) rst.close();
        if(stmt!=null) stmt.close();
    }
    
    protected void doException(Exception e)throws SQLException{
        try{
            if(rst!=null) rst.close();
            if(stmt!=null) stmt.close();
            if(conn!=null && !conn.getAutoCommit()) conn.rollback();
            if(conn!=null) conn.close();
        }catch(SQLException e1){
            System.out.println(e1.getMessage());
        }
        rst = null;
        stmt = null;
        conn = null;
        throw new SQLException(e.getMessage());
    }
    
    public int count(String sql)throws SQLException{
        int count = 0;
        try{
            stmt = conn.createStatement();
            rst = stmt.executeQuery(sql);
            if(!rst.next()){reset(); return 0;}
            String d = rst.getString(1);
            count = d==null ? 0 : Integer.parseInt(d);
            reset();
        }catch(SQLException e){
            doException(e);
        }
        return count;
    }
    
    public int lastId()throws SQLException{
        try{
            if(stmt==null) stmt = conn.createStatement();
            rst = stmt.executeQuery("select last_insert_id()");
            if(rst.next()) return rst.getInt(1);
            reset();
        }catch(SQLException e){
            doException(e);
        }
        return 0;
    }
    
    public ArrayList<String[]> getTable(String table, int rows)throws SQLException{
        String[] queries = {
            "SELECT * FROM " + table + " LIMIT 15",
            "SELECT * FROM " + table + " LIMIT " + (rows-5) + ", 5"
        };
        ArrayList<String[]> data = new ArrayList();
        try{
            conn.setCatalog("datasets");
            stmt = conn.createStatement();
            rst = stmt.executeQuery(queries[0]);
            
            String[] colums = new String[rst.getMetaData().getColumnCount() + 1];
            colums[0] = "";
            for(int i=0; i<colums.length - 1; i++) colums[i+1] = rst.getMetaData().getColumnName(i+1);
            data.add(colums);
            
            int row = 0;
            while(rst.next()){
                row++;
                String[] fields = new String[colums.length];
                data.add(fields);
                fields[0] = row + "";
                for(int i=0; i<colums.length - 1; i++) fields[i+1] = rst.getString(i+1);
            }
            row = rows - 5;
            if(rows>15){
                rst = stmt.executeQuery(queries[1]);
                while(rst.next()){
                    row++;
                    String[] fields = new String[colums.length];
                    data.add(fields);
                    fields[0] = row + "";
                    for(int i=0; i<colums.length - 1; i++) fields[i+1] = rst.getString(i+1);
                }
            }
            reset();
        }catch(SQLException e){
            doException(e);
        }finally{
            try{conn.setCatalog("stepper");}catch(SQLException e){}
        }
        
        return data;
    }
    
    public String[] queryRecord(String sql)throws SQLException{
        String[] fields = new String[0];
        try{
            stmt = conn.createStatement();
            rst = stmt.executeQuery(sql);
            if(!rst.next()){reset(); return fields;}
            fields = new String[rst.getMetaData().getColumnCount()];
            for(int i=0; i<fields.length; i++) fields[i] = rst.getString(i+1);
            reset();
        }catch(SQLException e){
            doException(e);
        }
        return fields;
    }
    
    public ArrayList<String[]> getRecords(String sql)throws SQLException{
        try{
            this.open();
            return this.queryRecords(sql);
        }finally{
            this.close();
        }
    }
    
    public ArrayList<String[]> queryRecords(String sql)throws SQLException{
        ArrayList<String[]> records = new ArrayList();
        try{
            stmt = conn.createStatement();
            rst = stmt.executeQuery(sql);
            int cols = rst.getMetaData().getColumnCount();
            while(rst.next()){
                String[] fields = new String[cols];
                for(int i=0; i<cols; i++){
                    try{fields[i] = rst.getString(i+1);}
                    catch(Exception e){fields[i]="U#";}
                }
                records.add(fields);
            }
            reset();
        }catch(SQLException e){
            doException(e);
        }
        return records;
    }
    
    public ArrayList<String[]> queryRecords(String catalog, String sql)throws SQLException{
        String _catalog = "stepper";
        try{
            _catalog = conn.getCatalog();
            conn.setCatalog(catalog);
            return this.queryRecords(sql);
        }catch(SQLException e){
            doException(e);
            throw e;
        }finally{
            conn.setCatalog(_catalog);
        }
    }

    public ResultSet executeQuery(String sql)throws SQLException{
        try{
            stmt = conn.createStatement();
            rst = stmt.executeQuery(sql);
        }catch(SQLException e){
            doException(e);
        }
        return rst;
    }
    
    public void executeUpdate(String sql)throws SQLException{
        try{
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
            reset();
        }catch(SQLException e){
            doException(e);
        }
    }
    
    public void batchUpdate(ArrayList<String> batch)throws SQLException{
        this.batchUpdate(batch.toArray(new String[0]));
    }
    
    public void batchUpdate(String[] sql)throws SQLException{
        if(sql==null || sql.length==0) return;
        try{
            stmt = conn.createStatement();
            for(int i=0; i<sql.length; i++) stmt.addBatch(sql[i]);
            stmt.executeBatch();
            reset();
        }catch(SQLException e){
            doException(e);
        }
    }
    
    protected void beginCommit()throws SQLException{
        try{
            if(conn==null) open();
            conn.setAutoCommit(false);
        }catch(SQLException e){
            doException(e);
        }
    }
    
    protected void endCommit()throws SQLException{
        if(conn==null) return;
        try{
            if(!conn.getAutoCommit()) conn.commit();
        }catch(SQLException e){
            doException(e);
        }
        close();
    }

    public String sequence(Collection<Integer> set){
        if(set==null || set.isEmpty()) return "";
        
        StringBuilder sql = new StringBuilder();
        set.stream().forEach((id) -> {sql.append(",").append(id);});
        return sql.substring(1);
    }
    
    public String sequence(ArrayList<String[]> records){
        if(records.isEmpty()) return "";
        
        StringBuilder seq = new StringBuilder();
        records.stream().forEach((fields) -> {seq.append(",").append(fields[0]);});
        return seq.substring(1);
    }
    
    public int toInt(String field){
        return field==null ? 0 : Integer.parseInt(field);
    }
    
    private static final String driver = "org.duckdb.DuckDBDriver";
    private static final String url = "jdbc:duckdb:chains.db";
}