/**
 * NewMessageActivity.java
 * @author Paul Roper
 */
package com.csulcv.Smessage;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

public class NewMessageActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private final static String TAG = "Smessage: New Message Activity";
    private static final int LOADER_ID = 0;

    private SmsManager smsManager = SmsManager.getDefault();
    private String enteredPhoneNumber = "";

    // ArrayAdapter stores messages from the SMS database and the ListView displays them
    private ListView messageList = null;
    private SimpleCursorAdapter messages = null;
    private LoaderManager.LoaderCallbacks<Cursor> callbacks = this;


    /**
     * 
     * @see android.app.Activity
     */
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_message);

        setupActionBar();


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
        getMenuInflater().inflate(R.menu.new_message_activity_actions, menu);
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

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {

        Uri smsUri = Uri.parse("content://sms/");
        String numberToFind = PhoneNumberHelperMethods.stripSeparatorsAndAreaCode(enteredPhoneNumber);

        Log.i(TAG, "Address substring: " + numberToFind);

        /*
         * SMS columns seem to be: _ID, THREAD_ID, ADDRESS, PERSON, DATE, DATE_SENT, READ, SEEN, STATUS
         * SUBJECT, BODY, PERSON, PROTOCOL, REPLY_PATH_PRESENT, SERVICE_CENTRE, LOCKED, ERROR_CODE, META_DATA
         */
        String[] returnedColumnsSmsCursor = {"_id", "thread_id", "address", "body", "date", "type", "person"};

        String address = "";

        // Set up WHERE clause; find texts from address containing the number without an area code. If the number is
        // actually a word (like Google), don't strip any separators from the address stored in the table
        if (numberToFind.matches("\\d")) {
            address = "REPLACE(REPLACE(address, ' ', ''), '-', '') LIKE " + DatabaseUtils.sqlEscapeString("%" + numberToFind);
        } else {
            address = "REPLACE(address, ' ', '') LIKE " + DatabaseUtils.sqlEscapeString("%" + numberToFind);
        }

        // Default sort order is date DESC, change to date ASC so texts appear in order
        String sortOrder = "thread_id ASC, date ASC";

        return new CursorLoader(this, smsUri, returnedColumnsSmsCursor, address, null, sortOrder);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor smsCursor) {

        messageList = (ListView) findViewById(R.id.message_list);
        messages = new SimpleCursorAdapter(this,
                                            R.layout.message_list_row,
                                            smsCursor,
                                            new String[] {"body"},
                                            new int[] {R.id.message},
                                            0);

        // Get the cursor loader used for getting messages
        messageList.setAdapter(messages);

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                messageList.setSelection(messages.getCount());
            }

        });

    }

    /*
     * Invoked when the CursorLoader is reset.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        /*
         * Clears out the adapter's reference to the Cursor.
         * This prevents memory leaks.
         */
        messages.swapCursor(null);

    }

    /**
     * Send an SMS message.
     * 
     * @param view The button that this method is assigned to.
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
        if (message != null && contactPhoneNumber != null && message.length() > 0 && contactPhoneNumber.length() > 0) {
            
            Log.i(TAG, "Sending text message"); 

            try {
                ArrayList<String> splitMessage = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(contactPhoneNumber, null, splitMessage, null, null);
            } catch (Exception e) {
                Log.d(TAG, "Error sending message");
            }

            editTextMessage.setText("");

            enteredPhoneNumber = contactPhoneNumber;
            getSupportLoaderManager().initLoader(LOADER_ID, null, this);
            
        } else {            
            // If there's no message to send, do nothing
        }   
        
    }
    
}
