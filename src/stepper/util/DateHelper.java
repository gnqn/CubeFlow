package stepper.util;

import java.util.*;
import stepper.model.sql.*;

public class DateHelper{
    private static final long MICROS_PER_SECOND = 1_000_000L;
    private static final long MICROS_PER_MINUTE = 60 * MICROS_PER_SECOND;
    private static final long MICROS_PER_HOUR = 60 * MICROS_PER_MINUTE;
    
    public static Object[] addHours(int hours, Object[] data, BitSet mark, Hyb2IntMap map){
        Long[] result = new Long[data.length];
        for(int i=0; i<data.length; i++){
            if(mark!=null && !mark.get(i)) continue;
            result[i] = (long)data[i] + hours * MICROS_PER_HOUR;
            map.map1.put((long)data[i] + hours * MICROS_PER_HOUR, i);
        }
        return result;
    }
    
    public static Object[] addMinutes(int minutes, Object[] data, BitSet mark, Hyb2IntMap map){
        Long[] result = new Long[data.length];
        for(int i=0; i<data.length; i++){
            if(mark!=null && !mark.get(i)) continue;
            result[i] = (long)data[i] + minutes * MICROS_PER_MINUTE;
            map.map1.put((long)data[i] + minutes * MICROS_PER_MINUTE, i);
        }
        return result;
    }
    
    public static Object[] addSeconds(int seconds, Object[] data, BitSet mark, Hyb2IntMap map){
        Long[] result = new Long[data.length];
        for(int i=0; i<data.length; i++){
            if(mark!=null && !mark.get(i)) continue;
            result[i] = (long)data[i] + seconds * MICROS_PER_SECOND;
            map.map1.put((long)data[i] + seconds * MICROS_PER_SECOND, i);
        }
        return result;
    }
    
    public static Object[] addDays(int days, Object[] data, BitSet mark, Hyb2IntMap map){
        Long[] result = new Long[data.length];
        for(int i=0; i<data.length; i++){
            if(mark!=null && !mark.get(i)) continue;
            result[i] = (long)data[i] + days;
            map.map1.put((long)data[i] + days, i);
        }
        return result;
    }
    
    public static Object[] addMonths(int months, Object[] data, BitSet mark, Hyb2IntMap map){
        Long[] result = new Long[data.length];
        for(int i=0; i<data.length; i++){
            if(mark!=null && !mark.get(i)) continue;
            int[] ymd = daysToYMD((long)data[i]);
            int totalMonths = (ymd[0] - 1970) * 12 + (ymd[1] - 1) + months;
        
            int newYear = 1970 + totalMonths / 12;
            int newMonth = totalMonths % 12 + 1;
            int newDay = Math.min(ymd[2], daysInMonth(1970 + totalMonths / 12, totalMonths % 12 + 1));
            result[i] = (long)ymdToDays(newYear, newMonth, newDay);
            map.map1.put((long)ymdToDays(newYear, newMonth, newDay), i);
        }
        return result;
    }
    
    public static Object[] truncToMonth(Object[] data, BitSet mark, Hyb2IntMap map, int[] base){
        Long[] result = new Long[data.length];
        for(int i=0; i<data.length; i++){
            if(mark!=null && !mark.get(i)) continue;
            int[] ymd = daysToYMD((long)data[i]);
            int date = ymdToDays(ymd[0], ymd[1], 1);
            int idx = map.map1.get(date);
            if(idx==-1){
                idx = map.map1.size();
                map.map1.put(date, idx);
                result[idx] = (long)date;
            }
            base[i] = idx;
        }
        Long[] temp = new Long[map.map1.size()];
        System.arraycopy(result, 0, temp, 0, temp.length);
        return temp;
    }
    
    public static Object[] truncToYear(Object[] data, BitSet mark, Hyb2IntMap map, int[] base){
        Long[] result = new Long[data.length];
        for(int i=0; i<data.length; i++){
            if(mark!=null && !mark.get(i)) continue;
            int date = ymdToDays(daysToYMD((long)data[i])[0], 1, 1);
            int idx = map.map1.get(date);
            if(idx==-1){
                idx = map.map1.size();
                map.map1.put(date, idx);
                result[idx] = (long)date;
            }
            base[i] = idx;
        }
        Long[] temp = new Long[map.map1.size()];
        System.arraycopy(result, 0, temp, 0, temp.length);
        return temp;
    }
    
    public static int[] daysToYMD(long daysSinceEpoch) {
        int days = (int)daysSinceEpoch;
        int year = 1970;
        
        while (days >= daysInYear(year)) {
            days -= daysInYear(year);
            year++;
        }
        
        int month = 1;
        while (days >= daysInMonth(year, month)) {
            days -= daysInMonth(year, month);
            month++;
        }
        
        int day = days + 1;
        
        return new int[]{year, month, day};
    }
    
    public static int ymdToDays(int year, int month, int day) {
        int days = 0;
        for (int y = 1970; y < year; y++) days += daysInYear(y);
        for(int m = 1; m < month; m++) days += daysInMonth(year, m);
        days += (day - 1);
        return days;
    }
    
    private static int daysInYear(int year) {
        return isLeapYear(year) ? 366 : 365;
    }
    
    private static int daysInMonth(int year, int month) {
        if (month == 2 && isLeapYear(year)) {
            return 29;
        }
        int[] normalDays = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        return normalDays[month - 1];
    }
    
    private static boolean isLeapYear(int year) {
        return (year % 4 == 0) && (year % 100 != 0 || year % 400 == 0);
    }
    
    public static String toIsoString(Object daysEpoch) {
        int[] ymd = daysToYMD((long)daysEpoch);
        return String.format("%04d-%02d-%02d", ymd[0], ymd[1], ymd[2]);
    }
    
    public static String toIsoString(int typ, Object value){
        if(typ==Attribute.Type.DATEDAY){
            int[] ymd = daysToYMD((long)value);
            return String.format("%04d-%02d-%02d", ymd[0], ymd[1], ymd[2]);
        }else if(typ==Attribute.Type.TIME){
            long totalSeconds = (long)value / 1_000_000L;
            int hours = (int) (totalSeconds / 3600);
            int minutes = (int) ((totalSeconds % 3600) / 60);
            int seconds = (int) (totalSeconds % 60);
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.valueOf(value);
    }
}