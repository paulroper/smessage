/**
 * SendMessageActivity.java
 * 
 * @author Paul Roper
 *
 */
package com.csulcv.Smessage;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
//import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class NewMessageActivity extends ActionBarActivity {

    private SmsManager smsManager = SmsManager.getDefault();
    
    private final static String TAG = "Smessage: NewMessage Activity";
    
    /**
     * 
     * @see android.app.Activity
     */
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_message);
        initialiseActionBar();  
    }  

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
    }    
    
    /**
     * 
     * @see android.app.Activity
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.send_message_activity_actions, menu);
        return true;
        
    }    
    
    /**
     * 
     * @see android.app.Activity
     */    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_search:
                //openSearch();
                return true;
            case R.id.action_settings:
                //openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        
    }
    
    /**
     * Set up the ActionBar for this activity.
     */
    public void initialiseActionBar() {        
        
        // Show the Up button in the action bar.
        setupActionBar();
        
        //ActionBar actionBar = getSupportActionBar();  
        
    }

    /**
     * Send an SMS message.
     * 
     * @param view
     * @throws IllegalArgumentException If there's an error sending the message, throw an exception.
     */
    public void sendMessage(View view) throws IllegalArgumentException {      
        
        // Get the phone number that the user entered
        EditText editTextNumber = (EditText) findViewById(R.id.contact_number);
        String contactPhoneNumber = editTextNumber.getText().toString();

        // Get text message from the text box
        EditText editTextMessage = (EditText) findViewById(R.id.edit_message);
        String message = editTextMessage.getText().toString();      
        
        // If we have a message to send, split it and send it
        // TODO: Error checking!
        if (message != null && contactPhoneNumber != null) {          
            Log.i(TAG, "Sending text message");           
            ArrayList<String> splitMessage = smsManager.divideMessage(message);            
            smsManager.sendMultipartTextMessage(contactPhoneNumber, null, splitMessage, null, null);            
            editTextMessage.setText("");            
        } else {            
            // If there's no message to send, do nothing
        }   
        
    }

    /**
     * Format the given phone number to get the original number minus the area code and without separators.
     * 
     * Currently only works for UK numbers but extend
     * 
     * @param  String phoneNumber The given phone number to format.
     * @return The formatted phone number without the area code and without separators.
     */
    public String formatPhoneNumber(String phoneNumber) {
        
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
