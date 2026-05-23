package stepper.model.engine;


import java.io.*;
import java.sql.*;
import java.util.*;

import one.dao.*;
import org.duckdb.*;
import org.apache.arrow.memory.*;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.ipc.*;
import org.apache.arrow.vector.types.*;
import org.apache.arrow.vector.types.pojo.*;

import stepper.util.*;
import stepper.model.*;
import stepper.model.sql.*;

public class RTLoader {
    private int arity;
    private long[] spans;
    protected ArrowType[] dtypes;
    protected ArrowType[] ctypes;
    protected Hyb2IntMap[] maps;
    private String sqldims, sqlcols;
    
    private final QRoot root;
    private final ChainDAO dao;
    protected final Loader loader;
    
    protected int vnum;
    protected int[] shares;
    
    public RTLoader(QRoot root, Loader loader){
        this.root = root;
        this.loader = loader;
        this.shares = loader.shareDims();
        dao = new ChainDAO();
    }
    
    public Cube load(){
        init();
        
        DimensionSpace space = loader.getSchema();
        Attribute[] attrs = new Attribute[arity];
        for(int i=0; i<arity; i++) attrs[i] = root.getAttribute(space.get(i).name());
        long batchSize = 1000000, cost = System.currentTimeMillis();
        try{
            dao.open();
            dao.stmt = dao.conn.createStatement();
            //dao.stmt.execute("PRAGMA threads=1");
            if(sqldims.length()!=0){
                dao.rst = dao.stmt.executeQuery(sqldims);
                load(batchSize, attrs);
                dao.rst.close();
            }
            for(int k=0; k<arity; k++) if(shares[k]!=-1) maps[k] = loader.share.maps[shares[k]];
            System.out.println("Dimensions Cost:\t" + (System.currentTimeMillis() - cost) + "ms");
            
            if(loader.maps!=null) loader.maps = maps;
            loader.cube = Cube.instance(this);
            spans = loader.cube.spans();
            
            dao.rst = dao.stmt.executeQuery(sqlcols);
            try(BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);
                ArrowReader reader = (ArrowReader)((DuckDBResultSet)dao.rst).arrowExportStream(allocator, batchSize)){
                VectorSchemaRoot schema = reader.getVectorSchemaRoot();
                for(int i=0; i<vnum; i++) ctypes[i] = schema.getVector(arity+i).getField().getType();
                if(loader.cube instanceof SparseCube) this.read(reader, (SparseCube)loader.cube);
                else this.read(reader, (ArrayCube)loader.cube);
            }
            System.out.println("Input Cost:\t" + (System.currentTimeMillis() - cost) + "ms \t(" + loader.cube.cardinality() + " rows)");
        }catch(IOException e){
            System.out.println("IOException: " + e.getMessage());
        }catch(SQLException e){
            System.out.println("SQLException: " + e.getMessage());
        }finally{
            try{dao.reset(); dao.close();}catch(SQLException e){}
        }
        
