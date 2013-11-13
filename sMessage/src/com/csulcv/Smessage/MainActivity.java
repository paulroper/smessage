
package com.csulcv.Smessage;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.PhoneLookup;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

/**
 * MainActivity.java
 * 
 * @author Paul Roper
 *
 */
public class MainActivity extends ActionBarActivity {

    public final static String TEST_NUMBER = "com.csulcv.smessage.testNumber";
    private final static String TAG = "Smessage: Main Activity"; 
    
    /**
     * 
     * @see android.app.Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
                        
        // Get ListView used for contacts and get contacts
        ListView contactList = (ListView) findViewById(R.id.contact_list);
        
        // Setup adapter for message list using array list of messages
        ArrayAdapter<String> contactListAdapter = new ArrayAdapter<String>(this, R.layout.contact_view_row, R.id.contact_name, 
                getContactNames(getContactsFromMessageList(getNewestMesssagesPerConversation())));
        
        contactList.setAdapter(contactListAdapter);       
        
    }
    
    /**
     * 
     * @see android.app.Activity
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);
        return true; 
        
    }
    
    /**
     * 
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
     * Create a new message activity when the user clicks create new.
     * 
     * @param view 
     * @throws Exception Throw an illegal state exception if the activity cannot be created.
     */
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

    /**
     * Get the newest message from a conversation.
     *  
     * Retrieve a list of the newest messages from each conversation thread. These (along with the address of the contact
     * the message is for) are used to build a contacts list.
     * 
     * @return An ArrayList containing Message objects.
     */
    public ArrayList<Message> getNewestMesssagesPerConversation() {     
        
        ArrayList<Message> newestConversationMessages = new ArrayList<Message>();        
        Uri smsUri = Uri.parse("content://sms");    

        /* SMS columns seem to be: _ID, THREAD_ID, ADDRESS, PERSON, DATE, DATE_SENT, READ, SEEN, STATUS
         * SUBJECT, BODY, PERSON, PROTOCOL, REPLY_PATH_PRESENT, SERVICE_CENTRE, LOCKED, ERROR_CODE, META_DATA
         */
        String[] returnedColumns = {"thread_id", "address", "person", "type", "body", "date"};
        
        // Set up WHERE clause; find texts from address containing the number without an area code
        String address = "";
        
        // Default sort order is date DESC, change to date ASC so texts appear in order
        String sortOrder = "thread_id DESC, date ASC";
        
        // Send the query to get SMS messages. Default sort order is date DESC.
        // TODO: Use a CursorLoader, it runs the query in the background.
        Cursor smsCursor = getContentResolver().query(smsUri, returnedColumns, null, null, sortOrder);

        // Indices are fixed constants based on position in the returnedColumns array
        int threadIdColumnIndex = 0;
        int addressColumnIndex = 1;
        int messageBodyColumnIndex = 4;
        
        int messageCounter = 0; 
        String currentThreadId = "";
        
        /* 
         * Use a cursor to iterate over the database. Offsets are used to (hopefully) speed up access.
         * Offsets'll have to be changed based on the number of columns we're querying.
         */
        while (smsCursor.moveToNext()) {  
            currentThreadId = smsCursor.getString(threadIdColumnIndex);                    

            // Last message will always be the newest for the first message thread
            if (smsCursor.isLast()) {
                newestConversationMessages.add(new Message(smsCursor.getString(messageBodyColumnIndex),
                        smsCursor.getString(addressColumnIndex))); 
                                
                Log.d("Message " + (messageCounter + 1), "-----------------");
                Log.d("thread_id", smsCursor.getString(threadIdColumnIndex));
                Log.d("address", smsCursor.getString(addressColumnIndex));
                Log.d("body", smsCursor.getString(messageBodyColumnIndex));
                Log.d("Adding message", "Adding message to ArrayList");
                
                messageCounter++;                
                
                break;
                
            } else {
                smsCursor.moveToNext();
            }
            
            /* 
             * If the next thread ID in the message list is different then we've reached the end of the current thread. As the data
             * is arranged by date, we add this newest message to the list and move on. 
             */
            if (!(smsCursor.getString(threadIdColumnIndex).equals(currentThreadId))) {               

                smsCursor.moveToPrevious();
                
                // If the latest message is one sent to yourself, address is set to null so make sure the user's own address is added
                if (smsCursor.getString(addressColumnIndex).contains("null")) {                    
                    
                    Log.d("NULL", "AHH, SOMETHING'S NULL");
                    
                    Log.d("Message " + messageCounter, "-----------------");
                    Log.d("thread_id", smsCursor.getString(threadIdColumnIndex));
                    Log.d("address", getOwnNumber());
                    Log.d("body", smsCursor.getString(messageBodyColumnIndex));
                    Log.d("Adding message", "Adding message to ArrayList");                  
                    
                    newestConversationMessages.add(new Message(smsCursor.getString(messageBodyColumnIndex), getOwnNumber()));    
                    
                    Log.d("End of message " + messageCounter, "-----------------");
                                        
                } else {
                   
                    Log.d("Message " + messageCounter, "-----------------");
                    Log.d("thread_id", smsCursor.getString(threadIdColumnIndex));
                    Log.d("address", smsCursor.getString(addressColumnIndex));
                    Log.d("body", smsCursor.getString(messageBodyColumnIndex));
                    Log.d("Adding message", "Adding message to ArrayList");
                    
                    newestConversationMessages.add(new Message(smsCursor.getString(messageBodyColumnIndex), 
                            smsCursor.getString(addressColumnIndex)));
                        
                    messageCounter++;                
                    Log.d("End of message " + messageCounter, "-----------------");
                
                }
                    
            } else {                    
                smsCursor.moveToPrevious();
            }    
    
        }
     
        return newestConversationMessages;
    }
    
