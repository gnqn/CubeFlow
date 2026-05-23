package stepper.model.sql;

public class Column {
    private final String max;
    private final String min;
    private final String name;
    private final String symbol;
    private final int level;
    private final int cardi;
    private final int count;
    
    
    public Column(String name, int symb, int level, int cardi, int count, String min, String max){
        this.name = name;
        this.level = level;
        this.cardi = cardi;
        this.count = count;
        this.max = max;
        this.min = min;
        this.symbol = Symbol.LABELS[symb];
    }

    public String max(){return max;}
    public String min(){return min;}
    public String name(){return name;}
    public String symbol(){return this.symbol;}
    public int level(){return this.level;}
    public int count(){return this.count;}
    public int unique(){return this.cardi;}
    
    public static boolean isInt(String data){
        return data.matches("^\\d+$");
    }
    
    public static boolean isMonth(String data){
        return data.matches("^0?[1-9]|1[012]$");
    }
    
    public static boolean isYear(String data){
        return data.matches("^(19|20)\\d{2}$");
    }
    
    public static boolean isDate(String data){
        return data.matches("^\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}$");
    }
    
    public static boolean isTime(String data){
        return data.matches("^\\d{4}[-/]\\d{1,2}[-/]\\d{1,2} \\d{2}:\\d{2}:\\d{2}$");
    }
    
    public static boolean isRegion(String data){
        String[] regions = {
            "North America", "Europe", "Asia", "Oceania", "Latin America", "Africa"
        };
        for(String region: regions) if(region.equalsIgnoreCase(data)) return true;
        return false;
    }
    
    public static boolean isCountry(String data){
        String[] countries = {
            "United Kingdom", "USA", "Singapore", "United States", "Switzerland", 
            "Australia", "China", "Canada", "Japan", "France", "South Korea", "Germany",
            "Netherlands", "Sweden", "Belgium", "New Zealand", "Finland", "Ireland", 
            "Russia", "Norway", "Denmark", "Brazil", "Mexico", "Malaysia", "Chile", 
            "Israel", "Spain", "India", "Italy", "Kazakhstan", "Colombia", "Thailand",
            "Saudi Arabia", "Czech Republic", "Argentina", "Turkey", "Portugal",
            "EIRE", "Poland", "Hungary", "Romania", "Belarus", "Azerbaijan", "Ukraine",
            "Philippines", "Kuwait", "Kazakhstan", "Lithuania", "Uganda", "Ecuador", 
            "Pakistan", "Venezuela", "Puerto Rico", "Costa Rica", "Peru", "Romania"
        };
        for(String country: countries) if(country.equalsIgnoreCase(data)) return true;
        return false;
    }
    
    public static boolean isCity(String data){
        String[] cities = {
            "Cambridge", "Stanford", "Pasadena", "Oxford", "London", "Chicago", "Princeton",
            "Singapore", "Lausanne", "New Haven", "Ithaca", "Baltimore", "Philadelphia", 
            "Edinburgh", "New York City", "New York", "Canberra", "Ann Arbor", "Beijing", 
            "Durham", "Evanston", "Hong Kong", "Berkeley", "Manchester", "Montreal", 
            "Los Angeles", "Toronto", "Paris", "Tokyo", "Seoul", "San Diego", "Bristol", 
            "Parkville", "Shanghai", "Vancouver", "Sydney", "Daejeon", "Providence", "Brisbane",
            "Coventry", "Madison", "Palaiseau", "Amsterdam", "Pittsburgh", "Seattle", "Munich",
            "Delft", "Osaka City", "Osaka", "Glasgow", "Melbourne", "Champaign", "Austin", 
            "Taipei City", "Copenhagen", "Atlanta", "Heidelberg", "Lund", "Durham", 
            "Sendai City", "Nottingham", "St. Andrews", "Chapel Hill", "Leuven", "Auckland",
            "Birmingham", "Pohang", "Sheffield", "Buenos Aires", "Davis", "Southampton", 
            "Columbus", "Boston", "Houston", "Helsinki", "West Lafayette", "Leeds", 
            "Edmonton", "Geneva", "Stockholm", "Uppsala", "Dublin", "Karlsruhe", "Leiden", 
            "Hefei", "Suwon", "Utrecht", "Moscow", "Kongens Lyngby", "Hangzhou", "Oslo", 
            "Groningen", "Nanjing", "Nagoya", "Santa Barbara", "Wageningen", "Berlin", 
            "Adelaide", "Montreal", "York", "Mexico City", "Lancaster", "Sapporo", "Ghent", 
            "Kuala Lumpur", "Espoo", "Fukuoka City", "Aachen", "Hsinchu City", "Waterloo", 
            "Bangalore", "Louvain-la-Neuve", "Barcelona", "Maastricht", "Lyon", "Enschede", 
            "Milan", "New Delhi", "Dhahran", "Nijmegen", "Campinas", "Cape Town", "Cleveland", 
            "Ankara", "Istanbul", "Almaty", "Kent", "Baku", "Khon Kaen", "Hofuf", "Bangkok", 
            "Hat Yai", "Laramie", "Zagreb", "Suzhou", "Wuhan", "Wroclaw", "Hanoi", "Gyeongsan", 
            "Beirut", "Budapest", "Bielefeld", "Binghamton", "Beirut", "Missoula", "Malang", 
            "Saratov", "Saskatoon", "Stirling", "Stillwater", "Stockholm", "Stockton", 
            "Stony Brook", "Storrs", "Stuttgart", "Strasbourg", "Subang Jaya", "Sumy", 
            "Suceava", "Sungai Long", "Sunshine Coast", "Surabaya", "Suwon", "Swansea"
        };
        for(String city: cities) if(city.equalsIgnoreCase(data)) return true;
        return false;
    }
    
    public static boolean isTypeString(String type){
        return type.equals("CHAR") || type.equals("LONGNVARCHAR") ||
               type.equals("LONGVARCHAR") || type.equals("NCHAR") ||
               type.equals("NVARCHAR") || type.equals("VARCHAR");
    }
    
    public static boolean isTypeInt(String type){
        return type.equals("BIGINT") || type.equals("INTEGER") ||
               type.equals("ROWID") || type.equals("SMALLINT") ||
               type.equals("TINYINT");
    }
    
    public static boolean isTypeNumeric(String type){
        return type.equals("DOUBLE") || type.equals("FLOAT") ||
               type.equals("DECIMAL") || type.equals("NUMERIC") ||
               type.equals("REAL");
    }
    
    public static boolean isTypeDate(String type){
        return type.equals("DATE");
    }
    
    public static boolean isTypeBoolean(String type){
        return type.equals("BIT") || type.equals("BOOLEAN");
    }
    
    public static boolean isTypeTime(String type){
        return type.equals("TIME") || type.equals("IME_WITH_TIMEZONE") ||
               type.equals("TIMESTAMP") || type.equals("TIMESTAMP_WITH_TIMEZONE");
    }
}
