
import one.sys.*;
import java.io.*;
import java.util.*;

public class CubeFlowDemo {
    public static String DEMO = "CubeFlow>";
    
    public static void main(String[] args){
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "OFF");
        System.setProperty("--illegal-access", "deny");
        
        File chainsDb = new File("chains.db");
        if (!chainsDb.exists() || !chainsDb.isFile()) {
            System.out.println("====================================");
            System.out.println("Missing chains.db!");
            System.out.println("Required directory structure (provided in ZIP package):");
            System.out.println("YourFolder/");
            System.out.println("  ├── SQLChains.jar");
            System.out.println("  ├── chains.db");
            System.out.println("  └── lib/");
            System.out.println("");
            System.out.println("Your current directory is NOT 'YourFolder'. Fix with:");
            System.out.println("1. Navigate to the correct directory and run:");
            System.out.println("    cd /path/to/YourFolder");
            System.out.println("    java -Xmx4g -Xms2g -jar SQLChains.jar");
            System.out.println("2. Or copy chains.db to your current working directory");
            System.out.println("====================================");
            System.exit(0);
        }
        
        System.out.println("Starting CubeFlow prototype...");
        
        Scanner reader = new Scanner(System.in);
        while(true){
            System.out.print(DEMO);
            Command cmd = new Command(reader.nextLine());
            if(!cmd.execute()) break;
        }
    }
}