        return loader.cube;
    }
    
    private void load(long batchSize, Attribute[] attrs)throws IOException, SQLException{
        try(BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);
            ArrowReader reader = (ArrowReader)((DuckDBResultSet)dao.rst).arrowExportStream(allocator, batchSize)){
            while(reader.loadNextBatch()){
                VectorSchemaRoot schema = reader.getVectorSchemaRoot();
                for(int i=0, k=0, c=0; k<arity; k++){
                    if(shares[k]!=-1) continue;
                    FieldVector vector = schema.getVector(c++);
                    ArrowBuf buf = vector.getDataBuffer();
                    dtypes[k] = vector.getField().getType();
                    if(vector instanceof BigIntVector){
                        if(maps[k].map1==null) maps[k].initLongs(attrs[k].num());
                        while(i<schema.getRowCount() && !vector.isNull(i)) maps[k].map1.put(buf.getLong(i++ * 8L), maps[k].map1.size());
                    }else if(vector instanceof TimeMicroVector){
                        if(maps[k].map1==null) maps[k].initLongs(attrs[k].num());
                        while(i<schema.getRowCount() && !vector.isNull(i)) maps[k].map1.put(buf.getLong(i++ * 8L), maps[k].map1.size());
                    }else if(vector instanceof DateDayVector){
                        if(maps[k].map1==null) maps[k].initLongs(attrs[k].num());
                        while(i<schema.getRowCount() && !vector.isNull(i)) maps[k].map1.put(buf.getInt(i++ * 4L), maps[k].map1.size());
                    }else if(vector instanceof IntVector){
                        if(maps[k].map1==null) maps[k].initLongs(attrs[k].num());
                        while(i<schema.getRowCount() && !vector.isNull(i)) maps[k].map1.put(buf.getInt(i++ * 4L), maps[k].map1.size());
                    }else{
                        if(maps[k].map2==null) maps[k].initObjects(attrs[k].num());
                        while(i<schema.getRowCount() && !vector.isNull(i)) maps[k].map2.put(vector.getObject(i++), maps[k].map2.size());
                    }
                }
            }
        }
    }
    
    private void read(ArrowReader reader, ArrayCube cube)throws IOException{
        int[] locs = null;
        BitSet bits = cube.bits();
        while(reader.loadNextBatch()){
            long cost = System.currentTimeMillis();
            VectorSchemaRoot schema = reader.getVectorSchemaRoot();
            
            int rows = schema.getRowCount();
            if(locs==null) locs = new int[rows];
            else Arrays.fill(locs, 0);
            for(int k=0; k<arity; k++){
                FieldVector vector = schema.getVector(k);
                ArrowBuf buf = vector.getDataBuffer();
                if(vector instanceof BigIntVector){
                    for(int i=0; i<rows; i++) locs[i] += maps[k].map1.get(buf.getLong(i * 8L)) * spans[k];
                }else if(vector instanceof TimeMicroVector){
                    for(int i=0; i<rows; i++) locs[i] += maps[k].map1.get(buf.getLong(i * 8L)) * spans[k];
                }else if(vector instanceof DateDayVector){
                    for(int i=0; i<rows; i++) locs[i] += maps[k].map1.get(buf.getInt(i * 4L)) * spans[k];
                }else if(vector instanceof IntVector){
                    for(int i=0; i<rows; i++) locs[i] += maps[k].map1.get(buf.getInt(i * 4L)) * spans[k];
                }else{
                    for(int i=0; i<rows; i++) locs[i] += maps[k].map2.getInt(vector.getObject(i)) * spans[k];
                }
            }
            for(int i=0; i<rows; i++) bits.set(locs[i]);
            
            for(int c=0; c<cube.values.length; c++){
                if(ctypes[c] instanceof ArrowType.Date){
                    FieldVector vector = schema.getVector(arity + c);
                    DateUnit dateUnit = ((ArrowType.Date)ctypes[c]).getUnit();
                    switch(dateUnit){
                        case DAY:
                            for(int i=0; i<rows; i++) cube.values[c][locs[i]] = (Integer)vector.getObject(i);
                        case MILLISECOND:
                            for(int i=0; i<rows; i++) cube.values[c][locs[i]] = (Long)vector.getObject(i);
                    }
                }else if(ctypes[c] instanceof ArrowType.Int){
                    BigIntVector vector = (BigIntVector)schema.getVector(arity + c);
                    ArrowBuf buf = vector.getDataBuffer();
                    for(int i=0; i<rows; i++) cube.values[c][locs[i]] = buf.getLong(i * 8L);
                }else if(ctypes[c] instanceof ArrowType.Decimal){
                    DecimalVector vector = (DecimalVector)schema.getVector(arity + c);
                    for(int i=0; i<rows; i++) cube.values[c][locs[i]] = vector.getObject(i).doubleValue();
                }else{
                    Float8Vector vector = (Float8Vector)schema.getVector(arity + c);
                    ArrowBuf buf = vector.getDataBuffer();
                    for(int i=0; i<rows; i++) cube.values[c][locs[i]] =  buf.getDouble(i * 8L);
                }
            }
            System.out.println("Batch Cost:\t" + (System.currentTimeMillis() - cost) + "ms \t(" + rows + " rows)");
        }
    }
    
    private void read(ArrowReader reader, SparseCube cube)throws IOException{
        while(reader.loadNextBatch()){
            long cost = System.currentTimeMillis();
            VectorSchemaRoot schema = reader.getVectorSchemaRoot();
            int rows = schema.getRowCount();
            
            cube.expand(rows);
            for(int k=0; k<arity; k++){
                FieldVector vector = schema.getVector(k);
                ArrowBuf buf = vector.getDataBuffer();
                if(vector instanceof BigIntVector){
                    for(int i=0; i<rows; i++) cube.longs.data[cube.longs.size + i] += maps[k].map1.get(buf.getLong(i * 8L)) * spans[k];
                }else if(vector instanceof TimeMicroVector){
                    for(int i=0; i<rows; i++) cube.longs.data[cube.longs.size + i] += maps[k].map1.get(buf.getLong(i * 8L)) * spans[k];
                }else if(vector instanceof DateDayVector){
                    for(int i=0; i<rows; i++) cube.longs.data[cube.longs.size + i] += maps[k].map1.get(buf.getInt(i * 4L)) * spans[k];
                }else if(vector instanceof IntVector){
                    for(int i=0; i<rows; i++) cube.longs.data[cube.longs.size + i] += maps[k].map1.get(buf.getInt(i * 4L)) * spans[k];
                }else{
                    for(int i=0; i<rows; i++) cube.longs.data[cube.longs.size + i] += maps[k].map2.getInt(vector.getObject(i)) * spans[k];
                }
            }
            
            for(int c=0; c<cube.values.length; c++){
                if(ctypes[c] instanceof ArrowType.Date){
                    FieldVector vector = schema.getVector(arity + c);
                    DateUnit dateUnit = ((ArrowType.Date)ctypes[c]).getUnit();
                    switch(dateUnit){
                        case DAY:
                            for(int i=0; i<rows; i++) cube.values[c][cube.longs.size + i] = (Integer)vector.getObject(i);
                        case MILLISECOND:
                            for(int i=0; i<rows; i++) cube.values[c][cube.longs.size + i] = (Long)vector.getObject(i);
                    }
                }else if(ctypes[c] instanceof ArrowType.Int){
                    BigIntVector vector = (BigIntVector)schema.getVector(arity + c);
                    ArrowBuf buf = vector.getDataBuffer();
                    for(int i=0; i<rows; i++) cube.values[c][cube.longs.size + i] = buf.getLong(i * 8L);
                }else if(ctypes[c] instanceof ArrowType.Decimal){
                    DecimalVector vector = (DecimalVector)schema.getVector(arity + c);
                    for(int i=0; i<rows; i++) cube.values[c][cube.longs.size + i] = vector.getObject(i).doubleValue();
                }else{
                    Float8Vector vector = (Float8Vector)schema.getVector(arity + c);
                    ArrowBuf buf = vector.getDataBuffer();
                    for(int i=0; i<rows; i++) cube.values[c][cube.longs.size + i] = buf.getDouble(i * 8L);
                }
            }
            cube.longs.size += rows;
            System.out.println("Batch Cost:\t" + (System.currentTimeMillis() - cost) + "ms \t(" + rows + " rows)");
        }
    }
    
    private void init(){
        DimensionSpace space = loader.getSchema();
        ArrayList<Attribute> measures = loader.getAttributes();
        
        arity = space.size();
        vnum = measures.size();
        spans = new long[arity];
        dtypes = new ArrowType[arity];
        ctypes = new ArrowType[vnum];
        maps = new Hyb2IntMap[arity];
        for(int i=0; i<arity; i++) maps[i] = new Hyb2IntMap();
        
        sqldims = "";
        sqlcols = "SELECT " + space.sequence();
        Condition filter = loader.getFilter();
        
        for(int i=0; i<arity; i++){
            if(shares[i]!=-1) continue;
            
            String union = "";
            for(int k=0; k<arity; k++) if(shares[k]==-1) union += "," + (k==i ? "" : "NULL AS ") + space.get(k).name();
            union = "SELECT " + union.substring(1) + " FROM " + root.makingSQL() + 
            (filter==null || filter.isEmpty() ? "" : " WHERE " + filter.sequence()) + 
            " GROUP BY " + space.get(i).name();
            sqldims += (sqldims.length()==0 ? "" : " UNION ALL ") + union;
        }
        if(sqldims.length()!=0){
            sqldims += " ORDER BY ";
            for(int i=0; i<arity; i++) if(shares[i]==-1) sqldims += space.get(i).name() + ",";
            sqldims = sqldims.substring(0, sqldims.length()-1);
        }
        
        for(Attribute attr: measures) sqlcols += (arity==0 ? "" : ",") + attr.makingSQL(space.sequence(), "", "");
        sqlcols += " FROM " + root.makingSQL() + (filter==null || filter.isEmpty() ? "" : " WHERE " + filter.sequence());
        if(arity>0) sqlcols += (loader.isAggregation() && !loader.isWindow() ? (" GROUP BY " + space.sequence()) : "") + " ORDER BY " + space.sequence();
        System.out.println("SQL Dimensions: " + sqldims + "\n" + "SQL Input:" + sqlcols);
    }
}
