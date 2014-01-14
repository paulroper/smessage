/**
 * HelperMethods.java
 * @author Paul Roper
 * 
 * Contains some utility methods used throughout the application.
 * 
 */

package com.csulcv.Smessage;

import android.util.Log;

public class HelperMethods {
    
    /**
     * Format the given phone number to get the original number minus the area code and without separators.
     * 
     * Currently only works for UK numbers but extend
     * 
     * @param  String phoneNumber The given phone number to format.
     * @return The formatted phone number without the area code and without separators.
     */
    public static String formatPhoneNumber(String phoneNumber) {
        
        String formattedNumberWithoutAreaCode = "";
        String phoneNumberNumeric = "";
        
        Log.d("Phone number before replacements", phoneNumber);
        
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
            phoneNumberNumeric = phoneNumber.replaceAll("[^\\d]", "");
            Log.d("Phone number with only numbers", phoneNumberNumeric);            
            formattedNumberWithoutAreaCode = phoneNumberNumeric.substring(FIRST_NUMBER_AFTER_AREA_CODE_INDEX);            
        } else if (phoneNumber.charAt(0) == '0') {
            phoneNumberNumeric = phoneNumber.replaceAll("[^\\d]", "");
            Log.d("Phone number with only numbers", phoneNumberNumeric);            
            formattedNumberWithoutAreaCode = phoneNumberNumeric.substring(FIRST_NUMBER_AFTER_ZERO_INDEX);
        } else {
            return phoneNumber;
        }
        
        return formattedNumberWithoutAreaCode;
        
    }  
    
}
