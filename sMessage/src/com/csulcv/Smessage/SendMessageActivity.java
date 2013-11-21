
package com.csulcv.Smessage;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

/**
 * SendMessageActivity.java
 * 
 * @author Paul Roper
 *
 */
public class SendMessageActivity extends ActionBarActivity {

    private SmsManager smsManager = SmsManager.getDefault();
    
    private final static String TAG = "Smessage: SendMessage Activity";
    private static String contactName = "";
    private static String contactPhoneNumber = "";
    
    /**
     * 
     * @see android.app.Activity
     */
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);
        initialiseActionBar();
        initialiseMessageList();       
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
        
        ActionBar actionBar = getSupportActionBar();
        
        // Get the message from the intent that created this activity
        Intent intent = getIntent();
        Bundle contactNameAddress = intent.getBundleExtra(MainActivity.CONTACT_NAME_PHONE_NUMBER);

        contactName = contactNameAddress.getString("CONTACT_NAME");
        contactPhoneNumber = contactNameAddress.getString("CONTACT_PHONE_NUMBER");
        
        if (contactName.equals("null")) {
            actionBar.setTitle(contactPhoneNumber);  
        } else {
            actionBar.setTitle(contactName);
            actionBar.setSubtitle(contactPhoneNumber);
        }        
        
    }
    
    /**
     * Generate the list of messages for the contact this activity was generated from.
     */
    public void initialiseMessageList() {
                
        // Get ListView used for messages and get messages
        ListView messageList = (ListView) findViewById(R.id.message_list);
        
        // TODO: THIS ISN'T FIXING THE PROBLEM, escape contact names!
        try {
            ArrayList<String> messages = getMessages();
            
            // Setup adapter for message list using array list of messages
            ArrayAdapter<String> messageListAdapter = new ArrayAdapter<String>(this, 
                    R.layout.message_view_row, R.id.message_row, messages);
            
            messageList.setAdapter(messageListAdapter);
        } catch (Exception initialiseMessageList) {
            Log.e(TAG, "Error initialising message list");
        }
            
    }    
    
    /**
     * Send an SMS message.
     * 
     * @param view
     * @throws IllegalArgumentException If there's an error sending the message, throw an exception.
     */
    public void sendMessage(View view) throws IllegalArgumentException {      
        
        // Get text message from the text box
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();      
        
        // If we have a message to send, split it and send it
        // TODO: Error checking!
        if (message != null) {          
            Log.i(TAG, "Sending text message");           
            ArrayList<String> splitMessage = smsManager.divideMessage(message);            
            smsManager.sendMultipartTextMessage(contactPhoneNumber, null, splitMessage, null, null);            
            editText.setText("");            
        } else {            
            // If there's no message to send, do nothing
        }   
        
    }

    /**
     * Get the list of SMS messages for the current contact.
     * 
     * @return A list of messages for the number specified
     */
    @SuppressWarnings("unused")
    public ArrayList<String> getMessages() throws Exception {        

        Uri smsUri = Uri.parse("content://sms/");
        Uri smsConversationsUri = Uri.parse("content://sms/conversations");   
        
        ArrayList<String> messages = new ArrayList<String>();
        String numberWithoutAreaCode = formatPhoneNumber(contactPhoneNumber);
        
        Log.i(TAG, "Address substring: " + numberWithoutAreaCode);
        
        /* SMS columns seem to be: _ID, THREAD_ID, ADDRESS, PERSON, DATE, DATE_SENT, READ, SEEN, STATUS
         * SUBJECT, BODY, PERSON, PROTOCOL, REPLY_PATH_PRESENT, SERVICE_CENTRE, LOCKED, ERROR_CODE, META_DATA
         */
        String[] returnedColumnsSmsCursor = {"thread_id", "address", "body", "date"};
        String[] returnedColumnsSmsConversationCursor = {"thread_id", "msg_count", "snippet"};
        
        // Set up WHERE clause; find texts from address containing the number without an area code
        String address = "REPLACE(REPLACE(address, ' ', ''), '-', '') LIKE '%" + numberWithoutAreaCode + "'";
        
        // Default sort order is date DESC, change to date ASC so texts appear in order
        String sortOrder = "thread_id ASC, date ASC";
        
        // Send the query to get SMS messages. Default sort order is date DESC.
        // TODO: Use a CursorLoader, it runs the query in the background.
        Cursor smsCursor = getContentResolver().query(smsUri, returnedColumnsSmsCursor, address, null, sortOrder);
        Cursor smsConversationCursor = getContentResolver().query(smsConversationsUri, null, null, null, sortOrder);
        
        int messageCounter = 0; 
        
        // Use cursor to iterate over the database and get each row
        while (smsCursor.moveToNext()) {            
            
            for (int i = 0; i < smsCursor.getColumnCount(); i++) {
                Log.d(smsCursor.getColumnName(i) + "", smsCursor.getString(i) + "");
                
                if (smsCursor.getColumnName(i).equals("body") && smsCursor.getString(i) != null) {
                    Log.d("Adding message", "Adding message to ArrayList");
                    messages.add(smsCursor.getString(i));
                }
                
            }
            
            messageCounter++;
            
            Log.d("End of message " + messageCounter, "-----------------");
            
        }
        
        smsCursor.close();
        smsConversationCursor.close();

        return messages;        
        
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
        } else if (contactPhoneNumber.charAt(0) == '0') {
            phoneNumberNumeric = phoneNumber.replaceAll("[^\\d]", "");
            Log.d("Phone number with only numbers", phoneNumberNumeric);            
            formattedNumberWithoutAreaCode = phoneNumberNumeric.substring(FIRST_NUMBER_AFTER_ZERO_INDEX);
        } else {
            return phoneNumber;
        }
        
        return formattedNumberWithoutAreaCode;
        
    }    
    
}
