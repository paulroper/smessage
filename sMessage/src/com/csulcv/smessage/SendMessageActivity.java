
package com.csulcv.smessage;

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

public class SendMessageActivity extends ActionBarActivity {

    private SmsManager smsManager = SmsManager.getDefault();
    
    private final static String TAG = "Smessage: SendMessage Activity";
    private static String testNumber = "";
    
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {         
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);
        
        // Show the Up button in the action bar.
        setupActionBar();
        
        ActionBar actionBar = getSupportActionBar();
        
        // Get the message from the intent that created this activity
        Intent intent = getIntent();
        testNumber = intent.getStringExtra(MainActivity.TEST_NUMBER);
        
        actionBar.setTitle("Test Contact");
        actionBar.setSubtitle(testNumber);     
        
        // Get ListView used for messages and get messages
        ListView messageList = (ListView) findViewById(R.id.message_list);
        ArrayList<String> messages = getMessages();
        
        // Setup adapter for message list using array list of messages
        ArrayAdapter<String> messageListAdapter = new ArrayAdapter<String>(this, R.layout.message_view_row, R.id.message_row, messages);
        
        messageList.setAdapter(messageListAdapter);
        
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
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.send_message_activity_actions, menu);
        return true;
        
    }
    
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
    
    public void sendMessage(View view) throws IllegalArgumentException {      
        
        // Get text message from the text box
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();      
        
        if (message != null) {            
            Log.i(TAG, "Sending text message");   
            
            ArrayList<String> splitMessage = smsManager.divideMessage(message);
            
            smsManager.sendMultipartTextMessage(testNumber, null, splitMessage, null, null);
            
            // Ensure message length doesn't surpass SMS character limit
/*            for (String s : smsManager.divideMessage(message)) {                
                // TODO: Adjust parameters for error checking
                smsManager.sendTextMessage(testNumber, null, s, null, null);
            } */ 
            
        } else {            
            // If there's no message to send, do nothing
        }   
        
    }
    
    // Uses the non-public SMS content provider. Be careful!
    public ArrayList<String> getMessages() {
        
        Uri smsUri = Uri.parse("content://sms");       
        
        // Get rid of area code from number so we can find texts from this number with a LIKE comparison
        String numberWithoutAreaCode = testNumber.substring(1);
        
        Log.i(TAG, "Address substring: " + numberWithoutAreaCode);
        
        /* SMS columns seem to be: _ID, THREAD_ID, ADDRESS, PERSON, DATE, DATE_SENT, READ, SEEN, STATUS
         * SUBJECT, BODY, PERSON, PROTOCOL, REPLY_PATH_PRESENT, SERVICE_CENTRE, LOCKED, ERROR_CODE, META_DATA
         */
        String[] returnedColumns = {"address", "person", "body", "date"};
        
        // Set up WHERE clause; find texts from address containing the number without an area code
        String address = "address LIKE '%" + numberWithoutAreaCode + "'";
        
        // Default sort order is date DESC, change to date ASC so texts appear in order
        String sortOrder = "date ASC";
        
        // ArrayList for storing the message text
        ArrayList<String> messages = new ArrayList<String>();
        
        // Send the query to get SMS messages. Default sort order is date DESC.
        Cursor smsCursor = getContentResolver().query(smsUri, returnedColumns, address, null, sortOrder);
        
        int messageCounter = 0; 
        
        // Use cursor to iterate over the database and get each row
        while (smsCursor.moveToNext()) {            
            
            for (int i = 0; i < smsCursor.getColumnCount(); i++) {
                Log.d(smsCursor.getColumnName(i) + "", smsCursor.getString(i) + "");
                
                if (smsCursor.getColumnName(i).equals("body")) {
                    Log.d("Adding message", "Adding message to ArrayList");
                    messages.add(smsCursor.getString(i));
                }
                
            }
            
            messageCounter++;
            
            Log.d("End of message " + messageCounter, "-----------------");
            
        }

        return messages;        
        
    }
    
}