    /**
     * Use a list of the latest text messages per conversation thread to get a list of contacts for those threads.
     * 
     * @param newestConversationMessages
     * @return
     */
    public ArrayList<Contact> getContactsFromMessageList(ArrayList<Message> newestConversationMessages) {             
        
        /*
         * Contacts columns seem to be: _ID, LOOKUP_KEY, DISPLAY_NAME, PHOTO_ID, IN_VISIBLE_GROUP, HAS_PHONE_NUMBER,
         * TIMES_CONTACTED, LAST_TIME_CONTACTED, STARRED, CUSTOM_RINGTONE, SEND_TO_VOICEMAIL 
         */
        String[] returnedColumns = {PhoneLookup.DISPLAY_NAME, PhoneLookup.PHOTO_ID};

        ArrayList<Contact> contacts = new ArrayList<Contact>();
        String lookupAddress = "";
        Uri lookupUri = null;
        Cursor contactsCursor = null;
        
        int displayNameColumnIndex = 0;
        int photoIdColumnIndex = 1;
        
        for (Message m : newestConversationMessages) {             

            lookupAddress = m.getAddress();
            
            // Use the contact's number from the message list to get their contact name
            lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(lookupAddress));            
                       
            // Run the query
            contactsCursor = getContentResolver().query(lookupUri, 
                    returnedColumns, null, null, null);
            
            /*
             *  If a contact is found for the given address, add it to the contacts list. Otherwise only add the address.
             */
            if (contactsCursor.moveToFirst()) {              
                
                for (int i = 0; i < contactsCursor.getColumnCount(); i++) {
                    Log.d(contactsCursor.getColumnName(i) + "", contactsCursor.getString(i) + "");
                }
                                
                Log.d("Adding contact", contactsCursor.getString(displayNameColumnIndex));
                
                contacts.add(new Contact(contactsCursor.getString(displayNameColumnIndex),
                        contactsCursor.getString(photoIdColumnIndex)));  
                
            } else {  
                Log.d("Adding contact", lookupAddress);                    
                contacts.add(new Contact(lookupAddress, null));          
            } 
            
            // Close the cursor, we're going to open a new one on the next iteration
            contactsCursor.close();
                       
        }
        
        return contacts;
        
    }
    
    /**
     * Convert a list of contacts into a list of contact names.
     * 
     * @param contacts An ArrayList of contacts.
     * @return An ArrayList of contact names.
     */
    public ArrayList<String> getContactNames(ArrayList<Contact> contacts) {
        
        ArrayList<String> contactNames = new ArrayList<String>();
        
        for (Contact c : contacts) {
            contactNames.add(c.getContactName());
        }
        
        return contactNames;        
        
    }
    
    /**
     * Get the user's telephone number.
     * 
     * @return A string containing the user's telephone number.
     */
    public String getOwnNumber() {        
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String ownNumber = telephonyManager.getLine1Number();
        
        return ownNumber;
    }   

}
