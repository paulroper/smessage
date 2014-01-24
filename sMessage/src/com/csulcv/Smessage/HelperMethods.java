/**
 * HelperMethods.java
 * @author Paul Roper
 * 
 * Contains some utility methods used throughout the application.
 */
package com.csulcv.Smessage;

import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;

public class HelperMethods {
    
    /**
     * Get the user's telephone number.
     * 
     * @param activityContext The context of the activity that this method was called from.
     * @return A String containing the user's telephone number.
     */
    public String getOwnNumber(Context activityContext) {    
        
        TelephonyManager telephonyManager = 
                (TelephonyManager) activityContext.getSystemService(Context.TELEPHONY_SERVICE);
        
        String ownNumber = telephonyManager.getLine1Number();        
        
        return PhoneNumberUtils.formatNumber(ownNumber);
        
    }  
    
    /**
     * Format the given phone number to get the original number minus the area code and without separators.
     * 
     * @param  String phoneNumber The given phone number to format.
     * @return The formatted phone number without the area code and without separators.
     */
    public static String stripSeparatorsAndAreaCode(String phoneNumber) {
        
        String phoneNumberWithoutSeparators = "";
        String phoneNumberStripped = "";
                
        Log.d("Phone number to format:", phoneNumber);
        
        final int FIRST_NUMBER_AFTER_AREA_CODE_INDEX = 2;
        final int FIRST_NUMBER_AFTER_ZERO_INDEX = 1;
        
        /* 
         * Get rid of area code from number so we can find texts from this number with a LIKE comparison. If the number
         * starts with a +XX, get the rest of the number after it. If it starts with a 0, get the rest of the number
         * after that. If neither of these apply (e.g. The number is actually a name like some couriers use) then just
         * return the original phone number (or name).
         * 
         */
        // TODO: Only works for UK numbers at the moment, extend to any!
        if (phoneNumber.charAt(0) == '+') {    
            
            // Use the regex [^\\d] to remove all non-numeric characters from the phone number
            phoneNumberWithoutSeparators = phoneNumber.replaceAll("[^\\d]", "");
            phoneNumberStripped = phoneNumberWithoutSeparators.substring(FIRST_NUMBER_AFTER_AREA_CODE_INDEX);
            
            Log.d("Phone number without separators:", phoneNumberWithoutSeparators);  
            
        } else if (phoneNumber.charAt(0) == '0') {
            
            phoneNumberWithoutSeparators = phoneNumber.replaceAll("[^\\d]", "");
            phoneNumberStripped = phoneNumberWithoutSeparators.substring(FIRST_NUMBER_AFTER_ZERO_INDEX);
            
            Log.d("Phone number without separators:", phoneNumberWithoutSeparators);    
            
        } else {
            return phoneNumber;
        }
        
        Log.d("Phone number without separators and area code:", phoneNumberStripped);
        
        return phoneNumberStripped;
        
    }  
    
}
