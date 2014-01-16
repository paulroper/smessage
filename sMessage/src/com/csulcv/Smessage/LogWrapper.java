/**
 * LogWrapper.java
 * @author Paul Roper
 * 
 * 
 */
package com.csulcv.Smessage;

import android.util.Log;

public class LogWrapper {
    
    private static boolean loggingEnabled = true;
    
    public static void d(String tag, String msg) {        
        if (loggingEnabled)
            Log.d(tag, msg);       
    }
    
    
    public static void e(String tag, String msg) {        
        if (loggingEnabled)
            Log.e(tag, msg);       
    }
    
    
    public static void i(String tag, String msg) {        
        if (loggingEnabled)
            Log.i(tag, msg);       
    }
    
    public static void v(String tag, String msg) {        
        if (loggingEnabled)
            Log.v(tag, msg);       
    }
    
    public static void w(String tag, String msg) {        
        if (loggingEnabled)
            Log.w(tag, msg);       
    }
    
    public static void wtf(String tag, String msg) {        
        if (loggingEnabled)
            Log.wtf(tag, msg);       
    }

}
