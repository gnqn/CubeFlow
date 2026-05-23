package one.sys;

import java.util.*;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

public class Monitor extends Thread {
    private volatile boolean running = true;
    private final ArrayList<Long> mems = new ArrayList();
    
    public Monitor(){
        
    }
    
    @Override
    public void run(){
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long start = osBean.getFreePhysicalMemorySize();
        while(running){
            long mem = start - osBean.getFreePhysicalMemorySize();
            mems.add(mem);
            System.out.println(mem/1024/1024 + "M");
            try{Thread.sleep(1000);} 
            catch(InterruptedException e){Thread.currentThread().interrupt();break;}
        }
    }
    
    public long avgMemory(){
        long sum = 0;
        for(long mem: mems) sum += mem;
        return sum/mems.size()/1024/1024;
    }
    
    public long maxMemory(){
        long max = 0;
        for(long mem: mems) if(mem>max) max = mem;
        return max/1024/1024;
    }
    
    public void stopMonitoring() {
        running = false;
    }
}
