
package com.csulcv.smessage;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends ActionBarActivity {

    public final static String TEST_NUMBER = "com.csulcv.smessage.testNumber";
    private final static String TAG = "Smessage: Main Activity";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);
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

    /** Called when the user clicks the Send button */
    public void createNewMessage(View view) throws Exception {
        // Create intent used to move to SendMessage activity
        Intent intent = new Intent(this, SendMessageActivity.class);
        
        // Get test phone number from text box
        EditText editText = (EditText) findViewById(R.id.test_address);
        String testNumber = editText.getText().toString();  
        intent.putExtra(TEST_NUMBER, testNumber);
        Log.i(TAG, "Starting SendMessage activity");          
        try {
            startActivity(intent);            
        } catch (IllegalStateException e) {
            throw new Exception("Error creating new activity");
        } 
    }

}
