/**
 * ConversationActivity.java
 * @author Paul Roper
 *
 *
 */
package com.csulcv.Smessage;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class ConversationActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private SmsManager smsManager = SmsManager.getDefault();
    
    private static final String TAG = "Smessage: SendMessage Activity";
    private static final int LOADER_ID = 0; 
    private static String contactName = "";
    private static String contactPhoneNumber = "";

    // SimpleCursorAdapter stores messages from the SMS database and the ListView displays them
    private ListView messageList = null;
    private SimpleCursorAdapter messages = null;
    
    private boolean loggingEnabled = true;
           
    /**
     * 
     * @see android.app.Activity
     */
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {  
        
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.activity_conversation);
        initialiseActionBar();           
        
        String[] smsColumnsToDisplay = {"body"};
        int[] displayMessageIn = {R.id.message_row}; 
        
        messageList = (ListView) findViewById(R.id.message_list);
        
        messages = new SimpleCursorAdapter(this, 
                R.layout.message_list_row, 
                null, 
                smsColumnsToDisplay,
                displayMessageIn, 
                0);               
        
        // Get the cursor loader used for getting messages
        messageList.setAdapter(messages);   
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        
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
        getMenuInflater().inflate(R.menu.conversation_activity_actions, menu);
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
        
        // TODO: Refresh message list after sending
        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
        
    }
  
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        
        Uri smsUri = Uri.parse("content://sms/");
        String numberWithoutAreaCode = HelperMethods.formatPhoneNumber(contactPhoneNumber);
        
        Log.i(TAG, "Address substring: " + numberWithoutAreaCode);
        
        /* 
         * SMS columns seem to be: _ID, THREAD_ID, ADDRESS, PERSON, DATE, DATE_SENT, READ, SEEN, STATUS
         * SUBJECT, BODY, PERSON, PROTOCOL, REPLY_PATH_PRESENT, SERVICE_CENTRE, LOCKED, ERROR_CODE, META_DATA
         */
        String[] returnedColumnsSmsCursor = {"_id", "thread_id", "address", "body", "date", "type"};

        // Set up WHERE clause; find texts from address containing the number without an area code
        String address = "REPLACE(REPLACE(address, ' ', ''), '-', '') LIKE '%" + numberWithoutAreaCode + "'";
        
        // Default sort order is date DESC, change to date ASC so texts appear in order
        String sortOrder = "thread_id ASC, date ASC";
        
        return new CursorLoader(this, smsUri, returnedColumnsSmsCursor, address, null, sortOrder);        
        
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (loggingEnabled) {       
            
            Log.i(TAG, "Cursor finished loading");            
            int messageCounter = 0;
            
            // Use cursor to iterate over the database and get each row
            while (cursor.moveToNext()) {  
                
                Log.i("Message " + messageCounter, "-----------------");
                
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    Log.d(cursor.getColumnName(i) + "", cursor.getString(i) + "");               
                }
                
                messageCounter++;                
                
            }
            
        }            
        
        /*
         * Moves the query results into the adapter, causing the ListView fronting this adapter to re-display
         */
        messages.swapCursor(cursor);
        
    }    
    
    /*
     * Invoked when the CursorLoader is being reset. For example, this is called if the data in the provider changes 
     * and the Cursor becomes stale.
     * 
     * TODO: Refresh message list when this happens.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        
        /*
         * Clears out the adapter's reference to the Cursor.
         * This prevents memory leaks.
         */
        messages.swapCursor(null);
        
        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);     
        messageList.setSelection(messages.getCount());
        
    }
   
}
