
package com.csulcv.smessage;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class SendMessageActivity extends Activity {

    SmsManager smsManager = SmsManager.getDefault();
    private final static String TAG = "Smessage: SendMessage Activity";
    public static String testNumber = "00000000000";
    
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);
        
        // Show the Up button in the action bar.
        setupActionBar();
        
        // Get the message from the intent that created this activity
        Intent intent = getIntent();
        testNumber = intent.getStringExtra(MainActivity.TEST_NUMBER);
        setTitle(testNumber);
        
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
            // Ensure message length doesn't surpass SMS character limit
            for (String s : smsManager.divideMessage(message)) {
                // TODO: Adjust parameters for error checking
                smsManager.sendTextMessage(testNumber, null, s, null, null);
            }         
        } else {            
            // If there's no message to send, do nothing
        }        
    }
    
}
